@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.protectalk.protectalk.ui.protection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddProtectionDialog(
    role: DialogRole,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, relation: Relation) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var relation by rememberSaveable { mutableStateOf(Relation.Family) }
    var relationMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() }, // Prevent dismiss while loading
        title = {
            Text(
                when (role) {
                    DialogRole.ProtegeeAsks -> "Add Trusted Contact"
                    DialogRole.TrustedOffers -> "Add Protegee"
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )

                // Relation dropdown
                ExposedDropdownMenuBox(
                    expanded = relationMenuOpen && !isLoading,
                    onExpandedChange = { if (!isLoading) relationMenuOpen = it },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    OutlinedTextField(
                        value = relation.name,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isLoading,
                        label = { Text("Relation") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = MenuAnchorType.PrimaryNotEditable,
                                enabled = !isLoading
                            )
                    )
                    ExposedDropdownMenu(
                        expanded = relationMenuOpen && !isLoading,
                        onDismissRequest = { relationMenuOpen = false }
                    ) {
                        Relation.values().forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.name) },
                                onClick = {
                                    relation = r
                                    relationMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSubmit(name.trim(), phone.trim(), relation)
                    }
                },
                enabled = !isLoading && name.isNotBlank() && phone.isNotBlank()
            ) {
                if (isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("Sending...")
                    }
                } else {
                    Text(
                        when (role) {
                            DialogRole.ProtegeeAsks -> "Send Request"
                            DialogRole.TrustedOffers -> "Send Offer"
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
