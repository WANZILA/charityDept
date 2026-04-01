package com.example.charityDept.presentation.screens.admin.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
//import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.AssessmentQuestion
import com.example.charityDept.data.model.AssessmentTaxonomy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionFormScreen(
    questionIdArg: String?,
    initialAssessmentKeyArg: String? = null,
    initialAssessmentLabelArg: String? = null,
    navigateUp: () -> Unit,
    onDone: (questionId: String) -> Unit,
    vm: AssessmentQuestionAdminViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    var assessmentKey by remember { mutableStateOf("") }
    var categoryKey by remember { mutableStateOf("") }
    var subCategoryKey by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var indexNumText by remember { mutableStateOf("0") }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val taxonomy by vm.taxonomy.collectAsStateWithLifecycle()

    val adminUi by vm.ui.collectAsStateWithLifecycle()

    val assessments = remember(taxonomy) {
        taxonomy
            .groupBy { it.assessmentKey }
            .mapNotNull { (key, items) ->
                val label = items.firstOrNull()?.assessmentLabel.orEmpty()
                if (key.isBlank()) null else key to label.ifBlank { key }
            }
            .sortedBy { it.second }
    }

    val splits = remember(taxonomy, assessmentKey) {
        taxonomy
            .filter { it.assessmentKey == assessmentKey }
            .groupBy { it.categoryKey }
            .mapNotNull { (key, items) ->
                val label = items.firstOrNull()?.categoryLabel.orEmpty()
                if (key.isBlank()) null else key to label.ifBlank { key }
            }
            .sortedBy { it.second }
    }

    val leafRows = remember(taxonomy, assessmentKey, categoryKey) {
        taxonomy
            .filter { it.assessmentKey == assessmentKey && it.categoryKey == categoryKey }
            .sortedBy { it.indexNum }
    }

    val selectedAssessmentLabel = remember(
        assessments,
        assessmentKey,
        initialAssessmentKeyArg,
        initialAssessmentLabelArg
    ) {
        when {
            assessmentKey.isBlank() -> ""
            assessmentKey == initialAssessmentKeyArg && !initialAssessmentLabelArg.isNullOrBlank() ->
                initialAssessmentLabelArg.replace("+", " ").uppercase()
            else ->
                assessments.firstOrNull { it.first == assessmentKey }?.second.orEmpty()
        }
    }

    val selectedSplitLabel = remember(splits, categoryKey) {
        splits.firstOrNull { it.first == categoryKey }?.second.orEmpty()
    }

    val selectedLeaf: AssessmentTaxonomy? = remember(leafRows, subCategoryKey) {
        leafRows.firstOrNull { it.subCategoryKey == subCategoryKey }
    }

    val normalizedQuestion = remember(question) {
        question.trim().lowercase()
    }

    val duplicateQuestion = remember(
        adminUi.items,
        questionIdArg,
        assessmentKey,
        categoryKey,
        subCategoryKey,
        normalizedQuestion
    ) {
        if (normalizedQuestion.isBlank()) {
            null
        } else {
            adminUi.items.firstOrNull { existing ->
                existing.questionId != (questionIdArg ?: "") &&
                        !existing.isDeleted &&
                        existing.assessmentKey == assessmentKey &&
                        existing.categoryKey == categoryKey &&
                        existing.subCategoryKey == subCategoryKey &&
                        existing.question.trim().lowercase() == normalizedQuestion
            }
        }
    }

    LaunchedEffect(initialAssessmentKeyArg, initialAssessmentLabelArg, questionIdArg, taxonomy) {
        if (questionIdArg == null &&
            !initialAssessmentKeyArg.isNullOrBlank() &&
            assessmentKey.isBlank()
        ) {
            val exists = taxonomy.any { it.assessmentKey == initialAssessmentKeyArg }
            if (exists) {
                assessmentKey = initialAssessmentKeyArg
                categoryKey = ""
                subCategoryKey = ""
            }
        }
    }

    LaunchedEffect(questionIdArg) {
        val id = questionIdArg ?: return@LaunchedEffect
        loading = true
        val existing = vm.loadOnce(id)
        if (existing != null) {
            assessmentKey = existing.assessmentKey
            categoryKey = existing.categoryKey
            subCategoryKey = existing.subCategoryKey
            indexNumText = existing.indexNum.toString()
            question = existing.question
            isActive = existing.isActive
        }
        loading = false
    }

    if (showDeleteConfirm && questionIdArg != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Question?") },
            text = {
                Text("This will soft-delete the question.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            vm.delete(questionIdArg)
                            navigateUp()
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (questionIdArg == null) "Add Question" else "Edit Question") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val chosenLeaf = selectedLeaf ?: return@IconButton
                            scope.launch {
                                val draft = AssessmentQuestion(
                                    questionId = questionIdArg ?: "",
                                    assessmentKey = chosenLeaf.assessmentKey,
                                    assessmentLabel = chosenLeaf.assessmentLabel,
                                    categoryKey = chosenLeaf.categoryKey,
                                    subCategoryKey = chosenLeaf.subCategoryKey,
                                    category = chosenLeaf.categoryLabel,
                                    subCategory = chosenLeaf.subCategoryLabel,
                                    indexNum = indexNumText.trim().toIntOrNull() ?: 0,
                                    question = question.trim(),
                                    isActive = isActive
                                )
                                vm.upsert(draft) { id -> onDone(id) }
                            }
                        },
                        enabled =
                            assessmentKey.isNotBlank() &&
                                    categoryKey.isNotBlank() &&
                                    subCategoryKey.isNotBlank() &&
                                    selectedLeaf != null &&
                                    question.isNotBlank() &&
                                    duplicateQuestion == null
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = "Save")
                    }

                    if (questionIdArg != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            var assessmentExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = assessmentExpanded,
                onExpandedChange = { assessmentExpanded = !assessmentExpanded }
            ) {
                OutlinedTextField(
                    value = selectedAssessmentLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assessment Tool") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assessmentExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = assessmentExpanded,
                    onDismissRequest = { assessmentExpanded = false }
                ) {
                    assessments.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                assessmentKey = key
                                categoryKey = ""
                                subCategoryKey = ""
                                assessmentExpanded = false
                            }
                        )
                    }
                }
            }

            var splitExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = splitExpanded,
                onExpandedChange = {
                    if (assessmentKey.isNotBlank()) splitExpanded = !splitExpanded
                }
            ) {
                OutlinedTextField(
                    value = selectedSplitLabel,
                    onValueChange = {},
                    readOnly = true,
                    enabled = assessmentKey.isNotBlank(),
                    label = { Text("Split") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = splitExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = splitExpanded,
                    onDismissRequest = { splitExpanded = false }
                ) {
                    splits.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                categoryKey = key
                                subCategoryKey = ""
                                splitExpanded = false
                            }
                        )
                    }
                }
            }

            var leafExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = leafExpanded,
                onExpandedChange = {
                    if (assessmentKey.isNotBlank() && categoryKey.isNotBlank()) {
                        leafExpanded = !leafExpanded
                    }
                }
            ) {
                OutlinedTextField(
                    value = selectedLeaf?.subCategoryLabel.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    enabled = assessmentKey.isNotBlank() && categoryKey.isNotBlank(),
                    label = { Text("Category / Subcategory") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = leafExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = leafExpanded,
                    onDismissRequest = { leafExpanded = false }
                ) {
                    leafRows.forEach { row ->
                        DropdownMenuItem(
                            text = { Text(row.subCategoryLabel) },
                            onClick = {
                                subCategoryKey = row.subCategoryKey
                                leafExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Prompt / Item") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                isError = duplicateQuestion != null
            )
            if (duplicateQuestion != null) {
                Text(
                    "A question with the same text already exists in this assessment category.",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = indexNumText,
                onValueChange = { indexNumText = it },
                label = { Text("Question Order (indexNum)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Active")
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }
        }
    }
}