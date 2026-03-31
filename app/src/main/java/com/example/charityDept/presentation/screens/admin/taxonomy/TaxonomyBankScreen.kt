package com.example.charityDept.presentation.screens.admin.taxonomy

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
import com.example.charityDept.data.model.AssessmentTaxonomy
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.navigation.NavController


private data class AssessmentBankRow(
    val assessmentKey: String,
    val assessmentLabel: String
)
private fun sanitizeRouteValue(value: String?): String? {
    val cleaned = value?.trim().orEmpty()
    if (cleaned.isBlank()) return null
    if (cleaned.contains("{") || cleaned.contains("}")) return null
    return cleaned
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxonomyBankScreen(
    initialSelectedAssessmentKey: String? = null,
    initialSelectedAssessmentLabel: String? = null,
    navigateUp: () -> Unit,
    onAddClick: (String?, String?) -> Unit,
    onEditClick: (String) -> Unit,
    vm: AssessmentTaxonomyAdminViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    var selectedAssessmentKey by remember {
        mutableStateOf(sanitizeRouteValue(initialSelectedAssessmentKey))
    }
    var selectedAssessmentLabel by remember {
        mutableStateOf(sanitizeRouteValue(initialSelectedAssessmentLabel))
    }

    var editAssessment by remember { mutableStateOf<AssessmentBankRow?>(null) }
    var deleteAssessment by remember { mutableStateOf<AssessmentBankRow?>(null) }
    var editAssessmentLabel by remember { mutableStateOf("") }

    val assessmentRows =
        ui.items
            .groupBy { it.assessmentKey.trim() }
            .mapNotNull { (assessmentKey, rows) ->
                if (assessmentKey.isBlank()) {
                    null
                } else {
                    AssessmentBankRow(
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
                compareBy<AssessmentTaxonomy>(
                    {
                        when (it.categoryKey.trim().uppercase()) {
                            "QA" -> 0
                            "OBS" -> 1
                            else -> 2
                        }
                    },
                    { it.indexNum },
                    { it.subCategoryLabel.lowercase() },
                    { it.subCategoryKey.lowercase() }
                )
            )

    val effectiveSelectedAssessmentLabel =
        assessmentRows.firstOrNull { it.assessmentKey == selectedAssessmentKey }
            ?.assessmentLabel
            ?: selectedAssessmentLabel

//    val selectedAssessmentLabel =
//        assessmentRows.firstOrNull { it.assessmentKey == selectedAssessmentKey }
//            ?.assessmentLabel
//            ?: selectedAssessmentKey.orEmpty()

    if (editAssessment != null) {
        AlertDialog(
            onDismissRequest = { editAssessment = null },
            title = { Text("Edit Assessment Label") },
            text = {
                OutlinedTextField(
                    value = editAssessmentLabel,
                    onValueChange = { editAssessmentLabel = it.replace("+", "").uppercase() },
                    label = { Text("Assessment Label") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val item = editAssessment ?: return@TextButton
                        val nextLabel = editAssessmentLabel.trim().replace("+", "").uppercase()
                        if (nextLabel.isNotBlank()) {
                            vm.renameAssessmentLabel(item.assessmentKey, nextLabel)
                        }
                        editAssessment = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editAssessment = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deleteAssessment != null) {
        AlertDialog(
            onDismissRequest = { deleteAssessment = null },
            title = { Text("Delete Assessment?") },
            text = {
                Text("This will soft-delete all taxonomy rows and questions under this assessment.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val item = deleteAssessment ?: return@TextButton
                        vm.deleteAssessment(item.assessmentKey)
                        if (selectedAssessmentKey == item.assessmentKey) {
                            selectedAssessmentKey = null
                        }
                        deleteAssessment = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAssessment = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (selectedAssessmentKey == null) {
                            "Taxonomy Bank"
                        } else {
                            effectiveSelectedAssessmentLabel ?: "Taxonomy Bank"
                        }
                    )
                },
//                title = {
//                    val effectiveSelectedAssessmentLabel =
//                        assessmentRows.firstOrNull { it.assessmentKey == selectedAssessmentKey }
//                            ?.assessmentLabel
//                            ?: selectedAssessmentLabel
//                            ?: "Taxonomy Bank"
//
//                    Text(
//                        if (selectedAssessmentKey == null) {
//                            "Taxonomy Bank"
//                        } else {
//                            effectiveSelectedAssessmentLabel
//                        }
//                    )
//                },
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
                            onAddClick(selectedAssessmentKey, effectiveSelectedAssessmentLabel)
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

            if (selectedItems.isEmpty()) {
                selectedAssessmentKey = null
                selectedAssessmentLabel = null
            }

            if (selectedAssessmentKey == null) {
                if (assessmentRows.isEmpty()) {
                    Text("No assessment labels found yet. Tap + to add one.")
                    return@Column
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(assessmentRows) { item ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAssessmentKey = item.assessmentKey
                                    selectedAssessmentLabel = item.assessmentLabel
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
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

                                Row {
                                    IconButton(
                                        onClick = {
                                            editAssessment = item
                                            editAssessmentLabel = item.assessmentLabel.replace("+", "").uppercase()
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Assessment")
                                    }

                                    IconButton(
                                        onClick = {
                                            deleteAssessment = item
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete Assessment")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (selectedItems.isEmpty()) {
                    Text("No taxonomy rows found for this assessment.")
                    return@Column
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(selectedItems) { item: AssessmentTaxonomy ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditClick(item.taxonomyId) }
//                                .clickable { onEdit(item.taxonomyId) }
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    item.categoryLabel.ifBlank {
                                        when (item.categoryKey.trim().uppercase()) {
                                            "QA" -> "Questions"
                                            "OBS" -> "Observations"
                                            else -> item.categoryKey
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.subCategoryLabel.ifBlank { item.subCategoryKey },
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "categoryKey=${item.categoryKey} • subCategoryKey=${item.subCategoryKey}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Order ${item.indexNum} • ${if (item.isActive) "Active" else "Inactive"}",
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