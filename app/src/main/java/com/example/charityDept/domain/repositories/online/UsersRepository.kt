package com.example.charityDept.domain.repositories.online

import android.content.Context
import com.example.charityDept.core.di.AdminAuth
import com.example.charityDept.core.di.UsersRef
import com.example.charityDept.data.mappers.toUsers
import com.example.charityDept.data.model.AssignedRole
import com.example.charityDept.data.model.UserProfile
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


data class UsersSnapshot(
    val users: List<UserProfile>,
    val fromCache: Boolean,        // true = served from local cache (offline or warming up)
    val hasPendingWrites: Boolean  // true = local changes not yet synced
)

data class LoginGate(val disabled: Boolean, val failedAttempts: Int)

interface UsersRepository {
    suspend fun fetchProfile(uid: String): UserProfile?
    //    fun streamAllUsers(limit: Long = 500): Flow<List<UserProfile>>
    suspend fun getUserFast(id: String): UserProfile?


    fun streamAllUserSnapshot(): Flow<UsersSnapshot>

    fun streamByViewerPreference(pref: AssignedRole): Flow<UsersSnapshot>

    fun streamByViewerPreferenceResilient(pref: AssignedRole ): Flow<UsersSnapshot>

    suspend fun setUserRoles(uid: String, assignedRoles: List<AssignedRole>)
    suspend fun claimPlaceholderIfAny(uid: String, email: String?)
    suspend fun claimPlaceholderIfAnySafe(uid: String, email: String?, usersRef: CollectionReference, isOnline: Boolean)
    suspend fun setUserDisabled(uid: String, disabled: Boolean)
    suspend fun upsertProfileByUid(uid: String, profile: UserProfile)
    suspend fun currentRoles(auth: FirebaseAuth): List<String>
    suspend fun currentRolesSafe(auth: FirebaseAuth, usersRef: CollectionReference): List<String>
    suspend fun isAdminOrLead(auth: FirebaseAuth): Boolean
    suspend fun upsertPlaceholderByEmail(email: String, assignedRoles: List<AssignedRole>, displayName: String? = null)

    // IMPORTANT: keep admin session — create user with secondary auth
    suspend fun registerUserAsAdmin(
        adminAuth: FirebaseAuth,
        email: String,
//        password: String,
        displayName: String? = null,
        userRole: AssignedRole,
        disabled: Boolean = false
    ): UserProfile

    suspend fun promoteUser(uid: String, newAssignedRole: AssignedRole)

    suspend fun sendPasswordReset(email: String)

    suspend fun softDeleteUserAndLog(uid: String, email: String?, reason: String? = null)

}

@Singleton
class UsersRepositoryImpl @Inject constructor(
    @UsersRef private val usersRef: CollectionReference,
    private val defaultAuth: FirebaseAuth,
    private val defaultDb: FirebaseFirestore,
    @AdminAuth private val adminAuth: FirebaseAuth,
//    @AdminAuth private val secondaryAuth: FirebaseAuth,
    @ApplicationContext private val appContext: Context
) : UsersRepository {

    //    // --- Secondary FirebaseAuth that won’t touch the main admin session
//    private val secondaryAuth: FirebaseAuth by lazy {
//        val name = "admin-util-auth"
//        val existing = FirebaseApp.getApps(appContext).firstOrNull { it.name == name }
//        val app = existing ?: FirebaseApp.initializeApp(
//            appContext,
//            FirebaseOptions.fromResource(appContext)!!,
//            name
//        )!!
//        FirebaseAuth.getInstance(app)
//    }
    private fun Query.toUsersSnapshotFlow(): Flow<UsersSnapshot> = callbackFlow {
        val reg = this@toUsersSnapshotFlow.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val docs = snap ?: return@addSnapshotListener
            val users = docs.documents.mapNotNull { it.toObject(UserProfile::class.java) }
            trySend(
                UsersSnapshot(
                    users = users,
                    fromCache = docs.metadata.isFromCache,
                    hasPendingWrites = docs.metadata.hasPendingWrites()
                )
            )
        }
        awaitClose { reg.remove() }
    }

    override suspend fun fetchProfile(uid: String): UserProfile? {
        val snap = usersRef.document(uid).get().await()
        if (snap.getBoolean("deletedUser") == true) return null
        return if (snap.exists()) snap.toObject(UserProfile::class.java) else null
    }

    override suspend fun getUserFast(id: String): UserProfile? {
        val doc = usersRef.document(id)
        // 1) Try CACHE first (instant if available)
        try {
            val cache = doc.get(Source.CACHE).await()
            cache.toObject(UserProfile::class.java)?.let { return it }
        } catch (_: Exception) {
            // cache miss — fall back to server
        }
        // 2) SERVER for fresh data
        val server = doc.get(Source.SERVER).await()
        return server.toObject(UserProfile::class.java)
    }


