@file:OptIn(ExperimentalMaterial3Api::class)

package com.protectalk.protectalk.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
private fun LoginScreen_Preview() {
    LoginScreen(
        onLogin = { _, _ -> },
        onBackToRegister = {},
        onForgotPassword = {},
        serverError = null,
        isSubmittingExternal = false
    )
}

@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    onBackToRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    serverError: String? = null,
    isSubmittingExternal: Boolean = false
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val isValidEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isWorking = isSubmittingExternal
    val canSubmit = email.isNotBlank() && isValidEmail && password.length >= 6 && !isWorking
    val anyError = localError ?: serverError

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Sign in") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 120.dp)
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; localError = null },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; localError = null },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = { onForgotPassword() },
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Forgot password?") }

                Spacer(Modifier.height(12.dp))

                // errors
                if (email.isNotBlank() && !isValidEmail) {
                    Text("Enter a valid email address.", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                }
                anyError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                }

                Button(
                    onClick = {
                        if (!isValidEmail) { localError = "Invalid email format"; return@Button }
                        if (password.length < 6) { localError = "Password must be at least 6 characters"; return@Button }
                        localError = null
                        onLogin(email, password) // VM will drive loading & errors via props
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text(if (isWorking) "Signing in..." else "Sign in") }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = onBackToRegister) {
                    Text("Need an account? Create one")
                }
            }
        }
    }
}
