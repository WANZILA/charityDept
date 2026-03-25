// package com.example.charityDept.presentation.viewModels
package com.example.charityDept.presentation.viewModels.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.core.Utils.Network.NetworkMonitorUtil
import com.example.charityDept.core.di.AppVersionCode
import com.example.charityDept.core.di.UsersRef
import com.example.charityDept.core.sync.ChildrenSyncScheduler
import com.example.charityDept.core.sync.attendance.AttendanceSyncScheduler
import com.example.charityDept.core.sync.event.EventSyncScheduler
import com.example.charityDept.data.model.AssignedRole
import com.example.charityDept.data.model.UserProfile
import com.example.charityDept.domain.repositories.online.AuthRepository
import com.example.charityDept.domain.repositories.online.UsersRepository
import com.example.charityDept.domain.repositories.online.UsersSnapshot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withTimeoutOrNull

// Attempts helpers
import java.security.MessageDigest


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val users: UsersRepository,
    private val networkMonitor: NetworkMonitorUtil,
    @UsersRef private val usersRef: CollectionReference,
    @AppVersionCode private val appVersionCode: Int,
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val appContext: android.content.Context,
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    private var usersJob: Job? = null

    private val fa: FirebaseAuth = FirebaseAuth.getInstance()
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val u = firebaseAuth.currentUser
        if (u == null) {
            // Signed out
            usersJob?.cancel(); usersJob = null
            _ui.update {
                it.copy(
                    isLoggedIn = false,
                    profile = null,
                    assignedRoles = emptyList(),
                    error = null,
                    loading = false
                )
            }
            _lastKnownDisplayName.value = ""
        } else {
            // Signed in or switched account
            viewModelScope.launch {
                // Make claims fresh if we have network (prevents role leakage)
//                runCatching { u.getIdToken(true).await() }

                viewModelScope.launch { runCatching { withTimeoutOrNull(800) { u.getIdToken(true).await() } } }

                val online = networkMonitor.onlineNow()
//                val profile = runCatching {
//                    if (online) {
//                        usersRef.document(u.uid)
//                            .get(Source.SERVER)
//                            .await()
//                            .toObject(UserProfile::class.java)
//                    } else {
//                        usersRef.document(u.uid)
//                            .get(Source.CACHE)
//                            .await()
//                            .toObject(UserProfile::class.java)
//                    }
//                }.getOrNull()

                var profile = runCatching {
                    usersRef.document(u.uid).get(Source.CACHE).await().toObject(UserProfile::class.java)
                }.getOrNull()

//                if (profile != null && !profile.disabled) {
//                    _ui.update {
//                        it.copy(
//                            isLoggedIn = true,
//                            profile = profile,
//                            assignedRoles = listOf(profile.userRole),
//                            error = null,
//                            loading = false
//                        )
//                    }
//                    updateLastKnownNameFrom(profile)
//
//                    usersJob?.cancel()
//                    maybeStartUsersDirectory(profile.userRole)
//                } else {
//                    _ui.update {
//                        it.copy(
//                            isLoggedIn = false,
//                            profile = null,
//                            assignedRoles = emptyList(),
//                            error = if (online)
//                                "No profile (or disabled). Contact an admin."
//                            else
//                                "Offline: open once while online to finish first login.",
//                            loading = false
//                        )
//                    }
//                    _lastKnownDisplayName.value = ""
//                }
                // Update UI optimistically if we have cache
                if (profile != null && !profile.disabled) {
                    _ui.update {
                        it.copy(
                            isLoggedIn = true,
                            profile = profile,
                            assignedRoles = listOf(profile.userRole),
                            error = null,
                            loading = false
                        )
                    }
                    // Keep the version fresh when we detect a valid signed-in user (non-blocking)
                    FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                        if (networkMonitor.onlineNow()) {
                            viewModelScope.launch { runCatching { upsertClientVersion(uid) } }
                        }
                    }

                    updateLastKnownNameFrom(profile)
                }

                // 2) then refresh from SERVER in background, but don't block UI
                if (networkMonitor.onlineNow()) {
                    viewModelScope.launch {
                        val fresh = runCatching {
                            usersRef.document(u.uid).get(Source.SERVER).await().toObject(UserProfile::class.java)
                        }.getOrNull()

                        if (fresh != null && !fresh.disabled) {
                            _ui.update {
                                it.copy(
                                    isLoggedIn = true,
                                    profile = fresh,
                                    assignedRoles = listOf(fresh.userRole),
                                    error = null,
                                    loading = false
                                )
                            }
                            updateLastKnownNameFrom(fresh)
                            usersJob?.cancel()
                            maybeStartUsersDirectory(fresh.userRole)
                        }
                    }
                } else {
                    // offline branch remains same as you had
                }
            }
        }
    }

    // Creates /users/{uid} with a minimal profile if missing.
