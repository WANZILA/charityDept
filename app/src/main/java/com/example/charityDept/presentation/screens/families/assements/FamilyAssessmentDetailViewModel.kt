package com.example.charityDept.presentation.screens.families.assessments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.AssessmentAnswer
import com.example.charityDept.domain.repositories.offline.OfflineAssessmentRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyAssessmentDetailUiState(
    val loading: Boolean = false,
    val items: List<AssessmentAnswer> = emptyList()
)

@HiltViewModel
class FamilyAssessmentDetailViewModel @Inject constructor(
    private val repo: OfflineAssessmentRepository
) : ViewModel() {

    fun session(
        familyId: String,
        generalId: String,
        mode: String,
        assessmentKey: String
    ): StateFlow<FamilyAssessmentDetailUiState> {
        return repo.observeSession(familyId, generalId, mode, assessmentKey)
            .map { list ->
                FamilyAssessmentDetailUiState(
                    loading = false,
                    items = list
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                FamilyAssessmentDetailUiState(loading = true)
            )
    }

    fun saveAllLocally(items: List<AssessmentAnswer>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            items.forEach { draft ->
                repo.upsertAnswerWithAudit(draft, uid)
            }
        }
    }

    fun softDeleteSession(answerIds: List<String>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            answerIds.forEach { answerId ->
                repo.softDeleteAnswer(answerId, uid)
            }
        }
    }
}