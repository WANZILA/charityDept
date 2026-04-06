package com.example.charityDept.core.utils

import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await

object FirestoreSafe {
    private const val TERMINATED = "terminated"

    // Always build refs on demand (fresh after terminate()).
    private fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()
    private fun users(): CollectionReference = db().collection("users")

    /**
     * Run a CollectionReference operation. If the client was terminated, retry once using a
     * freshly built reference. Returns null if both attempts fail.
     */
    suspend fun <T> withUsers(
        op: suspend (ref: CollectionReference) -> T
    ): T? {
        return try {
            op(users())
        } catch (e: IllegalStateException) {
            if (e.message?.contains(TERMINATED, ignoreCase = true) == true) {
                runCatching { op(users()) }.getOrNull()
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * One-shot safe get for a document by PATH (builds a NEW ref each time).
     * Example path: "users/$uid"
     */
    suspend fun <T> getDocSafe(
        path: String,
        map: (DocumentSnapshot) -> T
    ): T? {
        return try {
            db().document(path).get().await().let(map)
        } catch (e: IllegalStateException) {
            if (e.message?.contains(TERMINATED, true) == true) {
                runCatching { db().document(path).get().await().let(map) }.getOrNull()
            } else null
        } catch (_: Throwable) { null }
    }

    /**
     * Safely add a snapshot listener. If the client was terminated, retry with a fresh ref.
     * Usage:
     *   val reg = FirestoreSafe.listenDoc("users/$uid") { snap, err -> ... }
     *
     * NOTE: Keep the returned ListenerRegistration and call remove() on sign out.
     */
    fun listenDoc(
        path: String,
        onEvent: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration? {
        return try {
            db().document(path).addSnapshotListener(onEvent)
        } catch (e: IllegalStateException) {
            if (e.message?.contains(TERMINATED, true) == true) {
                runCatching { db().document(path).addSnapshotListener(onEvent) }.getOrNull()
            } else null
        } catch (_: Throwable) { null }
    }

    /**
     * Same idea for queries, if you need it later.
     */
    fun listenQuery(
        queryProvider: () -> Query,
        onEvent: (QuerySnapshot?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration? {
        return try {
            queryProvider().addSnapshotListener(onEvent)
        } catch (e: IllegalStateException) {
            if (e.message?.contains(TERMINATED, true) == true) {
                runCatching { queryProvider().addSnapshotListener(onEvent) }.getOrNull()
            } else null
        } catch (_: Throwable) { null }
    }
}