// Makes rules pass isEnabled() + hasRole("VOLUNTEER") on first login.
    // Creates /users/{uid} with a minimal profile if missing.
// Safe to call even when offline (wrapped in runCatching; write will just no-op offline).
    // Creates /users/{uid} if missing, or updates if exists.
// No pre-read; this is a single write that the client can queue offline.
    // Only CREATE when missing; never overwrite existing userRole.
    private suspend fun ensureUserDocExistsIfMissing() {
        val fbUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = fbUser.uid

        // 1) Fast check: CACHE
        val cached = runCatching { usersRef.document(uid).get(Source.CACHE).await() }.getOrNull()
        if (cached?.exists() == true) return

        // 2) Confirm with SERVER (short timeout so we don't block UX)
        val server = runCatching {
            withTimeoutOrNull(1000) { usersRef.document(uid).get(Source.SERVER).await() }
        }.getOrNull()
        if (server?.exists() == true) return

        // 3) Create minimal profile (first login)
        runCatching {
            usersRef.document(uid).set(
                mapOf(
                    "userId" to uid,
                    "email" to (fbUser.email ?: ""),
                    "displayName" to (fbUser.displayName ?: ""),
                    "userRole" to AssignedRole.VOLUNTEER.name, // default only on create
                    "disabled" to false,
                    "clientVersionCode" to appVersionCode,  // <-- ADDED
                    "deletedUser" to false
                ),
                SetOptions.merge()
            ).await()
        }
    }
    private suspend fun upsertClientVersion(uid: String) {
        runCatching {
            usersRef.document(uid)
                .set(mapOf("clientVersionCode" to appVersionCode), SetOptions.merge())
                .await()
        }
    }

    /*** Attempts section **/
    // ======== AUTH ATTEMPTS (free-plan, owner or admin can reset) ========

// 1) Collection ref
    private val attemptsColl
        get() = FirebaseFirestore.getInstance().collection("authAttempts")

    private fun emailLower(email: String) = email.trim().lowercase()

    /** True if 3+ failures recorded (SERVER read) */
    private suspend fun isHardLocked(email: String): Boolean {
        val key = emailKey(email)
        val snap = withTimeoutOrNull(800) {
            attemptsColl.document(key).get(Source.SERVER).await()
        } ?: return false // if server is slow, don't block sign-in

        val count = snap.getLong("count") ?: 0L
        return count >= 3
    }


    /** Increment attempts (+1). Returns true if threshold (>=3) is reached now. */
    private suspend fun bumpAttemptsAndCheckLocked(email: String): Boolean {
        val key = emailKey(email)
        val db = FirebaseFirestore.getInstance()
        val ref = attemptsColl.document(key)

        val newCount = db.runTransaction { tx ->
            val snap = tx.get(ref)
            val nowLower = emailLower(email)

            if (!snap.exists()) {
                // First failure → create with count=1
                tx.set(
                    ref,
                    mapOf(
                        "count" to 1,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "emailLower" to nowLower
                    ),
                    SetOptions.merge()
                )
                1
            } else {
                val current = (snap.getLong("count") ?: 0L).toInt()
                val existingLower = snap.getString("emailLower") ?: nowLower
                // Increment path (must keep emailLower unchanged per rules)
                tx.set(
                    ref,
                    mapOf(
                        "count" to current + 1,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "emailLower" to existingLower
                    ),
                    SetOptions.merge()
                )
                current + 1
            }
        }.await()

        return newCount >= 3
    }

    /** Reset attempts to 0 after a successful login by the same user (owner reset). */
    private suspend fun resetAttemptsOwner(email: String) {
        val key = emailKey(email)
        val ref = attemptsColl.document(key)

        // Read existing emailLower so we don't change it (rules require it to remain the same)
        val existingLower = runCatching { ref.get(Source.SERVER).await().getString("emailLower") }
            .getOrNull() ?: emailLower(email)

        runCatching {
            ref.set(
                mapOf(
                    "count" to 0,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "emailLower" to existingLower
                ),
                SetOptions.merge()
            ).await()
        }
    }
