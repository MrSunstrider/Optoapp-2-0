package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
fun CierreSection(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    onShowProximaDatePicker: () -> Unit,
    onSave: () -> Unit,
    evaluacionId: String?
) {
    Text("Diagnóstico y Plan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

    DiagnosticoCard(uiState, onUpdate)
    TratamientoCard(uiState, onUpdate)
    CitaCard(uiState, onUpdate, onShowProximaDatePicker)

    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Check, null)
        Spacer(Modifier.width(8.dp))
        Text(if (evaluacionId == null) "Guardar Evaluación" else "Actualizar Evaluación", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DiagnosticoCard(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Diagnóstico", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    DropdownField(
                        label = "Diagnóstico OD",
                        selected = uiState.diagnosticoOd.firstOrNull() ?: "",
                        options = com.example.optoapp.ui.screens.diagnosticosRefraccion,
                        onSelected = { onUpdate(uiState.copy(diagnosticoOd = listOf(it), balanceOd = it == "Balance")) }
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.balanceOd, onCheckedChange = { onUpdate(uiState.copy(balanceOd = it)) })
                    Text("Balance", fontSize = 12.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    DropdownField(
                        label = "Diagnóstico OI",
                        selected = uiState.diagnosticoOi.firstOrNull() ?: "",
                        options = com.example.optoapp.ui.screens.diagnosticosRefraccion,
                        onSelected = { onUpdate(uiState.copy(diagnosticoOi = listOf(it), balanceOi = it == "Balance")) }
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.balanceOi, onCheckedChange = { onUpdate(uiState.copy(balanceOi = it)) })
                    Text("Balance", fontSize = 12.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Otros Diagnósticos", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

            listOf(
                Triple("Presbicia", uiState.otrosPresbicia, uiState.autoPresbicia) to { v: Boolean -> onUpdate(uiState.copy(otrosPresbicia = v, autoPresbicia = false)) },
                Triple("Anisometropía", uiState.otrosAnisometropia, uiState.autoAnisometropia) to { v: Boolean -> onUpdate(uiState.copy(otrosAnisometropia = v, autoAnisometropia = false)) },
                Triple("Ambliopía", uiState.otrosAmbliopia, uiState.autoAmbliopia) to { v: Boolean -> onUpdate(uiState.copy(otrosAmbliopia = v, autoAmbliopia = false)) }
            ).forEach { (triple, onChecked) ->
                val (label, checked, auto) = triple
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked, onCheckedChange = onChecked)
                    Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    TextButton(onClick = { onUpdate(uiState.copy(autoPresbicia = !auto)) }) {
                        Text(if (auto) "Auto" else "Man", fontSize = 10.sp, color = if (auto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TratamientoCard(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tratamiento", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            OptoTextField(value = uiState.planTratamiento, onValueChange = { onUpdate(uiState.copy(planTratamiento = it)) }, label = "Plan de Tratamiento")
            OptoTextField(value = uiState.observaciones, onValueChange = { onUpdate(uiState.copy(observaciones = it)) }, label = "Observaciones Adicionales Generales")
        }
    }
}

@Composable
private fun CitaCard(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    onShowProximaDatePicker: () -> Unit
) {
    OutlinedButton(onClick = onShowProximaDatePicker, modifier = Modifier.fillMaxWidth()) {
        val labelText = uiState.proximaCita?.let { "Próxima Cita: ${DateUtils.formatLocalized(it)}" }
            ?: "Programar Próxima Cita"
        Text(labelText)
    }
    if (uiState.proximaCita != null) {
        val estadosCitaOpciones = listOf(
            "programada" to "Programada",
            "confirmada" to "Confirmada",
            "asistio" to "Asistió",
            "no_asistio" to "No asistió",
            "reprogramada" to "Reprogramada"
        )
        val labelSeleccionado = estadosCitaOpciones
            .find { it.first == uiState.citaEstado.ifBlank { "programada" } }
            ?.second
            ?: estadosCitaOpciones.first().second
        DropdownField(
            label = "Estado de la cita",
            selected = labelSeleccionado,
            options = estadosCitaOpciones.map { it.second },
            onSelected = { etiqueta ->
                val codigo = estadosCitaOpciones.find { it.second == etiqueta }?.first ?: "programada"
                onUpdate(uiState.copy(citaEstado = codigo))
            }
        )
    }
}
