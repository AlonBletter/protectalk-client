package com.protectalk.protectalk.ui.protection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProtegeePane(
    outgoing: List<PendingRequest>,
    trustedContacts: List<LinkContact>,
    onCancelOutgoing: (PendingRequest) -> Unit,
    onRemoveTrusted: (LinkContact) -> Unit,
    onOpenAddDialog: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = onOpenAddDialog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Trusted Contact")
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Outgoing requests") }
            if (outgoing.isEmpty()) {
                item { Hint("No outgoing requests yet.") }
            } else {
                items(outgoing, key = { it.id }) { req ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${req.otherName} • ${req.otherPhone}", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text("Relation: ${req.relation.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onCancelOutgoing(req) }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader("Your trusted contacts") }
            if (trustedContacts.isEmpty()) {
                item { Hint("You have no trusted contacts yet.") }
            } else {
                items(trustedContacts, key = { it.id }) { c ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${c.name} • ${c.phone}", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text("Relation: ${c.relation.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onRemoveTrusted(c) }) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}
@Composable private fun Hint(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 2.dp))
}
