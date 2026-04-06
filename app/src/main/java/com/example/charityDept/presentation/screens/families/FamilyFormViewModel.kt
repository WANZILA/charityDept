package com.example.charityDept.presentation.screens.families

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.core.utils.GenerateId
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
import com.example.charityDept.core.utils.picker.PickerFeature
import com.example.charityDept.core.utils.picker.PickerOption
import com.example.charityDept.data.model.Country
import com.example.charityDept.data.model.Gender
import com.example.charityDept.domain.repositories.offline.OfflineUgAdminRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
data class FamilyFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val fName: String = "",
    val lName: String = "",
    val country: String = "",
    val gender: Gender = Gender.MALE,
    val occupationOrSchoolGrade: String = "",

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
    private val repo: OfflineFamiliesRepository,
    private val ugRepo: OfflineUgAdminRepository
) : ViewModel() {

    class UgandaAddressPicker(
        private val scope: CoroutineScope,
        private val repo: OfflineUgAdminRepository,
        private val setAddress: (
            region: String,
            district: String,
            county: String,
            subCounty: String,
            parish: String,
            village: String
        ) -> Unit
    )
    {
        private val selectedRegionCode = MutableStateFlow<String?>(null)
        private val selectedDistrictCode = MutableStateFlow<String?>(null)
        private val selectedCountyCode = MutableStateFlow<String?>(null)
        private val selectedSubCountyCode = MutableStateFlow<String?>(null)
        private val selectedParishCode = MutableStateFlow<String?>(null)

        private var regionName: String = ""
        private var districtName: String = ""
        private var countyName: String = ""
        private var subCountyName: String = ""
        private var parishName: String = ""
        private var villageName: String = ""

        private fun emit() {
            setAddress(
                regionName,
                districtName,
                countyName,
                subCountyName,
                parishName,
                villageName
            )
        }

        val regionPicker = PickerFeature(
            scope = scope,
            optionsFlow = repo.watchRegions().map { list ->
                list.map { PickerOption(id = it.regionCode, name = it.regionName) }
            }
        )

        val districtPicker = PickerFeature(
            scope = scope,
            optionsFlow = selectedRegionCode.flatMapLatest { rc ->
                if (rc.isNullOrBlank()) flowOf(emptyList())
                else repo.watchDistricts(rc).map { list ->
                    list.map { PickerOption(id = it.districtCode, name = it.districtName) }
                }
            }
        )

        val districtSearchPicker = PickerFeature(
            scope = scope,
            optionsFlow = repo.watchAllDistricts().map { list ->
                list.map { PickerOption(id = it.districtCode, name = it.districtName) }
            }
        )

        val villageSearchPicker = PickerFeature(
            scope = scope,
            optionsFlow = repo.watchAllVillages().map { list ->
                list.map { PickerOption(id = it.villageCode, name = it.villageName) }
            }
        )

        val countyPicker = PickerFeature(
            scope = scope,
            optionsFlow = selectedDistrictCode.flatMapLatest { dc ->
                if (dc.isNullOrBlank()) flowOf(emptyList())
                else repo.watchCounties(dc).map { list ->
                    list.map { PickerOption(id = it.countyCode, name = it.countyName) }
                }
            }
        )

        val subCountyPicker = PickerFeature(
            scope = scope,
            optionsFlow = selectedCountyCode.flatMapLatest { cc ->
                if (cc.isNullOrBlank()) flowOf(emptyList())
                else repo.watchSubcounties(cc).map { list ->
                    list.map { PickerOption(id = it.subCountyCode, name = it.subCountyName) }
                }
            }
        )

        val parishPicker = PickerFeature(
            scope = scope,
            optionsFlow = selectedSubCountyCode.flatMapLatest { sc ->
                if (sc.isNullOrBlank()) flowOf(emptyList())
                else repo.watchParishes(sc).map { list ->
                    list.map { PickerOption(id = it.parishCode, name = it.parishName) }
                }
            }
        )

        val villagePicker = PickerFeature(
            scope = scope,
            optionsFlow = selectedParishCode.flatMapLatest { pc ->
                if (pc.isNullOrBlank()) flowOf(emptyList())
                else repo.watchVillages(pc).map { list ->
                    list.map { PickerOption(id = it.villageCode, name = it.villageName) }
                }
            }
        )

        fun onRegionPicked(opt: PickerOption) {
            selectedRegionCode.value = opt.id
            selectedDistrictCode.value = null
            selectedCountyCode.value = null
            selectedSubCountyCode.value = null
            selectedParishCode.value = null

            regionName = opt.name
            districtName = ""
            countyName = ""
            subCountyName = ""
            parishName = ""
            villageName = ""

            emit()
        }

        fun onDistrictPicked(opt: PickerOption) {
            scope.launch {
                val lookup = repo.getDistrictRegionByDistrictCode(opt.id)

                selectedRegionCode.value = lookup?.regionCode ?: selectedRegionCode.value
                selectedDistrictCode.value = opt.id
                selectedCountyCode.value = null
                selectedSubCountyCode.value = null
                selectedParishCode.value = null

                if (!lookup?.regionName.isNullOrBlank()) {
                    regionName = lookup!!.regionName
                }
                districtName = lookup?.districtName ?: opt.name
                countyName = ""
                subCountyName = ""
                parishName = ""
                villageName = ""

                districtSearchPicker.clearQuery()
                emit()
            }
        }

        fun onCountyPicked(opt: PickerOption) {
            selectedCountyCode.value = opt.id
            selectedSubCountyCode.value = null
            selectedParishCode.value = null

            countyName = opt.name
            subCountyName = ""
            parishName = ""
            villageName = ""

            emit()
        }

        fun onSubCountyPicked(opt: PickerOption) {
            selectedSubCountyCode.value = opt.id
            selectedParishCode.value = null

            subCountyName = opt.name
            parishName = ""
            villageName = ""

            emit()
        }

        fun onParishPicked(opt: PickerOption) {
            selectedParishCode.value = opt.id

            parishName = opt.name
            villageName = ""

            emit()
        }

        fun onVillagePicked(opt: PickerOption) {
            scope.launch {
                val lookup = repo.getVillageHierarchyByVillageCode(opt.id)

                if (lookup == null) {
                    villageName = opt.name
                    villageSearchPicker.clearQuery()
                    emit()
                    return@launch
                }

                selectedRegionCode.value = lookup.regionCode
                selectedDistrictCode.value = lookup.districtCode
                selectedCountyCode.value = lookup.countyCode
                selectedSubCountyCode.value = lookup.subCountyCode
                selectedParishCode.value = lookup.parishCode

                regionName = lookup.regionName
                districtName = lookup.districtName
                countyName = lookup.countyName
                subCountyName = lookup.subCountyName
                parishName = lookup.parishName
                villageName = lookup.villageName

                villageSearchPicker.clearQuery()
                emit()
            }
        }

        fun clearAll() {
            selectedRegionCode.value = null
            selectedDistrictCode.value = null
            selectedCountyCode.value = null
            selectedSubCountyCode.value = null
            selectedParishCode.value = null

            regionName = ""
            districtName = ""
            countyName = ""
            subCountyName = ""
            parishName = ""
            villageName = ""

            emit()
        }
    }

    val ugAncestralPicker = UgandaAddressPicker(viewModelScope, ugRepo) { r, d, c, s, p, v ->
        _ui.value = _ui.value.copy(
            memberAncestralRegion = r,
            memberAncestralDistrict = d,
            memberAncestralCounty = c,
            memberAncestralSubCounty = s,
            memberAncestralParish = p,
            memberAncestralVillage = v
        )
    }

    val ugRentalPicker = UgandaAddressPicker(viewModelScope, ugRepo) { r, d, c, s, p, v ->
        _ui.value = _ui.value.copy(
            memberRentalRegion = r,
            memberRentalDistrict = d,
            memberRentalCounty = c,
            memberRentalSubCounty = s,
            memberRentalParish = p,
            memberRentalVillage = v
        )
    }
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
                familyId = GenerateId.generateId("family")
            )
        }
    }

    fun onFName(v: String) { _ui.value = _ui.value.copy(fName = v) }
    fun onLName(v: String) { _ui.value = _ui.value.copy(lName = v) }
    fun onCountry(v: String) { _ui.value = _ui.value.copy(country = v) }
