package com.example.charityDept.presentation.screens.children.assessments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun ChildAssessmentDetailScreen(
    childId: String,
    generalId: String,
    mode: String,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    vm: ChildAssessmentDetailViewModel = hiltViewModel()
) {
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val label = when (mode) {
        "QA" -> "Q&A Session"
        "OBS" -> "Observations Session"
        else -> "Assessment Session"
    }

    val sessionFlow = remember(childId, generalId, mode) {
        vm.session(childId, generalId, mode)
    }
    val ui by sessionFlow.collectAsStateWithLifecycle()

    val drafts = remember(childId, generalId, mode) { mutableStateMapOf<String, Draft>() }

    var deleteTarget by remember { mutableStateOf<AssessmentAnswer?>(null) }

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
    }

    fun buildSaveList(): List<AssessmentAnswer> {
        return ui.items.map { base ->
            val d = drafts[base.answerId]
            if (d == null) base
            else base.copy(
                answer = d.answer,
                recommendation = d.recommendation,
                notes = d.notes,
                score = d.score
            )
        }
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete item?") },
            text = { Text("This will remove it from this session (soft delete).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = deleteTarget
                        deleteTarget = null
                        if (target != null) {
                            vm.softDeleteAnswer(target.answerId)
                            scope.launch { snack.showSnackbar("Deleted (soft).") }
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(label) },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val toSave = buildSaveList()
                    vm.saveAllLocally(toSave)
                    scope.launch { snack.showSnackbar("Saved locally (Room).") }
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
                    "No assessment items found for this session yet.\n\n",
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
                        Column(Modifier.padding(14.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (item.assessmentLabel.isNotBlank()) {
                                        Text(
                                            item.assessmentLabel,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }

                                    Text(
                                        "${item.category} • ${item.subCategory}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                IconButton(onClick = { deleteTarget = item }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            val questionText = item.questionSnapshot ?: "(no question text)"
                            Text(questionText, style = MaterialTheme.typography.titleMedium)

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = draft.answer,
                                onValueChange = { v ->
                                    drafts[item.answerId] = draft.copy(answer = v)
                                },
                                label = { Text("Answer / Observation") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = draft.recommendation,
                                onValueChange = { v ->
                                    drafts[item.answerId] = draft.copy(recommendation = v)
                                },
                                label = { Text("Recommendation") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )

                            Spacer(Modifier.height(10.dp))

                            val scoreFloat = draft.score.toFloat().coerceIn(0f, 10f)
                            Text("Score: ${draft.score}", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = scoreFloat,
                                onValueChange = { f ->
                                    drafts[item.answerId] = draft.copy(score = f.toInt().coerceIn(0, 10))
                                },
                                valueRange = 0f..10f,
                                steps = 9
                            )

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = draft.notes,
                                onValueChange = { v ->
                                    drafts[item.answerId] = draft.copy(notes = v)
                                },
                                label = { Text("Notes") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}