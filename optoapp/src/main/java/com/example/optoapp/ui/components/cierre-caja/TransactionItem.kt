package com.example.optoapp.ui.components.cierre_caja

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.Pago
import com.example.optoapp.util.DateUtils
import java.util.Locale

internal fun transactionLabel(pago: Pago): String = when {
    pago.dispensacionId != null -> "Dispensación"
    pago.servicioExtraId != null -> "Servicio Extra"
    else -> "Pago"
}

internal fun transactionDisplayLabel(
    pago: Pago,
    customLabel: String,
    tipoEntidad: String = transactionLabel(pago),
): String = customLabel.ifBlank { tipoEntidad.ifBlank { transactionLabel(pago) } }

@Composable
fun TransactionItem(
    pago: Pago,
    label: String? = null,
    tipoEntidad: String? = null,
    esCobroAtrasado: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val resolvedTipo = tipoEntidad ?: transactionLabel(pago)
    val title = transactionDisplayLabel(pago, customLabel = label.orEmpty(), tipoEntidad = resolvedTipo)
    val modifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(
                    text = resolvedTipo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = pago.metodoPago.ifBlank { pago.tipo },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = DateUtils.formatLocalized(pago.fecha),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (esCobroAtrasado) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Cobro atrasado", fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                        modifier = Modifier.height(24.dp),
                    )
                }
                if (pago.nota.isNotEmpty()) {
                    Text(pago.nota, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "s/. ${String.format(Locale.getDefault(), "%,.2f", pago.monto)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    pago.monto < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.tertiary
                },
            )
        }
    }
}
