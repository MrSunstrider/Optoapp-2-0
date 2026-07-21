package com.example.optoapp.ui.components.dispensacion

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.example.optoapp.domain.OpticalCatalog
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.DispensacionItemUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LenteForm(
    item: DispensacionItemUi,
    index: Int,
    isOnlyItem: Boolean,
    monturasActivas: List<Montura>,
    onUpdate: (DispensacionItemUi) -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.padding(0.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isOnlyItem) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        DropdownField(label = "Tipo de Lente", selected = item.tipoLente, options = OpticalCatalog.TIPO_LENTE) {
            val cleaned = when (it) {
                "Bifocal" -> item.copy(tipoLente = it, distanciaLente = "", altura = "")
                "Monofocal" -> item.copy(tipoLente = it, subTipoBifocal = "", altura = "")
                "Multifocal", "Ocupacional" -> item.copy(tipoLente = it, subTipoBifocal = "", distanciaLente = "")
                else -> item.copy(tipoLente = it, subTipoBifocal = "", distanciaLente = "", altura = "")
            }
            onUpdate(cleaned)
        }

        if (item.tipoLente == "Lentes de Contacto") {
            DropdownField(label = "Tipo de LC", selected = item.materialLente, options = listOf("Cosmético", "Graduado", "Terapéutico")) {
                onUpdate(item.copy(materialLente = it))
            }
            DropdownField(label = "Material", selected = item.colorLente, options = listOf("HEMA", "Silicon Hydrogel", "Híbrido", "RGP")) {
                onUpdate(item.copy(colorLente = it))
            }
            DropdownField(label = "Modalidad", selected = item.notasDiseno, options = listOf("Diario", "Quincenal", "Mensual", "Anual")) {
                onUpdate(item.copy(notasDiseno = it))
            }
        } else {
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

            if (item.tipoLente in setOf("Bifocal", "Multifocal", "Ocupacional")) {
                OptoTextField(value = item.altura, onValueChange = { onUpdate(item.copy(altura = it)) }, label = "Altura (mm)", keyboardType = KeyboardType.Decimal)
            }

            DropdownField(label = "Material del Lente", selected = item.materialLente, options = OpticalCatalog.MATERIALES) {
                onUpdate(item.copy(materialLente = it))
            }

            Text("Tratamientos", fontWeight = FontWeight.Bold, fontSize = 14.sp)

            if (item.tratamientos.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.tratamientos.forEach { trat ->
                        AssistChip(
                            onClick = { onUpdate(item.copy(tratamientos = item.tratamientos - trat)) },
                            label = { Text(trat, fontSize = 12.sp) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(16.dp)) },
                        )
                    }
                }
            }

            val available = OpticalCatalog.TRATAMIENTOS.filter { it !in item.tratamientos }
            if (available.isNotEmpty()) {
                var addExpanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { addExpanded = true },
                        label = { Text("+ Añadir", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                    DropdownMenu(expanded = addExpanded, onDismissRequest = { addExpanded = false }) {
                        available.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                onUpdate(item.copy(tratamientos = item.tratamientos + opt))
                                addExpanded = false
                            })
                        }
                    }
                }
            }

            OptoTextField(value = item.colorLente, onValueChange = { onUpdate(item.copy(colorLente = it)) }, label = "Color del Lente")
            OptoTextField(value = item.notasDiseno, onValueChange = { onUpdate(item.copy(notasDiseno = it)) }, label = "Notas de Diseño")

            if (item.tratamientos.contains("Filtro Discromatopsia")) {
                Text("Requiere evaluación previa: Ishihara + D-15 + prueba de filtro", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DropdownField(
                    label = "Tipo de Filtro Discromatopsia",
                    selected = item.filtroDiscromatopsiaTipo,
                    options = listOf(
                        "", "550 Rojo", "550 Rojo-Violeta", "550 Rojo-Marrón", "585 nm", "600 Rojo",
                        "Amarillo 450 nm (tritan)", "Amarillo 500 nm (tritan)",
                        "EnChroma Indoor", "EnChroma Outdoor",
                        "Pilestone A: rojo-verde leve/moderado", "Pilestone B: rojo-verde fuerte", "Pilestone C: interior",
                        "Pilestone D: protan (deficiencia roja)", "Pilestone E: tritan (azul-amarillo)",
                    ),
                ) { selected -> onUpdate(item.copy(filtroDiscromatopsiaTipo = selected)) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Montura", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            DropdownField(label = "Origen", selected = item.origenMontura, options = listOf("Tienda", "Paciente")) {
                if (it == "Tienda") {
                    onUpdate(item.copy(origenMontura = it))
                } else {
                    onUpdate(item.copy(origenMontura = it, monturaId = ""))
                }
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

                val filteredMonturas = if (monturaQuery.isBlank()) {
                    monturasActivas
                } else {
                    monturasActivas.filter {
                        it.marca.contains(monturaQuery, ignoreCase = true) ||
                            it.modelo.contains(monturaQuery, ignoreCase = true) ||
                            it.sku.contains(monturaQuery, ignoreCase = true)
                    }
                }

                ExposedDropdownMenuBox(expanded = expanded && filteredMonturas.isNotEmpty(), onExpandedChange = { expanded = it }) {
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded && filteredMonturas.isNotEmpty(), onDismissRequest = { expanded = false }) {
                        filteredMonturas.forEach { montura ->
                            DropdownMenuItem(
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${montura.marca} ${montura.modelo}", fontWeight = FontWeight.Bold)
                                            Text("SKU: ${montura.sku} | ${montura.color}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text("Stock: ${montura.stockActual}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                onClick = {
                                    monturaQuery = "${montura.marca} ${montura.modelo}"
                                    onUpdate(item.copy(monturaId = montura.id, tipoAro = montura.tipoAro, materialMontura = montura.materialMontura))
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }

            DropdownField(label = "Tipo de Aro", selected = item.tipoAro, options = OpticalCatalog.TIPO_ARO.keys.toList()) {
                onUpdate(item.copy(tipoAro = it))
            }
            DropdownField(label = "Material de la Montura", selected = item.materialMontura, options = listOf("Acetato", "Metal", "Carey", "TR-90", "Econ")) {
                onUpdate(item.copy(materialMontura = it))
            }
            OptoTextField(value = item.descripcionMontura, onValueChange = { onUpdate(item.copy(descripcionMontura = it)) }, label = "Descripción (Marca, Modelo)")
        } // end else (!Lentes de Contacto)
    }
}
