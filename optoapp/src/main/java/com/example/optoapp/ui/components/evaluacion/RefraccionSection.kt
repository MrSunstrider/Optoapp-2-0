package com.example.optoapp.ui.components.evaluacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.EvaluacionUiState
import com.example.optoapp.viewmodel.EvaluacionViewModel

@Composable
fun RefraccionSection(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    viewModel: EvaluacionViewModel,
) {
    var refObjetivaExpanded by remember { mutableStateOf(false) }
    val chevronRefObjRotation by animateFloatAsState(
        targetValue = if (refObjetivaExpanded) 180f else 0f,
        label = "chevronRefObj",
    )

    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { refObjetivaExpanded = !refObjetivaExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Refracción Objetiva", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (refObjetivaExpanded) "Colapsar" else "Expandir",
                    modifier = Modifier.rotate(chevronRefObjRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = refObjetivaExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                }
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text("VL Fórmula Optométrica", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(
            value = uiState.recetaOdEsf,
            onValueChange = { onUpdate(uiState.copy(recetaOdEsf = it)) },
            label = "OD Esf",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") }.testTag(TestTags.EVALUACION_ESFERA_OD),
        )
        OptoTextField(
            value = uiState.recetaOdCil,
            onValueChange = { onUpdate(uiState.copy(recetaOdCil = it)) },
            label = "OD Cil",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") }.testTag(TestTags.EVALUACION_CILINDRO_OD),
        )
        OptoTextField(
            value = uiState.recetaOdEje,
            onValueChange = { onUpdate(uiState.copy(recetaOdEje = it)) },
            label = "OD Eje",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoTextField(
            value = uiState.recetaOiEsf,
            onValueChange = { onUpdate(uiState.copy(recetaOiEsf = it)) },
            label = "OI Esf",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OI") },
        )
        OptoTextField(
            value = uiState.recetaOiCil,
            onValueChange = { onUpdate(uiState.copy(recetaOiCil = it)) },
            label = "OI Cil",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OI") },
        )
        OptoTextField(
            value = uiState.recetaOiEje,
            onValueChange = { onUpdate(uiState.copy(recetaOiEje = it)) },
            label = "OI Eje",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OI") },
        )
    }
    Text("VL AV CC", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
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
private fun DipSection(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("DIP / DNP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(value = uiState.dipLejos, onValueChange = { onUpdate(uiState.copy(dipLejos = it)) }, label = "DNP Lejos", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f).testTag(TestTags.EVALUACION_DIP_FIELD))
                val dipLabel = dipLabelForVpMode(uiState.isVpCerca)
                val dipValue = if (uiState.isVpCerca) uiState.dipCerca else uiState.dipIntermedio
                OptoTextField(
                    value = dipValue,
                    onValueChange = { newVal ->
                        if (uiState.isVpCerca) {
                            onUpdate(uiState.copy(dipCerca = newVal))
                        } else {
                            onUpdate(uiState.copy(dipIntermedio = newVal))
                        }
                    },
                    label = dipLabel,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PrismasSection(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    var prismasExpanded by remember { mutableStateOf(false) }
    val chevronPrismasRotation by animateFloatAsState(
        targetValue = if (prismasExpanded) 180f else 0f,
        label = "chevronPrismas",
    )

    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { prismasExpanded = !prismasExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Prismas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (prismasExpanded) "Colapsar" else "Expandir",
                    modifier = Modifier.rotate(chevronPrismasRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = prismasExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OptoTextField(
                            value = uiState.prismaOdValor,
                            onValueChange = { onUpdate(uiState.copy(prismaOdValor = it)) },
                            label = "OD",
                            modifier = Modifier.weight(1f),
                            trailingIcon = { if (uiState.prismaOdValor.isNotBlank()) PrismaSuffix() },
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownField(label = "Base", selected = uiState.prismaOdBase, options = basesPrisma, onSelected = { onUpdate(uiState.copy(prismaOdBase = it)) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OptoTextField(
                            value = uiState.prismaOiValor,
                            onValueChange = { onUpdate(uiState.copy(prismaOiValor = it)) },
                            label = "OI",
                            modifier = Modifier.weight(1f),
                            trailingIcon = { if (uiState.prismaOiValor.isNotBlank()) PrismaSuffix() },
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownField(label = "Base", selected = uiState.prismaOiBase, options = basesPrisma, onSelected = { onUpdate(uiState.copy(prismaOiBase = it)) })
                        }
                    }
                }
            }
        }
    }
}

internal val basesPrisma = listOf("Nasal", "Temporal", "Superior", "Inferior")

@Composable
internal fun PrismaSuffix() {
    Text(
        "DP",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun dipLabelForVpMode(isVpCerca: Boolean): String = if (isVpCerca) "DNP Cerca" else "DNP Intermedio"
