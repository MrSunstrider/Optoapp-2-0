package com.example.optoapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.optoapp.data.Montura
import com.example.optoapp.data.Pago
import com.example.optoapp.ui.components.AbonoDialog
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.FechaEntregaEditButton
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.DispensacionUiState
import com.example.optoapp.viewmodel.RegaloDispensacionUi
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonturaInfoSection(
    uiState: DispensacionUiState,
    monturasActivas: List<Montura>,
    onUpdate: (DispensacionUiState) -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Información de Montura", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            com.example.optoapp.ui.components.OptoDropdownMenuField(label = "Origen", selected = uiState.origenMontura, options = listOf("Tienda", "Paciente"), onSelected = {
                if (it == "Tienda") {
                    onUpdate(uiState.copy(origenMontura = it))
                } else {
                    onUpdate(uiState.copy(origenMontura = it, monturaId = ""))
                }
            })
            if (uiState.origenMontura == "Tienda" || uiState.origenMontura == "Nueva de Tienda") {
                val monturaSeleccionada = monturasActivas.firstOrNull { it.id == uiState.monturaId }
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

                ExposedDropdownMenuBox(
                    expanded = expanded && filteredMonturas.isNotEmpty(),
                    onExpandedChange = { expanded = it },
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded && filteredMonturas.isNotEmpty(),
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
                                    monturaQuery = "${montura.marca} ${montura.modelo}"
                                    onUpdate(
                                        uiState.copy(
                                            monturaId = montura.id,
                                            tipoAro = montura.tipoAro,
                                            materialMontura = montura.materialMontura,
                                        ),
                                    )
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            com.example.optoapp.ui.components.OptoDropdownMenuField(label = "Tipo de Aro", selected = uiState.tipoAro, options = listOf("Aro Completo", "Semi al aire", "Al aire"), onSelected = {
                onUpdate(uiState.copy(tipoAro = it))
            })
            com.example.optoapp.ui.components.OptoDropdownMenuField(label = "Material", selected = uiState.materialMontura, options = listOf("Acetato", "Metal", "Carey", "TR-90", "Econ"), onSelected = {
                onUpdate(uiState.copy(materialMontura = it))
            })
            OptoTextField(value = uiState.descripcionMontura, onValueChange = { onUpdate(uiState.copy(descripcionMontura = it)) }, label = "Descripción (Marca, Modelo)")
        }
    }
}

