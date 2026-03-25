package com.example.charityDept.domain.repositories.online

import com.example.charityDept.core.di.ChildrenRef
import com.example.charityDept.core.di.UsersRef
import com.example.charityDept.data.model.AuthUser

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
// add this import at the top with the others:
//import com.example.charityDept.BuildConfig
import com.example.charityDept.core.di.AppVersionCode


interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<AuthUser>
    suspend fun signUp(email: String, password: String): Result<AuthUser>
    fun currentUser(): AuthUser?
    suspend fun signOut()
}
//interface AuthRepository {
//    val currentUserFlow: kotlinx.coroutines.flow.Flow<com.google.firebase.auth.FirebaseUser?>
//    suspend fun signIn(email: String, password: String): Result<FirebaseUser?>
//    suspend fun signOut()
//}

//class AuthRepositoryImpl @Inject constructor(
//    private val auth: FirebaseAuth
//) : AuthRepository {

//    override val currentUser: Flow<FirebaseUser?> = callbackFlow {
//        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
//        auth.addAuthStateListener(listener)
//        trySend(auth.currentUser)
//        awaitClose { auth.removeAuthStateListener(listener) }
//    }

//    override suspend fun signIn(email: String, password: String): Result<FirebaseUser?> =
//        runCatching {
//            auth.signInWithEmailAndPassword(email, password).await()
//            auth.currentUser
//        }
//
//    override suspend fun signOut() {
//        auth.signOut()
//    }
//}
//
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    @UsersRef private val usersRef: CollectionReference,
    @AppVersionCode private val appVersionCode: Int
) : AuthRepository {

//    override suspend fun signIn(email: String, password: String): Result<AuthUser> = runCatching {
//        auth.signInWithEmailAndPassword(email, password).await()
//        val user = auth.currentUser ?: error("No user")
//        AuthUser(uid = user.uid, email = user.email)
//    }

    override suspend fun signIn(email: String, password: String): Result<AuthUser> = runCatching {
        // Sign in
        auth.signInWithEmailAndPassword(email, password).await()
        val user = auth.currentUser ?: error("No user after sign-in")
        usersRef.document(user.uid).set(
            mapOf("clientVersionCode" to appVersionCode),   // <-- use injected value
            SetOptions.merge()
        )

        // --- CRITICAL: flush any stale custom claims from previous session ---
        runCatching { user.getIdToken(true).await() }   // force refresh claims
        runCatching { user.reload().await() }           // ensure latest profile

        // ensure Firestore is online (harmless if already enabled)
        runCatching { com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork().await() }


        AuthUser(uid = user.uid, email = user.email)


    }

    override suspend fun signUp(email: String, password: String): Result<AuthUser> = runCatching {
        // Create account
        auth.createUserWithEmailAndPassword(email, password).await()
        val user = auth.currentUser ?: error("No user after sign-up")

        // --- Make sure we start with a clean token/claims snapshot ---
        runCatching { user.getIdToken(true).await() }
        runCatching { user.reload().await() }

        AuthUser(uid = user.uid, email = user.email)
    }

    override fun currentUser(): AuthUser? =
        auth.currentUser?.let { AuthUser(uid = it.uid, email = it.email) }

    override suspend fun signOut() {
        auth.signOut()
    }
}

