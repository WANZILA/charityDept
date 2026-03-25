package com.example.charityDept.presentation.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.AssignedRole
import com.example.charityDept.data.model.UserProfile
//import com.example.charityDept.data.model.Child
import com.example.charityDept.presentation.screens.widgets.DeleteIconWithConfirm
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
//import com.example.charityDept.presentation.viewModels.children.UserDetailViewModel
import com.example.charityDept.presentation.viewModels.users.UserDetailViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collect
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    uidArg: String,
    toEdit: (String) -> Unit,
    toDashboard: () -> Unit,
    toList: () -> Unit,
    vm: UserDetailViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val authUI by authVM.ui.collectAsStateWithLifecycle()
    val canEditChild = authUI.perms.canEditChild
    val canDeleteChild= authUI.perms.canDeleteEvent
    // load once
    LaunchedEffect(uidArg) { vm.load(uidArg) }

//    LaunchedEffect(Unit) {
//        vm.events.collect { ev ->
//            when (ev) {
//                is UserDetailViewModel.Event.Deleted -> toList()
//                is UserDetailViewModel.Event.Error -> snackbarHostState.showSnackbar(ev.msg)
//            }
//        }
//    }
    LaunchedEffect(vm) {
        vm.events.collect { ev ->
            when (ev) {
                is UserDetailViewModel.Event.Deleted -> {
                    snackbarHostState.showSnackbar("User deleted")
                    toList()
                }
                is UserDetailViewModel.Event.Error -> {
                    snackbarHostState.showSnackbar(ev.msg)
                }
            }
        }
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(ui.user?.displayName?.trim().orEmpty().ifBlank { "User details" }) },
                navigationIcon = {
                    IconButton(onClick = toDashboard) {
                        Icon(
                            Icons.Filled.ArrowCircleLeft, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    val user = ui.user
                    if (user != null) {
                        if(canEditChild){
                            IconButton(onClick = { toEdit(user.uid) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                            }
                        }
                        IconButton(onClick = { vm.refresh(user.uid) }, enabled = !ui.deleting) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }
//                 Reusable delete for CHILD
                        if(canDeleteChild){
                            DeleteIconWithConfirm(
                                label = "user ${user.displayName}".trim(),
                                deleting = ui.deleting,
                                onDelete = {
                                    vm.softDeleteUser()
                                }
                            )
                        }

                        IconButton(onClick = toList) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Text(
                    "Error: ${ui.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                ui.user != null -> DetailsContent(ui.user!!)
            }
        }
    }


}

@Composable
private fun DetailsContent(user: UserProfile) {
    var openBasic by rememberSaveable { mutableStateOf(true) }
    var openBackground by rememberSaveable { mutableStateOf(false) }
    var openEducation by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CollapsibleSection("Basic Info", openBasic, { openBasic = !openBasic }) {
                Field("Display name", user.displayName?.ifBlank { "-" } ?: "-")
                Field("Email", user.email?.ifBlank { "-" } ?:"-" )
                Field("Role", user.userRole.toString())
                Field("Disabled", user.disabled.toString())
               }
        }

        item {
            CollapsibleSection("Roles ", openBackground, { openBackground = !openBackground }) {
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Role", style = MaterialTheme.typography.titleMedium)

                        AssignedRole.values().forEach { role ->
                            ListItem(
                                headlineContent = { Text(role.name) },
                                supportingContent = {
                                    when (role) {
                                        AssignedRole.ADMIN -> Text("Full access")
                                        AssignedRole.LEADER -> Text("Manage events & children")
                                        AssignedRole.VOLUNTEER -> Text("Register children and check attendance")
                                        AssignedRole.SPONSOR -> Text("View only (donor)")
                                        else -> Text("Views every thing only")
                                    }
                                },

                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }








    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            Divider()
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(160.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MemberBlock(
    idx: Int,
    first: String?, last: String?,
    relation: String,
    phoneA: String?, phoneB: String?
) {
    Text("Member $idx", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Field("Name", "${first ?: "-"} ${last ?: ""}".trim().ifBlank { "-" })
    Field("Relationship", relation)
    Field("Phone (A)", phoneA ?: "-")
    Field("Phone (B)", phoneB ?: "-")
}

private fun Timestamp.asHuman(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    return sdf.format(this.toDate())
}

