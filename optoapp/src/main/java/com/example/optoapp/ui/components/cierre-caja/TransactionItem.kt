package com.example.optoapp.ui.components.cierre_caja

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.Pago
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.ui.theme.LocalOptoDensity
import com.example.optoapp.viewmodel.PagoDisplayItem
import java.util.Locale

internal fun transactionLabel(pago: Pago): String = when {
    pago.dispensacionId != null -> "Dispensación"
    pago.servicioExtraId != null -> "Servicio Extra"
    else -> "Pago"
}

/** Amount shown in cobros list — must match hero/method cards (PagoEffect). */
internal fun transactionDisplayAmount(pago: Pago): Double =
    PagoEffect.signedAmount(pago.tipo, pago.monto)

internal fun formatTransactionAmount(signedAmount: Double): String =
    "s/. ${String.format(Locale.getDefault(), "%,.2f", signedAmount)}"

@Composable
fun TransactionItem(
    item: PagoDisplayItem,
    pacienteNombre: String = "",
    onClick: (() -> Unit)? = null,
) {
    val density = LocalOptoDensity.current
    val pago = item.pago
    val signedAmount = PagoEffect.signedAmount(pago.tipo, pago.monto)
    Card(
        modifier = Modifier.fillMaxWidth().let { m ->
            if (onClick != null) m.clickable(onClick = onClick) else m
        },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(density.listItemPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.tipoEntidad,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "· ${pago.metodoPago}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (pacienteNombre.isNotBlank()) {
                    Text(
                        pacienteNombre,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.esCobroAtrasado) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            "Cobro atrasado",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                if (pago.nota.isNotEmpty()) {
                    Text(pago.nota, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatTransactionAmount(signedAmount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        signedAmount < 0 -> MaterialTheme.colorScheme.error
                        signedAmount == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                )
                Text(
                    pago.tipo,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun TransactionItem(pago: Pago) {
    val item = PagoDisplayItem(
        pago = pago,
        label = transactionLabel(pago),
        tipoEntidad = transactionLabel(pago),
        esCobroAtrasado = false,
        dispensacionId = pago.dispensacionId,
        servicioExtraId = pago.servicioExtraId,
        pacienteId = null,
    )
    TransactionItem(item = item)
}
