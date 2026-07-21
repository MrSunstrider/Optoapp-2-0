package com.example.optoapp.ui.screens.inventariofisico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.InventarioFisico
import com.example.optoapp.data.InventarioFisicoDetalle

@Composable
fun MonturaScanScreen(
    session: InventarioFisico,
    detalles: List<InventarioFisicoDetalle>,
    progressMessage: String?,
    onUpdateStock: (String, Int) -> Unit,
    onClose: () -> Unit,
    onBack: () -> Unit,
) {
    val canClose = session.estado == "EN_PROGRESO"

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Conteo: ${session.fecha}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (progressMessage != null) {
                    Text(
                        text = progressMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            TextButton(onClick = onBack) { Text("Volver") }
        }

        Spacer(Modifier.height(8.dp))

        val counted = detalles.count { it.stockContado != null }
        val total = detalles.size
        LinearProgressIndicator(
            progress = { if (total > 0) counted.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(detalles) { _, detalle ->
                DetalleRow(detalle) { stock ->
                    onUpdateStock(detalle.id, stock)
                }
            }
        }

        if (canClose) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                enabled = counted > 0,
            ) {
                Text("Completar conteo y ajustar stock")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = "Conteo completado — solo lectura",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DetalleRow(detalle: InventarioFisicoDetalle, onUpdate: (Int) -> Unit) {
    var stockText by remember(detalle.id, detalle.stockContado) {
        mutableStateOf(detalle.stockContado?.toString() ?: "")
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = detalle.monturaId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Sistema: ${detalle.stockSistema}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                detalle.diferencia?.let { diff ->
                    val color = when {
                        diff > 0 -> MaterialTheme.colorScheme.tertiary
                        diff < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = "Dif: ${if (diff > 0) "+$diff" else "$diff"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                    )
                }
            }
            OutlinedTextField(
                value = stockText,
                onValueChange = { v ->
                    stockText = v
                    v.toIntOrNull()?.let { onUpdate(it) }
                },
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                label = { Text("Contado") },
                enabled = detalle.stockContado == null || stockText == detalle.stockContado.toString(),
            )
        }
    }
}
