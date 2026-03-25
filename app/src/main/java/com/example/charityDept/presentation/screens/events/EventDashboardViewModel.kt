package com.example.charityDept.presentation.screens.events


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.Event
import com.example.charityDept.data.model.EventStatus
import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class EventTab { UPCOMING, PAST, NEW }

data class EventDashboardUi(
    val loading: Boolean = true,
    val error: String? = null,
    val all: List<Event> = emptyList(),
    val selectedTab: EventTab = EventTab.UPCOMING,
    val statusFilter: Set<EventStatus> = setOf(EventStatus.SCHEDULED, EventStatus.ACTIVE), // default show not-completed
    // KPI Cards
    val totalThisMonth: Int = 0,  //num of events in this month
    val nextEvent: Event? = null,
    val activeNowCount: Int = 0,
    // Derived list for current tab + filter
    val visible: List<Event> = emptyList()
)

@HiltViewModel
class EventDashboardViewModel @Inject constructor(
    private val repo: OfflineEventsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(EventDashboardUi())
    val ui: StateFlow<EventDashboardUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo.streamEventSnapshots() // Flow<List<Event>> with Timestamp fields
                .onStart { _ui.update { it.copy(loading = true, error = null) } }
                .catch { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Failed to load") } }
                .collect { list ->
                    _ui.update { s ->
                        val computed = computeKpis(list, s.selectedTab, s.statusFilter)
                        s.copy(
                            loading = false,
                            error = null,
                            all = list,
                            totalThisMonth = computed.totalThisMonth,  //num of events in this month
                            nextEvent = computed.nextEvent,
                            activeNowCount = computed.activeNowCount,
                            visible = computed.visible
                        )
                    }
                }
        }
    }

    fun setTab(tab: EventTab) {
        _ui.update { s ->
            val computed = computeKpis(s.all, tab, s.statusFilter)
            s.copy(selectedTab = tab, visible = computed.visible, activeNowCount = computed.activeNowCount)
        }
    }

    fun toggleStatusFilter(status: EventStatus) {
        _ui.update { s ->
            val newSet = s.statusFilter.toMutableSet().apply {
                if (contains(status)) remove(status) else add(status)
            }
            val computed = computeKpis(s.all, s.selectedTab, newSet)
            s.copy(statusFilter = newSet, visible = computed.visible, activeNowCount = computed.activeNowCount)
        }
    }

    private data class Kpis(
        val totalThisMonth: Int,
        val nextEvent: Event?,
        val activeNowCount: Int,
        val visible: List<Event>
    )

    private fun computeKpis(all: List<Event>, tab: EventTab, filter: Set<EventStatus>): Kpis {
        val now = System.currentTimeMillis()
        val sod = startOfDay(now)
        val (startMonth, endMonth) = monthBounds(now)

        fun Timestamp?.millisOr(min: Long = Long.MIN_VALUE): Long =
            this?.toDate()?.time ?: min
        fun Event.eventMillis(): Long = eventDate.toDate().time

        //num of events in this month
        val totalThisMonth = all.count { it.eventMillis() in startMonth..endMonth }

        val upcoming = all
            .filter { it.eventMillis() >= sod }
            .sortedBy { it.eventMillis() }
        val nextEvent = upcoming.firstOrNull()

        val activeNowCount = all.count { it.eventStatus == EventStatus.ACTIVE }

        val visible: List<Event> = when (tab) {
            EventTab.UPCOMING -> all
                .filter { it.eventMillis() >= sod }
                .filter { it.eventStatus in filter }
                .sortedBy { it.eventMillis() }

            EventTab.PAST -> all
                .filter { it.eventMillis() < sod }
                .filter { it.eventStatus in filter }
                .sortedByDescending { it.eventMillis() }

            EventTab.NEW -> all
                // "New" = created this month; keeps timestamps all through
                .filter { it.createdAt.millisOr() in startMonth..endMonth }
                .filter { it.eventStatus in filter }
                .sortedByDescending { it.createdAt.millisOr() }
        }

        return Kpis(
            totalThisMonth = totalThisMonth,
            nextEvent = nextEvent,
            activeNowCount = activeNowCount,
            visible = visible
        )
    }

    private fun startOfDay(time: Long): Long = Calendar.getInstance().run {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private fun monthBounds(time: Long): Pair<Long, Long> {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val start = c.timeInMillis
        c.add(Calendar.MONTH, 1)
        c.add(Calendar.MILLISECOND, -1)
        val end = c.timeInMillis
        return start to end
    }
}

