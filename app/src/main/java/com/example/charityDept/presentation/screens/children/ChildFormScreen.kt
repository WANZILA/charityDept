@file:Suppress("NAME_SHADOWING")

package com.example.charityDept.presentation.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.charityDept.data.model.*
import com.example.charityDept.presentation.screens.widgets.PickerDialog
import com.example.charityDept.presentation.screens.children.ChildFormUiState
import com.example.charityDept.presentation.screens.children.ChildFormViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildFormScreen(
    childIdArg: String?,
    onFinished: (String) -> Unit,
    onSave: () -> Unit,
    navigateUp: () -> Unit,
    toList: () -> Unit,
    vm: ChildFormViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ui = vm.ui
    val step = vm.step
    val state = vm.ui
    val snackbarHostState = remember { SnackbarHostState() }
    val savingOrLoading = state.saving || state.loading

    LaunchedEffect(childIdArg) {
        if (childIdArg.isNullOrBlank()) vm.ensureNewIdIfNeeded() else vm.loadForEdit(childIdArg)
    }

    LaunchedEffect(vm) {
        vm.events.collect { ev ->
            when (ev) {
                is ChildFormViewModel.ChildFormEvent.Error -> {
                    snackbarHostState.showSnackbar(ev.msg)
                }
                is ChildFormViewModel.ChildFormEvent.Saved -> {
                    onFinished(ev.id)
                    snackbarHostState.showSnackbar("Saved")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (childIdArg.isNullOrBlank()) Text("Register Child") else Text("Edit Child")
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            Icons.Default.ArrowCircleLeft,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = toList) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
            )
        },
        bottomBar = {
            StepNavBar(
                step = step,
                onBack = { vm.goBack() },
                onNext = {
                    if (step == RegistrationStatus.SPIRITUAL) {
                        vm.jumpToStep(RegistrationStatus.COMPLETE)
                        vm.finish()
                        onFinished(vm.ui.childId)
                    } else vm.goNext()
                },
                onSave = {
                    if (vm.validateStep(step)) {
                        vm.finish()
                        navigateUp()
                    }
                },
                nextLabel = if (step == RegistrationStatus.SPIRITUAL) "Finish" else "Next",
                backEnabled = step != RegistrationStatus.BASICINFOR,
                actionsEnabled = !savingOrLoading
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { inner ->
        Column(
            modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            StepHeader(step = step, onStepClicked = vm::jumpToStep)
            Spacer(Modifier.height(12.dp))

            when (step) {
                RegistrationStatus.BASICINFOR -> StepBasicInfo(uiState = ui, vm = vm)
                RegistrationStatus.BACKGROUND -> StepBackground(uiState = ui, vm = vm)
                RegistrationStatus.EDUCATION -> StepEducation(uiState = ui, vm = vm)
                RegistrationStatus.FAMILY -> StepFamily(uiState = ui, vm = vm)
                RegistrationStatus.SPONSORSHIP -> StepSponsorship(uiState = ui, vm = vm)
                RegistrationStatus.SPIRITUAL -> StepSpiritual(uiState = ui, vm = vm)
                RegistrationStatus.COMPLETE -> StepComplete(uiState = ui)
            }

            if (ui.loading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            ui.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StepNavBar(
    step: RegistrationStatus,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit,
    nextLabel: String,
    backEnabled: Boolean,
    actionsEnabled: Boolean
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onSave, enabled = actionsEnabled) { Text("Save") }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onBack, enabled = actionsEnabled && backEnabled) { Text("Back") }
            Button(onClick = onNext, enabled = actionsEnabled) { Text(nextLabel) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepHeader(
    step: RegistrationStatus,
    onStepClicked: (RegistrationStatus) -> Unit
) {
    val steps = listOf(
        RegistrationStatus.BASICINFOR to "Basic",
        RegistrationStatus.BACKGROUND to "Background",
        RegistrationStatus.EDUCATION to "Education",
        RegistrationStatus.FAMILY to "Family",
        RegistrationStatus.SPONSORSHIP to "Sponsorship",
        RegistrationStatus.SPIRITUAL to "Spiritual"
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        steps.forEach { (s, label) ->
            FilterChip(
                selected = step == s,
                onClick = { onStepClicked(s) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepBasicInfo(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val streetDisplay = vm.ui.street.trim().ifBlank { "Tap to choose Street" }
        var showStreetDialog by remember { mutableStateOf(false) }

        AppTextField(
            value = uiState.fName,
            onValueChange = vm::onFirstName,
            label = "First name*",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            trailingIcon = {
                if (uiState.fName.isNotEmpty()) {
                    IconButton(onClick = { vm.onFirstName("") }) {
                        Icon(Icons.Outlined.Clear, "Clear")
                    }
                }
            },
            errorText = uiState.fNameError
        )

        AppTextField(
            value = uiState.lName,
            onValueChange = vm::onLastName,
            label = "Last name*",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            trailingIcon = {
                if (uiState.lName.isNotEmpty()) {
                    IconButton(onClick = { vm.onLastName("") }) {
                        Icon(Icons.Outlined.Clear, "Clear")
                    }
                }
            },
            errorText = uiState.lNameError
        )

        AppTextField(
            value = uiState.oName,
            onValueChange = vm::onOtherName,
            label = "Other name",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            trailingIcon = {
                if (uiState.oName.isNotEmpty()) {
                    IconButton(onClick = { vm.onOtherName("") }) {
                        Icon(Icons.Outlined.Clear, "Clear")
                    }
                }
            }
        )

        EnumDropdown(
            title = "Gender",
            selected = uiState.gender,
            values = Gender.values().toList(),
            onSelected = vm::onGender,
            labelFor = ::labelForGender,
            iconFor = ::iconForGender
        )

        AppTextField(
            value = uiState.ageText,
            onValueChange = vm::onAge,
            label = "Age (0–25)",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            leadingIcon = { Icon(Icons.Outlined.Badge, null) },
            trailingIcon = {
                if (uiState.ageText.isNotEmpty()) {
                    IconButton(onClick = { vm.onAge("") }) {
                        Icon(Icons.Outlined.Clear, "Clear")
                    }
                }
            },
            errorText = uiState.ageError
        )

        AppDateField(
            label = "Date of Birth",
            value = uiState.dob,
            onChanged = vm::onDob,
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("DOB verified")
            Spacer(Modifier.width(12.dp))
            Switch(checked = uiState.dobVerified, onCheckedChange = vm::onDobVerified)
        }

        AppTextField(
            value = labelForProgram(uiState.program),
            onValueChange = { },
            label = "Program",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(iconForProgram(uiState.program), null) },
            readOnly = true
        )
        EnumDropdown(
            title = "Child type",
            selected = uiState.childType,
            values = ChildType.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(childType = it) },
            labelFor = ::labelForChildType,
            iconFor = ::iconForChildType
        )

        if (uiState.childType == ChildType.STREET) {
            AppTextField(
                value = streetDisplay,
                onValueChange = { /* read-only */ },
                label = "Street",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStreetDialog = true },
                leadingIcon = { Icon(Icons.Outlined.PanTool, null) },
                readOnly = true,
                enabled = false,
            )

            if (showStreetDialog) {
                PickerDialog(
                    title = "Select Street",
                    feature = vm.streetPicker,
                    onPicked = { vm.onStreetPicked(it); showStreetDialog = false },
                    onDismiss = { showStreetDialog = false }
                )
            }
        }
        EnumDropdown(
            title = "Accepted Jesus?",
            selected = uiState.acceptedJesus,
            values = Reply.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(acceptedJesus = it) },
            labelFor = ::labelForReply,
            iconFor = ::iconForReply
        )

        EnumDropdown(
            title = "Invited by",
            selected = uiState.invitedBy,
            values = Individual.values().toList(),
            onSelected = vm::onInvitedBy,
            labelFor = ::labelForIndividual,
            iconFor = ::iconForIndividual
        )

        if (uiState.invitedBy == Individual.OTHER) {
            AppTextField(
                value = uiState.invitedByTypeOther,
                onValueChange = { vm.ui = vm.ui.copy(invitedByTypeOther = it) },
                label = "Invited by (Other - text)",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.EditNote, null) }
            )
        }

        EnumDropdown(
            title = "Education preference",
            selected = uiState.educationPreference,
            values = EducationPreference.values().toList(),
            onSelected = vm::onEduPref,
            labelFor = ::labelForEducationPreference,
            iconFor = ::iconForEducationPreference
        )

        if (uiState.childType == ChildType.STREET) {
            EnumDropdown(
                title = "Resettlement preference",
                selected = uiState.resettlementPreference,
                values = ResettlementPreference.values().toList(),
                onSelected = { vm.ui = vm.ui.copy(resettlementPreference = it) },
                labelFor = ::labelForResettlementPreference,
                iconFor = ::iconForResettlementPreference
            )
        }
    }
}

@Composable
private fun StepBackground(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppTextField(
            value = uiState.reasonLeftHome,
            onValueChange = { vm.ui = vm.ui.copy(reasonLeftHome = it) },
            label = "Reason left home",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Info, null) }
        )

        AppDateField(
            label = "Left home date",
            value = uiState.leftHomeDate,
            onChanged = { vm.ui = vm.ui.copy(leftHomeDate = it) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
        )
    }
}

@Composable
private fun StepEducation(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    val skillsDisplay = vm.ui.technicalSkills.trim().ifBlank { "Tap to choose skill" }
    var showTechSkillDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.educationPreference == EducationPreference.SKILLING) {
            AppTextField(
                value = skillsDisplay,
                onValueChange = { /* read-only */ },
                label = "Select skill",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTechSkillDialog = true },
                leadingIcon = { Icon(Icons.Outlined.PanTool, null) },
                readOnly = true
            )

            if (showTechSkillDialog) {
                PickerDialog(
                    title = "Select skill",
                    feature = vm.technicalSkillsPicker,
                    onPicked = { vm.onTechnicalSkillsPicked(it); showTechSkillDialog = false },
                    onDismiss = { showTechSkillDialog = false }
                )
            }
        }

        EnumDropdown(
            title = "Educational Level",
            selected = uiState.educationLevel,
            values = EducationLevel.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(educationLevel = it) },
            labelFor = ::labelForEducationLevel,
            iconFor = ::iconForEducationLevel
        )

        if (
            uiState.educationLevel == EducationLevel.NURSERY ||
            uiState.educationLevel == EducationLevel.PRIMARY ||
            uiState.educationLevel == EducationLevel.SECONDARY
        ) {
            AppTextField(
                value = uiState.lastClass,
                onValueChange = { vm.ui = vm.ui.copy(lastClass = it) },
                label = "Last class",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.School, null) }
            )
            AppTextField(
                value = uiState.previousSchool,
                onValueChange = { vm.ui = vm.ui.copy(previousSchool = it) },
                label = "Previous school",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.School, null) }
            )

            EnumDropdown(
                title = "Who was your Sponsor",
                selected = uiState.formerSponsor,
                values = Relationship.values().toList(),
                onSelected = { vm.ui = vm.ui.copy(formerSponsor = it) },
                labelFor = ::labelForRelationship,
                iconFor = ::iconForRelationship
            )

            if (uiState.formerSponsor == Relationship.OTHER) {
                AppTextField(
                    value = uiState.formerSponsorOther,
                    onValueChange = { vm.ui = vm.ui.copy(formerSponsorOther = it) },
                    label = "Who was your sponsor",
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.School, null) }
                )
            }

            AppTextField(
                value = uiState.reasonLeftSchool,
                onValueChange = { vm.ui = vm.ui.copy(reasonLeftSchool = it) },
                label = "Reason left school",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Info, null) }
            )
        }
    }
}

