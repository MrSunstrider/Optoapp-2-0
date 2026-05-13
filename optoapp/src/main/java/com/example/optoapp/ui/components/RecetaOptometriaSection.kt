package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.viewmodel.EvaluacionUiState

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
