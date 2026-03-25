package com.example.charityDept.presentation.screens.splash

import com.example.charityDept.presentation.theme.CharityDeptTheme

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.R
import com.example.charityDept.presentation.screens.migrationToolKit.AppGateViewModel
import com.example.charityDept.presentation.viewModels.SplashViewModel
//import com.example.charityDept.presentation.viewModels.update.AppGateViewModel // <- add this VM file as we discussed

import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    toLogin: () -> Unit,
    toAdmin: () -> Unit,
    vm: SplashViewModel = hiltViewModel(),
    gateVm: AppGateViewModel = hiltViewModel(),    // <-- update gate VM
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val up by gateVm.ui.collectAsStateWithLifecycle() // force/soft update state

    // original delayed nav, but only if gate allows
    LaunchedEffect(up.loading, up.forceUpdate, up.showSoftPrompt) {
        // Wait a moment for visuals + give gate time to load
        delay(2_000)

        // If we must force-update, do NOT navigate
        if (up.forceUpdate) return@LaunchedEffect

        // If soft prompt is showing, also pause auto-nav (user decides in dialog)
        if (up.showSoftPrompt) return@LaunchedEffect

        // Otherwise continue as before
        if (vm.isLoggedIn()) toAdmin() else toLogin()
    }

    CharityDeptTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.tertiary),
            contentAlignment = Alignment.Center
        ) {
            // Centered logo + spinner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.zion_kids_logo),
                    contentDescription = "UpLift Logo",
                    modifier = Modifier.size(150.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onTertiary,
                    strokeWidth = 4.dp
                )
            }

            // Tagline at bottom
            Text(
                text = "Children for Christ Jesus ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }

        // --- Update gate dialogs ---
        if (!up.loading && (up.forceUpdate || up.showSoftPrompt)) {
            AlertDialog(
                onDismissRequest = { /* block dismiss if force */ },
                title = {
                    Text(if (up.forceUpdate) "Update required" else "Update available")
                },
                text = {
                    Text(
                        up.message ?: if (up.forceUpdate)
                            "This version is no longer supported. Please update to continue."
                        else
                            "A newer version is available."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        up.downloadUrl?.let { url ->
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            )
                        }
                        // If soft prompt, allow proceed after pressing "Update"
                        if (!up.forceUpdate) {
                            if (vm.isLoggedIn()) toAdmin() else toLogin()
                        }
                    }) { Text("Update") }
                },
                dismissButton = {
                    if (!up.forceUpdate) {
                        TextButton(onClick = {
                            // User chose Later → continue normal nav
                            if (vm.isLoggedIn()) toAdmin() else toLogin()
                        }) { Text("Later") }
                    }
                }
            )
        }
    }
}
//package com.example.charityDept.presentation.screens.splash
//
//import com.example.charityDept.presentation.theme.ZionKidsTheme
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.example.charityDept.R
//import com.example.charityDept.presentation.viewModels.SplashViewModel
//
//import kotlinx.coroutines.delay
//
//@Composable
//fun SplashScreen(
//    toLogin: () -> Unit,
//    toAdmin: () -> Unit,
//    vm: SplashViewModel = hiltViewModel(),
//    modifier: Modifier = Modifier
//) {
//    LaunchedEffect(Unit) {
//        delay(2_000)
//        if (vm.isLoggedIn()) toAdmin() else toLogin()
//    }
//
//    ZionKidsTheme {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(MaterialTheme.colorScheme.tertiary),
//            contentAlignment = Alignment.Center
//        ) {
//            // Centered logo + spinner
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
//            ) {
//                Image(
//                    painter = painterResource(id = R.drawable.zion_kids_logo),
//                    contentDescription = "UpLift Logo",
//                    modifier = Modifier.size(150.dp)
//                )
//                Spacer(modifier = Modifier.height(24.dp))
//                CircularProgressIndicator(
//                    color = MaterialTheme.colorScheme.onTertiary,
//                    strokeWidth = 4.dp
//                )
//            }
//
//            // Tagline at bottom
//            Text(
//                text = "Children for Christ Jesus ",
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.onPrimary,
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .padding(bottom = 48.dp)
//            )
//        }
//    }
//}

