package com.protectalk.protectalk.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Text(
            "Settings (UI only — TODO: real prefs later)",
            modifier = androidx.compose.ui.Modifier.padding(padding).padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
