package com.example.charityDept.domain.repositories.offline


import android.content.Context
import com.example.charityDept.core.di.AttendanceRef
import com.example.charityDept.core.sync.SyncCoordinatorScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.example.charityDept.data.model.Attendance
import com.example.charityDept.data.model.AttendanceStatus
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.local.dao.AttendanceDao
import com.example.charityDept.core.sync.attendance.AttendanceSyncScheduler // call enqueuePushNow after local writes
import com.example.charityDept.data.local.projection.EventFrequentAttendeeRow
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar

// Keep the same DTO so UI doesn’t change.
data class AttendanceSnapshot(
    val attendance: List<Attendance>,
    val fromCache: Boolean,
    val hasPendingWrites: Boolean
)

data class EligibleCounts(
    val totalEligible: Int,
    val presentEligible: Int
)

interface OfflineAttendanceRepository {

//    suspend fun getStatusForChildOnce(childId: String, eventId: String): AttendanceStatus?
    suspend fun upsertAttendance(att: Attendance): String
    fun enqueueUpsertAttendance(att: Attendance)
    fun streamAttendanceForEvent(eventId: String): Flow<AttendanceSnapshot>
    fun streamAttendanceForChild(childId: String): Flow<AttendanceSnapshot>
    suspend fun getAttendanceOnce(eventId: String): List<Attendance>
    suspend fun getStatusForChildOnce(childId: String, eventId: String): AttendanceStatus?
    suspend fun getPresentAttendanceOnce(eventId: String): List<Attendance>
    suspend fun getAbsentAttendanceOnce(eventId: String): List<Attendance>
    fun markAllInBatchChunked(
        eventId: String,
        adminId: String,
        children: List<Child>,
        existing: Map<String, Attendance?>,
        status: AttendanceStatus,
        chunkSize: Int = 400
    ): Task<Void>

    // 👇 NEW: pull remote rows for a single event into Room (marks clean)
    suspend fun hydrateEvent(eventId: String)

    suspend fun eligibleCountsForEvent(
        eventId: String,
        eventDate: Timestamp
    ): EligibleCounts

    fun observeFrequentAttendeesForEvents(eventIds: List<String>): Flow<List<EventFrequentAttendeeRow>>
}

/**
 * Offline-first implementation:
 * - Reads come from Room.
 * - Writes go to Room (markDirty + version bump); worker will push.
 * - After any local change, we nudge AttendanceSyncScheduler.enqueuePushNow().
 *
 * DAO expectations (mirror your EventDao patterns):
 *   - suspend fun upsert(a: Attendance)
 *   - suspend fun upsertAll(list: List<Attendance>)
 *   - suspend fun getOnce(id: String): Attendance?
 *   - suspend fun getByEvent(eventId: String): List<Attendance>
 *   - suspend fun getByEventAndStatus(eventId: String, status: AttendanceStatus): List<Attendance>
 *   - suspend fun getForChild(childId: String): List<Attendance>
 *   - fun observeByEvent(eventId: String): Flow<List<Attendance>>
 *   - fun observeByChild(childId: String): Flow<List<Attendance>>
 *   - suspend fun markDirty(id: String, now: Timestamp)
 */
