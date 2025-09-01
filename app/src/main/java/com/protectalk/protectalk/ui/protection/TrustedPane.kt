package com.protectalk.protectalk.ui.protection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrustedPane(
    incoming: List<PendingRequest>, // People offering to protect me
    outgoing: List<PendingRequest>, // Protection requests I sent
    trustedContacts: List<LinkContact>, // My current trusted contacts
    onAccept: (PendingRequest) -> Unit,
    onDecline: (PendingRequest) -> Unit,
    onCancelOutgoing: (PendingRequest) -> Unit,
    onRemoveTrusted: (LinkContact) -> Unit,
    onOpenAddDialog: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Add button - Ask for protection
        Button(
            onClick = onOpenAddDialog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ask for protection")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Incoming protection offers section
            if (incoming.isNotEmpty()) {
                item {
                    Text(
                        text = "Protection Offers",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(incoming) { request ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "${request.otherName} offered to protect you",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = request.otherPhone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Relationship: ${request.relation}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onDecline(request) }) {
                                    Text("Decline")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { onAccept(request) }) {
                                    Text("Accept")
                                }
                            }
                        }
                    }
                }
            }

            // Outgoing protection requests section
            if (outgoing.isNotEmpty()) {
                item {
                    Text(
                        text = "Pending Protection Requests",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(outgoing) { request ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Asked ${request.otherName} for protection",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = request.otherPhone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Relationship: ${request.relation}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { onCancelOutgoing(request) }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // My Trusted Contacts section
            if (trustedContacts.isNotEmpty()) {
                item {
                    Text(
                        text = "My Trusted Contacts",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(trustedContacts) { contact ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = contact.phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Relationship: ${contact.relation}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { onRemoveTrusted(contact) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove trusted contact",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Empty state
            if (incoming.isEmpty() && outgoing.isEmpty() && trustedContacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No trusted contacts yet. Ask someone to protect you!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
