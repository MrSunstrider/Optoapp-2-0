package com.example.optoapp.ui.components.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.domain.ConfiguracionFinancieraDraft
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.viewmodel.ConfiguracionFinancieraUiState

@Composable
fun ConfiguracionFinancieraSection(
    uiState: ConfiguracionFinancieraUiState,
    onDraftChange: (ConfiguracionFinancieraDraft) -> Unit,
    onSave: () -> Unit,
) {
    val draft = uiState.draft
    Card(
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = OptoTokens.elevation.level1),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Umbrales financieros",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Alertas y objetivos usados por recomendaciones e indicadores.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OptoTextField(
                value = draft.margenNetoObjetivo.toString(),
                onValueChange = {
                    onDraftChange(draft.copy(margenNetoObjetivo = it.toDoubleOrNull() ?: draft.margenNetoObjetivo))
                },
                label = "Margen neto objetivo (%)",
                enabled = uiState.saveEnabled || uiState.saving,
            )
            OptoTextField(
                value = draft.ticketPromedioObjetivo?.toString().orEmpty(),
                onValueChange = {
                    onDraftChange(
                        draft.copy(ticketPromedioObjetivo = it.toDoubleOrNull()),
                    )
                },
                label = "Ticket promedio objetivo",
                enabled = uiState.saveEnabled || uiState.saving,
            )
            OptoTextField(
                value = draft.caidaVentasAlertaPct.toString(),
                onValueChange = {
                    onDraftChange(draft.copy(caidaVentasAlertaPct = it.toDoubleOrNull() ?: draft.caidaVentasAlertaPct))
                },
                label = "Caída ventas alerta (%)",
                enabled = uiState.saveEnabled || uiState.saving,
            )
            OptoTextField(
                value = draft.deudaViejaAlertaDias.toString(),
                onValueChange = {
                    onDraftChange(draft.copy(deudaViejaAlertaDias = it.toIntOrNull() ?: draft.deudaViejaAlertaDias))
                },
                label = "Deuda vieja alerta (días)",
                enabled = uiState.saveEnabled || uiState.saving,
            )
            OptoTextField(
                value = draft.deudaTotalAlertaMonto.toString(),
                onValueChange = {
                    onDraftChange(draft.copy(deudaTotalAlertaMonto = it.toDoubleOrNull() ?: draft.deudaTotalAlertaMonto))
                },
                label = "Deuda total alerta (monto)",
                enabled = uiState.saveEnabled || uiState.saving,
            )
            OptoTextField(
                value = draft.stockEstancadoAlertaDias.toString(),
                onValueChange = {
                    onDraftChange(
                        draft.copy(stockEstancadoAlertaDias = it.toIntOrNull() ?: draft.stockEstancadoAlertaDias),
                    )
                },
                label = "Stock estancado alerta (días)",
                enabled = uiState.saveEnabled || uiState.saving,
            )
            OptoTextField(
                value = draft.stockBajoAlertaUnidades.toString(),
                onValueChange = {
                    onDraftChange(
                        draft.copy(stockBajoAlertaUnidades = it.toIntOrNull() ?: draft.stockBajoAlertaUnidades),
                    )
                },
                label = "Stock bajo alerta (unidades)",
                enabled = uiState.saveEnabled || uiState.saving,
            )
            OptoTextField(
                value = draft.minVentasParaRecomendar.toString(),
                onValueChange = {
                    onDraftChange(
                        draft.copy(minVentasParaRecomendar = it.toIntOrNull() ?: draft.minVentasParaRecomendar),
                    )
                },
                label = "Mín. ventas para recomendar",
                enabled = uiState.saveEnabled || uiState.saving,
            )
            OptoTextField(
                value = draft.frecuenciaRecalculoDias.toString(),
                onValueChange = {
                    onDraftChange(
                        draft.copy(frecuenciaRecalculoDias = it.toIntOrNull() ?: draft.frecuenciaRecalculoDias),
                    )
                },
                label = "Frecuencia recálculo (días)",
                enabled = uiState.saveEnabled || uiState.saving,
            )
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            Button(
                onClick = onSave,
                enabled = uiState.saveEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar umbrales")
                }
            }
        }
    }
}
