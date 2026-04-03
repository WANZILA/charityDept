package com.example.charityDept.presentation.screens.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
//import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
//import androidx.compose.material3.RowDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role.Companion.Switch
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.EventStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    eventIdArg: String?,
    parentEventIdArg: String?,
    isChildArg: Boolean,
    onFinished: (String) -> Unit,
    navigateUp: () -> Unit,
    vm: EventFormViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
//    val authUi by authVM.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scroll = rememberScrollState()
//    val canAdd = authUi.perms.canCreateEvent

    LaunchedEffect(Unit) {
        vm.events.collectLatest { ev ->
            when (ev) {
                is EventFormViewModel.EventFormEvent.Saved -> {
                    onFinished(ev.id)
                    navigateUp()
                }
                is EventFormViewModel.EventFormEvent.Error -> {
                    snackbarHostState.showSnackbar(ev.msg)
                }
            }
        }
    }

    LaunchedEffect(eventIdArg, parentEventIdArg, isChildArg) {
        when {
            !eventIdArg.isNullOrBlank() -> vm.loadForEdit(eventIdArg)
            isChildArg && !parentEventIdArg.isNullOrBlank() -> vm.seedNewChild(parentEventIdArg)
            else -> vm.ensureNewIdIfNeeded()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when {
                            !eventIdArg.isNullOrBlank() -> "Edit Event"
                            isChildArg -> "Create Child Event"
                            else -> "Create Event"
                        }
                    )
                },
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
                    Button(
                        onClick = { vm.save() },
//                        enabled = !(ui.loading || ui.saving)
                    ) { Text("Save") }
//                    IfRole(ui.roles, anyOf = listOf(Role.ADMIN, Role.LEAD)) {
//                        Button(
//                            onClick = { vm.save() },
//                            enabled = !(ui.loading || ui.saving)
//                        ) { Text("Save") }
//                    }


                    /***
                     * IfRole(
                     *                         roles = ui.roles,
                     *                         anyOf = listOf(Role.ADMIN, Role.LEAD)
                     *                     ) {
                     *                         Button(
                     *                             onClick = { vm.save() },
                     *                             enabled = !(ui.loading || ui.saving)
                     *                         ) { Text("Save") }
                     *                     }
                     */

                }
            }
        }
    ) { inner ->
        Column(
            modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            EventFields(ui = ui, vm = vm)

            ui.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(64.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventFields(
    ui: EventFormUIState,
    vm: EventFormViewModel
) {
    OutlinedTextField(
        value = ui.title ?: "",
        onValueChange = vm::onTitle,
        label = { Text("Title*") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth(),
        isError = ui.titleError != null,   // 👈 mark field as error
        supportingText = {                 // 👈 show error if present
            ui.titleError?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )

    EventDateField(
        ui = ui,
        onDatePicked = vm::onDatePicked
    )

    EnumDropdown(
        title = "Status",
        selected = ui.eventStatus,
        values = EventStatus.values().toList(),
        onSelected = vm::onStatus
    )
    // /// CHANGED: child/parent linkage
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Child Event",
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = ui.isChild,
            onCheckedChange = vm::onIsChild
        )
    }

    val parents by vm.parentCandidates.collectAsStateWithLifecycle()
//    var parentQuery by remember { mutableStateOf("") } // /// CHANGED: dialog search

    var showPickParent by remember { mutableStateOf(false) }
    var parentQuery by remember { mutableStateOf("") } // /// CHANGED: dialog search

    if (ui.isChild) {
        OutlinedTextField(
            value = ui.eventParentId,
            onValueChange = vm::onEventParentId,
            label = { Text("Parent Event ID") },
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = { parentQuery = ""; showPickParent = true }) { Text("Pick") } // /// CHANGED
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }


    if (showPickParent) {
        AlertDialog(
            onDismissRequest = { showPickParent = false },
            title = { Text("Select Parent Event") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = parentQuery,
                        onValueChange = { parentQuery = it },
                        placeholder = { Text("Search parent…") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    val q = parentQuery.trim().lowercase()
                    val filtered = if (q.isBlank()) parents else parents.filter { e ->
                        val label = "${e.title} ${e.eventId} ${e.eventDate.asHumanDate()}".lowercase()
                        label.contains(q)
                    }

                    Column(
                        Modifier
                            .heightIn(max = 340.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                    ) {
                        if (filtered.isEmpty()) {
                            Text(
                                text = "No matches.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            filtered.forEach { e ->
                                TextButton(
                                    onClick = {
                                        vm.onIsChild(true)
                                        vm.onEventParentId(e.eventId)
                                        showPickParent = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${e.title} • ${e.eventDate.asHumanDate()}")
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPickParent = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = { showPickParent = false }) { Text("Close") }
            }
        )
    }

    OutlinedTextField(
        value = ui.location ?: "",
        onValueChange = vm::onLocation,
        label = { Text("Location") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth(),
        isError = ui.locationError != null,   // 👈 mark field as error
        supportingText = {                 // 👈 show error if present
            ui.locationError?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )

    OutlinedTextField(
        value = ui.teamName ?: "",
        onValueChange = vm::onTeamName,
        label = { Text("Team Name") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth(),
        isError = ui.teamNameError != null,   // 👈 mark field as error
        supportingText = {                 // 👈 show error if present
            ui.teamNameError?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )

    OutlinedTextField(
        value = ui.teamLeaderNames ?: "",
        onValueChange = vm::onTeamLeaderNames,
        label = { Text("Team Leader Names*") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth(),
//        isError = ui.teamLeaderNamesError != null,   // 👈 mark field as error
//        supportingText = {                 // 👈 show error if present
//            ui.teamLeaderNamesError?.let { err ->
//                Text(
//                    text = err,
//                    color = MaterialTheme.colorScheme.error,
//                    style = MaterialTheme.typography.bodySmall
//                )
//            }
//        }
    )

    OutlinedTextField(
        value = ui.leaderTelephone1 ?: "",
        onValueChange = vm::onLeaderTelephone1,
        label = { Text("Telephone 1") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = ui.leaderTelephone2 ?: "",
        onValueChange = vm::onLeaderTelephone2,
        label = { Text("Telephone 2") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = ui.leaderEmail ?: "",
        onValueChange = vm::onLeaderEmail,
        label = { Text("Leader Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth()
    )



    OutlinedTextField(
        value = ui.notes ?: "",
        onValueChange = vm::onNotes,
        label = { Text("Notes") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDateField(ui: EventFormUIState, onDatePicked: (Timestamp) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = ui.eventDate.toDate().time // Timestamp → millis
    )

    OutlinedTextField(
        value = ui.eventDate.asHumanDate(),
        onValueChange = { /* read-only */ },
        readOnly = true,
        label = { Text("Event Date") },
        trailingIcon = { TextButton(onClick = { showDatePicker = true }) { Text("Pick") } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        onDatePicked(Timestamp(Date(millis))) // millis → Timestamp ✅
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(
    title: String,
    selected: T,
    values: List<T>,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v.name) },
                    onClick = { onSelected(v); expanded = false }
                )
            }
        }
    }
}

/* ------------ Small formatting helper (no API 26 requirement) ------------ */
private fun Timestamp.asHumanDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(this.toDate())
}

