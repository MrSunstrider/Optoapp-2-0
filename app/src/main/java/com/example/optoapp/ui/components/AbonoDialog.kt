package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Pago
import java.util.*

@Composable
fun AbonoDialog(pago: Pago? = null, onDismiss: () -> Unit, onConfirm: (Pago) -> Unit) {
    var monto by remember { mutableStateOf(pago?.monto?.toString() ?: "") }
    var metodo by remember { mutableStateOf(pago?.metodoPago ?: "Efectivo") }
    var nota by remember { mutableStateOf(pago?.nota ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pago == null) "Nuevo Abono" else "Editar Abono") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = "Monto",
                    keyboardType = KeyboardType.Decimal
                )
                DropdownField(
                    label = "Método de Pago",
                    selected = metodo,
                    options = listOf("Efectivo", "Tarjeta", "Transferencia"),
                    onSelected = { metodo = it }
                )
                OptoTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = "Nota (Ejem: Yape, Visa, etc.)"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = monto.toDoubleOrNull() ?: 0.0
                if (m > 0) {
                    onConfirm(
                        pago?.copy(monto = m, metodoPago = metodo, nota = nota)
                            ?: Pago(
                                id = UUID.randomUUID().toString(),
                                monto = m,
                                metodoPago = metodo,
                                nota = nota,
                                fecha = System.currentTimeMillis(),
                                tipo = "Abono"
                            )
                    )
                }
            }) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
