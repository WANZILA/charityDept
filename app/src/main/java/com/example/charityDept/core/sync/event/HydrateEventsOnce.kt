//package com.example.charityDept.core.sync.event
//
//
//// <app/src/main/java/com/example/zionkids/domain/sync/HydrateChildrenOnce.kt>
//// /// CHANGED: new one-shot hydrator that pulls Firestore -> Room via ChildDao; run on login/app start to fill the table now.
//
////package com.example.charityDept.domain.sync
//
////import com.example.charityDept.core.di.ChildrenRef
//import com.example.charityDept.core.di.EventsRef
//import com.example.charityDept.data.local.dao.ChildDao
//import com.example.charityDept.data.local.dao.EventDao
//import com.example.charityDept.data.model.Event
//import com.google.firebase.Timestamp
//import com.google.firebase.firestore.CollectionReference
//import com.google.firebase.firestore.Query
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.withContext
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class HydrateEventsOnce @Inject constructor(
//    @EventsRef private val eventsRef: CollectionReference,
//    private val eventDao: EventDao
//) {
//    suspend operator fun invoke(pageSize: Int = 500, maxPages: Int = 50) = withContext(Dispatchers.IO) {
//        var page = 0
//        var lastUpdated: Timestamp? = null
//        do {
//            val q = if (lastUpdated == null) {
//                eventsRef.orderBy("updatedAt", Query.Direction.ASCENDING).limit(pageSize.toLong())
//            } else {
//                eventsRef.orderBy("updatedAt", Query.Direction.ASCENDING)
//                    .startAfter(lastUpdated!!)
//                    .limit(pageSize.toLong())
//            }
//            val snap = q.get().await()
//            val items = snap.documents.mapNotNull { it.toObject(Event::class.java) }
//            if (items.isEmpty()) break
//
//            // Server is authoritative on pull
//            eventDao.upsertAll(items.map { it.copy(isDirty = false) })
//
//            lastUpdated = items.last().updatedAt
//            page++
//        } while (items.size >= pageSize && page < maxPages)
//    }
//}
//
////
////import android.content.Context
////import androidx.hilt.work.HiltWorker
////import androidx.work.CoroutineWorker
////import androidx.work.Data
////import androidx.work.WorkerParameters
////import com.example.charityDept.core.di.EventsRef
////import com.example.charityDept.core.sync.event.resolveEvent
////import com.example.charityDept.data.local.dao.EventDao
////import com.example.charityDept.data.model.Event
////import com.google.firebase.firestore.CollectionReference
////import com.google.firebase.firestore.FirebaseFirestore
////import dagger.assisted.Assisted
////import dagger.assisted.AssistedInject
////import dagger.hilt.EntryPoint
////import dagger.hilt.InstallIn
////import dagger.hilt.android.EntryPointAccessors
////import dagger.hilt.components.SingletonComponent
////import kotlinx.coroutines.tasks.await
////import timber.log.Timber
////
////@HiltWorker
////class HydrateEventsOnceWorker @AssistedInject constructor(
////    @Assisted appContext: Context,
////    @Assisted params: WorkerParameters,
////    @EventsRef private val eventsRef: CollectionReference,
////    private val eventDao: EventDao,
////    private val firestore: FirebaseFirestore // kept for symmetry / future use
////) : CoroutineWorker(appContext, params) {
////
////    companion object {
////        // /// CHANGED: Explicit key for event id
////        const val KEY_EVENT_ID = "event_id"
////
////        // /// CHANGED: Helper to build input Data
////        fun inputFor(eventId: String): Data = Data.Builder()
////            .putString(KEY_EVENT_ID, eventId)
////            .build()
////    }
////
////    // /// CHANGED: Hilt fallback entrypoint
////    @EntryPoint
////    @InstallIn(SingletonComponent::class)
////    interface Deps {
////        @EventsRef fun eventsRef(): CollectionReference
////        fun eventDao(): EventDao
////        fun firestore(): FirebaseFirestore
////    }
////
////    // /// CHANGED: Secondary constructor for default WorkManager factory
////    constructor(appContext: Context, params: WorkerParameters) : this(
////        appContext,
////        params,
////        EntryPointAccessors.fromApplication(appContext, Deps::class.java).eventsRef(),
////        EntryPointAccessors.fromApplication(appContext, Deps::class.java).eventDao(),
////        EntryPointAccessors.fromApplication(appContext, Deps::class.java).firestore()
////    )
////
////    override suspend fun doWork(): Result {
////        val eventId = inputData.getString(KEY_EVENT_ID)
////        if (eventId.isNullOrBlank()) {
////            Timber.w("HydrateEventOnceWorker: missing event id")
////            return Result.success() // no-op
////        }
////
////        return try {
////            // 1) Fetch remote once
////            val snap = eventsRef.document(eventId).get().await()
////            val remote = snap.toObject(Event::class.java)?.copy(
////                eventId = eventId,    // normalize id from doc
////                isDirty = false       // remote truth is clean
////            )
////
////            if (remote == null) {
////                Timber.i("HydrateEventOnceWorker: event %s not found remotely", eventId)
////                return Result.success()
////            }
////
////            // 2) Fetch local once
////            val local = eventDao.getOnce(eventId)
////
////            // 3) Resolve and upsert
////            val merged = resolveEvent(local, remote)
////            eventDao.upsertOne(merged)
////
////            Result.success()
////        } catch (t: Throwable) {
////            Timber.e(t, "HydrateEventOnceWorker failed for id=%s", eventId)
////            Result.retry()
////        }
////    }
////}

