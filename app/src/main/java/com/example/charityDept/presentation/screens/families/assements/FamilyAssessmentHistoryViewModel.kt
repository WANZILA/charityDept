package com.example.charityDept.presentation.screens.families.assessments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.AssessmentSessionRow
import com.example.charityDept.data.local.dao.AssessmentToolOption
import com.example.charityDept.domain.repositories.offline.OfflineAssessmentRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyAssessmentHistoryUiState(
    val loading: Boolean = false,
    val sessions: List<AssessmentSessionRow> = emptyList()
)

@HiltViewModel
class FamilyAssessmentHistoryViewModel @Inject constructor(
    private val repo: OfflineAssessmentRepository
) : ViewModel() {

    private val sessionsCache = mutableMapOf<String, StateFlow<FamilyAssessmentHistoryUiState>>()
    private val toolsCache = mutableMapOf<String, StateFlow<List<AssessmentToolOption>>>()

    fun sessions(familyId: String, mode: String): StateFlow<FamilyAssessmentHistoryUiState> {
        val key = "$familyId|$mode"
        return sessionsCache.getOrPut(key) {
            repo.observeSessionRows(familyId, mode)
                .map { FamilyAssessmentHistoryUiState(loading = false, sessions = it) }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    FamilyAssessmentHistoryUiState(loading = true)
                )
        }
    }

    fun tools(mode: String): StateFlow<List<AssessmentToolOption>> {
        return toolsCache.getOrPut(mode) {
            repo.observeAvailableAssessmentTools(mode)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    emptyList()
                )
        }
    }

    fun startNewAssessment(familyId: String, mode: String, onDone: (generalId: String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val generalId = repo.startNewSession(familyId, mode, uid)
            onDone(generalId)
        }
    }
}