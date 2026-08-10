package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Deprecated(
    message = "Use OptoDropdownMenuField instead for native ExposedDropdownMenuBox experience",
    replaceWith = ReplaceWith(
        "OptoDropdownMenuField(label, selected, options, modifier = Modifier, enabled = true, onSelected)",
        "com.example.optoapp.ui.components.OptoDropdownMenuField",
    ),
)
@Composable
fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selected,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    options.forEach { opt ->
                        val isSelected = opt == selected
                        Surface(
                            onClick = {
                                onSelected(opt)
                                showDialog = false
                            },
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            },
        )
    }
}