/**end of it */

    init {
        fa.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        fa.removeAuthStateListener(authListener)
        usersJob?.cancel()
    }

    // --- Simple form bindings
    fun onEmailChange(v: String) { _ui.update { it.copy(email = v, error = null) } }
    fun onPasswordChange(v: String) { _ui.update { it.copy(password = v, error = null) } }

    // --- Friendly display name cache (exposed to UI)
    private val _lastKnownDisplayName = MutableStateFlow("")
    val lastKnownDisplayName: StateFlow<String> = _lastKnownDisplayName.asStateFlow()

    private fun updateLastKnownNameFrom(p: UserProfile?) {
        val n = when {
            p?.displayName?.isNotBlank() == true -> p.displayName!!
            p?.email?.isNotBlank() == true -> {
                val local = p.email!!.substringBefore('@')
                local.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
            }
            else -> ""
        }
        if (n.isNotBlank()) _lastKnownDisplayName.value = n
    }

    /**
     * STRICT behavior:
     * - If ONLINE: normal sign-in, but block immediately if 3+ failures recorded.
     * - If OFFLINE: auto-complete sign-in from CACHE (if cached user + cached profile).
     */
//    fun signIn(onSuccess: () -> Unit) = viewModelScope.launch {
//        _ui.update { it.copy(loading = true, error = null) }
//
//        val email = ui.value.email.trim()
//        val pass  = ui.value.password
//        val online = networkMonitor.onlineNow()
//
//        if (!online) {
//            // Auto login from cache (no network)
//            offlineAutoSignIn()
//            _ui.update { it.copy(loading = false) }
//            if (ui.value.isLoggedIn) onSuccess()
//            return@launch
//        }
//
////        // ONLINE: hard-lock pre-check (3+ failures)
////        val hardLocked = runCatching { isHardLocked(email) }.getOrDefault(false)
////        if (hardLocked) {
////            _ui.update { it.copy(loading = false, error = "Account locked after multiple failed attempts. Contact an admin.") }
////            return@launch
////        }
//
//        auth.signIn(email, pass)
//            .onSuccess {
//               // Ensure the profile exists so rules (isEnabled + hasRole) pass for volunteers
//                runCatching { ensureUserDocExistsIfMissing() }
//                // Success → clear attempts
////                runCatching { clearAttempts(email) }
//                _ui.update { it.copy(loading = false) }
//                onSuccess() // AuthStateListener will load SERVER profile and update UI
//            }
//            .onFailure { e ->
//                // Failure → bump attempts; if threshold reached, also disable user profile (best-effort)
////                val lockedNow = runCatching { bumpAttemptsAndCheckLocked(email) }.getOrDefault(false)
////                if (lockedNow) {
////                    // Try to mark their profile disabled for admin visibility
////                    runCatching {
////                        usersRef.whereEqualTo("email", email.trim().lowercase())
////                            .limit(1).get().await()
////                            .documents.firstOrNull()?.reference
////                            ?.update("disabled", true)?.await()
////                    }
////                }
////                val msg = if (lockedNow)
////                    "Too many failed attempts. Account locked. Contact an admin."
////                else
////                    (e.message ?: "Sign in failed")
////                _ui.update { s -> s.copy(loading = false, error = msg) }
//            }
//    }

    fun signIn(onSuccess: () -> Unit) = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }

        val email = ui.value.email.trim()
        val pass  = ui.value.password
        val online = networkMonitor.onlineNow()

        if (!online) {
            // Auto login from cache (no network)
            offlineAutoSignIn()
            _ui.update { it.copy(loading = false) }
            if (ui.value.isLoggedIn) onSuccess()
            return@launch
        }

        // ONLINE: hard-lock pre-check (3+ failures)
        val hardLocked = runCatching { isHardLocked(email) }.getOrDefault(false)
        if (hardLocked) {
            _ui.update { it.copy(loading = false, error = "Account locked after multiple failed attempts. Contact an admin or sign in successfully to reset.") }
            return@launch
        }

        auth.signIn(email, pass)
            .onSuccess {
                // Kick off background tasks, don't await them
                viewModelScope.launch { runCatching { ensureUserDocExistsIfMissing() } }
                viewModelScope.launch { runCatching { resetAttemptsOwner(email) } }

                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    viewModelScope.launch { runCatching { upsertClientVersion(uid) } }
                }
                _ui.update { it.copy(loading = false) }
