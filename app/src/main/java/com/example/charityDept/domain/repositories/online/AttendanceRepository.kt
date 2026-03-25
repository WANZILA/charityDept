package com.example.charityDept.domain.repositories.online

import com.example.charityDept.core.Utils.isOfflineError
import com.example.charityDept.core.di.AttendanceRef
import com.example.charityDept.data.model.Attendance
import com.example.charityDept.data.model.AttendanceStatus
import com.example.charityDept.data.model.Child
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AttendanceSnapshot(
    val attendance: List<Attendance>,
    val fromCache: Boolean,
    val hasPendingWrites: Boolean
)

interface AttendanceRepository {
    suspend fun upsertAttendance(att: Attendance): String
    fun enqueueUpsertAttendance(att: Attendance)      // optimistic, no await
    fun streamAttendanceForEvent(eventId: String): Flow<AttendanceSnapshot>
    fun streamAttendanceForChild(childId: String): Flow<AttendanceSnapshot> // history

    /** One-shot fetch of all attendance rows for an event (cache → server). */
    suspend fun getAttendanceOnce(eventId: String): List<Attendance>

    /** Convenience: fetch a single child’s status for an event (or null if none). */
    suspend fun getStatusForChildOnce(childId: String, eventId: String): AttendanceStatus?

    /** Only PRESENT rows for an event (cache → server, status-filtered on server). */
    suspend fun getPresentAttendanceOnce(eventId: String): List<Attendance>

    /** Only ABSENT rows for an event (cache → server, status-filtered on server). */
    suspend fun getAbsentAttendanceOnce(eventId: String): List<Attendance>

