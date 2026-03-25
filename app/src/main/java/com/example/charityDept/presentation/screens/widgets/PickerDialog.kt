package com.example.charityDept.presentation.screens.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.charityDept.core.Utils.picker.PickerFeature
import com.example.charityDept.core.Utils.picker.PickerOption


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PickerDialog(
    title: String,
    feature: PickerFeature,
    onPicked: (PickerOption) -> Unit,
    onDismiss: () -> Unit
) {
    val query by feature.query.collectAsStateWithLifecycle()
    val filtered by feature.filtered.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = { feature.clearQuery(); onDismiss() },
        confirmButton = { /* picking an item acts as confirm */ },
        dismissButton = {
            TextButton(onClick = { feature.clearQuery(); onDismiss() }) {
                Text("Cancel")
            }
        },
        title = { Text(title) },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp) // keep room for buttons
            ) {
                // Scrollable content area
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    // Sticky search at top while results scroll
                    stickyHeader {
                        Surface(tonalElevation = 2.dp) {
                            AppTextField(
                                value = query,
                                onValueChange = feature::updateQuery,
                                label = "Search by name or ID",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                singleLine = true,
                                enabled = false
                            )
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "No results",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPicked(opt)
                                        feature.clearQuery()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
//                                Avatar(opt.imageUrl, size = 40)
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text(opt.name, style = MaterialTheme.typography.bodyLarge)
//                                    Text(
//                                        opt.id,
//                                        style = MaterialTheme.typography.bodySmall,
//                                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                                    )
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun Avatar(url: String?, size: Int) {
    if (url.isNullOrBlank()) {
        // fallback: colored circle
        Surface(
            modifier = Modifier
                .clip(CircleShape)
                .height(size.dp)
                .fillMaxWidth(0f),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {}
    } else {
        Image(
            painter = rememberAsyncImagePainter(url),
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .height(size.dp)
                .fillMaxWidth(0f) // height-only
        )
    }
}

