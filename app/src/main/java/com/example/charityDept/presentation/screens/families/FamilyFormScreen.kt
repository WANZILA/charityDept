package com.example.charityDept.presentation.screens.families

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
//import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.charityDept.data.model.Country
import com.example.charityDept.presentation.screens.widgets.AppTextField
import com.example.charityDept.presentation.screens.widgets.PickerDialog
import androidx.compose.material.icons.outlined.Man
import androidx.compose.material.icons.outlined.Woman
//import androidx.compose.ui.graphics.vector.ImageVector
import com.example.charityDept.data.model.Gender


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyFormScreen(
    familyIdArg: String?,
    onFinished: (String) -> Unit,
    navigateUp: () -> Unit,
    vm: FamilyFormViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) {
        vm.events.collectLatest { ev ->
            when (ev) {
                is FamilyFormViewModel.FamilyFormEvent.Saved -> {
                    onFinished(ev.id)
                    navigateUp()
                }
                is FamilyFormViewModel.FamilyFormEvent.Error -> {
                    snackbarHostState.showSnackbar(ev.msg)
                }
            }
        }
    }

    LaunchedEffect(familyIdArg) {
        if (familyIdArg.isNullOrBlank()) vm.ensureNewIdIfNeeded() else vm.loadForEdit(familyIdArg)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (familyIdArg.isNullOrBlank()) "Create Family" else "Edit Family") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { vm.save() }) {
                        Text("Save")
                    }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ui.loading || ui.saving) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            OutlinedTextField(
                value = ui.caseReferenceNumber,
                onValueChange = vm::onCaseReferenceNumber,
                label = { Text("Case Reference Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),

            )

            OutlinedTextField(
                value = ui.fName,
                onValueChange = vm::onFName,
                label = { Text("First Name*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
//                isError = ui.headError != null,
                supportingText = {
                    ui.headError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            OutlinedTextField(
                value = ui.lName,
                onValueChange = vm::onLName,
                label = { Text("Last Name*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
//                isError = ui.headError != null,
                supportingText = {
                    ui.headError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            )


            EnumDropdown(
                title = "Gender",
                selected = ui.gender,
                values = Gender.values().toList(),
                onSelected = vm::onGender,
                labelFor = ::labelForGender,
                iconFor = ::iconForGender
            )


            OutlinedTextField(
                value = ui.occupationOrSchoolGrade,
                onValueChange = vm::onOccupationOrSchoolGrade,
                label = { Text("Occupation / SchoolGrade") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = ui.headError != null,
                supportingText = {
                    ui.headError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Born Again", modifier = Modifier.weight(1f))
                Switch(
                    checked = ui.isBornAgain,
                    onCheckedChange = vm::onIsBornAgain
                )
            }

            OutlinedTextField(
                value = ui.personalPhone1,
                onValueChange = vm::onPersonalPhone1,
                label = { Text("Phone 1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = ui.personalPhone2,
                onValueChange = vm::onPersonalPhone2,
                label = { Text("Phone 2") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = ui.ninNumber,
                onValueChange = vm::onNinNumber,
                label = { Text("NIN Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Ancestral Location", style = MaterialTheme.typography.titleMedium)

            val ancestralCountry = ui.memberAncestralCountry.toCountryOrDefault()
            if (ancestralCountry == Country.UGANDA) {
                FamilyUgandaAddressBlock(
                    title = "Ancestral Location",
                    country = ancestralCountry,
                    onCountry = vm::onMemberAncestralCountryChanged,
                    picker = vm.ugAncestralPicker,
                    region = ui.memberAncestralRegion,
                    district = ui.memberAncestralDistrict,
                    county = ui.memberAncestralCounty,
                    subCounty = ui.memberAncestralSubCounty,
                    parish = ui.memberAncestralParish,
                    village = ui.memberAncestralVillage
                )
            } else {
                FamilyAddressBlock(
                    title = "Ancestral Location",
                    country = ancestralCountry,
                    onCountry = vm::onMemberAncestralCountryChanged,
                    region = ui.memberAncestralRegion,
                    onRegion = vm::onMemberAncestralRegion,
                    district = ui.memberAncestralDistrict,
                    onDistrict = vm::onMemberAncestralDistrict,
                    county = ui.memberAncestralCounty,
                    onCounty = vm::onMemberAncestralCounty,
                    subCounty = ui.memberAncestralSubCounty,
                    onSubCounty = vm::onMemberAncestralSubCounty,
                    parish = ui.memberAncestralParish,
                    onParish = vm::onMemberAncestralParish,
                    village = ui.memberAncestralVillage,
                    onVillage = vm::onMemberAncestralVillage
                )
            }

            Text("Rental Location", style = MaterialTheme.typography.titleMedium)

            val rentalCountry = ui.memberRentalCountry.toCountryOrDefault()
            if (rentalCountry == Country.UGANDA) {
                FamilyUgandaAddressBlock(
                    title = "Rental Location",
                    country = rentalCountry,
                    onCountry = vm::onMemberRentalCountryChanged,
                    picker = vm.ugRentalPicker,
                    region = ui.memberRentalRegion,
                    district = ui.memberRentalDistrict,
                    county = ui.memberRentalCounty,
                    subCounty = ui.memberRentalSubCounty,
                    parish = ui.memberRentalParish,
                    village = ui.memberRentalVillage
                )
            } else {
                FamilyAddressBlock(
                    title = "Rental Location",
                    country = rentalCountry,
                    onCountry = vm::onMemberRentalCountryChanged,
                    region = ui.memberRentalRegion,
                    onRegion = vm::onMemberRentalRegion,
                    district = ui.memberRentalDistrict,
                    onDistrict = vm::onMemberRentalDistrict,
                    county = ui.memberRentalCounty,
                    onCounty = vm::onMemberRentalCounty,
                    subCounty = ui.memberRentalSubCounty,
                    onSubCounty = vm::onMemberRentalSubCounty,
                    parish = ui.memberRentalParish,
                    onParish = vm::onMemberRentalParish,
                    village = ui.memberRentalVillage,
                    onVillage = vm::onMemberRentalVillage
                )
            }
            ui.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(72.dp))
        }
    }
}

private fun String.toCountryOrDefault(): Country {
    val raw = trim()
    return Country.values().firstOrNull {
        it.name.equals(raw, ignoreCase = true) || labelForCountry(it).equals(raw, ignoreCase = true)
    } ?: Country.UGANDA
}

private fun labelForCountry(v: Country): String = when (v) {
    Country.UGANDA -> "Uganda"
    Country.KENYA -> "Kenya"
    Country.TANZANIA -> "Tanzania"
    Country.RWANDA -> "Rwanda"
    Country.SUDAN -> "Sudan"
    Country.BURUNDI -> "Burundi"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryDropdown(
    title: String,
    selected: Country,
    onSelected: (Country) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = labelForCountry(selected),
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
            Country.values().forEach { country ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Public, contentDescription = null)
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Text(labelForCountry(country))
                        }
                    },
                    onClick = {
                        onSelected(country)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FamilyPickerField(
    label: String,
    value: String,
    placeholder: String = "Tap to choose",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val display = value.trim().ifBlank { placeholder }

    OutlinedTextField(
        value = display,
        onValueChange = {},
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
private fun FamilyAddressBlock(
    title: String,
    country: Country,
    onCountry: (Country) -> Unit,
    region: String,
    onRegion: (String) -> Unit,
    district: String,
    onDistrict: (String) -> Unit,
    county: String,
    onCounty: (String) -> Unit,
    subCounty: String,
    onSubCounty: (String) -> Unit,
    parish: String,
    onParish: (String) -> Unit,
    village: String,
    onVillage: (String) -> Unit
) {
    Text(title, style = MaterialTheme.typography.titleSmall)

    CountryDropdown(
        title = "Country",
        selected = country,
        onSelected = onCountry
    )

    AppTextField(region, onRegion, "Region", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
    AppTextField(district, onDistrict, "District", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
    AppTextField(county, onCounty, "County", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
    AppTextField(subCounty, onSubCounty, "Sub-county", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
    AppTextField(parish, onParish, "Parish", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
    AppTextField(village, onVillage, "Village", Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
}

@Composable
private fun FamilyUgandaAddressBlock(
    title: String,
    country: Country,
    onCountry: (Country) -> Unit,
    picker: FamilyFormViewModel.UgandaAddressPicker,
    region: String,
    district: String,
    county: String,
    subCounty: String,
    parish: String,
    village: String
) {
    var showRegion by remember { mutableStateOf(false) }
    var showDistrict by remember { mutableStateOf(false) }
    var showCounty by remember { mutableStateOf(false) }
    var showSubCounty by remember { mutableStateOf(false) }
    var showParish by remember { mutableStateOf(false) }
    var showVillage by remember { mutableStateOf(false) }
    var showDistrictSearch by remember { mutableStateOf(false) }
    var showVillageSearch by remember { mutableStateOf(false) }

    Text(title, style = MaterialTheme.typography.titleSmall)

    CountryDropdown(
        title = "Country",
        selected = country,
        onSelected = onCountry
    )

    FamilyPickerField(
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

    FamilyPickerField(
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

    FamilyPickerField(
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

    val districtEnabled = region.isNotBlank()
    FamilyPickerField(
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

    val countyEnabled = district.isNotBlank()
    FamilyPickerField(
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

    val subCountyEnabled = county.isNotBlank()
    FamilyPickerField(
        label = "Sub-county",
        value = subCounty,
        enabled = subCountyEnabled,
        placeholder = if (subCountyEnabled) "Tap to choose" else "Select County first",
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

    val parishEnabled = subCounty.isNotBlank()
    FamilyPickerField(
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

    val villageEnabled = parish.isNotBlank()
    FamilyPickerField(
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

private fun labelForGender(v: Gender): String = when (v) {
    Gender.MALE -> "Male"
    Gender.FEMALE -> "Female"
}

private fun iconForGender(v: Gender): ImageVector = when (v) {
    Gender.MALE -> Icons.Outlined.Man
    Gender.FEMALE -> Icons.Outlined.Woman
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(
    title: String,
    selected: T,
    values: List<T>,
    onSelected: (T) -> Unit,
    labelFor: (T) -> String = { it.name },
    iconFor: ((T) -> ImageVector)? = null
) {
    var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = labelFor(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            iconFor?.let {
                                Icon(it(item), contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(labelFor(item))
                        }
                    },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}