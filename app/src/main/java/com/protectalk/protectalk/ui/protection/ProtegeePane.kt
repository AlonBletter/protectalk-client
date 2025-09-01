package com.protectalk.protectalk.ui.protection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProtegeePane(
    incoming: List<PendingRequest>, // People asking for my protection
    outgoing: List<PendingRequest>, // Protection offers I sent
    protegees: List<LinkContact>, // People I currently protect
    onAccept: (PendingRequest) -> Unit,
    onDecline: (PendingRequest) -> Unit,
    onCancelOutgoing: (PendingRequest) -> Unit,
    onRemoveProtegee: (LinkContact) -> Unit,
    onOpenAddDialog: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Add button - Offer protection
        Button(
            onClick = onOpenAddDialog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Offer protection")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Incoming protection requests section
            if (incoming.isNotEmpty()) {
                item {
                    Text(
                        text = "Protection Requests",
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
                                text = "${request.otherName} wants your protection",
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

            // Outgoing protection offers section
            if (outgoing.isNotEmpty()) {
                item {
                    Text(
                        text = "Pending Protection Offers",
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
                                    text = "Offered protection to ${request.otherName}",
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

            // My Protegees section
            if (protegees.isNotEmpty()) {
                item {
                    Text(
                        text = "People I Protect",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(protegees) { protegee ->
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
                                    text = protegee.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = protegee.phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Relationship: ${protegee.relation}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { onRemoveProtegee(protegee) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Stop protecting",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Empty state
            if (incoming.isEmpty() && outgoing.isEmpty() && protegees.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No protegees yet. Offer to protect someone!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
