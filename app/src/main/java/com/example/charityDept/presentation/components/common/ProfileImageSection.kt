package com.example.charityDept.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun ProfileImageSection(
    profileImageLocalPath: String,
    profileImg: String,
    displayName: String? = null,
    imageSize: Dp = 120.dp,
    showImageSourceText: Boolean = false,
    modifier: Modifier = Modifier
) {
    val imageModel = remember(profileImageLocalPath, profileImg) {
        val localFile = profileImageLocalPath
            .takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.exists() }

        when {
            localFile != null -> localFile
            profileImg.isNotBlank() -> profileImg
            else -> null
        }
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (imageModel != null) {
                key(profileImageLocalPath, profileImg) {
                    Box(
                        modifier = Modifier
                            .size(imageSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Profile image",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(imageSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "No profile image",
                        modifier = Modifier.size((imageSize.value * 0.46f).dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!displayName.isNullOrBlank()) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (showImageSourceText) {
                Text(
                    text = when {
                        profileImageLocalPath.isNotBlank() -> "Showing cached local photo"
                        profileImg.isNotBlank() -> "Showing remote profile photo"
                        else -> "No profile photo yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}