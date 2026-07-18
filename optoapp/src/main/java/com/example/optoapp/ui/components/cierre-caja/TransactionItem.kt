package com.example.optoapp.ui.components.cierre_caja

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.Pago
import java.util.Locale

internal fun transactionLabel(pago: Pago): String = when {
    pago.dispensacionId != null -> "Dispensación"
    pago.servicioExtraId != null -> "Servicio Extra"
    else -> "Pago"
}

@Composable
fun TransactionItem(pago: Pago) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transactionLabel(pago),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(pago.metodoPago, fontWeight = FontWeight.Bold)
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
                }
            )
        }
    }
}
