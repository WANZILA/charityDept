package com.example.charityDept.presentation.screens.children.assessments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.core.utils.DatesUtils.asHuman
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildAssessmentHistoryScreen(
    childId: String,
    mode: String,
    navigateUp: () -> Unit,
    onOpenSession: (generalId: String) -> Unit,
    onStartNew: (generalId: String) -> Unit,
    vm: ChildAssessmentHistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {

    val label = when (mode) {
        "QA" -> "Q&A Assessments"
        "OBS" -> "Observations"
        else -> "Assessments"
    }

    // ✅ IMPORTANT: remember the flow so recomposition doesn’t recreate it
    // /// CHANGED
    val sessionsFlow = remember(childId, mode) { vm.sessions(childId, mode) }
    val ui by sessionsFlow.collectAsStateWithLifecycle()

//    val sessionsFlow = remember(childId) { vm.sessions(childId) }
//    val ui by sessionsFlow.collectAsStateWithLifecycle()

//    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(label) },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            vm.startNewAssessment(childId, mode) { newId ->
                                onStartNew(newId)
                            }
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
//            Text(text = "Child: $childId", style = MaterialTheme.typography.labelLarge)
//            Spacer(Modifier.height(12.dp))

            if (ui.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            if (ui.sessions.isEmpty()) {
                Text("No assessments yet. Tap + to start one.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ✅ add stable key
                    items(
                        items = ui.sessions,
                        key = { it.generalId }
                    ) { row ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSession(row.generalId) }
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

                                val lastUpdatedMillis = row.lastUpdated
                                Text(
                                    text = "Updated: ${lastUpdatedMillis.asHuman()}",
                                    style = MaterialTheme.typography.labelSmall
                                )

                                Text(
                                    "By: ${row.enteredByUid ?: "Unknown"}",
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





