package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.EvaluacionUiState

@Composable
fun ContactologiaSection(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    aplicarRecorteOd: Boolean,
    aplicarRecorteOi: Boolean,
    onRecorteOdChange: (Boolean) -> Unit,
    onRecorteOiChange: (Boolean) -> Unit,
    onShowLcDatePicker: () -> Unit
) {
    QueratometriaCard(uiState, onUpdate)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Sugerencias y Cálculos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            SuggestionCard(uiState.k1Od, uiState.k2Od, uiState.k1Oi, uiState.k2Oi)

            CalculationCard("Auto-Cálculo OD", uiState.recetaOdEsf, uiState.recetaOdCil, aplicarRecorteOd, onRecorteOdChange, { esf, cil ->
                onUpdate(uiState.copy(lcOdEsf = esf, lcOdCil = cil))
            })

            CalculationCard("Auto-Cálculo OI", uiState.recetaOiEsf, uiState.recetaOiCil, aplicarRecorteOi, onRecorteOiChange, { esf, cil ->
                onUpdate(uiState.copy(lcOiEsf = esf, lcOiCil = cil))
            })
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Prueba / Adaptación Final", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Text("Poder del Lente", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.lcOdEsf, onValueChange = { onUpdate(uiState.copy(lcOdEsf = it)) }, label = "OD Esf", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.lcOdCil, onValueChange = { onUpdate(uiState.copy(lcOdCil = it)) }, label = "OD Cil", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.lcOdEje, onValueChange = { onUpdate(uiState.copy(lcOdEje = it)) }, label = "OD Eje", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.lcOiEsf, onValueChange = { onUpdate(uiState.copy(lcOiEsf = it)) }, label = "OI Esf", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.lcOiCil, onValueChange = { onUpdate(uiState.copy(lcOiCil = it)) }, label = "OI Cil", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.lcOiEje, onValueChange = { onUpdate(uiState.copy(lcOiEje = it)) }, label = "OI Eje", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Parámetros Físicos", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.lcRadioBaseOd, onValueChange = { onUpdate(uiState.copy(lcRadioBaseOd = it)) }, label = "Curva Base (CB) OD", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.lcRadioBaseOi, onValueChange = { onUpdate(uiState.copy(lcRadioBaseOi = it)) }, label = "Curva Base (CB) OI", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.lcOdDia, onValueChange = { onUpdate(uiState.copy(lcOdDia = it)) }, label = "DIA OD", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.lcOiDia, onValueChange = { onUpdate(uiState.copy(lcOiDia = it)) }, label = "DIA OI", modifier = Modifier.weight(1f))
            }

            OptoTextField(value = uiState.lcLaboratorio, onValueChange = { onUpdate(uiState.copy(lcLaboratorio = it)) }, label = "Laboratorio / Marca")
            DropdownField(label = "Tipo de Lente", selected = uiState.lcTipoLente, options = com.example.optoapp.ui.screens.tiposLC, onSelected = { onUpdate(uiState.copy(lcTipoLente = it)) })
            DropdownField(label = "Material", selected = uiState.lcMaterial, options = com.example.optoapp.ui.screens.materialesLC, onSelected = { onUpdate(uiState.copy(lcMaterial = it)) })

            OutlinedButton(onClick = onShowLcDatePicker, modifier = Modifier.fillMaxWidth()) {
                val dText = uiState.lcFechaAdaptacion?.let { DateUtils.formatLocalized(it) } ?: "Seleccionar Fecha"
                Text("Fecha Adaptación: $dText")
            }
            OptoTextField(value = uiState.lcObservaciones, onValueChange = { onUpdate(uiState.copy(lcObservaciones = it)) }, label = "Notas Contactología")
        }
    }
}

@Composable
private fun QueratometriaCard(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Queratometría", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OD", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                OptoTextField(value = uiState.k1Od, onValueChange = { onUpdate(uiState.copy(k1Od = it)) }, label = "K1", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.k2Od, onValueChange = { onUpdate(uiState.copy(k2Od = it)) }, label = "K2", modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OI", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                OptoTextField(value = uiState.k1Oi, onValueChange = { onUpdate(uiState.copy(k1Oi = it)) }, label = "K1", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.k2Oi, onValueChange = { onUpdate(uiState.copy(k2Oi = it)) }, label = "K2", modifier = Modifier.weight(1f))
            }
        }
    }
}
