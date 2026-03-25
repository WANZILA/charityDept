package com.example.charityDept.presentation.screens.widgets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
//import androidx.compose.foundation.layout.weight

@Composable
fun PendingSyncLabel(
    isDirty: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Pending sync"
) {
    if (!isDirty) return
    Icon(
                            Icons.Filled.CloudUpload, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.secondary
                            )
//    Text(
//        text = label,
//        style = MaterialTheme.typography.labelSmall,
//        color = MaterialTheme.colorScheme.tertiary,
//        modifier = modifier
//    )
}
