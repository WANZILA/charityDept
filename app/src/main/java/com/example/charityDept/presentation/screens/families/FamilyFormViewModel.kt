package com.example.charityDept.presentation.screens.families

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.core.Utils.GenerateId
import com.example.charityDept.data.model.Family
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

data class FamilyFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val familyId: String = "",
    val caseReferenceNumber: String = "",
    val dateOfAssessment: Timestamp? = null,
    val primaryContactHeadOfHousehold: String = "",
    val addressLocation: String = "",
    val isBornAgain: Boolean = false,
    val personalPhone1: String = "",
    val personalPhone2: String = "",
    val ninNumber: String = "",
    val memberAncestralCountry: String = "",
    val memberAncestralRegion: String = "",
    val memberAncestralDistrict: String = "",
    val memberAncestralCounty: String = "",
    val memberAncestralSubCounty: String = "",
    val memberAncestralParish: String = "",
    val memberAncestralVillage: String = "",
    val memberRentalCountry: String = "",
    val memberRentalRegion: String = "",
    val memberRentalDistrict: String = "",
    val memberRentalCounty: String = "",
    val memberRentalSubCounty: String = "",
    val memberRentalParish: String = "",
    val memberRentalVillage: String = "",
    val headError: String? = null,
    val error: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val version: Long = 0L
)

