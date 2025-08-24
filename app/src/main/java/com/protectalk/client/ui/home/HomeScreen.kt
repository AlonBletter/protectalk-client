package com.protectalk.client.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Home") }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text("Welcome to ProtecTalk!", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text("This is a placeholder for settings and shortcuts.")
        }
    }
}
