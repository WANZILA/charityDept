package com.example.charityDept.presentation.viewModels.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.UserProfile
import com.example.charityDept.domain.repositories.online.UsersRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val repo: UsersRepository
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
        val user: UserProfile? = null,
        val error: String? = null,
        val deleting: Boolean = false,
        val deleted: Boolean = false,
        val lastRefreshed: Timestamp? = null // purely informational
    )


    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    /** Loads child: tries cache first, then server (repo handles that). */
    fun load(uid: String) = viewModelScope.launch {
        _ui.value = Ui(loading = true)
        runCatching { repo.getUserFast(uid) }
            .onSuccess { c ->
                _ui.value = _ui.value.copy(
                    loading = false,
                    user = c,
                    error = if (c == null) "user not found" else null,
                    lastRefreshed = Timestamp.now()
                )
            }
            .onFailure { e ->
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load"
                )
            }
    }

    /** Fire-and-forget delete; emits navigation immediately, queues deletion (works offline). */

    fun softDeleteUser() = viewModelScope.launch {
        val id = _ui.value.user?.uid ?: return@launch
        val email = _ui.value.user?.email ?: return@launch
        _ui.value = _ui.value.copy(deleting = true, error = null)

        try {
            repo.softDeleteUserAndLog(uid = id, email = email, reason = "Admin request")   // suspends, awaits completion
            _events.trySend(UserDetailViewModel.Event.Deleted)  // include the id
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message ?: "Failed to delete")
            _events.trySend(UserDetailViewModel.Event.Error("Failed to delete: ${e.message ?: ""}".trim()))
        } finally {
            _ui.value = _ui.value.copy(deleting = false)
        }
    }

    /** Simple refresh (same behavior as load). */
    fun refresh(uid: String) {
        load(uid)
    }
}

