package com.example.optoapp.ui.components.evaluacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.ui.components.OptoQuickAddChip
import com.example.optoapp.ui.components.OptoSegmentedSelector
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.theme.SurfaceDark
import com.example.optoapp.ui.theme.TextPrimaryDark
import com.example.optoapp.viewmodel.EvaluacionUiState
import java.util.Locale

@Composable
internal fun AddSection(uiState: EvaluacionUiState, onUpdate: (EvaluacionUiState) -> Unit) {
    val currentAddOd = if (uiState.isVpCerca) uiState.addCercaOd else uiState.addIntermediaOd
    val currentAddOi = if (uiState.isVpCerca) uiState.addCercaOi else uiState.addIntermediaOi

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Adición",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            OptoSegmentedSelector(
                options = listOf("Sin Adición", "Con Adición"),
                selectedIndex = if (uiState.hasAdd) 1 else 0,
                onSelect = { index ->
                    if (index == 0) {
                        onUpdate(
                            uiState.copy(
                                hasAdd = false,
                                addCercaOd = "",
                                addCercaOi = "",
                                addIntermediaOd = "",
                                addIntermediaOi = "",
                            ),
                        )
                    } else {
                        onUpdate(uiState.copy(hasAdd = true))
                    }
                },
            )

            AnimatedVisibility(visible = uiState.shouldShowCercaIntermedio) {
                OptoSegmentedSelector(
                    options = listOf("Cerca", "Intermedio"),
                    selectedIndex = if (uiState.isVpCerca) 0 else 1,
                    onSelect = { onUpdate(uiState.copy(isVpCerca = it == 0)) },
                )
            }

            AnimatedVisibility(visible = uiState.hasAdd) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumericAddStepper(
                        value = currentAddOd,
                        onValueChange = { newVal ->
                            val updated = if (uiState.isAddAo) {
                                if (uiState.isVpCerca) {
                                    uiState.copy(addCercaOd = newVal, addCercaOi = newVal)
                                } else {
                                    uiState.copy(addIntermediaOd = newVal, addIntermediaOi = newVal)
                                }
                            } else {
                                if (uiState.isVpCerca) {
                                    uiState.copy(addCercaOd = newVal)
                                } else {
                                    uiState.copy(addIntermediaOd = newVal)
                                }
                            }
                            onUpdate(updated)
                        },
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        listOf("+1.00", "+2.00", "+3.00", "+4.00").forEach { value ->
                            OptoQuickAddChip(
                                value = value,
                                isSelected = currentAddOd == value,
                                onClick = {
                                    val updated = if (uiState.isAddAo) {
                                        if (uiState.isVpCerca) {
                                            uiState.copy(addCercaOd = value, addCercaOi = value)
                                        } else {
                                            uiState.copy(addIntermediaOd = value, addIntermediaOi = value)
                                        }
                                    } else {
                                        if (uiState.isVpCerca) {
                                            uiState.copy(addCercaOd = value)
                                        } else {
                                            uiState.copy(addIntermediaOd = value)
                                        }
                                    }
                                    onUpdate(updated)
                                },
                                modifier = Modifier.weight(1f),
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
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Ambos ojos igual", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptoTextField(
                            value = currentAddOd,
                            onValueChange = { newVal ->
                                val base = uiState.copy(isAddAo = false)
                                val updated = if (uiState.isVpCerca) {
                                    base.copy(addCercaOd = newVal)
                                } else {
                                    base.copy(addIntermediaOd = newVal)
                                }
                                onUpdate(updated)
                            },
                            label = "Add OD",
                            modifier = Modifier.weight(1f),
                        )
                        OptoTextField(
                            value = if (uiState.isAddAo) currentAddOd else currentAddOi,
                            onValueChange = { newVal ->
                                val base = uiState.copy(isAddAo = false)
                                val updated = if (uiState.isVpCerca) {
                                    base.copy(addCercaOi = newVal)
                                } else {
                                    base.copy(addIntermediaOi = newVal)
                                }
                                onUpdate(updated)
                            },
                            label = "Add OI",
                            enabled = !uiState.isAddAo,
                            modifier = Modifier.weight(1f),
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
                textAlign = TextAlign.Center,
            )
            Text(
                "Aplica a Px présbitas y no présbitas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OptoTextField(
                    value = uiState.addAv,
                    onValueChange = { onUpdate(uiState.copy(addAv = it)) },
                    label = "AV VP",
                    modifier = Modifier.width(140.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun NumericAddStepper(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentVal = value.toDoubleOrNull() ?: 2.0

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularStepperButton(
            icon = Icons.Default.Remove,
            contentDescription = "Decrementar",
            onClick = {
                val newVal = (currentVal - 0.25).coerceAtLeast(0.75)
                onValueChange(formatAddValue(newVal))
            },
        )

        Spacer(Modifier.width(20.dp))

        Text(
            text = "${formatAddValue(currentVal)} D",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.width(20.dp))

        CircularStepperButton(
            icon = Icons.Default.Add,
            contentDescription = "Incrementar",
            onClick = {
                val newVal = (currentVal + 0.25).coerceAtMost(4.00)
                onValueChange(formatAddValue(newVal))
            },
        )
    }
}

@Composable
private fun CircularStepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(46.dp),
        shape = CircleShape,
        color = SurfaceDark,
        contentColor = TextPrimaryDark,
        tonalElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

private fun formatAddValue(value: Double): String = "+%.2f".format(Locale.US, value)