@HiltViewModel
class FamilyFormViewModel @Inject constructor(
    private val repo: OfflineFamiliesRepository
) : ViewModel() {

    sealed class FamilyFormEvent {
        data class Saved(val id: String) : FamilyFormEvent()
        data class Error(val msg: String) : FamilyFormEvent()
    }

    private val _ui = MutableStateFlow(FamilyFormUiState())
    val ui: StateFlow<FamilyFormUiState> = _ui.asStateFlow()

    private val _events = Channel<FamilyFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun ensureNewIdIfNeeded() {
        if (_ui.value.familyId.isBlank()) {
            _ui.value = _ui.value.copy(
                familyId = "family_${Timestamp.now().seconds}_${Timestamp.now().nanoseconds}"
            )
        }
    }

    fun loadForEdit(familyId: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            repo.observeFamily(familyId).collect { family ->
                if (family == null) {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = "Family not found"
                    )
                } else {
                    _ui.value = family.toUi().copy(
                        loading = false,
                        saving = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onCaseReferenceNumber(v: String) { _ui.value = _ui.value.copy(caseReferenceNumber = v) }
    fun onPrimaryContactHeadOfHousehold(v: String) {
        _ui.value = _ui.value.copy(
            primaryContactHeadOfHousehold = v,
            headError = null
        )
    }
    fun onAddressLocation(v: String) { _ui.value = _ui.value.copy(addressLocation = v) }
    fun onIsBornAgain(v: Boolean) { _ui.value = _ui.value.copy(isBornAgain = v) }
    fun onPersonalPhone1(v: String) { _ui.value = _ui.value.copy(personalPhone1 = v) }
    fun onPersonalPhone2(v: String) { _ui.value = _ui.value.copy(personalPhone2 = v) }
    fun onNinNumber(v: String) { _ui.value = _ui.value.copy(ninNumber = v) }

    fun onMemberAncestralCountry(v: String) { _ui.value = _ui.value.copy(memberAncestralCountry = v) }
    fun onMemberAncestralRegion(v: String) { _ui.value = _ui.value.copy(memberAncestralRegion = v) }
    fun onMemberAncestralDistrict(v: String) { _ui.value = _ui.value.copy(memberAncestralDistrict = v) }
    fun onMemberAncestralCounty(v: String) { _ui.value = _ui.value.copy(memberAncestralCounty = v) }
    fun onMemberAncestralSubCounty(v: String) { _ui.value = _ui.value.copy(memberAncestralSubCounty = v) }
    fun onMemberAncestralParish(v: String) { _ui.value = _ui.value.copy(memberAncestralParish = v) }
    fun onMemberAncestralVillage(v: String) { _ui.value = _ui.value.copy(memberAncestralVillage = v) }

    fun onMemberRentalCountry(v: String) { _ui.value = _ui.value.copy(memberRentalCountry = v) }
    fun onMemberRentalRegion(v: String) { _ui.value = _ui.value.copy(memberRentalRegion = v) }
    fun onMemberRentalDistrict(v: String) { _ui.value = _ui.value.copy(memberRentalDistrict = v) }
    fun onMemberRentalCounty(v: String) { _ui.value = _ui.value.copy(memberRentalCounty = v) }
    fun onMemberRentalSubCounty(v: String) { _ui.value = _ui.value.copy(memberRentalSubCounty = v) }
    fun onMemberRentalParish(v: String) { _ui.value = _ui.value.copy(memberRentalParish = v) }
    fun onMemberRentalVillage(v: String) { _ui.value = _ui.value.copy(memberRentalVillage = v) }

    fun save() {
        val now = Timestamp.now()
        val state = _ui.value

        if (state.primaryContactHeadOfHousehold.isBlank()) {
            _ui.value = state.copy(headError = "Head of household is required")
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(saving = true, error = null)

                val isNew = state.familyId.isBlank()
                val id  = GenerateId.generateId("family")
                val family = Family(
                    familyId = id,
                    caseReferenceNumber = state.caseReferenceNumber.trim(),
                    dateOfAssessment = state.dateOfAssessment ?: now,
                    primaryContactHeadOfHousehold = state.primaryContactHeadOfHousehold.trim(),
                    addressLocation = state.addressLocation.trim(),
                    isBornAgain = state.isBornAgain,
                    personalPhone1 = state.personalPhone1.trim(),
                    personalPhone2 = state.personalPhone2.trim(),
                    ninNumber = state.ninNumber.trim(),
                    memberAncestralCountry = state.memberAncestralCountry.trim(),
                    memberAncestralRegion = state.memberAncestralRegion.trim(),
                    memberAncestralDistrict = state.memberAncestralDistrict.trim(),
                    memberAncestralCounty = state.memberAncestralCounty.trim(),
                    memberAncestralSubCounty = state.memberAncestralSubCounty.trim(),
                    memberAncestralParish = state.memberAncestralParish.trim(),
                    memberAncestralVillage = state.memberAncestralVillage.trim(),
                    memberRentalCountry = state.memberRentalCountry.trim(),
                    memberRentalRegion = state.memberRentalRegion.trim(),
                    memberRentalDistrict = state.memberRentalDistrict.trim(),
                    memberRentalCounty = state.memberRentalCounty.trim(),
                    memberRentalSubCounty = state.memberRentalSubCounty.trim(),
                    memberRentalParish = state.memberRentalParish.trim(),
                    memberRentalVillage = state.memberRentalVillage.trim(),
                    createdAt = if (isNew) now else state.dateOfAssessment ?: now,
                    updatedAt = now,
                    isDirty = true,
                    isDeleted = false,

                )

                val savedId = repo.upsertFamily(family, isNew = isNew)
                _ui.value = _ui.value.copy(saving = false)
                _events.send(FamilyFormEvent.Saved(savedId))
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    saving = false,
                    error = t.message ?: "Failed to save family"
                )
                _events.send(FamilyFormEvent.Error(t.message ?: "Failed to save family"))
            }
        }
    }
}

private fun Family.toUi(): FamilyFormUiState =
    FamilyFormUiState(
        familyId = familyId,
        caseReferenceNumber = caseReferenceNumber,
        dateOfAssessment = dateOfAssessment,
        primaryContactHeadOfHousehold = primaryContactHeadOfHousehold,
        addressLocation = addressLocation,
        isBornAgain = isBornAgain,
        personalPhone1 = personalPhone1,
        personalPhone2 = personalPhone2,
        ninNumber = ninNumber,
        memberAncestralCountry = memberAncestralCountry,
        memberAncestralRegion = memberAncestralRegion,
        memberAncestralDistrict = memberAncestralDistrict,
        memberAncestralCounty = memberAncestralCounty,
        memberAncestralSubCounty = memberAncestralSubCounty,
        memberAncestralParish = memberAncestralParish,
        memberAncestralVillage = memberAncestralVillage,
        memberRentalCountry = memberRentalCountry,
        memberRentalRegion = memberRentalRegion,
        memberRentalDistrict = memberRentalDistrict,
        memberRentalCounty = memberRentalCounty,
        memberRentalSubCounty = memberRentalSubCounty,
        memberRentalParish = memberRentalParish,
        memberRentalVillage = memberRentalVillage,
        createdAt = createdAt,
        version = version,
    )