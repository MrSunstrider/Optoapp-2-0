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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.InventarioFisico
import com.example.optoapp.data.InventarioFisicoDetalle
import com.example.optoapp.viewmodel.MonturaConteoLabel

@Composable
fun MonturaScanScreen(
    session: InventarioFisico,
    detalles: List<InventarioFisicoDetalle>,
    labelsByMonturaId: Map<String, MonturaConteoLabel>,
    progressMessage: String?,
    onUpdateStock: (String, Int) -> Unit,
    onClose: () -> Unit,
    onBack: () -> Unit,
) {
    val canClose = session.estado == "EN_PROGRESO"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Conteo de monturas · ${session.fecha}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Anota las unidades físicas en vitrina / almacén",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            itemsIndexed(detalles, key = { _, d -> d.id }) { _, detalle ->
                DetalleRow(
                    detalle = detalle,
                    label = labelsByMonturaId[detalle.monturaId],
                    onUpdate = { stock -> onUpdateStock(detalle.id, stock) },
                )
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
                    text = "Conteo cerrado — solo consulta",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DetalleRow(
    detalle: InventarioFisicoDetalle,
    label: MonturaConteoLabel?,
    onUpdate: (Int) -> Unit,
) {
    var stockText by remember(detalle.id, detalle.stockContado) {
        mutableStateOf(detalle.stockContado?.toString() ?: "")
    }
    val titulo = label?.titulo ?: "Montura"
    val subtitulo = label?.subtitulo ?: "SKU no encontrado · ${detalle.monturaId.take(8)}…"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Sistema: ${detalle.stockSistema}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                detalle.diferencia?.let { diff ->
                    val color = when {
                        diff > 0 -> MaterialTheme.colorScheme.tertiary
                        diff < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = "Diferencia: ${if (diff > 0) "+$diff" else "$diff"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            OutlinedTextField(
                value = stockText,
                onValueChange = { v ->
                    stockText = v
                    v.toIntOrNull()?.let { onUpdate(it) }
                },
                modifier = Modifier.width(88.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                label = { Text("Físico") },
                enabled = detalle.stockContado == null || stockText == detalle.stockContado.toString(),
            )
        }
    }
}