@Composable
private fun StepFamily(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var showM1Ancestral by rememberSaveable { mutableStateOf(false) }
        var showM1Rental by rememberSaveable { mutableStateOf(false) }

        var showM2Ancestral by rememberSaveable { mutableStateOf(false) }
        var showM2Rental by rememberSaveable { mutableStateOf(false) }

        var showM3Ancestral by rememberSaveable { mutableStateOf(false) }
        var showM3Rental by rememberSaveable { mutableStateOf(false) }

        if (uiState.childType == ChildType.STREET) {
            Text("Resettlement", style = MaterialTheme.typography.titleMedium)

            AppDateField(
                label = "Leave street date",
                value = uiState.leaveStreetDate,
                onChanged = { vm.ui = vm.ui.copy(leaveStreetDate = it) },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Resettled")
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = uiState.resettled,
                    onCheckedChange = { vm.ui = vm.ui.copy(resettled = it) }
                )
            }

            AppDateField(
                label = "Resettlement date",
                value = uiState.resettlementDate,
                onChanged = { vm.ui = vm.ui.copy(resettlementDate = it) },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
            )
        }
//        EnumDropdown(
//            title = "Country",
//            selected = uiState.country,
//            values = Country.values().toList(),
//            onSelected = { vm.ui = vm.ui.copy(country = it) },
//            labelFor = ::labelForCountry,
//            iconFor = { Icons.Outlined.Public }
//        )

        // ---- Child main address ----
        // ---- Child main address ----
        if (uiState.country == Country.UGANDA) {
            UgandaAddressBlock(
                title = "Child — Resettlement Address",
                country = uiState.country,
                onCountry = { newCountry ->
                    vm.ui = vm.ui.copy(
                        country = newCountry,
                        region = "",
                        district = "",
                        county = "",
                        subCounty = "",
                        parish = "",
                        village = ""
                    )
                },
                picker = vm.ugResettlementPicker,
                region = uiState.region,
                district = uiState.district,
                county = uiState.county,
                subCounty = uiState.subCounty,
                parish = uiState.parish,
                village = uiState.village
            )
        } else {
            AddressBlock(
                title = "Child — Resettlement Address",
                country = uiState.country,
                onCountry = { newCountry ->
                    vm.ui = vm.ui.copy(
                        country = newCountry,
                        region = "",
                        district = "",
                        county = "",
                        subCounty = "",
                        parish = "",
                        village = ""
                    )
                },
                region = uiState.region, onRegion = { vm.ui = vm.ui.copy(region = it) },
                district = uiState.district, onDistrict = { vm.ui = vm.ui.copy(district = it) },
                county = uiState.county, onCounty = { vm.ui = vm.ui.copy(county = it) },
                subCounty = uiState.subCounty, onSubCounty = { vm.ui = vm.ui.copy(subCounty = it) },
                parish = uiState.parish, onParish = { vm.ui = vm.ui.copy(parish = it) },
                village = uiState.village, onVillage = { vm.ui = vm.ui.copy(village = it) }
            )
        }

