package com.example.charityDept.presentation.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.charityDept.data.model.AssignedRole

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserFormScreen(
    uidArg: String?,                 // unused for create
    toList: () -> Unit,    // if you want to use it
    toDetail: (String) -> Unit,
    navigateUp: () -> Unit,
    vm: UserFormViewModel = hiltViewModel(),
//    onSuccess: ((String) -> Unit)? = null
) {
    val ui = vm.ui
    val scroll = rememberScrollState()

    val isCreate = uidArg.isNullOrBlank()
    val title = if (isCreate) "Register User" else "Edit User"

    LaunchedEffect(uidArg) {
        if (uidArg.isNullOrBlank()) "no need" else vm.loadForEdit(uidArg)
    }
    LaunchedEffect(ui.success, ui.createdUid) {
        if (ui.success && ui.createdUid != null) {
//            onSuccess?.invoke(ui.createdUid)
            toDetail(ui.createdUid) // <-- actually call it
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (uidArg.isNullOrBlank()) Text("Register User") else Text("Edit User")
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Default.ArrowCircleLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = toList) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = ui.email,
                onValueChange = vm::updateEmail,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                readOnly = !isCreate
            )

//            if (isCreate) {
//                OutlinedTextField(
//                    value = ui.password,
//                    onValueChange = vm::updatePassword,
//                    label = { Text("Password (min 6 chars)") },
//                    singleLine = true,
//                    visualTransformation = PasswordVisualTransformation(),
//                    modifier = Modifier.fillMaxWidth(),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
//                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
//                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
//                    )
//                )
//            }

            if (!isCreate) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Send password reset email on Update", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = ui.sendResetLink,
                        onCheckedChange = { vm.setSendResetLink(it) }
                    )
                }
            }


            OutlinedTextField(
                value = ui.displayName,
                onValueChange = vm::updateDisplayName,
                label = { Text("Display name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                )
            )

            Text("Role", style = MaterialTheme.typography.titleMedium)

            // Exclusive selection chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val all = listOf(AssignedRole.ADMIN, AssignedRole.LEADER,  AssignedRole.VIEWER , AssignedRole.SPONSOR, AssignedRole.VOLUNTEER)
                all.forEach { role ->
                    FilterChip(
                        selected = ui.selectedAssignedRole == role,
                        onClick = { vm.setRole(role) }, // <-- sets single role
                        label = { Text(role.name) }
                    )
                }
            }

            // Status Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Active", style = MaterialTheme.typography.titleMedium)

                Switch(
                    checked = !ui.disabled,             // default true, since disabled = false
                    onCheckedChange = { checked ->
                        vm.setDisabled(!checked)       // flip stored disabled flag
                    }
                )
            }

            val statusText = if (ui.disabled) "User is disabled" else "User is active"
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


            if (ui.error != null) {
                Text(
                    text = ui.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))



            Button(
                onClick = {
                    if (isCreate) vm.submit(isOnline = true) else vm.saveEdits()
                },
                enabled = !ui.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (ui.loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(if (isCreate) "Saving…" else "Updating…")
                } else {
                    Text(if (isCreate) "Save" else "Update")
                }
            }



        }
    }
}

