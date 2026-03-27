package com.example.charityDept.presentation.screens.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.AssignedRole
import com.example.charityDept.presentation.components.action.CharityDeptAppTopBar
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
import com.example.charityDept.presentation.viewModels.users.UsersDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersDashboardScreen(
    toUsersList: () -> Unit,
    toAddUser: () -> Unit,
    toUsersByRole: (AssignedRole) -> Unit,
    toActiveUsers: () -> Unit,
    toDisabledUsers:() -> Unit,
    vm: UsersDashboardViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    val authUi by authVM.ui.collectAsStateWithLifecycle()
    val locked by vm.locked.collectAsStateWithLifecycle()


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor   = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CharityDeptAppTopBar(
                canNavigateBack = false,
                navigateUp = { /* no-op */ },
            )
        },
        // If you want a FAB later, uncomment and wire toAddUser just like your children screen
        // floatingActionButton = {
        //     FloatingActionButton(
        //         onClick = toAddUser,
        //         containerColor = MaterialTheme.colorScheme.secondary,
        //         contentColor   = MaterialTheme.colorScheme.onSecondary
        //     ) { Icon(Icons.Default.Add, contentDescription = "Add") }
        // }
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
                // KPI Cards (adjust names to match your UsersDashboardUiState)
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Total Users", ui.total.toString(), Modifier.weight(1f))
//                        StatCard("Active", ui.active.toString(), Modifier.weight(1f))
                        StatCard(
                            title = "Active",
                            value = "View: ${ui.active}",
                            modifier = Modifier.weight(1f),
                            onClick = toActiveUsers
                        )
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                        StatCard("Disabled", ui.disabled.toString(), Modifier.weight(1f))
                        StatCard(
                            title = "Disabled",
                            value = "View: ${ui.disabled}",
                            modifier = Modifier.weight(1f),
                            onClick = toDisabledUsers
                        )
                        StatCard("Locked", locked.totalLocked.toString(), Modifier.weight(1f))
//                        StatCard("Admins", (ui.assignedRoleDist[AssignedRole.ADMIN] ?: 0).toString(), Modifier.weight(1f))
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (authUi.perms.canCreateUser) {
                            StatCard(
                                title = "Users",
                                value = "Add New",
                                modifier = Modifier.weight(1f),
                                onClick = toAddUser
                            )
                        }
                        StatCard(
                            title = "Users",
                            value = "View All",
                            modifier = Modifier.weight(1f),
                            onClick = toUsersList
                        )
                    }
                }

                // Role distribution
                item {
                    SectionCard(title = "Role Distribution") {
                        val total = ui.assignedRoleDist.values.sum().coerceAtLeast(1)
                        AssignedRole.entries.forEach { role ->
                            val count = ui.assignedRoleDist[role] ?: 0
                            RoleRow(
                                label = role.name,
                                count = count,
                                total = total,
                                onClick = { toUsersByRole(role) }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                item {
                    LockedAccountsSection(
                        locked = locked,
                        canUnlock = authUi.assignedRoles.contains(AssignedRole.ADMIN),
                        onUnlock = { emailLower -> vm.adminUnlock(emailLower) }
                    )
                }



            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier, sub: String = "") {
    ElevatedCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// Clickable overload (kept identical to your pattern)
@Composable
fun StatCard(
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
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun RoleRow(
    label: String,
    count: Int,
    total: Int,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        enabled = true
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text("View: $count", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = (count / total.toFloat()).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

@Composable
private fun LockedAccountsSection(
    locked: com.example.charityDept.presentation.viewModels.users.LockedListUiState,
    canUnlock: Boolean,
    onUnlock: (String) -> Unit
) {
    SectionCard(title = "Locked Accounts (${locked.totalLocked})") {
        when {
            locked.loading -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
            locked.error != null -> {
                Text(
                    "Error: ${locked.error}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            locked.locked.isEmpty() -> {
                Text("No locked accounts ", style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    locked.locked.forEach { row ->
                        LockedAccountRow(
                            emailLower = row.emailLower,
                            count = row.count,
                            role = row.userRole,
                            disabled = row.disabled,
                            canUnlock = canUnlock,
                            onUnlock = onUnlock
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedAccountRow(
    emailLower: String,
    count: Int,
    role: AssignedRole?,
    disabled: Boolean?,
    canUnlock: Boolean,
    onUnlock: (String) -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(emailLower, style = MaterialTheme.typography.bodyLarge)
                Text("x$count", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                Text(
//                    "Role: ${role?.name ?: "—"}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
                Text(
                    "Disabled: ${disabled?.toString() ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = canUnlock,
                    onClick = { showConfirm = true },

                ) {
                    Text(if (canUnlock) "Unlock" else "No Access")
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Unlock account?") },
            text = {
                Column {
                    Text("This will reset failed attempts for:")
                    Spacer(Modifier.height(6.dp))
                    Text(emailLower, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Current attempts: $count")
                    if (disabled == true) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Note: This does not change the user’s disabled flag.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onUnlock(emailLower)
                }) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}


/* ---------- Expected ViewModel/UI state sketch (for reference) ----------
data class UsersDashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val total: Int = 0,
    val active: Int = 0,
    val disabled: Int = 0,
    val roleDist: Map<Role, Int> = emptyMap()
)
-------------------------------------------------------------------------- */