@Deprecated(
    message = "Use InformacionFinancieraScreen instead",
    replaceWith = ReplaceWith("InformacionFinancieraScreen(navController, dispensacionId)"),
)
@Composable
fun FinancieraInfoSection(
    uiState: DispensacionUiState,
    onUpdate: (DispensacionUiState) -> Unit,
    onAddPago: (Pago) -> Unit,
    onUpdatePago: (Pago) -> Unit,
    onRemovePago: (Pago) -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Información Financiera", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            OptoTextField(
                value = uiState.montoTotal,
                onValueChange = { onUpdate(uiState.copy(montoTotal = it)) },
                label = "Monto Total",
                keyboardType = KeyboardType.Decimal,
            )

            HorizontalDivider()

            Text("Historial de Abonos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            val total = uiState.montoTotal.toDoubleOrNull() ?: 0.0
            val pagado = uiState.pagos.sumOf { it.monto }
            val saldo = total - pagado

            uiState.pagos.forEach { pago ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
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
                                val totalActual = uiState.montoTotal.toDoubleOrNull() ?: 0.0
                                val otrosAbonos = uiState.pagos
                                    .filter { it.id != pago.id }
                                    .sumOf { it.monto }
                                val maximo = (totalActual - otrosAbonos).coerceAtLeast(0.0)
                                AbonoDialog(
                                    pago = pago,
                                    montoMaximo = maximo,
                                    onDismiss = { showEditDialog = false },
                                    onConfirm = { updatedPago: Pago ->
                                        onUpdatePago(updatedPago)
                                        showEditDialog = false
                                    },
                                )
                            }
                            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onRemovePago(pago) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            var showAddDialog by remember { mutableStateOf(false) }
            if (showAddDialog) {
                val totalActual = uiState.montoTotal.toDoubleOrNull() ?: 0.0
                val pagadoActual = uiState.pagos.sumOf { it.monto }
                val maximo = (totalActual - pagadoActual).coerceAtLeast(0.0)
                AbonoDialog(
                    defaultFecha = DateUtils.today(),
                    montoMaximo = maximo,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { nuevoPago: Pago ->
                        onAddPago(nuevoPago)
                        showAddDialog = false
                    },
                )
            }

            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
                Spacer(Modifier.width(8.dp))
                Text("Agregar Abono")
            }

            HorizontalDivider()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("SALDO RESTANTE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                Text(
                    text = "s/. " + formattedSaldo,
                    color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            com.example.optoapp.ui.components.OptoDropdownMenuField(label = "Estado de Entrega", selected = uiState.estadoEntrega, options = listOf("Pendiente", "Entregado"), onSelected = { newEstado ->
                val newFechaEntrega = when (newEstado) {
                    "Entregado" -> uiState.fechaEntrega ?: LocalDate.now()
                    else -> null
                }
                onUpdate(uiState.copy(estadoEntrega = newEstado, fechaEntrega = newFechaEntrega))
            })
            if (uiState.fechaEntrega != null) {
                FechaEntregaEditButton(
                    fechaEntrega = uiState.fechaEntrega,
                    onFechaChanged = { nuevaFecha ->
                        onUpdate(uiState.copy(fechaEntrega = nuevaFecha))
                    },
                )
            } else {
                TextButton(onClick = {
                    onUpdate(uiState.copy(fechaEntrega = LocalDate.now()))
                }) {
                    Text("Asignar fecha de entrega", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegalosSection(
    uiState: DispensacionUiState,
    monturas: List<Montura>,
    onAddRegalo: (RegaloDispensacionUi) -> Unit,
    onRemoveRegalo: (Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedMonturaId by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var motivo by remember { mutableStateOf("") }
    val selectedMontura = monturas.find { it.id == selectedMonturaId }

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Regalos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            uiState.regalos.forEachIndexed { index, regalo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                regalo.descripcion.ifBlank { "Producto sin nombre" },
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onRemoveRegalo(index) }, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar regalo",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Cantidad: ${regalo.cantidad}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Costo: S/. ${String.format(Locale.getDefault(), "%.2f", regalo.costoUnitario)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        if (regalo.motivo.isNotBlank()) {
                            Text(
                                "Motivo: ${regalo.motivo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
                Spacer(Modifier.width(8.dp))
                Text("Agregar Regalo")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Agregar Regalo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Producto search
                    var query by remember { mutableStateOf("") }
                    var showResults by remember { mutableStateOf(false) }

                    val filtered = if (query.isBlank()) {
                        emptyList()
                    } else {
                        monturas.filter {
                            it.marca.contains(query, ignoreCase = true) ||
                                it.modelo.contains(query, ignoreCase = true) ||
                                it.sku.contains(query, ignoreCase = true)
                        }
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            showResults = it.isNotBlank()
                            if (it.isEmpty()) selectedMonturaId = ""
                        },
                        label = { Text("Buscar producto") },
                        placeholder = { Text("Marca, modelo o SKU...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (showResults && filtered.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                            LazyColumn {
                                items(filtered) { m ->
                                    Text(
                                        text = "${m.marca} ${m.modelo} ${m.color} (Stock: ${m.stockActual})".trim(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedMonturaId = m.id
                                                query = "${m.marca} ${m.modelo} ${m.color}".trim()
                                                showResults = false
                                            }
                                            .padding(12.dp),
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }

                    if (query.isNotBlank() && filtered.isEmpty() && showResults) {
                        Text("Sin resultados", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }

                    // Cantidad
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it.filter { c -> c.isDigit() } },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Motivo
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Motivo") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Costo (auto-filled, no editable)
                    if (selectedMontura != null) {
                        Text(
                            "Costo unitario: S/. ${String.format(Locale.getDefault(), "%.2f", selectedMontura.costo)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedMontura != null && cantidad.toIntOrNull() != null && cantidad.toInt() > 0) {
                            onAddRegalo(
                                RegaloDispensacionUi(
                                    productoId = selectedMontura!!.id,
                                    descripcion = "${selectedMontura!!.marca} ${selectedMontura!!.modelo} ${selectedMontura!!.color}".trim(),
                                    cantidad = cantidad.toInt(),
                                    costoUnitario = selectedMontura!!.costo,
                                    motivo = motivo,
                                ),
                            )
                            showDialog = false
                            selectedMonturaId = ""
                            cantidad = "1"
                            motivo = ""
                        }
                    },
                    enabled = selectedMontura != null && cantidad.toIntOrNull() != null && cantidad.toInt() > 0,
                ) { Text("Agregar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            },
        )
    }
}
