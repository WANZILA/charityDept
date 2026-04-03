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
import com.example.charityDept.domain.repositories.offline.OfflineAttendanceRepository
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.example.charityDept.domain.repositories.offline.EligibleCounts

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
    val error: String? = null
)

@HiltViewModel
class SingleEventDashboardViewModel @Inject constructor(
    private val eventsRepo: OfflineEventsRepository,
    private val attendanceRepo: OfflineAttendanceRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SingleEventDashboardUiState())
    val ui: StateFlow<SingleEventDashboardUiState> = _ui.asStateFlow()

    private var observeJob: Job? = null

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _ui.value = SingleEventDashboardUiState(loading = true)

            combine(
                eventsRepo.observeEventById(eventId),
                eventsRepo.observeChildrenForParent(eventId),
                eventsRepo.observeChildCountForParent(eventId)
            ) { event, children, childCount ->
                Triple(event, children, childCount)
            }
                .flatMapLatest { (event, children, childCount) ->
                    if (event == null) {
                        flowOf(
                            SingleEventDashboardUiState(
                                loading = false,
                                event = null,
                                childEvents = emptyList(),
                                childCount = 0,
                                frequentAttendees = emptyList(),
                                error = "Event not found"
                            )
                        )
                    } else {
                        val scopedEventIds = if (event.isChild) {
                            listOf(event.eventId)
                        } else {
                            listOf(event.eventId) + children.map { it.eventId }
                        }

                        attendanceRepo.observeFrequentAttendeesForEvents(scopedEventIds)
                            .map { frequentRows ->
                                val summary = attendanceRepo.eligibleCountsForEvent(
                                    eventId = event.eventId,
                                    eventDate = event.eventDate
                                )

                                SingleEventDashboardUiState(
                                    loading = false,
                                    event = event,
                                    childEvents = children,
                                    childCount = childCount,
                                    attendanceSummary = summary,
                                    frequentAttendees = frequentRows,
                                    error = null
                                )
                            }

//                        attendanceRepo.observeFrequentAttendeesForEvents(scopedEventIds)
//                            .map { frequentRows ->
//                                SingleEventDashboardUiState(
//                                    loading = false,
//                                    event = event,
//                                    childEvents = children,
//                                    childCount = childCount,
//                                    frequentAttendees = frequentRows,
//                                    error = null
//                                )
//                            }
                    }
                }
                .collectLatest { next ->
                    _ui.value = next
                }
        }
    }
}