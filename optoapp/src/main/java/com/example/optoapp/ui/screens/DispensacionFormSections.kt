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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.data.Montura
import com.example.optoapp.data.Pago
import com.example.optoapp.domain.OrderStatusPolicy
import com.example.optoapp.domain.estadoAfterFechaEntrega
import com.example.optoapp.ui.components.AbonoDialog
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.FechaEntregaEditButton
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.theme.saldoColor
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.DispensacionUiState
import com.example.optoapp.viewmodel.RegaloDispensacionUi
import java.time.LocalDate

@Composable
fun MonturaInfoSection(
    uiState: DispensacionUiState,
    monturasActivas: List<Montura>,
    onUpdate: (DispensacionUiState) -> Unit,
) {
    com.example.optoapp.ui.components.dispensacion.MonturaForm(
        uiState = uiState,
        onUpdate = onUpdate,
        monturasActivas = monturasActivas,
    )
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
            Text(stringResource(R.string.disp_informacion_financiera), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            OptoTextField(
                value = uiState.montoTotal,
                onValueChange = { onUpdate(uiState.copy(montoTotal = it)) },
                label = stringResource(R.string.pagos_monto_total),
                keyboardType = KeyboardType.Decimal,
            )

            HorizontalDivider()

            Text(stringResource(R.string.pagos_historial_abonos), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

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
                            Text(
                                stringResource(R.string.pagos_abono_linea, pago.metodoPago, MoneyFormatter.format(pago.monto)),
                                fontWeight = FontWeight.Bold,
                            )
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
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.pagos_editar_cd), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onRemovePago(pago) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.pagos_borrar_cd), tint = MaterialTheme.colorScheme.error)
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
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pagos_agregar_abono))
            }

            HorizontalDivider()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.pagos_saldo_restante_titulo),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = MoneyFormatter.format(saldo),
                    color = saldoColor(saldo),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            com.example.optoapp.ui.components.OptoDropdownMenuField(
                label = stringResource(R.string.pagos_estado_entrega),
                selected = uiState.estadoEntrega,
                options = OrderStatusPolicy.selectableEstados(uiState.estadoEntrega),
                enabled = OrderStatusPolicy.isEditable(uiState.estadoEntrega),
                onSelected = { newEstado ->
                    val newFechaEntrega = when (newEstado) {
                        "Entregado" -> uiState.fechaEntrega ?: LocalDate.now()
                        else -> null
                    }
                    onUpdate(uiState.copy(estadoEntrega = newEstado, fechaEntrega = newFechaEntrega))
                },
            )
            if (uiState.fechaEntrega != null) {
                FechaEntregaEditButton(
                    fechaEntrega = uiState.fechaEntrega,
                    onFechaChanged = { nuevaFecha ->
                        onUpdate(
                            uiState.copy(
                                fechaEntrega = nuevaFecha,
                                estadoEntrega = estadoAfterFechaEntrega(uiState.estadoEntrega, nuevaFecha),
                            ),
                        )
                    },
                )
            } else {
                TextButton(onClick = {
                    val fecha = LocalDate.now()
                    onUpdate(
                        uiState.copy(
                            fechaEntrega = fecha,
                            estadoEntrega = estadoAfterFechaEntrega(uiState.estadoEntrega, fecha),
                        ),
                    )
                }) {
                    Text(stringResource(R.string.disp_asignar_fecha_entrega), fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegalosSection(
    regalos: List<RegaloDispensacionUi>,
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
            Text(stringResource(R.string.disp_form_regalos), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            regalos.forEachIndexed { index, regalo ->
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
                                regalo.descripcion.ifBlank { stringResource(R.string.disp_form_regalo_sin_nombre) },
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onRemoveRegalo(index) }, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.disp_form_eliminar_regalo_cd),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.disp_form_regalo_cantidad, regalo.cantidad),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                stringResource(R.string.disp_form_regalo_costo, MoneyFormatter.format(regalo.costoUnitario)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        if (regalo.motivo.isNotBlank()) {
                            Text(
                                stringResource(R.string.disp_form_regalo_motivo, regalo.motivo),
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
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.disp_form_agregar_regalo))
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.disp_form_agregar_regalo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        label = { Text(stringResource(R.string.disp_form_buscar_producto)) },
                        placeholder = { Text(stringResource(R.string.disp_form_buscar_producto_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (showResults && filtered.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                            LazyColumn {
                                items(filtered) { m ->
                                    Text(
                                        text = stringResource(
                                            R.string.disp_form_resultado_stock,
                                            "${m.marca} ${m.modelo} ${m.color}".trim(),
                                            m.stockActual,
                                        ),
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
                        Text(stringResource(R.string.common_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }

                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.disp_form_cantidad)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text(stringResource(R.string.disp_form_motivo)) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (selectedMontura != null) {
                        Text(
                            stringResource(R.string.disp_form_costo_unitario, MoneyFormatter.format(selectedMontura.costo)),
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
                ) { Text(stringResource(R.string.disp_form_agregar)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}
