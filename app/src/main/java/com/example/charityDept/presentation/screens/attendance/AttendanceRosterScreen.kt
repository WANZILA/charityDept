package com.example.charityDept.presentation.screens.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

// Paging compose imports
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import com.example.charityDept.presentation.components.common.ProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceRosterScreen(
    eventId: String,
    adminId: String,
    navigateUp: () -> Unit,
    vm: AttendanceRosterViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val bulkBusy by vm.bulkMode.collectAsStateWithLifecycle()
    val authUi by authVM.ui.collectAsStateWithLifecycle()

    val isDone = ui.eventStatus == com.example.charityDept.data.model.EventStatus.DONE

    val pagedChildren: LazyPagingItems<com.example.charityDept.data.model.Child> =
        vm.childrenPaging.collectAsLazyPagingItems()
    val searchCount by vm.searchCount.collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(eventId) { vm.load(eventId) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is AttendanceRosterViewModel.UiEvent.Saved -> {
                    snackbarHostState.showSnackbar(if (ev.pendingSync) "Saved (pending sync)" else "Saved")
                }
            }
        }
    }

    var query by rememberSaveable { mutableStateOf("") }
    val visible = ui.children
    val total = visible.size
    val presentCount = visible.count { it.present }
    val absentCount = total - presentCount

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Attendance")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { /* unchanged */ }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = ui.eventTitle ?: "Loading…",
                        style = MaterialTheme.typography.displaySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDate(ui.eventDate),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    vm.onSearchQueryChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Search children…") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                )
            )

            if (total > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                        label = { Text("P: $presentCount") }
                    )
                    AssistChip(
                        onClick = {},
                        leadingIcon = { Icon(Icons.Outlined.Circle, null) },
                        label = { Text("A: $absentCount") }
                    )
                    AssistChip(onClick = {}, label = { Text("All: $total") })
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    ui.error != null -> Text(
                        text = ui.error ?: "Error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    pagedChildren.itemCount > 0 -> {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pagedChildren.itemCount) { index ->
                                val child = pagedChildren[index]
                                if (child != null) {
                                    val rc = ui.children.firstOrNull { it.child.childId == child.childId }
                                        ?: RosterChild(child = child, attendance = null, present = false)

                                    AttendanceRow(
                                        rosterChild = rc,
                                        onToggle = { vm.toggleAttendance(eventId, rc, adminId) },
                                        onNotesChange = { notes -> vm.updateNotes(eventId, rc, adminId, notes) },
                                        hideToggle = isDone
                                    )
                                } else {
                                    ElevatedCard(Modifier.fillMaxWidth()) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .padding(12.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) { Text("Loading…") }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                    visible.isEmpty() && query.isNotBlank() ->
                        Text("No matches for “$query”.", Modifier.align(Alignment.Center))
                    visible.isEmpty() ->
                        Text("No children to show.", Modifier.align(Alignment.Center))
                    else -> {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                visible,
                                key = { "${it.child.childId}:${it.attendance?.attendanceId ?: ""}" },
                                contentType = { "row" }
                            ) { rc ->
                                AttendanceRow(
                                    rosterChild = rc,
                                    onToggle = { vm.toggleAttendance(eventId, rc, adminId) },
                                    onNotesChange = { notes -> vm.updateNotes(eventId, rc, adminId, notes) },
                                    hideToggle = isDone
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceRow(
    rosterChild: RosterChild,
    onToggle: () -> Unit,
    onNotesChange: (String) -> Unit,
    hideToggle: Boolean = false
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isEditing by remember { mutableStateOf(false) }

    var localNotes by rememberSaveable(rosterChild.child.childId) {
        mutableStateOf(rosterChild.attendance?.notes.orEmpty())
    }

    // editor starts open only if Absent and NO note
    var showNotes by remember {
        mutableStateOf(!rosterChild.present && rosterChild.attendance?.notes.orEmpty().isBlank())
    }

    LaunchedEffect(rosterChild.attendance?.notes) {
        if (!isEditing) localNotes = rosterChild.attendance?.notes.orEmpty()
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            )
            {
                ProfileAvatar(
                    profileImageLocalPath = rosterChild.child.profileImageLocalPath,
                    profileImg = "",
//                    profileImg = rosterChild.child.profileImg
                )

                Column(modifier = Modifier.weight(1f)) {
                    val fullName = listOf(
                        rosterChild.child.fName.trim(),
                        rosterChild.child.lName.trim()
                    ).filter { it.isNotBlank() }.joinToString(" ")

                    Text(
                        text = fullName.ifBlank { "Unnamed Child" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val line2 = buildString {
                        if (rosterChild.child.street.isNotBlank()) {
                            append("Street: ${rosterChild.child.street}")
                        }
                    }

                    if (line2.isNotBlank()) {
                        Text(
                            text = line2,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                FilledTonalButton(
                    onClick = {
                        if (!hideToggle) {
                            // if currently Present, next becomes Absent
                            val goingToAbsent = rosterChild.present
                            onToggle()
                            showNotes = goingToAbsent && localNotes.isBlank()
                        }
                    },
                    enabled = !hideToggle,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (rosterChild.present) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Present")
                        Spacer(Modifier.width(6.dp)); Text("Present")
                    } else {
                        Icon(Icons.Outlined.Circle, contentDescription = "Absent")
                        Spacer(Modifier.width(6.dp)); Text("Absent")
                    }
                }
            }
            // Read-only note display when Absent (ACTIVE and DONE)
            if (!rosterChild.present) {
                val note = rosterChild.attendance?.notes.orEmpty().trim()
                if (note.isNotBlank() && (!showNotes || hideToggle)) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Editable note only when ACTIVE (not DONE) and Absent
            if (!hideToggle && !rosterChild.present) {
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { showNotes = !showNotes }) {
                        Text(
                            if (showNotes) "Hide note"
                            else if (localNotes.isBlank()) "Add note"
                            else "Edit note"
                        )
                    }
                }

                if (showNotes) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = localNotes,
                        onValueChange = { localNotes = it },
                        placeholder = { Text("Reason for absence…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                val lostFocus = !it.isFocused && isEditing
                                isEditing = it.isFocused
                                if (lostFocus) {
                                    onNotesChange(localNotes)
                                    showNotes = false
                                }
                            },
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onNotesChange(localNotes)
                                keyboardController?.hide()
                                showNotes = false
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        )
                    )
                }
            }
        }
    }
}

private fun formatDate(ts: Timestamp): String = dayFmt.format(ts.toDate())
private val dayFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

