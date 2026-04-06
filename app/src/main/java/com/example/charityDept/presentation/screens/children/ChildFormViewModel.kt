package com.example.charityDept.presentation.screens.children

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.utils.FormValidatorUtil
import com.example.charityDept.core.utils.GenerateId
import com.example.charityDept.core.utils.picker.PickerFeature
import com.example.charityDept.core.utils.picker.PickerOption
import com.example.charityDept.data.model.*
import com.example.charityDept.domain.repositories.offline.OfflineChildrenRepository
import com.example.charityDept.domain.repositories.offline.OfflineUgAdminRepository
import com.example.charityDept.domain.repositories.online.StreetsRepository
import com.example.charityDept.domain.repositories.online.TechnicalSkillsRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import com.example.charityDept.core.utils.ChildImageFileHelper

private const val MAX_AGE = 25

@HiltViewModel
class ChildFormViewModel @Inject constructor(
    val repo: OfflineChildrenRepository,
    private val techRepo: TechnicalSkillsRepository,
    private val streetRepo: StreetsRepository,
    private val ugRepo: OfflineUgAdminRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    var ui by mutableStateOf(ChildFormUiState())

    /**
     * FIXED: This picker now remembers the previously chosen names and always emits the FULL chain.
     * Your old version was wiping region/district/etc. because it emitted "" for fields you didn't pick.
     */
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
    ) {
        // selected CODES (drive downstream queries)
        private val selectedRegionCode = MutableStateFlow<String?>(null)
        private val selectedDistrictCode = MutableStateFlow<String?>(null)
        private val selectedCountyCode = MutableStateFlow<String?>(null)
        private val selectedSubCountyCode = MutableStateFlow<String?>(null)
        private val selectedParishCode = MutableStateFlow<String?>(null)

        // selected NAMES (what you store in ChildFormUiState)
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
//        fun onDistrictPicked(opt: PickerOption) {
//            selectedDistrictCode.value = opt.id
//            selectedCountyCode.value = null
//            selectedSubCountyCode.value = null
//            selectedParishCode.value = null
//
//            districtName = opt.name
//            countyName = ""
//            subCountyName = ""
//            parishName = ""
//            villageName = ""
//
//            emit()
//        }

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
//        fun onVillagePicked(opt: PickerOption) {
//            villageName = opt.name
//            emit()
//        }

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


    // ---------------- Uganda pickers (7 instances) ----------------

    val ugResettlementPicker = UgandaAddressPicker(viewModelScope, ugRepo) { r, d, c, s, p, v ->
        ui = ui.copy(region = r, district = d, county = c, subCounty = s, parish = p, village = v)
    }

    val ugM1AncestralPicker = UgandaAddressPicker(viewModelScope, ugRepo) { r, d, c, s, p, v ->
        ui = ui.copy(
            member1AncestralRegion = r,
            member1AncestralDistrict = d,
            member1AncestralCounty = c,
            member1AncestralSubCounty = s,
            member1AncestralParish = p,
            member1AncestralVillage = v
        )
    }

    val ugM1RentalPicker = UgandaAddressPicker(viewModelScope, ugRepo) { r, d, c, s, p, v ->
        ui = ui.copy(
            member1RentalRegion = r,
            member1RentalDistrict = d,
            member1RentalCounty = c,
            member1RentalSubCounty = s,
            member1RentalParish = p,
            member1RentalVillage = v
        )
    }

    val ugM2AncestralPicker = UgandaAddressPicker(viewModelScope, ugRepo) { r, d, c, s, p, v ->
        ui = ui.copy(
            member2AncestralRegion = r,
            member2AncestralDistrict = d,
            member2AncestralCounty = c,
            member2AncestralSubCounty = s,
            member2AncestralParish = p,
            member2AncestralVillage = v
        )
    }

    val ugM2RentalPicker = UgandaAddressPicker(viewModelScope, ugRepo) { r, d, c, s, p, v ->
        ui = ui.copy(
            member2RentalRegion = r,
            member2RentalDistrict = d,
            member2RentalCounty = c,
            member2RentalSubCounty = s,
            member2RentalParish = p,
            member2RentalVillage = v
        )
    }

    val ugM3AncestralPicker = UgandaAddressPicker(viewModelScope, ugRepo) { r, d, c, s, p, v ->
        ui = ui.copy(
            member3AncestralRegion = r,
            member3AncestralDistrict = d,
            member3AncestralCounty = c,
            member3AncestralSubCounty = s,
            member3AncestralParish = p,
            member3AncestralVillage = v
        )
    }

    val ugM3RentalPicker = UgandaAddressPicker(viewModelScope, ugRepo) { r, d, c, s, p, v ->
        ui = ui.copy(
            member3RentalRegion = r,
            member3RentalDistrict = d,
            member3RentalCounty = c,
            member3RentalSubCounty = s,
            member3RentalParish = p,
            member3RentalVillage = v
        )
    }

    // ---------------- Other pickers ----------------

    val streetPicker = PickerFeature(
        scope = viewModelScope,
        optionsFlow = streetRepo.streetsPickerWatchAll()
    )

    fun onStreetPicked(opt: PickerOption) {
        // update your form state with client id/name/img
//        update { copy(clientId = opt.id, clientName = opt.name, clientImage = opt.imageUrl) }
        ui =  ui.copy(
            street = opt.name
        )
        streetPicker.clearQuery()
    }

    val technicalSkillsPicker = PickerFeature(
        scope = viewModelScope,
        optionsFlow = techRepo.techSkillsPickerWatchAll()
    )

    fun onTechnicalSkillsPicked(opt: PickerOption) {
        ui = ui.copy(technicalSkills = opt.name)
        technicalSkillsPicker.clearQuery()
    }

    // ---- step control ----
    var step by mutableStateOf(RegistrationStatus.BASICINFOR)
        private set

    fun jumpToStep(s: RegistrationStatus) {
        step = s
        ui = ui.copy(registrationStatus = s)
    }

    fun goBack() {
        step = when (step) {
            RegistrationStatus.BASICINFOR -> RegistrationStatus.BASICINFOR
            RegistrationStatus.BACKGROUND -> RegistrationStatus.BASICINFOR
            RegistrationStatus.EDUCATION  -> RegistrationStatus.BACKGROUND
            RegistrationStatus.FAMILY     -> RegistrationStatus.SPONSORSHIP
            RegistrationStatus.SPONSORSHIP -> RegistrationStatus.SPIRITUAL
            RegistrationStatus.SPIRITUAL  -> RegistrationStatus.COMPLETE
            RegistrationStatus.COMPLETE   -> RegistrationStatus.SPIRITUAL
        }
        ui = ui.copy(registrationStatus = step)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun goNext(onAfterSave: (() -> Unit)? = null) = viewModelScope.launch {
        val ok = validateStep(step)
        if (!ok) return@launch
        step = when (step) {
            RegistrationStatus.BASICINFOR -> RegistrationStatus.BACKGROUND
            RegistrationStatus.BACKGROUND -> RegistrationStatus.EDUCATION
            RegistrationStatus.EDUCATION  -> RegistrationStatus.FAMILY
            RegistrationStatus.FAMILY     -> RegistrationStatus.SPONSORSHIP
            RegistrationStatus.SPONSORSHIP -> RegistrationStatus.SPIRITUAL
            RegistrationStatus.SPIRITUAL  -> RegistrationStatus.COMPLETE
            RegistrationStatus.COMPLETE   -> RegistrationStatus.SPIRITUAL
        }
        ui = ui.copy(registrationStatus = step)
        onAfterSave?.invoke()
    }

    // ---- one-shot events ----
    private val _events = Channel<ChildFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    sealed interface ChildFormEvent {
        data class Saved(val id: String) : ChildFormEvent
        data class Error(val msg: String) : ChildFormEvent
    }

    // ---- load existing ----
    fun loadForEdit(childId: String) = viewModelScope.launch {
        ui = ui.copy(loading = true, error = null)
        val existing = repo.getChildFast(childId)
        ui = if (existing != null) {
            ui.from(existing).copy(loading = false)
        } else {
            ui.copy(loading = false, error = "Child not found")
        }
    }

    // ---- common setters used by UI ----
    fun onFirstName(v: String) {
        val res = FormValidatorUtil.validateName(v)
        ui = ui.copy(fName = res.value, fNameError = res.error)
    }

    fun onLastName(v: String) {
        val res = FormValidatorUtil.validateName(v)
        ui = ui.copy(lName = res.value, lNameError = res.error)
    }

    fun onStreet(v: String) {
        val res = FormValidatorUtil.validateName(v)
        ui = ui.copy(street = res.value, streetError = res.error)
    }

    fun onOtherName(v: String) { ui = ui.copy(oName = v) }

    fun onInvitedBy(v: Individual) { ui = ui.copy(invitedBy = v) }
    fun onEduPref(v: EducationPreference) { ui = ui.copy(educationPreference = v) }
    fun onTechSkills(v: String) { ui = ui.copy(technicalSkills = v) }
    fun onFormerSponsor(v: Relationship) { ui = ui.copy(formerSponsor = v) }
    fun onFormerSponsorOther(v: String) { ui = ui.copy(formerSponsorOther = v) }
    fun onConfessedBy(v: ConfessedBy) { ui = ui.copy(confessedBy = v) }
    fun onMinistryName(v: String) { ui = ui.copy(ministryName = v) }

    fun onGender(v: Gender) { ui = ui.copy(gender = v) }
    fun onDobVerified(v: Boolean) { ui = ui.copy(dobVerified = v) }

    fun onSubCounty(v: String?) { ui = ui.copy(subCounty = v ?: "") }
    fun onSponsored(v: Boolean) { ui = ui.copy(sponsoredForEducation = v) }
    fun onSponsorNotes(v: String?) { ui = ui.copy(sponsorNotes = v ?: "") }

    fun onGraduated(checked: Boolean) {
        ui = ui.copy(graduated = if (checked) Reply.YES else Reply.NO)
    }

    fun generalNotes(v: String?) { ui = ui.copy(generalComments = v ?: "") }

    fun onCountry(v: Country) { ui = ui.copy(country = v) }

    fun onClassGroup(v: ClassGroup) { ui = ui.copy(classGroup = v) }

    private val kampalaTz = TimeZone.getTimeZone("Africa/Kampala")

    private fun dobFromAge(age: Int): Timestamp {
        val cal = Calendar.getInstance(kampalaTz)
        cal.add(Calendar.YEAR, -age)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Timestamp(cal.time)
    }

    private fun yearsBetweenDobAndToday(dob: Timestamp): Int {
        val now = Calendar.getInstance(kampalaTz)
        val birth = Calendar.getInstance(kampalaTz).apply { time = dob.toDate() }

        var years = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        val birthdayThisYear =
            now.get(Calendar.MONTH) > birth.get(Calendar.MONTH) ||
                    (now.get(Calendar.MONTH) == birth.get(Calendar.MONTH) &&
                            now.get(Calendar.DAY_OF_MONTH) >= birth.get(Calendar.DAY_OF_MONTH))

        if (!birthdayThisYear) years -= 1
        return years.coerceIn(0, MAX_AGE)
    }

    fun onAge(text: String) {
        val clean = text.filter { it.isDigit() }.take(2)
        var newUi = ui.copy(ageText = clean)

        val age = clean.toIntOrNull()
        if (age != null && age in 0..MAX_AGE) {
            val inferredDob = dobFromAge(age)
            newUi = newUi.copy(
                dob = inferredDob,
                dobVerified = false,
                program = programForAge(age),
                classGroup = classGroupForAge(age)
            )
        }

        ui = newUi
    }

    fun onDob(dob: Timestamp?) {
        var newUi = ui.copy(dob = dob)
        if (dob != null) {
            val age = yearsBetweenDobAndToday(dob)
            newUi = newUi.copy(
                ageText = age.toString(),
                program = programForAge(age),
                classGroup = classGroupForAge(age)
            )
        }
        ui = newUi
    }

    private fun programForAge(age: Int): Program {
        return when {
            age >= 17 -> Program.BROTHERS_AND_SISTERS_OF_ZION
            age >= 0 -> Program.CHILDREN_OF_ZION
            else -> ui.program
        }
    }

    private fun classGroupForAge(age: Int): ClassGroup {
        return when (age) {
            in 0..5 -> ClassGroup.SERGEANT
            in 6..9 -> ClassGroup.LIEUTENANT
            in 10..12 -> ClassGroup.CAPTAIN
            in 13..16 -> ClassGroup.GENERAL
            in 17..MAX_AGE -> ClassGroup.BROTHERS_AND_SISTERS_OF_ZION
            else -> ui.classGroup
        }
    }




    @RequiresApi(Build.VERSION_CODES.O)
    fun validateStep(s: RegistrationStatus): Boolean =
        when (s) {
            RegistrationStatus.BASICINFOR -> {
                val fRes = FormValidatorUtil.validateName(ui.fName)
                val lRes = FormValidatorUtil.validateName(ui.lName)
//                val streetRes = FormValidatorUtil.validateName(ui.street)
                val ageRes = FormValidatorUtil.validateAgeString(
                    ui.ageText.orEmpty(),
                    minAge = 0,
                    maxAge = MAX_AGE
                )

                ui = ui.copy(
                    fName = fRes.value, fNameError = fRes.error,
                    lName = lRes.value, lNameError = lRes.error,
//                    street = streetRes.value, streetError = streetRes.error,
                    ageText = ageRes.value.first.toString(),
                    ageError = ageRes.error,
                    error = null
                )

                val ok = fRes.isValid && lRes.isValid  && ageRes.isValid
                if (!ok) {
                    ui = ui.copy(error = "Please fix the highlighted fields.")
                    _events.trySend(ChildFormEvent.Error("Missing or invalid fields"))
                }
                ok
            }
            else -> {
                ui = ui.copy(error = null)
                true
            }
        }

    fun ensureNewIdIfNeeded() {
        if (ui.childId.isBlank()) {
            val now = Timestamp.now()
            ui = ui.copy(
                childId = GenerateId.generateId("child"),
                createdAt = now,
                isNew = true
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun save() = viewModelScope.launch {
        ui = ui.copy(saving = true, error = null)

        if (!validateStep(RegistrationStatus.BASICINFOR)) {
            ui = ui.copy(saving = false)
            _events.trySend(ChildFormEvent.Error("Missing/invalid basic info"))
            return@launch
        }

        if (ui.isNew && ui.childId.isBlank()) ensureNewIdIfNeeded()
        val now = Timestamp.now()
        val id = ui.childId

        val stagedPath = ui.profileImageStagedLocalPath
        if (stagedPath.isNotBlank()) {
            val promotedPath = ChildImageFileHelper.promoteChildProfileStagedFile(appContext, id)
            if (!promotedPath.isNullOrBlank()) {
                ui = ui.copy(
                    profileImageLocalPath = promotedPath,
                    profileImageStagedLocalPath = "",
                    profileImageUpdatedAt = Timestamp.now()
                )
            }
        }

        val child = buildChild(id = id, now = now, status = ui.registrationStatus)
        runCatching { repo.upsert(child, isNew = ui.isNew) }
            .onSuccess {
                ui = ui.copy(saving = false, childId = id, isNew = false)
                _events.trySend(ChildFormEvent.Saved(id))
            }
            .onFailure {
                ui = ui.copy(saving = false, error = it.message ?: "Failed to save")
                _events.trySend(ChildFormEvent.Error("Failed to save"))
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun finish() = save()

    // inside ChildFormViewModel (same file)
    fun onProfileImageSelected(localPath: String) {
        ui = ui.copy(
            profileImageStagedLocalPath = localPath,
            error = null
        )
    }

    fun clearProfilePhoto() {
        if (ui.childId.isNotBlank()) {
            ChildImageFileHelper.deleteChildProfileStagedFile(appContext, ui.childId)
        }
        ui = ui.copy(
            profileImg = "",
            profileImageStoragePath = "",
            profileImageLocalPath = "",
            profileImageStagedLocalPath = "",
            profileImageUpdatedAt = null
        )
    }

    fun discardStagedProfileImage() {
        if (ui.childId.isNotBlank()) {
            ChildImageFileHelper.deleteChildProfileStagedFile(appContext, ui.childId)
        }
        ui = ui.copy(profileImageStagedLocalPath = "")
    }

    fun onError(msg: String) {
        ui = ui.copy(error = msg)
        _events.trySend(ChildFormEvent.Error(msg))
    }


    private fun buildChild(id: String, now: Timestamp, status: RegistrationStatus): Child {
        val derivedAge = ui.ageText.toIntOrNull() ?: 0

       return   Child(
            childId = id,

           profileImg = ui.profileImg,
           profileImageStoragePath = ui.profileImageStoragePath,
           profileImageLocalPath = ui.profileImageLocalPath,
           profileImageUpdatedAt = ui.profileImageUpdatedAt,
//           profileImageStagedLocalPath = ui.profileImageStagedLocalPath,
           fName = ui.fName.trim(),

            lName = ui.lName.trim(),
            oName = ui.oName.trim(),
            gender = ui.gender,
//            age = ui.ageText.toIntOrNull() ?: 0,

            age = derivedAge,
            ninNumber = ui.ninNumber.trim(),
            childType = ui.childType,
            program = programForAge(derivedAge),
            dob = ui.dob,
            dobVerified = ui.dobVerified,

            street = ui.street,
            personalPhone1 = ui.personalPhone1,
            personalPhone2 = ui.personalPhone2,

            invitedBy = ui.invitedBy,
            invitedByIndividualId = ui.invitedByIndividualId,
            invitedByTypeOther = ui.invitedByTypeOther,
            educationPreference = ui.educationPreference,
            technicalSkills = ui.technicalSkills,

            leftHomeDate = ui.leftHomeDate,
            reasonLeftHome = ui.reasonLeftHome,
            leaveStreetDate = ui.leaveStreetDate,

            educationLevel = ui.educationLevel,
            lastClass = ui.lastClass,
            previousSchool = ui.previousSchool,
            reasonLeftSchool = ui.reasonLeftSchool,
            formerSponsor = ui.formerSponsor,
            formerSponsorOther = ui.formerSponsorOther,

            country = ui.country,
            resettlementPreference = ui.resettlementPreference,
            resettlementPreferenceOther = ui.resettlementPreferenceOther,
            resettled = ui.resettled,
            resettlementDate = ui.resettlementDate,
            region = ui.region,
            district = ui.district,
            county = ui.county,
            subCounty = ui.subCounty,
            parish = ui.parish,
            village = ui.village,

            memberFName1 = ui.memberFName1,
            memberLName1 = ui.memberLName1,
            relationship1 = ui.relationship1,
            telephone1a = ui.telephone1a,
            telephone1b = ui.telephone1b,

            member1AncestralCountry = ui.member1AncestralCountry,
            member1AncestralRegion = ui.member1AncestralRegion,
            member1AncestralDistrict = ui.member1AncestralDistrict,
            member1AncestralCounty = ui.member1AncestralCounty,
            member1AncestralSubCounty = ui.member1AncestralSubCounty,
            member1AncestralParish = ui.member1AncestralParish,
            member1AncestralVillage = ui.member1AncestralVillage,

            member1RentalCountry = ui.member1RentalCountry,
            member1RentalRegion = ui.member1RentalRegion,
            member1RentalDistrict = ui.member1RentalDistrict,
            member1RentalCounty = ui.member1RentalCounty,
            member1RentalSubCounty = ui.member1RentalSubCounty,
            member1RentalParish = ui.member1RentalParish,
            member1RentalVillage = ui.member1RentalVillage,

            memberFName2 = ui.memberFName2,
            memberLName2 = ui.memberLName2,
            relationship2 = ui.relationship2,
            telephone2a = ui.telephone2a,
            telephone2b = ui.telephone2b,

            member2AncestralCountry = ui.member2AncestralCountry,
            member2AncestralRegion = ui.member2AncestralRegion,
            member2AncestralDistrict = ui.member2AncestralDistrict,
            member2AncestralCounty = ui.member2AncestralCounty,
            member2AncestralSubCounty = ui.member2AncestralSubCounty,
            member2AncestralParish = ui.member2AncestralParish,
            member2AncestralVillage = ui.member2AncestralVillage,

            member2RentalCountry = ui.member2RentalCountry,
            member2RentalRegion = ui.member2RentalRegion,
            member2RentalDistrict = ui.member2RentalDistrict,
            member2RentalCounty = ui.member2RentalCounty,
            member2RentalSubCounty = ui.member2RentalSubCounty,
            member2RentalParish = ui.member2RentalParish,
            member2RentalVillage = ui.member2RentalVillage,

            memberFName3 = ui.memberFName3,
            memberLName3 = ui.memberLName3,
            relationship3 = ui.relationship3,
            telephone3a = ui.telephone3a,
            telephone3b = ui.telephone3b,

            member3AncestralCountry = ui.member3AncestralCountry,
            member3AncestralRegion = ui.member3AncestralRegion,
            member3AncestralDistrict = ui.member3AncestralDistrict,
            member3AncestralCounty = ui.member3AncestralCounty,
            member3AncestralSubCounty = ui.member3AncestralSubCounty,
            member3AncestralParish = ui.member3AncestralParish,
            member3AncestralVillage = ui.member3AncestralVillage,

            member3RentalCountry = ui.member3RentalCountry,
            member3RentalRegion = ui.member3RentalRegion,
            member3RentalDistrict = ui.member3RentalDistrict,
            member3RentalCounty = ui.member3RentalCounty,
            member3RentalSubCounty = ui.member3RentalSubCounty,
            member3RentalParish = ui.member3RentalParish,
            member3RentalVillage = ui.member3RentalVillage,

            acceptedJesus = ui.acceptedJesus,
            confessedBy = ui.confessedBy,
            ministryName = ui.ministryName,
            acceptedJesusDate = ui.acceptedJesusDate,
            whoPrayed = ui.whoPrayed,
            whoPrayedOther = ui.whoPrayedOther,
            whoPrayedId = ui.whoPrayedId,
            classGroup = classGroupForAge(derivedAge),
            outcome = ui.outcome,
            generalComments = ui.generalComments,

            partnershipForEducation = ui.sponsoredForEducation,
            partnerId = ui.sponsorId,
            partnerFName = ui.sponsorFName,
            partnerLName = ui.sponsorLName,
            partnerTelephone1 = ui.sponsorTelephone1,
            partnerTelephone2 = ui.sponsorTelephone2,
            partnerEmail = ui.sponsorEmail,
            partnerNotes = ui.sponsorNotes,

            registrationStatus = status,
            graduated = ui.graduated,
            createdAt = ui.createdAt ?: now,
            updatedAt = now
        )
    }

    private fun ChildFormUiState.from(c: Child) = copy(
        childId = c.childId,
        profileImg = c.profileImg,
        profileImageStoragePath = c.profileImageStoragePath,
        profileImageLocalPath = c.profileImageLocalPath,
        profileImageUpdatedAt = c.profileImageUpdatedAt,
//        profileImageStagedLocalPath = c.profileImageStagedLocalPath,
        fName = c.fName,

        lName = c.lName,
        oName = c.oName,
        gender = c.gender,
        ageText = c.age.toString(),
        ninNumber = c.ninNumber,
        childType = c.childType,
        program = if (c.age >= 0) programForAge(c.age) else c.program,
        dob = c.dob,
//        ageText = c.age.toString(),
//        dob = c.dob,
        dobVerified = c.dobVerified,
        street = c.street,
        personalPhone1 = c.personalPhone1,
        personalPhone2 = c.personalPhone2,
        invitedBy = c.invitedBy,
        invitedByIndividualId = c.invitedByIndividualId,
        invitedByTypeOther = c.invitedByTypeOther,
        educationPreference = c.educationPreference,
        technicalSkills = c.technicalSkills,

        leftHomeDate = c.leftHomeDate,
        reasonLeftHome = c.reasonLeftHome,
        leaveStreetDate = c.leaveStreetDate,

        educationLevel = c.educationLevel,
        lastClass = c.lastClass,
        previousSchool = c.previousSchool,
        reasonLeftSchool = c.reasonLeftSchool,
        formerSponsor = c.formerSponsor,
        formerSponsorOther = c.formerSponsorOther,

        resettlementPreference = c.resettlementPreference,
        resettlementPreferenceOther = c.resettlementPreferenceOther,
        resettled = c.resettled,
        resettlementDate = c.resettlementDate,
        country = c.country,
        region = c.region,
        district = c.district,
        county = c.county,
        subCounty = c.subCounty,
        parish = c.parish,
        village = c.village,

        memberFName1 = c.memberFName1,
        memberLName1 = c.memberLName1,
        relationship1 = c.relationship1,
        telephone1a = c.telephone1a,
        telephone1b = c.telephone1b,
        member1AncestralCountry = c.member1AncestralCountry,
        member1AncestralRegion = c.member1AncestralRegion,
        member1AncestralDistrict = c.member1AncestralDistrict,
        member1AncestralCounty = c.member1AncestralCounty,
        member1AncestralSubCounty = c.member1AncestralSubCounty,
        member1AncestralParish = c.member1AncestralParish,
        member1AncestralVillage = c.member1AncestralVillage,

        member1RentalCountry = c.member1RentalCountry,
        member1RentalRegion = c.member1RentalRegion,
        member1RentalDistrict = c.member1RentalDistrict,
        member1RentalCounty = c.member1RentalCounty,
        member1RentalSubCounty = c.member1RentalSubCounty,
        member1RentalParish = c.member1RentalParish,
        member1RentalVillage = c.member1RentalVillage,

        memberFName2 = c.memberFName2,
        memberLName2 = c.memberLName2,
        relationship2 = c.relationship2,
        telephone2a = c.telephone2a,
        telephone2b = c.telephone2b,
        member2AncestralCountry = c.member2AncestralCountry,
        member2AncestralRegion = c.member2AncestralRegion,
        member2AncestralDistrict = c.member2AncestralDistrict,
        member2AncestralCounty = c.member2AncestralCounty,
        member2AncestralSubCounty = c.member2AncestralSubCounty,
        member2AncestralParish = c.member2AncestralParish,
        member2AncestralVillage = c.member2AncestralVillage,

        member2RentalCountry = c.member2RentalCountry,
        member2RentalRegion = c.member2RentalRegion,
        member2RentalDistrict = c.member2RentalDistrict,
        member2RentalCounty = c.member2RentalCounty,
        member2RentalSubCounty = c.member2RentalSubCounty,
        member2RentalParish = c.member2RentalParish,
        member2RentalVillage = c.member2RentalVillage,

        memberFName3 = c.memberFName3,
        memberLName3 = c.memberLName3,
        relationship3 = c.relationship3,
        telephone3a = c.telephone3a,
        telephone3b = c.telephone3b,
        member3AncestralCountry = c.member3AncestralCountry,
        member3AncestralRegion = c.member3AncestralRegion,
        member3AncestralDistrict = c.member3AncestralDistrict,
        member3AncestralCounty = c.member3AncestralCounty,
        member3AncestralSubCounty = c.member3AncestralSubCounty,
        member3AncestralParish = c.member3AncestralParish,
        member3AncestralVillage = c.member3AncestralVillage,

        member3RentalCountry = c.member3RentalCountry,
        member3RentalRegion = c.member3RentalRegion,
        member3RentalDistrict = c.member3RentalDistrict,
        member3RentalCounty = c.member3RentalCounty,
        member3RentalSubCounty = c.member3RentalSubCounty,
        member3RentalParish = c.member3RentalParish,
        member3RentalVillage = c.member3RentalVillage,

        acceptedJesus = c.acceptedJesus,
        confessedBy = c.confessedBy,
        ministryName = c.ministryName,
        acceptedJesusDate = c.acceptedJesusDate,
        whoPrayed = c.whoPrayed,
        whoPrayedOther = c.whoPrayedOther,
        whoPrayedId = c.whoPrayedId,
        classGroup = c.classGroup,
        outcome = c.outcome,
        generalComments = c.generalComments,

        sponsoredForEducation = c.partnershipForEducation,
        sponsorId = c.partnerId,
        sponsorFName = c.partnerFName,
        sponsorLName = c.partnerLName,
        sponsorTelephone1 = c.partnerTelephone1,
        sponsorTelephone2 = c.partnerTelephone2,
        sponsorEmail = c.partnerEmail,
        sponsorNotes = c.partnerNotes,

        graduated = c.graduated,
        registrationStatus = c.registrationStatus,
        createdAt = c.createdAt
    )
}

// -------------------- UI STATE --------------------
data class ChildFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val isNew: Boolean = true,

    val fNameError: String? = null,
    val lNameError: String? = null,
    val ageError: String? = null,
    val streetError: String? = null,

    val childId: String = "",
    val profileImg: String = "",
    val profileImageStoragePath: String = "",
    val profileImageLocalPath: String = "",
    val profileImageUpdatedAt: Timestamp? = null,
    val profileImageStagedLocalPath: String = "",

    val fName: String = "",
    val lName: String = "",
    val oName: String = "",

    val ageText: String = "",
    val gender: Gender = Gender.MALE,
    val ninNumber: String = "",
    val childType: ChildType = ChildType.FAMILY,
    val program: Program = Program.CHILDREN_OF_ZION,

    val dob: Timestamp? = null,
    val dobVerified: Boolean = false,

    val street: String = "",
    val personalPhone1: String = "",
    val personalPhone2: String = "",

    val invitedBy: Individual = Individual.UNCLE,
    val invitedByIndividualId: String = "",
    val invitedByTypeOther: String = "",

    val educationPreference: EducationPreference = EducationPreference.NONE,
    val technicalSkills: String = "",

    val leftHomeDate: Timestamp? = null,
    val reasonLeftHome: String = "",
    val leaveStreetDate: Timestamp? = null,

    val educationLevel: EducationLevel = EducationLevel.NONE,
    val lastClass: String = "",
    val previousSchool: String = "",
    val reasonLeftSchool: String = "",
    val formerSponsor: Relationship = Relationship.NONE,
    val formerSponsorOther: String = "",

    val country: Country = Country.UGANDA,
    val resettlementPreference: ResettlementPreference = ResettlementPreference.DIRECT_HOME,
    val resettlementPreferenceOther: String = "",
    val resettled: Boolean = false,
    val resettlementDate: Timestamp? = null,
    val region: String = "",
    val district: String = "",
    val county: String = "",
    val subCounty: String = "",
    val parish: String = "",
    val village: String = "",

    val memberFName1: String = "",
    val memberLName1: String = "",
    val relationship1: Relationship = Relationship.NONE,
    val telephone1a: String = "",
    val telephone1b: String = "",

    val member1AncestralCountry: Country = Country.UGANDA,
    val member1AncestralRegion: String = "",
    val member1AncestralDistrict: String = "",
    val member1AncestralCounty: String = "",
    val member1AncestralSubCounty: String = "",
    val member1AncestralParish: String = "",
    val member1AncestralVillage: String = "",

    val member1RentalCountry: Country = Country.UGANDA,
    val member1RentalRegion: String = "",
    val member1RentalDistrict: String = "",
    val member1RentalCounty: String = "",
    val member1RentalSubCounty: String = "",
    val member1RentalParish: String = "",
    val member1RentalVillage: String = "",

    val memberFName2: String = "",
    val memberLName2: String = "",
    val relationship2: Relationship = Relationship.NONE,
    val telephone2a: String = "",
    val telephone2b: String = "",

    val member2AncestralCountry: Country = Country.UGANDA,
    val member2AncestralRegion: String = "",
    val member2AncestralDistrict: String = "",
    val member2AncestralCounty: String = "",
    val member2AncestralSubCounty: String = "",
    val member2AncestralParish: String = "",
    val member2AncestralVillage: String = "",

    val member2RentalCountry: Country = Country.UGANDA,
    val member2RentalRegion: String = "",
    val member2RentalDistrict: String = "",
    val member2RentalCounty: String = "",
    val member2RentalSubCounty: String = "",
    val member2RentalParish: String = "",
    val member2RentalVillage: String = "",

    val memberFName3: String = "",
    val memberLName3: String = "",
    val relationship3: Relationship = Relationship.NONE,
    val telephone3a: String = "",
    val telephone3b: String = "",

    val member3AncestralCountry: Country = Country.UGANDA,
    val member3AncestralRegion: String = "",
    val member3AncestralDistrict: String = "",
    val member3AncestralCounty: String = "",
    val member3AncestralSubCounty: String = "",
    val member3AncestralParish: String = "",
    val member3AncestralVillage: String = "",

    val member3RentalCountry: Country = Country.UGANDA,
    val member3RentalRegion: String = "",
    val member3RentalDistrict: String = "",
    val member3RentalCounty: String = "",
    val member3RentalSubCounty: String = "",
    val member3RentalParish: String = "",
    val member3RentalVillage: String = "",

    val acceptedJesus: Reply = Reply.NO,
    val confessedBy: ConfessedBy = ConfessedBy.NONE,
    val ministryName: String = " ",
    val acceptedJesusDate: Timestamp? = null,
    val whoPrayed: Individual = Individual.UNCLE,
    val whoPrayedOther: String = "",
    val whoPrayedId: String = "",
    val classGroup: ClassGroup = ClassGroup.SERGEANT,
    val outcome: String = "",
    val generalComments: String = "",

    val registrationStatus: RegistrationStatus = RegistrationStatus.BASICINFOR,
    val graduated: Reply = Reply.NO,

    val sponsoredForEducation: Boolean = false,
    val sponsorId: String = "",
    val sponsorFName: String = "",
    val sponsorLName: String = "",
    val sponsorTelephone1: String = "",
    val sponsorTelephone2: String = "",
    val sponsorEmail: String = "",
    val sponsorNotes: String = "",

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

