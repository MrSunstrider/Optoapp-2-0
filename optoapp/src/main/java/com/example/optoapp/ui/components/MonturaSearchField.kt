package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.optoapp.data.Montura
import com.example.optoapp.domain.inventario.monturaLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonturaSearchField(
    monturas: List<Montura>,
    selectedMonturaId: String?,
    onMonturaSelected: (Montura) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Buscar montura por marca, modelo o SKU",
    placeholder: String = "Ej: Ray-Ban, RX-1234...",
    onlyInStock: Boolean = true,
) {
    val monturaSeleccionada = monturas.firstOrNull { it.id == selectedMonturaId }
    var monturaQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // Sync label whenever the selected id changes (including programmatic restore on edit).
    LaunchedEffect(selectedMonturaId, monturaSeleccionada) {
        if (monturaSeleccionada != null) {
            monturaQuery = monturaLabel(monturaSeleccionada)
        }
    }

    val selectedLabel = monturaSeleccionada?.let { monturaLabel(it) }
    val isSelected = selectedLabel != null && monturaQuery == selectedLabel

    val searchable = if (onlyInStock) {
        monturas.filter { it.stockActual > 0 }
    } else {
        monturas
    }

    val filteredMonturas = if (isSelected) {
        emptyList()
    } else if (monturaQuery.isBlank()) {
        searchable
    } else {
        searchable.filter {
            it.marca.contains(monturaQuery, ignoreCase = true) ||
                it.modelo.contains(monturaQuery, ignoreCase = true) ||
                it.sku.contains(monturaQuery, ignoreCase = true) ||
                it.color.contains(monturaQuery, ignoreCase = true)
        }
    }

    val menuExpanded = expanded && filteredMonturas.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = menuExpanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = monturaQuery,
            onValueChange = { newQuery ->
                monturaQuery = newQuery
                when {
                    newQuery.isEmpty() -> onClear()
                    selectedLabel != null && newQuery != selectedLabel -> onClear()
                }
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
            singleLine = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { expanded = false },
        ) {
            filteredMonturas.forEach { montura ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${montura.marca} ${montura.modelo}", fontWeight = FontWeight.Bold)
                                Text(
                                    "SKU: ${montura.sku} | ${montura.color}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                "Stock: ${montura.stockActual}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    onClick = {
                        monturaQuery = monturaLabel(montura)
                        onMonturaSelected(montura)
                        expanded = false
                    },
                )
            }
        }
    }
}
