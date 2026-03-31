package com.example.charityDept.presentation.screens.admin.taxonomy

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.charityDept.data.model.AssessmentTaxonomy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxonomyFormScreen(
    taxonomyIdArg: String?,
    navigateUp: () -> Unit,
    onDone: (id: String) -> Unit,
    vm: AssessmentTaxonomyAdminViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var assessmentKey by remember { mutableStateOf("") }
    var assessmentLabel by remember { mutableStateOf("") }
    var categoryKey by remember { mutableStateOf("") }
    var categoryLabel by remember { mutableStateOf("") }
    var subCategoryKey by remember { mutableStateOf("") }
    var subCategoryLabel by remember { mutableStateOf("") }
    var indexNumText by remember { mutableStateOf("0") }
    var isActive by remember { mutableStateOf(true) }

    LaunchedEffect(taxonomyIdArg) {
        val id = taxonomyIdArg ?: return@LaunchedEffect
        loading = true
        val existing = vm.loadOnce(id)
        if (existing != null) {
            assessmentKey = existing.assessmentKey
            assessmentLabel = existing.assessmentLabel
            categoryKey = existing.categoryKey
            categoryLabel = existing.categoryLabel
            subCategoryKey = existing.subCategoryKey
            subCategoryLabel = existing.subCategoryLabel
            indexNumText = existing.indexNum.toString()
            isActive = existing.isActive
        }
        loading = false
    }

    if (showDeleteConfirm && taxonomyIdArg != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Taxonomy Row?") },
            text = {
                Text("This will soft-delete the taxonomy row.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            vm.delete(taxonomyIdArg)
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
                title = { Text(if (taxonomyIdArg == null) "Add Taxonomy" else "Edit Taxonomy") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val draft = AssessmentTaxonomy(
                                    taxonomyId = taxonomyIdArg ?: "",
                                    assessmentKey = assessmentKey.trim(),
                                    assessmentLabel = assessmentLabel.trim(),
                                    categoryKey = categoryKey.trim(),
                                    categoryLabel = categoryLabel.trim(),
                                    subCategoryKey = subCategoryKey.trim(),
                                    subCategoryLabel = subCategoryLabel.trim(),
                                    indexNum = indexNumText.trim().toIntOrNull() ?: 0,
                                    isActive = isActive
                                )
                                vm.upsert(draft) { id -> onDone(id) }
                            }
                        },
                        enabled =
                            assessmentKey.isNotBlank() &&
                                    assessmentLabel.isNotBlank() &&
                                    categoryKey.isNotBlank() &&
                                    categoryLabel.isNotBlank() &&
                                    subCategoryKey.isNotBlank() &&
                                    subCategoryLabel.isNotBlank()
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = "Save")
                    }

                    if (taxonomyIdArg != null) {
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

            OutlinedTextField(
                value = assessmentKey,
                onValueChange = { assessmentKey = it },
                label = { Text("Assessment Key") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = assessmentLabel,
                onValueChange = { assessmentLabel = it },
                label = { Text("Assessment Label") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = categoryKey,
                onValueChange = { categoryKey = it },
                label = { Text("Split Key") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = categoryLabel,
                onValueChange = { categoryLabel = it },
                label = { Text("Split Label") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subCategoryKey,
                onValueChange = { subCategoryKey = it },
                label = { Text("Sub Category Key") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subCategoryLabel,
                onValueChange = { subCategoryLabel = it },
                label = { Text("Sub Category Label") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = indexNumText,
                onValueChange = { indexNumText = it },
                label = { Text("Order (indexNum)") },
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