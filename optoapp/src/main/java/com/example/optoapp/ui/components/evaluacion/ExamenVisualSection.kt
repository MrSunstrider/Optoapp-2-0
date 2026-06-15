package com.example.optoapp.ui.components.evaluacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.EvaluacionUiState

@Composable
fun ExamenVisualSection(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    onShowOsdiDialog: () -> Unit
) {
    var showAvSc by remember { mutableStateOf(false) }
    var showAvCc by remember { mutableStateOf(false) }
    var showVisionBinocular by remember { mutableStateOf(false) }
    var showColor by remember { mutableStateOf(false) }
    var showSaludOcular by remember { mutableStateOf(false) }
    var showOtrasPruebas by remember { mutableStateOf(false) }

    CollapsibleExamenCard(
        title = "Agudeza Visual SIN corrección",
        expanded = showAvSc,
        onToggle = { showAvSc = it }
    ) {
        OptoTextField(
            value = uiState.avScAo,
            onValueChange = { onUpdate(uiState.copy(avScAo = it)) },
            label = "Ambos ojos"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptoTextField(value = uiState.avScOdLejos, onValueChange = { onUpdate(uiState.copy(avScOdLejos = it)) }, label = "OD", modifier = Modifier.weight(1f))
            OptoTextField(value = uiState.avScOiLejos, onValueChange = { onUpdate(uiState.copy(avScOiLejos = it)) }, label = "OI", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptoTextField(value = uiState.phOd, onValueChange = { onUpdate(uiState.copy(phOd = it)) }, label = "PH OD", modifier = Modifier.weight(1f))
            OptoTextField(value = uiState.phOi, onValueChange = { onUpdate(uiState.copy(phOi = it)) }, label = "PH OI", modifier = Modifier.weight(1f))
        }
    }

    CollapsibleExamenCard(
        title = "Agudeza Visual CON corrección PX",
        expanded = showAvCc,
        onToggle = { showAvCc = it }
    ) {
        OptoTextField(
            value = uiState.avCcAoPx,
            onValueChange = { onUpdate(uiState.copy(avCcAoPx = it)) },
            label = "Ambos ojos"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptoTextField(value = uiState.avCcOdLejos, onValueChange = { onUpdate(uiState.copy(avCcOdLejos = it)) }, label = "OD", modifier = Modifier.weight(1f))
            OptoTextField(value = uiState.avCcOiLejos, onValueChange = { onUpdate(uiState.copy(avCcOiLejos = it)) }, label = "OI", modifier = Modifier.weight(1f))
        }
    }

    CollapsibleExamenCard(
        title = "Visión Binocular y Percepción",
        expanded = showVisionBinocular,
        onToggle = { showVisionBinocular = it }
    ) {
        DropdownField(label = "Estereopsis", selected = uiState.estereopsisValor, options = estereopsisOptions, onSelected = { onUpdate(uiState.copy(estereopsisValor = it)) })
        OptoTextField(value = uiState.estereopsisSegundos, onValueChange = { onUpdate(uiState.copy(estereopsisSegundos = it)) }, label = "Segundos de arco (opcional)")
        DropdownField(label = "Test de Lang", selected = uiState.lang, options = langOptions, onSelected = { onUpdate(uiState.copy(lang = it)) })
        DropdownField(label = "Test de Worth", selected = uiState.worth, options = worthOptions, onSelected = { onUpdate(uiState.copy(worth = it)) })
    }

    CollapsibleExamenCard(
        title = "Percepción del Color",
        expanded = showColor,
        onToggle = { showColor = it }
    ) {
        OptoTextField(value = uiState.ishihara, onValueChange = { onUpdate(uiState.copy(ishihara = it)) }, label = "Test de Ishihara")
        DropdownField(label = "Test de Farnsworth", selected = uiState.farnsworth, options = farnsworthOptions, onSelected = { onUpdate(uiState.copy(farnsworth = it)) })
    }

    CollapsibleExamenCard(
        title = "Salud de la Superficie Ocular y Función Visual",
        expanded = showSaludOcular,
        onToggle = { showSaludOcular = it }
    ) {
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

        DropdownField(label = "Sensibilidad al contraste", selected = uiState.sensibilidadContraste, options = sensibilidadOptions, onSelected = { onUpdate(uiState.copy(sensibilidadContraste = it)) })
        OptoTextField(value = uiState.sensibilidadFrecuencia, onValueChange = { onUpdate(uiState.copy(sensibilidadFrecuencia = it)) }, label = "Frecuencia espacial (opcional)")
    }

    CollapsibleExamenCard(
        title = "Otras Pruebas y Exámenes Previos",
        expanded = showOtrasPruebas,
        onToggle = { showOtrasPruebas = it }
    ) {
        OptoTextField(value = uiState.amsler, onValueChange = { onUpdate(uiState.copy(amsler = it)) }, label = "Test de Amsler")
        DropdownField(label = "Campo visual por confrontación", selected = uiState.campoVisual, options = campoVisualOptions, onSelected = { onUpdate(uiState.copy(campoVisual = it)) })
        if (uiState.campoVisual == "Anomalía detectada") {
            OptoTextField(value = uiState.campoVisualDescripcion, onValueChange = { onUpdate(uiState.copy(campoVisualDescripcion = it)) }, label = "Descripción de anomalía (Campo Visual)")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Exámenes Previos", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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

// ─── Collapsible Card ────────────────────────────────────────────────────────

/**
 * Tarjeta plegable con toggle No/Sí para mostrar/ocultar contenido.
 * Por defecto arranca cerrada (No). El usuario la expande tocando "Sí".
 */
@Composable
private fun CollapsibleExamenCard(
    title: String,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            // Header row: title + toggle No / Sí
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!expanded) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SwitchableLabel(
                        label = "No",
                        isActive = !expanded,
                        onClick = { onToggle(false) }
                    )
                    SwitchableLabel(
                        label = "Sí",
                        isActive = expanded,
                        onClick = { onToggle(true) }
                    )
                }
            }

            // Content: animated collapse
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SwitchableLabel(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Dropdown options ─────────────────────────────────────────────────────────

internal val estereopsisOptions = listOf("Normal", "Reducida", "Ausente")
internal val langOptions = listOf("Positivo", "Negativo")
internal val worthOptions = listOf("Fusión normal", "Supresión OD", "Supresión OI", "Diplopía")
internal val farnsworthOptions = listOf("Normal", "Deutan", "Protan", "Tritan")
internal val sensibilidadOptions = listOf("Normal", "Disminuida")
internal val campoVisualOptions = listOf("Normal", "Anomalía detectada")
