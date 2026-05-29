package com.example.optoapp.ui.components.dispensacion

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Montura
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.DispensacionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonturaForm(
    uiState: DispensacionUiState,
    onUpdate: (DispensacionUiState) -> Unit,
    monturasActivas: List<Montura>
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Información de Montura", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            DropdownField(label = "Origen", selected = uiState.origenMontura, options = listOf("Tienda", "Paciente")) {
                if (it == "Tienda") onUpdate(uiState.copy(origenMontura = it))
                else onUpdate(uiState.copy(origenMontura = it, monturaId = ""))
            }
            if (uiState.origenMontura == "Tienda" || uiState.origenMontura == "Nueva de Tienda") {
                val monturaSeleccionada = monturasActivas.firstOrNull { it.id == uiState.monturaId }
                var monturaQuery by remember { mutableStateOf("") }
                var expanded by remember { mutableStateOf(false) }

                LaunchedEffect(monturaSeleccionada) {
                    if (monturaSeleccionada != null && monturaQuery.isEmpty()) {
                        monturaQuery = "${monturaSeleccionada.marca} ${monturaSeleccionada.modelo}"
                    }
                }

                val isSelected = monturaSeleccionada != null &&
                    monturaQuery == "${monturaSeleccionada.marca} ${monturaSeleccionada.modelo}"

                val filteredMonturas = if (isSelected) {
                    emptyList()
                } else if (monturaQuery.isBlank()) {
                    monturasActivas.filter { it.stockActual > 0 }
                } else {
                    monturasActivas
                        .filter { it.stockActual > 0 }
                        .filter {
                            it.marca.contains(monturaQuery, ignoreCase = true) ||
                            it.modelo.contains(monturaQuery, ignoreCase = true) ||
                            it.sku.contains(monturaQuery, ignoreCase = true)
                        }
                }

                ExposedDropdownMenuBox(
                    expanded = expanded && filteredMonturas.isNotEmpty(),
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = monturaQuery,
                        onValueChange = {
                            monturaQuery = it
                            if (it.isEmpty()) {
                                onUpdate(uiState.copy(monturaId = "", descripcionMontura = ""))
                            }
                            expanded = true
                        },
                        label = { Text("Buscar montura por marca, modelo o SKU") },
                        placeholder = { Text("Ej: Ray-Ban, RX-1234...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded && filteredMonturas.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredMonturas.forEach { montura ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${montura.marca} ${montura.modelo}", fontWeight = FontWeight.Bold)
                                            Text("SKU: ${montura.sku} | ${montura.color}",
                                                style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text("Stock: ${montura.stockActual}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                onClick = {
                                    monturaQuery = "${montura.marca} ${montura.modelo}"
                                    onUpdate(uiState.copy(
                                        monturaId = montura.id,
                                        tipoAro = montura.tipoAro,
                                        materialMontura = montura.materialMontura
                                    ))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            DropdownField(label = "Tipo de Aro", selected = uiState.tipoAro, options = listOf("Aro Completo", "Semi al aire", "Al aire")) {
                onUpdate(uiState.copy(tipoAro = it))
            }
            DropdownField(label = "Material", selected = uiState.materialMontura, options = listOf("Acetato", "Metal", "Carey", "Econ")) {
                onUpdate(uiState.copy(materialMontura = it))
            }
            OptoTextField(value = uiState.descripcionMontura, onValueChange = { onUpdate(uiState.copy(descripcionMontura = it)) }, label = "Descripción (Marca, Modelo)")
        }
    }
}
