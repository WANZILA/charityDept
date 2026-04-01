package com.example.charityDept.presentation.screens.children.assessments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.AssessmentAnswer
import com.example.charityDept.domain.repositories.offline.OfflineAssessmentRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AssessmentDetailUiState(
    val loading: Boolean = false,
    val items: List<AssessmentAnswer> = emptyList()
)

@HiltViewModel
class ChildAssessmentDetailViewModel @Inject constructor(
    private val repo: OfflineAssessmentRepository
) : ViewModel() {

    fun session(
        childId: String,
        generalId: String,
        mode: String,
        assessmentKey: String
    ): StateFlow<AssessmentDetailUiState> {
        return repo.observeSession(childId, generalId, mode, assessmentKey)
            .map { list ->
                AssessmentDetailUiState(
                    loading = false,
                    items = list
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                AssessmentDetailUiState(loading = true)
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

//    fun softDeleteAnswer(answerId: String) {
//        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        viewModelScope.launch {
//            repo.softDeleteAnswer(answerId, uid)
//        }
//    }
    fun softDeleteSession(answerIds: List<String>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            answerIds.forEach { answerId ->
                repo.softDeleteAnswer(answerId, uid)
            }
        }
    }
}