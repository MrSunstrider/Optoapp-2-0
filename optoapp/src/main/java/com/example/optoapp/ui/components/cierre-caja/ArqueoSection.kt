package com.example.optoapp.ui.components.cierre_caja

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.arqueo.ArqueoCaja
import com.example.optoapp.viewmodel.ArqueoCajaUiState
import com.example.optoapp.viewmodel.BadgeColor
import java.util.Locale

@Composable
fun ArqueoSection(
    arqueoFromCierre: ArqueoCaja?,
    arqueoUiState: ArqueoCajaUiState,
    systemTotals: Map<String, Double>,
    fecha: java.time.LocalDate,
    opticaId: String,
    onFondoCajaChange: (Double) -> Unit,
    onEfectivoContadoChange: (Double) -> Unit,
    onTarjetaContadoChange: (Double) -> Unit,
    onTransferenciaContadoChange: (Double) -> Unit,
    onMovilContadoChange: (Double) -> Unit,
    onCerrarDia: () -> Unit
) {
    val isSellado = arqueoFromCierre?.sellado == true
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSellado)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Arqueo de Caja", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSellado) {
                        AssistChip(
                            onClick = {},
                            label = { Text("SELLADO") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Colapsar arqueo" else "Expandir arqueo",
                        modifier = Modifier.rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    if (isSellado) {
                        val arqueo = arqueoFromCierre ?: return@AnimatedVisibility
                        ReadOnlyField("Fondo de Caja", arqueo.fondoCaja)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Método", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        ArqueoReadOnlyRow("Efectivo", arqueo.efectivoContado, arqueo.efectivoCobrado, arqueo.diferenciaEfectivo)
                        ArqueoReadOnlyRow("Tarjeta", arqueo.tarjetaContado, arqueo.tarjetaCobrado, arqueo.diferenciaTarjeta)
                        ArqueoReadOnlyRow("Transferencia", arqueo.transferenciaContado, arqueo.transferenciaCobrado, arqueo.diferenciaTransferencia)
                        ArqueoReadOnlyRow("Móvil", arqueo.movilContado, arqueo.movilCobrado, arqueo.diferenciaMovil)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        ReadOnlyField("Diferencia Total", arqueo.diferenciaTotal)
                        ReadOnlyField("Cerrado por", arqueo.cerradoPor)
                    } else {
                        ArqueoNumberField("Fondo de Caja", arqueoUiState.fondoCaja, onFondoCajaChange)
                        ArqueoNumberField("Efectivo Contado", arqueoUiState.efectivoContado, onEfectivoContadoChange)
                        ArqueoNumberField("Tarjeta Contado", arqueoUiState.tarjetaContado, onTarjetaContadoChange)
                        ArqueoNumberField("Transferencia Contado", arqueoUiState.transferenciaContado, onTransferenciaContadoChange)
                        ArqueoNumberField("Móvil Contado", arqueoUiState.movilContado, onMovilContadoChange)

                        arqueoUiState.validationErrors.forEach { (field, error) ->
                            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = onCerrarDia,
                            enabled = !arqueoUiState.isSellado,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Cerrar Día")
                        }
                    }

                }
            }
        }
    }
}

@Composable
internal fun ArqueoNumberField(label: String, value: Double, onValueChange: (Double) -> Unit) {
    var textValue by remember(value) { mutableStateOf(if (value == 0.0) "" else value.toString()) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            textValue = newText
            val parsed = newText.toDoubleOrNull()
            if (parsed != null) onValueChange(parsed)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun ReadOnlyField(label: String, value: Any) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            when (value) {
                is Double -> "s/. ${String.format(Locale.getDefault(), "%.2f", value)}"
                else -> value.toString()
            },
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
internal fun ArqueoReadOnlyRow(
    method: String,
    contado: Double,
    cobrado: Double,
    diferencia: Double
) {
    val cobradoAmount = cobrado
    val badgeColor = when {
        diferencia == 0.0 -> BadgeColor.GREEN
        cobradoAmount == 0.0 -> BadgeColor.RED
        else -> {
            val ratio = kotlin.math.abs(diferencia) / cobradoAmount
            if (ratio <= 0.05) BadgeColor.YELLOW else BadgeColor.RED
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(method, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("Cont: ${"%.0f".format(contado)}", fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("Cob: ${"%.0f".format(cobrado)}", fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(
            "Dif: ${"%.0f".format(diferencia)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when (badgeColor) {
                BadgeColor.GREEN -> Color(0xFF2E7D32)
                BadgeColor.YELLOW -> Color(0xFFF57F17)
                BadgeColor.RED -> Color(0xFFC62828)
            },
            modifier = Modifier.weight(1f)
        )
    }
}
