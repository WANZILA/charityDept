@file:Suppress("FunctionName")

package com.example.charityDept.presentation.screens.attendance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.charityDept.data.model.AttendanceStatus
import com.example.charityDept.presentation.components.common.ProfileAvatar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsecutiveAttendanceScreen(
    toAttendanceDashboard: () -> Unit,
    vm: ConsecutiveAttendanceViewModel = hiltViewModel()
) {
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val query by vm.query.collectAsState()
    val present by vm.filteredPresent.collectAsState()
    val absent by vm.filteredAbsent.collectAsState()
    val reasons by vm.reasons.collectAsState()
    val eventLabels by vm.recentEventLabels.collectAsState()

    var tab by rememberSaveable { mutableStateOf(0) }

    // Knobs UI state
    var minStreak by rememberSaveable { mutableStateOf(1) }
    var lookback by rememberSaveable { mutableStateOf(2) }
    var minExpanded by remember { mutableStateOf(false) }
    var lookbackExpanded by remember { mutableStateOf(false) }
    val minStreakOptions = listOf(1, 2, 3, 4, 5)
    val lookbackOptions = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val isNarrow = LocalConfiguration.current.screenWidthDp < 380

    // AppBar hides on scroll
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // One shared list state so we can detect scroll across tabs
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 8
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text( "Attendance List" ) },
                    navigationIcon = {
                        IconButton(onClick = toAttendanceDashboard) {
                            Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
                        }
                    },
                    actions = {

//                            IconButton(onClick = { onEdit(child.childId) }) {
//                                Icon(Icons.Outlined.Edit, contentDescription = "Edit")
//                            }
                            IconButton(onClick = { vm.refresh() }, ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                            }
//

                            IconButton(onClick = toAttendanceDashboard) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close")
                            }

                    }
                ,
                    scrollBehavior = scrollBehavior
                )
            }
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(horizontal = 12.dp)
            ) {

                // === Filters (auto-hide on scroll) =======================================
                AnimatedVisibility(visible = !isScrolled) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = vm::setQuery,
                                label = { Text("Search by name or ID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (isNarrow) {
                                // Quick knobs row
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Unified threshold (both tabs)
                                    ExposedDropdownMenuBox(
                                        expanded = minExpanded,
                                        onExpandedChange = { minExpanded = !minExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = "Min: $minStreak",
                                            onValueChange = {},
                                            label = { Text("Min P / A") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = minExpanded,
                                            onDismissRequest = { minExpanded = false }
                                        ) {
                                            minStreakOptions.forEach { opt ->
                                                DropdownMenuItem(
                                                    text = { Text("$opt") },
                                                    onClick = {
                                                        minExpanded = false
                                                        if (minStreak != opt) {
                                                            minStreak = opt
                                                            vm.setMinStreak(opt) // applies to BOTH tabs
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    // Lookback dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = lookbackExpanded,
                                        onExpandedChange = { lookbackExpanded = !lookbackExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = "Events: $lookback",
                                            onValueChange = {},
                                            label = { Text("Recent ") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lookbackExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = lookbackExpanded,
                                            onDismissRequest = { lookbackExpanded = false }
                                        ) {
                                            lookbackOptions.forEach { opt ->
                                                DropdownMenuItem(
                                                    text = { Text("$opt") },
                                                    onClick = {
                                                        lookbackExpanded = false
                                                        if (lookback != opt) {
                                                            lookback = opt
                                                            vm.setLookback(opt)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(15.dp))


//                                ThresholdDropdown(
//                                    label = "Min Present / Absent",
//                                    valueText = "Threshold: $minStreak",
//                                    expanded = minExpanded,
//                                    onExpandedChange = { minExpanded = !minExpanded },
//                                    options = minStreakOptions,
//                                    onSelect = { minStreak = it; vm.setMinStreak(it) }
//                                )
//                                ThresholdDropdown(
//                                    label = "Recent Events",
//                                    valueText = "Lookback: $lookback",
//                                    expanded = lookbackExpanded,
//                                    onExpandedChange = { lookbackExpanded = !lookbackExpanded },
//                                    options = lookbackOptions,
//                                    onSelect = { lookback = it; vm.setLookback(it) }
//                                )
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Unified threshold (both tabs)
                                    ExposedDropdownMenuBox(
                                        expanded = minExpanded,
                                        onExpandedChange = { minExpanded = !minExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = "Min: $minStreak",
                                            onValueChange = {},
                                            label = { Text("Min P / A") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = minExpanded,
                                            onDismissRequest = { minExpanded = false }
                                        ) {
                                            minStreakOptions.forEach { opt ->
                                                DropdownMenuItem(
                                                    text = { Text("$opt") },
                                                    onClick = {
                                                        minExpanded = false
                                                        if (minStreak != opt) {
                                                            minStreak = opt
                                                            vm.setMinStreak(opt) // applies to BOTH tabs
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    // Lookback dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = lookbackExpanded,
                                        onExpandedChange = { lookbackExpanded = !lookbackExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = "Events: $lookback",
                                            onValueChange = {},
                                            label = { Text("Recent ") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lookbackExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = lookbackExpanded,
                                            onDismissRequest = { lookbackExpanded = false }
                                        ) {
                                            lookbackOptions.forEach { opt ->
                                                DropdownMenuItem(
                                                    text = { Text("$opt") },
                                                    onClick = {
                                                        lookbackExpanded = false
                                                        if (lookback != opt) {
                                                            lookback = opt
                                                            vm.setLookback(opt)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(15.dp))

                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(12.dp))
                // Tabs
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Present (${present.size})") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Absent (${absent.size})") })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Reasons (${reasons.size})") })
                }

                // Subtle summary
                Text(
                    text = "Min: $minStreak • Events: ${eventLabels.size} events",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Content lists share the same listState, so the header hides as soon as you scroll
                when {
                    loading -> Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                    error != null -> Text(error ?: "Error", color = MaterialTheme.colorScheme.error)

                    tab == 0 -> PresentList(present, eventLabels, listState)
                    tab == 1 -> AbsentList(absent, eventLabels, listState)
                    else     -> ReasonsList(reasons, listState)
                }
            }
        }
    }
}

@Composable
private fun PresentList(
    present: List<PresentCount>,
    eventLabels: List<String>,
    listState: LazyListState
) {
    if (present.isEmpty()) {
        Text("No matching present children.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(present) { pc ->
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                ListItem(
                    leadingContent = {
                        ProfileAvatar(
                            profileImageLocalPath = pc.child.profileImageLocalPath,
                            profileImg = pc.child.profileImg
                        )
                    },
                    headlineContent = {
                        Text(
                            (
                                    "${pc.child.fName ?: ""} ${pc.child.lName ?: ""}".trim())
                                .ifEmpty { pc.child.childId },
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Times present: ${pc.count} ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusChips(labels = eventLabels, statuses = pc.lastStatuses)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AbsentList(
    absent: List<AbsenceStreak>,
    eventLabels: List<String>,
    listState: LazyListState
) {
    if (absent.isEmpty()) {
        Text("No matching consecutive absentees.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(absent) { s ->
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                ListItem(
                    leadingContent = {
                        ProfileAvatar(
                            profileImageLocalPath = s.child.profileImageLocalPath,
                            profileImg = s.child.profileImg
                        )
                    },
                    headlineContent = {
                        Text(
                            (
                                    "${s.child.fName ?: ""} ${s.child.lName ?: ""}".trim())
                                .ifEmpty { s.child.childId },
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Consecutive absences: ${s.count} ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusChips(labels = eventLabels, statuses = s.lastStatuses)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ReasonsList(
    reasons: List<ReasonCount>,
    listState: LazyListState
) {
    if (reasons.isEmpty()) {
        Text("No absence reasons recorded in this window.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val total = reasons.sumOf { it.count }.coerceAtLeast(1)
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(reasons) { r ->
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                ListItem(
                    headlineContent = {
                        Text(
                            r.reason.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    supportingContent = {
                        val pct = ((r.count * 100f) / total).roundToInt()
                        Text(
                            "Count: ${r.count} (${pct}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}


@Composable
private fun StatusChips(
    labels: List<String>,
    statuses: List<AttendanceStatus>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.zip(statuses).forEach { (label, status) ->
            SuggestionChip(
                onClick = {},
                label = { Text("$label: ${if (status == AttendanceStatus.PRESENT) "P" else "A"}") }
            )
        }
    }
}

