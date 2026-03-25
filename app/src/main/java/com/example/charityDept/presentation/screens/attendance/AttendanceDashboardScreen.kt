package com.example.charityDept.presentation.screens.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.AttendanceStatus
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.Event
import com.example.charityDept.presentation.components.action.ZionKidAppTopBar
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDashboardScreen(
    navigateUp: () -> Unit,
    onContactGuardian: (childId: String) -> Unit,
    toPresent: (String) -> Unit,
    toAbsent: (String) -> Unit,
    toConsecutiveAbsentees: () -> Unit,
    vm: AttendanceDashboardViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ZionKidAppTopBar(
                canNavigateBack = false,
                txtLabel = ui.selectedEventTitle,
                dateTxt = ui.selectedEventDateText,
            )
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Text(
                    text = ui.error ?: "Error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Event picker
                        item {
                            EventPickerRow(
                                events = ui.events,
                                selectedId = ui.selectedEventId,
                                onSelect = vm::onSelectEvent
                            )
                        }

                        // /// CHANGED: tiny status row (Offline / Syncing / Online) using Room flags
//                        item {
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.End
//                            ) {
//                                StatusChip(
//                                    isOffline = ui.isOffline,
//                                    isSyncing = ui.isSyncing
//                                )
//                            }
//                        }

                        // Summary
                        item {
                            SummaryRow(
                                present = ui.present,
                                absent = ui.absent,
                                total = ui.total,
                                presentPct = ui.presentPct,
                                absentPct = ui.absentPct
                            )
                        }

                        // Trend
                        if (ui.trend.isNotEmpty()) {
                            item {
                                SectionCard(title = "Trend (last ${ui.trend.size} events)") {
                                    SimpleBarRow(
                                        data = ui.trend,
                                        onBarClick = { /* could jump to event id by label if mapped */ }
                                    )
                                }
                            }
                        }

                        // “Attendance List” entry point (kept, routes to your screen)
                        item {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(onClick = { toConsecutiveAbsentees() }) {
                                    Text(text = "View Attendance")
                                }
                            }
//                            SectionCard(
//                                title = "Attendance List",
//                                onClick = { toConsecutiveAbsentees() }  // you already passed a lambda
//                            ) {
//                                Text(
//                                    "Click.",
//                                    style = MaterialTheme.typography.bodyMedium
//                                )
//                            }

                        }
                    }
                }
            }
        }
    }
}

