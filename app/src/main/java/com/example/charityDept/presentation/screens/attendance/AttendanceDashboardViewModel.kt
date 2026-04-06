package com.example.charityDept.presentation.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.core.utils.Network.NetworkMonitorUtil
import com.example.charityDept.data.model.AttendanceStatus
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.Event
import com.example.charityDept.domain.repositories.offline.OfflineAttendanceRepository
import com.example.charityDept.domain.repositories.offline.OfflineChildrenRepository
import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class ChildHistory(
    val child: Child,
    val lastEvents: List<AttendanceStatus>,
    val consecutiveAbsences: Int
)

@HiltViewModel
class AttendanceDashboardViewModel @Inject constructor(
    private val childrenRepo: OfflineChildrenRepository,        // Room-backed
    private val attendanceRepo: OfflineAttendanceRepository,    // Room-backed
    private val eventsRepo: OfflineEventsRepository,            // Room-backed
    private val networkMonitor: NetworkMonitorUtil,
) : ViewModel() {

    private val _selectedEventId = MutableStateFlow<String?>(null)

    data class Ui(
        val loading: Boolean = true,
        val error: String? = null,
        val isOffline: Boolean = false,
        val isSyncing: Boolean = false,
        val events: List<Event> = emptyList(),
        val selectedEventId: String? = null,
        val selectedEventTitle: String = "Attendance",
        val selectedEventDateText: String = "",
        val total: Int = 0,
        val present: Int = 0,
        val absent: Int = 0,
        val presentPct: Int = 0,
        val absentPct: Int = 0,
        val trend: List<Pair<String, Int>> = emptyList(),
        val alerts: List<ChildHistory> = emptyList(),
        val notesTop: List<Pair<String, Int>> = emptyList(),
        val topAttendees: List<Child> = emptyList(),
        val frequentAbsentees: List<Child> = emptyList(),
        val histories: List<ChildHistory> = emptyList()
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    // 1) Cache events stream so we don’t requery / re-wake Room repeatedly
    private val eventsFlow: StateFlow<List<Event>> =
        eventsRepo.streamEventSnapshots()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // 2) Pick first event ASAP (no spinner wait)
        viewModelScope.launch {
            val firstList = eventsFlow.first()
            if (_selectedEventId.value == null && firstList.isNotEmpty()) {
                _selectedEventId.value = firstList.first().eventId
            }
        }

        // 3) Keep UI’s events list in sync (cheap; comes from cached flow)
        viewModelScope.launch {
            eventsFlow.collect { events ->
                _ui.update { it.copy(events = events, loading = false, error = null) }
                // Ensure a selected event if still null
                if (_selectedEventId.value == null && events.isNotEmpty()) {
                    _selectedEventId.value = events.first().eventId
                }
            }
        }

        // 4) Core dashboard stream — all Room, non-blocking hydration in background
        viewModelScope.launch {
            combine(
                childrenRepo.streamAllNotGraduated(),
                _selectedEventId.filterNotNull(),
                networkMonitor.isOnline
            ) { childrenSnap, eid, isOnline ->
                Triple(childrenSnap, eid, isOnline)
            }
                .flatMapLatest { (childrenSnap, eid, isOnline) ->
                    // Kick hydration in the background if Room is empty for this event — do NOT block UI
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching {
                            if (attendanceRepo.getAttendanceOnce(eid).isEmpty()) {
                                attendanceRepo.hydrateEvent(eid)
                            }
                        }
                    }

                    // Combine fast local event lookup + attendance stream (both Room)
                    combine(
                        flow { emit(eventsRepo.getEventFast(eid)) },
                        attendanceRepo.streamAttendanceForEvent(eid)
                    ) { ev, attSnap ->
                        Triple(childrenSnap, ev, Pair(attSnap, isOnline))
                    }
                }
                .map { (childrenSnap, event, attAndNet) ->
                    val (attSnap, isOnline) = attAndNet

                    // Compute eligible counts from repo (Room query) — instant
                    val counts = event?.eventDate?.let { dt ->
                        attendanceRepo.eligibleCountsForEvent(event.eventId, dt)
                    }

                    val total   = counts?.totalEligible ?: 0
                    val present = counts?.presentEligible ?: 0
                    val absent  = (total - present).coerceAtLeast(0)
                    val presentPct = pct(present, total)
                    val absentPct  = pct(absent, total)

                    // Tiny trend (cheap; still Room)
                    val recent = eventsFlow.value.take(3)
                    val trend = recent.map { ev ->
                        val c = attendanceRepo.getAttendanceOnce(ev.eventId)
                            .count { it.status == AttendanceStatus.PRESENT }
                        label(ev.title.trim().take(15)) to c
                    }

                    val offlineHeuristic =
                        (childrenSnap.fromCache && !childrenSnap.hasPendingWrites) &&
                                (attSnap.fromCache && !attSnap.hasPendingWrites)
                    val syncing = childrenSnap.hasPendingWrites || attSnap.hasPendingWrites
                    val isOffline = !isOnline || offlineHeuristic

                    Ui(
                        loading = false,
                        error = null,
                        isOffline = isOffline,
                        isSyncing = syncing,
                        events = eventsFlow.value,
                        selectedEventId = event?.eventId,
                        selectedEventTitle = event?.title ?: "Attendance",
                        selectedEventDateText = event?.eventDate?.let { fullLabel(it) } ?: "",
                        total = total,
                        present = present,
                        absent = absent,
                        presentPct = presentPct,
                        absentPct = absentPct,
                        trend = trend
                    )
                }
                // Avoid noisy recompositions while hydration trickles in
                .distinctUntilChanged()
                .catch { e -> _ui.update { it.copy(loading = false, error = e.message) } }
                .collect { s -> _ui.value = s }
        }
    }

    fun onSelectEvent(id: String) { _selectedEventId.value = id }

    // ----- helpers -----
    private fun pct(n: Int, d: Int) = if (d <= 0) 0 else ((n.toDouble() / d) * 100).toInt()
    private fun label(s: String): String = s
    private fun fullLabel(ts: Timestamp): String =
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(ts.toDate())
}

