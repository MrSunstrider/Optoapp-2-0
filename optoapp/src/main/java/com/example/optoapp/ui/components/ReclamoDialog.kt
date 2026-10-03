package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.optoapp.domain.MONEY_EPSILON
import com.example.optoapp.util.MontoDraftFormatting
import kotlin.math.max

private val METODOS_REEMBOLSO_BASE = listOf("Efectivo", "Tarjeta", "Transferencia")

/**
 * The replacement receives the original's net paid as credit; only the excess over the new total
 * is refunded to the patient, and a new total above that credit leaves a balance to collect.
 */
data class ReclamoPreview(
    val creditoTransferido: Double,
    val nuevoTotal: Double?,
    val reembolso: Double,
    val saldoReemplazo: Double,
) {
    val muestraMetodoReembolso: Boolean get() = reembolso > 0.0
}

fun reclamoPreview(creditoTransferido: Double, nuevoTotalInput: String): ReclamoPreview {
    val credito = max(creditoTransferido, 0.0)
    val total = nuevoTotalInput.trim().replace(",", ".").toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
        ?: return ReclamoPreview(credito, nuevoTotal = null, reembolso = 0.0, saldoReemplazo = 0.0)
    return ReclamoPreview(
        creditoTransferido = credito,
        nuevoTotal = total,
        reembolso = (credito - total).takeIf { it > MONEY_EPSILON } ?: 0.0,
        saldoReemplazo = max(total - credito, 0.0),
    )
}

fun canConfirmReclamo(motivo: String, preview: ReclamoPreview, metodoReembolso: String, submitting: Boolean): Boolean =
    canConfirmMotivo(motivo, submitting) && preview.nuevoTotal != null &&
        (!preview.muestraMetodoReembolso || metodoReembolso.isNotBlank())

fun metodosReembolso(sugerido: String): List<String> =
    if (sugerido.isBlank() || sugerido in METODOS_REEMBOLSO_BASE) METODOS_REEMBOLSO_BASE
    else METODOS_REEMBOLSO_BASE + sugerido

@Composable
fun ReclamoDialog(
    ot: String,
    montoTotalOriginal: Double,
    creditoTransferido: Double,
    metodoReembolsoSugerido: String,
    submitting: Boolean,
    onConfirm: (motivo: String, nuevoMontoTotal: Double, metodoReembolso: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nuevoTotalInput by rememberSaveable { mutableStateOf(MontoDraftFormatting.formatDraft(montoTotalOriginal)) }
    var metodo by rememberSaveable(metodoReembolsoSugerido) { mutableStateOf(metodoReembolsoSugerido) }
    val preview = remember(creditoTransferido, nuevoTotalInput) { reclamoPreview(creditoTransferido, nuevoTotalInput) }

    MotivoDialog(
        title = "¿Reclamar garantía de OT $ot?",
        confirmText = "Reclamar",
        message = "La orden queda como Reclamada y se crea una orden de reemplazo con el crédito pagado. No se puede deshacer.",
        submitting = submitting,
        onConfirm = { motivo ->
            val total = preview.nuevoTotal
            if (total != null && canConfirmReclamo(motivo, preview, metodo, submitting)) onConfirm(motivo, total, metodo)
        },
        onDismiss = onDismiss,
    ) {
        OptoTextField(
            value = nuevoTotalInput,
            onValueChange = { nuevoTotalInput = it },
            label = "Nuevo monto total *",
            keyboardType = KeyboardType.Decimal,
            isError = preview.nuevoTotal == null,
            enabled = !submitting,
        )
        ReclamoPreviewSummary(preview)
        if (preview.muestraMetodoReembolso) {
            MetodoReembolsoField(
                metodo = metodo,
                opciones = metodosReembolso(metodoReembolsoSugerido),
                enabled = !submitting,
                onMetodoChange = { metodo = it },
            )
        }
    }
}

@Composable
private fun ReclamoPreviewSummary(preview: ReclamoPreview) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (preview.nuevoTotal == null) {
            Text("Ingresa un monto válido (0 o mayor).", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        PreviewRow("Crédito transferido", preview.creditoTransferido)
        if (preview.muestraMetodoReembolso) PreviewRow("Reembolso al paciente", preview.reembolso, highlight = true)
        if (preview.saldoReemplazo > 0.0) PreviewRow("Saldo del reemplazo", preview.saldoReemplazo)
    }
}

@Composable
private fun PreviewRow(label: String, monto: Double, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            "s/. ${"%.2f".format(monto)}",
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MetodoReembolsoField(
    metodo: String,
    opciones: List<String>,
    enabled: Boolean,
    onMetodoChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = metodo,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            isError = metodo.isBlank(),
            label = { Text("Método de reembolso *") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion, fontWeight = if (opcion == metodo) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = {
                        onMetodoChange(opcion)
                        expanded = false
                    },
                )
            }
        }
    }
}
