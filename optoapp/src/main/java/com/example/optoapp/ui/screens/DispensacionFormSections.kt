package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.Montura
import com.example.optoapp.data.Pago
import com.example.optoapp.ui.components.AbonoDialog
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.DispensacionUiState
import java.util.*

@Composable
fun LenteInfoSection(
    uiState: DispensacionUiState,
    onUpdate: (DispensacionUiState) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Información del Lente", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            DropdownField(label = "Tipo de Lente", selected = uiState.tipoLente, options = listOf("Monofocal", "Bifocal", "Progresivo", "Ocupacional")) {
                val base = uiState.copy(tipoLente = it)
                val cleaned = when (it) {
                    "Bifocal" -> base.copy(distanciaLente = "", altura = "")
                    "Monofocal" -> base.copy(subTipoBifocal = "", altura = "")
                    "Progresivo", "Ocupacional" -> base.copy(subTipoBifocal = "", distanciaLente = "")
                    else -> base.copy(subTipoBifocal = "", distanciaLente = "", altura = "")
                }
                onUpdate(cleaned)
            }

            if (uiState.tipoLente == "Bifocal") {
                DropdownField(label = "Sub-tipo Bifocal", selected = uiState.subTipoBifocal, options = listOf("Flaptop", "Invisible")) {
                    onUpdate(uiState.copy(subTipoBifocal = it))
                }
            }

            if (uiState.tipoLente == "Monofocal") {
                DropdownField(label = "Distancia", selected = uiState.distanciaLente, options = listOf("Lejos", "Intermedia", "Cerca")) {
                    onUpdate(uiState.copy(distanciaLente = it))
                }
            }

            if (uiState.tipoLente == "Bifocal" || uiState.tipoLente == "Progresivo" || uiState.tipoLente == "Ocupacional") {
                OptoTextField(
                    value = uiState.altura,
                    onValueChange = { onUpdate(uiState.copy(altura = it)) },
                    label = "Altura (mm)",
                    keyboardType = KeyboardType.Decimal
                )
            }

            DropdownField(label = "Material", selected = uiState.materialLente, options = listOf("Resina", "Policarbonato", "Cristal", "Trivex")) {
                onUpdate(uiState.copy(materialLente = it))
            }
            Text("Tratamientos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            val opts = listOf("Ninguno", "Antireflejo", "Antirayas", "Filtro UV 400", "Fotocromático", "AR Blue Defense")

            val currentTrats = if (uiState.tratamientos.isEmpty()) listOf("Ninguno") else uiState.tratamientos + "Ninguno"

            currentTrats.distinct().forEachIndexed { index, selectedValue ->
                if (index == 0 || currentTrats[index - 1] != "Ninguno") {
                    DropdownField(
                        label = if (index == 0) "Tratamiento Principal" else "Tratamiento Adicional",
                        selected = selectedValue,
                        options = opts
                    ) { selected ->
                        val newList = uiState.tratamientos.toMutableList()
                        if (index < uiState.tratamientos.size) {
                            if (selected == "Ninguno") newList.removeAt(index)
                            else newList[index] = selected
                        } else if (selected != "Ninguno") {
                            newList.add(selected)
                        }
                        onUpdate(uiState.copy(tratamientos = newList.distinct().filter { t -> t != "Ninguno" }))
                    }
                }
            }
            OptoTextField(value = uiState.colorLente, onValueChange = { onUpdate(uiState.copy(colorLente = it)) }, label = "Color")
            OptoTextField(value = uiState.notasDiseno, onValueChange = { onUpdate(uiState.copy(notasDiseno = it)) }, label = "Notas de Diseño")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonturaInfoSection(
    uiState: DispensacionUiState,
    monturasActivas: List<Montura>,
    onUpdate: (DispensacionUiState) -> Unit
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

@Composable
fun FinancieraInfoSection(
    uiState: DispensacionUiState,
    onUpdate: (DispensacionUiState) -> Unit,
    onAddPago: (Pago) -> Unit,
    onUpdatePago: (Pago) -> Unit,
    onRemovePago: (Pago) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Información Financiera", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            OptoTextField(
                value = uiState.montoTotal,
                onValueChange = { onUpdate(uiState.copy(montoTotal = it)) },
                label = "Monto Total",
                keyboardType = KeyboardType.Decimal
            )

            HorizontalDivider()

            Text("Historial de Abonos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            val total = uiState.montoTotal.toDoubleOrNull() ?: 0.0
            val pagado = uiState.pagos.sumOf { it.monto }
            val saldo = total - pagado

            uiState.pagos.forEach { pago ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${pago.metodoPago}: s/. ${String.format(Locale.getDefault(), "%.2f", pago.monto)}", fontWeight = FontWeight.Bold)
                            if (pago.nota.isNotEmpty()) {
                                Text(pago.nota, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(DateUtils.formatLocalized(pago.fecha), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            var showEditDialog by remember { mutableStateOf(false) }
                            if (showEditDialog) {
                                AbonoDialog(
                                    pago = pago,
                                    onDismiss = { showEditDialog = false },
                                    onConfirm = { updatedPago: Pago ->
                                        val totalActual = uiState.montoTotal.toDoubleOrNull() ?: 0.0
                                        val otrosAbonos = uiState.pagos
                                            .filter { it.id != pago.id }
                                            .sumOf { it.monto }
                                        if (totalActual <= 0.0) {
                                            onUpdate(uiState.copy(error = FinanzasRemoteDefaults.Messages.MONTO_TOTAL_INVALIDO))
                                            return@AbonoDialog
                                        }
                                        if (otrosAbonos + updatedPago.monto > totalActual) {
                                            onUpdate(uiState.copy(error = FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL))
                                            return@AbonoDialog
                                        }
                                        onUpdatePago(updatedPago)
                                        showEditDialog = false
                                    }
                                )
                            }
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onRemovePago(pago) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            var showAddDialog by remember { mutableStateOf(false) }
            if (showAddDialog) {
                AbonoDialog(
                    defaultFecha = DateUtils.today(),
                    onDismiss = { showAddDialog = false },
                    onConfirm = { nuevoPago: Pago ->
                        val totalActual = uiState.montoTotal.toDoubleOrNull() ?: 0.0
                        val totalConNuevo = uiState.pagos.sumOf { it.monto } + nuevoPago.monto
                        if (totalActual <= 0.0) {
                            onUpdate(uiState.copy(error = FinanzasRemoteDefaults.Messages.MONTO_TOTAL_INVALIDO))
                            return@AbonoDialog
                        }
                        if (totalConNuevo > totalActual) {
                            onUpdate(uiState.copy(error = FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL))
                            return@AbonoDialog
                        }
                        onAddPago(nuevoPago)
                        showAddDialog = false
                    }
                )
            }

            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar Abono")
            }

            HorizontalDivider()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SALDO RESTANTE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                Text(
                    text = "s/. " + formattedSaldo,
                    color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            DropdownField(label = "Estado de Entrega", selected = uiState.estadoEntrega, options = listOf("Pendiente", "Entregado")) {
                onUpdate(uiState.copy(estadoEntrega = it))
            }
        }
    }
}