//                ChildrenSyncScheduler.enqueuePeriodicPush(context)
//                ChildrenSyncScheduler.enqueuePushNow(appContext)
//                EventSyncScheduler.enqueuePushNow(appContext)
//                AttendanceSyncScheduler.enqueuePushNow(appContext)
//                AttendanceSyncScheduler.enqueuePeriodicPush(appContext)


                onSuccess()
            }

            .onFailure { e ->
                // Failure → bump attempts; show stronger message when threshold reached
                val lockedNow = runCatching { bumpAttemptsAndCheckLocked(email) }.getOrDefault(false)
                val msg = if (lockedNow)
                    "Too many failed attempts. Account locked. Contact an admin or try again later and sign in successfully to reset."
                else
                    (e.message ?: "Sign in failed")
                _ui.update { s -> s.copy(loading = false, error = msg) }
            }
    }

    fun signOut(onSuccess: () -> Unit = {}) = viewModelScope.launch {
        usersJob?.cancel(); usersJob = null
        runCatching { auth.signOut() }
        _lastKnownDisplayName.value = ""
        onSuccess() // Listener flips UI to logged-out
    }

    // --- Admin-only stream gate (don’t start when offline)
    private fun maybeStartUsersDirectory(role: AssignedRole) {
        val isAllowed = when (role) {
            AssignedRole.ADMIN, AssignedRole.LEADER -> true
            else -> false
        }
        if (!isAllowed || !networkMonitor.onlineNow()) {
            usersJob?.cancel(); usersJob = null
            return
        }

        usersJob?.cancel()
        usersJob = viewModelScope.launch {
            users.streamAllUserSnapshot()
                .catch { e ->
                    // If rules block this for some reason, we swallow and don't crash the UI
                    // Optional: log the code for quick diagnosis
                    // if (e is FirebaseFirestoreException) Log.e("AuthVM", "code=${e.code}", e)
                    emit(
                        UsersSnapshot(
                            users = emptyList(),
                            fromCache = true,
                            hasPendingWrites = false
                        )
                    )
                }
                .collect { snap: UsersSnapshot ->
                    // TODO: update admin directory UI/state if needed
                }
        }
    }

