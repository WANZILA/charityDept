package com.example.charityDept.domain.repositories.online


import com.example.charityDept.data.model.LockedAccount
//import com.example.charityDept.domain.model.LockedAccount
import kotlinx.coroutines.flow.Flow
import com.example.charityDept.data.model.UserProfile
//import com.example.charityDept.domain.model.LockedAccount
import com.example.charityDept.domain.repositories.online.LockedAccountsRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
//import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton

interface LockedAccountsRepository {
    /** Stream all locked accounts (count >= 3). Admin-only per rules. */
    fun streamLockedAccounts(): Flow<List<LockedAccount>>

    /** Admin action: reset attempts for this email (set count=0). */
    suspend fun adminUnlock(emailLower: String)
}



@Singleton
class LockedAccountsRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : LockedAccountsRepository {

    private val attemptsColl get() = db.collection("authAttempts")
    private val usersColl get() = db.collection("users")

    override fun streamLockedAccounts(): Flow<List<LockedAccount>> = callbackFlow {
        val reg = attemptsColl
            .whereGreaterThanOrEqualTo("count", 3)
            .orderBy("count", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    // You can also trySend(emptyList()) before close() if you prefer
                    close(err); return@addSnapshotListener
                }
                val docs = snap ?: return@addSnapshotListener

                // Map attempts
                val base = docs.documents.map { d ->
                    LockedAccount(
                        emailLower = (d.getString("emailLower") ?: "").lowercase(),
                        count = (d.getLong("count") ?: 0).toInt(),
                        updatedAtMillis = d.getTimestamp("updatedAt")?.toDate()?.time,
                        userRole = null,
                        disabled = null
                    )
                }.filter { it.emailLower.isNotBlank() }

                // Enrich with user profiles in background
                launch {
                    val enriched = enrichWithUsers(base)
                    trySend(enriched).isSuccess
                }
            }

        awaitClose { reg.remove() }
    }

    private suspend fun enrichWithUsers(base: List<LockedAccount>): List<LockedAccount> {
        if (base.isEmpty()) return emptyList()
        val emails = base.map { it.emailLower }.distinct()

        val found = mutableMapOf<String, UserProfile>()
        for (chunk in emails.chunked(10)) {
            val snap = runCatching {
                usersColl.whereIn("email", chunk).get(Source.SERVER).await()
            }.getOrNull() ?: continue

            snap.documents.forEach { d ->
                d.toObject(UserProfile::class.java)?.let { up ->
                    up.email?.lowercase()?.let { found[it] = up }
                }
            }
        }

        return base.map { row ->
            val up = found[row.emailLower]
            row.copy(
                userRole = up?.userRole,
                disabled = up?.disabled
            )
        }
    }

    override suspend fun adminUnlock(emailLower: String) {
        val lower = emailLower.lowercase()
        val key = sha256Lower(lower)
        val ref = attemptsColl.document(key)

        val existingLower = runCatching {
            ref.get(Source.SERVER).await().getString("emailLower")
        }.getOrNull() ?: lower

        ref.set(
            mapOf(
                "count" to 0,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                // keep same emailLower (rules require it unchanged)
                "emailLower" to existingLower
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    private fun sha256Lower(emailLower: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(emailLower.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}


