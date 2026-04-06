package com.example.charityDept.presentation.screens.admin.streets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.core.utils.picker.PickerOption
import com.example.charityDept.data.model.Street
import com.example.charityDept.domain.repositories.online.StreetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreetsViewModel @Inject constructor(
    private val repo: StreetsRepository
) : ViewModel() {

    // Live streets (active only)
    val streets: StateFlow<List<Street>> =
        repo.watchAll(activeOnly = true)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Picker feed: id + display name for dialogs/search pickers
    val picker: StateFlow<List<PickerOption>> =
        repo.streetsPickerWatchAll(activeOnly = true)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addOrUpdate(street: Street, isNew: Boolean) = viewModelScope.launch {
        repo.upsert(street, isNew)
    }

    fun deactivate(streetId: String) = viewModelScope.launch {
        repo.patch(streetId, mapOf("isActive" to false))
    }

    fun delete(streetId: String) = viewModelScope.launch {
        repo.delete(streetId)
    }

    fun deactivateMany(ids: Collection<String>) = viewModelScope.launch {
        ids.forEach { repo.patch(it, mapOf("isActive" to false)) }
    }

    fun deleteMany(ids: Collection<String>) = viewModelScope.launch {
        ids.forEach { repo.delete(it) }
    }

    suspend fun search(prefix: String): List<Street> =
        repo.searchByNamePrefix(prefix)
}

