package com.example.optoapp.ui.components.evaluacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.EvaluacionUiState

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

    var antecedentesExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (antecedentesExpanded) 180f else 0f,
        label = "chevron"
    )

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { antecedentesExpanded = !antecedentesExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Antecedentes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (antecedentesExpanded) "Colapsar" else "Expandir",
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = antecedentesExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OptoTextField(value = uiState.antecedentesPersonalesOculares, onValueChange = { onUpdate(uiState.copy(antecedentesPersonalesOculares = it)) }, label = "Pers. Oculares")
                    OptoTextField(value = uiState.antecedentesPersonalesSistemicos, onValueChange = { onUpdate(uiState.copy(antecedentesPersonalesSistemicos = it)) }, label = "Pers. Sistémicos")
                    OptoTextField(value = uiState.antecedentesFamiliaresOculares, onValueChange = { onUpdate(uiState.copy(antecedentesFamiliaresOculares = it)) }, label = "Fam. Oculares")
                    OptoTextField(value = uiState.antecedentesFamiliaresSistemicos, onValueChange = { onUpdate(uiState.copy(antecedentesFamiliaresSistemicos = it)) }, label = "Fam. Sistémicos")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptoTextField(value = uiState.medicacion, onValueChange = { onUpdate(uiState.copy(medicacion = it)) }, label = "Medicación", modifier = Modifier.weight(1f))
                        OptoTextField(value = uiState.alergias, onValueChange = { onUpdate(uiState.copy(alergias = it)) }, label = "Alergias", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
