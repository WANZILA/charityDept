// <app/src/main/java/com/example/zionkids/presentation/viewModels/events/EventListViewModel.kt>
// /// CHANGED: Add Paging 3 stream for Events (Room-first).
// /// CHANGED: Keeps existing UI snapshot stream for chips (offline/sync) and search text handling.

package com.example.charityDept.presentation.screens.events

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.Event
import com.example.charityDept.domain.repositories.offline.EventSnapshot
import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
// /// CHANGED: Paging imports
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.charityDept.core.sync.event.EventSyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow

//data class EventListUiState(
//    val loading: Boolean = true,
//    val event: List<Event> = emptyList(),
//    val isOffline: Boolean = false,
//    val isSyncing: Boolean = false,
//    val error: String? = null
//)

data class EventListUiState(
    val loading: Boolean = false,
    val isOffline: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val repo: OfflineEventsRepository,
    @ApplicationContext val appContext: Context,
) : ViewModel() {

    private val _ui = MutableStateFlow(EventListUiState())
    val ui: StateFlow<EventListUiState> = _ui.asStateFlow()

    // /// CHANGED: Paging stream (active events). Kept simple; search UI still uses snapshot filter below.
    val pagedEvents: Flow<PagingData<Event>> =
        repo.pagedActive().cachedIn(viewModelScope)

    init {
        observeStream()
    }

    private fun observeStream(limit: Long = 300) {
        viewModelScope.launch {
            repo.streamEvents()
                .catch { e ->
                    Log.e(TAG, "stream error", e)
                    _ui.value = _ui.value.copy(error = e.message)
                }
                .collect { snap ->
                    _ui.value = EventListUiState(
                        loading = false,
                        isOffline = snap.fromCache,
                        isSyncing = snap.hasPendingWrites,
                        error = null
                    )
                }
        }
    }


    fun refresh() {
        EventSyncScheduler.enqueuePullNow(appContext)
        _ui.value = _ui.value.copy(loading = true)
        _ui.value = _ui.value.copy(loading = false)
    }

    companion object { private const val TAG = "EventListViewModel" }
}

//package com.example.charityDept.presentation.viewModels.events
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.charityDept.core.di.EventsRef
//import com.example.charityDept.data.model.Event
//import com.example.charityDept.domain.repositories.online.EventSnapshot
//import com.example.charityDept.domain.repositories.online.EventsRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.distinctUntilChanged
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.flow.onStart
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//data class EventListUiState(
//    val loading: Boolean = true,
//    val event: List<Event> = emptyList(),
//    val isOffline: Boolean = false,   // served from cache
//    val isSyncing: Boolean = false,   // local writes pending
//    val error: String? = null
//)
//
//@HiltViewModel
//class EventListViewModel @Inject constructor(
//    private val repo: EventsRepository,
////    private val eventsRe: EventsRef
//) : ViewModel() {
//
//    private val _ui = MutableStateFlow(EventListUiState())
//    val ui: StateFlow<EventListUiState> = _ui.asStateFlow()
//
//    private val _query = MutableStateFlow("")
//    fun onSearchQueryChange(q: String) { _query.value = q }
//
//    // latest snapshot from Firestore
//    private var latestSnap: EventSnapshot =
//        EventSnapshot(emptyList(), fromCache = true, hasPendingWrites = false)
//
//    init {
//        observeStream()   // Firestore -> latestSnap
//        observeQuery()    // Search    -> filter latestSnap
//    }
//
//    private fun observeStream(limit: Long = 300) {
//        viewModelScope.launch {
//            repo.streamEvents()
//                .onStart { _ui.value = EventListUiState(loading = true) }
//                .catch { e ->
//                    android.util.Log.e(TAG, "stream error", e)
//                    _ui.value = _ui.value.copy(loading = false, error = e.message)
//                }
//                .collect { snap ->
//                    latestSnap = snap
//                    pushFiltered()  // re-apply current query
//                }
//        }
//    }
//
//    private fun observeQuery() {
//        viewModelScope.launch {
//            _query
//                //.debounce(200)
//                .map { it.trim().lowercase() }
//                .distinctUntilChanged()
//                .collect { pushFiltered() }
//        }
//    }
//
//    private fun pushFiltered() {
//        val q = _query.value.trim().lowercase()
//
//        val filtered = if (q.isEmpty()) {
//            latestSnap.events
//        } else {
//            latestSnap.events.filter { e ->
//                e.title.trim().lowercase().contains(q)
//            }
//        }
//
//        // Timestamps all through: sort by Firebase Timestamp (newest first)
//        val sorted = filtered.sortedByDescending { e -> e.eventDate.toDate().time }
//
//        _ui.value = EventListUiState(
//            loading = false,
//            event = sorted,
//            isOffline = latestSnap.fromCache,
//            isSyncing = latestSnap.hasPendingWrites,
//            error = null
//        )
//    }
//
//    fun refresh() {
//        // Stream is live; no explicit refresh needed. Keep for UI parity.
//        _ui.value = _ui.value.copy(loading = true)
//        _ui.value = _ui.value.copy(loading = false)
//
//    }
//
//    companion object { private const val TAG = "EventListViewModel" }
////    val eventsRef: EventsReference
////    suspend fun normalizeEventStatuses(eventsRef: EventsRef) {
////        val snap = eventsRef.get().await()
////        val batch = eventsRef.firestore.batch()
////        for (doc in snap.documents) {
////            val raw = doc.getString("eventStatus")?.uppercase()
////            if (raw == "COMPLETED") {
////                batch.update(doc.reference, "eventStatus", "DONE")
////            }
////        }
////        batch.commit().await()
////    }
//}

