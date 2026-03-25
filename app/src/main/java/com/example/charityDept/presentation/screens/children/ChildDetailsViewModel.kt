package com.example.charityDept.presentation.screens.children

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.Child
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.domain.repositories.offline.OfflineChildrenRepository
//import com.example.charityDept.domain.sync.ChildrenSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
@HiltViewModel
class ChildDetailsViewModel @Inject constructor(
//    private val repo: ChildrenRepository
    private val repo: OfflineChildrenRepository,
    private val childDao: ChildDao,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    // one-shot events (navigation/snacks)
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    sealed interface Event {
        data object Deleted : Event
        data class Error(val msg: String) : Event
    }

    data class Ui(
        val loading: Boolean = true,
        val child: Child? = null,
        val error: String? = null,
        val deleting: Boolean = false,
        val deleted: Boolean = false,
        val lastRefreshed: Timestamp? = null // purely informational
    )
    private var observeJob: Job? = null
    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    /** Loads child: tries cache first, then server (repo handles that). */
    /** Loads & observes the child from Room (live updates). */
    fun load(childId: String) {
        // cancel previous observation if any
        observeJob?.cancel()
        _ui.value = Ui(loading = true)
        observeJob = viewModelScope.launch {
            childDao
                .observeById(childId)              // Flow<Child?>
                .distinctUntilChanged()
                .onEach { c ->
                    _ui.value = _ui.value.copy(
                        loading = false,
                        child = c,
                        error = if (c == null) "Child not found" else null,
                        lastRefreshed = Timestamp.now()
                    )
                }
                .catch { e ->
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = e.message ?: "Failed to load"
                    )
                }
                .collect { /* handled in onEach */ }
        }
    }


//    fun load(childId: String) = viewModelScope.launch {
//        // cancel previous observation if any
//        observeJob?.cancel()
//        _ui.value = Ui(loading = true)
//        runCatching { repo.getChildFast(childId) }
//            .onSuccess { c ->
//                _ui.value = _ui.value.copy(
//                    loading = false,
//                    child = c,
//                    error = if (c == null) "Child not found" else null,
//                    lastRefreshed = Timestamp.now()
//                )
//            }
//            .onFailure { e ->
//                _ui.value = _ui.value.copy(
//                    loading = false,
//                    error = e.message ?: "Failed to load"
//                )
//            }
//    }

    /** Fire-and-forget delete; emits navigation immediately, queues deletion (works offline). */
//    fun deleteChildOptimistic() {
//        val id = _ui.value.child?.childId ?: return
//        viewModelScope.launch { _events.send(Event.Deleted) }
//        try {
//            repo.enqueueCascadeDelete(id)
//        } catch (e: Exception) {
//            viewModelScope.launch {
//                _events.send(Event.Error("Delete queue failed: ${e.message}"))
//            }
//        }
//    }
//    fun deleteChildOptimistic() = viewModelScope.launch {
//        _ui.value = _ui.value.copy(deleting = true, error = null)
//        val id = _ui.value.child?.childId ?: return@launch
////        ui = ui.copy(deleting = true, error = null)
//        runCatching {
//            repo.deleteChildAndAttendances(id) // suspend version
//        }.onSuccess {
//            _ui.value = _ui.value.copy(deleting = false)
////            ui = ui.copy(deleting = false)
//            _events.trySend(ChildDetailsViewModel.Event.Deleted)
//        }.onFailure { e ->
//            _ui.value = _ui.value.copy(deleting = false, error = e.message ?: "Failed to delete")
////            ui = ui.copy(deleting = false, error = e.message ?: "Failed to delete")
//            _events.trySend(ChildDetailsViewModel.Event.Error("Failed to delete"))
//        }
//    }
//    fun deleteChildOptimistic() = viewModelScope.launch {
//        val id = _ui.value.child?.childId ?: return@launch
//        _ui.value = _ui.value.copy(deleting = true, error = null)
//
//        try {
//            repo.deleteChildAndAttendances(id)   // suspends, awaits completion
//            _events.trySend(ChildDetailsViewModel.Event.Deleted)  // include the id
//        } catch (e: Exception) {
//            _ui.value = _ui.value.copy(error = e.message ?: "Failed to delete")
//            _events.trySend(ChildDetailsViewModel.Event.Error("Failed to delete: ${e.message ?: ""}".trim()))
//        } finally {
//            _ui.value = _ui.value.copy(deleting = false)
//        }
//    }

    /** Fire-and-forget delete; marks in Room and lets worker cascade later. */
    fun deleteChildOptimistic() = viewModelScope.launch {
        val id = _ui.value.child?.childId ?: return@launch
        _ui.value = _ui.value.copy(deleting = true, error = null)
        try {
            // Hard delete locally and enqueue remote cascade
            repo.deleteChildCascade(id)

            _events.trySend(Event.Deleted)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message ?: "Failed to delete")
            _events.trySend(Event.Error("Failed to delete: ${e.message ?: ""}".trim()))
        } finally {
            _ui.value = _ui.value.copy(deleting = false)
        }
    }
//    fun deleteChildOptimistic() = viewModelScope.launch {
//        val id = _ui.value.child?.childId ?: return@launch
//        _ui.value = _ui.value.copy(deleting = true, error = null)
//        try {
//            repo.deleteChildAndAttendances(id)   // offline mark; sync will push later
//            _events.trySend(Event.Deleted)
//        } catch (e: Exception) {
//            _ui.value = _ui.value.copy(error = e.message ?: "Failed to delete")
//            _events.trySend(Event.Error("Failed to delete: ${e.message ?: ""}".trim()))
//        } finally {
//            _ui.value = _ui.value.copy(deleting = false)
//        }
//    }

    /** Simple refresh: no-op for Room observer; keeps timestamp UX. */
    fun refresh(childId: String) {
        _ui.value = _ui.value.copy(lastRefreshed = Timestamp.now())
        // No network call here; Room observer keeps it fresh.
        // If you want to force a remote re-hydration, trigger your worker here.
    }
//    /** Simple refresh (same behavior as load). */
//    fun refresh(childId: String) {
//        load(childId)
//    }
}

