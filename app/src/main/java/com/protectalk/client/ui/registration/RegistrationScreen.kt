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

@Preview(showBackground = true)
@Composable
private fun RegistrationScreen_Preview() {
    RegistrationScreen(onContinue = { /* no-op for preview */ })
}

@Composable
fun RegistrationScreen(
    onContinue: (phone: String) -> Unit
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
                        // TODO: Start Firebase phone auth: verifyPhoneNumber(phone)
                        //  - Save verificationId/resendToken in ViewModel or rememberSaveable
                        onContinue(phone)
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
