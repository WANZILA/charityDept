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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                value = ui.fname,
                onValueChange = vm::onfName,
                label = { Text("Name*") },
                modifier = Modifier.fillMaxWidth(),
                isError = ui.nameError != null,
                supportingText = {
                    ui.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            OutlinedTextField(
                value = ui.lname,
                onValueChange = vm::onlName,
                label = { Text("Name*") },
                modifier = Modifier.fillMaxWidth(),
                isError = ui.nameError != null,
                supportingText = {
                    ui.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            OutlinedTextField(
                value = ui.age,
                onValueChange = vm::onAge,
                label = { Text("Age") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.gender,
                onValueChange = vm::onGender,
                label = { Text("Gender") },
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.personalPhone2,
                onValueChange = vm::onPersonalPhone2,
                label = { Text("Phone 2") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.ninNumber,
                onValueChange = vm::onNinNumber,
                label = { Text("NIN Number") },
                modifier = Modifier.fillMaxWidth()
            )

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(72.dp))
        }
    }
}