@file:OptIn(ExperimentalMaterial3Api::class)

package com.protectalk.protectalk.ui.protection

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
fun ProtectionScreen(
    viewModel: ProtectionViewModel = viewModel()
) {
    val ui = viewModel.ui.collectAsState().value
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTab by rememberSaveable { mutableStateOf(ProtectionTab.Protegee) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var dialogRole by rememberSaveable { mutableStateOf(DialogRole.ProtegeeAsks) }
    var isAppInForeground by remember { mutableStateOf(true) }

    // Observe lifecycle to track foreground/background state
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppInForeground = true
                Lifecycle.Event.ON_PAUSE -> isAppInForeground = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Automatic refresh every 30 seconds when app is in foreground
    LaunchedEffect(isAppInForeground) {
        while (isAppInForeground) {
            delay(30_000) // 30 seconds
            if (isAppInForeground && !ui.isRefreshing) {
                viewModel.refresh()
            }
        }
    }

    // Clear error when dialog is dismissed
    LaunchedEffect(showDialog) {
        if (!showDialog) {
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Protection") },
                actions = {
                    // Manual refresh button
                    IconButton(
                        onClick = {
                            if (isAppInForeground && !ui.isRefreshing) {
                                viewModel.refresh()
                            }
                        }
                    ) {
                        if (ui.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val tabs = listOf(
                ProtectionTab.Protegee to "My Protegees",
                ProtectionTab.Trusted to "My Trusted Contacts"
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
                    incoming = ui.incomingProtectionRequests, // People asking for my protection
                    outgoing = ui.outgoingProtegeeOffers, // Protection offers I sent
                    protegees = ui.protegees, // People I protect
                    onAccept = { viewModel.accept(it) },
                    onDecline = { viewModel.decline(it) },
                    onCancelOutgoing = { viewModel.cancelOutgoing(it) },
                    onRemoveProtegee = { viewModel.removeProtegee(it) },
                    onOpenAddDialog = { dialogRole = DialogRole.TrustedOffers; showDialog = true }
                )
                ProtectionTab.Trusted -> TrustedPane(
                    incoming = ui.incomingProtectionOffers, // People offering to protect me
                    outgoing = ui.outgoingTrustedRequests, // Protection requests I sent
                    trustedContacts = ui.trusted, // My trusted contacts
                    onAccept = { viewModel.accept(it) },
                    onDecline = { viewModel.decline(it) },
                    onCancelOutgoing = { viewModel.cancelOutgoing(it) },
                    onRemoveTrusted = { viewModel.removeTrusted(it) },
                    onOpenAddDialog = { dialogRole = DialogRole.ProtegeeAsks; showDialog = true }
                )
            }
        }

        // Dialog and other components outside the main Column
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
                }
            )
        }

        // Auto-close dialog on successful submission
        LaunchedEffect(ui.isLoading, ui.error) {
            if (!ui.isLoading && ui.error == null && showDialog) {
                showDialog = false
            }
        }
    }
}
