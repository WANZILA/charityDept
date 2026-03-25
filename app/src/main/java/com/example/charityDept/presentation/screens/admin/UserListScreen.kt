package com.example.charityDept.presentation.screens.admin

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.example.charityDept.data.model.user
import com.example.charityDept.data.model.UserProfile
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
//import com.example.charityDept.presentation.viewModels.Users.UsersListViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserListScreen(
    toUserForm: () -> Unit,
    navigateUp: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onClearFilter: () -> Unit,            // clear School/Skilling/None filter
    vm: UserListViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val authUi by authVM.ui.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }

    var showConfirmClear by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(ui.error) {
        ui.error?.let { Log.d("UsersListScreen", "Error: $it") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Users")

                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            Icons.Filled.ArrowCircleLeft, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.secondary
                            )
                    }
                },
                actions = {
                    if (ui.userRoleFilter != null || ui.disabledFilter != null 
                    ) {
                        IconButton(onClick = { showConfirmClear = true }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear Filter")
                        }
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    if(authUi.perms.canCreateChild){
                        IconButton(onClick = toUserForm) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add")
                        }
                    }
                }
            )
        }
    ) { inner ->

        // Confirm dialog (outside top bar)
        if (showConfirmClear) {
            AlertDialog(
                onDismissRequest = { showConfirmClear = false },
                title = { Text("Clear  preference?") },
                text  = { Text("This will remove the current filter and show all users.") },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmClear = false
                        onClearFilter()
                        scope.launch { snackbarHostState.showSnackbar("Filter cleared") }
                    }) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmClear = false }) { Text("Cancel") }
                }
            )
        }

        Column(Modifier.fillMaxSize().padding(inner)) {

            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                    vm.onSearchQueryChange(it)          // search remains client-side
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Search by name…") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                )
            )
            // Under the search field (after OutlinedTextField)
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Users: ${ui.filtered} / ${ui.total}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }


            // --- Active filter chips (wraps automatically) ---
            if (ui.userRoleFilter != null || ui.disabledFilter != null 
            ) {
                // If you added FlowRow earlier, keep it; otherwise keep Row. FlowRow example:
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ui.userRoleFilter?.let {
                        AssistChip(onClick = {}, label = { Text("Role: $it (${ui.filtered})") })
                    }
      

                    ui.disabledFilter?.let { v ->
                        AssistChip(onClick = {}, label = { Text("Sponsored: $v (${ui.filtered})") })
                    }
                   
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    ui.error != null -> Text(
                        "Failed to load: ${ui.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    ui.users.isEmpty() -> Text("No matches", modifier = Modifier.align(Alignment.Center))
                    else -> UsersList(items = ui.users, onUserClick = onUserClick)
                }
            }
        }
    }
}

@Composable
private fun UsersList(
    items: List<UserProfile>,
    onUserClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.uid }) { user ->
            UserRow(user = user, onClick = { onUserClick(user.uid) })
        }
    }
}

@Composable
private fun UserRow(
    user: UserProfile,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
//                text = "${user.fName} ${user.lName}".trim(),
                text = "${user.displayName}".trim(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
//                text = "Role: ${user.updatedAt.asHuman()}",
                text = "Role: ${user.userRole}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** Pretty-print a Firestore Timestamp, e.g. "29 Aug 2025 • 14:05". */
private fun Timestamp.asHuman(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    return sdf.format(toDate())
}


