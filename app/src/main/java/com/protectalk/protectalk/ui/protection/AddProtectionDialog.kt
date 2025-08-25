@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.protectalk.protectalk.ui.protection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddProtectionDialog(
    role: DialogRole,
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, relation: Relation) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var relation by rememberSaveable { mutableStateOf(Relation.Family) }
    var relationMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss() }, // Cancel hides dialog
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
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )

                // Relation dropdown
                ExposedDropdownMenuBox(
                    expanded = relationMenuOpen,
                    onExpandedChange = { relationMenuOpen = it },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    OutlinedTextField(
                        value = relation.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Relation") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = MenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                    )
                    ExposedDropdownMenu(
                        expanded = relationMenuOpen,
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
            TextButton(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSubmit(name.trim(), phone.trim(), relation)
                    }
                }
            ) { Text("Save") /* TODO: rename to "Send" on Protegee, "Offer" on Trusted if desired */ }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel") // TODO: if you add draft state later, rollback here too
            }
        },
        modifier = Modifier.height(IntrinsicSize.Min)
    )
}
