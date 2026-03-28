package com.example.charityDept.presentation.screens.families

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.Family
import com.example.charityDept.data.model.FamilyMember
import com.example.charityDept.domain.repositories.offline.OfflineFamiliesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyDetailsUiState(
    val loading: Boolean = true,
    val deleting: Boolean = false,
    val family: Family? = null,
    val members: List<FamilyMember> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class FamilyDetailsViewModel @Inject constructor(
    private val repo: OfflineFamiliesRepository
) : ViewModel() {

    sealed class Event {
        object Deleted : Event()
        data class Error(val msg: String) : Event()
    }

    private val _ui = MutableStateFlow(FamilyDetailsUiState())
    val ui: StateFlow<FamilyDetailsUiState> = _ui.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentFamilyId: String? = null

    fun load(familyId: String) {
        currentFamilyId = familyId
        viewModelScope.launch {
            repo.observeFamilyDetails(familyId).collect { snap ->
                _ui.value = _ui.value.copy(
                    loading = false,
                    family = snap.family,
                    members = snap.members,
                    error = if (snap.family == null) "Family not found" else null
                )
            }
        }
    }

    fun refresh(familyId: String) {
        load(familyId)
    }

    fun deleteFamilyOptimistic() {
        val id = currentFamilyId ?: return
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(deleting = true)
                repo.softDeleteFamily(id)
                _ui.value = _ui.value.copy(deleting = false)
                _events.send(Event.Deleted)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    deleting = false,
                    error = t.message ?: "Failed to delete family"
                )
                _events.send(Event.Error(t.message ?: "Failed to delete family"))
            }
        }
    }
}