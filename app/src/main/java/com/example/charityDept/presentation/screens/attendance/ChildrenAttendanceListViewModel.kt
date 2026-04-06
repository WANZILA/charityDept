//package com.example.charityDept.presentation.viewModels.attendance

package com.example.charityDept.presentation.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.example.charityDept.data.model.AttendanceStatus
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.Event
import com.example.charityDept.domain.repositories.online.AttendanceRepository
import com.example.charityDept.domain.repositories.online.ChildrenRepository
import com.example.charityDept.domain.repositories.online.EventsRepository
import com.example.charityDept.core.utils.Network.NetworkMonitorUtil
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class ChildAttendanceHistory(
    val child: Child,
    val lastEvents: List<AttendanceStatus>,   // newest-first list per child
    val consecutiveAbsences: Int
)

@HiltViewModel
class ChildrenAttendanceListViewModel @Inject constructor(
    private val childrenRepo: ChildrenRepository,
    private val attendanceRepo: AttendanceRepository,
    private val eventsRepo: EventsRepository,
    private val networkMonitor: NetworkMonitorUtil,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ----- Route args -----
    private val mode: String = (savedStateHandle["mode"] ?: "present").toString().lowercase()
    private val eventIdArg: String? = savedStateHandle["eventId"]

    // ----- UI State -----
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
        val trend: List<Pair<String,Int>> = emptyList(),
        val alerts: List<ChildAttendanceHistory> = emptyList(),
        val notesTop: List<Pair<String,Int>> = emptyList(),
        val topAttendees: List<Child> = emptyList(),
        val frequentAbsentees: List<Child> = emptyList(),
        val histories: List<ChildAttendanceHistory> = emptyList()
    )
    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    // selection flows
    private val _selectedEventId = MutableStateFlow<String?>(eventIdArg)

    init {
        // Stream events; if no event pinned in args, auto-select latest
        viewModelScope.launch {
            eventsRepo.streamEventSnapshots()  // DESC by eventDate inside repo
                .onStart { _ui.update { it.copy(loading = true) } }
                .catch { e -> _ui.update { it.copy(loading = false, error = e.message) } }
                .collect { events ->
                    _ui.update { it.copy(events = events) }
                    if (_selectedEventId.value == null && events.isNotEmpty() && eventIdArg == null) {
                        _selectedEventId.value = events.first().eventId
                    }
                }
        }

        // Main compute pipeline: children + (selected event -> event + attendance) + network
        viewModelScope.launch {
            combine(
                childrenRepo.streamAllNotGraduated(),
                _selectedEventId.filterNotNull().flatMapLatest { id ->
                    combine(
                        flow { emit(eventsRepo.getEventFast(id)) },
                        attendanceRepo.streamAttendanceForEvent(id)
                    ) { ev, attSnap -> ev to attSnap }
                },
                networkMonitor.isOnline
            ) { childrenSnap, (event, attSnap), isOnline ->

                val children = childrenSnap.children
                val att = attSnap.attendance

                val present = att.count { it.status == AttendanceStatus.PRESENT }
                val total = children.size
                val absent = (total - present).coerceAtLeast(0)
                val presentPct = pct(present, total)
                val absentPct = pct(absent, total)

                // Absent reasons (top 5)
                val notesTop = att
                    .filter { it.status == AttendanceStatus.ABSENT }
                    .map { it.notes.trim() }
                    .filter { it.isNotEmpty() }
                    .groupingBy { normalize(it) }.eachCount()
                    .entries.sortedByDescending { it.value }.take(5)
                    .map { it.key to it.value }

                // Use the already-streamed events list for history
                val recent = _ui.value.events

                // Build per-child status history across "recent" events
                val histories = children.map { child ->
                    val statuses = recent.map { ev ->
                        attendanceRepo.getStatusForChildOnce(child.childId, ev.eventId)
                            ?: AttendanceStatus.ABSENT
                    }
                    ChildAttendanceHistory(
                        child = child,
                        lastEvents = statuses,
                        consecutiveAbsences = consecutiveAbsencesFromNewest(statuses)
                    )
                }

                val presentHistoriesOnly = histories.filter { it.lastEvents.firstOrNull() == AttendanceStatus.PRESENT }
                val absentHistoriesOnly  = histories.filter { it.lastEvents.firstOrNull() == AttendanceStatus.ABSENT }

                val topAttendeesAll = histories
                    .filter { it.lastEvents.isNotEmpty() && it.lastEvents.all { s -> s == AttendanceStatus.PRESENT } }
                    .map { it.child }
                    .take(3)

                val frequentAbsenteesAll = histories
                    .filter { it.lastEvents.isNotEmpty() }
                    .filter { h -> h.lastEvents.count { it == AttendanceStatus.ABSENT } >= (h.lastEvents.size / 2.0) }
                    .map { it.child }
                    .take(3)

                // (Optional) you can keep a trend if needed elsewhere
                // val trend = recent.map { ev ->
                //     val c = attendanceRepo.getAttendanceOnce(ev.eventId)
                //         .count { it.status == AttendanceStatus.PRESENT }
                //     label(ev.eventDate) to c
                // }.reversed()

                // Online/offline flags
                val offlineHeuristic =
                    (childrenSnap.fromCache && !childrenSnap.hasPendingWrites) &&
                            (attSnap.fromCache && !attSnap.hasPendingWrites)
                val syncing = childrenSnap.hasPendingWrites || attSnap.hasPendingWrites
                val isOffline = !isOnline || offlineHeuristic

                // Shape UI based on mode
                when (mode) {
                    "absent" -> Ui(
                        loading = false,
                        error = null,
                        isOffline = isOffline,
                        isSyncing = syncing,
                        events = _ui.value.events,
                        selectedEventId = _selectedEventId.value,
                        selectedEventTitle = event?.title ?: "Attendance",
                        selectedEventDateText = event?.eventDate?.let { fullLabel(it) } ?: "",
                        total = total,
                        present = present,
                        absent = absent,
                        presentPct = presentPct,
                        absentPct = absentPct,
                        trend = emptyList(),
                        alerts = emptyList(),
                        notesTop = notesTop,                         // show reasons
                        topAttendees = emptyList(),
                        frequentAbsentees = frequentAbsenteesAll,    // show absentees
                        histories = absentHistoriesOnly              // only absent rows
                    ).also { _ui.value = it }

                    else -> Ui(
                        loading = false,
                        error = null,
                        isOffline = isOffline,
                        isSyncing = syncing,
                        events = _ui.value.events,
                        selectedEventId = _selectedEventId.value,
                        selectedEventTitle = event?.title ?: "Attendance",
                        selectedEventDateText = event?.eventDate?.let { fullLabel(it) } ?: "",
                        total = total,
                        present = present,
                        absent = absent,
                        presentPct = presentPct,
                        absentPct = absentPct,
                        trend = emptyList(),
                        alerts = emptyList(),
                        notesTop = emptyList(),                      // hide reasons
                        topAttendees = topAttendeesAll,              // show top present
                        frequentAbsentees = emptyList(),
                        histories = presentHistoriesOnly             // only present rows
                    ).also { _ui.value = it }
                }
            }
                .catch { e -> _ui.update { it.copy(loading = false, error = e.message) } }
                .collect { /* already pushed to _ui */ }
        }
    }

    fun onSelectEvent(id: String) { _selectedEventId.value = id }

    // ----- helpers -----
    private fun pct(n: Int, d: Int) = if (d <= 0) 0 else ((n.toDouble() / d) * 100).toInt()

    private fun consecutiveAbsencesFromNewest(statuses: List<AttendanceStatus>): Int {
        var c = 0
        for (i in statuses.indices.reversed()) if (statuses[i] == AttendanceStatus.ABSENT) c++ else break
        return c
    }

    private fun normalize(s: String) =
        s.lowercase().replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9 \\-]"), "")
            .trim()

    private fun label(ts: Timestamp): String =
        SimpleDateFormat("MMM d", Locale.getDefault()).format(ts.toDate())

    private fun fullLabel(ts: Timestamp): String =
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(ts.toDate())
}


