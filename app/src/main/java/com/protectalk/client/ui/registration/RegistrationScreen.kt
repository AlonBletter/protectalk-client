@file:OptIn(ExperimentalMaterial3Api::class)

package com.protectalk.client.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Preview(showBackground = true)
@Composable
private fun RegistrationScreen_Preview() {
    RegistrationScreen(onContinue = { /* no-op for preview */ })
}

@Composable
fun RegistrationScreen(
    onContinue: (phone: String) -> Unit,
    viewModel: AuthViewModel = viewModel()   // ← inject VM (keeps look identical)
) {
    var phone by remember { mutableStateOf("") }

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
                    .padding(horizontal = 20.dp, vertical = 150.dp)
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Enter your phone number", style = MaterialTheme.typography.headlineSmall)

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        // Hand off to AuthViewModel (UI-only stub for now)
                        viewModel.setPhone(phone)
                        viewModel.startPhoneVerification() // TODO(Firebase): real PhoneAuthOptions w/ Activity
                        onContinue(phone)                  // navigate to Verification
                    },
                    enabled = phone.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text("Continue") }

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
