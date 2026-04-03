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

data class SingleEventDashboardUiState(
    val loading: Boolean = true,
    val event: Event? = null,
    val childEvents: List<Event> = emptyList(),
    val childCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class SingleEventDashboardViewModel @Inject constructor(
    private val repo: OfflineEventsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SingleEventDashboardUiState())
    val ui: StateFlow<SingleEventDashboardUiState> = _ui.asStateFlow()

    private var observeJob: Job? = null

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _ui.value = SingleEventDashboardUiState(loading = true)

            combine(
                repo.observeEventById(eventId),
                repo.observeChildrenForParent(eventId),
                repo.observeChildCountForParent(eventId)
            ) { event, children, childCount ->
                Triple(event, children, childCount)
            }.collectLatest { (event, children, childCount) ->
                _ui.value = SingleEventDashboardUiState(
                    loading = false,
                    event = event,
                    childEvents = children,
                    childCount = childCount,
                    error = if (event == null) "Event not found" else null
                )
            }
        }
    }
}