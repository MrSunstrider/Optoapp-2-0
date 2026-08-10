package com.example.optoapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.theme.OptoTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptoDropdownMenuField(
    label: String,
    selected: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredOptions = remember(options, searchQuery) {
        if (searchQuery.isBlank()) options
        else options.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
            enabled = enabled,
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; searchQuery = "" },
        ) {
            if (options.size > 8) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OptoTokens.spacing.sm, vertical = OptoTokens.spacing.xs),
                )
            }

            if (filteredOptions.isEmpty()) {
                Text(
                    text = "Sin resultados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(OptoTokens.spacing.md),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(filteredOptions) { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (option == selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                onSelected(option)
                                expanded = false
                                searchQuery = ""
                            },
                        )
                    }
                }
            }
        }
    }
}
