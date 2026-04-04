package com.example.charityDept.presentation.screens.families.assessments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.AssessmentAnswer
import kotlinx.coroutines.launch

private data class Draft(
    val answer: String,
    val recommendation: String,
    val notes: String,
    val score: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyAssessmentDetailScreen(
    familyId: String,
    generalId: String,
    mode: String,
    assessmentKey: String,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    vm: FamilyAssessmentDetailViewModel = hiltViewModel()
) {
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isObservationMode = mode == "OBS"

    val screenTitle = when (mode) {
        "QA" -> "Family Q&A Session"
        "OBS" -> "Family Observation Session"
        else -> "Family Assessment Session"
    }

    val primaryInputLabel = if (isObservationMode) "Observation" else "Answer"
    val recommendationLabel = if (isObservationMode) "Action / Follow-up" else "Recommendation"
    val notesLabel = if (isObservationMode) "Observation Notes" else "Notes"
    val emptyStateText = if (isObservationMode) {
        "No family observation items found for this session yet."
    } else {
        "No family question items found for this session yet."
    }

    val sessionFlow = remember(familyId, generalId, mode, assessmentKey) {
        vm.session(familyId, generalId, mode, assessmentKey)
    }
    val ui by sessionFlow.collectAsStateWithLifecycle()

    val drafts = remember(familyId, generalId, mode) { mutableStateMapOf<String, Draft>() }

    var showDeleteSessionConfirm by remember { mutableStateOf(false) }
    var sessionRecommendation by remember(familyId, generalId, mode) { mutableStateOf("") }

    LaunchedEffect(ui.items) {
        val incomingIds = ui.items.asSequence().map { it.answerId }.toHashSet()

        drafts.keys.toList().forEach { id ->
            if (id !in incomingIds) drafts.remove(id)
        }

        ui.items.forEach { a ->
            drafts.putIfAbsent(
                a.answerId,
                Draft(
                    answer = a.answer,
                    recommendation = a.recommendation,
                    notes = a.notes,
                    score = a.score
                )
            )
        }

        if (sessionRecommendation.isBlank()) {
            sessionRecommendation = ui.items
                .firstOrNull { it.recommendation.isNotBlank() }
                ?.recommendation
                .orEmpty()
        }
    }

    fun buildSaveList(): List<AssessmentAnswer> {
        return ui.items.map { base ->
            val d = drafts[base.answerId]
            if (d == null) {
                if (isObservationMode) base.copy(score = 0) else base
            } else {
                base.copy(
                    answer = d.answer,
                    recommendation = sessionRecommendation,
                    notes = d.notes,
                    score = if (isObservationMode) 0 else d.score
                )
            }
        }
    }

    if (showDeleteSessionConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSessionConfirm = false },
            title = {
                Text(
                    if (isObservationMode) "Delete whole family observation session?"
                    else "Delete whole family Q&A session?"
                )
            },
            text = {
                Text("This will soft-delete all items in this session.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSessionConfirm = false
                        vm.softDeleteSession(ui.items.map { it.answerId })
                        scope.launch {
                            snack.showSnackbar(
                                if (isObservationMode) "Family observation session deleted (soft)."
                                else "Family Q&A session deleted (soft)."
                            )
                        }
                        navigateUp()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSessionConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteSessionConfirm = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete Session")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val toSave = buildSaveList()
                    vm.saveAllLocally(toSave)
                    scope.launch {
                        snack.showSnackbar("Saved locally (Room).")
                    }
                    navigateUp()
                }
            ) {
                Icon(Icons.Outlined.Save, contentDescription = "Save")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            if (ui.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            if (ui.items.isEmpty()) {
                Text(
                    emptyStateText,
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = ui.items,
                    key = { _, item -> item.answerId }
                ) { _, item ->
                    val draft = drafts[item.answerId]
                        ?: Draft(
                            answer = item.answer,
                            recommendation = item.recommendation,
                            notes = item.notes,
                            score = item.score
                        )

                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.assessmentLabel.isNotBlank()) {
                                Text(
                                    item.assessmentLabel,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Text(
                                item.questionSnapshot.orEmpty().ifBlank { "Question" },
                                style = MaterialTheme.typography.titleMedium
                            )

                            OutlinedTextField(
                                value = draft.answer,
                                onValueChange = {
                                    drafts[item.answerId] = draft.copy(answer = it)
                                },
                                label = { Text(primaryInputLabel) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!isObservationMode) {
                                Text(
                                    text = "Score: ${draft.score}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Slider(
                                    value = draft.score.toFloat(),
                                    onValueChange = {
                                        drafts[item.answerId] = draft.copy(score = it.toInt())
                                    },
                                    valueRange = 0f..10f,
                                    steps = 9
                                )
                            }

                            OutlinedTextField(
                                value = sessionRecommendation,
                                onValueChange = { sessionRecommendation = it },
                                label = { Text(recommendationLabel) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = draft.notes,
                                onValueChange = {
                                    drafts[item.answerId] = draft.copy(notes = it)
                                },
                                label = { Text(notesLabel) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}