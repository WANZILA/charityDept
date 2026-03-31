package com.example.charityDept.presentation.screens.admin.taxonomy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.AssessmentTaxonomy
import com.example.charityDept.domain.repositories.offline.OfflineAssessmentTaxonomyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaxonomyBankUiState(
    val loading: Boolean = false,
    val items: List<AssessmentTaxonomy> = emptyList()
)

@HiltViewModel
class AssessmentTaxonomyAdminViewModel @Inject constructor(
    private val repo: OfflineAssessmentTaxonomyRepository
) : ViewModel() {

    val ui: StateFlow<TaxonomyBankUiState> =
        repo.observeAdminAll()
            .map { TaxonomyBankUiState(loading = false, items = it) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TaxonomyBankUiState(loading = true)
            )

    fun delete(taxonomyId: String) = viewModelScope.launch {
        repo.softDelete(taxonomyId)
    }

    fun upsert(draft: AssessmentTaxonomy, onDone: (id: String) -> Unit) = viewModelScope.launch {
        val id = repo.upsertWithAudit(draft)
        onDone(id)
    }

    suspend fun loadOnce(taxonomyId: String): AssessmentTaxonomy? = repo.getOnce(taxonomyId)
}