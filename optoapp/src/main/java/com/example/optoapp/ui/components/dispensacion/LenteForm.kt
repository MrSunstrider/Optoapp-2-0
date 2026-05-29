package com.example.optoapp.ui.components.dispensacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.Montura
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.DispensacionItemUi

/**
 * Formulario para UN item (lente + montura) dentro de una dispensación.
 * Se repite por cada lente/montura que necesite el paciente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LenteForm(
    item: DispensacionItemUi,
    index: Int,
    isOnlyItem: Boolean,
    monturasActivas: List<Montura>,
    onUpdate: (DispensacionItemUi) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // ─── Header: número de item + botón eliminar ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lente ${index + 1}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!isOnlyItem) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider()
            Text("Lente", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            //── Info del lente ───────────────────────────────────────────
            DropdownField(label = "Tipo de Lente", selected = item.tipoLente, options = listOf("Monofocal", "Bifocal", "Progresivo", "Ocupacional")) {
                val cleaned = when (it) {
                    "Bifocal" -> item.copy(tipoLente = it, distanciaLente = "", altura = "")
                    "Monofocal" -> item.copy(tipoLente = it, subTipoBifocal = "", altura = "")
                    "Progresivo", "Ocupacional" -> item.copy(tipoLente = it, subTipoBifocal = "", distanciaLente = "")
                    else -> item.copy(tipoLente = it, subTipoBifocal = "", distanciaLente = "", altura = "")
                }
                onUpdate(cleaned)
            }

            if (item.tipoLente == "Bifocal") {
                DropdownField(label = "Sub-tipo Bifocal", selected = item.subTipoBifocal, options = listOf("Flaptop", "Invisible")) {
                    onUpdate(item.copy(subTipoBifocal = it))
                }
            }

            if (item.tipoLente == "Monofocal") {
                DropdownField(label = "Distancia", selected = item.distanciaLente, options = listOf("Lejos", "Intermedia", "Cerca")) {
                    onUpdate(item.copy(distanciaLente = it))
                }
            }

            if (item.tipoLente == "Bifocal" || item.tipoLente == "Progresivo" || item.tipoLente == "Ocupacional") {
                OptoTextField(
                    value = item.altura,
                    onValueChange = { onUpdate(item.copy(altura = it)) },
                    label = "Altura (mm)",
                    keyboardType = KeyboardType.Decimal
                )
            }

            DropdownField(label = "Material del Lente", selected = item.materialLente, options = listOf("Resina", "Policarbonato", "Cristal", "Trivex")) {
                onUpdate(item.copy(materialLente = it))
            }

            Text("Tratamientos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            val opts = listOf("Ninguno", "Antireflejo", "Antirayas", "Filtro UV 400", "Fotocromático", "AR Blue Defense")
            val currentTrats = if (item.tratamientos.isEmpty()) listOf("Ninguno") else item.tratamientos + "Ninguno"
            currentTrats.distinct().forEachIndexed { idx, selectedValue ->
                if (idx == 0 || currentTrats[idx - 1] != "Ninguno") {
                    DropdownField(
                        label = if (idx == 0) "Tratamiento Principal" else "Tratamiento Adicional",
                        selected = selectedValue,
                        options = opts
                    ) { selected ->
                        val newList = item.tratamientos.toMutableList()
                        if (idx < item.tratamientos.size) {
                            if (selected == "Ninguno") newList.removeAt(idx)
                            else newList[idx] = selected
                        } else if (selected != "Ninguno") {
                            newList.add(selected)
                        }
                        onUpdate(item.copy(tratamientos = newList.distinct().filter { t -> t != "Ninguno" }))
                    }
                }
            }

            OptoTextField(value = item.colorLente, onValueChange = { onUpdate(item.copy(colorLente = it)) }, label = "Color del Lente")
            OptoTextField(value = item.notasDiseno, onValueChange = { onUpdate(item.copy(notasDiseno = it)) }, label = "Notas de Diseño")

            //── Info de la montura ───────────────────────────────────────
            HorizontalDivider()
            Text("Montura", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            DropdownField(label = "Origen", selected = item.origenMontura, options = listOf("Tienda", "Paciente")) {
                if (it == "Tienda") onUpdate(item.copy(origenMontura = it))
                else onUpdate(item.copy(origenMontura = it, monturaId = ""))
            }

            if (item.origenMontura == "Tienda") {
                val monturaSeleccionada = monturasActivas.firstOrNull { it.id == item.monturaId }
                var monturaQuery by remember { mutableStateOf("") }
                var expanded by remember { mutableStateOf(false) }

                LaunchedEffect(monturaSeleccionada) {
                    if (monturaSeleccionada != null && monturaQuery.isEmpty()) {
                        monturaQuery = "${monturaSeleccionada.marca} ${monturaSeleccionada.modelo}"
                    }
                }

                val isSelected = monturaSeleccionada != null &&
                    monturaQuery == "${monturaSeleccionada.marca} ${monturaSeleccionada.modelo}"

                val filteredMonturas = if (isSelected) emptyList()
                else if (monturaQuery.isBlank()) monturasActivas.filter { it.stockActual > 0 }
                else monturasActivas.filter { it.stockActual > 0 }
                    .filter {
                        it.marca.contains(monturaQuery, ignoreCase = true) ||
                        it.modelo.contains(monturaQuery, ignoreCase = true) ||
                        it.sku.contains(monturaQuery, ignoreCase = true)
                    }

                ExposedDropdownMenuBox(
                    expanded = expanded && filteredMonturas.isNotEmpty(),
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = monturaQuery,
                        onValueChange = {
                            monturaQuery = it
                            if (it.isEmpty()) onUpdate(item.copy(monturaId = "", descripcionMontura = ""))
                            expanded = true
                        },
                        label = { Text("Buscar montura") },
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
                                    onUpdate(item.copy(
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

            DropdownField(label = "Tipo de Aro", selected = item.tipoAro, options = listOf("Aro Completo", "Semi al aire", "Al aire")) {
                onUpdate(item.copy(tipoAro = it))
            }
            DropdownField(label = "Material de la Montura", selected = item.materialMontura, options = listOf("Acetato", "Metal", "Carey", "Econ")) {
                onUpdate(item.copy(materialMontura = it))
            }
            OptoTextField(value = item.descripcionMontura, onValueChange = { onUpdate(item.copy(descripcionMontura = it)) }, label = "Descripción (Marca, Modelo)")
        }
    }
}
