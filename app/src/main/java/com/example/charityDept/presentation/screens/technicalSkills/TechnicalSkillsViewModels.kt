package com.example.charityDept.presentation.screens.technicalSkills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.TechnicalSkill
import com.example.charityDept.domain.repositories.online.TechnicalSkillsRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TechnicalSkillsViewModel @Inject constructor(
    private val repo: TechnicalSkillsRepository
) : ViewModel() {

    private val _skills = MutableStateFlow<List<TechnicalSkill>>(emptyList())
    val skills: StateFlow<List<TechnicalSkill>> = _skills.asStateFlow()

    init {
        viewModelScope.launch {
            repo.watchAll(activeOnly = true).collect { _skills.value = it }
        }
    }

    fun addOrUpdate(skill: TechnicalSkill, isNew: Boolean) = viewModelScope.launch {
        repo.upsert(skill, isNew)
    }

    fun deactivate(skillId: String) = viewModelScope.launch {
        repo.patch(skillId, mapOf("isActive" to false))
    }

    fun delete(skillId: String) = viewModelScope.launch {
        repo.delete(skillId)
    }

    // SkillsViewModel.kt — add:
    fun deactivateMany(ids: Collection<String>) = viewModelScope.launch {
        ids.forEach { repo.patch(it, mapOf("isActive" to false)) }
    }
    fun deleteMany(ids: Collection<String>) = viewModelScope.launch {
        ids.forEach { repo.delete(it) }
    }


    suspend fun search(prefix: String): List<TechnicalSkill> =
        repo.searchByNamePrefix(prefix)
}

