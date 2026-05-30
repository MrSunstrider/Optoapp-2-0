package com.example.optoapp.ui.components.virtualtryon

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.optoapp.viewmodel.TryOnState

/**
 * Result screen with save, share, and measurement save actions.
 *
 * Buttons are enabled once the composite overlay is rendered.
 *
 * @param state The [TryOnState.OverlayReady] holding the composited bitmap
 * @param onSaveClick Saves composite to device gallery (shows snackbar feedback via parent)
 * @param onShareClick Saves (if needed) then fires share intent
 * @param onSaveMeasurementsClick Persists DIP/DNP/height to the patient's evaluation record
 * @param onRetryClick Resets state to [TryOnState.Idle] for a new photo
 */
@Composable
fun ResultScreen(
    state: TryOnState.OverlayReady,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveMeasurementsClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Measurement summary
        MeasurementDisplay(
            measurements = state.measurements,
            showPdwarning = false,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Save to gallery
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar en galería", fontWeight = FontWeight.SemiBold)
            }

            // Share
            OutlinedButton(
                onClick = onShareClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save measurements to evaluation
        OutlinedButton(
            onClick = onSaveMeasurementsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guardar mediciones", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Retry with a different photo
        TextButton(onClick = onRetryClick) {
            Text("Probar con otra foto")
        }
    }
}
