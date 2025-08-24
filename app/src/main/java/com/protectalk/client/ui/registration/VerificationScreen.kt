@file:OptIn(ExperimentalMaterial3Api::class)

package com.protectalk.client.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val TOTAL_SECONDS = 60

@Composable
fun VerificationScreen(
    phoneNumber: String,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }

    // countdown state
    var secondsLeft by rememberSaveable { mutableIntStateOf(TOTAL_SECONDS) }
    val isResendEnabled by remember { derivedStateOf { secondsLeft == 0 } }

    // Start / maintain countdown
    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        }
    }

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
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp)
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "We sent an SMS code to\n$phoneNumber",
                    style = MaterialTheme.typography.titleMedium
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
                        // TODO: Firebase sign-in with verificationId + code:
                        //  val credential = PhoneAuthProvider.getCredential(verificationId, code)
                        //  FirebaseAuth.getInstance().signInWithCredential(credential) { ... }
                        onVerified()
                    },
                    enabled = code.length == 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text("Verify") }

                Spacer(Modifier.height(10.dp))

                TextButton(
                    onClick = {
                        // TODO: Firebase resend with resendToken:
                        //  PhoneAuthProvider.verifyPhoneNumber(options.withResendToken(resendToken))
                        // restart the countdown UI
                        secondsLeft = TOTAL_SECONDS
                    },
                    enabled = isResendEnabled
                ) {
                    Text(
                        if (isResendEnabled) "Resend code"
                        else "Resend in ${secondsLeft}s"
                    )
                }

                // TODO: Show Firebase error messages (e.g., invalid code) below:
                // Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
