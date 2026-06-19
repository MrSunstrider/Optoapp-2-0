package com.example.optoapp.ui.components.evaluacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoSegmentedSelector
import com.example.optoapp.ui.components.OptoQuickAddChip
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.OptoVisionInput
import com.example.optoapp.ui.theme.*
import com.example.optoapp.viewmodel.EvaluacionUiState
import com.example.optoapp.viewmodel.EvaluacionViewModel
import java.util.Locale

@Composable
fun RefraccionSection(
    uiState: EvaluacionUiState,
    onUpdate: (EvaluacionUiState) -> Unit,
    viewModel: EvaluacionViewModel
) {
    var refObjetivaExpanded by remember { mutableStateOf(false) }
    val chevronRefObjRotation by animateFloatAsState(
        targetValue = if (refObjetivaExpanded) 180f else 0f,
        label = "chevronRefObj"
    )

    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { refObjetivaExpanded = !refObjetivaExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Refracción Objetiva", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (refObjetivaExpanded) "Colapsar" else "Expandir",
                    modifier = Modifier.rotate(chevronRefObjRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = refObjetivaExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") }.testTag(TestTags.EVALUACION_ESFERA_OD)
        )
        OptoTextField(
            value = uiState.recetaOdCil,
            onValueChange = { onUpdate(uiState.copy(recetaOdCil = it)) },
            label = "OD Cil",
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") }.testTag(TestTags.EVALUACION_CILINDRO_OD)
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
    Text("VL AV CC", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptoVisionInput(value = uiState.recetaOdAv, onValueChange = { onUpdate(uiState.copy(recetaOdAv = it)) }, label = "AV OD", modifier = Modifier.weight(1f))
        OptoVisionInput(value = uiState.recetaOiAv, onValueChange = { onUpdate(uiState.copy(recetaOiAv = it)) }, label = "AV OI", modifier = Modifier.weight(1f))
        OptoVisionInput(value = uiState.avCcAoPx, onValueChange = { onUpdate(uiState.copy(avCcAoPx = it)) }, label = "AV AO", modifier = Modifier.weight(1f))
    }

    AddSection(uiState, onUpdate)
    DipSection(uiState, onUpdate)
    PrismasSection(uiState, onUpdate)
}

@Composable
private fun AddSection(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    val currentAddOd = if (uiState.isVpCerca) uiState.addCercaOd else uiState.addIntermediaOd
    val currentAddOi = if (uiState.isVpCerca) uiState.addCercaOi else uiState.addIntermediaOi

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Adición",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            OptoSegmentedSelector(
                options = listOf("Sin Adición", "Con Adición"),
                selectedIndex = if (uiState.hasAdd) 1 else 0,
                onSelect = { index ->
                    if (index == 0) {
                        onUpdate(uiState.copy(
                            hasAdd = false,
                            addCercaOd = "", addCercaOi = "",
                            addIntermediaOd = "", addIntermediaOi = ""
                        ))
                    } else {
                        onUpdate(uiState.copy(hasAdd = true))
                    }
                }
            )

            OptoSegmentedSelector(
                options = listOf("Cerca", "Intermedio"),
                selectedIndex = if (uiState.isVpCerca) 0 else 1,
                onSelect = { onUpdate(uiState.copy(isVpCerca = it == 0)) }
            )

            AnimatedVisibility(visible = uiState.hasAdd) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    NumericAddStepper(
                        value = currentAddOd,
                        onValueChange = { newVal ->
                            val updated = if (uiState.isAddAo) {
                                if (uiState.isVpCerca) uiState.copy(addCercaOd = newVal, addCercaOi = newVal)
                                else uiState.copy(addIntermediaOd = newVal, addIntermediaOi = newVal)
                            } else {
                                if (uiState.isVpCerca) uiState.copy(addCercaOd = newVal)
                                else uiState.copy(addIntermediaOd = newVal)
                            }
                            onUpdate(updated)
                        }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("+1.00", "+2.00", "+3.00", "+4.00").forEach { value ->
                            OptoQuickAddChip(
                                value = value,
                                isSelected = currentAddOd == value,
                                onClick = {
                                    val updated = if (uiState.isAddAo) {
                                        if (uiState.isVpCerca) uiState.copy(addCercaOd = value, addCercaOi = value)
                                        else uiState.copy(addIntermediaOd = value, addIntermediaOi = value)
                                    } else {
                                        if (uiState.isVpCerca) uiState.copy(addCercaOd = value)
                                        else uiState.copy(addIntermediaOd = value)
                                    }
                                    onUpdate(updated)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A/O", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = uiState.isAddAo,
                            onCheckedChange = { onUpdate(uiState.copy(isAddAo = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Ambos ojos igual", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptoVisionInput(
                            value = currentAddOd,
                            onValueChange = { newVal ->
                                val base = uiState.copy(isAddAo = false)
                                val updated = if (uiState.isVpCerca) base.copy(addCercaOd = newVal)
                                else base.copy(addIntermediaOd = newVal)
                                onUpdate(updated)
                            },
                            label = "Add OD",
                            modifier = Modifier.weight(1f)
                        )
                        OptoVisionInput(
                            value = if (uiState.isAddAo) currentAddOd else currentAddOi,
                            onValueChange = { newVal ->
                                val base = uiState.copy(isAddAo = false)
                                val updated = if (uiState.isVpCerca) base.copy(addCercaOi = newVal)
                                else base.copy(addIntermediaOi = newVal)
                                onUpdate(updated)
                            },
                            label = "Add OI",
                            enabled = !uiState.isAddAo,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                "AV VP",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                "Aplica a Px présbitas y no présbitas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OptoVisionInput(
                    value = uiState.addAv,
                    onValueChange = { onUpdate(uiState.copy(addAv = it)) },
                    label = "AV VP",
                    modifier = Modifier.width(140.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun NumericAddStepper(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentVal = value.toDoubleOrNull() ?: 2.0

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularStepperButton(
            icon = Icons.Default.Remove,
            contentDescription = "Decrementar",
            onClick = {
                val newVal = (currentVal - 0.25).coerceAtLeast(0.75)
                onValueChange(formatAddValue(newVal))
            }
        )

        Spacer(Modifier.width(20.dp))

        Text(
            text = "${formatAddValue(currentVal)} D",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(20.dp))

        CircularStepperButton(
            icon = Icons.Default.Add,
            contentDescription = "Incrementar",
            onClick = {
                val newVal = (currentVal + 0.25).coerceAtMost(4.00)
                onValueChange(formatAddValue(newVal))
            }
        )
    }
}

@Composable
private fun CircularStepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(46.dp),
        shape = CircleShape,
        color = SurfaceDark,
        contentColor = TextPrimaryDark,
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

private fun formatAddValue(value: Double): String =
    "+%.2f".format(Locale.US, value)

@Composable
private fun DipSection(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
                        if (uiState.isVpCerca) onUpdate(uiState.copy(dipCerca = newVal))
                        else onUpdate(uiState.copy(dipIntermedio = newVal))
                    },
                    label = dipLabel,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
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
        label = "chevronPrismas"
    )

    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prismasExpanded = !prismasExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Prismas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (prismasExpanded) "Colapsar" else "Expandir",
                    modifier = Modifier.rotate(chevronPrismasRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = prismasExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OptoTextField(
                            value = uiState.prismaOdValor,
                            onValueChange = { onUpdate(uiState.copy(prismaOdValor = it)) },
                            label = "OD",
                            modifier = Modifier.weight(1f),
                            trailingIcon = { if (uiState.prismaOdValor.isNotBlank()) PrismaSuffix() }
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
                            trailingIcon = { if (uiState.prismaOiValor.isNotBlank()) PrismaSuffix() }
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
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

internal fun dipLabelForVpMode(isVpCerca: Boolean): String =
    if (isVpCerca) "DNP Cerca" else "DNP Intermedio"