//        Divider(Modifier.padding(vertical = 8.dp))
        if (uiState.program == Program.BROTHERS_AND_SISTERS_OF_ZION) {
            AppTextField(
                value = uiState.ninNumber,
                onValueChange = { vm.ui = vm.ui.copy(ninNumber = it) },
                label = "NIN Number",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                leadingIcon = { Icon(Icons.Outlined.Badge, null) }
            )

            Divider(Modifier.padding(vertical = 4.dp))
            Text("Child personal contacts", style = MaterialTheme.typography.titleMedium)

            PhoneRow(
                a = uiState.personalPhone1,
                b = uiState.personalPhone2,
                onA = { vm.ui = vm.ui.copy(personalPhone1 = it.filter { ch -> ch.isDigit() }) },
                onB = { vm.ui = vm.ui.copy(personalPhone2 = it.filter { ch -> ch.isDigit() }) }
            )
        }
        Text("Primary contact", style = MaterialTheme.typography.titleMedium)

        NameRow(
            first = uiState.memberFName1,
            last = uiState.memberLName1,
            onFirst = { vm.ui = vm.ui.copy(memberFName1 = it) },
            onLast = { vm.ui = vm.ui.copy(memberLName1 = it) }
        )

        EnumDropdown(
            title = "Relationship",
            selected = uiState.relationship1,
            values = Relationship.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(relationship1 = it) },
            labelFor = ::labelForRelationship,
            iconFor = ::iconForRelationship
        )

        PhoneRow(
            a = uiState.telephone1a,
            b = uiState.telephone1b,
            onA = { vm.ui = vm.ui.copy(telephone1a = it.filter { ch -> ch.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(telephone1b = it.filter { ch -> ch.isDigit() }) }
        )

        Spacer(Modifier.height(8.dp))

        ToggleRow("Add Member 1 ancestral address", showM1Ancestral) { showM1Ancestral = it }
        if (showM1Ancestral) {
            if (uiState.member1AncestralCountry == Country.UGANDA) {
                UgandaAddressBlock(
                    title = "Member 1 — Ancestral Home",
                    country = uiState.member1AncestralCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member1AncestralCountry = newCountry,
                            member1AncestralRegion = "",
                            member1AncestralDistrict = "",
                            member1AncestralCounty = "",
                            member1AncestralSubCounty = "",
                            member1AncestralParish = "",
                            member1AncestralVillage = ""
                        )
                    },
                    picker = vm.ugM1AncestralPicker,
                    region = uiState.member1AncestralRegion,
                    district = uiState.member1AncestralDistrict,
                    county = uiState.member1AncestralCounty,
                    subCounty = uiState.member1AncestralSubCounty,
                    parish = uiState.member1AncestralParish,
                    village = uiState.member1AncestralVillage
                )
            } else {
                AddressBlock(
                    title = "Member 1 — Ancestral Home",
                    country = uiState.member1AncestralCountry,
                    onCountry = {
                            newCountry ->
                        vm.ui = vm.ui.copy(
                            member1AncestralCountry = newCountry,
                            member1AncestralRegion = "",
                            member1AncestralDistrict = "",
                            member1AncestralCounty = "",
                            member1AncestralSubCounty = "",
                            member1AncestralParish = "",
                            member1AncestralVillage = ""
                        )

                    },
                    region = uiState.member1AncestralRegion, onRegion = { vm.ui = vm.ui.copy(member1AncestralRegion = it) },
                    district = uiState.member1AncestralDistrict, onDistrict = { vm.ui = vm.ui.copy(member1AncestralDistrict = it) },
                    county = uiState.member1AncestralCounty, onCounty = { vm.ui = vm.ui.copy(member1AncestralCounty = it) },
                    subCounty = uiState.member1AncestralSubCounty, onSubCounty = { vm.ui = vm.ui.copy(member1AncestralSubCounty = it) },
                    parish = uiState.member1AncestralParish, onParish = { vm.ui = vm.ui.copy(member1AncestralParish = it) },
                    village = uiState.member1AncestralVillage, onVillage = { vm.ui = vm.ui.copy(member1AncestralVillage = it) }
                )
            }
        }

        ToggleRow("Add Member 1 rental address", showM1Rental) { showM1Rental = it }
        if (showM1Rental) {
            if (uiState.member1RentalCountry == Country.UGANDA) {
                UgandaAddressBlock(
                    title = "Member 1 — Rental Home",
                    country = uiState.member1RentalCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member1RentalCountry = newCountry,
                            member1RentalRegion = "",
                            member1RentalDistrict = "",
                            member1RentalCounty = "",
                            member1RentalSubCounty = "",
                            member1RentalParish = "",
                            member1RentalVillage = ""
                        )
                    },
                    picker = vm.ugM1RentalPicker,
                    region = uiState.member1RentalRegion,
                    district = uiState.member1RentalDistrict,
                    county = uiState.member1RentalCounty,
                    subCounty = uiState.member1RentalSubCounty,
                    parish = uiState.member1RentalParish,
                    village = uiState.member1RentalVillage
                )
            } else {
                AddressBlock(
                    title = "Member 1 — Rental Home",
                    country = uiState.member1RentalCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member1RentalCountry = newCountry,
                            member1RentalRegion = "",
                            member1RentalDistrict = "",
                            member1RentalCounty = "",
                            member1RentalSubCounty = "",
                            member1RentalParish = "",
                            member1RentalVillage = ""
                        )
                    },

                    region = uiState.member1RentalRegion, onRegion = { vm.ui = vm.ui.copy(member1RentalRegion = it) },
                    district = uiState.member1RentalDistrict, onDistrict = { vm.ui = vm.ui.copy(member1RentalDistrict = it) },
                    county = uiState.member1RentalCounty, onCounty = { vm.ui = vm.ui.copy(member1RentalCounty = it) },
                    subCounty = uiState.member1RentalSubCounty, onSubCounty = { vm.ui = vm.ui.copy(member1RentalSubCounty = it) },
                    parish = uiState.member1RentalParish, onParish = { vm.ui = vm.ui.copy(member1RentalParish = it) },
                    village = uiState.member1RentalVillage, onVillage = { vm.ui = vm.ui.copy(member1RentalVillage = it) }
                )
            }
        }

        Divider(Modifier.padding(vertical = 8.dp))
        Text("Secondary contact", style = MaterialTheme.typography.titleMedium)

        NameRow(
            first = uiState.memberFName2,
            last = uiState.memberLName2,
            onFirst = { vm.ui = vm.ui.copy(memberFName2 = it) },
            onLast = { vm.ui = vm.ui.copy(memberLName2 = it) }
        )

        EnumDropdown(
            title = "Relationship",
            selected = uiState.relationship2,
            values = Relationship.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(relationship2 = it) },
            labelFor = ::labelForRelationship,
            iconFor = ::iconForRelationship
        )

        PhoneRow(
            a = uiState.telephone2a,
            b = uiState.telephone2b,
            onA = { vm.ui = vm.ui.copy(telephone2a = it.filter { ch -> ch.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(telephone2b = it.filter { ch -> ch.isDigit() }) }
        )

        ToggleRow("Add Member 2 ancestral address", showM2Ancestral) { showM2Ancestral = it }
        if (showM2Ancestral) {
            if (uiState.member2AncestralCountry == Country.UGANDA) {
                UgandaAddressBlock(
                    title = "Member 2 — Ancestral Home",
                    country = uiState.member2AncestralCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member2AncestralCountry = newCountry,
                            member2AncestralRegion = "",
                            member2AncestralDistrict = "",
                            member2AncestralCounty = "",
                            member2AncestralSubCounty = "",
                            member2AncestralParish = "",
                            member2AncestralVillage = ""
                        )
                    },
                    picker = vm.ugM2AncestralPicker,
                    region = uiState.member2AncestralRegion,
                    district = uiState.member2AncestralDistrict,
                    county = uiState.member2AncestralCounty,
                    subCounty = uiState.member2AncestralSubCounty,
                    parish = uiState.member2AncestralParish,
                    village = uiState.member2AncestralVillage
                )
            } else {
                AddressBlock(
                    title = "Member 2 — Ancestral Home",
                    country = uiState.member2AncestralCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member2AncestralCountry = newCountry,
                            member2AncestralRegion = "",
                            member2AncestralDistrict = "",
                            member2AncestralCounty = "",
                            member2AncestralSubCounty = "",
                            member2AncestralParish = "",
                            member2AncestralVillage = ""
                        )
                    },

                    region = uiState.member2AncestralRegion, onRegion = { vm.ui = vm.ui.copy(member2AncestralRegion = it) },
                    district = uiState.member2AncestralDistrict, onDistrict = { vm.ui = vm.ui.copy(member2AncestralDistrict = it) },
                    county = uiState.member2AncestralCounty, onCounty = { vm.ui = vm.ui.copy(member2AncestralCounty = it) },
                    subCounty = uiState.member2AncestralSubCounty, onSubCounty = { vm.ui = vm.ui.copy(member2AncestralSubCounty = it) },
                    parish = uiState.member2AncestralParish, onParish = { vm.ui = vm.ui.copy(member2AncestralParish = it) },
                    village = uiState.member2AncestralVillage, onVillage = { vm.ui = vm.ui.copy(member2AncestralVillage = it) }
                )
            }
        }

        ToggleRow("Add Member 2 rental address", showM2Rental) { showM2Rental = it }
        if (showM2Rental) {
            if (uiState.member2RentalCountry == Country.UGANDA) {
                UgandaAddressBlock(
                    title = "Member 2 — Rental Home",
                    country = uiState.member2RentalCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member2RentalCountry = newCountry,
                            member2RentalRegion = "",
                            member2RentalDistrict = "",
                            member2RentalCounty = "",
                            member2RentalSubCounty = "",
                            member2RentalParish = "",
                            member2RentalVillage = ""
                        )
                    },
                    picker = vm.ugM2RentalPicker,
                    region = uiState.member2RentalRegion,
                    district = uiState.member2RentalDistrict,
                    county = uiState.member2RentalCounty,
                    subCounty = uiState.member2RentalSubCounty,
                    parish = uiState.member2RentalParish,
                    village = uiState.member2RentalVillage
                )
            } else {
                AddressBlock(
                    title = "Member 2 — Rental Home",
                    country = uiState.member2RentalCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member2RentalCountry = newCountry,
                            member2RentalRegion = "",
                            member2RentalDistrict = "",
                            member2RentalCounty = "",
                            member2RentalSubCounty = "",
                            member2RentalParish = "",
                            member2RentalVillage = ""
                        )
                    }
                    ,
                    region = uiState.member2RentalRegion, onRegion = { vm.ui = vm.ui.copy(member2RentalRegion = it) },
                    district = uiState.member2RentalDistrict, onDistrict = { vm.ui = vm.ui.copy(member2RentalDistrict = it) },
                    county = uiState.member2RentalCounty, onCounty = { vm.ui = vm.ui.copy(member2RentalCounty = it) },
                    subCounty = uiState.member2RentalSubCounty, onSubCounty = { vm.ui = vm.ui.copy(member2RentalSubCounty = it) },
                    parish = uiState.member2RentalParish, onParish = { vm.ui = vm.ui.copy(member2RentalParish = it) },
                    village = uiState.member2RentalVillage, onVillage = { vm.ui = vm.ui.copy(member2RentalVillage = it) }
                )
            }
        }

        Divider(Modifier.padding(vertical = 8.dp))
        Text("Tertiary contact", style = MaterialTheme.typography.titleMedium)

        NameRow(
            first = uiState.memberFName3,
            last = uiState.memberLName3,
            onFirst = { vm.ui = vm.ui.copy(memberFName3 = it) },
            onLast = { vm.ui = vm.ui.copy(memberLName3 = it) }
        )

        EnumDropdown(
            title = "Relationship",
            selected = uiState.relationship3,
            values = Relationship.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(relationship3 = it) },
            labelFor = ::labelForRelationship,
            iconFor = ::iconForRelationship
        )

        PhoneRow(
            a = uiState.telephone3a,
            b = uiState.telephone3b,
            onA = { vm.ui = vm.ui.copy(telephone3a = it.filter { ch -> ch.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(telephone3b = it.filter { ch -> ch.isDigit() }) }
        )

        ToggleRow("Add Member 3 ancestral address", showM3Ancestral) { showM3Ancestral = it }
        if (showM3Ancestral) {
            if (uiState.member3AncestralCountry == Country.UGANDA) {
                UgandaAddressBlock(
                    title = "Member 3 — Ancestral Home",
                    country = uiState.member3AncestralCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member3AncestralCountry = newCountry,
                            member3AncestralRegion = "",
                            member3AncestralDistrict = "",
                            member3AncestralCounty = "",
                            member3AncestralSubCounty = "",
                            member3AncestralParish = "",
                            member3AncestralVillage = ""
                        )
                    },
                    picker = vm.ugM3AncestralPicker,
                    region = uiState.member3AncestralRegion,
                    district = uiState.member3AncestralDistrict,
                    county = uiState.member3AncestralCounty,
                    subCounty = uiState.member3AncestralSubCounty,
                    parish = uiState.member3AncestralParish,
                    village = uiState.member3AncestralVillage
                )
            } else {
                AddressBlock(
                    title = "Member 3 — Ancestral Home",
                    country = uiState.member3AncestralCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member3AncestralCountry = newCountry,
                            member3AncestralRegion = "",
                            member3AncestralDistrict = "",
                            member3AncestralCounty = "",
                            member3AncestralSubCounty = "",
                            member3AncestralParish = "",
                            member3AncestralVillage = ""
                        )
                    }
                    ,
                    region = uiState.member3AncestralRegion, onRegion = { vm.ui = vm.ui.copy(member3AncestralRegion = it) },
                    district = uiState.member3AncestralDistrict, onDistrict = { vm.ui = vm.ui.copy(member3AncestralDistrict = it) },
                    county = uiState.member3AncestralCounty, onCounty = { vm.ui = vm.ui.copy(member3AncestralCounty = it) },
                    subCounty = uiState.member3AncestralSubCounty, onSubCounty = { vm.ui = vm.ui.copy(member3AncestralSubCounty = it) },
                    parish = uiState.member3AncestralParish, onParish = { vm.ui = vm.ui.copy(member3AncestralParish = it) },
                    village = uiState.member3AncestralVillage, onVillage = { vm.ui = vm.ui.copy(member3AncestralVillage = it) }
                )
            }
        }

        ToggleRow("Add Member 3 rental address", showM3Rental) { showM3Rental = it }
        if (showM3Rental) {
            if (uiState.member3RentalCountry == Country.UGANDA) {
                UgandaAddressBlock(
                    title = "Member 3 — Rental Home",
                    country = uiState.member3RentalCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member3RentalCountry = newCountry,
                            member3RentalRegion = "",
                            member3RentalDistrict = "",
                            member3RentalCounty = "",
                            member3RentalSubCounty = "",
                            member3RentalParish = "",
                            member3RentalVillage = ""
                        )
                    },
                    picker = vm.ugM3RentalPicker,
                    region = uiState.member3RentalRegion,
                    district = uiState.member3RentalDistrict,
                    county = uiState.member3RentalCounty,
                    subCounty = uiState.member3RentalSubCounty,
                    parish = uiState.member3RentalParish,
                    village = uiState.member3RentalVillage
                )
            } else {
                AddressBlock(
                    title = "Member 3 — Rental Home",
                    country = uiState.member3RentalCountry,
                    onCountry = { newCountry ->
                        vm.ui = vm.ui.copy(
                            member3RentalCountry = newCountry,
                            member3RentalRegion = "",
                            member3RentalDistrict = "",
                            member3RentalCounty = "",
                            member3RentalSubCounty = "",
                            member3RentalParish = "",
                            member3RentalVillage = ""
                        )
                    }
                    ,
                    region = uiState.member3RentalRegion, onRegion = { vm.ui = vm.ui.copy(member3RentalRegion = it) },
                    district = uiState.member3RentalDistrict, onDistrict = { vm.ui = vm.ui.copy(member3RentalDistrict = it) },
                    county = uiState.member3RentalCounty, onCounty = { vm.ui = vm.ui.copy(member3RentalCounty = it) },
                    subCounty = uiState.member3RentalSubCounty, onSubCounty = { vm.ui = vm.ui.copy(member3RentalSubCounty = it) },
                    parish = uiState.member3RentalParish, onParish = { vm.ui = vm.ui.copy(member3RentalParish = it) },
                    village = uiState.member3RentalVillage, onVillage = { vm.ui = vm.ui.copy(member3RentalVillage = it) }
                )
            }
        }
    }
}

