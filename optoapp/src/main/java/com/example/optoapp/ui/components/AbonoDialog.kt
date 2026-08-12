package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Pago
import com.example.optoapp.util.DateUtils
import java.time.LocalDate
import java.util.UUID

/** Abono con fecha del movimiento de caja (no la fecha de la orden), para cierre por día correcto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbonoDialog(
    pago: Pago? = null,
    defaultFecha: LocalDate = DateUtils.today(),
    montoMaximo: Double = Double.MAX_VALUE,
    onDismiss: () -> Unit,
    onConfirm: (Pago) -> Unit,
) {
    // pago?.id is null for new abonos, causing all new-abono dialogs to share the same key
    // and leak state from a previous dismissed dialog. Generate a stable per-invocation key.
    val dialogKey = remember { pago?.id ?: UUID.randomUUID().toString() }

    var monto by remember(dialogKey) { mutableStateOf(pago?.monto?.toString() ?: "") }
    var metodo by remember(dialogKey) { mutableStateOf(pago?.metodoPago ?: "Efectivo") }
    var nota by remember(dialogKey) { mutableStateOf(pago?.nota ?: "") }
    var fechaAbono by remember(dialogKey) { mutableStateOf(pago?.fecha ?: defaultFecha) }
    var showDatePicker by remember(dialogKey) { mutableStateOf(false) }
    var showMetodoPicker by remember(dialogKey) { mutableStateOf(false) }
    var errorMsg by remember(dialogKey) { mutableStateOf<String?>(null) }
    val metodosPago = listOf("Efectivo", "Tarjeta", "Transferencia")

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(fechaAbono),
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        fechaAbono = DateUtils.pickerMillisToLocalDate(ms)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pago == null) "Nuevo Abono" else "Editar Abono") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Fecha", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Fecha del abono: ${DateUtils.formatLocalized(fechaAbono)}")
                }
                OptoTextField(
                    value = monto,
                    onValueChange = {
                        monto = it
                        errorMsg = null
                    },
                    label = "Monto",
                    keyboardType = KeyboardType.Decimal,
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = metodo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Método de Pago") },
                        trailingIcon = {
                            IconButton(onClick = { showMetodoPicker = true }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = showMetodoPicker,
                        onDismissRequest = { showMetodoPicker = false },
                    ) {
                        metodosPago.forEach { opt ->
                            val isSelected = opt == metodo
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    metodo = opt
                                    showMetodoPicker = false
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
                OptoTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = "Nota (Ejem: Yape, Visa, etc.)",
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = monto.toDoubleOrNull() ?: 0.0
                if (m <= 0) {
                    errorMsg = "El monto debe ser mayor a 0."
                    return@TextButton
                }
                if (m > montoMaximo) {
                    errorMsg = "El pago no puede ser mayor al saldo pendiente (s/. ${"%.2f".format(montoMaximo)})."
                    return@TextButton
                }
                onConfirm(
                    pago?.copy(
                        monto = m,
                        metodoPago = metodo,
                        nota = nota,
                        fecha = fechaAbono,
                    )
                        ?: Pago(
                            id = UUID.randomUUID().toString(),
                            monto = m,
                            metodoPago = metodo,
                            nota = nota,
                            fecha = fechaAbono,
                            tipo = "Abono",
                        ),
                )
            }) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
