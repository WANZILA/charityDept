package com.example.charityDept.presentation.screens.widgets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


@Composable
fun DeleteIconWithConfirm(
    label: String,              // e.g. "child John Doe", "event Sunday Service", "attendance record"
    deleting: Boolean,
    onDelete: () -> Unit,
    icon: ImageVector = Icons.Outlined.Delete,
    contentDescription: String = "Delete"
) {
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = { showConfirm = true }, enabled = !deleting) {
        Icon(icon, contentDescription)
    }

    ConfirmDialog(
        visible = showConfirm,
        onDismiss = { showConfirm = false },
        onConfirm = {
            showConfirm = false
            onDelete()
        },
        title = "Delete",
        message = "Are you sure you want to delete $label? This cannot be undone.",
        confirming = deleting,
        destructive = true
    )
}

@Composable
fun ConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "Delete",
    dismissText: String = "Cancel",
    confirming: Boolean = false,
    destructive: Boolean = true
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = { Text(message) },
        confirmButton = {
            TextButton(
                enabled = !confirming,
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                if (confirming) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(enabled = !confirming, onClick = onDismiss) { Text(dismissText) }
        }
    )
}