//    override fun streamAllUsers(limit: Long): Flow<List<UserProfile>> = callbackFlow {
//        val reg = FirebaseFirestore.getInstance()
//            .collection("users")
//            .orderBy("email")
//            .limit(limit)
//            .addSnapshotListener { snap, err ->
//                if (err != null) {
//                    trySend(emptyList()).isSuccess
//                    return@addSnapshotListener
//                }
//                trySend(snap?.toObjects(UserProfile::class.java) ?: emptyList()).isSuccess
//            }
//        awaitClose { reg.remove() }
//    }

    override fun streamAllUserSnapshot(): Flow<UsersSnapshot> = callbackFlow {
        val q = usersRef
            .whereEqualTo("deletedUser", false)
            .orderBy("email")

        val registration = q.addSnapshotListener { snap, err ->
            if (err != null) {
                cancel("Firestore listener error", err)
                return@addSnapshotListener
            }
            val list = snap!!.toUsers()
            val meta = snap.metadata
            val fromCache = meta.isFromCache
            val hasLocalWrites = meta.hasPendingWrites()
            trySend(UsersSnapshot(list, fromCache, hasLocalWrites)).isSuccess
        }

        awaitClose { registration.remove() }
    }

    override fun streamByViewerPreference(pref: AssignedRole): Flow<UsersSnapshot> =
        usersRef
            .whereEqualTo("deletedUser", false)
