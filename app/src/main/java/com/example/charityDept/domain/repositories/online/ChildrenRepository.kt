package com.example.charityDept.domain.repositories.online

import com.example.charityDept.core.di.AttendanceRef
import com.example.charityDept.core.di.ChildrenRef
import com.example.charityDept.data.mappers.toChildren
import com.example.charityDept.data.mappers.toFirestoreMapPatch
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.EducationPreference
import com.example.charityDept.data.model.Reply
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.content.Context
import javax.inject.Singleton
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
//import com.example.charityDept.core.di.ChildrenDeltaInEnqueuer
//import com.example.charityDept.data.local.projection.ChildRow
import com.example.charityDept.data.mappers.toChildOrNull
//import com.example.charityDept.data.mappers.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.io.File

data class ChildProfileImageUploadResult(
    val downloadUrl: String,
    val storagePath: String
)

data class ChildrenSnapshot(
    val children: List<Child>,
    val fromCache: Boolean,        // true = served from local cache (offline or warming up)
    val hasPendingWrites: Boolean  // true = local changes not yet synced
)

interface ChildrenRepository {
    suspend fun getChildFast(id: String): Child?

    suspend fun uploadChildProfileImage(
        childId: String,
        localPath: String,
        previousStoragePath: String?
    ): ChildProfileImageUploadResult

    suspend fun deleteChildProfileImage(
        storagePath: String
    )

    fun streamChildren(): Flow<List<Child>>
    suspend fun upsert(child: Child, isNew: Boolean): String
    suspend fun getAll(): List<Child>
    suspend fun getAllNotGraduated(): List<Child>
    fun streamAllNotGraduated(): Flow<ChildrenSnapshot>
//    suspend fun deleteChild(id: String)
// (optional) fire-and-forget offline version
   suspend fun deleteChildAndAttendances(childId: String)

    fun streamByEducationPreference(pref: EducationPreference): Flow<ChildrenSnapshot>

    fun streamByEducationPreferenceResilient(pref: EducationPreference): Flow<ChildrenSnapshot>

    suspend fun getByEducationPreference(pref: EducationPreference): List<Child>


}

@Singleton
class ChildrenRepositoryImpl @Inject constructor(
    @ChildrenRef private val childrenRef: CollectionReference,
    @AttendanceRef private val attendanceRef: CollectionReference,
    @ApplicationContext private val context: Context,
//    private val childDao: ChildDao,
//    private val syncDao: SyncStateDao,
////    private val context: Context,
//    private val childDao: ChildDao,
//    private val deltaInEnqueuer: ChildrenDeltaInEnqueuer
) : ChildrenRepository {

    private val storage = FirebaseStorage.getInstance()

    private fun Query.toChildrenSnapshotFlow(): Flow<ChildrenSnapshot> = callbackFlow {
        val reg = this@toChildrenSnapshotFlow.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val docs = snap ?: return@addSnapshotListener
            val children = docs.documents.mapNotNull { it.toObject(Child::class.java) }
            trySend(
                ChildrenSnapshot(
                    children = children,
                    fromCache = docs.metadata.isFromCache,
                    hasPendingWrites = docs.metadata.hasPendingWrites()
                )
            )
        }
        awaitClose { reg.remove() }
    }
    /* ------------ Reads ------------ */

    override suspend fun getAll(): List<Child> =
        childrenRef.get().await().toChildren()

    override suspend fun uploadChildProfileImage(
        childId: String,
        localPath: String,
        previousStoragePath: String?
    ): ChildProfileImageUploadResult {
        val file = File(localPath)
        require(file.exists()) { "Local profile image file does not exist" }

        val timestamp = System.currentTimeMillis()
        val storagePath = "children/$childId/profile/profile_$timestamp.jpg"
        val storageRef = storage.reference.child(storagePath)

        storageRef.putFile(Uri.fromFile(file)).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        if (!previousStoragePath.isNullOrBlank() && previousStoragePath != storagePath) {
            try {
                storage.reference.child(previousStoragePath).delete().await()
                Timber.d("Deleted old child profile image: $previousStoragePath")
            } catch (e: Exception) {
                Timber.w(e, "Failed to delete old child profile image: $previousStoragePath")
            }
        }

        return ChildProfileImageUploadResult(
            downloadUrl = downloadUrl,
            storagePath = storagePath
        )
    }

    override suspend fun deleteChildProfileImage(
        storagePath: String
    ) {
        if (storagePath.isBlank()) return
        storage.reference.child(storagePath).delete().await()
    }

    override fun streamChildren(): Flow<List<Child>> = callbackFlow {
        val q = childrenRef
            .orderBy("fName", ) // Timestamp
            .orderBy("lName", ) // Timestamp

        val registration = q.addSnapshotListener { snap, err ->
            if (err != null) {
                cancel("Firestore listener error", err)
                return@addSnapshotListener
            }
            val list = snap!!.toChildren()
            trySend(list).isSuccess
        }

        awaitClose { registration.remove() }
    }

    override suspend fun getChildFast(id: String): Child? {
        val doc = childrenRef.document(id)
        // 1) Try CACHE first (instant if available)
        try {
            val cache = doc.get(Source.CACHE).await()
            cache.toObject(Child::class.java)?.let { return it }
        } catch (_: Exception) {
            // cache miss — fall back to server
        }
        // 2) SERVER for fresh data
        val server = doc.get(Source.SERVER).await()
        return server.toObject(Child::class.java)
    }

    /* ------------ Create / Update ------------ */

    override suspend fun upsert(child: Child, isNew: Boolean): String {
        val id = child.childId
        require(id.isNotBlank()) { "childId required (generate one before saving)" }

        val docRef = childrenRef.document(id)

        // Start from your PATCH map (already Timestamp-based) and make sure audit/search fields are present.
        val patch = child.toFirestoreMapPatch().toMutableMap()

        // Always update updatedAt with a Timestamp
        patch["updatedAt"] = Timestamp.now()

        // Ensure graduated field is present for queries (string enum in Firestore)
        if (!patch.containsKey("graduated")) patch["graduated"] = Reply.NO.name

        // On create only: set createdAt and childId
        if (isNew) {
//            patch["createdAt"] = child.createdAt.takeIf { it != null } ?: Timestamp.now()
//            patch["createdAt"] = Timestamp.now()
            patch["childId"] = id
        }

        // Normalized name search (handy for prefix search)
        fun buildNameSearch(f: String, l: String): String =
            (f.trim() + " " + l.trim()).lowercase()
        patch["nameSearch"] = buildNameSearch(child.fName, child.lName)

        // Fire-and-sync (works offline; merges on server)
        docRef.set(patch, SetOptions.merge()).await()

        return id
    }

    /* ------------ Convenience queries ------------ */

    override suspend fun getAllNotGraduated(): List<Child> =
        childrenRef
            .whereEqualTo("graduated", Reply.NO.name)
            .orderBy("fName")
            .orderBy( "lName")
