package com.example.charityDept.presentation.screens.admin.questions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.AssessmentQuestion

private data class QuestionAssessmentRow(
    val assessmentKey: String,
    val assessmentLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankScreen(
    navigateUp: () -> Unit,
    onAdd: (initialAssessmentKey: String?, initialAssessmentLabel: String?) -> Unit,
    onEdit: (questionId: String) -> Unit,
    vm: AssessmentQuestionAdminViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var selectedAssessmentKey by remember { mutableStateOf<String?>(null) }

    val assessmentRows =
        ui.items
            .groupBy { it.assessmentKey.trim() }
            .mapNotNull { (assessmentKey, rows) ->
                if (assessmentKey.isBlank()) {
                    null
                } else {
                    QuestionAssessmentRow(
                        assessmentKey = assessmentKey,
                        assessmentLabel = rows.firstOrNull { it.assessmentLabel.isNotBlank() }
                            ?.assessmentLabel
                            ?.trim()
                            .orEmpty()
                            .ifBlank { assessmentKey }
                    )
                }
            }
            .sortedBy { it.assessmentLabel.lowercase() }

    val selectedItems =
        ui.items
            .filter { it.assessmentKey == selectedAssessmentKey }
            .sortedWith(
                compareBy<AssessmentQuestion>(
                    {
                        when (it.categoryKey.trim().uppercase()) {
                            "QA" -> 0
                            "OBS" -> 1
                            else -> 2
                        }
                    },
                    { it.indexNum },
                    { it.subCategory.lowercase() },
                    { it.question.lowercase() }
                )
            )

    val selectedAssessmentLabel =
        assessmentRows.firstOrNull { it.assessmentKey == selectedAssessmentKey }
            ?.assessmentLabel
            ?: selectedAssessmentKey.orEmpty()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (selectedAssessmentKey == null) {
                            "Question Bank"
                        } else {
                            selectedAssessmentLabel
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedAssessmentKey != null) {
                                selectedAssessmentKey = null
                            } else {
                                navigateUp()
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val selectedLabel =
                                assessmentRows.firstOrNull { it.assessmentKey == selectedAssessmentKey }
                                    ?.assessmentLabel
                            onAdd(selectedAssessmentKey, selectedLabel)
                        }
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
        ) {
            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            if (ui.items.isEmpty()) {
                Text("No questions yet. Tap + to add one.")
                return@Column
            }

            if (selectedAssessmentKey == null) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(assessmentRows) { item ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAssessmentKey = item.assessmentKey }
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    item.assessmentLabel,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    item.assessmentKey,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            } else {
                if (selectedItems.isEmpty()) {
                    Text("No questions found for this assessment.")
                    return@Column
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(selectedItems) { q ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEdit(q.questionId) }
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    q.category.ifBlank { q.categoryKey },
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    q.subCategory.ifBlank { q.subCategoryKey },
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(q.question, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Order ${q.indexNum} • ${if (q.isActive) "Active" else "Inactive"}",
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