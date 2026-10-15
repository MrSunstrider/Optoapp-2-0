package com.example.optoapp.ui.components.dispensacion

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.data.Pago
import com.example.optoapp.domain.OrderStatusPolicy
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.AbonoDialog
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.theme.saldoColor
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.DispensacionUiState

@Composable
fun PagosSection(
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
                modifier = Modifier.testTag(TestTags.DISPENSACION_MONTO_TOTAL),
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
                                val otrosAbonos = uiState.pagos
                                    .filter { it.id != pago.id }
                                    .sumOf { it.monto }
                                val maximo = (total - otrosAbonos).coerceAtLeast(0.0)
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
                val pagadoActual = uiState.pagos.sumOf { it.monto }
                val maximo = (total - pagadoActual).coerceAtLeast(0.0)
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

            OptoDropdownMenuField(
                label = stringResource(R.string.pagos_estado_entrega),
                selected = uiState.estadoEntrega,
                options = OrderStatusPolicy.selectableEstados(uiState.estadoEntrega),
                enabled = OrderStatusPolicy.isEditable(uiState.estadoEntrega),
            ) {
                onUpdate(uiState.copy(estadoEntrega = it))
            }
        }
    }
}
