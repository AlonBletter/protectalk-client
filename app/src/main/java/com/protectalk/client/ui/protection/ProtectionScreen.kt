@file:OptIn(ExperimentalMaterial3Api::class)

package com.protectalk.client.ui.protection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// --- simple UI models (replace with real data later) ---
data class PendingRequest(val id: String, val phone: String)
data class Contact(val id: String, val phone: String, val note: String? = null)

@Composable
fun ProtectionScreen() {
    var selectedTab by rememberSaveable { mutableStateOf(ProtectionTab.Protegee) }

    // Temporary UI state (preview/sample) — replace via ViewModel later
    var trustedPhone by rememberSaveable { mutableStateOf("") }
    var outgoing by rememberSaveable {
        mutableStateOf(
            listOf(
                PendingRequest("req1", "+1 555-0100"),
                PendingRequest("req2", "+972 52-555-1111")
            )
        )
    }
    var incoming by rememberSaveable {
        mutableStateOf(
            listOf(
                PendingRequest("reqA", "+44 7700 900123"),
            )
        )
    }
    var myTrusted by rememberSaveable {
        mutableStateOf(
            listOf(
                Contact("c1", "+1 555-0177", "Primary"),
            )
        )
    }
    var myProtegees by rememberSaveable {
        mutableStateOf(
            listOf(
                Contact("p1", "+972 54-111-2222"),
            )
        )
    }
    val snackbarHost = remember { SnackbarHostState() }
    var snack by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(snack) { snack?.let { snackbarHost.showSnackbar(it); snack = null } }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Protection") }) },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Tabs (simple & stable)
            ProtectionTabs(selectedTab) { selectedTab = it }
            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                ProtectionTab.Protegee -> ProtegeePane(
                    phone = trustedPhone,
                    onPhoneChange = { trustedPhone = it },
                    onSendRequest = {
                        // TODO: validate phone format
                        // TODO: check duplicates (already requested / already trusted)
                        // TODO(Firebase): create request in DB:
                        //   /requests/{id} { fromUserId, toPhone, status="pending", createdAt }
                        // TODO(FCM): push to target if token available
                        if (trustedPhone.isNotBlank()) {
                            outgoing = outgoing + PendingRequest("req_${System.currentTimeMillis()}", trustedPhone)
                            trustedPhone = ""
                            snack = "Protection request sent"
                        }
                    },
                    onShareLink = {
                        // TODO(deep link): protectalk://add-trusted?code=XYZ
                        // TODO(server): create invite code, persist, copy to clipboard
                        snack = "Invite link copied (TODO)"
                    },
                    onShowQr = {
                        // TODO: open QR dialog with invite code deep link
                        snack = "QR dialog (TODO)"
                    },
                    outgoing = outgoing,
                    myTrusted = myTrusted,
                    onRemoveTrusted = { c ->
                        // TODO(DB): delete relationship both sides
                        myTrusted = myTrusted.filterNot { it.id == c.id }
                        snack = "Removed trusted contact"
                    }
                )
                ProtectionTab.Trusted -> TrustedPane(
                    incoming = incoming,
                    onAccept = { req ->
                        // TODO(DB): mark request accepted
                        // TODO(DB): create relationship protegee <-> trusted
                        // TODO(FCM): notify requester
                        incoming = incoming.filterNot { it.id == req.id }
                        myProtegees = myProtegees + Contact("prot_${req.id}", req.phone)
                        snack = "Request accepted"
                    },
                    onDecline = { req ->
                        // TODO(DB): mark request declined
                        // TODO(FCM): optionally notify requester
                        incoming = incoming.filterNot { it.id == req.id }
                        snack = "Request declined"
                    },
                    myProtegees = myProtegees,
                    onRemoveProtegee = { c ->
                        // TODO(DB): remove relationship both sides
                        myProtegees = myProtegees.filterNot { it.id == c.id }
                        snack = "Removed protegee"
                    }
                )
            }
        }
    }
}

private enum class ProtectionTab { Protegee, Trusted }

@Composable
private fun ProtectionTabs(selected: ProtectionTab, onSelected: (ProtectionTab) -> Unit) {
    val tabs = listOf(ProtectionTab.Protegee to "Protegee", ProtectionTab.Trusted to "Trusted")
    val selectedIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEachIndexed { index, pair ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelected(pair.first) },
                text = { Text(pair.second) }
            )
        }
    }
}

// ---------------------- PANE: Protegee ----------------------
@Composable
private fun ProtegeePane(
    phone: String,
    onPhoneChange: (String) -> Unit,
    onSendRequest: () -> Unit,
    onShareLink: () -> Unit,
    onShowQr: () -> Unit,
    outgoing: List<PendingRequest>,
    myTrusted: List<Contact>,
    onRemoveTrusted: (Contact) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Ask someone to protect you", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Trusted contact phone") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSendRequest,
                    enabled = phone.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null) // This is AddPerson
                    Spacer(Modifier.width(8.dp)); Text("Send request")
                }
                OutlinedButton(onClick = onShareLink, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, contentDescription = null) // This is Link
                    Spacer(Modifier.width(8.dp)); Text("Share link")
                }
                OutlinedButton(onClick = onShowQr) {
                    Icon(Icons.Filled.Star, contentDescription = null) // This is QR
                }
            }
        }

        // Outgoing requests
        item { SectionHeader("Outgoing requests") }
        if (outgoing.isEmpty()) {
            item { EmptyRow("No outgoing requests yet.") }
        } else {
            items(outgoing) { req ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(req.phone, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        AssistChip(onClick = { /* TODO: cancel request in DB */ }, label = { Text("Cancel") })
                    }
                }
            }
        }

        // Current trusted contacts
        item { SectionHeader("Your trusted contacts") }
        if (myTrusted.isEmpty()) {
            item { EmptyRow("You have no trusted contacts yet.") }
        } else {
            items(myTrusted) { c ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(c.phone, style = MaterialTheme.typography.bodyLarge)
                        c.note?.let {
                            Spacer(Modifier.height(2.dp))
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onRemoveTrusted(c) }) { Text("Remove") }
                            // TODO(UI): maybe "Set primary" / "Rename" later
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- PANE: Trusted Contact ----------------------
@Composable
private fun TrustedPane(
    incoming: List<PendingRequest>,
    onAccept: (PendingRequest) -> Unit,
    onDecline: (PendingRequest) -> Unit,
    myProtegees: List<Contact>,
    onRemoveProtegee: (Contact) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Incoming requests
        item { SectionHeader("Incoming requests") }
        if (incoming.isEmpty()) {
            item { EmptyRow("No incoming requests.") }
        } else {
            items(incoming) { req ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(req.phone, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDecline(req) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { onAccept(req) }) {
                            Icon(Icons.Filled.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Current protegees
        item { SectionHeader("Your protegees") }
        if (myProtegees.isEmpty()) {
            item { EmptyRow("You have no protegees yet.") }
        } else {
            items(myProtegees) { c ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(c.phone, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onRemoveProtegee(c) }) { Text("Remove") }
                            // TODO(UI): "Mute alerts" / "Rename" later
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- helpers ----------------------
@Composable private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
}
@Composable private fun EmptyRow(hint: String) {
    Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 2.dp))
}