@Singleton
class OfflineAttendanceRepositoryImpl @Inject constructor(
    private val dao: AttendanceDao,
    @ApplicationContext private val appContext: Context,
    @AttendanceRef private val attendanceRef: CollectionReference,

    ) : OfflineAttendanceRepository {

    // inside OfflineAttendanceRepositoryImpl
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun idFor(eventId: String, childId: String) = "${eventId}_${childId}"



    override suspend fun upsertAttendance(att: Attendance): String {
        val id = att.attendanceId.ifBlank { idFor(att.eventId, att.childId) }
        val now = Timestamp.now()

        val current = dao.getOnce(id)
        val nextVersion = (current?.version ?: 0L) + 1L

        // Preserve createdAt if present; otherwise keep att.createdAt
        val createdAt = current?.createdAt ?: att.createdAt

        val toSave = att.copy(
            attendanceId = id,
            createdAt = createdAt,
            updatedAt = now,
            checkedAt = att.checkedAt, // caller’s check time
            version = nextVersion,
            isDirty = true
        )

        dao.upsert(toSave)
        AttendanceSyncScheduler.enqueuePushNow(appContext) // 🚀 push soon
        return id
    }



    override fun enqueueUpsertAttendance(att: Attendance) {
        val id  = att.attendanceId.ifBlank { idFor(att.eventId, att.childId) }
        val now = Timestamp.now()

        repoScope.launch {
            val current = dao.getOnce(id)
            val toSave = att.copy(
                attendanceId = id,
                createdAt = current?.createdAt ?: att.createdAt,
                updatedAt = now,
                checkedAt = att.checkedAt,
                version = (current?.version ?: 0L) + 1L,
                isDirty = true
            )
            Timber.i("ATT/UPSERT → id=%s v=%d e=%s c=%s", id, toSave.version, toSave.eventId, toSave.childId)
            dao.upsert(toSave)
            val roundTrip = dao.getOnce(id)
            Timber.i("ATT/READBACK ← id=%s status=%s version=%d", id, roundTrip?.status, roundTrip?.version ?: -1)
//            AttendanceSyncScheduler.enqueuePushNow(appContext)
            SyncCoordinatorScheduler.enqueuePushAllNow(appContext)
        }
    }


    override fun streamAttendanceForEvent(eventId: String): Flow<AttendanceSnapshot> =
        dao.observeByEvent(eventId).map { list ->
            AttendanceSnapshot(
                attendance = list,
                fromCache = true,              // Room = local cache
                hasPendingWrites = list.any { it.isDirty }
            )
        }

    override fun streamAttendanceForChild(childId: String): Flow<AttendanceSnapshot> =
        dao.observeByChild(childId).map { list ->
            AttendanceSnapshot(
                attendance = list,
                fromCache = true,
                hasPendingWrites = list.any { it.isDirty }
            )
        }

    override suspend fun getAttendanceOnce(eventId: String): List<Attendance> =
        dao.getByEvent(eventId)

    // Repo
    override suspend fun getStatusForChildOnce(childId: String, eventId: String): AttendanceStatus? =
        dao.getOneForChildAtEvent(eventId, childId)?.status

    override suspend fun getPresentAttendanceOnce(eventId: String): List<Attendance> =
        dao.getByEventAndStatus(eventId, AttendanceStatus.PRESENT)

    override suspend fun getAbsentAttendanceOnce(eventId: String): List<Attendance> =
        dao.getByEventAndStatus(eventId, AttendanceStatus.ABSENT)

    override fun markAllInBatchChunked(
        eventId: String,
        adminId: String,
        children: List<Child>,
        existing: Map<String, Attendance?>,
        status: AttendanceStatus,
        chunkSize: Int
    ): Task<Void> {

        repoScope.launch {
            val now = Timestamp.now()
            val rows = ArrayList<Attendance>(children.size)

            children.forEach { child ->
                val cur = existing[child.childId]
                if (cur?.status == status) return@forEach

                val id = idFor(eventId, child.childId)
                rows += Attendance(
                    attendanceId = id,
                    childId = child.childId,
                    eventId = eventId,
                    adminId = adminId,
                    status = status,
                    notes = cur?.notes ?: "",
                    checkedAt = now,
                    createdAt = cur?.createdAt ?: now,
                    updatedAt = now,
                    isDirty = true,
                    version = (cur?.version ?: 0L) + 1L
                )
            }

            if (rows.isNotEmpty()) {
                // optional: split to smaller chunks if you like
                dao.upsertAll(rows)
                AttendanceSyncScheduler.enqueuePushNow(appContext)
            }
        }

        return Tasks.forResult(null)
    }


    override fun observeFrequentAttendeesForEvents(eventIds: List<String>): Flow<List<EventFrequentAttendeeRow>> =
        dao.observeFrequentAttendeesForEvents(eventIds)
    // 🔥 Hydrate exactly one event’s rows from Firestore → Room (server truth → clean)
    override suspend fun hydrateEvent(eventId: String) = withContext(Dispatchers.IO) {
        val snap = attendanceRef
            .whereEqualTo("eventId", eventId)
            .orderBy("updatedAt", Query.Direction.ASCENDING) // optional but nice for paging consistency
            .get()
            .await()

        val items = snap.documents.mapNotNull { it.toObject(Attendance::class.java) }
        if (items.isNotEmpty()) {
            dao.upsertAll(items.map { it.copy(isDirty = false) })
        }
    }

    /** selects the count of the children based on the eventDate **/

    override suspend fun eligibleCountsForEvent(
        eventId: String,
        eventDate: Timestamp
    ): EligibleCounts {
        val cutoffNanos = endOfDayNanos(eventDate)
        val total = dao.countTotalEligibleByCutoff(cutoffNanos)

        val presentStatus = AttendanceStatus.PRESENT.name

        val present = dao.countPresentEligibleForEvent(
            eventId = eventId,
            presentStatus = presentStatus,
            cutoffNanos = cutoffNanos
        )
        return EligibleCounts(totalEligible = total, presentEligible = present)
    }

    private fun endOfDayNanos(ts: Timestamp): Long {
        val cal = Calendar.getInstance()
        cal.time = ts.toDate()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis * 1_000_000L
    }

}

