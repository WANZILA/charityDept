package com.example.charityDept.core.Utils

import kotlinx.coroutines.tasks.await

object FireBatch {
    suspend fun <T> writeChunked(
        db: com.google.firebase.firestore.FirebaseFirestore,
        items: List<T>,
        maxPerBatch: Int = 500,
        apply: (batch: com.google.firebase.firestore.WriteBatch, item: T) -> Unit
    ) {
        if (items.isEmpty()) return
        items.chunked(maxPerBatch).forEach { chunk ->
            db.runBatch { b -> chunk.forEach { apply(b, it) } }.await()
        }
    }

    suspend fun deleteByRefsChunked(
        db: com.google.firebase.firestore.FirebaseFirestore,
        refs: List<com.google.firebase.firestore.DocumentReference>,
        maxPerBatch: Int = 500
    ) = writeChunked(db, refs, maxPerBatch) { b, ref -> b.delete(ref) }
}

