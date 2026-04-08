package com.example.charityDept.presentation.screens.children.childDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.presentation.components.common.ProfileImageSection
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDashboardScreen(
    childIdArg: String,
    navigateUp: () -> Unit,
    toEditChild: (childId: String) -> Unit,
    toEvents: (childId: String) -> Unit,
    toAttendance: (childId: String) -> Unit,
    toQa: (childId: String) -> Unit,
    toObservations: (childId: String) -> Unit,
    vm: ChildDashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(childIdArg) {
        if (childIdArg.isNotBlank()) vm.setChildId(childIdArg)
    }

    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Child Dashboard") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.loading -> LoadingSpinner()

                state.error != null -> ErrorMessage(
                    message = state.error ?: "An error occurred",
                    onRetry = { vm.setChildId(state.childId.ifBlank { childIdArg }) }
                )

                else -> {
                    val child = state.child
                    val cid = state.childId.ifBlank { childIdArg }

                    val name = child?.fullName().orEmpty().ifBlank { "Child" }
                    val attendanceValue = "${state.attendancePresent}/${state.attendanceTotal}"

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // ─── HEADER ───────────────────────────────
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically,
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Box(
//                                modifier = Modifier
//                                    .size(44.dp)
//                                    .clip(CircleShape)
//                                    .background(MaterialTheme.colorScheme.secondary),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Text(
//                                    text = initials,
//                                    style = MaterialTheme.typography.titleLarge,
//                                    color = MaterialTheme.colorScheme.onSecondary
//                                )
//                            }
                            ProfileImageSection(
                                profileImageLocalPath = state.child?.profileImageLocalPath.orEmpty(),
                                profileImg = "",
//                                profileImg = state.child?.profileImg.orEmpty(),
                                displayName = name,
                                showImageSourceText = false,
                            )

//                            Spacer(Modifier.width(12.dp))

//                            Column {
//                                Text(
//                                    text = name,
//                                    style = MaterialTheme.typography.titleLarge,
//                                    fontWeight = FontWeight.SemiBold
//                                )
//                                Text(
//                                    text = cid,
//                                    style = MaterialTheme.typography.titleSmall,
//                                    fontWeight = FontWeight.SemiBold
//                                )
//                            }
//                        }

                        Spacer(Modifier.height(16.dp))

                        // ─── ROW 1 ───────────────────────────────
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashboardCardLike(
                                title = "👤 Profile / Edit",
                                value = "",
                                modifier = Modifier.weight(1f)
                            ) { toEditChild(cid) }

                            DashboardCardLike(
                                title = "📅 Events",
                                value = state.eventsCount.toString(),
                                modifier = Modifier.weight(1f)
                            ) { toEvents(cid) }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashboardCardLike(
                                title = "✅ Attendance",
                                value = attendanceValue,
                                modifier = Modifier.weight(1f)
                            ) { toAttendance(cid) }

                        }

                        Spacer(Modifier.height(8.dp))
                            // ─── ROW 2 ───────────────────────────────
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            DashboardCardLike(
                                title = "📝 Q&A",
                                value = "",
                                modifier = Modifier.weight(1f)
                            ) { toQa(cid) }
                            DashboardCardLike(
                                title = "👀 Observations",
                                value = "",
                                modifier = Modifier.weight(1f)
                            ) { toObservations(cid) }

                        }

                        Spacer(Modifier.height(8.dp))

                        // ─── ROW 3 ───────────────────────────────
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {


                            DashboardCardLike(
                                title = "🕒 Last Updated",
                                value = state.lastUpdated?.toDate()?.toString().orEmpty(),
                                modifier = Modifier.weight(1f),
                                enabled = false
                            ) {}
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
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
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (value.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LoadingSpinner() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

