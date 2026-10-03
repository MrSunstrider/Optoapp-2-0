package com.example.optoapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.domain.OrderStatusPolicy
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.ui.theme.warningAmber

enum class OrderEstadoTone { POSITIVE, ALERT, WARNING, NEUTRAL, CLAIMED }

data class OrderEstadoChipStyle(val label: String, val tone: OrderEstadoTone)

fun orderEstadoChipStyle(estado: String, saldo: Double? = null, servicio: Boolean = false): OrderEstadoChipStyle =
    when (val normalized = estado.trim()) {
        OrderStatusPolicy.ENTREGADO -> OrderEstadoChipStyle(normalized, OrderEstadoTone.POSITIVE)
        OrderStatusPolicy.PENDIENTE -> OrderEstadoChipStyle(
            normalized,
            if (saldo != null && saldo > 0) OrderEstadoTone.ALERT else OrderEstadoTone.WARNING,
        )
        OrderStatusPolicy.ANULADO -> OrderEstadoChipStyle(if (servicio) "Anulado" else "Anulada", OrderEstadoTone.NEUTRAL)
        OrderStatusPolicy.RECLAMADA -> OrderEstadoChipStyle(normalized, OrderEstadoTone.CLAIMED)
        else -> OrderEstadoChipStyle(normalized, OrderEstadoTone.NEUTRAL)
    }

@Composable
fun orderEstadoColor(tone: OrderEstadoTone): Color = when (tone) {
    OrderEstadoTone.POSITIVE -> MaterialTheme.colorScheme.positiveGreen
    OrderEstadoTone.ALERT -> MaterialTheme.colorScheme.alertRed
    OrderEstadoTone.WARNING, OrderEstadoTone.CLAIMED -> MaterialTheme.colorScheme.warningAmber
    OrderEstadoTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun orderEstadoColor(estado: String, saldo: Double? = null): Color = orderEstadoColor(orderEstadoChipStyle(estado, saldo).tone)

@Composable
fun OrderEstadoChip(
    estado: String,
    modifier: Modifier = Modifier,
    saldo: Double? = null,
    servicio: Boolean = false,
) {
    val style = orderEstadoChipStyle(estado, saldo, servicio)
    val color = orderEstadoColor(style.tone)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = if (style.tone == OrderEstadoTone.CLAIMED) BorderStroke(1.dp, color) else null,
    ) {
        Text(
            style.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}
