package com.example.charityDept.presentation.screens.families

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.core.utils.GenerateId
import com.example.charityDept.data.model.FamilyMember
import com.example.charityDept.data.model.Gender
import com.example.charityDept.domain.repositories.offline.OfflineFamiliesRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyMemberFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val familyMemberId: String = "",
    val familyId: String = "",
    val fName: String = "",
    val lName: String = "",
    val age: String = "",
    val gender: Gender = Gender.MALE,
    val relationship: String = "",
    val occupationOrSchoolGrade: String = "",
    val healthOrDisabilityStatus: String = "",
    val personalPhone1: String = "",
    val personalPhone2: String = "",
    val ninNumber: String = "",
    val createdAt: Timestamp? = null,
    val version: Long = 0L,
    val nameError: String? = null,
    val error: String? = null
)

@HiltViewModel
class FamilyMemberFormViewModel @Inject constructor(
    private val repo: OfflineFamiliesRepository
) : ViewModel() {

    sealed interface Event {
        data class Saved(val id: String) : Event
        data class Error(val msg: String) : Event
    }

    private val _ui = MutableStateFlow(FamilyMemberFormUiState())
    val ui: StateFlow<FamilyMemberFormUiState> = _ui.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun ensureNewIdIfNeeded(familyId: String) {
        if (_ui.value.familyMemberId.isBlank()) {
            _ui.value = _ui.value.copy(
                familyId = familyId,
                familyMemberId = GenerateId.generateId("familyMember")
            )
        }
    }

    fun loadForEdit(familyId: String, familyMemberId: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            repo.observeFamilyMember(familyMemberId).collect { member ->
                if (member == null) {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = "Family member not found"
                    )
                } else {
                    _ui.value = FamilyMemberFormUiState(
                        loading = false,
                        familyMemberId = member.familyMemberId,
                        familyId = familyId,
                        fName = member.fName,
                        lName = member.lName,
                        age = if (member.age > 0) member.age.toString() else "",
                        gender = member.gender,
                        relationship = member.relationship,
                        occupationOrSchoolGrade = member.occupationOrSchoolGrade,
                        healthOrDisabilityStatus = member.healthOrDisabilityStatus,
                        personalPhone1 = member.personalPhone1,
                        personalPhone2 = member.personalPhone2,
                        ninNumber = member.ninNumber,
                        createdAt = member.createdAt,
                        version = member.version
                    )
                }
            }
        }
    }

    fun onfName(v: String) { _ui.value = _ui.value.copy(fName = v, nameError = null) }
    fun onlName(v: String) { _ui.value = _ui.value.copy(lName = v, nameError = null) }

    fun onAge(v: String) { _ui.value = _ui.value.copy(age = v) }
    fun onGender(v: Gender) {
        _ui.value = _ui.value.copy(gender = v)
    }
    fun onRelationship(v: String) {
        _ui.value = _ui.value.copy(relationship = v.toWordCase())
    }

    fun onOccupationOrSchoolGrade(v: String) {
        _ui.value = _ui.value.copy(occupationOrSchoolGrade = v.toWordCase())
    }

    fun onHealthOrDisabilityStatus(v: String) {
        _ui.value = _ui.value.copy(healthOrDisabilityStatus = v.toWordCase())
    }
     fun onPersonalPhone1(v: String) {
        _ui.value = _ui.value.copy(personalPhone1 = v.filter { it.isDigit() })
    }

    fun onPersonalPhone2(v: String) {
        _ui.value = _ui.value.copy(personalPhone2 = v.filter { it.isDigit() })
    }
    fun onNinNumber(v: String) { _ui.value = _ui.value.copy(ninNumber = v) }

    fun save() {
        val state = _ui.value
        if (state.fName.isBlank() && state.lName.isBlank()) {
            _ui.value = state.copy(nameError = "Name is required")
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(saving = true, error = null)

                val isNew = state.createdAt == null
                val now = Timestamp.now()
                val id = if (isNew) GenerateId.generateId("familyMember") else state.familyMemberId

                val member = FamilyMember(
                    familyMemberId = id,
                    familyId = state.familyId,
                    fName = state.fName.trim(),
                    lName = state.lName.trim(),
                    age = state.age.toIntOrNull() ?: 0,
                    gender = state.gender,
                    relationship = state.relationship.trim(),
                    occupationOrSchoolGrade = state.occupationOrSchoolGrade.trim(),
                    healthOrDisabilityStatus = state.healthOrDisabilityStatus.trim(),
                    personalPhone1 = state.personalPhone1.trim(),
                    personalPhone2 = state.personalPhone2.trim(),
                    ninNumber = state.ninNumber.trim(),
                    createdAt = state.createdAt ?: now,
                    updatedAt = now,
                    isDirty = true,
                    isDeleted = false,
                    version = state.version
                )

                val savedId = repo.upsertFamilyMember(member, isNew)
                _ui.value = _ui.value.copy(saving = false)
                _events.send(Event.Saved(savedId))
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    saving = false,
                    error = t.message ?: "Failed to save family member"
                )
                _events.send(Event.Error(t.message ?: "Failed to save family member"))
            }
        }
    }
}

private fun String.toWordCase(): String {
    return split(" ").joinToString(" ") { part ->
        if (part.isBlank()) {
            part
        } else {
            part.lowercase().replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase() else ch.toString()
            }
        }
    }
}