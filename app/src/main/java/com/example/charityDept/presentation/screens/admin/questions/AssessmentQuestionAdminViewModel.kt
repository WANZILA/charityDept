package com.example.charityDept.presentation.screens.admin.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.AssessmentTaxonomyDao
import com.example.charityDept.data.model.AssessmentQuestion
import com.example.charityDept.domain.repositories.offline.OfflineAssessmentQuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuestionBankUiState(
    val loading: Boolean = false,
    val items: List<AssessmentQuestion> = emptyList(),

)

@HiltViewModel
class AssessmentQuestionAdminViewModel @Inject constructor(
    private val repo: OfflineAssessmentQuestionRepository,
    private val taxonomyDao: AssessmentTaxonomyDao
) : ViewModel() {

    val ui: StateFlow<QuestionBankUiState> =
        repo.observeAdminAll()
            .map { QuestionBankUiState(loading = false, items = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestionBankUiState(loading = true))

    val taxonomy = taxonomyDao.observeActiveAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(questionId: String) = viewModelScope.launch {
        repo.softDelete(questionId)
    }

    fun upsert(draft: AssessmentQuestion, onDone: (id: String) -> Unit) = viewModelScope.launch {
        val id = repo.upsertWithAudit(draft)
        onDone(id)
    }

    suspend fun loadOnce(questionId: String): AssessmentQuestion? = repo.getOnce(questionId)
}

