package com.example.charityDept.presentation.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.AttendanceDao
import com.example.charityDept.data.model.Attendance
import com.example.charityDept.data.model.AttendanceStatus
import com.example.charityDept.data.model.Child
import com.example.charityDept.domain.repositories.offline.OfflineAttendanceRepository
import com.example.charityDept.domain.repositories.offline.OfflineChildrenRepository
import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// /// CHANGED: add Paging imports (keeps your Paging hooks compiling)
import androidx.paging.PagingData // /// CHANGED
import androidx.paging.cachedIn  // /// CHANGED
import com.example.charityDept.data.model.EventStatus

data class RosterChild(
    val child: Child,
    val attendance: Attendance? = null,
    val present: Boolean = false
)

data class ListUi(
    val query: String = "",
    val pageSize: Int = 50
)

data class AttendanceRosterUiState(
    val loading: Boolean = true,
    val children: List<RosterChild> = emptyList(),
    val eventTitle: String? = null,
    val eventDate: Timestamp = Timestamp.now(),
    val eventStatus: EventStatus = EventStatus.SCHEDULED,

    val error: String? = null,
    val isOffline: Boolean = false,
    val isSyncing: Boolean = false
)

@HiltViewModel
class AttendanceRosterViewModel @Inject constructor(
    private val childrenRepo: OfflineChildrenRepository,       // SoT: Room
    private val attendanceDao: AttendanceDao,                  // SoT: Room (read)
    private val attendanceRepo: OfflineAttendanceRepository,   // SoT: Room (write + enqueue sync)
    private val eventRepo: OfflineEventsRepository             // SoT: Room (or hydrate-then-read)
) : ViewModel() {

    private val _ui = MutableStateFlow(AttendanceRosterUiState())
    val ui: StateFlow<AttendanceRosterUiState> = _ui.asStateFlow()

    // --- Search state ---
    private val _query = MutableStateFlow("") // /// CHANGED: keep dedicated query state
    fun onSearchQueryChange(q: String) {
        _query.value = q.trim()               // /// CHANGED: fixed (was trying to mutate _ui with a non-existent field)
    }

    // --- Debounced search text (used by Paging hooks and by combine below) ---
    private val needle: Flow<String> = _query // /// CHANGED: base on _query, not _ui
        .debounce(250)
        .distinctUntilChanged()

    // --- Optional Paging hooks (compile-safe, no logic removed) ---
    // NOTE: These assume OfflineChildrenRepository exposes pagedNotGraduated(...) and countNotGraduated(...).
    // If not present yet, add them in the repo/DAO (as we drafted earlier) — no ViewModel logic changes needed here.
    val childrenPaging: Flow<PagingData<Child>> =               // /// CHANGED (new import + fixed needle source)
        needle.flatMapLatest { n ->
            childrenRepo.pagedNotGraduated(n, pageSize = 50)    // page size from ListUi default; keep it simple here
        }.cachedIn(viewModelScope)

    val searchCount: Flow<Int> =
        needle.flatMapLatest { n -> childrenRepo.countNotGraduated(n) } // /// CHANGED

    // one-off UI events (snackbar)
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    sealed class UiEvent {
        data class Saved(val pendingSync: Boolean) : UiEvent()
    }

    // bulk flag
    private val _bulkMode = MutableStateFlow(false)
    val bulkMode: StateFlow<Boolean> = _bulkMode.asStateFlow()

    // Cancel duplicate loads
    private var loadJob: Job? = null

    fun load(eventId: String, limit: Long = 300) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val eventFlow = flow {
                emit(runCatching { eventRepo.getEventFast(eventId) }.getOrNull())
            }

            val attendanceFlow = attendanceRepo.streamAttendanceForEvent(eventId)
            val smoothAttendanceFlow =
                _bulkMode.flatMapLatest { bulk ->
                    if (bulk) attendanceFlow.debounce(120) else attendanceFlow
                }

            combine(
                childrenRepo.streamAllNotGraduated(),
                smoothAttendanceFlow,
                eventFlow,
                _query.debounce(200) // keep search filter in this pipeline too
            ) { childrenSnap, attendanceSnap, eventSnap, q ->
                val attMap = attendanceSnap.attendance.associateBy { it.childId }
                val merged = childrenSnap.children.map { child ->
                    val att = attMap[child.childId]
                    RosterChild(
                        child = child,
                        attendance = att,
                        present = att?.status == AttendanceStatus.PRESENT
                    )
                }

                val filtered = if (q.isBlank()) merged else {
                    val needle = q.lowercase()
                    merged.filter { rc ->
                        val full = "${rc.child.fName} ${rc.child.lName}".trim().lowercase()
                        full.contains(needle)
                    }
                }
//                eventStatus = eventSnap?.eventStatus ?: com.example.charityDept.data.model.EventStatus.SCHEDULED,

//                AttendanceRosterUiState(
//                    loading = false,
//                    children = filtered,
//                    eventTitle = eventSnap?.title,
//                    eventDate = eventSnap!!.eventDate, // keeping original behavior; if nullable is a concern, we can guard explicitly.
//                    error = null,
//                    isOffline = childrenSnap.fromCache || attendanceSnap.fromCache,
//                    isSyncing = childrenSnap.hasPendingWrites || attendanceSnap.hasPendingWrites
//                )
                AttendanceRosterUiState(
                    loading = false,
                    children = filtered,
                    eventTitle = eventSnap?.title,
                    eventDate = eventSnap!!.eventDate,
                    eventStatus = eventSnap?.eventStatus ?: EventStatus.SCHEDULED, // /// CHANGED
                    // /// CHANGED
                    error = null,
                    isOffline = childrenSnap.fromCache || attendanceSnap.fromCache,
                    isSyncing = childrenSnap.hasPendingWrites || attendanceSnap.hasPendingWrites
                )

            }
                .onStart { _ui.value = AttendanceRosterUiState(loading = true) }
                .catch { e ->
                    _ui.value = AttendanceRosterUiState(
                        loading = false,
                        error = e.message ?: "Failed to load roster"
                    )
                }
                .collect { state -> _ui.value = state }
        }
    }

    /** Notes updates for ABSENT children — Timestamp throughout ✅ */
    fun updateNotes(eventId: String, rosterChild: RosterChild, adminId: String, notes: String) {
        if (_ui.value.eventStatus == EventStatus.DONE) return // /// CHANGED

        val nowTs = Timestamp.now()
        val att = Attendance(
            attendanceId = "${eventId}_${rosterChild.child.childId}",
            childId = rosterChild.child.childId,
            eventId = eventId,
            adminId = adminId,
            status = AttendanceStatus.ABSENT,
            notes = notes,
            checkedAt = nowTs,
            createdAt = rosterChild.attendance?.createdAt ?: nowTs,
            updatedAt = nowTs
        )
        attendanceRepo.enqueueUpsertAttendance(att)
        _events.tryEmit(UiEvent.Saved(pendingSync = _ui.value.isOffline || _ui.value.isSyncing))
    }

    /** Toggle PRESENT/ABSENT — Timestamp throughout ✅ */
    /** Toggle PRESENT/ABSENT — Timestamp throughout ✅ */
    fun toggleAttendance(eventId: String, rosterChild: RosterChild, adminId: String) {
        if (_ui.value.eventStatus == EventStatus.DONE) return // /// CHANGED

        val nowTs = Timestamp.now()

        // /// CHANGED: stable id per event+child so we UPDATE the same row (prevents duplicate rows)
        val stableId = "${eventId}_${rosterChild.child.childId}"

        val newStatus = if (rosterChild.present) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT

        val att = Attendance(
            attendanceId = stableId, // /// CHANGED
            childId = rosterChild.child.childId,
            eventId = eventId,
            adminId = adminId,
            status = newStatus,
            notes = if (newStatus == AttendanceStatus.PRESENT) "" else rosterChild.attendance?.notes.orEmpty(),
            createdAt = rosterChild.attendance?.createdAt ?: nowTs,
            updatedAt = nowTs
        )

        attendanceRepo.enqueueUpsertAttendance(att)
        _events.tryEmit(UiEvent.Saved(pendingSync = _ui.value.isOffline || _ui.value.isSyncing))
    }

