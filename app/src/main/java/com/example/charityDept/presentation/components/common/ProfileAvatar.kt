package com.example.charityDept.presentation.components.common

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
fun ProfileAvatar(
    profileImageLocalPath: String,
    profileImg: String,
    imageSize: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    Log.d(
        "ProfileAvatar",
        "profileImageLocalPath=$profileImageLocalPath exists=${profileImageLocalPath.takeIf { it.isNotBlank() }?.let { File(it).exists() }}"
    )

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

    Box(
        modifier = modifier
            .size(imageSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = "Profile image",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "No profile image",
                modifier = Modifier.size((imageSize.value * 0.46f).dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}