// /// CHANGED: restored and simplified status chip (no surprises)
//@Composable
//private fun StatusChip(
//    isOffline: Boolean,
//    isSyncing: Boolean
//) {
//    when {
//        isOffline -> {
//            AssistChip(
//                onClick = {},
//                leadingIcon = { Icon(Icons.Outlined.CloudOff, contentDescription = null) },
//                label = { Text("Offline") }
//            )
//        }
//        isSyncing -> {
//            AssistChip(
//                onClick = {},
//                leadingIcon = { Icon(Icons.Outlined.Sync, contentDescription = null) },
//                label = { Text("Syncing…") }
//            )
//        }
//        else -> {
//            AssistChip(
//                onClick = {},
//                leadingIcon = { Icon(Icons.Outlined.CloudDone, contentDescription = null) },
//                label = { Text("Online") }
//            )
//        }
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventPickerRow(
    events: List<Event>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = events.firstOrNull { it.eventId == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = selected?.title ?: "Select event",
            onValueChange = {},
            label = { Text("Event") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            events.forEach { ev ->
                DropdownMenuItem(
                    text = { Text("${ev.title}  —  ${formatShort(ev.eventDate)}") },
                    onClick = {
                        onSelect(ev.eventId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SummaryRow(
    present: Int,
    absent: Int,
    total: Int,
    presentPct: Int,
    absentPct: Int
) {
    SectionCard {
        Column {
            Text("Summary", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Present: $present ($presentPct%)")
            Text("Absent: $absent ($absentPct%)")
            Text("Total: $total")
        }
    }
}

@Composable
fun SimpleBarRow(
    data: List<Pair<String, Int>>,
    onBarClick: ((label: String) -> Unit)? = null
) {
    val max = (data.maxOfOrNull { it.second } ?: 1).toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        data.forEach { (label, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onBarClick != null) { onBarClick?.invoke(label) }
            ) {
                Text(label, modifier = Modifier.weight(1f))
                LinearProgressIndicator(
                    progress = value / max,
                    modifier = Modifier
                        .weight(0.3f) //.width(50.dp) //.weight(0.3f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
                Spacer(Modifier.width(6.dp))
                Text("$value", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun FlowChips(
    labels: List<String>,
    onChipClick: ((String) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    AssistChip(
                        onClick = { onChipClick?.invoke(label) },
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryRow(
    hist: ChildAttendanceHistory,
    onOpenChildDetails: (String) -> Unit,
    onContactGuardian: () -> Unit
) {
    val streak = hist.consecutiveAbsences
    val name = hist.child.fullName().trim()
    val title = if (streak >= 3) "⚠️ $name: ✗ $streak" else "$name: ✗ $streak"

    SectionCard(
        title = title,
        onClick = { onOpenChildDetails(hist.child.childId) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniHistoryPills(hist.lastEvents)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onContactGuardian) { Text("Contact") }
        }
    }
}

@Composable
fun MiniHistoryPills(statuses: List<AttendanceStatus>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        statuses.takeLast(6).forEach { st ->
            val label = if (st == AttendanceStatus.PRESENT) "✓" else "✗"
            AssistChip(onClick = {}, label = { Text(label) })
        }
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

/* ---------- Timestamp formatting (timestamps all through) ---------- */

private fun formatShort(ts: Timestamp): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(ts.toDate())

private fun Child.fullName(): String =
    listOfNotNull(fName, lName).joinToString(" ").trim()

//package com.example.charityDept.presentation.screens.attendance
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.outlined.CloudDone
//import androidx.compose.material.icons.outlined.CloudOff
//import androidx.compose.material.icons.outlined.Sync
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.example.charityDept.data.model.AttendanceStatus
//import com.example.charityDept.data.model.Child
//import com.example.charityDept.data.model.Event
//import com.example.charityDept.presentation.components.action.ZionKidAppTopBar
//import com.example.charityDept.presentation.screens.attendance.AttendanceDashboardViewModel
//import com.example.charityDept.presentation.screens.attendance.ChildAttendanceHistory
//import com.google.firebase.Timestamp
//import java.text.SimpleDateFormat
//import java.util.Locale
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AttendanceDashboardScreen(
//    navigateUp: () -> Unit,
//    onContactGuardian: (childId: String) -> Unit,
//    toPresent: (String) -> Unit,
//    toAbsent: (String) -> Unit,
//    toConsecutiveAbsentees: () -> Unit,
//    vm: AttendanceDashboardViewModel = hiltViewModel()
//) {
//    val ui by vm.ui.collectAsStateWithLifecycle()
//
//    Scaffold(
//        topBar = {
//            ZionKidAppTopBar(
//                canNavigateBack = false,
//                txtLabel = ui.selectedEventTitle,
//                dateTxt = ui.selectedEventDateText,
//            )
//        }
//    ) { inner ->
//        Box(Modifier.fillMaxSize().padding(inner)) {
//            when {
//                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
//                ui.error != null -> Text(
//                    text = ui.error ?: "Error",
//                    color = MaterialTheme.colorScheme.error,
//                    modifier = Modifier.align(Alignment.Center)
//                )
//                else -> {
//                    LazyColumn(
//                        modifier = Modifier.fillMaxSize(),
//                        contentPadding = PaddingValues(12.dp),
//                        verticalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        // Event picker
//                        item {
//                            EventPickerRow(
//                                events = ui.events,
//                                selectedId = ui.selectedEventId,
//                                onSelect = vm::onSelectEvent
//                            )
//                        }
//
//                        // Summary
//                        item {
//                            SummaryRow(
//                                present = ui.present,
//                                absent = ui.absent,
//                                total = ui.total,
//                                presentPct = ui.presentPct,
//                                absentPct = ui.absentPct
//                            )
//                        }
//
//                        // Trend
//                        if (ui.trend.isNotEmpty()) {
//                            item {
//                                SectionCard(title = "Trend (last ${ui.trend.size} events)") {
//                                    SimpleBarRow(
//                                        data = ui.trend,
//                                        onBarClick = { /* handle */ }
//                                    )
//                                }
//                            }
//                        }
//
//                        // Common reasons
////                        if (ui.notesTop.isNotEmpty()) {
////                            item {
////                                SectionCard(title = "Common Reasons (absent)") {
////                                    FlowChips(
////                                        ui.notesTop.map { "${it.first} (${it.second})" }
////                                    )
////                                }
////                            }
////                        }
//
//
//                        item {
//
//                            SectionCard(
//                                title = "Attendance List",
//                                onClick = { toConsecutiveAbsentees() }) {
//                            }
//
//                        }
//
//
//                    }
//                }
//            }
//        }
//    }
//}
//
////@Composable
////private fun StatusChip(
////    isOffline: Boolean,
////    isSyncing: Boolean
////) {
////    when {
////        isOffline -> {
////            AssistChip(
////                onClick = {},
////                leadingIcon = { Icon(Icons.Outlined.CloudOff, contentDescription = null) },
////                label = { Text("Offline") }
////            )
////        }
////        isSyncing -> {
////            AssistChip(
////                onClick = {},
////                leadingIcon = { Icon(Icons.Outlined.Sync, contentDescription = null) },
////                label = { Text("Syncing…") }
////            )
////        }
////        else -> {
////            AssistChip(
////                onClick = {},
////                leadingIcon = { Icon(Icons.Outlined.CloudDone, contentDescription = null) },
////                label = { Text("Online") }
////            )
////        }
////    }
////}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun EventPickerRow(
//    events: List<Event>,
//    selectedId: String?,
//    onSelect: (String) -> Unit
//) {
//    var expanded by remember { mutableStateOf(false) }
//    val selected = events.firstOrNull { it.eventId == selectedId }
//    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
//        OutlinedTextField(
//            readOnly = true,
//            value = selected?.title ?: "Select event",
//            onValueChange = {},
//            label = { Text("Event") },
//            colors = OutlinedTextFieldDefaults.colors(
//                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
//                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
//                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
//            ),
//            modifier = Modifier
//                .menuAnchor()
//                .fillMaxWidth()
//        )
//        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
//            events.forEach { ev ->
//                DropdownMenuItem(
//                    text = { Text("${ev.title}  —  ${formatShort(ev.eventDate)}") },
//                    onClick = {
//                        onSelect(ev.eventId)
//                        expanded = false
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun SummaryRow(
//    present: Int,
//    absent: Int,
//    total: Int,
//    presentPct: Int,
//    absentPct: Int
//) {
//    SectionCard {
//        Column {
//            Text("Summary", style = MaterialTheme.typography.titleMedium)
//            Spacer(Modifier.height(8.dp))
//            Text("Present: $present ($presentPct%)")
//            Text("Absent: $absent ($absentPct%)")
//            Text("Total: $total")
//        }
//    }
//}
//
//@Composable
//fun SimpleBarRow(
//    data: List<Pair<String, Int>>,
//    onBarClick: ((label: String) -> Unit)? = null
//) {
//    val max = (data.maxOfOrNull { it.second } ?: 1).toFloat()
//    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
//        data.forEach { (label, value) ->
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable(enabled = onBarClick != null) { onBarClick?.invoke(label) }
//            ) {
//                Text(label, modifier = Modifier.width(60.dp))
//                LinearProgressIndicator(
//                    progress = value / max,
//                    modifier = Modifier
//                        .weight(1f)
//                        .height(8.dp)
//                        .clip(RoundedCornerShape(50))
//                )
//                Spacer(Modifier.width(6.dp))
//                Text("$value", style = MaterialTheme.typography.labelSmall)
//            }
//        }
//    }
//}
//
//@Composable
//fun FlowChips(
//    labels: List<String>,
//    onChipClick: ((String) -> Unit)? = null
//) {
//    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
//        labels.chunked(3).forEach { row ->
//            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                row.forEach { label ->
//                    AssistChip(
//                        onClick = { onChipClick?.invoke(label) },
//                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun HistoryRow(
//    hist: ChildAttendanceHistory,
//    onOpenChildDetails: (String) -> Unit,
//    onContactGuardian: () -> Unit
//) {
//    val streak = hist.consecutiveAbsences
//    val name = hist.child.fullName().trim()
//    val title = if (streak >= 3) "⚠️ $name: ✗ $streak" else "$name: ✗ $streak"
//
//    SectionCard(
//        title = title,
//        onClick = { onOpenChildDetails(hist.child.childId) }
//    ) {
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            MiniHistoryPills(hist.lastEvents)
//            Spacer(Modifier.weight(1f))
//            TextButton(onClick = onContactGuardian) { Text("Contact") }
//        }
//    }
//}
//
//@Composable
//fun MiniHistoryPills(statuses: List<AttendanceStatus>) {
//    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
//        statuses.takeLast(6).forEach { st ->
//            val label = if (st == AttendanceStatus.PRESENT) "✓" else "✗"
//            AssistChip(onClick = {}, label = { Text(label) })
//        }
//    }
//}
//
//@Composable
//fun SectionCard(
//    title: String? = null,
//    onClick: (() -> Unit)? = null,
//    content: @Composable ColumnScope.() -> Unit
//) {
//    ElevatedCard(
//        modifier = Modifier
//            .fillMaxWidth()
//            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
//    ) {
//        Column(Modifier.fillMaxWidth().padding(12.dp)) {
//            if (title != null) {
//                Text(title, style = MaterialTheme.typography.titleMedium)
//                Spacer(Modifier.height(8.dp))
//            }
//            content()
//        }
//    }
//}
//
///* ---------- Timestamp formatting (timestamps all through) ---------- */
//
//private fun formatShort(ts: Timestamp): String =
//    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(ts.toDate())
//
//private fun Child.fullName(): String =
//    listOfNotNull(fName, lName).joinToString(" ").trim()

