package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.domain.OrderStatusPolicy
import com.example.optoapp.util.DateUtils
import java.time.LocalDate

fun readOnlyBannerTitle(estado: String, servicio: Boolean = false): String = when {
    servicio -> "Servicio anulado"
    estado.trim() == OrderStatusPolicy.RECLAMADA -> "Orden reclamada"
    else -> "Orden anulada"
}

fun readOnlyBannerDetail(motivo: String?, fecha: LocalDate?): String {
    val motivoText = motivo?.trim()?.takeIf { it.isNotEmpty() }?.let { "Motivo: $it" } ?: "Sin motivo registrado"
    return if (fecha == null) motivoText else "$motivoText · ${DateUtils.formatLocalized(fecha)}"
}

fun reemplazoLinkLabel(ot: String): String =
    ot.trim().takeIf { it.isNotEmpty() }?.let { "Ver reemplazo OT $it" } ?: "Ver orden de reemplazo"

fun reclamoOrigenLinkLabel(ot: String): String =
    ot.trim().takeIf { it.isNotEmpty() }?.let { "Reclamo de OT $it" } ?: "Reclamo de la orden original"

@Composable
fun ClaimLinkButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun OrderReadOnlyBanner(
    estado: String,
    motivo: String?,
    fecha: LocalDate?,
    modifier: Modifier = Modifier,
    servicio: Boolean = false,
    link: (@Composable () -> Unit)? = null,
) {
    val color = orderEstadoColor(orderEstadoChipStyle(estado, servicio = servicio).tone)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(readOnlyBannerTitle(estado, servicio), fontWeight = FontWeight.Bold, color = color)
                    OrderEstadoChip(estado, servicio = servicio)
                }
                Text(readOnlyBannerDetail(motivo, fecha), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Solo lectura: no se puede editar ni registrar pagos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                link?.invoke()
            }
        }
    }
}
