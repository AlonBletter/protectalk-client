@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.protectalk.protectalk.ui.protection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProtectionScreen(
    viewModel: ProtectionViewModel = viewModel()
) {
    val ui = viewModel.ui.collectAsState().value

    var selectedTab by rememberSaveable { mutableStateOf(ProtectionTab.Protegee) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var dialogRole by rememberSaveable { mutableStateOf(DialogRole.ProtegeeAsks) }

    // Clear error when dialog is dismissed
    LaunchedEffect(showDialog) {
        if (!showDialog) {
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Protection") }) }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp)
        ) {
            val tabs = listOf(
                ProtectionTab.Protegee to "Protegee",
                ProtectionTab.Trusted to "Trusted Contact"
            )
            val selectedIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)
            TabRow(selectedTabIndex = selectedIndex) {
                tabs.forEachIndexed { index, (tab, label) ->
                    Tab(
                        selected = index == selectedIndex,
                        onClick = { selectedTab = tab },
                        text = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Show error message if any
            if (ui.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ui.error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            when (selectedTab) {
                ProtectionTab.Protegee -> ProtegeePane(
                    outgoing = ui.outgoing,
                    trustedContacts = ui.trusted,
                    onCancelOutgoing = { viewModel.cancelOutgoing(it) },
                    onRemoveTrusted = { viewModel.removeTrusted(it) },
                    onOpenAddDialog = { dialogRole = DialogRole.ProtegeeAsks; showDialog = true }
                )
                ProtectionTab.Trusted -> TrustedPane(
                    incoming = ui.incoming,
                    protegees = ui.protegees,
                    onAccept = { viewModel.accept(it) },
                    onDecline = { viewModel.decline(it) },
                    onRemoveProtegee = { viewModel.removeProtegee(it) },
                    onOpenAddDialog = { dialogRole = DialogRole.TrustedOffers; showDialog = true }
                )
            }
        }

        if (showDialog) {
            AddProtectionDialog(
                role = dialogRole,
                isLoading = ui.isLoading,
                onDismiss = { showDialog = false },
                onSubmit = { name, phone, relation ->
                    when (dialogRole) {
                        DialogRole.ProtegeeAsks -> viewModel.sendRequest(name, phone, relation)
                        DialogRole.TrustedOffers -> viewModel.offerProtection(name, phone, relation)
                    }
                    // Don't close dialog immediately - let the API call complete
                    // Dialog will be closed by the success case or stay open for error handling
                }
            )
        }

        // Auto-close dialog on successful submission
        LaunchedEffect(ui.isLoading, ui.error) {
            if (!ui.isLoading && ui.error == null && showDialog) {
                // Only close if we were loading (indicating a successful API call)
                showDialog = false
            }
        }
    }
}