@Composable
private fun StepSponsorship(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sponsorship", style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sponsored for education")
            Spacer(Modifier.width(12.dp))
            Switch(checked = uiState.sponsoredForEducation, onCheckedChange = vm::onSponsored)
        }

        AppTextField(
            value = uiState.sponsorFName,
            onValueChange = { vm.ui = vm.ui.copy(sponsorFName = it) },
            label = "Partner first name",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Person, null) }
        )
        AppTextField(
            value = uiState.sponsorLName,
            onValueChange = { vm.ui = vm.ui.copy(sponsorLName = it) },
            label = "Partner last name",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Person, null) }
        )
        PhoneRow(
            a = uiState.sponsorTelephone1,
            b = uiState.sponsorTelephone2,
            onA = { vm.ui = vm.ui.copy(sponsorTelephone1 = it.filter { ch -> ch.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(sponsorTelephone2 = it.filter { ch -> ch.isDigit() }) }
        )

        AppTextField(
            value = uiState.sponsorEmail,
            onValueChange = { vm.ui = vm.ui.copy(sponsorEmail = it) },
            label = "Partner email",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Email, null) }
        )
        AppTextField(
            value = uiState.sponsorNotes,
            onValueChange = vm::onSponsorNotes,
            label = "Partner notes",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.EditNote, null) }
        )
    }
}

