package com.example.charityDept.presentation.screens.technicalSkills

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.charityDept.core.utils.GenerateId
import com.example.charityDept.data.model.TechnicalSkill
//import com.example.upliftadmin.data.models.TechnicalSkill
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable
import com.example.charityDept.presentation.screens.widgets.BulkConfirmDialog

private const val TAG = "TechnicalSkillsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalSkillsScreen(
    modifier: Modifier = Modifier,
    vm: TechnicalSkillsViewModel = hiltViewModel()
) {
    val skills by vm.skills.collectAsState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    // UI state
    var search by remember { mutableStateOf(TextFieldValue("")) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TechnicalSkill?>(null) }
    var searchResults by remember { mutableStateOf<List<TechnicalSkill>?>(null) }

    LaunchedEffect(search.text) {
        val q = search.text.trim()
        searchResults = if (q.isBlank()) null else vm.search(q)
    }

    val list = searchResults ?: skills

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showEditor = true
            }) { Icon(Icons.Default.Add, contentDescription = "Add skill") }
        },
        topBar = {
            TopAppBar(
                title = { Text("Technical Skills") },
                actions = {}
            )
        }
    ) { pad ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            // Search
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search skills by name…") }
            )

            // List
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No skills yet")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.skillId }) { skill ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        SkillRow(
                            skill = skill,
                            onEdit = {
                                editing = skill
                                showEditor = true
                            },
                            onDeactivate = {
                                scope.launch {
                                    vm.deactivate(skill.skillId)
                                    Log.d(TAG, "Deactivated ${skill.name}")
                                }
                            },
                            onDelete = {
                                showDeleteConfirm = true
//                                scope.launch {
//                                    vm.delete(skill.skillId)
//                                    Log.d(TAG, "Deleted ${skill.name}")
//                                }
                            }
                        )

                        BulkConfirmDialog(
                            show = showDeleteConfirm,
                            onDismiss = { showDeleteConfirm = false },
                            onConfirm = {
                                showDeleteConfirm = false
                                scope.launch {
                                    vm.delete(skill.skillId)
                                    Log.d(TAG, "Deleted ${skill.name}")
                                }
                            },
                            message = "This action cannot be undone."
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        AddEditSkillSheet(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { model, isNew ->
                // id must be provided by caller (you can plug in your GenerateId if needed)
                if (model.skillId.isBlank()) {
                    // Simple guard; let form handle validation, but skillId must not be blank on upsert
                    // In your flow you likely pass an ensuredId before calling repo.upsert
                    // For convenience, generate here:
                    val ensured = model.copy(skillId = GenerateId.generateId("technicalSkills"))
                    scope.launch { vm.addOrUpdate(ensured, isNew = true) }
                } else {
                    scope.launch { vm.addOrUpdate(model, isNew) }
                }
                showEditor = false
            }
        )
    }
}


@Composable
private fun SkillRow(
    skill: TechnicalSkill,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            skill.name.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text(skill.category.ifBlank { "Uncategorized" }) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Level: ${skill.level} • Active: ${skill.isActive}", style = MaterialTheme.typography.bodyMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
//                IconButton(onClick = onDeactivate) { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = "Deactivate") }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditSkillSheet(
    initial: TechnicalSkill?,
    onDismiss: () -> Unit,
    onSave: (TechnicalSkill, Boolean) -> Unit
) {
    val isNew = initial == null

    var skillId by remember { mutableStateOf(initial?.skillId.orEmpty()) } // can be auto-filled on save
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category.orEmpty()) }
    var level by remember { mutableStateOf(initial?.level ?: 3) }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }

    // Validation errors (show on Save)
    var showErrors by remember { mutableStateOf(false) }
    val nameErr = remember(name, showErrors) {
        if (!showErrors) null
        else if (name.isBlank()) "Name is required" else null
    }
    val levelErr = remember(level, showErrors) {
        if (!showErrors) null
        else if (level !in 1..5) "Level must be between 1 and 5" else null
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (isNew) "Add Skill" else "Edit Skill", style = MaterialTheme.typography.titleLarge)

            // Skill ID (read-only if editing; optional if new)
            OutlinedTextField(
                value = skillId,
                onValueChange = { skillId = it },
                label = { Text("Skill ID (auto if blank)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = isNew // allow manual override only for new
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                isError = nameErr != null,
                supportingText = { if (nameErr != null) Text(nameErr) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            CategoryPicker(
                value = category,
                onValueChange = { category = it }
            )

            Column {
                Text("Level: $level")
                Slider(
                    value = level.toFloat(),
                    onValueChange = { level = it.toInt().coerceIn(1, 5) },
                    valueRange = 1f..5f,
                    steps = 3 // 1..5 → 3 steps between
                )
                if (levelErr != null) {
                    Text(levelErr, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                Text("Active")
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = {
                    showErrors = true
                    if (nameErr == null && levelErr == null) {
                        val model = TechnicalSkill(
                            skillId = skillId, // may be blank; caller can generate
                            name = name.trim(),
                            category = category.trim(),
                            level = level.coerceIn(1, 5),
                            isActive = isActive
                        )
                        onSave(model, isNew)
                    }
                }) { Text("Save") }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    value: String,
    onValueChange: (String) -> Unit,
    categories: List<String> = listOf("Mobile", "Web", "Backend", "Data", "3D", "DevOps", "Other")
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text("Category") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat) },
                    onClick = {
                        onValueChange(cat)
                        expanded = false
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillRow(
    skill: TechnicalSkill,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelect() else onEdit()
                },
                onLongClick = onLongPress
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(skill.name.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = {}, label = { Text(skill.category.ifBlank { "Uncategorized" }) })
                }
                Spacer(Modifier.height(4.dp))
                Text("Level: ${skill.level} • Active: ${skill.isActive}", style = MaterialTheme.typography.bodyMedium)
            }

            if (!selectionMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = onDeactivate) { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = "Deactivate") }
                    IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete") }
                }
            }
        }
    }
}

