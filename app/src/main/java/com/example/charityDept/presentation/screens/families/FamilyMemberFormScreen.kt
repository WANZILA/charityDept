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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Man
import androidx.compose.material.icons.outlined.Woman
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.Gender
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMemberFormScreen(
    familyIdArg: String,
    familyMemberIdArg: String?,
    onFinished: () -> Unit,
    navigateUp: () -> Unit,
    vm: FamilyMemberFormViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) {
        vm.events.collectLatest { ev ->
            when (ev) {
                is FamilyMemberFormViewModel.Event.Saved -> onFinished()
                is FamilyMemberFormViewModel.Event.Error -> snackbarHostState.showSnackbar(ev.msg)
            }
        }
    }

    LaunchedEffect(familyIdArg, familyMemberIdArg) {
        if (familyMemberIdArg.isNullOrBlank()) {
            vm.ensureNewIdIfNeeded(familyIdArg)
        } else {
            vm.loadForEdit(familyIdArg, familyMemberIdArg)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (familyMemberIdArg.isNullOrBlank()) "Add Family Member" else "Edit Family Member") },
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
                value = ui.fName,
                onValueChange = vm::onfName,
                label = { Text(" First Name*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                isError = ui.nameError != null,
                supportingText = {
                    ui.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            OutlinedTextField(
                value = ui.lName,
                onValueChange = vm::onlName,
                label = { Text("Last Name*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                isError = ui.nameError != null,
                supportingText = {
                    ui.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            OutlinedTextField(
                value = ui.age,
                onValueChange = vm::onAge,
                label = { Text("Age") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
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
                value = ui.relationship,
                onValueChange = vm::onRelationship,
                label = { Text("Relationship") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.occupationOrSchoolGrade,
                onValueChange = vm::onOccupationOrSchoolGrade,
                label = { Text("Occupation / School Grade") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.healthOrDisabilityStatus,
                onValueChange = vm::onHealthOrDisabilityStatus,
                label = { Text("Health / Disability Status") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.personalPhone1,
                onValueChange = vm::onPersonalPhone1,
                label = { Text("Phone 1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
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
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = ui.ninNumber,
                onValueChange = vm::onNinNumber,
                label = { Text("NIN Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(72.dp))
        }
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