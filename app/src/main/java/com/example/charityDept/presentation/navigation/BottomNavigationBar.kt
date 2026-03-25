@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.charityDept.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.DoDisturbOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(
    navController: NavController,
    authUi: com.example.charityDept.presentation.viewModels.auth.AuthUiState
) {
    val canListUsers = authUi.perms.canListUsers
    val sessionUid = authUi.profile?.uid

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val primary = listOf(
        Screen.HomeDashboard,
        Screen.ChildrenDashboard,
        Screen.EventsDashboard
    )

    // ❌ no remember — build each recomposition so it can’t stick
    val overflow = buildList {

        if (canListUsers) {
//            add(Screen.Migration)
//            add(Screen.AdminUsers)
//            add(Screen.ReportsDashboard)
//            add(Screen.TechnicalSkillsDashboard)
//            add(Screen.StreetsDashboard)
            add(Screen.AdminDashboard)

        }
        add(Screen.AttendanceDashboard)
    }

    fun isSelected(route: String) =
        currentDestination?.hierarchy?.any { it.route == route } == true

    val overflowSelected =
        currentDestination?.hierarchy?.any { d -> overflow.any { it.route == d.route } } == true

    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(canListUsers) { if (!canListUsers && menuOpen) menuOpen = false }

    // ✅ re-mount on user switch + permission flip
    key(sessionUid, canListUsers) {
        NavigationBar {
            // Primary
            primary.forEach { screen ->
                val selected = isSelected(screen.route)
                Surface(
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20),
                    color = Color.Transparent,
                    tonalElevation = if (selected) 1.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (screen) {
                                is Screen.HomeDashboard -> Icons.Filled.Home
                                is Screen.ChildrenDashboard -> Icons.Filled.Person
                                is Screen.EventsDashboard -> Icons.Filled.AddTask
                                else -> Icons.Filled.AccountBalance
                            },
                            modifier = Modifier.size(24.dp),
                            contentDescription = screen.route,
                            tint = if (selected) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = screen.route.replaceFirstChar { it.uppercase() },
                            fontSize = 10.sp,
                            color = if (selected) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Overflow
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { menuOpen = true },
                    shape = RoundedCornerShape(20),
                    color = Color.Transparent,
                    tonalElevation = if (overflowSelected) 1.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
//                        // debug: remove later
//                        Text(
//                            text = "canListUsers=$canListUsers  roles=${authUi.roles.joinToString { it.name }}",
//                            fontSize = 9.sp,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )

                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            modifier = Modifier.size(24.dp),
                            contentDescription = "More",
                            tint = if (overflowSelected) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "More",
                            fontSize = 10.sp,
                            color = if (overflowSelected) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (overflowSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    overflow.forEach { screen ->
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = when (screen) {
                                        is Screen.AdminDashboard -> Icons.Filled.Person
//                                        is Screen.Migration -> Icons.Filled.Outbox
//                                        is Screen.AdminUsers -> Icons.Filled.Person
//                                        is Screen.ReportsDashboard -> Icons.Filled.Home
//                                        is Screen.StreetsDashboard -> Icons.Filled.AccountBalance
//                                        is Screen.TechnicalSkillsDashboard -> Icons.Filled.PanTool
                                        is Screen.AttendanceDashboard -> Icons.Filled.PeopleAlt
                                        else -> Icons.Filled.DoDisturbOff
                                    },
                                    contentDescription = null
                                )
                            },
                            text = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                menuOpen = false
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

