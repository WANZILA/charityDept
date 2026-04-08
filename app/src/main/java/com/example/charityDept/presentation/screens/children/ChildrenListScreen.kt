package com.example.charityDept.presentation.screens.children

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
import androidx.compose.foundation.layout.width
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
import com.example.charityDept.data.model.Child
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
/// ADDED (imports)
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import com.example.charityDept.core.utils.DatesUtils.asHuman
import com.example.charityDept.presentation.components.common.ProfileAvatar

//import com.example.charityDept.data.local.projection.ChildRow as UiChildRow  // alias to avoid name clash


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChildrenListScreen(
    toChildForm: () -> Unit,
    navigateUp: () -> Unit,
    toChildDashboard: (String) -> Unit = {},
    onClearFilter: () -> Unit,            // clear School/Skilling/None filter
    vm: ChildrenListViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    /// ADDED — collect Room paging items
//    val pagedRows = vm.paging.collectAsLazyPagingItems()
    val pagedRows = vm.paging.collectAsLazyPagingItems()
//    val pagedRows = vm.paging.collectAsLazyPagingItems()

    val authUi by authVM.ui.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }

    var showConfirmClear by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()



    LaunchedEffect(ui.error) {
        ui.error?.let { Log.d("ChildrenListScreen", "Error: $it") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Not Graduated")

//                        if (ui.activeFilter != null) {
//                            AssistChip(
//                                onClick = { /* no-op */ },
//                                label = { Text("Filtered: ${ui.activeFilter}") }
//                            )
//                            Spacer(Modifier.width(6.dp))
//                        }
//                        if (ui.isOffline) {
//                            AssistChip(onClick = {}, label = { Text("Offline") })
//                            Spacer(Modifier.width(6.dp))
//                        }
//                        if (ui.isSyncing) {
//                            AssistChip(onClick = {}, label = { Text("Syncing…") })
//                            Spacer(Modifier.width(6.dp))
//                        }
 //                       ui.lastRefreshed?.let {
 //                           AssistChip(onClick = {}, label = { Text("Refreshed ${it.asHuman()}") })
 //                       }
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
                    val activeStreet = ui.activeFilterStreet?.takeIf { it.isNotBlank() }
                    val activeRegion = ui.activeFilterRegion?.takeIf { it.isNotBlank() }

                    if (activeStreet != null || activeRegion != null || ui.activeFilter != null
                        || ui.activeFilterSponsored != null || ui.activeFilterGraduated != null
                        || ui.activeFilterClassGroup != null || ui.activeFilterAcceptedJesus != null
                        || ui.activeFilterResettled != null || ui.activeFilterDobVerified != null
                    ) {
                        IconButton(onClick = { showConfirmClear = true }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear Filter")
                        }
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    if(authUi.perms.canCreateChild){
                        IconButton(onClick = toChildForm) {
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
                title = { Text("Clear filters?") },
                text  = { Text("This will remove the current filters and show the full children list again.") },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmClear = false
                        onClearFilter()
                        scope.launch { snackbarHostState.showSnackbar("Filters cleared") }
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
                    text = "Children: ${ui.filtered} / ${ui.total}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }


            val activeStreet = ui.activeFilterStreet?.takeIf { it.isNotBlank() }
            val activeRegion = ui.activeFilterRegion?.takeIf { it.isNotBlank() }

            if (activeStreet != null || activeRegion != null || ui.activeFilter != null
                || ui.activeFilterSponsored != null || ui.activeFilterGraduated != null
                || ui.activeFilterClassGroup != null || ui.activeFilterAcceptedJesus != null
                || ui.activeFilterResettled != null || ui.activeFilterDobVerified != null
            ) {
                // If you added FlowRow earlier, keep it; otherwise keep Row. FlowRow example:
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ui.activeFilter?.let {
                        AssistChip(onClick = {}, label = { Text("Edu: $it (${ui.filtered})") })
                    }
                    activeStreet?.let {
                        AssistChip(onClick = {}, label = { Text("Street: $it (${ui.filtered})") })
                    }
                    activeRegion?.let {
                        AssistChip(onClick = {}, label = { Text("Region: $it (${ui.filtered})") })
                    }

                    ui.activeFilterSponsored?.let { v ->
                        AssistChip(onClick = {}, label = { Text("Sponsored: $v (${ui.filtered})") })
                    }
                    ui.activeFilterGraduated?.let { v ->
                        AssistChip(onClick = {}, label = { Text("Graduated: $v (${ui.filtered})") })
                    }
                    ui.activeFilterClassGroup?.let { v ->
                        AssistChip(onClick = {}, label = { Text("Class: $v (${ui.filtered})") })
                    }
                    ui.activeFilterAcceptedJesus?.let { v ->
                        AssistChip(onClick = {}, label = { Text("Accepted: $v (${ui.filtered})") })
                    }
                    ui.activeFilterResettled?.let { v ->
                        AssistChip(onClick = {}, label = { Text("Resettled: $v (${ui.filtered})") })
                    }
                    ui.activeFilterDobVerified?.let { v ->
                        AssistChip(onClick = {}, label = { Text("DOB Verified: $v (${ui.filtered})") })
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.loading && pagedRows.itemCount == 0 ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    ui.error != null -> Text(
                        "Failed to load: ${ui.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    pagedRows.itemCount > 0 -> ChildrenPagingList(
                        items = pagedRows,
                        onChildClick = toChildDashboard
                    )
                    else -> Text("No matches", modifier = Modifier.align(Alignment.Center))
                }
            }
//            Box(Modifier.fillMaxSize()) {
//                when {
//                    ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
//                    ui.error != null -> Text(
//                        "Failed to load: ${ui.error}",
//                        color = MaterialTheme.colorScheme.error,
//                        modifier = Modifier.align(Alignment.Center)
//                    )
//                    ui.children.isEmpty() -> Text("No matches", modifier = Modifier.align(Alignment.Center))
//                    else -> ChildrenList(items = ui.children, toChildDashboard = toChildDashboard)
//                }
//            }
//            Box(Modifier.fillMaxSize()) {
//                when {
//                    ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
//                    ui.error != null -> Text(
//                        "Failed to load: ${ui.error}",
//                        color = MaterialTheme.colorScheme.error,
//                        modifier = Modifier.align(Alignment.Center)
//                    )
////                    ui.children.isEmpty() -> Text("No matches", modifier = Modifier.align(Alignment.Center))
////                    else -> ChildrenList(items = ui.children, toChildDashboard = toChildDashboard)
//                    /// CHANGED — prefer Room paging list for rendering
////                    pagedRows.itemCount > 0 -> ChildrenPagingList(
////                        items = pagedRows,
////                        toChildDashboard = toChildDashboard
////                    )
//                    else -> Text("No matches", modifier = Modifier.align(Alignment.Center))
//                }
//            }
        }
    }
}

/// ADDED

/// ADDED
//

@Composable
private fun ChildrenList(
    items: List<Child>,
    onChildClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.childId }) { child ->
            ChildRow(child = child, onClick = { onChildClick(child.childId) })
        }
    }
}

@Composable
private fun ChildRow(
    child: Child,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                profileImageLocalPath = child.profileImageLocalPath,
                profileImg = child.profileImg
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "${child.fName} ${child.lName}".trim(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
//                Text(
//                    text = "District: ${child.  updatedAt.asHuman()}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.outline
//                )
//                Text(
//                    text = "Updated: ${child.updatedAt.asHuman()}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.outline
//                )
            }
        }
    }
}
@Composable
private fun ChildrenPagingList(
    items: LazyPagingItems<Child>,
    onChildClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items.itemCount) { index ->
            val child = items[index]
            if (child != null) {
                ChildRow(child = child) { onChildClick(child.childId) }
            }
        }
        // (optional) simple footer spinner while loading next page
        item {
            if (items.loadState.append.endOfPaginationReached.not()) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