//            .orderBy("updatedAt", Query.Direction.DESCENDING) // Timestamp
//            .orderBy("createdAt", Query.Direction.DESCENDING) // Timestamp
            .get()
            .await()
            .toChildren()

    override fun streamAllNotGraduated(): Flow<ChildrenSnapshot> = callbackFlow {
        val q = childrenRef
            .whereEqualTo("graduated", Reply.NO.name)
            .orderBy("fName")
            .orderBy( "lName")
//            .orderBy("updatedAt", Query.Direction.DESCENDING)
//            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration = q.addSnapshotListener { snap, err ->
            if (err != null) {
                cancel("Firestore listener error", err)
                return@addSnapshotListener
            }
            val list = snap!!.toChildren()
            val meta = snap.metadata
            val fromCache = meta.isFromCache
            val hasLocalWrites = meta.hasPendingWrites()
            trySend(ChildrenSnapshot(list, fromCache, hasLocalWrites)).isSuccess
        }

        awaitClose { registration.remove() }
    }

    /* ------------ Deletes ------------ */



    override suspend fun deleteChildAndAttendances(childId: String) {
        val db = childrenRef.firestore

        // delete the child immediately
        childrenRef.document(childId).delete()

        // listener registration placeholder
        var registration: ListenerRegistration? = null

        registration = db.collectionGroup("attendance")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                if (snap.isEmpty) {
                    // no more attendances ⇒ stop listening
                    registration?.remove()
                    return@addSnapshotListener
                }

                val chunks = snap.documents.chunked(450)
                chunks.forEach { chunk ->
                    db.runBatch { b -> chunk.forEach { b.delete(it.reference) } }
                }
            }
    }

    override fun streamByEducationPreference(pref: EducationPreference): Flow<ChildrenSnapshot> =
        childrenRef
            .whereEqualTo("graduated", Reply.NO.name)
            .whereEqualTo("educationPreference", pref.name) // enums saved as strings
            .orderBy("fName")
            .orderBy( "lName")
            .toChildrenSnapshotFlow()

    override fun streamByEducationPreferenceResilient(pref: EducationPreference): Flow<ChildrenSnapshot> =
        combine(
            streamByEducationPreference(pref),
            streamAllNotGraduated()
        ) { filtered, all ->
            if (filtered.fromCache && filtered.children.isEmpty() && all.children.isNotEmpty()) {
                filtered.copy(
                    children = all.children.filter { it.educationPreference == pref },
                    fromCache = true // stay honest about cache origin
                )
            } else filtered
        }

    override suspend fun getByEducationPreference(pref: EducationPreference): List<Child> {
        return try {
            val snapshot = childrenRef
                .whereEqualTo("graduated", false)
                .whereEqualTo("educationPreference", pref.name) // store enums as strings
                .orderBy("fName") // optional, but consistent with your streams
                .get()
                .await()  // from kotlinx-coroutines-play-services

            snapshot.documents.mapNotNull { it.toObject(Child::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // /// ADDED — Room-backed paging flow (pageSize=50, prefetch=1, placeholders=false)
//    override fun pagingChildren(): Flow<PagingData<ChildRow>> =
//        Pager(
//            config = PagingConfig(
//                pageSize = 50,
//                prefetchDistance = 1,
//                enablePlaceholders = false,
//                initialLoadSize = 50
//            ),
//            pagingSourceFactory = { childDao.pagingAll() }
//        ).flow.flowOn(Dispatchers.IO)

    /// ADDED
//    override fun pagingChildren(): Flow<PagingData<ChildRow>> =
//        Pager(
//            config = PagingConfig(pageSize = 50, prefetchDistance = 1, enablePlaceholders = false),
//            pagingSourceFactory = { childDao.pagingAll() as PagingSource<Int, ChildRow> }
//        ).flow

    /// ADDED
//    override fun triggerDeltaInNow() {
//        val req = OneTimeWorkRequestBuilder<ChildrenDeltaInWorker>()
//            .setInputData(ChildrenDeltaInWorker.input(pageSize = 200, maxPages = 50))
//            .addTag("delta_in_children")
//            .build()
//
//        WorkManager.getInstance(context).enqueueUniqueWork(
//            "delta_in_children_once",
//            ExistingWorkPolicy.KEEP,
//            req
//        )
//    }

    /// ADDED
//    override fun triggerSimpleDeltaIn() {
//        // Lightweight: no WorkManager; runs once on IO
//        CoroutineScope(Dispatchers.IO).launch {
//            runCatching { deltaInOnce(pageSize = 200, maxPages = 50) }
//                .onFailure { Timber.e(it, "SimpleDeltaIn failed") }
//        }
//    }

    /// ADDED — core loop that pages by updatedAt,childId and writes to Room
//    private suspend fun deltaInOnce(pageSize: Int, maxPages: Int) = withContext(Dispatchers.IO) {
//        val key = "cursor_children_updatedAt"
////        val lastMillis = syncDao.getCursorMillis(key) ?: 0L
//        var cursorTs = if (lastMillis <= 0L) Timestamp(0, 1) else Timestamp(Date(lastMillis))
//        var cursorId = ""
//        var pages = 0
//        var total = 0
//
//        Timber.i("SimpleDeltaIn: start last=%d", lastMillis)
//
//        while (pages < maxPages) {
//            var q = childrenRef
//                .orderBy("updatedAt", Query.Direction.ASCENDING)
//                .orderBy("childId", Query.Direction.ASCENDING)
//
//            // On very first pass, skip null updatedAt docs (they break progress).
//            if (lastMillis <= 0L) q = q.whereGreaterThan("updatedAt", Timestamp(0, 0))
//            if (cursorId.isNotEmpty() || lastMillis > 0L) q = q.startAfter(cursorTs, cursorId)
//
//            val snap = q.limit(pageSize.toLong()).get().await()
//            if (snap.isEmpty) {
//                Timber.i("SimpleDeltaIn: done pages=%d total=%d", pages, total)
//                break
//            }
//
//            val children = snap.documents.mapNotNull { it.toChildOrNull() }
//            val valid = children.filter { it.updatedAt != null }
//            if (valid.isEmpty()) {
//                Timber.w("SimpleDeltaIn: page had only null updatedAt; stopping.")
//                break
//            }
//
//            childDao.upsertAll(valid.map { it.toEntity() })
//            total += valid.size
//
//            // Advance cursor by last valid (ASC)
//            val lastValid = valid.maxBy { it.updatedAt!! }
//            syncDao.upsertCursor(key, lastValid.updatedAt!!.toDate().time)
//            cursorTs = lastValid.updatedAt!!
//            cursorId = lastValid.childId
//
//            pages++
//            Timber.i("SimpleDeltaIn: page=%d wrote=%d cursor=%s/%s",
//                pages, valid.size, cursorTs.toDate(), cursorId)
//        }
//    }




// …inside class ChildrenRepositoryImpl…

    /// ADDED — Paging 3 over Room projection
//    override fun pagingChildren(): Flow<PagingData<ChildRow>> =
//        Pager(
//            config = PagingConfig(
//                pageSize = 50,
//                prefetchDistance = 1,
//                enablePlaceholders = false
//            ),
//            pagingSourceFactory = { childDao.pagingAll() }
//        ).flow
//
//    override suspend fun peekLocalCount(): Int = withContext(Dispatchers.IO) {
//        childDao.countAll()
//    }
//    /// ADDED
//    override suspend fun triggerDeltaInNow(pageSize: Int, maxPages: Int) {
//        deltaInEnqueuer.enqueueOnce(pageSize = pageSize, maxPages = maxPages)
//    }

    /// ADDED
//    override suspend fun verifyParityNow() {
//        deltaInEnqueuer.enqueueParityCheck()
//    }

//    override suspend fun peekLocalCount(): Int = childDao.countAll()


}


