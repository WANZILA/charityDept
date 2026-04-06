//package com.example.charityDept.core.Utils
//
//
//import com.google.firebase.firestore.*
//import kotlinx.coroutines.tasks.await
//
//object FirestoreSafe {
//    private const val TERMINATED = "terminated"
//
//    private fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()
//    private fun users(): CollectionReference = db().collection("users")
//
//    suspend fun <T> withUsers(
//        op: suspend (ref: CollectionReference) -> T
//    ): T? {
//        return try {
//            op(users())
//        } catch (e: IllegalStateException) {
//            if (e.message?.contains(TERMINATED, ignoreCase = true) == true) {
//                runCatching { op(users()) }.getOrNull()
//            } else null
//        } catch (_: Throwable) {
//            null
//        }
//    }
//
//    suspend fun <T> getDocSafe(
//        path: String,
//        map: (DocumentSnapshot) -> T
//    ): T? {
//        return try {
//            db().document(path).get().await().let(map)
//        } catch (e: IllegalStateException) {
//            if (e.message?.contains(TERMINATED, true) == true) {
//                runCatching { db().document(path).get().await().let(map) }.getOrNull()
//            } else null
//        } catch (_: Throwable) { null }
//    }
//
//    fun listenDoc(
//        path: String,
//        onEvent: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit
//    ): ListenerRegistration? {
//        return try {
//            db().document(path).addSnapshotListener(onEvent)
//        } catch (e: IllegalStateException) {
//            if (e.message?.contains(TERMINATED, true) == true) {
//                runCatching { db().document(path).addSnapshotListener(onEvent) }.getOrNull()
//            } else null
//        } catch (_: Throwable) { null }
//    }
//
//    fun listenQuery(
//        queryProvider: () -> Query,
//        onEvent: (QuerySnapshot?, FirebaseFirestoreException?) -> Unit
//    ): ListenerRegistration? {
//        return try {
//            queryProvider().addSnapshotListener(onEvent)
//        } catch (e: IllegalStateException) {
//            if (e.message?.contains(TERMINATED, true) == true) {
//                runCatching { queryProvider().addSnapshotListener(onEvent) }.getOrNull()
//            } else null
//        } catch (_: Throwable) { null }
//    }
//}