//package com.example.charityDept.presentation.viewModels.attendance
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.charityDept.core.Utils.Network.NetworkMonitorUtil
//import com.example.charityDept.data.model.AttendanceStatus
//import com.example.charityDept.data.model.Child
//import com.example.charityDept.data.model.Event
//import com.example.charityDept.domain.repositories.offline.OfflineAttendanceRepository
//import com.example.charityDept.domain.repositories.offline.OfflineChildrenRepository
//import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
//import com.google.firebase.Timestamp
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import timber.log.Timber
//import java.text.SimpleDateFormat
//import java.util.Locale
//import javax.inject.Inject
//
//data class ChildHistory(
//    val child: Child,
//    val lastEvents: List<AttendanceStatus>,
//    val consecutiveAbsences: Int
//)
//
//@HiltViewModel
//class AttendanceDashboardViewModel @Inject constructor(
//    private val childrenRepo: OfflineChildrenRepository,        // Room-backed
//    private val attendanceRepo: OfflineAttendanceRepository,    // Room-backed
//    private val eventsRepo: OfflineEventsRepository,            // Room-backed
//    private val networkMonitor: NetworkMonitorUtil,
//) : ViewModel() {
//
//    private val _selectedEventId = MutableStateFlow<String?>(null)
//
//    data class Ui(
//        val loading: Boolean = true,
//        val error: String? = null,
//        val isOffline: Boolean = false,
//        val isSyncing: Boolean = false,
//        val events: List<Event> = emptyList(),
//        val selectedEventId: String? = null,
//        val selectedEventTitle: String = "Attendance",
//        val selectedEventDateText: String = "",
//        val total: Int = 0,
//        val present: Int = 0,
//        val absent: Int = 0,
//        val presentPct: Int = 0,
//        val absentPct: Int = 0,
//        val trend: List<Pair<String, Int>> = emptyList(),
//        val alerts: List<ChildHistory> = emptyList(),
//        val notesTop: List<Pair<String, Int>> = emptyList(),
//        val topAttendees: List<Child> = emptyList(),
//        val frequentAbsentees: List<Child> = emptyList(),
//        val histories: List<ChildHistory> = emptyList()
//    )
//
//    private val _ui = MutableStateFlow(Ui())
//    val ui: StateFlow<Ui> = _ui.asStateFlow()
//
//    init {
//        // 1) Stream events from Room; pick the first as default
//        viewModelScope.launch {
//            eventsRepo.streamEventSnapshots()
//                .onStart { _ui.update { it.copy(loading = true) } }
//                .catch { e -> _ui.update { it.copy(loading = false, error = e.message) } }
//                .collect { events ->
//                    _ui.update { it.copy(events = events) }
//                    if (_selectedEventId.value == null && events.isNotEmpty()) {
//                        _selectedEventId.value = events.first().eventId
//                    }
//                }
//        }
//
//        // 2) When event changes, hydrate-if-empty then stream everything from Room
//        viewModelScope.launch {
//            _selectedEventId
//                .filterNotNull()
//                .flatMapLatest { id ->
//                    // Ensure Room has this event’s attendance at least once
//                    flow {
//                        val already = attendanceRepo.getAttendanceOnce(id)
//                        if (already.isEmpty()) {
//                            runCatching { attendanceRepo.hydrateEvent(id) }
//                        }
//                        emit(id)
//                    }.flatMapLatest { eid ->
//                        combine(
//                            childrenRepo.streamAllNotGraduated(),           // Room snapshot (for status flags)
//                            combine(
//                                flow { emit(eventsRepo.getEventFast(eid)) }, // Room fast lookup
//                                attendanceRepo.streamAttendanceForEvent(eid) // Room snapshot (for status flags)
//                            ) { ev, attSnap -> ev to attSnap },
//                            networkMonitor.isOnline
//                        ) { childrenSnap, (event, attSnap), isOnline ->
//
//                            // --- Core totals based on eligibility rule in the repo ---
//                            val counts = event?.eventDate?.let { dt ->
//                                attendanceRepo.eligibleCountsForEvent(eid, dt)
//                            }
//                            val total   = counts?.totalEligible ?: 0
//                            val present = counts?.presentEligible ?: 0
//                            val absent  = (total - present).coerceAtLeast(0)
//                            val presentPct = pct(present, total)
//                            val absentPct  = pct(absent, total)
//                            Timber.d("DASH counts eid=%s total=%d present=%d absent=%d", eid, total, present, absent)
//                            // Trend (last 3 events) – current simple present counts from Room
//                            // (If you want the same eligibility rule per event, we can swap this later.)
//                            val recent = _ui.value.events.take(3)
//                            val trend = recent.map { ev ->
//                                val c = attendanceRepo.getAttendanceOnce(ev.eventId)
//                                    .count { it.status == AttendanceStatus.PRESENT }
//                                label(ev.title.trim().take(15)) to c
//                            }
//
//                            // Status flags from Room metadata + network
//                            val offlineHeuristic =
//                                (childrenSnap.fromCache && !childrenSnap.hasPendingWrites) &&
//                                        (attSnap.fromCache && !attSnap.hasPendingWrites)
//                            val syncing = childrenSnap.hasPendingWrites || attSnap.hasPendingWrites
//                            val isOffline = !isOnline || offlineHeuristic
//
//                            Ui(
//                                loading = false,
//                                error = null,
//                                isOffline = isOffline,
//                                isSyncing = syncing,
//                                events = _ui.value.events,
//                                selectedEventId = eid,
//                                selectedEventTitle = event?.title ?: "Attendance",
//                                selectedEventDateText = event?.eventDate?.let { fullLabel(it) } ?: "",
//                                total = total,
//                                present = present,
//                                absent = absent,
//                                presentPct = presentPct,
//                                absentPct = absentPct,
//                                trend = trend
//                            )
//                        }
//                    }
//                }
//                .catch { e -> _ui.update { it.copy(loading = false, error = e.message) } }
//                .collect { s -> _ui.value = s }
//        }
//    }
//
//    fun onSelectEvent(id: String) { _selectedEventId.value = id }
//
//    // ----- helpers -----
//    private fun pct(n: Int, d: Int) = if (d <= 0) 0 else ((n.toDouble() / d) * 100).toInt()
//    private fun label(s: String): String = s
//    private fun fullLabel(ts: Timestamp): String =
//        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(ts.toDate())
//}