//    fun toggleAttendance(eventId: String, rosterChild: RosterChild, adminId: String) {
//        val nowTs = Timestamp.now()
//        val attendanceId = GenerateId.generateId("attendance")
//        val newStatus = if (rosterChild.present) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
//        val att = Attendance(
//            attendanceId = attendanceId,
//            childId = rosterChild.child.childId,
//            eventId = eventId,
//            adminId = adminId,
//            status = newStatus,
////            notes = rosterChild.attendance?.notes.orEmpty(),
//            notes = if (newStatus == AttendanceStatus.PRESENT) "" else rosterChild.attendance?.notes.orEmpty(),
//            createdAt = rosterChild.attendance?.createdAt ?: nowTs,
//            updatedAt = nowTs
//        )
//        attendanceRepo.enqueueUpsertAttendance(att)
//        _events.tryEmit(UiEvent.Saved(pendingSync = _ui.value.isOffline || _ui.value.isSyncing))
//    }

    // --- BULK API (Present / Absent) ---
    fun markAllPresent(eventId: String, adminId: String) =
        markAllInternalBatch(eventId, adminId, AttendanceStatus.PRESENT)

    fun markAllAbsent(eventId: String, adminId: String) =
        markAllInternalBatch(eventId, adminId, AttendanceStatus.ABSENT)

    private fun markAllInternalBatch(eventId: String, adminId: String, status: AttendanceStatus) {
        val kids = _ui.value.children
        if (kids.isEmpty()) return

        _ui.value = AttendanceRosterUiState( // keep your original busy behavior
            loading = true,
        )
        _bulkMode.value = true

        viewModelScope.launch {
            try {
                val targets = kids.filter { rc -> rc.attendance?.status != status }
                if (targets.isEmpty()) {
                    _events.tryEmit(UiEvent.Saved(pendingSync = false))
                    return@launch
                }

                val existing = kids.associate { it.child.childId to it.attendance }

                val task = attendanceRepo.markAllInBatchChunked(
                    eventId = eventId,
                    adminId = adminId,
                    children = targets.map { it.child },
                    existing = existing,
                    status = status
                )

                _events.tryEmit(UiEvent.Saved(pendingSync = true))

                // Await completion so busy stays true while work runs (kept)
                try {
                    task?.await()
                    _events.tryEmit(UiEvent.Saved(pendingSync = false))
                } catch (e: Exception) {
                    _events.tryEmit(UiEvent.Saved(pendingSync = true))
                }
            } finally {
                _bulkMode.value = false
            }
        }
    }
}

