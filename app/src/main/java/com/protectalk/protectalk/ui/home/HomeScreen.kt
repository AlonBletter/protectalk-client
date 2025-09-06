package com.protectalk.protectalk.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.protectalk.protectalk.R
import com.protectalk.protectalk.domain.RegisterDeviceUseCase
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
private fun HomeScreen_Preview() {
    HomeScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val registerDeviceUseCase = remember { RegisterDeviceUseCase() }

    // Register device when HomeScreen is first composed (for existing users who sign in)
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                registerDeviceUseCase(context)
                // Don't need to handle the result here - it's best-effort
            } catch (e: Exception) {
                // Silently handle errors - device registration is not critical for app functionality
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 40.dp)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ProtectTalk Logo
            Image(
                painter = painterResource(id = R.drawable.protectalk),
                contentDescription = "ProtectTalk Logo",
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Use 90% of available width
                    .padding(bottom = 32.dp)
            )

            Text("Welcome!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Status", style = MaterialTheme.typography.titleSmall)
                    Text("You're signed in. Protections are not configured yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Notifications", style = MaterialTheme.typography.titleSmall)
                    Text("No alerts yet. (UI only — logic TODO later)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