//    private fun maybeStartUsersDirectory(roleStrings: List<String>) {
//        // If you now use AssignedRole names, the leader is "LEADER" (not "LEAD")
//        val isAllowed = roleStrings.contains("ADMIN") || roleStrings.contains("LEADER")
//        if (!isAllowed) { usersJob?.cancel(); return }
//        if (!networkMonitor.onlineNow()) { usersJob?.cancel(); return }
//
//        usersJob?.cancel()
//        usersJob = viewModelScope.launch {
//            users.streamAllUserSnapshot()
//                // IMPORTANT: emit an empty UsersSnapshot, not a List
//                .catch {
//                    // adjust ctor if your data class has more fields
//                    emit(
//                        UsersSnapshot(
//                            users = emptyList(),
//                            fromCache = true,
//                            hasPendingWrites = false
//                        )
//                    )
//                    // or: emit(UsersSnapshot(emptyList()))
//                }
//                .collect { _: UsersSnapshot ->
//                    // update your UI/state here if needed
//                }
//        }
//    }

//    private fun maybeStartUsersDirectory(roleStrings: List<String>) {
//        val isAllowed = roleStrings.contains("ADMIN") || roleStrings.contains("LEAD")
//        if (!isAllowed) { usersJob?.cancel(); return }
//        if (!networkMonitor.onlineNow()) { usersJob?.cancel(); return } // avoid snapshot while offline
//
//        usersJob?.cancel()
//        usersJob = viewModelScope.launch {
//            users.streamAllUserSnapshot()
//                .catch { emit(UsersSnapshot(users = emptyList())) }
//                .collect { /* update admin directory UI if you expose it */ }
//        }
//    }

    // --- OFFLINE auto-login helper
    private suspend fun offlineAutoSignIn() {
        val u = FirebaseAuth.getInstance().currentUser
        if (u == null) {
            _ui.update {
                it.copy(
                    isLoggedIn = false,
                    profile = null,
                    assignedRoles = emptyList(),
                    error = "Offline and no previous session. Connect to sign in."
                )
            }
            _lastKnownDisplayName.value = ""
            return
        }

        val cachedProfile = runCatching {
            usersRef.document(u.uid)
                .get(Source.CACHE)
                .await()
                .toObject(UserProfile::class.java)
        }.getOrNull()

        if (cachedProfile != null && !cachedProfile.disabled) {
            _ui.update {
                it.copy(
                    isLoggedIn = true,
                    profile = cachedProfile,
                    assignedRoles = listOf(cachedProfile.userRole),
                    error = null
                )
            }

            // Ensure the profile exists so rules (isEnabled + hasRole) pass for volunteers
            // (If offline, this safely no-ops; will succeed next time online.)
            runCatching { ensureUserDocExistsIfMissing() }
//            ChildrenSyncScheduler.enqueuePushNow(appContext)
//            EventSyncScheduler.enqueuePushNow(appContext)
//            AttendanceSyncScheduler.enqueuePushNow(appContext)

            updateLastKnownNameFrom(cachedProfile)
            usersJob?.cancel(); usersJob = null // don’t run admin stream offline
        } else {
            _ui.update {
                it.copy(
                    isLoggedIn = false,
                    profile = null,
                    assignedRoles = emptyList(),
                    error = "Offline: open once while online to finish first login."
                )
            }
            _lastKnownDisplayName.value = ""
        }
    }

