package com.protectalk.protectalk.ui.protection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrustedPane(
    incoming: List<PendingRequest>,
    protegees: List<LinkContact>,
    onAccept: (PendingRequest) -> Unit,
    onDecline: (PendingRequest) -> Unit,
    onRemoveProtegee: (LinkContact) -> Unit,
    onOpenAddDialog: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = onOpenAddDialog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Protegee")
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Incoming requests") }
            if (incoming.isEmpty()) {
                item { Hint("No incoming requests.") }
            } else {
                items(incoming, key = { it.id }) { req ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${req.otherName} • ${req.otherPhone}", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(4.dp))
                                Text("Relation: ${req.relation.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader("Your protegees") }
            if (protegees.isEmpty()) {
                item { Hint("You have no protegees yet.") }
            } else {
                items(protegees, key = { it.id }) { c ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${c.name} • ${c.phone}", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text("Relation: ${c.relation.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onRemoveProtegee(c) }) { Text("Remove") }
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