@Composable
private fun StepSpiritual(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EnumDropdown(
            title = "Who prayed with them?",
            selected = uiState.whoPrayed,
            values = Individual.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(whoPrayed = it) },
            labelFor = ::labelForIndividual,
            iconFor = ::iconForIndividual
        )
        if (uiState.whoPrayed == Individual.OTHER) {
            AppTextField(
                value = uiState.whoPrayedOther,
                onValueChange = { vm.ui = vm.ui.copy(whoPrayedOther = it) },
                label = "Who prayed (Other - text)",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.EditNote, null) }
            )
        }

        AppDateField(
            label = "Spiritual decision date",
            value = uiState.acceptedJesusDate,
            onChanged = { vm.ui = vm.ui.copy(acceptedJesusDate = it) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
        )

        EnumDropdown(
            title = "Class Group",
            selected = uiState.classGroup,
            values = ClassGroup.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(classGroup = it) },
            labelFor = ::labelForClassGroup,
            iconFor = ::iconForClassGroup
        )

        AppDateField(
            label = "Registered On",
            value = uiState.createdAt,
            onChanged = { vm.ui = vm.ui.copy(createdAt = it) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
        )

        AppTextField(
            value = uiState.outcome,
            onValueChange = { vm.ui = vm.ui.copy(outcome = it) },
            label = "Notes / outcome",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.EditNote, null) },
            maxLines = 5
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Graduated")
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = uiState.graduated == Reply.YES,
                onCheckedChange = vm::onGraduated
            )
        }

        AppTextField(
            value = uiState.generalComments,
            onValueChange = { vm.ui = vm.ui.copy(generalComments = it) },
            label = "General comments",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.EditNote, null) },
            maxLines = 5
        )
    }
}

