package com.example.optoapp.ui.components.financiera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.Pago
import com.example.optoapp.ui.components.AbonoDialog
import com.example.optoapp.util.DateUtils
import java.util.Locale

enum class SaldoDisplayStyle { Hero, Card }

@Composable
fun FinancieraPagosSection(
    state: PagosSectionState,
    onAddPago: (Pago) -> Unit,
    onUpdatePago: (Pago) -> Unit,
    onRemovePago: (Pago) -> Unit,
    modifier: Modifier = Modifier,
    saldoStyle: SaldoDisplayStyle = SaldoDisplayStyle.Hero,
) {
    var pagoEnEdicion by remember { mutableStateOf<Pago?>(null) }
    var mostrarNuevoAbono by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Historial de Abonos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        state.pagos.forEach { pago ->
            PagoRow(
                pago = pago,
                onEdit = { pagoEnEdicion = pago },
                onRemove = { onRemovePago(pago) },
            )
        }

        OutlinedButton(
            onClick = { mostrarNuevoAbono = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar")
            Spacer(Modifier.width(8.dp))
            Text("Agregar Abono")
        }

        HorizontalDivider()

        when (saldoStyle) {
            SaldoDisplayStyle.Hero -> SaldoHero(state.saldo)
            SaldoDisplayStyle.Card -> SaldoCard(state.saldo)
        }
    }

    pagoEnEdicion?.let { pago ->
        AbonoDialog(
            pago = pago,
            montoMaximo = state.montoMaximoParaEdicion(pago.id),
            onDismiss = { pagoEnEdicion = null },
            onConfirm = { actualizado ->
                onUpdatePago(actualizado)
                pagoEnEdicion = null
            },
        )
    }

    if (mostrarNuevoAbono) {
        AbonoDialog(
            defaultFecha = DateUtils.today(),
            montoMaximo = state.montoMaximoParaNuevo(),
            onDismiss = { mostrarNuevoAbono = false },
            onConfirm = { nuevo ->
                onAddPago(nuevo)
                mostrarNuevoAbono = false
            },
        )
    }
}

@Composable
private fun PagoRow(
    pago: Pago,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
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
                    "${pago.metodoPago}: s/. ${formatMonto(pago.monto)}",
                    fontWeight = FontWeight.Bold,
                )
                if (pago.nota.isNotEmpty()) {
                    Text(pago.nota, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    DateUtils.formatLocalized(pago.fecha),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SaldoHero(saldo: Double) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "SALDO RESTANTE",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "s/. ${formatMonto(saldo)}",
            color = saldoColor(saldo),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun SaldoCard(saldo: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Saldo Restante:", fontWeight = FontWeight.Bold)
            Text(
                text = "s/. ${formatMonto(saldo)}",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = saldoColor(saldo),
            )
        }
    }
}

@Composable
private fun saldoColor(saldo: Double) =
    if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

private fun formatMonto(monto: Double): String = String.format(Locale.getDefault(), "%.2f", monto)
