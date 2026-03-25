package com.example.charityDept.presentation.screens.children.assessments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.AssessmentSessionRow
import com.example.charityDept.domain.repositories.offline.OfflineAssessmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

data class AssessmentHistoryUiState(
    val loading: Boolean = false,
    val sessions: List<AssessmentSessionRow> = emptyList()
)
@HiltViewModel
class ChildAssessmentHistoryViewModel @Inject constructor(
    private val repo: OfflineAssessmentRepository
) : ViewModel() {

    private val sessionsCache = mutableMapOf<String, StateFlow<AssessmentHistoryUiState>>()

    fun sessions(childId: String, mode: String): StateFlow<AssessmentHistoryUiState> {
        val key = "$childId|$mode"
        return sessionsCache.getOrPut(key) {
            repo.observeSessionRows(childId, mode)
                .map { AssessmentHistoryUiState(loading = false, sessions = it) }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    AssessmentHistoryUiState(loading = true)
                )
        }
    }

    fun startNewAssessment(childId: String, mode: String, onDone: (generalId: String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val generalId = repo.startNewSession(childId, mode, uid)
            onDone(generalId)
        }
    }
}

