package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.EvaluacionUiState

// ─── Tab 0: Anamnesis ──────────────────────────────────────────────────────

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

// ─── Tab 1: Examen Visual ──────────────────────────────────────────────────

@Composable
fun ExamenVisualSection(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    onShowOsdiDialog: () -> Unit
) {
    Text("Agudeza Visual SIN corrección", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    OptoTextField(
        value = uiState.avScAo,
        onValueChange = { onUpdate(uiState.copy(avScAo = it)) },
        label = "Ambos ojos"
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
        label = "Ambos ojos"
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
    onUpdate: (EvaluacionUiState) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
    onUpdate: (EvaluacionUiState) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
    onShowOsdiDialog: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
    onUpdate: (EvaluacionUiState) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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

// ─── Tab 2: Refracción ─────────────────────────────────────────────────────

@Composable
fun RefraccionSection(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    viewModel: com.example.optoapp.viewmodel.EvaluacionViewModel
) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text("Refracción Objetiva", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(value = uiState.objOdEsf, onValueChange = { onUpdate(uiState.copy(objOdEsf = it)) }, label = "OD Esf", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.objOdCil, onValueChange = { onUpdate(uiState.copy(objOdCil = it)) }, label = "OD Cil", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.objOdEje, onValueChange = { onUpdate(uiState.copy(objOdEje = it)) }, label = "OD Eje", modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(value = uiState.objOiEsf, onValueChange = { onUpdate(uiState.copy(objOiEsf = it)) }, label = "OI Esf", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.objOiCil, onValueChange = { onUpdate(uiState.copy(objOiCil = it)) }, label = "OI Cil", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.objOiEje, onValueChange = { onUpdate(uiState.copy(objOiEje = it)) }, label = "OI Eje", modifier = Modifier.weight(1f))
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text("Refracción Subjetiva", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(value = uiState.subjOdEsf, onValueChange = { onUpdate(uiState.copy(subjOdEsf = it)) }, label = "OD Esf", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.subjOdCil, onValueChange = { onUpdate(uiState.copy(subjOdCil = it)) }, label = "OD Cil", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.subjOdEje, onValueChange = { onUpdate(uiState.copy(subjOdEje = it)) }, label = "OD Eje", modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(value = uiState.subjOiEsf, onValueChange = { onUpdate(uiState.copy(subjOiEsf = it)) }, label = "OI Esf", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.subjOiCil, onValueChange = { onUpdate(uiState.copy(subjOiCil = it)) }, label = "OI Cil", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.subjOiEje, onValueChange = { onUpdate(uiState.copy(subjOiEje = it)) }, label = "OI Eje", modifier = Modifier.weight(1f))
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text("Fórmula final (gafas)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(
            value = uiState.recetaOdEsf,
            onValueChange = { onUpdate(uiState.copy(recetaOdEsf = it)) },
            label = "OD Esf",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") }
        )
        OptoTextField(
            value = uiState.recetaOdCil,
            onValueChange = { onUpdate(uiState.copy(recetaOdCil = it)) },
            label = "OD Cil",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") }
        )
        OptoTextField(
            value = uiState.recetaOdEje,
            onValueChange = { onUpdate(uiState.copy(recetaOdEje = it)) },
            label = "OD Eje",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") }
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(
            value = uiState.recetaOiEsf,
            onValueChange = { onUpdate(uiState.copy(recetaOiEsf = it)) },
            label = "OI Esf",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OI") }
        )
        OptoTextField(
            value = uiState.recetaOiCil,
            onValueChange = { onUpdate(uiState.copy(recetaOiCil = it)) },
            label = "OI Cil",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OI") }
        )
        OptoTextField(
            value = uiState.recetaOiEje,
            onValueChange = { onUpdate(uiState.copy(recetaOiEje = it)) },
            label = "OI Eje",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OI") }
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(value = uiState.recetaOdAv, onValueChange = { onUpdate(uiState.copy(recetaOdAv = it)) }, label = "AV OD", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.recetaOiAv, onValueChange = { onUpdate(uiState.copy(recetaOiAv = it)) }, label = "AV OI", modifier = Modifier.weight(1f))
        OptoTextField(value = uiState.avCcAoPx, onValueChange = { onUpdate(uiState.copy(avCcAoPx = it)) }, label = "AV AO", modifier = Modifier.weight(1f))
    }

    AddSection(uiState, onUpdate)
    DipSection(uiState, onUpdate)
    PrismasSection(uiState, onUpdate)
}

@Composable
private fun AddSection(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Adición (ADD)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text("A/O", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Switch(
                    checked = uiState.isAddAo,
                    onCheckedChange = { newVal -> onUpdate(uiState.copy(isAddAo = newVal)) },
                    modifier = Modifier.scale(0.8f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.addCercaOd, onValueChange = { onUpdate(uiState.copy(addCercaOd = it)) }, label = "Add OD", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.addCercaOi, onValueChange = { onUpdate(uiState.copy(addCercaOi = it)) }, label = "Add OI", modifier = Modifier.weight(1f), enabled = !uiState.isAddAo)
                OptoTextField(value = uiState.addAv, onValueChange = { onUpdate(uiState.copy(addAv = it)) }, label = "AV Add", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DipSection(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("DIP (Distancia Interpupilar)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.dipLejos, onValueChange = { onUpdate(uiState.copy(dipLejos = it)) }, label = "DIP Lejos", modifier = Modifier.weight(1f))
                OptoTextField(value = uiState.dipCerca, onValueChange = { onUpdate(uiState.copy(dipCerca = it)) }, label = "DIP Cerca", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PrismasSection(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Prismas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OptoTextField(value = uiState.prismaOdValor, onValueChange = { onUpdate(uiState.copy(prismaOdValor = it)) }, label = "Prisma OD", modifier = Modifier.weight(1f))
                Box(modifier = Modifier.weight(1f)) {
                    DropdownField(label = "Base", selected = uiState.prismaOdBase, options = com.example.optoapp.ui.screens.basesPrisma, onSelected = { onUpdate(uiState.copy(prismaOdBase = it)) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OptoTextField(value = uiState.prismaOiValor, onValueChange = { onUpdate(uiState.copy(prismaOiValor = it)) }, label = "Prisma OI", modifier = Modifier.weight(1f))
                Box(modifier = Modifier.weight(1f)) {
                    DropdownField(label = "Base", selected = uiState.prismaOiBase, options = com.example.optoapp.ui.screens.basesPrisma, onSelected = { onUpdate(uiState.copy(prismaOiBase = it)) })
                }
            }
        }
    }
}

// ─── Tab 3: Contactología ──────────────────────────────────────────────────

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

// ─── Tab 4: Cierre ─────────────────────────────────────────────────────────

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
        val labelText = if (uiState.proximaCita == null) "Programar Próxima Cita"
        else "Próxima Cita: ${DateUtils.formatLocalized(uiState.proximaCita!!)}"
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
