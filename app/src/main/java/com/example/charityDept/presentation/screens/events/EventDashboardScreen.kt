// EventsDashboardScreen.kt
package com.example.charityDept.presentation.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.R
import com.example.charityDept.data.model.Event
import com.example.charityDept.data.model.EventStatus
import com.example.charityDept.presentation.components.action.CharityDeptAppTopBar
import com.example.charityDept.presentation.screens.widgets.PendingSyncLabel
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDashboardScreen(
    onAddEvent: () -> Unit,
    onOpenEvent: (String) -> Unit,
    toEventList: () -> Unit,
    toEventDetails: (String) -> Unit,
    toEventForm: () -> Unit,
    vm: EventDashboardViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()

//    val authUI by authVM.ui.collectAsStateWithLifecycle()
//    val canAdd = authUI.perms.canCreateEvent
//    val canAdd = authVM.ui.perms.canCreateEvent

    Scaffold(
        topBar = {
            CharityDeptAppTopBar(
                canNavigateBack = false,
                txtLabel = stringResource(R.string.events),
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DashboardHeader(
                    totalThisMonth = ui.totalThisMonth,  //num of events in this month
                    nextEvent = ui.nextEvent,
                    activeNowCount = ui.activeNowCount,
                    statusFilter = ui.statusFilter,
                    onToggle = vm::toggleStatusFilter,
                    selectedTab = ui.selectedTab,
                    toEventList = toEventList,
                    onUpcoming = { vm.setTab(EventTab.UPCOMING) },
                    onPast = { vm.setTab(EventTab.PAST) },
                    toEventForm = toEventForm
                )
            }

//            item {
//                StatCard(
//                    title = "Events",
//                    value = "View All",
//                    modifier = Modifier.fillMaxWidth(),
//                    onClick = toEventList
//                )
//            }

            when {
                ui.loading -> {
                    item {
                        Box(
                            Modifier
                                .fillParentMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }
                ui.error != null -> {
                    item {
                        Text(
                            text = "Error: ${ui.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                ui.visible.isEmpty() -> {
                    item {
                        Text(
                            "No events here yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    items(ui.visible, key = { it.eventId }) { e ->
                        EventRow(
                            event = e,
                            onClick = { onOpenEvent(e.eventId) },
                            toEventDetails = { toEventDetails(e.eventId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabsRow(
    selected: EventTab,
    onUpcoming: () -> Unit,
    onPast: () -> Unit
) {
    TabRow(selectedTabIndex = if (selected == EventTab.UPCOMING) 0 else 1) {
        Tab(
            selected = selected == EventTab.UPCOMING,
            onClick = onUpcoming,
            text = { Text("Upcoming") }
        )
        Tab(
            selected = selected == EventTab.PAST,
            onClick = onPast,
            text = { Text("Past") }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DashboardHeader(
    totalThisMonth: Int,
    nextEvent: Event?,
    activeNowCount: Int,
    statusFilter: Set<EventStatus>,
    onToggle: (EventStatus) -> Unit,
    selectedTab: EventTab,
    toEventList: () -> Unit,
    onUpcoming: () -> Unit,
    toEventForm: () -> Unit,
    onPast: () -> Unit,
    authVM: AuthViewModel = hiltViewModel(),
) {
    val allowedStatuses: Set<EventStatus> = when (selectedTab) {
        EventTab.UPCOMING -> setOf(EventStatus.SCHEDULED, EventStatus.ACTIVE, EventStatus.DONE)
        EventTab.PAST     -> setOf(EventStatus.SCHEDULED, EventStatus.ACTIVE, EventStatus.DONE)
        EventTab.NEW -> TODO()
    }
    val selectedIndex = if (selectedTab == EventTab.UPCOMING) 0 else 1

    val authUI by authVM.ui.collectAsStateWithLifecycle()
    val canAdd = authUI.perms.canCreateEvent
//    val canAdd = authVM.ui.perms.canCreateEvent
//
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // KPI row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "This Month",
                value = totalThisMonth.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Active Now",
                value = activeNowCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if(canAdd){
                StatCard(
                    title = "Event",
                    value = "Add New",
                    modifier = Modifier.weight(1f),
                    onClick = toEventForm
                )
            }
            StatCard(
                title = "Event",
                value = "View All",
                modifier = Modifier.weight(1f),
                onClick = toEventList
            )

        }
        // Tabs
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface,
            indicator = { positions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(positions[selectedIndex])
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        ) {
            Tab(
                selected = selectedTab == EventTab.UPCOMING,
                onClick = onUpcoming,
                text = { Text("Upcoming") }
            )
            Tab(
                selected = selectedTab == EventTab.PAST,
                onClick = onPast,
                text = { Text("Past") }
            )
        }




        // Status Filters
        Text("Status Filters", style = MaterialTheme.typography.titleSmall)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventStatus.entries
                .filter { it in allowedStatuses }
                .forEach { st ->
                    FilterChip(
                        selected = st in statusFilter,
                        onClick = { onToggle(st) },
                        label = { Text(st.name) },
                        leadingIcon = { Icon(Icons.Outlined.FilterList, contentDescription = null) }
                    )
                }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String = "",
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier,
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EventRow(
    event: Event,
    onClick: () -> Unit,
    toEventDetails: () -> Unit
) {
    val label = when (event.eventStatus) {
        EventStatus.SCHEDULED -> "S"
        EventStatus.ACTIVE    -> "A"
        EventStatus.DONE -> "D"

    }
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // ✅ counter badge on the left
//                    IndexBadge(index = index)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        event.title.ifBlank { "Untitled Event" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    PendingSyncLabel(isDirty = event.isDirty)
                }
                Text(
                    formatDate(event.eventDate), // Timestamp ✅
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.location.isNotBlank()) {
                    Text(
                        event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (event.notes.isNotBlank()) {
                    Text(
                        event.notes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AssistChip(
                onClick = onClick,
                label = { Text(label) }
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = toEventDetails) {
                Icon(Icons.Outlined.RemoveRedEye, contentDescription = "Edit")
            }
        }
    }
}

/* ---------- Timestamp formatting (timestamps all through) ---------- */

private val dayFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

private fun formatDate(ts: Timestamp): String = dayFmt.format(ts.toDate())