//    private suspend fun offlineAutoSignIn() {
//        val u = FirebaseAuth.getInstance().currentUser
//        if (u == null) {
//            _ui.update {
//                it.copy(
//                    isLoggedIn = false,
//                    profile = null,
//                    assignedRoles = emptyList(),
//                    error = "Offline and no previous session. Connect to sign in."
//                )
//            }
//            _lastKnownDisplayName.value = ""
//            return
//        }
//
//        val cachedProfile = runCatching {
//            usersRef.document(u.uid)
//                .get(Source.CACHE)
//                .await()
//                .toObject(UserProfile::class.java)
//        }.getOrNull()
//
//        if (cachedProfile != null && !cachedProfile.disabled) {
//            _ui.update {
//                it.copy(
//                    isLoggedIn = true,
//                    profile = cachedProfile,
//                    assignedRoles = listOf(cachedProfile.userRole),
//                    error = null
//                )
//            }
//            //Ensure the profile exists so rules (isEnabled + hasRole) pass for volunteers
//            runCatching { ensureUserDocExistsIfMissing() }
//            updateLastKnownNameFrom(cachedProfile)
//            usersJob?.cancel(); usersJob = null // don’t run admin stream offline
//        } else {
//            _ui.update {
//                it.copy(
//                    isLoggedIn = false,
//                    profile = null,
//                    assignedRoles = emptyList(),
//                    error = "Offline: open once while online to finish first login."
//                )
//            }
//            _lastKnownDisplayName.value = ""
//        }
//    }

    // ======== SINGLE attempts system (authAttempts, hashed email) ========

//    private val attemptsColl
//        get() = FirebaseFirestore.getInstance().collection("authAttempts")

    private fun emailKey(email: String): String {
        val e = email.trim().lowercase()
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(e.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** True if 3+ failures already recorded. */
//    private suspend fun isHardLocked(email: String): Boolean {
//        val key = emailKey(email)
//        val snap = runCatching { attemptsColl.document(key).get(Source.SERVER).await() }.getOrNull()
//        val count = snap?.getLong("count") ?: 0L
//        return count >= 3
//    }

    /** Increment attempts; return true when threshold (>=3) is reached now. */
//    private suspend fun bumpAttemptsAndCheckLocked(email: String): Boolean {
//        val key = emailKey(email)
//        val newCount = FirebaseFirestore.getInstance().runTransaction { tx ->
//            val ref = attemptsColl.document(key)
//            val snap = tx.get(ref)
//            val current = (snap.getLong("count") ?: 0L).toInt()
//            val next = current + 1
//            tx.set(
//                ref,
//                mapOf(
//                    "count" to next,
//                    "updatedAt" to FieldValue.serverTimestamp()
//                ),
//                SetOptions.merge()
//            )
//            next
//        }.await()
//        return newCount >= 3
//    }

    /** Clear attempts after successful sign-in. */
//    private suspend fun clearAttempts(email: String) {
//        val key = emailKey(email)
//        runCatching {
//            attemptsColl.document(key)
//                .set(
//                    mapOf(
//                        "count" to 0,
//                        "updatedAt" to FieldValue.serverTimestamp()
//                    ),
//                    SetOptions.merge()
//                )
//                .await()
//        }
//    }
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val profile: UserProfile? = null,
    val assignedRoles: List<AssignedRole> = emptyList(),
) {
    val perms: UserPermissions
        get() = permissionsForRoles(assignedRoles)

    // ✅ role helpers (use these everywhere)
    val isAdmin: Boolean
        get() = assignedRoles.contains(AssignedRole.ADMIN)

    val isLeader: Boolean
        get() = assignedRoles.contains(AssignedRole.LEADER)

    val isVolunteer: Boolean
        get() = assignedRoles.contains(AssignedRole.VOLUNTEER)

    val isViewer: Boolean
        get() = assignedRoles.contains(AssignedRole.VIEWER)

    val isSponsor: Boolean
        get() = assignedRoles.contains(AssignedRole.SPONSOR)

    val canSeeAdminScreens: Boolean
        get() = isAdmin || isLeader
}

//data class AuthUiState(
//    val email: String = "",
//    val password: String = "",
//    val loading: Boolean = false,
//    val error: String? = null,
//    val isLoggedIn: Boolean = false,
//    val profile: UserProfile? = null,
//    val assignedRoles: List<AssignedRole> = emptyList(),
//) {
//    val perms: UserPermissions
//        get() = permissionsForRoles(assignedRoles)
//}