@Composable
private fun StepComplete(uiState: ChildFormUiState) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.childId.isNotBlank())
            Text("Ready to save updates for #${uiState.childId}")
        else
            Text("Review all details, then tap Save")
    }
}

@Composable
private fun NameRow(first: String, last: String, onFirst: (String) -> Unit, onLast: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTextField(
            value = first,
            onValueChange = onFirst,
            label = "First name",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
        )
        AppTextField(
            value = last,
            onValueChange = onLast,
            label = "Last name",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
        )
    }
}

@Composable
private fun PhoneRow(
    a: String,
    b: String,
    onA: (String) -> Unit,
    onB: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppTextField(
            value = a,
            onValueChange = onA,
            label = "Phone 1",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
            trailingIcon = {
                if (a.isNotEmpty()) {
                    IconButton(onClick = { onA("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                    }
                }
            }
        )
        AppTextField(
            value = b,
            onValueChange = onB,
            label = "Phone 2",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
            trailingIcon = {
                if (b.isNotEmpty()) {
                    IconButton(onClick = { onB("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDateField(
    label: String,
    value: com.google.firebase.Timestamp?,
    onChanged: (com.google.firebase.Timestamp?) -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var show by rememberSaveable { mutableStateOf(false) }

    AppTextField(
        value = formatDate(value),
        onValueChange = { /* read-only */ },
        label = label,
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        leadingIcon = leadingIcon,
        trailingIcon = {
            IconButton(onClick = { show = true }) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick date")
            }
        }
    )

    if (show) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.toDate()?.time
        )

        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    onChanged(millis?.let { com.google.firebase.Timestamp(java.util.Date(it)) })
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state, showModeToggle = true)
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    placeholder: String = "Tap to choose",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val display = value.trim().ifBlank { placeholder }

    OutlinedTextField(
        value = display,
        onValueChange = { /* read-only */ },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        readOnly = true,
        enabled = false,
        trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) }
    )
}

@Composable
private fun UgandaAddressBlock(
    title: String,
    country: Country,
    onCountry: (Country) -> Unit,
    picker: ChildFormViewModel.UgandaAddressPicker,
    region: String,
    district: String,
    county: String,
    subCounty: String,
    parish: String,
    village: String
)  {
    var showRegion by remember { mutableStateOf(false) }
    var showDistrict by remember { mutableStateOf(false) }
    var showCounty by remember { mutableStateOf(false) }
    var showSubCounty by remember { mutableStateOf(false) }
    var showParish by remember { mutableStateOf(false) }
    var showVillage by remember { mutableStateOf(false) }
    var showDistrictSearch by remember { mutableStateOf(false) }
    var showVillageSearch by remember { mutableStateOf(false) }

    Text(title, style = MaterialTheme.typography.titleSmall)

    EnumDropdown(
        title = "Country",
        selected = country,
        values = Country.entries,
        onSelected = onCountry,
        labelFor = ::labelForCountry,
        iconFor = { Icons.Outlined.Public }
    )

    // Region
    PickerField(
        label = "Search District",
        value = district,
        enabled = true,
        placeholder = "Search district and fill region",
        onClick = { showDistrictSearch = true }
    )
    if (showDistrictSearch) {
        PickerDialog(
            title = "Search District",
            feature = picker.districtSearchPicker,
            onPicked = {
                picker.onDistrictPicked(it)
                showDistrictSearch = false
            },
            onDismiss = { showDistrictSearch = false }
        )
    }

    PickerField(
        label = "Search Village",
        value = village,
        enabled = true,
        placeholder = "Search village and fill full hierarchy",
        onClick = { showVillageSearch = true }
    )
    if (showVillageSearch) {
        PickerDialog(
            title = "Search Village",
            feature = picker.villageSearchPicker,
            onPicked = {
                picker.onVillagePicked(it)
                showVillageSearch = false
            },
            onDismiss = { showVillageSearch = false }
        )
    }

    // Region
    PickerField(
        label = "Region",
        value = region,
        enabled = true,
        onClick = { showRegion = true }
    )
    if (showRegion) {
        PickerDialog(
            title = "Select Region",
            feature = picker.regionPicker,
            onPicked = {
                picker.onRegionPicked(it)
                showRegion = false
            },
            onDismiss = { showRegion = false }
        )
    }

    // District
    val districtEnabled = region.isNotBlank()
    PickerField(
        label = "District",
        value = district,
        enabled = districtEnabled,
        placeholder = if (districtEnabled) "Tap to choose" else "Select Region first",
        onClick = { showDistrict = true }
    )
    if (showDistrict) {
        PickerDialog(
            title = "Select District",
            feature = picker.districtPicker,
            onPicked = {
                picker.onDistrictPicked(it)
                showDistrict = false
            },
            onDismiss = { showDistrict = false }
        )
    }

    // County
    val countyEnabled = district.isNotBlank()
    PickerField(
        label = "County",
        value = county,
        enabled = countyEnabled,
        placeholder = if (countyEnabled) "Tap to choose" else "Select District first",
        onClick = { showCounty = true }
    )
    if (showCounty) {
        PickerDialog(
            title = "Select County",
            feature = picker.countyPicker,
            onPicked = {
                picker.onCountyPicked(it)
                showCounty = false
            },
            onDismiss = { showCounty = false }
        )
    }

    // SubCounty
    val subEnabled = county.isNotBlank()
    PickerField(
        label = "Sub-county",
        value = subCounty,
        enabled = subEnabled,
        placeholder = if (subEnabled) "Tap to choose" else "Select County first",
        onClick = { showSubCounty = true }
    )
    if (showSubCounty) {
        PickerDialog(
            title = "Select Sub-county",
            feature = picker.subCountyPicker,
            onPicked = {
                picker.onSubCountyPicked(it)
                showSubCounty = false
            },
            onDismiss = { showSubCounty = false }
        )
    }

    // Parish
    val parishEnabled = subCounty.isNotBlank()
    PickerField(
        label = "Parish",
        value = parish,
        enabled = parishEnabled,
        placeholder = if (parishEnabled) "Tap to choose" else "Select Sub-county first",
        onClick = { showParish = true }
    )
    if (showParish) {
        PickerDialog(
            title = "Select Parish",
            feature = picker.parishPicker,
            onPicked = {
                picker.onParishPicked(it)
                showParish = false
            },
            onDismiss = { showParish = false }
        )
    }

    // Village
    val villageEnabled = parish.isNotBlank()
    PickerField(
        label = "Village",
        value = village,
        enabled = villageEnabled,
        placeholder = if (villageEnabled) "Tap to choose" else "Select Parish first",
        onClick = { showVillage = true }
    )
    if (showVillage) {
        PickerDialog(
            title = "Select Village",
            feature = picker.villagePicker,
            onPicked = {
                picker.onVillagePicked(it)
                showVillage = false
            },
            onDismiss = { showVillage = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(
    title: String,
    selected: T,
    values: List<T>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (T) -> String = { it.name },
    iconFor: (T) -> ImageVector? = { null }
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = labelFor(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach { v ->
                DropdownMenuItem(
                    text = {
                        Row {
                            iconFor(v)?.let {
                                Icon(it, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                            }
                            Text(labelFor(v))
                        }
                    },
                    onClick = {
                        onSelected(v)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun iconForIndividual(v: Individual): ImageVector = when (v) {
    Individual.UNCLE -> Icons.Outlined.Person
    Individual.AUNTY -> Icons.Outlined.Female
    Individual.CHILD -> Icons.Outlined.Face
    Individual.OTHER -> Icons.Outlined.Group
}
private fun labelForIndividual(v: Individual): String = when (v) {
    Individual.UNCLE -> "Uncle"
    Individual.AUNTY -> "Aunty"
    Individual.CHILD -> "Child"
    Individual.OTHER -> "Other"
}

private fun iconForEducationPreference(v: EducationPreference): ImageVector = when (v) {
    EducationPreference.SCHOOL -> Icons.Outlined.School
    EducationPreference.SKILLING -> Icons.Outlined.Build
    EducationPreference.NONE -> Icons.Outlined.Block
}
private fun labelForEducationPreference(v: EducationPreference): String = when (v) {
    EducationPreference.SCHOOL -> "School"
    EducationPreference.SKILLING -> "Skilling"
    EducationPreference.NONE -> "None"
}

private fun iconForReply(v: Reply): ImageVector = when (v) {
    Reply.YES -> Icons.Outlined.CheckCircle
    Reply.NO -> Icons.Outlined.Cancel
}
private fun labelForReply(v: Reply): String = when (v) {
    Reply.YES -> "Yes"
    Reply.NO -> "No"
}

private fun labelForGender(v: Gender): String = when (v) {
    Gender.MALE -> "Male"
    Gender.FEMALE -> "Female"
}

private fun iconForGender(v: Gender): ImageVector = when (v) {
    Gender.MALE -> Icons.Outlined.Man
    Gender.FEMALE -> Icons.Outlined.Woman
}

private fun labelForResettlementPreference(v: ResettlementPreference): String = when (v) {
    ResettlementPreference.DIRECT_HOME -> "Direct Home"
    ResettlementPreference.TEMPORARY_HOME -> "Temporary Home"
    ResettlementPreference.OTHER -> "Other"
}

private fun iconForResettlementPreference(v: ResettlementPreference): ImageVector = when (v) {
    ResettlementPreference.DIRECT_HOME -> Icons.Outlined.Home
    ResettlementPreference.TEMPORARY_HOME -> Icons.Outlined.Hotel
    ResettlementPreference.OTHER -> Icons.Outlined.OtherHouses
}

private fun iconForRelationship(v: Relationship): ImageVector = when (v) {
    Relationship.NONE -> Icons.Outlined.HorizontalRule
    Relationship.PARENT -> Icons.Outlined.EmojiPeople
    Relationship.UNCLE -> Icons.Outlined.Person
    Relationship.AUNTY -> Icons.Outlined.Woman
    Relationship.OTHER -> Icons.Outlined.Group
}
private fun labelForRelationship(v: Relationship): String = when (v) {
    Relationship.NONE -> "None"
    Relationship.PARENT -> "Parent/Guardian"
    Relationship.UNCLE -> "Uncle"
    Relationship.AUNTY -> "Aunty"
    Relationship.OTHER -> "Other"
}

private fun iconForEducationLevel(v: EducationLevel): ImageVector = when (v) {
    EducationLevel.NONE -> Icons.Outlined.NoAccounts
    EducationLevel.NURSERY -> Icons.Outlined.BabyChangingStation
    EducationLevel.PRIMARY -> Icons.Outlined.ChildCare
    EducationLevel.SECONDARY -> Icons.Outlined.Person
}
private fun labelForEducationLevel(v: EducationLevel): String = when (v) {
    EducationLevel.NONE -> "None"
    EducationLevel.NURSERY -> "Nursery"
    EducationLevel.PRIMARY -> "Primary"
    EducationLevel.SECONDARY -> "Secondary"
}

private fun labelForCountry(v: Country): String = when (v) {
    Country.UGANDA -> "Uganda"
    Country.KENYA -> "Kenya"
    Country.TANZANIA -> "Tanzania"
    Country.RWANDA -> "Rwanda"
    Country.SUDAN -> "Sudan"
    Country.BURUNDI -> "Burundi"
}

private fun labelForChildType(v: ChildType): String = when (v) {
    ChildType.STREET -> "Street"
    ChildType.INMATES -> "Inmates"
    ChildType.FAMILY -> "Family"
    ChildType.HOSPITAL -> "Hospital"
}

private fun iconForChildType(v: ChildType): ImageVector = when (v) {
    ChildType.STREET -> Icons.Outlined.DirectionsWalk
    ChildType.INMATES -> Icons.Outlined.Groups
    ChildType.FAMILY -> Icons.Outlined.Home
    ChildType.HOSPITAL -> Icons.Outlined.LocalHospital
}

private fun labelForProgram(v: Program): String = when (v) {
    Program.CHILDREN_OF_ZION -> "Children of Zion"
    Program.BROTHERS_AND_SISTERS_OF_ZION -> "Brothers and Sisters of Zion"
}

private fun iconForProgram(v: Program): ImageVector = when (v) {
    Program.CHILDREN_OF_ZION -> Icons.Outlined.ChildCare
    Program.BROTHERS_AND_SISTERS_OF_ZION -> Icons.Outlined.Groups
}

private fun labelForClassGroup(v: ClassGroup): String = when (v) {
    ClassGroup.SERGEANT -> "Sergeant: 0-5"
    ClassGroup.LIEUTENANT -> "Lieutenant: 6-9"
    ClassGroup.CAPTAIN -> "Captain: 10-12"
    ClassGroup.GENERAL -> "13-16"
    ClassGroup.BROTHERS_AND_SISTERS_OF_ZION -> "Brothers and Sisters of Zion"
}

private fun iconForClassGroup(v: ClassGroup): ImageVector = when (v) {
    ClassGroup.SERGEANT -> Icons.Outlined.SpatialAudioOff
    ClassGroup.LIEUTENANT -> Icons.Outlined.Mood
    ClassGroup.CAPTAIN -> Icons.Outlined.Badge
    ClassGroup.GENERAL -> Icons.Outlined.ShutterSpeed
    ClassGroup.BROTHERS_AND_SISTERS_OF_ZION -> Icons.Outlined.Groups
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorText: String? = null,
    supportingText: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = errorText != null,
        supportingText = {
            when {
                errorText != null -> Text(errorText, color = MaterialTheme.colorScheme.error)
                supportingText != null -> Text(supportingText)
            }
        },
        textStyle = textStyle,
        minLines = minLines,
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorContainerColor = MaterialTheme.colorScheme.surface,
            errorCursorColor = MaterialTheme.colorScheme.error,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@Composable
private fun AddressBlock(
    title: String,
    country: Country,
    onCountry: (Country) -> Unit,
    region: String, onRegion: (String) -> Unit,
    district: String, onDistrict: (String) -> Unit,
    county: String, onCounty: (String) -> Unit,
    subCounty: String, onSubCounty: (String) -> Unit,
    parish: String, onParish: (String) -> Unit,
    village: String, onVillage: (String) -> Unit
) {
    Text(title, style = MaterialTheme.typography.titleSmall)

    EnumDropdown(
        title = "Country",
        selected = country,
        values = Country.values().toList(),
        onSelected = onCountry,
        labelFor = ::labelForCountry,
        iconFor = { Icons.Outlined.Public }
    )

    AppTextField(region, onRegion, "Region", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
    AppTextField(district, onDistrict, "District", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
    AppTextField(county, onCounty, "County", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
    AppTextField(subCounty, onSubCounty, "Sub-county", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
    AppTextField(parish, onParish, "Parish", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
    AppTextField(village, onVillage, "Village", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
}

private fun formatDate(ts: Timestamp?): String {
    if (ts == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(ts.toDate())
}

