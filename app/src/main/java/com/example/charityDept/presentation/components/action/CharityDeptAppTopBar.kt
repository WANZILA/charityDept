// package com.example.charityDept.presentation.components.action
package com.example.charityDept.presentation.components.action

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.R
import com.example.charityDept.presentation.navigation.LocalNavController
import com.example.charityDept.presentation.navigation.Screen
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharityDeptAppTopBar(
    canNavigateBack: Boolean,
    modifier: Modifier = Modifier,
    txtLabel: String = "",
    dateTxt: String = "",
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigateUp: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    vModel: AuthViewModel = hiltViewModel()
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val nav = LocalNavController.current

    // ---- YOUR COLOR CHOICES (as in your pasted code) ----
    // Use theme colors you were already using:
    val menuText = MaterialTheme.colorScheme.secondary
    val menuIcon = MaterialTheme.colorScheme.secondary
    // If you later want a colored menu background, uncomment and use:
    // val menuBg   = MaterialTheme.colorScheme.secondary

    // Current user from StateFlow
    val authUi by vModel.ui.collectAsStateWithLifecycle()
    val lastName by vModel.lastKnownDisplayName.collectAsStateWithLifecycle()
    val sessionKey = authUi.profile?.uid ?: "anon"
//    val lastName by authVm.lastKnownDisplayName.collectAsStateWithLifecycle()
    //UI cache to avoid brief blanks on screen swaps
    var stableName by rememberSaveable(sessionKey) { mutableStateOf(lastName) }
    LaunchedEffect(lastName) { if (lastName.isNotBlank()) stableName = lastName }

    val displayName = when {
        stableName.isNotBlank() -> stableName
        sessionKey == "anon"    -> "Guest"
        else                    -> "" // tiny gap if any, but no “Guest” flash
    }

    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.zion_kids_logo),
                    contentDescription = stringResource(R.string.company_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_size))
                        .clip(MaterialTheme.shapes.medium)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Main title — your original color
//                    for displaying logined in User display name
//                    Text(
//                        text = txtLabel.trim(),
//                        style = MaterialTheme.typography.displaySmall,
//                        color = MaterialTheme.colorScheme.secondary
//                    )
                    // Date — your original color
                    if (dateTxt.isNotBlank()) {
                        Text(
                            text = dateTxt,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Display name — matches your small caption color
//                    Text(
//                        text = displayName.trim(),
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
                }
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowCircleLeft,
                        contentDescription = stringResource(id = R.string.back_button),
                        // your icon tint
                        tint = menuIcon
                    )
                }
            } else {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.cd_open_menu),
                            // your icon tint
                            tint = menuIcon
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                        // .background(menuBg) // <- if you decide to color the menu background
                    ) {
                        // Optional header (disabled)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = menuText
                                )
                            },
                            leadingIcon = { /* could place avatar here */ },
                            enabled = false,
                            onClick = {}
                        )
                        Divider()

                        DropdownMenuItem(
                            text = { Text("Reports", color = menuText) },
                            leadingIcon = { Icon(Icons.Filled.Assessment, contentDescription = null, tint = menuIcon) },
                            onClick = {
                                menuExpanded = false
                                onReportsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sign out", color = menuText) },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = menuIcon) },
                            onClick = {
                                menuExpanded = false
                                vModel.signOut {
                                    nav?.navigate(Screen.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        // You had these commented out before; leaving them out to keep your original look.
        // colors = TopAppBarDefaults.centerAlignedTopAppBarColors(...),
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        windowInsets = WindowInsets(0)
    )
}

