package com.example.charityDept.presentation.screens.streets


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.charityDept.core.Utils.GenerateId
import com.example.charityDept.data.model.Street
import com.example.charityDept.presentation.screens.widgets.BulkConfirmDialog
import kotlinx.coroutines.launch

private const val TAG = "StreetsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreetsScreen(
    modifier: Modifier = Modifier,
    vm: StreetsViewModel = hiltViewModel()
) {
    val streets by vm.streets.collectAsState()
    val scope = rememberCoroutineScope()

    var search by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Street?>(null) }

    val list = remember(streets, search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) streets
        else streets.filter { s ->
            s.streetName.lowercase().contains(q) ||
                    s.manifestName.lowercase().contains(q) ||
                    s.manifestId.lowercase().contains(q)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showEditor = true
            }) { Icon(Icons.Default.Add, contentDescription = "Add street") }
        },
        topBar = {
            TopAppBar(title = { Text("Streets") })
        }
    ) { pad ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search streets…") }
            )

            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No streets yet")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.streetId }) { street ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }

                        StreetRow(
                            street = street,
                            onEdit = {
                                editing = street
                                showEditor = true
                            },
                            onDelete = { showDeleteConfirm = true },
                            onDeactivate = { // optional toggle via patch
                                scope.launch { vm.deactivate(street.streetId) }
                            }
                        )

                        BulkConfirmDialog(
                            show = showDeleteConfirm,
                            onDismiss = { showDeleteConfirm = false },
                            onConfirm = {
                                showDeleteConfirm = false
                                scope.launch {
                                    vm.delete(street.streetId)
                                    Log.d(TAG, "Deleted ${street.streetName}")
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
        AddEditStreetSheet(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { model, isNew ->
                val ensured = if (model.streetId.isBlank())
                    model.copy(streetId = GenerateId.generateId("street"))
                else model
                scope.launch { vm.addOrUpdate(ensured, isNew = model.streetId.isBlank() || isNew) }
                showEditor = false
            }
        )
    }
}

@Composable
private fun StreetRow(
    street: Street,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeactivate: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(street.streetName.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                val manifestLine = buildString {
                    if (street.manifestId.isNotBlank()) append("#${street.manifestId}  ")
                    if (street.manifestName.isNotBlank()) append(street.manifestName)
                }.ifBlank { "No manifest" }
                Text(manifestLine, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text("Active: ${street.isActive}", style = MaterialTheme.typography.bodySmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete") }
                // Uncomment if you want a quick deactivate button:
                // IconButton(onClick = onDeactivate) { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = "Deactivate") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditStreetSheet(
    initial: Street?,
    onDismiss: () -> Unit,
    onSave: (Street, Boolean) -> Unit
) {
    val isNew = initial == null
    var streetId by remember { mutableStateOf(initial?.streetId.orEmpty()) }
    var name by remember { mutableStateOf(initial?.streetName.orEmpty()) }
    var manifestId by remember { mutableStateOf(initial?.manifestId.orEmpty()) }
    var manifest by remember { mutableStateOf(initial?.manifestName.orEmpty()) }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }

    var showErrors by remember { mutableStateOf(false) }
    val nameErr = if (showErrors && name.isBlank()) "Name is required" else null

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (isNew) "Add Street" else "Edit Street", style = MaterialTheme.typography.titleLarge)

//            OutlinedTextField(
//                value = streetId,
//                onValueChange = { streetId = it },
//                label = { Text("Street ID (auto if blank)") },
//                singleLine = true,
//                modifier = Modifier.fillMaxWidth(),
//                enabled = isNew
//            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                isError = nameErr != null,
                supportingText = { if (nameErr != null) Text(nameErr) },
                label = { Text(" Street Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

//            OutlinedTextField(
//                value = manifestId,
//                onValueChange = { manifestId = it },
//                label = { Text("Manifest ID") },
//                singleLine = true,
//                modifier = Modifier.fillMaxWidth()
//            )

            OutlinedTextField(
                value = manifest,
                onValueChange = { manifest = it },
                label = { Text("Manifest Name ") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                Text("Active")
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = {
                    showErrors = true
                    if (nameErr == null) {
                        onSave(
                            Street(
                                streetId = streetId,
                                streetName = name.trim(),
                                manifestId = manifestId.trim(),
                                manifestName = manifest.trim(),
                                isActive = isActive
                            ),
                            isNew
                        )
                    }
                }) { Text("Save") }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

