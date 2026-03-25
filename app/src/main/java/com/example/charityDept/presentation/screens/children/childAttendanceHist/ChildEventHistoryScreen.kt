package com.example.charityDept.presentation.screens.children.childAttendanceHist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildEventHistoryScreen(
    childIdArg: String,
    navigateUp: () -> Unit,
    onEventClick: (eventId: String) -> Unit = {},
    vm: ChildEventHistoryViewModel = hiltViewModel()
) {
    LaunchedEffect(childIdArg) {
        if (childIdArg.isNotBlank()) vm.setChildId(childIdArg)
    }

    val ui by vm.ui.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    val shown by remember(ui.items, query) {
        derivedStateOf {
            val needle = query.trim().lowercase()
            if (needle.isEmpty()) ui.items
            else ui.items.filter { row ->
                row.title.lowercase().contains(needle) ||
                        row.teamName.lowercase().contains(needle) ||
                        row.location.lowercase().contains(needle) ||
                        row.notes.lowercase().contains(needle) // ✅ allow searching by reason/notes
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Child Events") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {

            // Mode chips (KISS)
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = ui.mode == ChildEventMode.ALL,
                    onClick = { vm.setMode(ChildEventMode.ALL) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = ui.mode == ChildEventMode.ATTENDED,
                    onClick = { vm.setMode(ChildEventMode.ATTENDED) },
                    label = { Text("Attended") }
                )
                FilterChip(
                    selected = ui.mode == ChildEventMode.MISSED,
                    onClick = { vm.setMode(ChildEventMode.MISSED) },
                    label = { Text("Missed") }
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                placeholder = { Text("Search…") },
                singleLine = true
            )

            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total: ${ui.items.size}")
                Spacer(Modifier.width(12.dp))
                Text("Showing ${shown.size} of ${ui.items.size}")
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    ui.error != null -> Text(
                        "Failed to load: ${ui.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    shown.isEmpty() -> Text("No matches", modifier = Modifier.align(Alignment.Center))
                    else -> {
                        val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(shown, key = { it.eventId }) { row ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onEventClick(row.eventId) }
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(row.title, style = MaterialTheme.typography.titleMedium)

                                        Spacer(Modifier.height(4.dp))

                                        Text(
                                            "${df.format(row.eventDate.toDate())} • ${row.teamName}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        if (row.location.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(row.location, style = MaterialTheme.typography.bodySmall)
                                        }

                                        // ✅ Show reason/notes (especially for MISSED)
                                        if (row.notes.isNotBlank()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = row.notes,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        AssistChip(
                                            onClick = {},
                                            label = { Text(row.status.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

