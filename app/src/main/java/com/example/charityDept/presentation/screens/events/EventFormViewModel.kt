// <app/src/main/java/com/example/zionkids/presentation/viewModels/events/EventFormViewModel.kt>
// /// CHANGED: preserve version/createdAt + keep non-form fields (eventParentId, tombstone fields, etc.) on edit.
// /// CHANGED: always set isDirty=true and updatedAt=now on save.
// /// CHANGED: never reset version to 0 on edits (carry existing version from Room).

package com.example.charityDept.presentation.screens.events

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.core.utils.FormValidatorUtil
import com.example.charityDept.core.Utils.GenerateId
import com.example.charityDept.core.sync.event.EventSyncScheduler
import com.example.charityDept.core.sync.event.EventSyncWorker
import com.example.charityDept.data.model.Event
import com.example.charityDept.data.model.EventStatus
import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventFormViewModel @Inject constructor(
    private val repo: OfflineEventsRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _ui = MutableStateFlow(EventFormUIState())
    val ui: StateFlow<EventFormUIState> = _ui.asStateFlow()

    private val _events = Channel<EventFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val parentCandidates: StateFlow<List<Event>> =
        repo.observeEventsForParentPick() // you'll add this in repo/dao
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    sealed interface EventFormEvent {
        data class Saved(val id: String) : EventFormEvent
        data class Error(val msg: String) : EventFormEvent
    }

    // ---- setters for fields ----
    fun onTitle(v: String) { _ui.value = _ui.value.copy(title = v) }
    fun onTeamName(v: String) { _ui.value = _ui.value.copy(teamName = v) }
    fun onTeamLeaderNames(v: String) { _ui.value = _ui.value.copy(teamLeaderNames = v) }
    fun onLeaderTelephone1(v: String) { _ui.value = _ui.value.copy(leaderTelephone1 = v) }
    fun onLeaderTelephone2(v: String) { _ui.value = _ui.value.copy(leaderTelephone2 = v) }
    fun onLeaderEmail(v: String) { _ui.value = _ui.value.copy(leaderEmail = v) }
    fun onDatePicked(ts: Timestamp) { _ui.value = _ui.value.copy(eventDate = ts) }
    fun onDatePickedMillis(millis: Long?) {
        _ui.value = _ui.value.copy(
            eventDate = millis?.let { Timestamp(it / 1000, ((it % 1000).toInt()) * 1_000_000) }
                ?: _ui.value.eventDate
        )
    }
    fun onLocation(v: String) { _ui.value = _ui.value.copy(location = v) }
    fun onNotes(v: String) { _ui.value = _ui.value.copy(notes = v) }
    fun onAdminId(v: String) { _ui.value = _ui.value.copy(adminId = v) }
    fun onStatus(v: EventStatus) { _ui.value = _ui.value.copy(eventStatus = v) }

//    fun onIsChild(v: Boolean) { _ui.value = _ui.value.copy(isChild = v) }
    fun onEventParentId(v: String) { _ui.value = _ui.value.copy(eventParentId = v) }

    fun onIsChild(v: Boolean) {
        _ui.value = _ui.value.copy(
            isChild = v,
            eventParentId = if (!v) "" else _ui.value.eventParentId // /// CHANGED: clear parentId when not a child
        )
    }

    fun ensureNewIdIfNeeded() {
        val curr = _ui.value
        if (curr.eventId.isBlank()) {
            val now = Timestamp.now()
            _ui.value = curr.copy(
                eventId = GenerateId.generateId("event"),
                createdAt = now,
                updatedAt = now,
                isNew = true
            )
        }
    }

    // ---- load existing (Room-first) ----
    fun loadForEdit(eventId: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        val existing = repo.getEventFast(eventId)
        _ui.value = if (existing != null) {
            _ui.value.from(existing).copy(loading = false, isNew = false)
        } else {
            _ui.value.copy(loading = false, error = "Event not found")
        }
    }

    fun seedNewChild(parentEventId: String) {
        ensureNewIdIfNeeded()
        _ui.value = _ui.value.copy(
            isNew = true,
            isChild = true,
            eventParentId = parentEventId
        )
    }

    /** Final save (create or update). Emits Saved(id) on success. */
    fun save() = viewModelScope.launch {
        _ui.value = _ui.value.copy(saving = true, error = null)

        val curr = _ui.value
        val titleRes    = FormValidatorUtil.validateName(curr.title)
        val locationRes = FormValidatorUtil.validateName(curr.location)
        val teamRes     = FormValidatorUtil.validateName(curr.teamName)

        val hasInvalid = listOf(titleRes, locationRes, teamRes).any { !it.isValid }
        if (hasInvalid) {
            _ui.value = curr.copy(
                saving = false,
                error = "Please fix the highlighted fields.",
                title = titleRes.value,             titleError = titleRes.error,
                location = locationRes.value,       locationError = locationRes.error,
                teamName = teamRes.value,           teamNameError = teamRes.error,
            )
            _events.trySend(EventFormEvent.Error("Missing or invalid fields"))
            return@launch
        }

        // Ensure ID for new event
        val ensured = _ui.value
        val finalId = if (ensured.eventId.isBlank()) {
            GenerateId.generateId("event").also {
                _ui.value = ensured.copy(eventId = it, isNew = true)
            }
        } else ensured.eventId

        val nowTs = Timestamp.now()

        // /// CHANGED: on edits, base off existing row so we preserve version + non-form fields.
        val existing: Event? = if (_ui.value.isNew) null else repo.getEventFast(finalId)

        val event = buildEvent(
            state = _ui.value,
            id = finalId,
            nowTs = nowTs,
            base = existing
        )

        runCatching { repo.createOrUpdateEvent(event, isNew = _ui.value.isNew) }
            .onSuccess {
                // /// CHANGED: keep createdAt from base for edits; updatedAt is nowTs.
                val createdAtToShow = existing?.createdAt ?: (event.createdAt)

                _ui.value = _ui.value.copy(
                    saving = false,
                    eventId = finalId,
                    isNew = false,
                    createdAt = createdAtToShow,
                    updatedAt = nowTs
                )

                val req = OneTimeWorkRequestBuilder<EventSyncWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .addTag("event_sync_now")
                    .build()

                WorkManager.getInstance(appContext).enqueueUniqueWork(
                    "event_sync_queue",
                    ExistingWorkPolicy.APPEND,
                    req
                )

                EventSyncScheduler.enqueuePushNow(appContext)
                _events.trySend(EventFormEvent.Saved(finalId))
            }
            .onFailure { e ->
                _ui.value = _ui.value.copy(saving = false, error = e.message ?: "Failed to save")
                _events.trySend(EventFormEvent.Error("Failed to save"))
            }
    }

    // ---- helpers ----
    // /// CHANGED: "base" preserves version/isDeleted/deletedAt/eventParentId/etc on edit.
    private fun buildEvent(
        state: EventFormUIState,
        id: String,
        nowTs: Timestamp,
        base: Event?
    ): Event {
        val seed = base ?: Event(
            eventId = id,
            createdAt = state.createdAt ?: nowTs,
            updatedAt = nowTs
        )

        return seed.copy(
            eventId = id,
            title = state.title ?: "",
            eventDate = state.eventDate,
            teamName = state.teamName,
            teamLeaderNames = state.teamLeaderNames,
            leaderTelephone1 = state.leaderTelephone1,
            leaderTelephone2 = state.leaderTelephone2,
            leaderEmail = state.leaderEmail,
            location = state.location ?: "",
            eventStatus = state.eventStatus,
            notes = state.notes ?: "",
            adminId = state.adminId ?: "",

            // ✅ ADDED: allow create/update to carry parent-child info
            isChild = state.isChild,
            eventParentId = state.eventParentId.ifBlank { seed.eventParentId },

            // touch updatedAt + mark dirty
            updatedAt = nowTs,
            isDirty = true,

            // keep createdAt + version
            createdAt = seed.createdAt,
            version = seed.version,

            // keep tombstone fields
            isDeleted = seed.isDeleted,
            deletedAt = seed.deletedAt
        )

    }

    private fun EventFormUIState.from(e: Event) = copy(
        eventId = e.eventId,
        title = e.title,
        eventDate = e.eventDate,
        teamName = e.teamName,
        teamLeaderNames = e.teamLeaderNames,
        leaderTelephone1 = e.leaderTelephone1,
        leaderTelephone2 = e.leaderTelephone2,
        leaderEmail = e.leaderEmail,
        location = e.location,
        eventStatus = e.eventStatus,
        notes = e.notes,
        adminId = e.adminId,

        // ✅ ADDED
        isChild = e.isChild,
        eventParentId = e.eventParentId,

        createdAt = e.createdAt,
        updatedAt = e.updatedAt
    )

}

data class EventFormUIState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val isNew: Boolean = true,

    // per-field errors
    val titleError: String? = null,
    val locationError: String? = null,
    val teamNameError: String? = null,
    val teamLeaderNameError: String? = null,

    val eventId: String = "",
    val title: String? = null,
    val eventDate: Timestamp = Timestamp.now(),
    val teamName: String = "",
    val teamLeaderNames: String = "",
    val leaderTelephone1: String = "",
    val leaderTelephone2: String = "",
    val leaderEmail: String = "",
    val location: String? = null,
    val eventStatus: EventStatus = EventStatus.SCHEDULED,
    val notes: String? = null,
    val adminId: String? = null,

    // ✅ ADDED: parent/child linkage
    val isChild: Boolean = false,
    val eventParentId: String = "",

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

