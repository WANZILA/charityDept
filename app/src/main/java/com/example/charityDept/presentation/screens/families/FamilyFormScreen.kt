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
                singleLine = true
            )

            OutlinedTextField(
                value = ui.primaryContactHeadOfHousehold,
                onValueChange = vm::onPrimaryContactHeadOfHousehold,
                label = { Text("Head of Household*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = ui.headError != null,
                supportingText = {
                    ui.headError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            OutlinedTextField(
                value = ui.addressLocation,
                onValueChange = vm::onAddressLocation,
                label = { Text("Address / Location") },
                modifier = Modifier.fillMaxWidth()
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

            OutlinedTextField(ui.memberAncestralCountry, vm::onMemberAncestralCountry, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberAncestralRegion, vm::onMemberAncestralRegion, label = { Text("Region") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberAncestralDistrict, vm::onMemberAncestralDistrict, label = { Text("District") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberAncestralCounty, vm::onMemberAncestralCounty, label = { Text("County") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberAncestralSubCounty, vm::onMemberAncestralSubCounty, label = { Text("Subcounty") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberAncestralParish, vm::onMemberAncestralParish, label = { Text("Parish") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberAncestralVillage, vm::onMemberAncestralVillage, label = { Text("Village") }, modifier = Modifier.fillMaxWidth())

            Text("Rental Location", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(ui.memberRentalCountry, vm::onMemberRentalCountry, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberRentalRegion, vm::onMemberRentalRegion, label = { Text("Region") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberRentalDistrict, vm::onMemberRentalDistrict, label = { Text("District") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberRentalCounty, vm::onMemberRentalCounty, label = { Text("County") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberRentalSubCounty, vm::onMemberRentalSubCounty, label = { Text("Subcounty") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberRentalParish, vm::onMemberRentalParish, label = { Text("Parish") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.memberRentalVillage, vm::onMemberRentalVillage, label = { Text("Village") }, modifier = Modifier.fillMaxWidth())

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