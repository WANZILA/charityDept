@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.charityDept.presentation.screens.reports

// com/example/zionkids/presentation/report/ReportScreen.kt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun ReportScreen(vm: ReportViewModel = hiltViewModel()) {
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reports", style = MaterialTheme.typography.headlineSmall)

        DateRangeFilter { startMillis, endExclusive -> vm.loadCustom(startMillis, endExclusive) }

        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        state.monthly?.let { r ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Monthly / Range: ${r.label}", style = MaterialTheme.typography.titleMedium)
                    Text("Grand total (all souls / children): ${r.totalChildren}")
                    Text("Attendance — 1×:${r.attendanceBuckets.times1}  2×:${r.attendanceBuckets.times2}  3×:${r.attendanceBuckets.times3}  4×+:${r.attendanceBuckets.times4plus}")
                    Text("≥50% attendance: ${r.percentAtLeast50}%")
                    Text("Spiritual growth — new decisions: ${r.newDecisions} | total to date: ${r.totalAcceptedJesusToDate}")
                    Text("Educational support (Primary P1–P7): ${r.sponsoredPrimaryP1toP7}")
                    Text("Skills training (Mechanics): ${r.skillsMechanicsEnrolled}")
                    Text("Family resettlement — in period: ${r.resettledThisPeriod} | to date: ${r.resettledToDate}")
                }
            }
        }

        state.quarterly?.let { r ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Quarterly: ${r.label}", style = MaterialTheme.typography.titleMedium)
                    Text("Grand total (all souls / children): ${r.totalChildren}")
                    Text("Attendance — 1×:${r.attendanceBuckets.times1}  2×:${r.attendanceBuckets.times2}  3×:${r.attendanceBuckets.times3}  4×+:${r.attendanceBuckets.times4plus}")
                    Text("≥50% attendance: ${r.percentAtLeast50}%")
                    Text("Spiritual growth — new decisions: ${r.newDecisions} | total to date: ${r.totalAcceptedJesusToDate}")
                    Text("Educational support (Primary P1–P7): ${r.sponsoredPrimaryP1toP7}")
                    Text("Skills training (Mechanics): ${r.skillsMechanicsEnrolled}")
                    Text("Family resettlement — this quarter: ${r.resettledThisPeriod} | to date: ${r.resettledToDate}")
                }
            }
        }
    }
}



@Composable
fun DateRangeFilter(
    onApply: (startMillis: Long, endMillisExclusive: Long) -> Unit
) {
    var show by remember { mutableStateOf(false) }
    val state = rememberDateRangePickerState()

    // Launcher card
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Custom range", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { show = true }) { Text("Pick dates") }
        }
    }

    if (show) {
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                    onClick = {
                        val s = state.selectedStartDateMillis!!
                        val e = state.selectedEndDateMillis!!
                        val (startMillis, endExclusive) = normalizeToLocalDayBounds(s, e)
                        show = false
                        onApply(startMillis, endExclusive)
                    }
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DateRangePicker(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp) // keep it comfy on small screens
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                title = { Text("Select date range") },
                // ✅ Simple, working headline. Remove this block entirely if your M3 version lacks it.
//                headline = { DateRangePickerDefaults.DateRangePickerHeadline(state = state) },
                headline = {
                    val zone = ZoneId.systemDefault()
                    fun fmt(m: Long?) = m?.let {
                        Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toString()
                    } ?: "—"
                    Text("${fmt(state.selectedStartDateMillis)} → ${fmt(state.selectedEndDateMillis)}")
                }
//                showModeToggle = true
            )
        }
    }
}

/** Convert picker millis to LOCAL start-of-day inclusive and next-day start exclusive. */
private fun normalizeToLocalDayBounds(
    startMillis: Long,
    endMillis: Long
): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val sLd = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
    val eLd = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
    val startOfDay = ZonedDateTime.of(sLd.year, sLd.monthValue, sLd.dayOfMonth, 0, 0, 0, 0, zone)
    val endNextDay = ZonedDateTime.of(eLd.year, eLd.monthValue, eLd.dayOfMonth, 0, 0, 0, 0, zone).plusDays(1)
    return startOfDay.toInstant().toEpochMilli() to endNextDay.toInstant().toEpochMilli()
}