//
//import androidx.lifecycle.SavedStateHandle
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.charityDept.data.model.AttendanceStatus
//import com.example.charityDept.data.model.Child
//import com.example.charityDept.data.model.Event
//import com.example.charityDept.domain.repositories.online.AttendanceRepository
//import com.example.charityDept.domain.repositories.online.ChildrenRepository
//import com.example.charityDept.domain.repositories.online.EventsRepository
//import com.example.charityDept.core.Utils.Network.NetworkMonitorUtil
//import com.google.firebase.Timestamp
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.Locale
//import javax.inject.Inject
//
// data class ChildAttendanceHistory(
//    val child: Child,
//    val lastEvents: List<AttendanceStatus>,   // newest first (size up to N)
//    val consecutiveAbsences: Int              // streak from most recent backwards
//)
//
// data  class ChildrenAttendanceUiState(
//    val loading: Boolean = true,
//    val error: String? = null,
//    val isOffline: Boolean = false,
//    val isSyncing: Boolean = false,
//    val eventTitle: String = "Attendance",
//    val eventDateText: String = "",
//    val totalChildren: Int = 0,
//    val presentCount: Int = 0,
//    val absentCount: Int = 0,
//    val presentPct: Int = 0,
//    val absentPct: Int = 0,
//    val recentEventTrend: List<Pair<String, Int>> = emptyList(),
//    val topAttendees: List<Child> = emptyList(),
//    val frequentAbsentees: List<Child> = emptyList(),
//    val consecutiveAbsenceAlerts: List<ChildAttendanceHistory> = emptyList(),
//    val notesSummaryTop: List<Pair<String, Int>> = emptyList(),
//    val childHistories: List<ChildAttendanceHistory> = emptyList()
//)
//
//@HiltViewModel
//class ChildrenAttendanceListViewModel @Inject constructor(
//    private val childrenRepo: ChildrenRepository,
//    private val attendanceRepo: AttendanceRepository,
//    private val eventsRepo: EventsRepository,
//    private val networkMonitor: NetworkMonitorUtil,
//    private val savedStateHandle: SavedStateHandle,
//) : ViewModel() {
//
//    private val mode: String = (savedStateHandle["mode"] as? String)?.lowercase() ?: "present"
//    private val eventIdArg: String? = savedStateHandle["eventId"] as? String
//
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
//        val trend: List<Pair<String,Int>> = emptyList(),
//        val alerts: List<ChildAttendanceHistory> = emptyList(),
//        val notesTop: List<Pair<String,Int>> = emptyList(),
//        val topAttendees: List<Child> = emptyList(),
//        val frequentAbsentees: List<Child> = emptyList(),
//        val histories: List<ChildAttendanceHistory> = emptyList()
//    )
//    private val _ui = MutableStateFlow(Ui())
//    val ui: StateFlow<Ui> = _ui.asStateFlow()
//
//    init {
//        // Load events list and auto-select first
//        viewModelScope.launch {
//            eventsRepo.streamEventSnapshots()  // ordered DESC by eventDate in repo
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
//        // When an event is selected, compute the dashboard
//        viewModelScope.launch {
//            combine(
//                childrenRepo.streamAllNotGraduated(),
//                _selectedEventId.filterNotNull().flatMapLatest { id ->
//                    combine(
//                        flow { emit(eventsRepo.getEventFast(id)) },
//                        attendanceRepo.streamAttendanceForEvent(id)
//                    ) { ev, attSnap -> ev to attSnap }
//                },
//                networkMonitor.isOnline
//            ) { childrenSnap, (event, attSnap), isOnline ->
//
//                val children = childrenSnap.children
//                val att = attSnap.attendance
//                val present = att.count { it.status == AttendanceStatus.PRESENT }
//                val total = children.size
//                val absent = (total - present).coerceAtLeast(0)
//                val presentPct = pct(present, total)
//                val absentPct = pct(absent, total)
//
//                // notes summary
//                val notesTop = att.filter { it.status == AttendanceStatus.ABSENT }
//                    .map { it.notes.trim() }
//                    .filter { it.isNotEmpty() }
//                    .groupingBy { normalize(it) }.eachCount()
//                    .entries.sortedByDescending { it.value }.take(5)
//                    .map { it.key to it.value }
//
//                // --- Use Timestamp for event times ---
//                fun Event.timeMillis(): Long = this.eventDate.toDate().time
//
//                // trend (last 7 events present counts)
////                val recent = _ui.value.events.take(4)
//                val recent = _ui.value.events
//                val trend = recent.map { ev ->
//                    val count = attendanceRepo.getAttendanceOnce(ev.eventId)
//                        .count { it.status == AttendanceStatus.PRESENT }
//                    label(ev.eventDate) to count
//                }.reversed()
//
//                // per-child histories + alerts
//                val histories = children.map { child ->
//                    val statuses = recent.map { ev ->
//                        attendanceRepo.getStatusForChildOnce(child.childId, ev.eventId)
//                            ?: AttendanceStatus.ABSENT
//                    }
//                    ChildAttendanceHistory(
//                        child = child,
//                        lastEvents = statuses,
//                        consecutiveAbsences = consecutiveAbsencesFromNewest(statuses)
//                    )
//                }
//                val alerts = histories.filter { it.consecutiveAbsences >= 3 }
//                    .sortedByDescending { it.consecutiveAbsences }
//
//                val topAttendees = histories
//                    .filter { it.lastEvents.isNotEmpty() && it.lastEvents.all { s -> s == AttendanceStatus.PRESENT } }
//                    .map { it.child }
//                    .take(3)
//
//                val frequentAbsentees = histories
//                    .filter { it.lastEvents.isNotEmpty() }
//                    .filter { h -> h.lastEvents.count { it == AttendanceStatus.ABSENT } >= (h.lastEvents.size / 2.0) }
//                    .map { it.child }
//                    .take(3)
//
//                // Status flags
//                val offlineHeuristic =
//                    (childrenSnap.fromCache && !childrenSnap.hasPendingWrites) &&
//                            (attSnap.fromCache && !attSnap.hasPendingWrites)
//                val syncing = childrenSnap.hasPendingWrites || attSnap.hasPendingWrites
//                val isOffline = !isOnline || offlineHeuristic
//
//                Ui(
//                    loading = false,
//                    error = null,
//                    isOffline = isOffline,
//                    isSyncing = syncing,
//                    events = _ui.value.events,
//                    selectedEventId = _selectedEventId.value,
//                    selectedEventTitle = event?.title ?: "Attendance",
//                    selectedEventDateText = event?.eventDate?.let { fullLabel(it) } ?: "",
//                    total = total,
//                    present = present,
//                    absent = absent,
//                    presentPct = presentPct,
//                    absentPct = absentPct,
//                    trend = trend,
//                    alerts = alerts,
//                    notesTop = notesTop,
//                    topAttendees = topAttendees,
//                    frequentAbsentees = frequentAbsentees,
//                    histories = histories
//                )
//            }
//                .catch { e ->
//                    _ui.update { it.copy(loading = false, error = e.message) }
//                }
//                .collect { s -> _ui.value = s }
//        }
//    }
//
//    fun onSelectEvent(id: String) { _selectedEventId.value = id }
//
//    // helpers
//    private fun pct(n: Int, d: Int) = if (d <= 0) 0 else ((n.toDouble() / d) * 100).toInt()
//
//    private fun consecutiveAbsencesFromNewest(statuses: List<AttendanceStatus>): Int {
//        var c = 0
//        for (i in statuses.indices.reversed()) if (statuses[i] == AttendanceStatus.ABSENT) c++ else break
//        return c
//    }
//
//    private fun normalize(s: String) =
//        s.lowercase().replace(Regex("\\s+"), " ").replace(Regex("[^a-z0-9 \\-]"), "").trim()
//
//    // ----- Timestamp-based labels -----
//    private fun label(ts: Timestamp): String =
//        SimpleDateFormat("MMM d", Locale.getDefault()).format(ts.toDate())
//
//    private fun fullLabel(ts: Timestamp): String =
//        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(ts.toDate())
//}

