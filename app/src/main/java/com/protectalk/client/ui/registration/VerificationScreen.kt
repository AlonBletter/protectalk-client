@file:OptIn(ExperimentalMaterial3Api::class)

package com.protectalk.client.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Preview(showBackground = true)
@Composable
private fun VerificationScreen_Preview() {
    VerificationScreen(
        phoneNumber = "+972-52-8849-564",
        onVerified = {}
    )
}

@Composable
fun VerificationScreen(
    phoneNumber: String,
    onVerified: () -> Unit,
    viewModel: AuthViewModel = viewModel()     // ← inject the VM
) {
    var code by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // Read VM state (countdown, errors, etc.)
    val ui = viewModel.ui.collectAsState().value
    val secondsLeft = ui.secondsLeft
    val isResendEnabled = secondsLeft == 0
    val errorToShow = localError ?: ui.error

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Verify Phone") }
            )
        }
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
                Text(
                    text = "We sent an SMS code to\n$phoneNumber",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
                    label = { Text("6‑digit code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = { Text("${code.length}/6") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        localError = null
                        viewModel.verifyCode(
                            code = code,
                            onSuccess = onVerified,
                            onError = { msg -> localError = msg } // show simple inline message
                        )
                    },
                    enabled = code.length == 6 && !ui.isVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text(if (ui.isVerifying) "Verifying..." else "Verify") }

                Spacer(Modifier.height(10.dp))

                TextButton(
                    onClick = {
                        localError = null
                        viewModel.resendCode()  // VM handles restarting countdown
                    },
                    enabled = isResendEnabled
                ) {
                    Text(
                        if (isResendEnabled) "Resend code"
                        else "Resend in ${secondsLeft}s"
                    )
                }

                // Inline error (VM or local)
                if (!errorToShow.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorToShow!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
