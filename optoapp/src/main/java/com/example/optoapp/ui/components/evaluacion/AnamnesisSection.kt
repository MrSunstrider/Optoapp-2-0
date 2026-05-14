package com.example.optoapp.ui.components.evaluacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.EvaluacionUiState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text

@Composable
fun AnamnesisSection(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    onShowDatePicker: () -> Unit
) {
    OutlinedButton(onClick = onShowDatePicker, modifier = Modifier.fillMaxWidth()) {
        Text("Fecha Registro: ${DateUtils.formatLocalized(uiState.fecha)}")
    }
    OptoTextField(value = uiState.motivoConsulta, onValueChange = { onUpdate(uiState.copy(motivoConsulta = it)) }, label = "Motivo de consulta")
    OptoTextField(value = uiState.sintomas, onValueChange = { onUpdate(uiState.copy(sintomas = it)) }, label = "Síntomas y signos")

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text("Antecedentes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

    OptoTextField(value = uiState.antecedentesPersonalesOculares, onValueChange = { onUpdate(uiState.copy(antecedentesPersonalesOculares = it)) }, label = "Pers. Oculares")
    OptoTextField(value = uiState.antecedentesPersonalesSistemicos, onValueChange = { onUpdate(uiState.copy(antecedentesPersonalesSistemicos = it)) }, label = "Pers. Sistémicos")
    OptoTextField(value = uiState.antecedentesFamiliaresOculares, onValueChange = { onUpdate(uiState.copy(antecedentesFamiliaresOculares = it)) }, label = "Fam. Oculares")
    OptoTextField(value = uiState.antecedentesFamiliaresSistemicos, onValueChange = { onUpdate(uiState.copy(antecedentesFamiliaresSistemicos = it)) }, label = "Fam. Sistémicos")

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(value = uiState.medicacion, onValueChange = { onUpdate(uiState.copy(medicacion = it)) }, label = "Medicación", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.alergias, onValueChange = { onUpdate(uiState.copy(alergias = it)) }, label = "Alergias", modifier = Modifier.weight(1f))
    }
}
