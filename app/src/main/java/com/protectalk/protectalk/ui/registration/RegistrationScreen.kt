@file:OptIn(ExperimentalMaterial3Api::class)

package com.protectalk.protectalk.ui.registration

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
private fun RegistrationScreen_Preview() {
    RegistrationScreen(
        onRegister = { _, _ -> },
        onNavigateToLogin = {},
        serverError = null,
        isSubmittingExternal = false
    )
}

@Composable
fun RegistrationScreen(
    onRegister: (email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    serverError: String? = null,          // ← show VM/ Firebase errors here (e.g., email already in use)
    isSubmittingExternal: Boolean = false // ← disable while VM is working
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) } // local UI toggle

    fun isValidEmail(s: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches()

    val passwordsMatch = password == confirm
    val passwordOk = password.length >= 6
    val emailOk = isValidEmail(email)
    val anyError = errorText ?: serverError
    val isWorking = isSubmitting || isSubmittingExternal
    val canSubmit = emailOk && passwordOk && passwordsMatch && !isWorking

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("ProtecTalk") }) }
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
                Text("Create your account", style = MaterialTheme.typography.headlineSmall)

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorText = null },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = null },
                    label = { Text("Password (min 6)") },
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

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it; errorText = null },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirm = !showConfirm }) {
                            Icon(
                                imageVector = if (showConfirm) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showConfirm) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // Inline validation hints
                if (!emailOk && email.isNotBlank()) {
                    Text("Enter a valid email address.", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                }
                if (!passwordOk && password.isNotEmpty()) {
                    Text("Password must be at least 6 characters.", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                }
                if (!passwordsMatch && confirm.isNotEmpty()) {
                    Text("Passwords do not match.", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                }

                // Server/VM error (e.g., "The email address is already in use by another account.")
                anyError?.let {
                    val friendly = if (it.contains("already in use", ignoreCase = true))
                        "This email is already registered. Try signing in."
                    else it
                    Text(friendly, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                }

                Button(
                    onClick = {
                        if (!emailOk) { errorText = "Invalid email format"; return@Button }
                        if (!passwordOk) { errorText = "Password too short"; return@Button }
                        if (!passwordsMatch) { errorText = "Passwords don’t match"; return@Button }

                        errorText = null
                        isSubmitting = true
                        // Delegate to VM via onRegister; VM will toggle isSubmittingExternal & set serverError
                        onRegister(email, password)
                        // Leave local spinner minimal; AppNavHost can pass isSubmittingExternal=true while VM works
                        isSubmitting = false
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text(if (isWorking) "Creating..." else "Create account") }

                Spacer(Modifier.height(16.dp))

                // Already registered? -> Login
                TextButton(onClick = onNavigateToLogin) {
                    Text("Already have an account? Sign in")
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    "By continuing you agree to our basic terms (placeholder).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
