package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.viewmodel.EvaluacionUiState

@Composable
fun ExamenVisualSection(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    onShowOsdiDialog: () -> Unit,
) {
    Text("Agudeza Visual SIN corrección", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    OptoTextField(
        value = uiState.avScAo,
        onValueChange = { onUpdate(uiState.copy(avScAo = it)) },
        label = "Ambos ojos",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(value = uiState.avScOdLejos, onValueChange = { onUpdate(uiState.copy(avScOdLejos = it)) }, label = "OD", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.avScOiLejos, onValueChange = { onUpdate(uiState.copy(avScOiLejos = it)) }, label = "OI", modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("Agudeza Visual CON corrección PX", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    OptoTextField(
        value = uiState.avCcAoPx,
        onValueChange = { onUpdate(uiState.copy(avCcAoPx = it)) },
        label = "Ambos ojos",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(value = uiState.avCcOdLejos, onValueChange = { onUpdate(uiState.copy(avCcOdLejos = it)) }, label = "OD", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.avCcOiLejos, onValueChange = { onUpdate(uiState.copy(avCcOiLejos = it)) }, label = "OI", modifier = Modifier.weight(1f))
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    VisionBinocularCard(uiState, onUpdate)
    ColorPerceptionCard(uiState, onUpdate)
    SaludOcularCard(uiState, onUpdate, onShowOsdiDialog)
    OtrasPruebasCard(uiState, onUpdate)
}

@Composable
private fun VisionBinocularCard(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Visión Binocular y Percepción", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            DropdownField(label = "Estereopsis", selected = uiState.estereopsisValor, options = com.example.optoapp.ui.screens.estereopsisOptions, onSelected = { onUpdate(uiState.copy(estereopsisValor = it)) })
            OptoTextField(value = uiState.estereopsisSegundos, onValueChange = { onUpdate(uiState.copy(estereopsisSegundos = it)) }, label = "Segundos de arco (opcional)")
            DropdownField(label = "Test de Lang", selected = uiState.lang, options = com.example.optoapp.ui.screens.langOptions, onSelected = { onUpdate(uiState.copy(lang = it)) })
            DropdownField(label = "Test de Worth", selected = uiState.worth, options = com.example.optoapp.ui.screens.worthOptions, onSelected = { onUpdate(uiState.copy(worth = it)) })
        }
    }
}

@Composable
private fun ColorPerceptionCard(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Percepción del Color", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            OptoTextField(value = uiState.ishihara, onValueChange = { onUpdate(uiState.copy(ishihara = it)) }, label = "Test de Ishihara")
            DropdownField(label = "Test de Farnsworth", selected = uiState.farnsworth, options = com.example.optoapp.ui.screens.farnsworthOptions, onSelected = { onUpdate(uiState.copy(farnsworth = it)) })
        }
    }
}

@Composable
private fun SaludOcularCard(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    onShowOsdiDialog: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Salud de la Superficie Ocular y Función Visual", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Test de Schirmer (mm)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.schirmerOd, onValueChange = { onUpdate(uiState.copy(schirmerOd = it)) }, label = "OD", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.schirmerOi, onValueChange = { onUpdate(uiState.copy(schirmerOi = it)) }, label = "OI", modifier = Modifier.weight(1f))
            }

            Text("Test OSDI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onShowOsdiDialog, modifier = Modifier.weight(1f)) {
                    Text("Realizar test OSDI")
                }
                if (uiState.osdiPuntuacion != null) {
                    Text("${uiState.osdiPuntuacion} - ${uiState.osdiClasificacion}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            DropdownField(label = "Sensibilidad al contraste", selected = uiState.sensibilidadContraste, options = com.example.optoapp.ui.screens.sensibilidadOptions, onSelected = { onUpdate(uiState.copy(sensibilidadContraste = it)) })
            OptoTextField(value = uiState.sensibilidadFrecuencia, onValueChange = { onUpdate(uiState.copy(sensibilidadFrecuencia = it)) }, label = "Frecuencia espacial (opcional)")
        }
    }
}

@Composable
private fun OtrasPruebasCard(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Otras Pruebas y Exámenes Previos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            OptoTextField(value = uiState.amsler, onValueChange = { onUpdate(uiState.copy(amsler = it)) }, label = "Test de Amsler")
            DropdownField(label = "Campo visual por confrontación", selected = uiState.campoVisual, options = com.example.optoapp.ui.screens.campoVisualOptions, onSelected = { onUpdate(uiState.copy(campoVisual = it)) })
            if (uiState.campoVisual == "Anomalía detectada") {
                OptoTextField(value = uiState.campoVisualDescripcion, onValueChange = { onUpdate(uiState.copy(campoVisualDescripcion = it)) }, label = "Descripción de anomalía (Campo Visual)")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Exámenes Previos", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.phOd, onValueChange = { onUpdate(uiState.copy(phOd = it)) }, label = "PH OD", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.phOi, onValueChange = { onUpdate(uiState.copy(phOi = it)) }, label = "PH OI", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.kappaOd, onValueChange = { onUpdate(uiState.copy(kappaOd = it)) }, label = "Kappa OD", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.kappaOi, onValueChange = { onUpdate(uiState.copy(kappaOi = it)) }, label = "Kappa OI", modifier = Modifier.weight(1f))
            }
            OptoTextField(value = uiState.hirshberg, onValueChange = { onUpdate(uiState.copy(hirshberg = it)) }, label = "Hirshberg")

            Text("Ducciones:", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.duccionesOd, onValueChange = { onUpdate(uiState.copy(duccionesOd = it)) }, label = "OD", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.duccionesOi, onValueChange = { onUpdate(uiState.copy(duccionesOi = it)) }, label = "OI", modifier = Modifier.weight(1f))
            }
            OptoTextField(value = uiState.versionesAo, onValueChange = { onUpdate(uiState.copy(versionesAo = it)) }, label = "Versiones Ambos Ojos")

            Text("Cover Test", color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.coverTest6m, onValueChange = { onUpdate(uiState.copy(coverTest6m = it)) }, label = "6m", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.coverTest40cm, onValueChange = { onUpdate(uiState.copy(coverTest40cm = it)) }, label = "40cm", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.coverTest10cm, onValueChange = { onUpdate(uiState.copy(coverTest10cm = it)) }, label = "10cm", modifier = Modifier.weight(1f))
            }

            Text("PPC", color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.ppcOr, onValueChange = { onUpdate(uiState.copy(ppcOr = it)) }, label = "OR", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.ppcLuz, onValueChange = { onUpdate(uiState.copy(ppcLuz = it)) }, label = "Luz", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.ppcFrl, onValueChange = { onUpdate(uiState.copy(ppcFrl = it)) }, label = "FR + L", modifier = Modifier.weight(1f))
            }

            Text("Reflejos Pupilares", color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.reflejoFotomotor, onValueChange = { onUpdate(uiState.copy(reflejoFotomotor = it)) }, label = "Fotomotor", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.reflejoConsensual, onValueChange = { onUpdate(uiState.copy(reflejoConsensual = it)) }, label = "Consensual", modifier = Modifier.weight(1f))
            }
            OptoTextField(value = uiState.reflejoAcomodativo, onValueChange = { onUpdate(uiState.copy(reflejoAcomodativo = it)) }, label = "Acomodativo")
        }
    }
}
