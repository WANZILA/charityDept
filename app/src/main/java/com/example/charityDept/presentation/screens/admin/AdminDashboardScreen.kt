package com.example.charityDept.presentation.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    toMigrationToolKit: () -> Unit,
    toUsersDashboard: () -> Unit,
    toQuestionBank: () -> Unit,
    toChildrenDashboard: () -> Unit,
    toEventsDashboard: () -> Unit,
    toAttendanceDashboard: () -> Unit,
    toReportsDashboard: () -> Unit,
    vm: AdminDashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    canNavigateBack: Boolean = false,
    navigateUp: (() -> Unit)? = null
) {
    val ui by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    // Keep minimal — only show if caller wires it
                    if (canNavigateBack && navigateUp != null) {
                        // You can swap this with your existing ZionKids topbar component if you have one
                        Text(
                            text = "Back",
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .clickable { navigateUp() },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->

        Column(
            modifier = modifier
                .padding(pad)
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                ui.isLoading -> LoadingSpinner()

                ui.error != null -> ErrorMessage(
                    message = ui.error ?: "An error occurred",
                    onRetry = vm::refresh
                )

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // --- Row 1 (Admin actions) ---
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZionDashboardCard(
                                title = "🚚 Migration ToolKit",
                                value = "",
                                modifier = Modifier.weight(1f),
                                onClick = toMigrationToolKit
                            )

                            ZionDashboardCard(
                                title = "👥 Users Dashboard",
                                value = "",
                                modifier = Modifier.weight(1f),
                                onClick = toUsersDashboard
                            )
                        }

                        // --- Row 2 (Question Bank + Reports) ---
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZionDashboardCard(
                                title = "🧠 Question Bank",
                                value = "${ui.questionsActive}/${ui.questionsTotal}",
                                modifier = Modifier.weight(1f),
                                onClick = toQuestionBank
                            )

                            ZionDashboardCard(
                                title = "📊 Reports",
                                value = "",
                                modifier = Modifier.weight(1f),
                                onClick = toReportsDashboard
                            )
                        }

                        // --- Row 3 (Core modules) ---
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZionDashboardCard(
                                title = "👧 Children",
                                value = "${ui.childrenTotal}",
                                modifier = Modifier.weight(1f),
                                onClick = toChildrenDashboard
                            )

                            ZionDashboardCard(
                                title = "📅 Events",
                                value = "${ui.eventsTotal}",
                                modifier = Modifier.weight(1f),
                                onClick = toEventsDashboard
                            )
                        }

                        // --- Row 4 (Attendance + Assessments) ---
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZionDashboardCard(
                                title = "✅ Attendance",
                                value = "${ui.attendanceTotal}",
                                modifier = Modifier.weight(1f),
                                onClick = toAttendanceDashboard
                            )

                            ZionDashboardCard(
                                title = "📝 Assessments",
                                value = "${ui.assessmentSessionsTotal} sessions",
                                modifier = Modifier.weight(1f),
                                onClick = { /* optional: later route to assessments admin */ }
                            )
                        }

                        // --- Row 5 (Sync health) ---
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZionDashboardCard(
                                title = "☁️ Pending Sync",
                                value = "${ui.dirtyTotal}",
                                modifier = Modifier.weight(1f),
                                onClick = { /* optional: later screen showing dirty rows */ }
                            )

                            ZionDashboardCard(
                                title = "🗑️ Deleted (pending)",
                                value = "${ui.deletedPendingTotal}",
                                modifier = Modifier.weight(1f),
                                onClick = { /* optional: later screen */ }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (ui.isEmpty) {
                            Text(
                                text = "No data yet — add children/events, seed Question Bank, and start recording attendance & assessments.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Tip: Keep Question Bank clean (category/subCategory/indexNum) to make assessment seeding consistent.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ZionDashboardCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            if (value.isNotBlank()) {
                Text(value, style = MaterialTheme.typography.headlineSmall)
            } else {
                Text("Open", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LoadingSpinner() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

