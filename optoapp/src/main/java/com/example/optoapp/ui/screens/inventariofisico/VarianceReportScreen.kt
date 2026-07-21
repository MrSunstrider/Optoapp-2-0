package com.example.optoapp.ui.screens.inventariofisico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.InventarioFisico
import com.example.optoapp.data.InventarioFisicoDetalle

@Composable
fun VarianceReportScreen(
    session: InventarioFisico,
    detalles: List<InventarioFisicoDetalle>,
    onBack: () -> Unit,
) {
    val withDiffs = detalles.filter { it.diferencia != null && it.diferencia != 0 }
    val totalDiff = withDiffs.sumOf { it.diferencia ?: 0 }
    val variants = withDiffs.size

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Reporte de diferencias",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onBack) { Text("Volver") }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryChip("Variantes", "$variants")
            SummaryChip("Total dif", if (totalDiff > 0) "+$totalDiff" else "$totalDiff")
            SummaryChip("Estado", session.estado)
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text("Montura", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("Sistema", Modifier.weight(0.7f), fontWeight = FontWeight.SemiBold)
            Text("Contado", Modifier.weight(0.7f), fontWeight = FontWeight.SemiBold)
            Text("Dif", Modifier.weight(0.5f), fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(withDiffs) { d ->
                val diff = d.diferencia ?: 0
                val rowColor = when {
                    diff > 0 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    diff < 0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                }

                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = rowColor),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(d.monturaId, Modifier.weight(1f))
                        Text("${d.stockSistema}", Modifier.weight(0.7f))
                        Text("${d.stockContado}", Modifier.weight(0.7f))
                        Text(
                            text = if (diff > 0) "+$diff" else "$diff",
                            modifier = Modifier.weight(0.5f),
                            color = when {
                                diff > 0 -> MaterialTheme.colorScheme.tertiary
                                diff < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (withDiffs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Sin diferencias — el stock físico coincide con el sistema",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
