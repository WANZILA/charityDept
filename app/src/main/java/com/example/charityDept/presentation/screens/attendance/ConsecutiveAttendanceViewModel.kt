@file:Suppress("FunctionName")

package com.example.charityDept.presentation.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.Attendance
import com.example.charityDept.data.model.AttendanceStatus
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.Event
// import com.example.charityDept.domain.repositories.online.AttendanceRepository           // /// CHANGED: remove online
// import com.example.charityDept.domain.repositories.online.ChildrenRepository            // /// CHANGED: remove online
// import com.example.charityDept.domain.repositories.online.EventsRepository              // /// CHANGED: remove online
import com.example.charityDept.domain.repositories.offline.OfflineAttendanceRepository     // /// CHANGED: use offline
import com.example.charityDept.domain.repositories.offline.OfflineChildrenRepository       // /// CHANGED: use offline
import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository         // /// CHANGED: use offline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class AbsenceStreak(
    val child: Child,
    val count: Int,
    val lastStatuses: List<AttendanceStatus>
)

data class PresentCount(
    val child: Child,
    val count: Int,
    val lastStatuses: List<AttendanceStatus>
)

/** New: reason + count for “Reasons” tab */
data class ReasonCount(
    val reason: String,
    val count: Int
)

@HiltViewModel
class ConsecutiveAttendanceViewModel @Inject constructor(
    private val childrenRepo: OfflineChildrenRepository,        // /// CHANGED: Room source
    private val attendanceRepo: OfflineAttendanceRepository,    // /// CHANGED: Room source
    private val eventsRepo: OfflineEventsRepository             // /// CHANGED: Room source
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    fun setQuery(text: String) { _query.value = text }

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _streaks = MutableStateFlow<List<AbsenceStreak>>(emptyList())
    val streaks: StateFlow<List<AbsenceStreak>> = _streaks.asStateFlow()

    private val _present = MutableStateFlow<List<PresentCount>>(emptyList())
    val present: StateFlow<List<PresentCount>> = _present.asStateFlow()

    private val _absent = MutableStateFlow<List<AbsenceStreak>>(emptyList())
    val absent: StateFlow<List<AbsenceStreak>> = _absent.asStateFlow()

    /** New: reasons state */
    private val _reasons = MutableStateFlow<List<ReasonCount>>(emptyList())
    val reasons: StateFlow<List<ReasonCount>> = _reasons.asStateFlow()

    private val _recentEventLabels = MutableStateFlow<List<String>>(emptyList())
    val recentEventLabels: StateFlow<List<String>> = _recentEventLabels.asStateFlow()

    val filteredPresent: StateFlow<List<PresentCount>> =
        combine(present, query) { list, q ->
            val n = q.trim().lowercase()
            if (n.isEmpty()) list else list.filter { pc ->
                (pc.child.fName?.lowercase()?.contains(n) == true) ||
                        (pc.child.lName?.lowercase()?.contains(n) == true) ||
                        pc.child.childId.lowercase().contains(n)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredAbsent: StateFlow<List<AbsenceStreak>> =
        combine(absent, query) { list, q ->
            val n = q.trim().lowercase()
            if (n.isEmpty()) list else list.filter { s ->
                (s.child.fName?.lowercase()?.contains(n) == true) ||
                        (s.child.lName?.lowercase()?.contains(n) == true) ||
                        s.child.childId.lowercase().contains(n)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Shared knobs
    private var lookbackEvents: Int = 2
    private var minStreak: Int = 1

    init { refresh() }

    fun setLookback(n: Int) { lookbackEvents = n.coerceAtLeast(1); refresh() }
    fun setMinStreak(n: Int) { minStreak = n.coerceAtLeast(1); refresh() }

    fun refresh() = viewModelScope.launch {
        try {
            _loading.value = true
            _error.value = null

            // Room snapshots
            val kidsSnap = childrenRepo.streamAllNotGraduated().first()          // /// CHANGED: Room
            val children = kidsSnap.children

            val allEvents = eventsRepo.streamEventSnapshots().first()            // /// CHANGED: Room
            val recent = allEvents
                .sortedByDescending { it.eventDate?.seconds ?: 0L }
                .take(lookbackEvents)

            if (recent.isEmpty()) {
                _present.value = emptyList()
                _absent.value = emptyList()
                _streaks.value = emptyList()
                _recentEventLabels.value = emptyList()
                _reasons.value = emptyList()
                return@launch
            }

            _recentEventLabels.value = recent.map { labelOf(it) }

            // Pull PRESENT rows from Room for each recent event
            val attByEvent: Map<String, List<Attendance>> =
                recent.associate { ev ->
                    ev.eventId to attendanceRepo.getPresentAttendanceOnce(ev.eventId)   // /// CHANGED: Room
                }

            // Pull ABSENT rows from Room for reasons aggregation
            val absByEvent: Map<String, List<Attendance>> =
                recent.associate { ev ->
                    ev.eventId to attendanceRepo.getAbsentAttendanceOnce(ev.eventId)    // /// CHANGED: Room
                }

            val presentByEvent: Map<String, Set<String>> =
                attByEvent.mapValues { (_, list) ->
                    list.asSequence()
                        .filter { it.status == AttendanceStatus.PRESENT }
                        .map { it.childId }
                        .toSet()
                }

            val presentCounts = ArrayList<PresentCount>(children.size)
            val absentStreaks = ArrayList<AbsenceStreak>(children.size)
            val summaryStreaks = ArrayList<AbsenceStreak>(children.size)

            for (child in children) {
                val statuses = recent.map { ev ->
                    if (presentByEvent[ev.eventId]?.contains(child.childId) == true)
                        AttendanceStatus.PRESENT
                    else
                        AttendanceStatus.ABSENT
                }

                val presentCount = statuses.count { it == AttendanceStatus.PRESENT }
                if (presentCount >= minStreak) {
                    presentCounts += PresentCount(child = child, count = presentCount, lastStatuses = statuses)
                }

                val streak = consecutiveAbsentsFromNewest(statuses)
                if (streak >= minStreak) {
                    val row = AbsenceStreak(child = child, count = streak, lastStatuses = statuses)
                    absentStreaks += row
                    summaryStreaks += row
                }
            }

            // Reasons: aggregate ABSENT notes across lookback (Room values)
            val counts = mutableMapOf<String, Int>()
            absByEvent.values.flatten().forEach { att ->
                val raw = att.notes?.trim().orEmpty()   // safe for null notes ✅
                if (raw.isNotEmpty()) {
                    val key = normalizeReason(raw)
                    counts[key] = (counts[key] ?: 0) + 1
                }
            }
            val reasonList = counts.entries
                .sortedByDescending { it.value }
                .map { ReasonCount(reason = it.key, count = it.value) }

            // Publish to UI
            _present.value = presentCounts.sortedByDescending { it.count }
            _absent.value  = absentStreaks.sortedByDescending { it.count }
            _streaks.value = summaryStreaks.sortedByDescending { it.count }
            _reasons.value = reasonList
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _loading.value = false
        }
    }

    private fun consecutiveAbsentsFromNewest(statuses: List<AttendanceStatus>): Int {
        var c = 0
        for (s in statuses) {
            if (s == AttendanceStatus.ABSENT) c++ else break
        }
        return c
    }

    private fun labelOf(ev: Event): String {
        val ts = ev.eventDate?.toDate()
        return when {
            ev.title?.isNotBlank() == true -> ev.title!!
            ts != null -> SimpleDateFormat("MMM d", Locale.getDefault()).format(ts)
            else -> "Event"
        }
    }

    /** Normalize notes so similar wording buckets together */
    private fun normalizeReason(s: String): String =
        s.lowercase(Locale.getDefault())
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9 \\-]"), "")
}

//@file:Suppress("FunctionName")
//
//package com.example.charityDept.presentation.viewModels.attendance
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.charityDept.data.model.Attendance
//import com.example.charityDept.data.model.AttendanceStatus
//import com.example.charityDept.data.model.Child
//import com.example.charityDept.data.model.Event
//import com.example.charityDept.domain.repositories.online.AttendanceRepository
//import com.example.charityDept.domain.repositories.online.ChildrenRepository
//import com.example.charityDept.domain.repositories.online.EventsRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.Locale
//import javax.inject.Inject
//
//data class AbsenceStreak(
//    val child: Child,
//    val count: Int,
//    val lastStatuses: List<AttendanceStatus>
//)
//
//data class PresentCount(
//    val child: Child,
//    val count: Int,
//    val lastStatuses: List<AttendanceStatus>
//)
//
///** New: reason + count for “Reasons” tab */
//data class ReasonCount(
//    val reason: String,
//    val count: Int
//)
//
//@HiltViewModel
//class ConsecutiveAttendanceViewModel @Inject constructor(
//    private val childrenRepo: ChildrenRepository,
//    private val attendanceRepo: AttendanceRepository,
//    private val eventsRepo: EventsRepository
//) : ViewModel() {
//
//    private val _query = MutableStateFlow("")
//    val query: StateFlow<String> = _query.asStateFlow()
//    fun setQuery(text: String) { _query.value = text }
//
//    private val _loading = MutableStateFlow(true)
//    val loading: StateFlow<Boolean> = _loading.asStateFlow()
//
//    private val _error = MutableStateFlow<String?>(null)
//    val error: StateFlow<String?> = _error.asStateFlow()
//
//    private val _streaks = MutableStateFlow<List<AbsenceStreak>>(emptyList())
//    val streaks: StateFlow<List<AbsenceStreak>> = _streaks.asStateFlow()
//
//    private val _present = MutableStateFlow<List<PresentCount>>(emptyList())
//    val present: StateFlow<List<PresentCount>> = _present.asStateFlow()
//
//    private val _absent = MutableStateFlow<List<AbsenceStreak>>(emptyList())
//    val absent: StateFlow<List<AbsenceStreak>> = _absent.asStateFlow()
//
//    /** New: reasons state */
//    private val _reasons = MutableStateFlow<List<ReasonCount>>(emptyList())
//    val reasons: StateFlow<List<ReasonCount>> = _reasons.asStateFlow()
//
//    private val _recentEventLabels = MutableStateFlow<List<String>>(emptyList())
//    val recentEventLabels: StateFlow<List<String>> = _recentEventLabels.asStateFlow()
//
//    val filteredPresent: StateFlow<List<PresentCount>> =
//        combine(present, query) { list, q ->
//            val n = q.trim().lowercase()
//            if (n.isEmpty()) list else list.filter { pc ->
//                (pc.child.fName?.lowercase()?.contains(n) == true) ||
//                        (pc.child.lName?.lowercase()?.contains(n) == true) ||
//                        pc.child.childId.lowercase().contains(n)
//            }
//        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
//
//    val filteredAbsent: StateFlow<List<AbsenceStreak>> =
//        combine(absent, query) { list, q ->
//            val n = q.trim().lowercase()
//            if (n.isEmpty()) list else list.filter { s ->
//                (s.child.fName?.lowercase()?.contains(n) == true) ||
//                        (s.child.lName?.lowercase()?.contains(n) == true) ||
//                        s.child.childId.lowercase().contains(n)
//            }
//        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
//
//    // Shared knobs
//    private var lookbackEvents: Int = 2
//    private var minStreak: Int = 1
//
//    init { refresh() }
//
//    fun setLookback(n: Int) { lookbackEvents = n.coerceAtLeast(1); refresh() }
//    fun setMinStreak(n: Int) { minStreak = n.coerceAtLeast(1); refresh() }
//
//    fun refresh() = viewModelScope.launch {
//        try {
//            _loading.value = true
//            _error.value = null
//
//            val kidsSnap = childrenRepo.streamAllNotGraduated().first()
//            val children = kidsSnap.children
//
//            val allEvents = eventsRepo.streamEventSnapshots().first()
//            val recent = allEvents
//                .sortedByDescending { it.eventDate?.seconds ?: 0L }
//                .take(lookbackEvents)
//
//            if (recent.isEmpty()) {
//                _present.value = emptyList()
//                _absent.value = emptyList()
//                _streaks.value = emptyList()
//                _recentEventLabels.value = emptyList()
//                _reasons.value = emptyList()              // ← New
//                return@launch
//            }
//
//            _recentEventLabels.value = recent.map { labelOf(it) }
//
//            // Fetch PRESENT rows (bandwidth-friendly)
//            val attByEvent: Map<String, List<Attendance>> =
//                recent.associate { ev ->
//                    ev.eventId to attendanceRepo.getPresentAttendanceOnce(ev.eventId)
//                }
//
//            // Fetch ABSENT rows (for reasons)
//            val absByEvent: Map<String, List<Attendance>> =
//                recent.associate { ev ->
//                    ev.eventId to attendanceRepo.getAbsentAttendanceOnce(ev.eventId)
//                }
//
//            val presentByEvent: Map<String, Set<String>> =
//                attByEvent.mapValues { (_, list) ->
//                    list.asSequence()
//                        .filter { it.status == AttendanceStatus.PRESENT }
//                        .map { it.childId }
//                        .toSet()
//                }
//
//            val presentCounts = ArrayList<PresentCount>(children.size)
//            val absentStreaks = ArrayList<AbsenceStreak>(children.size)
//            val summaryStreaks = ArrayList<AbsenceStreak>(children.size)
//
//            for (child in children) {
//                val statuses = recent.map { ev ->
//                    if (presentByEvent[ev.eventId]?.contains(child.childId) == true)
//                        AttendanceStatus.PRESENT
//                    else
//                        AttendanceStatus.ABSENT
//                }
//
//                val presentCount = statuses.count { it == AttendanceStatus.PRESENT }
//                if (presentCount >= minStreak) {
//                    presentCounts += PresentCount(child = child, count = presentCount, lastStatuses = statuses)
//                }
//
//                val streak = consecutiveAbsentsFromNewest(statuses)
//                if (streak >= minStreak) {
//                    val row = AbsenceStreak(child = child, count = streak, lastStatuses = statuses)
//                    absentStreaks += row
//                    summaryStreaks += row
//                }
//            }
//
//            // --- Reasons: aggregate notes from ABSENT rows across lookback ---
//            val counts = mutableMapOf<String, Int>()
//            absByEvent.values.flatten().forEach { att ->
//                val raw = att.notes?.trim().orEmpty()
//                if (raw.isNotEmpty()) {
//                    val key = normalizeReason(raw)
//                    counts[key] = (counts[key] ?: 0) + 1
//                }
//            }
//            val reasonList = counts.entries
//                .sortedByDescending { it.value }
//                .map { ReasonCount(reason = it.key, count = it.value) }
//
//            // Publish
//            _present.value = presentCounts.sortedByDescending { it.count }
//            _absent.value  = absentStreaks.sortedByDescending { it.count }
//            _streaks.value = summaryStreaks.sortedByDescending { it.count }
//            _reasons.value = reasonList                              // ← New
//        } catch (e: Exception) {
//            _error.value = e.message
//        } finally {
//            _loading.value = false
//        }
//    }
//
//    private fun consecutiveAbsentsFromNewest(statuses: List<AttendanceStatus>): Int {
//        var c = 0
//        for (s in statuses) {
//            if (s == AttendanceStatus.ABSENT) c++ else break
//        }
//        return c
//    }
//
//    private fun labelOf(ev: Event): String {
//        val ts = ev.eventDate?.toDate()
//        return when {
//            ev.title?.isNotBlank() == true -> ev.title!!
//            ts != null -> SimpleDateFormat("MMM d", Locale.getDefault()).format(ts)
//            else -> "Event"
//        }
//    }
//
//    /** Normalize notes so similar wording buckets together */
//    private fun normalizeReason(s: String): String =
//        s.lowercase(Locale.getDefault())
//            .trim()
//            .replace(Regex("\\s+"), " ")
//            .replace(Regex("[^a-z0-9 \\-]"), "")
//}

