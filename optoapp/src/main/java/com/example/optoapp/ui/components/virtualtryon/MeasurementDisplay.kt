package com.example.optoapp.ui.components.virtualtryon

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.viewmodel.FaceMeasurements

/**
 * Displays extracted face measurements (DIP, DNP, segment height).
 *
 * Shows a warning icon when PD was estimated using default fallback (63mm).
 *
 * @param measurements The extracted face measurements to display
 * @param showPdwarning Whether to show a warning about default PD fallback
 * @param modifier Modifier for the composable
 */
@Composable
fun MeasurementDisplay(
    measurements: FaceMeasurements,
    showPdwarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Mediciones",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MeasurementItem(
                    label = "DIP",
                    value = "DIP: %.1f mm".format(measurements.dipMm)
                )
                MeasurementItem(
                    label = "DNP OD",
                    value = "DNP OD: %.1f mm".format(measurements.dnpOdMm)
                )
                MeasurementItem(
                    label = "DNP OI",
                    value = "DNP OI: %.1f mm".format(measurements.dnpOiMm)
                )
                MeasurementItem(
                    label = "Altura",
                    value = measurements.segmentHeight?.let {
                        "Altura: %.1f mm".format(it)
                    } ?: "Altura: —"
                )
            }

            // PD warning
            if (showPdwarning) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Advertencia",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PD estimado (63mm por defecto). Verificá con datos reales de la evaluación.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single measurement value display.
 */
@Composable
private fun MeasurementItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value.substringAfter(": "),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
