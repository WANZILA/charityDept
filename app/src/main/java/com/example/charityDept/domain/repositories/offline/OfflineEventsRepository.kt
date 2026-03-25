package com.example.charityDept.domain.repositories.offline

// <app/src/main/java/com/example/zionkids/domain/repositories/offline/OfflineEventsRepository.kt>
// /// NEW: Offline-first repository for events. UI reads Room; local writes mark isDirty;
// ///      mirrors your online repo’s API so you can swap without breaking callers.
// + createOrUpdateEvent() writes to Room, marks isDirty, updates updatedAt; generates id if needed
// + streamEvents() emits list + metadata (hasPendingWrites from dirty count; fromCache=true)
// + streamEventSnapshots() emits just the list
// + getEventFast() reads Room
// + deleteEventAndAttendances() soft-deletes event (attendance cascade can be added later)
//package com.example.charityDept.domain.repositories.offline

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.charityDept.core.Utils.GenerateId
import com.example.charityDept.core.sync.attendance.AttendanceSyncScheduler
import com.example.charityDept.core.sync.event.EventSyncScheduler
import com.example.charityDept.data.local.dao.AttendanceDao
//import com.example.charityDept.core.sync.event.EventCascadeDeleteWorker
import com.example.charityDept.data.local.dao.EventDao
import com.example.charityDept.data.local.dao.KpiDao
import com.example.charityDept.data.model.Attendance
import com.example.charityDept.data.model.Event
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

data class EventSnapshot(
    val events: List<Event>,
    val fromCache: Boolean,
    val hasPendingWrites: Boolean
)

private const val KPI_EVENTS_TOTAL = "events_total"
private fun dayBucket(ts: Timestamp): String {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    return "events_day_" + sdf.format(ts.toDate())
}
interface OfflineEventsRepository {
    suspend fun createOrUpdateEvent(event: Event, isNew: Boolean): String
    fun streamEvents(): Flow<EventSnapshot>               // list + metadata (Room-based)
    fun streamEventSnapshots(): Flow<List<Event>>         // just the list
    suspend fun getEventFast(id: String): Event?

    fun pagedActive(): Flow<PagingData<Event>>
    suspend fun deleteEventAndAttendances(eventId: String)

    fun observeEventsForParentPick(): Flow<List<Event>>

}

@Singleton
class OfflineEventsRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val attendanceDao: AttendanceDao,
    private val kpiDao: KpiDao,
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val appContext: android.content.Context
) : OfflineEventsRepository {

    override suspend fun createOrUpdateEvent(event: Event, isNew: Boolean): String {
        val id = event.eventId.ifBlank { GenerateId.generateId("event") }
        val now = Timestamp.now()
        val nextVersion = (event.version + 1).coerceAtLeast(1)
//        val id = event.eventId.ifBlank { GenerateId.generateId("event") }
//        val nowTs = Timestamp.now()
//        val wasNew = isNew || event.eventId.isBlank()
        // + Minimal-diff fields: keep your Event model; mark dirty for sync; soft-resolve version later
        val toSave = event.copy(
            eventId   = id,
            createdAt = if (isNew) now else event.createdAt,
            updatedAt = now,
            isDirty   = true,
            isDeleted = false,
            version   = nextVersion
            // keep version as-is; the conflict rule prefers higher version else newer updatedAt
        )
        eventDao.upsertOne(toSave)


        // --- KPI bumps (cheap, no full-table read) ---
        // Total events
//        if (wasNew) kpiDao.bump(KPI_EVENTS_TOTAL, +1)

        // Bump counters only for NEW events
        if (isNew) {
            kpiDao.ensureKey(KPI_EVENTS_TOTAL)
            kpiDao.ensureKey(todayKeyUTC())
            kpiDao.add(KPI_EVENTS_TOTAL, +1)
            kpiDao.add(todayKeyUTC(), +1)
        }
        // Per-day bucket (based on eventDate)
//        kpiDao.bump(dayBucket(toSave.eventDate), +1)

        return id
    }

    override fun streamEvents(): Flow<EventSnapshot> {
        // + hasPendingWrites derived from dirty count; this is local truth so fromCache=true
        val lists: Flow<List<Event>> = eventDao.observeAllActive()
        val dirtyCount: Flow<Int> = eventDao.observeDirtyCount()
        return combine(lists, dirtyCount) { evts, dirty ->
            EventSnapshot(
                events = evts,
                fromCache = true,
                hasPendingWrites = dirty > 0
            )
        }
    }

    override fun streamEventSnapshots(): Flow<List<Event>> =
        eventDao.observeAllActive()

    override suspend fun getEventFast(id: String): Event? =
        eventDao.getOnce(id)

    override suspend fun deleteEventAndAttendances(eventId: String) {
        require(eventId.isNotBlank()) { "eventId is blank" }

        val now = Timestamp.now()

        // Plain-English: 1) mark the event as deleted (tombstone) locally
        val prev = eventDao.getOnce(eventId)


        // Plain-English: 2) mark related tables (attendances) as deleted too
        attendanceDao.softDeleteByEventId(eventId, now)

        eventDao.softDeleteChildByEventId(eventId, now)
        eventDao.softDeleteByEventId(eventId, now)

        // Plain-English: KPI update (optional)
        // If you want symmetric KPI decrements, do it here using `prev`.
        // prev?.let { ... }

        // Plain-English: 3) queue sync workers so Firestore gets the tombstones
        EventSyncScheduler.enqueuePushNow(appContext)
        AttendanceSyncScheduler.enqueuePushNow(appContext)

        // Plain-English: 4) optional: remote cascade worker (only if you build one later)
        // EventSyncScheduler.enqueueCascadeDelete(appContext, eventId)
    }
    // /// CHANGED: Paging 3 hook (UI calls collectAsLazyPagingItems())
    override fun pagedActive(): Flow<PagingData<Event>> =
        Pager(
            config = PagingConfig(
                pageSize = 50,
                initialLoadSize = 100,
                prefetchDistance = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { eventDao.pagingActive() }
        ).flow

    override fun observeEventsForParentPick(): Flow<List<Event>> = eventDao.observeParentCandidates()

    // --- keep other methods unchanged ---

    private fun todayKeyUTC(): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        return "events_day_" + sdf.format(java.util.Date())
    }


    companion object {
        private const val KPI_EVENTS_TOTAL = "events_total"
    }
}

