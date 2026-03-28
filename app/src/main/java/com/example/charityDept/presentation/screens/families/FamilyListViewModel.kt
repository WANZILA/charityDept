package com.example.charityDept.presentation.screens.families

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.Family
import com.example.charityDept.domain.repositories.offline.FamilySnapshot
import com.example.charityDept.domain.repositories.offline.OfflineFamiliesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyListUiState(
    val loading: Boolean = true,
    val families: List<Family> = emptyList(),
    val totalFamilies: Int = 0,
    val totalMembers: Int = 0,
    val error: String? = null
)

@HiltViewModel
class FamilyListViewModel @Inject constructor(
    private val repo: OfflineFamiliesRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(FamilyListUiState())
    val ui: StateFlow<FamilyListUiState> = _ui.asStateFlow()

    private val _query = MutableStateFlow("")
    private var latestSnap = FamilySnapshot(
        families = emptyList(),
        totalFamilies = 0,
        totalMembers = 0
    )

    init {
        observeFamilies()
        observeQuery()
    }

    fun onSearchQueryChange(value: String) {
        _query.value = value
    }

    private fun observeFamilies() {
        viewModelScope.launch {
            repo.streamFamilies().collect { snap ->
                latestSnap = snap
                pushFiltered()
            }
        }
    }

    private fun observeQuery() {
        viewModelScope.launch {
            _query
                .map { it.trim().lowercase() }
                .distinctUntilChanged()
                .collect {
                    pushFiltered()
                }
        }
    }

    private fun pushFiltered() {
        val q = _query.value.trim().lowercase()

        val filtered = if (q.isBlank()) {
            latestSnap.families
        } else {
            latestSnap.families.filter { family ->
                family.caseReferenceNumber.lowercase().contains(q) ||
                        family.fName.lowercase().contains(q) ||
                        family.lName.lowercase().contains(q) ||
                        family.primaryContactHeadOfHousehold.lowercase().contains(q) ||
                        family.addressLocation.lowercase().contains(q) ||
                        family.personalPhone1.lowercase().contains(q) ||
                        family.personalPhone2.lowercase().contains(q)
            }
        }

        _ui.value = FamilyListUiState(
            loading = false,
            families = filtered,
            totalFamilies = latestSnap.totalFamilies,
            totalMembers = latestSnap.totalMembers,
            error = null
        )
    }
}