//    fun onGender(v: String) { _ui.value = _ui.value.copy(gender = v) }
fun onGender(v: Gender) { _ui.value = _ui.value.copy(gender = v) }
    fun onMemberAncestralCountryChanged(country: Country) {
        if (country != Country.UGANDA) {
            ugAncestralPicker.clearAll()
        }

        _ui.value = _ui.value.copy(
            memberAncestralCountry = country.name,
            memberAncestralRegion = "",
            memberAncestralDistrict = "",
            memberAncestralCounty = "",
            memberAncestralSubCounty = "",
            memberAncestralParish = "",
            memberAncestralVillage = ""
        )
    }

    fun onMemberRentalCountryChanged(country: Country) {
        if (country != Country.UGANDA) {
            ugRentalPicker.clearAll()
        }

        _ui.value = _ui.value.copy(
            memberRentalCountry = country.name,
            memberRentalRegion = "",
            memberRentalDistrict = "",
            memberRentalCounty = "",
            memberRentalSubCounty = "",
            memberRentalParish = "",
            memberRentalVillage = ""
        )
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
//    fun onPrimaryContactHeadOfHousehold(v: String) {
//        _ui.value = _ui.value.copy(
//            primaryContactHeadOfHousehold = v,
//            headError = null
//        )
//    }
    fun onOccupationOrSchoolGrade(v: String) { _ui.value = _ui.value.copy(occupationOrSchoolGrade = v) }
    fun onIsBornAgain(v: Boolean) { _ui.value = _ui.value.copy(isBornAgain = v) }
    fun onPersonalPhone1(v: String) {
        _ui.value = _ui.value.copy(personalPhone1 = v.filter { it.isDigit() })
    }

    fun onPersonalPhone2(v: String) {
        _ui.value = _ui.value.copy(personalPhone2 = v.filter { it.isDigit() })
    }
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

//        if (state.primaryContactHeadOfHousehold.isBlank()) {
//            _ui.value = state.copy(headError = "Head of household is required")
//            return
//        }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(saving = true, error = null)

                val isNew = state.familyId.isBlank()
                val id = if (isNew) GenerateId.generateId("family") else state.familyId

                val family = Family(
                    familyId = id,
                    fName = state.fName,
                    lName = state.lName,
                    gender = state.gender,
                    occupationOrSchoolGrade = state.occupationOrSchoolGrade,
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
                    createdAt = state.createdAt,
                    updatedAt = now,
                    isDirty = true,
                    isDeleted = false,
                    version = state.version
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
        fName = fName,
        lName = lName,
        gender = gender,
        occupationOrSchoolGrade = occupationOrSchoolGrade,
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