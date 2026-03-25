package com.example.charityDept.presentation.screens.admin.questions

import androidx.compose.foundation.layout.*
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
import com.example.charityDept.data.model.AssessmentQuestion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionFormScreen(
    questionIdArg: String?, // null -> new
    navigateUp: () -> Unit,
    onDone: (questionId: String) -> Unit,
    vm: AssessmentQuestionAdminViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    var category by remember { mutableStateOf("") }
    var subCategory by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }

    var indexNumText by remember { mutableStateOf("0") }
    var categoryKey by remember { mutableStateOf("") }
    var subCategoryKey by remember { mutableStateOf("") }

    // ✅ confirm delete dialog
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val taxonomy by vm.taxonomy.collectAsStateWithLifecycle()

    val categories = remember(taxonomy) {
        taxonomy
            .groupBy { it.categoryKey }
            .map { (key, items) -> key to (items.firstOrNull()?.categoryLabel ?: key) }
            .sortedBy { it.second }
    }

    val subCategories = remember(taxonomy, categoryKey) {
        taxonomy
            .filter { it.categoryKey == categoryKey }
            .sortedBy { it.indexNum }
    }

    // ✅ when editing (ONLY ONCE — removed duplicate LaunchedEffect)
    LaunchedEffect(questionIdArg) {
        val id = questionIdArg ?: return@LaunchedEffect
        loading = true
        val existing = vm.loadOnce(id)
        if (existing != null) {
            category = existing.category
            subCategory = existing.subCategory
            categoryKey = existing.categoryKey
            subCategoryKey = existing.subCategoryKey
            indexNumText = existing.indexNum.toString()
            question = existing.question
            isActive = existing.isActive
        }
        loading = false
    }

    // ✅ delete confirm dialog
    if (showDeleteConfirm && questionIdArg != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Question?") },
            text = {
                Text(
                    "This will soft-delete the question (it can be cleaned up later by the cleanup worker)."
                )
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
                            scope.launch {
                                val draft = AssessmentQuestion(
                                    questionId = questionIdArg ?: "",
                                    categoryKey = categoryKey.trim(),
                                    subCategoryKey = subCategoryKey.trim(),
                                    category = category.trim(),
                                    subCategory = subCategory.trim(),
                                    indexNum = indexNumText.trim().toIntOrNull() ?: 0,
                                    question = question.trim(),
                                    isActive = isActive
                                )
                                vm.upsert(draft) { id -> onDone(id) }
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = "Save")
                    }

                    if (questionIdArg != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true }
                        ) {
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
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())

            // CATEGORY dropdown
            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = !catExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false }
                ) {
                    categories.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                categoryKey = key
                                category = label

                                // reset subcategory when category changes
                                subCategoryKey = ""
                                subCategory = ""

                                catExpanded = false
                            }
                        )
                    }
                }
            }

            // SUBCATEGORY dropdown
            var subExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = subExpanded,
                onExpandedChange = {
                    if (categoryKey.isNotBlank()) subExpanded = !subExpanded
                }
            ) {
                OutlinedTextField(
                    value = subCategory,
                    onValueChange = {},
                    readOnly = true,
                    enabled = categoryKey.isNotBlank(),
                    label = { Text("Sub Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = subExpanded,
                    onDismissRequest = { subExpanded = false }
                ) {
                    subCategories.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.subCategoryLabel) },
                            onClick = {
                                subCategoryKey = t.subCategoryKey
                                subCategory = t.subCategoryLabel
                                subExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Question") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = indexNumText,
                onValueChange = { indexNumText = it },
                label = { Text("Order (indexNum)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Active")
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }
        }
    }
}

