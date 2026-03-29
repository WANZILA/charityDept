package com.example.charityDept.presentation.screens.families

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.Family
import com.example.charityDept.data.model.FamilyMember
import com.example.charityDept.domain.repositories.offline.OfflineFamiliesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SingleFamilyDashboardUiState(
    val loading: Boolean = true,
    val family: Family? = null,
    val members: List<FamilyMember> = emptyList(),
    val error: String? = null
) {
    val memberCount: Int get() = members.size
}

@HiltViewModel
class SingleFamilyDashboardViewModel @Inject constructor(
    private val repo: OfflineFamiliesRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SingleFamilyDashboardUiState())
    val ui: StateFlow<SingleFamilyDashboardUiState> = _ui.asStateFlow()

    fun load(familyId: String) {
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
}