//            .whereEqualTo("userRole", AssignedRole.VOLUNTEER.name)
            .whereEqualTo("userRole", pref.name) // enums saved as strings
            .orderBy("email")
            .orderBy( "displayName")
            .toUsersSnapshotFlow()


    override fun streamByViewerPreferenceResilient(pref: AssignedRole): Flow<UsersSnapshot> =
        combine(
            streamByViewerPreference(pref),
            streamAllUserSnapshot()
        ) { filtered, all ->
            if (filtered.fromCache && filtered.users.isEmpty() && all.users.isNotEmpty()) {
                filtered.copy(
                    users = all.users.filter { it.userRole == pref },
                    fromCache = true // stay honest about cache origin
                )
            } else filtered
        }

    override suspend fun isAdminOrLead(auth: FirebaseAuth): Boolean {
        val roles = currentRoles(auth)
        return "ADMIN" in roles || "LEAD" in roles
    }

    override suspend fun setUserRoles(uid: String, assignedRoles: List<AssignedRole>) {
        usersRef.document(uid).update("roles", assignedRoles).await()
    }

    override suspend fun claimPlaceholderIfAny(uid: String, email: String?) {
        val e = email?.trim()?.lowercase() ?: return
        val pendingId = "_pending_$e"
        val pendingSnap = runCatching { usersRef.document(pendingId).get().await() }.getOrElse { return }
        if (!pendingSnap.exists()) return
        runCatching { usersRef.document(pendingId).delete().await() }
    }

    override suspend fun claimPlaceholderIfAnySafe(
        uid: String, email: String?, usersRef: CollectionReference, isOnline: Boolean
    ) {
        if (!isOnline) return
        val e = email?.trim()?.lowercase() ?: return
        val pendingId = "_pending_$e"
        val pendingSnap = runCatching { usersRef.document(pendingId).get().await() }.getOrNull() ?: return
        if (!pendingSnap.exists()) return
        runCatching { usersRef.document(pendingId).delete().await() }
    }

    override suspend fun setUserDisabled(uid: String, disabled: Boolean) {
        usersRef.document(uid).update("disabled", disabled).await()
    }

    override suspend fun currentRoles(auth: FirebaseAuth): List<String> {
        val user = auth.currentUser ?: return emptyList()
        val token = user.getIdToken(true).await()
        val fromClaims = (token.claims["roles"] as? List<*>)?.filterIsInstance<String>().orEmpty()
        if (fromClaims.isNotEmpty()) return fromClaims
        val doc = usersRef.document(user.uid).get().await()
        return (doc.get("roles") as? List<*>)?.filterIsInstance<String>().orEmpty()
    }

    override suspend fun currentRolesSafe(auth: FirebaseAuth, usersRef: CollectionReference): List<String> {
        val user = auth.currentUser ?: return emptyList()
        val claims = runCatching { user.getIdToken(false).await().claims }.getOrNull().orEmpty()
        val claimRoles = (claims["roles"] as? List<*>)?.filterIsInstance<String>().orEmpty()
        if (claimRoles.isNotEmpty()) return claimRoles
        val fromCache = runCatching { usersRef.document(user.uid).get(Source.CACHE).await() }.getOrNull()
        val docRoles = (fromCache?.get("roles") as? List<*>)?.filterIsInstance<String>().orEmpty()
        return docRoles
    }

    override suspend fun upsertPlaceholderByEmail(email: String, assignedRoles: List<AssignedRole>, displayName: String?) {
        val key = "_pending_${email.trim().lowercase()}"
        val placeholder = mapOf(
            "email" to email.trim().lowercase(),
            "displayName" to (displayName ?: ""),
            "userRole" to assignedRoles,
            "disabled" to false,
            "deletedUser" to false,
            "isPlaceholder" to true
        )
//        if (!ui.disabled) runCatching { usersCol.document(uid).set(mapOf("failedAttempts" to 0), SetOptions.merge()).await() }

        usersRef.document(key).set(placeholder).await()
    }

    // ---------- THE IMPORTANT PART ----------
    // Create user via SECONDARY auth so the admin stays logged in on the default auth.
// UsersRepositoryImpl.kt (only the two methods shown need changing)

    // CREATE new user profile (admin stays logged in via secondary auth as you already set up)
    // UsersRepositoryImpl.kt
    // UsersRepository.kt (or wherever your repo impl lives)
    // ---------- THE IMPORTANT PART ----------
