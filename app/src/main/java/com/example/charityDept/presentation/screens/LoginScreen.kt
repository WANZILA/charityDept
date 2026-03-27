package com.example.charityDept.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.charityDept.R
import com.example.charityDept.presentation.theme.CharityDeptTheme
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
//import com.example.upliftfinance.presentation.viewmodels.LoginViewModel

private  const val TAG = "LoginScreen"

@Composable
fun LoginScreen(
    toAdminDashboard: () -> Unit,
    vm: AuthViewModel
) {
    // ⬇️ Collect the StateFlow so UI updates when AuthUiState changes
    val state by vm.ui.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Navigate once when login succeeds
    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) toAdminDashboard()
    }
    // Show errors as toasts
    LaunchedEffect(state.error) {
        state.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val isValid = remember(state.email, state.password) {
        state.email.contains("@") && state.password.length >= 6
    }
    val signInEnabled = isValid && !state.loading

    CharityDeptTheme {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Text("CharityDept Login", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(24.dp))

                Image(
                    painter = painterResource(id = R.drawable.zion_kids_logo),
                    contentDescription = "Charity Dept Logo",
                    modifier = Modifier.size(120.dp)
                )

                Spacer(Modifier.height(24.dp))

                // Email
                OutlinedTextField(
                    value = state.email,
                    onValueChange = vm::onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Password
                var showPwd by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = state.password,
                    onValueChange = vm::onPasswordChange,
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showPwd = !showPwd }) {
                            Text(if (showPwd) "HIDE" else "SHOW")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            if (signInEnabled) {
                                vm.signIn(onSuccess = toAdminDashboard)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    enabled = signInEnabled,
                    onClick = {
                        keyboard?.hide()
                        vm.signIn(onSuccess = toAdminDashboard)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.loading) "Signing in…" else "Sign In")
                }
            }
        }
    }
}

//@Composable
//fun LoginScreen(
//    toAdminDashboard: () -> Unit,
//   vm: AuthViewModel = hiltViewModel()
//){
//
//    val state = vm.ui
//    val isValid = state.email.contains("@") && state.password.length >= 6
//
//
//    ZionKidsTheme {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp),
////                .background(MaterialTheme.colorScheme.tertiary),
//            contentAlignment = Alignment.Center
//        ) {
//            val keyboardController = LocalSoftwareKeyboardController.current
//
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center,
//                modifier = Modifier
//                    .padding(20.dp)
//                    .fillMaxWidth()
//                    .align(Alignment.Center)
//            ) {
//                Text("ZionKids Login", style = MaterialTheme.typography.headlineSmall)
//                Spacer(Modifier.height(24.dp))
//                // Logo
//                Image(
//                    painter = painterResource(id = R.drawable.zion_kids_logo),
//                    contentDescription = "UpLift Logo",
//                    modifier = Modifier.size(120.dp)
//                )
//                Spacer(Modifier.height(24.dp))
//
//                OutlinedTextField(
//                    value = state.email,
//                    onValueChange = vm::onEmailChange,
//
//                    singleLine = true,
//                    textStyle = LocalTextStyle.current.copy(
//                        color = MaterialTheme.colorScheme.onSurface
//                    ),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
//                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
//                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
//                    ),
//                    keyboardOptions = KeyboardOptions.Default.copy(
//                        keyboardType = KeyboardType.Email,
//                        imeAction = ImeAction.Done
//                    ),
//                    keyboardActions = KeyboardActions (
//                        onDone = {
//                            keyboardController?.hide()
//                        }
//                    ),
//
//
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(Modifier.height(12.dp))
//
//                var showPwd by remember { mutableStateOf(false) }
//
//                OutlinedTextField(
//                    value = state.password,
//                    onValueChange = vm::onPasswordChange,
//                    label = { Text("Password") },
//                    singleLine = true,
//                    leadingIcon = { Icon(Icons.Default.Lock, null) },
//                    visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
//                    trailingIcon = {
//                        TextButton(onClick = { showPwd = !showPwd }) {
//                            Text(if (showPwd) "HIDE" else "SHOW")
//                        }
//                    },
//                    textStyle = LocalTextStyle.current.copy(
//                        color = MaterialTheme.colorScheme.onSurface
//                    ),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
//                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
//                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
//                    ),
//                    keyboardOptions = KeyboardOptions.Default.copy(
//                        keyboardType = KeyboardType.Password,
//                        imeAction = ImeAction.Done
//                    ),
//                    keyboardActions = KeyboardActions (
//                        onDone = {
//                            keyboardController?.hide()
//                        }
//                    ),
//
//
//                    modifier = Modifier.fillMaxWidth()
//                )
//                if (state.error != null) {
//                    Spacer(Modifier.height(8.dp))
//                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
//                }
//
//
//                Spacer(Modifier.height(16.dp))
//
//                // Login button
//                Button(
//                    onClick = { vm.signIn(
//                            email = state.email,
//                            password = state.password,
//                            onSuccess = {
//                                toAdminDashboard()
//                            }
//                         ) },
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = MaterialTheme.colorScheme.secondary,
//                        contentColor   = MaterialTheme.colorScheme.onSecondary
//                    ),
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text(if (state.loading) "Signing in…" else "Sign In")
//                }
//            }
//        }
//    }
//}
//
//
//
//@Preview(showBackground = true)
//@Composable
//fun LoginScreenPreview() {
//    ZionKidsTheme {
//        // Light stub version for preview
//        Box(Modifier.background(MaterialTheme.colorScheme.primary)) {
//            Column(Modifier.padding(16.dp)) {
//                Text("ZionKids Login", color = MaterialTheme.colorScheme.onPrimary)
//            }
//        }
//    }
//}

