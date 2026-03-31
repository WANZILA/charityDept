package com.example.charityDept.presentation.screens.children.assessments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.menuAnchor
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
//import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildAssessmentHistoryScreen(
    childId: String,
    mode: String,
    navigateUp: () -> Unit,
    onOpenSession: (generalId: String, assessmentKey: String) -> Unit,
    onStartNew: (generalId: String, assessmentKey: String) -> Unit,
    vm: ChildAssessmentHistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val isObservationMode = mode == "OBS"

    val screenLabel = when (mode) {
        "QA" -> "Q&A Assessments"
        "OBS" -> "Observations"
        else -> "Assessments"
    }

    val dialogTitle = if (isObservationMode) {
        "Choose Observation Tool"
    } else {
        "Choose Q&A Tool"
    }

    val dropdownLabel = if (isObservationMode) {
        "Observation Tool"
    } else {
        "Q&A Tool"
    }

    val emptyToolsText = if (isObservationMode) {
        "No active observation tools found yet."
    } else {
        "No active Q&A tools found yet."
    }

    val emptyHistoryText = if (isObservationMode) {
        "No observation sessions yet. Tap + to start one."
    } else {
        "No Q&A sessions yet. Tap + to start one."
    }

    val startButtonLabel = if (isObservationMode) "Start Observation" else "Start Q&A"

    val sessionsFlow = remember(childId, mode) { vm.sessions(childId, mode) }
    val ui by sessionsFlow.collectAsStateWithLifecycle()

    val toolsFlow = remember(mode) { vm.tools(mode) }
    val tools by toolsFlow.collectAsStateWithLifecycle()

    var showStartDialog by remember { mutableStateOf(false) }
    var selectedAssessmentKey by remember { mutableStateOf("") }
    var selectedAssessmentLabel by remember { mutableStateOf("") }

    LaunchedEffect(showStartDialog, tools) {
        if (showStartDialog && selectedAssessmentKey.isBlank() && tools.isNotEmpty()) {
            selectedAssessmentKey = tools.first().assessmentKey
            selectedAssessmentLabel = tools.first().assessmentLabel
        }
    }

    if (showStartDialog) {
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text(dialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (tools.isEmpty()) {
                        Text(emptyToolsText)
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedAssessmentLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(dropdownLabel) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                tools.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.assessmentLabel) },
                                        onClick = {
                                            selectedAssessmentKey = option.assessmentKey
                                            selectedAssessmentLabel = option.assessmentLabel
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedAssessmentKey.isNotBlank() && tools.isNotEmpty(),
                    onClick = {
                        showStartDialog = false
                        vm.startNewAssessment(childId, mode) { newId ->
                            onStartNew(newId, selectedAssessmentKey)
                        }
                    }
                ) { Text(startButtonLabel) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenLabel) },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedAssessmentKey = ""
                            selectedAssessmentLabel = ""
                            showStartDialog = true
                        }
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Start New")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (ui.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            if (ui.sessions.isEmpty()) {
                Text(emptyHistoryText)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(
                        items = ui.sessions,
                        key = { it.generalId }
                    ) { row ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenSession(row.generalId, row.assessmentKey)
                                }
                        ) {
                            val sessionTypeLabel = when (mode) {
                                "QA" -> "Questions Session"
                                "OBS" -> "Observation Session"
                                else -> "Assessment Session"
                            }

                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    row.assessmentLabel,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))

                                Text(
                                    sessionTypeLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.height(6.dp))

                                Text(
                                    "Preview: ${row.questionSnapshot}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "Items: ${row.itemCount}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}