// CREATE new user profile (admin stays logged in via secondary auth)
    override suspend fun registerUserAsAdmin(
        adminAuth: FirebaseAuth,
        email: String,
        // password: String,          // ← keep commented (no plaintext)
        displayName: String?,
        userRole: AssignedRole,
        disabled: Boolean
    ): UserProfile {
        // 0) Generate a throwaway temp password (never stored)
        val tempPassword = buildString {
            val pool = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#\$%^&*()-_=+"
            repeat(20) { append(pool.random()) }
        }

        // 1) Create Firebase Auth user on secondary auth (does NOT touch admin session)
        val result = adminAuth.createUserWithEmailAndPassword(email.trim(), tempPassword).await()
        val fbUser = result.user ?: error("Firebase user missing after sign-up")

        // Optional: set displayName without switching sessions
        fbUser.updateProfile(
            userProfileChangeRequest { this.displayName = displayName ?: "" }
        ).await()

        // 2) Compose profile
        val profile = UserProfile(
            uid = fbUser.uid,
            email = email.trim().lowercase(),
            displayName = displayName ?: "",
            userRole = userRole,
            disabled = disabled
        )

        // 3) Write /users/{uid} with timestamps
        val data = hashMapOf(
            "uid" to profile.uid,
            "email" to profile.email,
            "displayName" to profile.displayName,
            "userRole" to profile.userRole,
            "deletedUser" to false,
            "disabled" to profile.disabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        usersRef.document(profile.uid).set(data).await()

        // 4) KISS: just send the reset email; let Google’s hosted page handle the password UI
        //    No ActionCodeSettings, no URL, Spark-friendly.
        adminAuth.sendPasswordResetEmail(profile.email.toString()).await()   // ← CHANGED

        return profile
    }

//    override suspend fun sendPasswordReset(email: String) {
//        val actionSettings = ActionCodeSettings.newBuilder()
//            .setUrl("https://yourapp.example.com/postSignIn")
//            .setAndroidPackageName("com.example.charityDept", true, null)
//            .setHandleCodeInApp(true)
//            .build()
//
//        FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim(), actionSettings).await()
//    }

    override suspend fun sendPasswordReset(email: String) {
        adminAuth.sendPasswordResetEmail(email).await()
    }

//    override suspend fun registerUserAsAdmin(
//        adminAuth: FirebaseAuth,
//        email: String,
////        password: String,
//        displayName: String?,
//        userRole: AssignedRole,
//        disabled: Boolean
//    ): UserProfile {
//        // 1) Create Firebase Auth user on secondary auth (does NOT touch admin session)
//        val result = adminAuth.createUserWithEmailAndPassword(email.trim(), password).await()
//        val fbUser = result.user ?: error("Firebase user missing after sign-up")
//
//        // 2) Compose profile
//        val profile = UserProfile(
//            uid = fbUser.uid,
//            email = email.trim().lowercase(),
//            displayName = displayName ?: "",
//            userRole = userRole,
//            disabled = disabled
//        )
//
//        // 3) Write /users/{uid} with timestamps
//        val data = hashMapOf(
//            "uid" to profile.uid,
//            "email" to profile.email,
//            "displayName" to profile.displayName,
//            "userRole" to profile.userRole,
//            "deletedUser" to false,
//            "disabled" to profile.disabled,
//            "createdAt" to FieldValue.serverTimestamp(),
//            "updatedAt" to FieldValue.serverTimestamp()
//        )
//        usersRef.document(profile.uid).set(data).await()
//
//        return profile
//    }

    // EDIT existing user profile (DO NOT touch createdAt; only bump updatedAt)
    override suspend fun upsertProfileByUid(uid: String, profile: UserProfile) {
        val update = hashMapOf(
            "uid" to uid,
            "email" to profile.email,                 // keep original email if that’s your policy
            "displayName" to (profile.displayName ?: ""),
            "userRole" to profile.userRole,
            "deletedUser" to false,
            "disabled" to profile.disabled,
            "updatedAt" to FieldValue.serverTimestamp()
            // no createdAt here: leave it as-is
        )
//        if (profile.disabled == false) {
//            update["failedAttempts"] = 0                 // ← reset on re-enable
//        }
        usersRef.document(uid).set(update, SetOptions.merge()).await()
    }

    override suspend fun promoteUser(uid: String, newAssignedRole: AssignedRole) {
        val snap = runCatching { usersRef.document(uid).get().await() }.getOrNull() ?: return
        val current = runCatching { snap.toObject(UserProfile::class.java) }.getOrNull()
        val existingAssignedRoles: List<AssignedRole> =
            (current?.userRole ?: ((snap.get("roles") as? List<*>)?.mapNotNull { it?.toString() }
                ?.mapNotNull { runCatching { AssignedRole.valueOf(it) }.getOrNull() } ?: listOf(AssignedRole.VOLUNTEER))) as List<AssignedRole>
        if (existingAssignedRoles.contains(newAssignedRole)) return
        usersRef.document(uid).update("roles", existingAssignedRoles + newAssignedRole).await()
    }


    override suspend fun softDeleteUserAndLog(
        uid: String,
        email: String?,
        reason: String?
    ) {
        val db = usersRef.firestore
        val userRef = usersRef.document(uid)
        val logRef  = db.collection("deletedUsers").document(uid)

        // Try to fetch email if not provided
        val safeEmail = email?.trim()?.lowercase() ?: runCatching {
            userRef.get().await().getString("email")?.trim()?.lowercase()
        }.getOrNull()

        // One atomic batch: (1) flag in /users, (2) write to /deletedUsers
        db.runBatch { b ->
            b.update(
                userRef,
                mapOf(
                    "deletedUser" to true,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            b.set(
                logRef,
                mapOf(
                    "uid" to uid,
                    "email" to safeEmail,
                    "reason" to (reason ?: ""),
                    "deletedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }.await()
    }



}

