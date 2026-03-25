// <app/src/main/java/com/example/zionkids/presentation/viewModels/events/EventDetailsViewModel.kt>
// /// CHANGED: Read from Room via EventDao.observeById(eventId).
// /// CHANGED: Soft-delete locally (Room) using Firestore Timestamp; worker will push.
// /// CHANGED: Removed online EventsRepository dependency.

package com.example.charityDept.presentation.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.EventDao // /// CHANGED
import com.example.charityDept.data.model.Event
import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
import com.google.firebase.Timestamp // /// CHANGED
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job // /// CHANGED
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest // /// CHANGED
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    private val repo: OfflineEventsRepository,
    private val eventDao: EventDao // /// CHANGED: inject DAO (Room source of truth)
) : ViewModel() {

    private val _events = Channel<EventDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    sealed interface EventDetailsEvent {
        data object Deleted : EventDetailsEvent
        data class Error(val msg: String) : EventDetailsEvent
    }

    /**
     * Holds the loaded [Event]. The Event model itself uses Firebase **Timestamp**
     * for `eventDate`, `createdAt`, and `updatedAt`.
     */
    data class Ui(
        val loading: Boolean = true,
        val event: Event? = null,
        val error: String? = null,
        val deleting: Boolean = false,
        val deleted: Boolean = false
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    // /// CHANGED: Keep only one active collector when event id changes
    private var observeJob: Job? = null

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _ui.update { Ui(loading = true) }
            eventDao.observeById(eventId).collectLatest { e ->
                _ui.update {
                    it.copy(
                        loading = false,
                        event = e,
                        error = if (e == null) "Event not found" else null,
                        deleted = false,
                        deleting = false
                    )
                }
            }
        }
    }

    /**
     * Optimistic delete: soft-delete in Room; background sync/worker pushes later.
     * Timestamps remain Firestore Timestamp.
     */
    fun deleteChildOptimistic() = viewModelScope.launch {
        val id = _ui.value.event?.eventId ?: return@launch
        _ui.value = _ui.value.copy(deleting = true, error = null)
        runCatching {
            val now = Timestamp.now()
            repo.deleteEventAndAttendances(id)
//           // eventDao.hardDelete(id) // /// CHANGED: local soft delete; worker will cascade/push
        }.onSuccess {
            _events.trySend(EventDetailsEvent.Deleted)
        }.onFailure { e ->
            _ui.value = _ui.value.copy(error = e.message ?: "Failed to delete")
            _events.trySend(EventDetailsEvent.Error("Failed to delete: ${e.message ?: ""}".trim()))
        }.also {
            _ui.value = _ui.value.copy(deleting = false)
        }
    }

    fun refresh(eventId: String) {
        // /// CHANGED: Reading from Room; rebind the observer (no network hop).
        load(eventId)
    }
}

