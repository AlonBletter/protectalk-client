@file:OptIn(ExperimentalMaterial3Api::class)

package com.protectalk.protectalk.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Preview(showBackground = true)
@Composable
private fun UserProfileScreen_Preview() {
    UserProfileScreen(
        onComplete = {},
        onBack = {},
        isLoading = false,
        errorMessage = null
    )
}

@Composable
fun UserProfileScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()

    fun isValidPhoneNumber(phone: String): Boolean {
        // Basic phone validation - adjust regex as needed for your region
        return phone.replace(Regex("[^\\d]"), "").length >= 10
    }

    val isFormValid = name.trim().length >= 2 && isValidPhoneNumber(phoneNumber)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Complete Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Help your contacts find and reach you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                localError = null
            },
            label = { Text("Full Name") },
            placeholder = { Text("Enter your full name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isSubmitting && !isLoading,
            singleLine = true,
            isError = name.trim().length in 1..1 // Show error only if they started typing but name is too short
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                localError = null
            },
            label = { Text("Phone Number") },
            placeholder = { Text("+1 (555) 123-4567") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            enabled = !isSubmitting && !isLoading,
            singleLine = true,
            isError = phoneNumber.isNotEmpty() && !isValidPhoneNumber(phoneNumber)
        )

        // Error message display
        if (errorMessage != null || localError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage ?: localError ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = {
                if (isFormValid) {
                    isSubmitting = true
                    localError = null // Clear previous errors

                    // Use the new complete registration flow
                    authViewModel.completeFullRegistration(
                        name = name.trim(),
                        phoneNumber = phoneNumber.trim(),
                        context = context,
                        onSuccess = {
                            // Registration completed successfully, navigate to main app
                            onComplete()
                        },
                        onError = { error ->
                            localError = error
                            isSubmitting = false
                        }
                    )
                } else {
                    when {
                        name.trim().length < 2 -> localError = "Please enter your full name"
                        !isValidPhoneNumber(phoneNumber) -> localError = "Please enter a valid phone number"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = isFormValid && !isSubmitting && !isLoading
        ) {
            if (isSubmitting || isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Complete Registration")
            }
        }

        TextButton(
            onClick = onBack,
            enabled = !isSubmitting && !isLoading,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Back")
        }
    }
}
