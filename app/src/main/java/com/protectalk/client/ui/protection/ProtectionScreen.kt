@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.protectalk.client.ui.protection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProtectionScreen(
    viewModel: ProtectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ui = viewModel.ui.collectAsState().value

    var selectedTab by rememberSaveable { mutableStateOf(ProtectionTab.Protegee) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var dialogRole by rememberSaveable { mutableStateOf(DialogRole.ProtegeeAsks) }

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
                onDismiss = { showDialog = false },
                onSubmit = { name, phone, relation ->
                    when (dialogRole) {
                        DialogRole.ProtegeeAsks -> viewModel.sendRequest(name, phone, relation)
                        DialogRole.TrustedOffers -> viewModel.offerProtection(name, phone, relation)
                    }
                    showDialog = false
                }
            )
        }
    }
}
