package com.example.charityDept.presentation.screens.children

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountsScreen(
    navigateUp: () -> Unit,
    onItemClick: (name: String) -> Unit = {},
    vm: CountsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    val shown by remember(ui.items, query) {
        derivedStateOf {
            val needle = query.trim().lowercase()
            if (needle.isEmpty()) ui.items
            else ui.items.filter { (name, _) ->
                name.lowercase().contains(needle)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("All ${vm.title}") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Search…") },
                singleLine = true
            )

            Row(
                Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total children: ${ui.totalChildren}")
                Spacer(Modifier.width(12.dp))
//                Text("Unique: ${ui.uniqueKeys}")
                Spacer(Modifier.width(12.dp))
                Text("Showing ${shown.size} of ${ui.uniqueKeys}")
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

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = shown,
                            key = { it.first }
                        ) { (name, count) ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onItemClick(name) }
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, style = MaterialTheme.typography.titleMedium)
                                    AssistChip(onClick = {}, label = { Text(count.toString()) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

