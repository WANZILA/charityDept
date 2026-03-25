// <app/src/main/java/com/example/zionkids/presentation/viewModels/HomeDashboardViewModel.kt>
// /// CHANGED: Read KPI counters via KpiDao.observe(...), and ensure keys exist on start.

package com.example.charityDept.presentation.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.KpiDao
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.EducationPreference
import com.example.charityDept.data.model.Event
import com.example.charityDept.data.model.EventStatus
import com.example.charityDept.data.model.Reply
import com.example.charityDept.domain.repositories.offline.OfflineChildrenRepository
import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class HomeUi(
    val loading: Boolean = true,
    val error: String? = null,
    val childrenTotal: Int = 0,
    val childrenNewThisMonth: Int = 0,
    val childrenGraduated: Int = 0,
    val resettled: Int = 0,
    val toBeResettled: Int = 0,
    val eventsToday: Int = 0,
    val eventsActiveNow: Int = 0,
    val acceptedChrist: Int = 0,
    val yetToAcceptChrist: Int = 0,
    val happeningToday: List<Event> = emptyList(),
    val eduDist: Map<EducationPreference, Int> = emptyMap(),
    val regionTop: List<Pair<String, Int>> = emptyList(),
    val streetTop: List<Pair<String, Int>> = emptyList()
)

private const val KPI_EVENTS_TOTAL = "events_total"
private fun todayKey(): String {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return "events_day_" + sdf.format(Date())
}

@HiltViewModel
class HomeDashboardViewModel @Inject constructor(
    private val childrenRepo: OfflineChildrenRepository,
    private val eventsRepo: OfflineEventsRepository,
    private val kpiDao: KpiDao
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUi())
    val ui: StateFlow<HomeUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            // Make sure counter rows exist so observe() never fails
            ensureKeys()

            combine(
                kpiDao.observe(KPI_EVENTS_TOTAL).map { it.toInt() }.catch { emit(0) },
                kpiDao.observe(todayKey()).map { it.toInt() }.catch { emit(0) },
                childrenRepo.streamChildren(),
                eventsRepo.streamEventSnapshots()
            ) { eventsTotal, eventsTodayCnt, children, events ->
                val computed = compute(children, events)
                computed.copy(
                    eventsToday = eventsTodayCnt
                    // You can expose eventsTotal somewhere if you add it to HomeUi
                )
            }
                .onStart { _ui.value = _ui.value.copy(loading = true, error = null) }
                .catch { e ->
                    _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Failed to load")
                }
                .collect { state ->
                    _ui.value = state.copy(loading = false, error = null)
                }
        }
    }

    private suspend fun ensureKeys() {
        // Idempotent: creates rows if missing, else does nothing.
        kpiDao.ensureKey(KPI_EVENTS_TOTAL)
        kpiDao.ensureKey(todayKey())
    }

    // --- helpers unchanged below ---
    private fun Timestamp?.millisOrZero(): Long = this?.toDate()?.time ?: 0L
    private fun Event.timeMillis(): Long = this.eventDate.toDate().time

    private fun compute(children: List<Child>, events: List<Event>): HomeUi {
        val nowMillis = System.currentTimeMillis()
        val sod = startOfDay(nowMillis)
        val eod = endOfDay(nowMillis)
        val (mStart, mEnd) = monthBounds(nowMillis)

        val childrenTotal = children.size
        val childrenNewThisMonth = children.count { it.createdAt.millisOrZero() in mStart..mEnd }
        val childrenGraduated = children.count { it.graduated == Reply.YES }
        val resettled = children.count { it.resettled }
        val toBeResettled = children.count { !it.resettled }

        val acceptedChrist = children.count { c ->
            c.acceptedJesus == Reply.YES &&
                    c.acceptedJesusDate?.toDate()?.time?.let { it in mStart..mEnd } == true
        }
        val yetToAcceptChrist = children.count { it.acceptedJesus == Reply.NO }

        val eventsActiveNow = events.count { it.eventStatus == EventStatus.ACTIVE }
        val happeningToday = events
            .filter { e -> e.timeMillis() in sod..eod }
            .sortedBy { it.timeMillis() }
            .take(4)

        return HomeUi(
            loading = false,
            childrenTotal = childrenTotal,
            childrenNewThisMonth = childrenNewThisMonth,
            childrenGraduated = childrenGraduated,
            resettled = resettled,
            toBeResettled = toBeResettled,
            eventsToday = 0, // replaced by counter in init()
            eventsActiveNow = eventsActiveNow,
            happeningToday = happeningToday,
            acceptedChrist = acceptedChrist,
            yetToAcceptChrist = yetToAcceptChrist,
            eduDist = children.groupingBy { it.educationPreference }.eachCount(),
            regionTop = children.groupingBy { it.region ?: "Unknown" }.eachCount().entries
                .sortedByDescending { it.value }.take(3).map { it.key to it.value },
            streetTop = children.groupingBy { it.region ?: "Unknown" }.eachCount().entries
                .sortedByDescending { it.value }.take(3).map { it.key to it.value }
        )
    }

    private fun startOfDay(time: Long): Long = Calendar.getInstance().run {
        timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); timeInMillis
    }
    private fun endOfDay(time: Long): Long = Calendar.getInstance().run {
        timeInMillis = time; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999); timeInMillis
    }
    private fun monthBounds(time: Long): Pair<Long, Long> {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        c.set(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        val start = c.timeInMillis; c.add(Calendar.MONTH, 1); c.add(Calendar.MILLISECOND, -1)
        return start to c.timeInMillis
    }
}

