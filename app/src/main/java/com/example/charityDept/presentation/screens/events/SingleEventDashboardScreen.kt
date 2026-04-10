package com.example.charityDept.presentation.screens.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.Event
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.charityDept.data.local.projection.EventFrequentAttendeeRow
import com.example.charityDept.domain.repositories.offline.EligibleCounts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleEventDashboardScreen(
    eventIdArg: String,
    onEditEvent: (String) -> Unit,
    onAddChildEvent: (String) -> Unit,
    onOpenAttendance: (String) -> Unit,
    onOpenChildEvent: (String) -> Unit,
    onOpenFrequentAttendee: (String) -> Unit,
    navigateUp: () -> Unit,
    vm: SingleEventDashboardViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val authUI by authVM.ui.collectAsStateWithLifecycle()
    val canEdit = authUI.perms.canEditEvent

    LaunchedEffect(eventIdArg) {
        vm.load(eventIdArg)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        ui.event?.title?.ifBlank { "Single Event Dashboard" } ?: "Single Event Dashboard",
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    val event = ui.event
                    if (event != null && canEdit) {
                        IconButton(onClick = { onEditEvent(event.eventId) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit event")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            ui.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            ui.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ui.error ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            ui.event != null -> {
                DashboardContent(
                    event = ui.event!!,
                    childEvents = ui.childEvents,
                    childCount = ui.childCount,
                    attendanceSummary = ui.attendanceSummary,
                    frequentAttendees = ui.frequentAttendees,
                    childEventsLoaded = ui.childEventsLoaded,
                    frequentAttendeesLoaded = ui.frequentAttendeesLoaded,
                    childEventsLoading = ui.childEventsLoading,
                    frequentAttendeesLoading = ui.frequentAttendeesLoading,
                    onLoadChildEvents = { vm.loadChildEvents(eventIdArg) },
                    onLoadFrequentAttendees = { vm.loadFrequentAttendees(eventIdArg) },
                    onEditEvent = onEditEvent,
                    onAddChildEvent = onAddChildEvent,
                    onOpenAttendance = onOpenAttendance,
                    onOpenChildEvent = onOpenChildEvent,
                    onOpenFrequentAttendee = onOpenFrequentAttendee,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    event: Event,
    childEvents: List<Event>,
    childCount: Int,
    attendanceSummary: EligibleCounts,
    frequentAttendees: List<EventFrequentAttendeeRow>,
    childEventsLoaded: Boolean,
    frequentAttendeesLoaded: Boolean,
    childEventsLoading: Boolean,
    frequentAttendeesLoading: Boolean,
    onLoadChildEvents: () -> Unit,
    onLoadFrequentAttendees: () -> Unit,
    onEditEvent: (String) -> Unit,
    onAddChildEvent: (String) -> Unit,
    onOpenAttendance: (String) -> Unit,
    onOpenChildEvent: (String) -> Unit,
    onOpenFrequentAttendee: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalEligible = attendanceSummary.totalEligible.coerceAtLeast(0)
    val presentCount = attendanceSummary.presentEligible.coerceIn(0, totalEligible)
    val absentCount = (totalEligible - presentCount).coerceAtLeast(0)

    val presentValue = if (totalEligible > 0) {
        val percent = (presentCount * 100) / totalEligible
        "$presentCount ($percent%)"
    } else {
        "0 (0%)"
    }

    val absentValue = if (totalEligible > 0) {
        val percent = (absentCount * 100) / totalEligible
        "$absentCount ($percent%)"
    } else {
        "0 (0%)"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = event.title.ifBlank { "Untitled Event" },
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
            Text(
                text = "📊 Attendance Summary",
                style = MaterialTheme.typography.titleMedium
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                DashboardCardLike(
                    title = "✅ Present",
                    value = presentValue,
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {}
                DashboardCardLike(
                    title = "❌ Absent",
//                    value = (attendanceSummary.totalEligible - attendanceSummary.presentEligible)
//                        .coerceAtLeast(0)
//                        .toString(),
                    value = absentValue,
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {}
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCardLike(
                    title = "👧 Eligible Children",
//                    value = attendanceSummary.totalEligible.toString(),
                    value = totalEligible.toString(),
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {}
                DashboardCardLike(
                    title = "🌍 Scope",
                    value = "This event only",
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {}
            }
        }

        item {
            Text(
                text = "${event.eventDate.asDateOnly()} • ${event.eventStatus.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCardLike(
                    title = "✏️ Edit Event",
                    modifier = Modifier.weight(1f)
                ) {
                    onEditEvent(event.eventId)
                }

                DashboardCardLike(
                    title = "✅ Attendance",
                    modifier = Modifier.weight(1f)
                ) {
                    onOpenAttendance(event.eventId)
                }
            }
        }

        if (!event.isChild) {
            item {
                DashboardCardLike(
                    title = "➕ Add Child Event",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onAddChildEvent(event.eventId)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCardLike(
                    title = "📅 Child Events",
                    value = childCount.toString(),
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {}

                DashboardCardLike(
                    title = "🏷️ Type",
                    value = if (event.isChild) "Child Event" else "Parent Event",
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {}
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCardLike(
                    title = "📍 Location",
                    value = event.location.ifBlank { "-" },
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {}

                DashboardCardLike(
                    title = "🧬 Parent Event ID",
                    value = event.eventParentId.ifBlank { "-" },
                    modifier = Modifier.weight(1f),
                    enabled = false
                ) {}
            }
        }

        item {
            Text(
                text = "🧒 Child Events",
                style = MaterialTheme.typography.titleMedium
            )
        }



        if (!childEventsLoaded) {
            item {
                Button(
                    onClick = onLoadChildEvents,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (childEventsLoading) "Loading Child Events..." else "View Child Events")
                }
            }
        } else if (childEvents.isEmpty()) {
            item {
                Text(
                    text = "No child events linked yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(childEvents, key = { it.eventId }) { child ->
                ChildEventRow(
                    event = child,
                    onClick = { onOpenChildEvent(child.eventId) }
                )
            }
        }

        item {
            Text(
                text = if (event.isChild) {
                    "⭐ Frequent Attendees (This Event)"
                } else {
                    "⭐ Frequent Attendees (Parent + Children)"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (!frequentAttendeesLoaded) {
            item {
                Button(
                    onClick = onLoadFrequentAttendees,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (frequentAttendeesLoading) {
                            "Loading Frequent Children..."
                        } else {
                            "View Frequent Children"
                        }
                    )
                }
            }
        } else if (frequentAttendees.isEmpty()) {
            item {
                Text(
                    text = "No frequent attendees yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(frequentAttendees.take(10), key = { it.childId }) { row ->
                FrequentAttendeeRow(
                    row = row,
                    onClick = { onOpenFrequentAttendee(row.childId) }
                )
            }
        }
    }
}

@Composable
private fun DashboardCardLike(
    title: String,
    value: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        onClick = { if (enabled) onClick() },
        enabled = enabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (value.isNotBlank()) {
                Spacer(Modifier.width(0.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ChildEventRow(
    event: Event,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = event.title.ifBlank { "Untitled Child Event" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = event.eventDate.asDateOnly(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = event.location.ifBlank { "-" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Tap to open dashboard",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FrequentAttendeeRow(
    row: EventFrequentAttendeeRow,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = listOf(row.fName, row.lName).joinToString(" ").trim()
                            .ifBlank { "Unknown Child" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = row.childId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap to view attendance history",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = row.presentCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private val dateOnlyFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
private fun Timestamp.asDateOnly(): String = dateOnlyFmt.format(this.toDate())