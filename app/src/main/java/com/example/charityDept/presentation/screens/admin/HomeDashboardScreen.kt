package com.example.charityDept.presentation.screens.admin

import android.content.pm.ApplicationInfo
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.charityDept.R
import com.example.charityDept.data.model.Event
import com.example.charityDept.presentation.components.action.CharityDeptAppTopBar
//import com.example.charityDept.presentation.screens.admin.StatCard
//import com.example.charityDept.presentation.screens.children.StatCard
import com.google.firebase.Firebase
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.crashlytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    toChildrenList: () -> Unit,
    toEventsList: () -> Unit,
    toAccepted: () -> Unit,
    toYetAccept: () -> Unit,
    toResettled: () -> Unit,
    toBeResettled: () -> Unit,
    toFamilyDashboard: () -> Unit,
    vm: HomeDashboardViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor   = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CharityDeptAppTopBar(
                canNavigateBack = false,
                navigateUp = { /* no-op on home */ },
                txtLabel = stringResource(R.string.home),
            )
        },
    ) { inner ->
        when {
            ui.loading -> Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            ui.error != null -> Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("Error: ${ui.error}", color = MaterialTheme.colorScheme.error) }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // KPI rows
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Total", ui.childrenTotal.toString(), Modifier.weight(1f), onClick = toChildrenList)
                        StatCard("New This Month", ui.childrenNewThisMonth.toString(), Modifier.weight(1f))
                       }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Graduated", ui.childrenGraduated.toString(), Modifier.weight(1f))
//                        StatCard("Sponsored", ui.sponsored.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Accepted Christ", ui.acceptedChrist.toString(),  onClick = toAccepted,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard("Yet to Accept", ui.yetToAcceptChrist.toString(), onClick = toYetAccept,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                         StatCard("Resettled", ui.resettled.toString(), onClick = toResettled , modifier = Modifier.weight(1f))
                         StatCard("To Resettle", ui.toBeResettled.toString(),onClick = toBeResettled , modifier =  Modifier.weight(1f))

//                         StatCard("Events Today", ui.eventsToday.toString(), Modifier.weight(1f), onClick = toEventsList)

                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            "Families",
                            ui.familiesTotal.toString(),
                            modifier = Modifier.weight(1f),
                            onClick = toFamilyDashboard
                        )
                        StatCard(
                            "Family Members",
                            ui.familyMembersTotal.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
//                item {
//                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                        StatCard("Active Now", ui.eventsActiveNow.toString(), Modifier.weight(1f))
//                    }
//                }
//item{
//    CrashlyticsTestScreen()
//}

                // Happening Today
                item {
                    SectionCard("Happening Today") {
                        if (ui.happeningToday.isEmpty()) {
                            Text("No events today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ui.happeningToday.forEach { e ->
                                    EventRowSmall(e, onOpen = toEventsList)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------- Reusable bits ---------- */

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
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EventRowSmall(event: Event, onOpen: () -> Unit) {
    ElevatedCard(onClick = onOpen) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    event.title.ifBlank { "Untitled Event" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    event.eventDate.asTimeAndDate(), // Timestamp → formatted string
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
            }
            AssistChip(onClick = onOpen, label = { Text(event.eventStatus.name.first().toString()) })
        }
    }
}

/* ---------- Timestamp formatting ---------- */

private fun Timestamp.asTimeAndDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    return sdf.format(this.toDate()) // uses the device's locale/timezone
}

// (Optional: keep the Long version if you still use it elsewhere)
private fun Long.asTimeAndDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}


@Composable
fun CrashlyticsTestScreen() {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Crashlytics Diagnostics", style = MaterialTheme.typography.titleLarge)

        Button(onClick = {
            Firebase.crashlytics.setCustomKey("screen", "CrashlyticsTestScreen")
//            Firebase.crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            Firebase.crashlytics.log("About to throw a test RuntimeException")
                        // Derive build type without BuildConfig
                        val appInfo = ctx.applicationContext.applicationInfo
                        val isDebug = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                        val buildType = if (isDebug) "debug" else "release"
                        Firebase.crashlytics.setCustomKey("build_type", buildType)
            // FATAL (app will crash)
            throw RuntimeException("🔥 Test crash from CrashlyticsTestScreen")
        }) { Text("Force FATAL crash") }

        Button(onClick = {
            val e = IllegalStateException("🤕 Test NON-FATAL exception")
            Firebase.crashlytics.recordException(e)
            Toast.makeText(ctx, "Recorded non-fatal", Toast.LENGTH_SHORT).show()
        }) { Text("Record NON-FATAL") }

        Button(onClick = {
            Firebase.crashlytics.log("Breadcrumb: tapped breadcrumb button")
            Toast.makeText(ctx, "Breadcrumb logged", Toast.LENGTH_SHORT).show()
        }) { Text("Log breadcrumb") }
    }
}
