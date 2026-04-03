package com.example.charityDept.presentation.screens.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.Event
import com.example.charityDept.presentation.screens.widgets.DeleteIconWithConfirm
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    eventIdArg: String,
    onEdit: (String) -> Unit,
    toAttendanceRoster: (String, String) -> Unit,
    toEventList: () -> Unit,
    navigateUp: (String) -> Unit,
    vm: EventDetailsViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val authUI by authVM.ui.collectAsStateWithLifecycle()
    val canEdit = authUI.perms.canEditEvent
    val canDelete = authUI.perms.canDeleteEvent

    // load once
    LaunchedEffect(eventIdArg) { vm.load(eventIdArg) }

//    LaunchedEffect(Unit) {
//        vm.events.collectLatest { ev ->
//            when (ev) {
//                is EventDetailsViewModel.EventDetailsEvent.Deleted -> navigateUp()
//                is EventDetailsViewModel.EventDetailsEvent.Error -> snackbarHostState.showSnackbar(ev.msg)
//            }
//        }
//    }
    LaunchedEffect(vm) {
        vm.events.collect { ev ->
            when (ev) {
                is EventDetailsViewModel.EventDetailsEvent.Deleted -> {
                    snackbarHostState.showSnackbar("Event deleted")
                    toEventList()
                }
                is EventDetailsViewModel.EventDetailsEvent.Error -> {
                    snackbarHostState.showSnackbar(ev.msg)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(ui.event?.title?.ifBlank { "Event details" } ?: "Event details", style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = { navigateUp(eventIdArg) }) {
                        Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    ui.event?.let { event ->
                        if (canEdit) {
                        IconButton(onClick = { onEdit(event.eventId) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                        } }
                        IconButton(onClick = { vm.refresh(event.eventId) }, enabled = true) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }

                        if(canDelete){
                            DeleteIconWithConfirm(
                                label = "Event: ${event.title} ".trim(),
                                deleting = ui.deleting,
                                onDelete = {
                                    vm.deleteChildOptimistic()
                                }
                            )
                        }
//
                        IconButton(onClick = toEventList) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Text(
                    "Error: ${ui.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                ui.event != null -> DetailsContent(
                    event = ui.event!!,
                    toAttendanceRoster = toAttendanceRoster
                )
            }
        }
    }


}

@Composable
private fun DetailsContent(
    event: Event,
    toAttendanceRoster: (String, String) -> Unit
) {
    var openEvent by rememberSaveable { mutableStateOf(true) }
    var openMeta by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { toAttendanceRoster(event.eventId, event.adminId) }) {
                    Text(text = "Attendance List")
                }
            }
        }
        item {
            CollapsibleSection("Event Info", openEvent, { openEvent = !openEvent }) {
                Field("Title", event.title.ifBlank { "-" })
                Field("Date", event.eventDate.asDateOnly())     // Timestamp ✅
                Field("Status", event.eventStatus.name)
                Field("Location", event.location.ifBlank { "-" })
                Field("Team Name", event.teamName)     // Timestamp ✅
                Field("Team Leader", event.teamLeaderNames)
                Field("Tel", event.leaderTelephone1.ifBlank { "-" })
                Field("Tel", event.leaderTelephone2.ifBlank { "-" })
                Field("Notes", event.notes.ifBlank { "-" })
            }
        }
        item {
            CollapsibleSection("Meta", openMeta, { openMeta = !openMeta }) {
                Field("Created at", event.createdAt.asHuman())  // Timestamp ✅
                Field("Updated at", event.updatedAt.asHuman())  // Timestamp ✅
                Field("Event ID", event.eventId.ifBlank { "-" })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/* ---------- Timestamp formatting helpers (timestamps all through) ---------- */

private val humanFmt = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
private val dateOnlyFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

private fun Timestamp.asHuman(): String = humanFmt.format(this.toDate())
private fun Timestamp.asDateOnly(): String = dateOnlyFmt.format(this.toDate())

