// <app/src/main/java/com/example/zionkids/presentation/screens/events/EventListScreen.kt>
// /// CHANGED: Render Paging 3 LazyPagingItems with load-state handling.
// /// CHANGED: Preserve chips (offline/sync) from ViewModel snapshot state.
// /// CHANGED: Kept your search box (filters snapshot list; paging shows full active list).

package com.example.charityDept.presentation.screens.events

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.R
import com.example.charityDept.data.model.Event
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
// /// CHANGED: Paging compose imports
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.LoadState
import com.example.charityDept.presentation.screens.widgets.PendingSyncLabel
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    toEventForm: () -> Unit,
    navigateUp: () -> Unit,
    onEventClick: (String) -> Unit = {},
    vm: EventListViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }

    val authUI by authVM.ui.collectAsStateWithLifecycle()
    val canAdd = authUI.perms.canCreateEvent

    // /// CHANGED: Collect paging items
    val pagingItems = vm.pagedEvents.collectAsLazyPagingItems()

    val allLoadedEvents = (0 until pagingItems.itemCount)
        .mapNotNull { index -> pagingItems[index] }

    val visibleEvents = allLoadedEvents.filter { event ->
        search.isBlank() || event.title.contains(search, ignoreCase = true)
    }


    LaunchedEffect(ui.error) {
        ui.error?.let { Log.d("EventListScreen", "Error: $it") }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.events))
                        Spacer(Modifier.width(8.dp))
//                        if (ui.isOffline) {
//                            Spacer(Modifier.width(6.dp))
//                            SuggestionChip(onClick = {}, enabled = false, label = { Text("offline") })
//                        }
//                        if (ui.isSyncing) {
//                            Spacer(Modifier.width(6.dp))
//                            SuggestionChip(onClick = {}, enabled = false, label = { Text("syncing…") })
//                        }
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    if (canAdd){
                        IconButton(onClick = toEventForm) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add")
                        }
                    }
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {

            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Search by title…") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                )
            )

            Text(
                text = if (search.isBlank()) {
                    "${allLoadedEvents.size} event${if (allLoadedEvents.size == 1) "" else "s"}"
                } else {
                    "${visibleEvents.size} of ${allLoadedEvents.size} event${if (allLoadedEvents.size == 1) "" else "s"}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // /// CHANGED: Paging-driven content
            Box(Modifier.fillMaxSize()) {
                when (val state = pagingItems.loadState.refresh) {
                    is LoadState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is LoadState.Error -> Text(
                        "Failed to load: ${state.error.message ?: "unknown"}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    is LoadState.NotLoading -> {
                        if (allLoadedEvents.isEmpty()) {
                            Text("No events yet", modifier = Modifier.align(Alignment.Center))
                        } else if (visibleEvents.isEmpty()) {
                            Text("No events match your search", modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = visibleEvents,
                                    key = { it.eventId }
                                ) { event ->
                                    EventRow(event = event) { onEventClick(event.eventId) }
                                }

                                item {
                                    when (val ap = pagingItems.loadState.append) {
                                        is LoadState.Loading -> {
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.Center
                                            ) { CircularProgressIndicator() }
                                        }
                                        is LoadState.Error -> {
                                            Text(
                                                "More failed: ${ap.error.message ?: "unknown"}",
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            )
                                        }
                                        else -> Unit
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

@Composable
private fun EventRow(
    event: Event,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = event.title.ifBlank { "Untitled Event" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (event.isChild) "Child Event" else "Parent Event",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            text = event.eventStatus.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            if (!event.isDirty) {
                Spacer(Modifier.height(8.dp))
                PendingSyncLabel(isDirty = event.isDirty)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "📅 ${event.eventDate.asHuman()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (event.location.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "📍 ${event.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (event.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = event.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
/* ---------- Timestamp formatting (timestamps all through) ---------- */

private val humanFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

private fun Timestamp.asHuman(): String = humanFmt.format(this.toDate())

