package com.protectalk.protectalk.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    email: String? = null,
    onLogout: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account section
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account", style = MaterialTheme.typography.titleMedium)
                    Text(
                        email ?: "Not available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Danger zone / Sign out
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Session", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { showConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log out")
                    }
                    // TODO: add more settings (notifications, theme, privacy) later
                }
            }
        }

        if (showConfirm) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                title = { Text("Log out?") },
                text = { Text("You’ll be returned to the sign-in screen.") },
                confirmButton = {
                    TextButton(onClick = { showConfirm = false; onLogout() }) {
                        Text("Log out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
