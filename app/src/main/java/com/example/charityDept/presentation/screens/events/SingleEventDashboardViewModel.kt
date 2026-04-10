package com.example.charityDept.presentation.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.Event
import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.charityDept.data.local.projection.EventFrequentAttendeeRow
import com.example.charityDept.data.model.AttendanceStatus
import com.example.charityDept.domain.repositories.offline.OfflineAttendanceRepository
import com.example.charityDept.domain.repositories.offline.EligibleCounts
import com.example.charityDept.domain.repositories.offline.OfflineChildrenRepository
import kotlinx.coroutines.Dispatchers

data class SingleEventDashboardUiState(
    val loading: Boolean = true,
    val event: Event? = null,
    val childEvents: List<Event> = emptyList(),
    val childCount: Int = 0,
    val attendanceSummary: EligibleCounts = EligibleCounts(
        totalEligible = 0,
        presentEligible = 0
    ),
    val frequentAttendees: List<EventFrequentAttendeeRow> = emptyList(),
    val childEventsLoaded: Boolean = false,
    val frequentAttendeesLoaded: Boolean = false,
    val childEventsLoading: Boolean = false,
    val frequentAttendeesLoading: Boolean = false,
    val error: String? = null
)

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@HiltViewModel
class SingleEventDashboardViewModel @Inject constructor(
    private val eventsRepo: OfflineEventsRepository,
    private val attendanceRepo: OfflineAttendanceRepository,
    private val childrenRepo: OfflineChildrenRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SingleEventDashboardUiState())
    val ui: StateFlow<SingleEventDashboardUiState> = _ui.asStateFlow()

    private var observeJob: Job? = null
    private var childEventsJob: Job? = null
    private var frequentAttendeesJob: Job? = null

    fun load(eventId: String) {
        observeJob?.cancel()
        childEventsJob?.cancel()
        frequentAttendeesJob?.cancel()

        observeJob = viewModelScope.launch {
            _ui.value = SingleEventDashboardUiState(loading = true)

            combine(
                eventsRepo.observeEventById(eventId),
                eventsRepo.observeChildCountForParent(eventId),
                childrenRepo.streamAllNotGraduated(),
                attendanceRepo.streamAttendanceForEvent(eventId)
            ) { event, childCount, childrenSnap, attendanceSnap ->
                Quadruple(event, childCount, childrenSnap, attendanceSnap)
            }.collectLatest { (event, childCount, childrenSnap, attendanceSnap) ->
                if (event == null) {
                    _ui.value = SingleEventDashboardUiState(
                        loading = false,
                        event = null,
                        childEvents = emptyList(),
                        childCount = 0,
                        frequentAttendees = emptyList(),
                        childEventsLoaded = false,
                        frequentAttendeesLoaded = false,
                        childEventsLoading = false,
                        frequentAttendeesLoading = false,
                        error = "Event not found"
                    )
                } else {
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching {
                            if (attendanceRepo.getAttendanceOnce(event.eventId).isEmpty()) {
                                attendanceRepo.hydrateEvent(event.eventId)
                            }
                        }
                    }

                    val attMap = attendanceSnap.attendance.associateBy { it.childId }
                    val merged = childrenSnap.children.map { child ->
                        attMap[child.childId]
                    }

                    val total = merged.size
                    val present = merged.count { it?.status == AttendanceStatus.PRESENT }

                    _ui.value = _ui.value.copy(
                        loading = false,
                        event = event,
                        childCount = childCount,
                        attendanceSummary = EligibleCounts(
                            totalEligible = total,
                            presentEligible = present
                        ),
                        error = null
                    )
                }
            }
        }
    }


    fun loadChildEvents(eventId: String) {
        if (_ui.value.childEventsLoaded || _ui.value.childEventsLoading) return

        _ui.value = _ui.value.copy(childEventsLoading = true)

        childEventsJob?.cancel()
        childEventsJob = viewModelScope.launch {
            eventsRepo.observeChildrenForParent(eventId)
                .collectLatest { children ->
                    _ui.value = _ui.value.copy(
                        childEvents = children,
                        childEventsLoaded = true,
                        childEventsLoading = false
                    )
                }
        }
    }

    fun loadFrequentAttendees(eventId: String) {
        if (_ui.value.frequentAttendeesLoaded || _ui.value.frequentAttendeesLoading) return

        _ui.value = _ui.value.copy(frequentAttendeesLoading = true)

        frequentAttendeesJob?.cancel()
        frequentAttendeesJob = viewModelScope.launch {
            combine(
                eventsRepo.observeEventById(eventId),
                eventsRepo.observeChildrenForParent(eventId)
            ) { event, children ->
                event to children
            }.collectLatest { (event, children) ->
                if (event == null) {
                    _ui.value = _ui.value.copy(
                        frequentAttendees = emptyList(),
                        frequentAttendeesLoaded = true,
                        frequentAttendeesLoading = false
                    )
                } else {
                    val scopedEventIds = if (event.isChild) {
                        listOf(event.eventId)
                    } else {
                        listOf(event.eventId) + children.map { it.eventId }
                    }

                    attendanceRepo.observeFrequentAttendeesForEvents(scopedEventIds)
                        .collectLatest { frequentRows ->
                            _ui.value = _ui.value.copy(
                                frequentAttendees = frequentRows,
                                frequentAttendeesLoaded = true,
                                frequentAttendeesLoading = false
                            )
                        }
                }
            }
        }
    }
}