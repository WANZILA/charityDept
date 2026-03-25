package com.example.charityDept.presentation.screens.widgets

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BulkConfirmDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    message: String
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirm Action") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Yes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