    /** NEW: bulk mark all using a single Firestore WriteBatch (offline-safe). */
    fun markAllInBatchChunked(
        eventId: String,
        adminId: String,
        children: List<Child>,
        existing: Map<String, Attendance?>,
        status: AttendanceStatus,
        chunkSize: Int = 400 // <= 500; keep headroom for safety
    ): Task<Void>
}

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    @AttendanceRef private val attendanceRef: CollectionReference
) : AttendanceRepository {

    private fun idFor(eventId: String, childId: String) = "${eventId}_${childId}"

    // --- Shared helper to run the same cache → server logic for any Query ---
    private suspend fun <T> fetchListWithOfflineFallback(
        query: Query,
        mapper: (com.google.firebase.firestore.DocumentSnapshot) -> T?
    ): List<T> {
        // 1) CACHE first
        val cacheList = try {
            val cache = query.get(Source.CACHE).await()
            cache.documents.mapNotNull(mapper)
        } catch (_: Exception) { emptyList() }

        // 2) SERVER; on offline fall back to cache
        return try {
            val server = query.get(Source.SERVER).await()
            server.documents.mapNotNull(mapper)
        } catch (e: Exception) {
            if (e.isOfflineError()) cacheList else throw e
        }
    }

    /** Writes using Timestamps throughout. */
    override suspend fun upsertAttendance(att: Attendance): String {
        val id = att.attendanceId.ifBlank { idFor(att.eventId, att.childId) }
        val nowTs = Timestamp.now()

        val patch = mapOf(
            "attendanceId" to id,
            "childId"      to att.childId,
            "eventId"      to att.eventId,
            "adminId"      to att.adminId,
            "status"       to att.status.name,
            "notes"        to att.notes,
            "checkedAt"    to att.checkedAt,          // Timestamp ✅
            "updatedAt"    to nowTs,                  // Timestamp ✅
            "createdAt"    to att.createdAt           // Timestamp ✅
        )

        attendanceRef.document(id).set(patch, SetOptions.merge()).await()
        return id
    }

    /** Fire-and-forget; queued offline by Firestore SDK. */
    override fun enqueueUpsertAttendance(att: Attendance) {
        val id = att.attendanceId.ifBlank { idFor(att.eventId, att.childId) }
        val nowTs = Timestamp.now()
        attendanceRef.document(id).set(
            att.copy(
                attendanceId = id,
                updatedAt = nowTs,
                createdAt = att.createdAt
            ),
            SetOptions.merge()
        )
    }

    // ✅ FIXED to allow pending cache emissions pre-server (offline toggles show immediately)
    override fun streamAttendanceForEvent(eventId: String) = callbackFlow {
        val q = attendanceRef
            .whereEqualTo("eventId", eventId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        // Step 1: emit CACHE immediately
        try {
            val cacheSnap = q.get(Source.CACHE).await()
            val cacheList = cacheSnap.toObjects(Attendance::class.java)
                .mapIndexed { i, a -> a.copy(attendanceId = cacheSnap.documents[i].id) }

            trySend(
                AttendanceSnapshot(
                    attendance = cacheList,
                    fromCache = true,
                    hasPendingWrites = cacheSnap.metadata.hasPendingWrites()
                )
            )
        } catch (_: Exception) { /* no cache is fine */ }

        // Step 2: listener
        var sawServerOnce = false
        val reg = q.addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) { cancel("attendance stream error", err); return@addSnapshotListener }
            if (snap == null) return@addSnapshotListener

            val isCache = snap.metadata.isFromCache
            val pending = snap.metadata.hasPendingWrites()

            // Only ignore non-pending cache before first server
            if (isCache && !pending && !sawServerOnce) return@addSnapshotListener

            val list = snap.toObjects(Attendance::class.java)
                .mapIndexed { i, a -> a.copy(attendanceId = snap.documents[i].id) }

            trySend(
                AttendanceSnapshot(
                    attendance = list,
                    fromCache = isCache,
                    hasPendingWrites = pending
                )
            )

            if (!isCache) sawServerOnce = true
        }

        awaitClose { reg.remove() }
    }

    // ✅ Same fix for child history stream
    override fun streamAttendanceForChild(childId: String) = callbackFlow {
        val q = attendanceRef
            .whereEqualTo("childId", childId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        // Step 1: cache first
        try {
            val cacheSnap = q.get(Source.CACHE).await()
            val cacheList = cacheSnap.toObjects(Attendance::class.java)
                .mapIndexed { i, a -> a.copy(attendanceId = cacheSnap.documents[i].id) }

            trySend(
                AttendanceSnapshot(
                    attendance = cacheList,
                    fromCache = true,
                    hasPendingWrites = cacheSnap.metadata.hasPendingWrites()
                )
            )
        } catch (_: Exception) { /* ok */ }

        // Step 2: listener
        var sawServerOnce = false
        val reg = q.addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) { cancel("attendance stream error", err); return@addSnapshotListener }
            if (snap == null) return@addSnapshotListener

            val isCache = snap.metadata.isFromCache
            val pending = snap.metadata.hasPendingWrites()

            if (isCache && !pending && !sawServerOnce) return@addSnapshotListener

            val list = snap.toObjects(Attendance::class.java)
                .mapIndexed { i, a -> a.copy(attendanceId = snap.documents[i].id) }

            trySend(
                AttendanceSnapshot(
                    attendance = list,
                    fromCache = isCache,
                    hasPendingWrites = pending
                )
            )

            if (!isCache) sawServerOnce = true
        }

        awaitClose { reg.remove() }
    }

    override suspend fun getAttendanceOnce(eventId: String): List<Attendance> {
        val q = attendanceRef.whereEqualTo("eventId", eventId)

        val cacheList = try {
            val cache = q.get(Source.CACHE).await()
            cache.documents.mapNotNull { d ->
                d.toObject(Attendance::class.java)?.copy(attendanceId = d.id)
            }
        } catch (_: Exception) { emptyList() }

        return try {
            val server = q.get(Source.SERVER).await()
            server.documents.mapNotNull { d ->
                d.toObject(Attendance::class.java)?.copy(attendanceId = d.id)
            }
        } catch (e: Exception) {
            if (e.isOfflineError()) cacheList else throw e
        }
    }

    override suspend fun getStatusForChildOnce(childId: String, eventId: String): AttendanceStatus? {
        val q = attendanceRef
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("childId", childId)

        val cacheStatus = try {
            val cache = q.get(Source.CACHE).await()
            cache.documents.firstOrNull()?.toObject(Attendance::class.java)?.status
        } catch (_: Exception) { null }

        return try {
            val server = q.get(Source.SERVER).await()
            server.documents.firstOrNull()?.toObject(Attendance::class.java)?.status ?: cacheStatus
        } catch (_: Exception) {
            cacheStatus
        }
    }

    override suspend fun getPresentAttendanceOnce(eventId: String): List<Attendance> {
        val q = attendanceRef
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("status", AttendanceStatus.PRESENT.name)
        return fetchListWithOfflineFallback(q) { d ->
            d.toObject(Attendance::class.java)?.copy(attendanceId = d.id)
        }
    }

    override suspend fun getAbsentAttendanceOnce(eventId: String): List<Attendance> {
        val q = attendanceRef
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("status", AttendanceStatus.ABSENT.name)
        return fetchListWithOfflineFallback(q) { d ->
            d.toObject(Attendance::class.java)?.copy(attendanceId = d.id)
        }
    }

    /** NEW: Single WriteBatch to reduce N updates → 1 snapshot (offline-safe). */
    // In AttendanceRepository
    override fun markAllInBatchChunked(
        eventId: String,
        adminId: String,
        children: List<Child>,
        existing: Map<String, Attendance?>,
        status: AttendanceStatus,
        chunkSize: Int  // <= 500; keep headroom for safety
    ): Task<Void> {
        val chunks = children.chunked(chunkSize)

        // Chain the commits so they run sequentially
        var chain: Task<Void> = com.google.android.gms.tasks.Tasks.forResult(null)

        chunks.forEach { sub ->
            chain = chain.onSuccessTask {
                val now = com.google.firebase.Timestamp.now()
                val db = attendanceRef.firestore
                val batch = db.batch()

                sub.forEach { child ->
                    val current = existing[child.childId]
                    if (current?.status == status) return@forEach // skip no-ops

                    val id = "${eventId}_${child.childId}"
                    val patch = mapOf(
                        "attendanceId" to id,
                        "childId"      to child.childId,
                        "eventId"      to eventId,
                        "adminId"      to adminId,
                        "status"       to status.name,
                        "notes"        to (current?.notes ?: ""),
                        "checkedAt"    to now,
                        "createdAt"    to (current?.createdAt ?: now),
                        "updatedAt"    to now
                    )
                    batch.set(attendanceRef.document(id), patch, com.google.firebase.firestore.SetOptions.merge())
                }

                batch.commit() // returns Task<Void>
            }
        }

        return chain
    }

}

