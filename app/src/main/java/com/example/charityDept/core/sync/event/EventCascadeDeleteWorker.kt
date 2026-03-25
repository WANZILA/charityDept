//package com.example.charityDept.core.sync.event
//
//// <app/src/main/java/com/example/zionkids/core/sync/EventCascadeDeleteWorker.kt>
//// /// CHANGED: New cascade-delete worker for Events.
//// /// CHANGED: Deletes the event doc, then batch-deletes all attendance docs (collectionGroup) for that eventId.
//// /// CHANGED: Uses Hilt fallback EntryPoint for default WorkManager factory scenarios.
//// /// CHANGED: Keeps input key name as KEY_CHILD_ID to avoid breaking existing calls from the scheduler.
//
////package com.example.charityDept.core.sync
//
//import android.content.Context
//import androidx.hilt.work.HiltWorker
//import androidx.work.CoroutineWorker
//import androidx.work.WorkerParameters
//import androidx.work.Data
//import com.example.charityDept.core.di.EventsRef
//import com.google.firebase.firestore.CollectionReference
//import com.google.firebase.firestore.FirebaseFirestore
//import dagger.assisted.Assisted
//import dagger.assisted.AssistedInject
//import dagger.hilt.EntryPoint
//import dagger.hilt.InstallIn
//import dagger.hilt.android.EntryPointAccessors
//import dagger.hilt.components.SingletonComponent
//import kotlinx.coroutines.tasks.await
//import timber.log.Timber
//
//@HiltWorker
//class EventCascadeDeleteWorker @AssistedInject constructor(
//    @Assisted appContext: Context,
//    @Assisted params: WorkerParameters,
//    @EventsRef private val eventsRef: CollectionReference,
//    private val firestore: FirebaseFirestore
//) : CoroutineWorker(appContext, params) {
//
//    companion object {
//        const val KEY_CHILD_ID = "child_id"      // legacy; carries eventId
//        const val KEY_EVENT_ID = "event_id"      // new; clearer
//    }
//
//    @EntryPoint
//    @InstallIn(SingletonComponent::class)
//    interface Deps {
//        @EventsRef fun eventsRef(): CollectionReference
//        fun firestore(): FirebaseFirestore
//    }
//
//    constructor(appContext: Context, params: WorkerParameters) : this(
//        appContext,
//        params,
//        EntryPointAccessors.fromApplication(appContext, Deps::class.java).eventsRef(),
//        EntryPointAccessors.fromApplication(appContext, Deps::class.java).firestore()
//    )
//
//    override suspend fun doWork(): Result {
//        // 0) Resolve ID from either key, log source
//        val idFromEvent = inputData.getString(KEY_EVENT_ID)
//        val idFromChild = inputData.getString(KEY_CHILD_ID)
//        val eventId = idFromEvent ?: idFromChild
//
//        Timber.i(
//            "EventCascadeDeleteWorker: start (event_id=%s, child_id=%s) -> resolved=%s",
//            idFromEvent, idFromChild, eventId
//        )
//
//        if (eventId.isNullOrBlank()) {
//            Timber.w("EventCascadeDeleteWorker: missing eventId (KEY_EVENT_ID/KEY_CHILD_ID)")
//            return Result.success() // no-op
//        }
//
//        // Helper for failure Data payloads
//        fun fail(e: Throwable, stage: String): Result {
//            Timber.e(e, "EventCascadeDeleteWorker: FAILED at %s (eventId=%s)", stage, eventId)
//            val d = Data.Builder()
//                .putString("stage", stage)
//                .putString("eventId", eventId)
//                .putString("errorClass", e::class.java.name)
//                .putString("errorMessage", e.message ?: "")
//                .build()
//            // Retry by default unless it’s a rules error; the catch below decides
//            return Result.failure(d)
//        }
//
//        try {
//            // 1) Hard delete the event doc (requires ADMIN per your rules)
//            Timber.i("EventCascadeDeleteWorker: deleting /events/%s", eventId)
//            try {
//                eventsRef.document(eventId).delete().await()
//            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
//                // Log Firestore status for instant diagnosis
//                Timber.e(
//                    e,
//                    "EventCascadeDeleteWorker: delete failed (code=%s, cause=%s)",
//                    e.code, e.cause?.javaClass?.simpleName
//                )
//                // For auth/rules issues, don't keep retrying endlessly—surface as failure
//                return when (e.code) {
//                    com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED,
//                    com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAUTHENTICATED,
//                    com.google.firebase.firestore.FirebaseFirestoreException.Code.INVALID_ARGUMENT -> fail(e, "delete_event_rules")
//                    else -> Result.retry()
//                }
//            }
//
//            // 1b) Server verification (no cache)
//            val exists = eventsRef.document(eventId)
//                .get(com.google.firebase.firestore.Source.SERVER)
//                .await()
//                .exists()
//            Timber.i("EventCascadeDeleteWorker: server verify /events/%s exists=%s", eventId, exists)
//            if (exists) {
//                // Unlikely (race or latency), but bail with failure so you can see it
//                return Result.failure(
//                    Data.Builder()
//                        .putString("stage", "verify_event_still_exists")
//                        .putString("eventId", eventId)
//                        .build()
//                )
//            }
//
//            // 2) Cascade delete attendances (TOP-LEVEL collection version)
//            //    If you actually use subcollections, swap block for collectionGroup version below.
//            val pageSize = 450
//            var totalDeleted = 0
//            Timber.i("EventCascadeDeleteWorker: cascading in collection='attendances' for eventId=%s", eventId)
//            while (true) {
//                val snap = firestore.collection("attendances")
//                    .whereEqualTo("eventId", eventId)
//                    .limit(pageSize.toLong())
//                    .get()
//                    .await()
//
//                if (snap.isEmpty) break
//
//                try {
//                    firestore.runBatch { b ->
//                        snap.documents.forEach { b.delete(it.reference) }
//                    }.await()
//                } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
//                    Timber.e(
//                        e,
//                        "EventCascadeDeleteWorker: batch delete failed (code=%s) size=%d",
//                        e.code, snap.size()
//                    )
//                    return when (e.code) {
//                        com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED,
//                        com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAUTHENTICATED -> fail(e, "cascade_rules")
//                        else -> Result.retry()
//                    }
//                }
//
//                totalDeleted += snap.size()
//                Timber.i("EventCascadeDeleteWorker: deleted %d attendances (running total=%d)", snap.size(), totalDeleted)
//                if (snap.size() < pageSize) break
//            }
//            Timber.i("EventCascadeDeleteWorker: cascade complete (eventId=%s, totalAttendancesDeleted=%d)", eventId, totalDeleted)
//
//            return Result.success()
//        } catch (t: Throwable) {
//            // Any unexpected throwable
//            Timber.e(t, "EventCascadeDeleteWorker: unexpected error")
//            return Result.retry()
//        }
//    }
//
//
////    override suspend fun doWork(): Result {
////        val eventId = inputData.getString(KEY_EVENT_ID)
////            ?: inputData.getString(KEY_CHILD_ID)
////
////        if (eventId.isNullOrBlank()) {
////            Timber.w("EventCascadeDeleteWorker: missing eventId (KEY_EVENT_ID/KEY_CHILD_ID)")
////            return Result.success() // no-op
////        }
////
////        return try {
////            Timber.i("EventCascadeDeleteWorker: deleting event=%s", eventId)
////
////            // 1) Delete the event doc (hard delete; requires ADMIN by rules)
////            eventsRef.document(eventId).delete().await()
////            Timber.i("EventCascadeDeleteWorker: event doc deleted")
////
////            // 2) Cascade: delete all attendance docs for this eventId
////            //    Use the RIGHT collection name. Choose ONE of the following blocks:
////
////            // --- A) Top-level collection: /attendances (most common in your codebase)
////            val pageSize = 450
////            while (true) {
////                val snap = firestore
////                    .collection("attendances")
////                    .whereEqualTo("eventId", eventId)
////                    .limit(pageSize.toLong())
////                    .get()
////                    .await()
////
////                if (snap.isEmpty) break
////
////                firestore.runBatch { b ->
////                    snap.documents.forEach { b.delete(it.reference) }
////                }.await()
////
////                Timber.i("EventCascadeDeleteWorker: deleted %d attendances (running)", snap.size())
////                if (snap.size() < pageSize) break
////            }
////
////            // --- B) If you actually use repeated subcollections named "attendances":
////            // val pageSize = 450
////            // while (true) {
////            //     val snap = firestore
////            //         .collectionGroup("attendances")
////            //         .whereEqualTo("eventId", eventId)
////            //         .limit(pageSize.toLong())
////            //         .get()
////            //         .await()
////            //     if (snap.isEmpty) break
////            //     firestore.runBatch { b -> snap.documents.forEach { b.delete(it.reference) } }.await()
////            //     Timber.i("EventCascadeDeleteWorker: deleted %d attendances (running)", snap.size())
////            //     if (snap.size() < pageSize) break
////            // }
////
////            Timber.i("EventCascadeDeleteWorker: cascade complete for event=%s", eventId)
////            Result.success()
////        } catch (t: Throwable) {
////            Timber.e(t, "EventCascadeDeleteWorker failed for eventId=%s", eventId)
////            Result.retry()
////        }
////    }
//}
//
////@HiltWorker
////class EventCascadeDeleteWorker @AssistedInject constructor(
////    @Assisted appContext: Context,
////    @Assisted params: WorkerParameters,
////    @EventsRef private val eventsRef: CollectionReference,
////    private val firestore: FirebaseFirestore
////) : CoroutineWorker(appContext, params)
////{
////
////    companion object {
////        // /// CHANGED: Keep legacy key name to avoid breaking callers (scheduler passes childId).
////        const val KEY_CHILD_ID = "child_id"   // carries the eventId value
////    }
////
////    // /// CHANGED: Hilt fallback entrypoint for default WorkManager factory.
////    @EntryPoint
////    @InstallIn(SingletonComponent::class)
////    interface Deps {
////        @EventsRef fun eventsRef(): CollectionReference
////        fun firestore(): FirebaseFirestore
////    }
////
////    // /// CHANGED: Secondary ctor for non-Hilt WM factory paths.
////    constructor(appContext: Context, params: WorkerParameters) : this(
////        appContext,
////        params,
////        EntryPointAccessors.fromApplication(appContext, Deps::class.java).eventsRef(),
////        EntryPointAccessors.fromApplication(appContext, Deps::class.java).firestore()
////    )
////
////    override suspend fun doWork(): Result {
////        val eventId = inputData.getString(KEY_CHILD_ID)
////        if (eventId.isNullOrBlank()) {
////            Timber.w("EventCascadeDeleteWorker: missing eventId in KEY_CHILD_ID")
////            return Result.success() // nothing to do; treat as no-op
////        }
////
////        return try {
////            // 1) Delete the event document (idempotent).
////            eventsRef.document(eventId).delete().await()
////
////            // 2) Cascade delete all attendance docs with this eventId (collectionGroup).
////            //    We chunk in ≤450 to stay under Firestore batch limit with headroom.
////            val pageSize = 450
////            while (true) {
////                val snap = firestore.collectionGroup("attendance")
////                    .whereEqualTo("eventId", eventId)
////                    .limit(pageSize.toLong())
////                    .get()
////                    .await()
////
////                if (snap.isEmpty) break
////
////                firestore.runBatch { b ->
////                    snap.documents.forEach { b.delete(it.reference) }
////                }.await()
////
////                if (snap.size() < pageSize) break
////            }
////
////            Result.success()
////        } catch (t: Throwable) {
////            Timber.e(t, "EventCascadeDeleteWorker failed for eventId=%s", eventId)
////            Result.retry()
////        }
////    }
////}

