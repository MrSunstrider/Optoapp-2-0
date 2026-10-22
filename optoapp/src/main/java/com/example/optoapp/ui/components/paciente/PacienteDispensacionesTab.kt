package com.example.optoapp.ui.components.paciente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.R
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import com.example.optoapp.ui.components.common.EmptyState
import com.example.optoapp.ui.components.common.InfoSection
import com.example.optoapp.ui.components.LaboratorioTicketAlertDialog
import com.example.optoapp.ui.components.OrderEstadoChip
import com.example.optoapp.ui.theme.saldoColor
import com.example.optoapp.util.DispensacionLaboratorioTicket
import com.example.optoapp.util.LaboratorioTicketContext
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.LaboratorioConfigViewModel

@Composable
fun DispensacionesList(
    dispensaciones: List<DispensacionOptica>,
    paciente: Paciente,
    evaluaciones: List<EvaluacionClinica>,
    onEdit: (String) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    laboratorioVm: LaboratorioConfigViewModel = hiltViewModel(),
    pagosSumMap: Map<String, Double> = emptyMap(),
) {
    val selectedDispForResumen = remember { mutableStateOf<DispensacionOptica?>(null) }
    val selectedDispForTicket = remember { mutableStateOf<DispensacionOptica?>(null) }
    var dispToDelete by remember { mutableStateOf<DispensacionOptica?>(null) }
    val labCfg by laboratorioVm.uiState.collectAsState()

    if (dispensaciones.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.paciente_disp_vacio_titulo),
            subtitle = stringResource(R.string.paciente_disp_vacio_mensaje),
        )
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(dispensaciones, key = { it.id }) { disp ->
                val date = com.example.optoapp.util.DateUtils.formatLocalized(disp.fecha)
                val montoPagado = pagosSumMap[disp.id] ?: 0.0
                val saldo = disp.montoTotal - montoPagado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = { onEdit(disp.id) },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = date,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            OrderEstadoChip(disp.estadoEntrega, saldo = saldo)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(modifier = Modifier.size(40.dp), onClick = { selectedDispForTicket.value = disp }) {
                                Icon(
                                    Icons.Filled.Science,
                                    contentDescription = stringResource(R.string.paciente_disp_ticket_cd),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(modifier = Modifier.size(40.dp), onClick = { selectedDispForResumen.value = disp }) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = stringResource(R.string.paciente_disp_ver_resumen_cd),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            if (onDelete != null) {
                                IconButton(modifier = Modifier.size(40.dp), onClick = { dispToDelete = disp }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.paciente_disp_eliminar_cd),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                        if (disp.fechaEntrega != null) {
                            Text(
                                text = stringResource(
                                    R.string.paciente_disp_entregado_el,
                                    com.example.optoapp.util.DateUtils.formatLocalized(disp.fechaEntrega),
                                ),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.paciente_disp_lente_resumen, disp.tipoLente, disp.materialLente),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(text = stringResource(R.string.disp_saldo), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = MoneyFormatter.format(saldo), color = saldoColor(saldo), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    selectedDispForTicket.value?.let { dispTicket ->
        val ultimaEval = evaluaciones.maxByOrNull { it.fecha }
        val ticketCtx = LaboratorioTicketContext.fromDispensacion(dispTicket, paciente.nombreCompleto)
        LaboratorioTicketAlertDialog(
            onDismiss = { selectedDispForTicket.value = null },
            ticketTextoCompleto = DispensacionLaboratorioTicket.textoCompleto(ticketCtx, ultimaEval),
            ticketTextoCompacto = DispensacionLaboratorioTicket.textoCompacto(ticketCtx, ultimaEval),
            laboratorioNombre = labCfg.laboratorioNombre,
            laboratorioContacto = labCfg.laboratorioContacto,
        )
    }

    dispToDelete?.let { disp ->
        AlertDialog(
            onDismissRequest = { dispToDelete = null },
            title = { Text(stringResource(R.string.paciente_disp_eliminar_titulo)) },
            text = {
                Text(
                    stringResource(
                        R.string.paciente_disp_eliminar_mensaje,
                        disp.ot.ifBlank { "—" },
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke(disp.id)
                        dispToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { dispToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    selectedDispForResumen.value?.let { currentDisp ->
        ResumenDispensacionDialog(
            disp = currentDisp,
            paciente = paciente,
            onDismiss = { selectedDispForResumen.value = null },
            onEdit = { onEdit(currentDisp.id) },
            pagosSum = pagosSumMap[currentDisp.id] ?: 0.0,
        )
    }
}

@Composable
fun ResumenDispensacionDialog(
    disp: DispensacionOptica,
    paciente: Paciente,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onGoToFinanciero: ((DispensacionOptica) -> Unit)? = null,
    pagosSum: Double = 0.0,
) {
    val date = com.example.optoapp.util.DateUtils.formatLocalized(disp.fecha)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.paciente_disp_resumen_titulo))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoSection(stringResource(R.string.paciente_disp_datos_paciente)) {
                    if (disp.ot.isNotBlank()) Text(stringResource(R.string.paciente_disp_ot, disp.ot), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.paciente_disp_nombre, paciente.nombreCompleto), fontSize = 14.sp)
                    Text(stringResource(R.string.paciente_disp_edad, paciente.edad), fontSize = 14.sp)
                    if (!paciente.historiaOptometrica.isNullOrBlank()) {
                        Text(stringResource(R.string.paciente_disp_historia, paciente.historiaOptometrica), fontSize = 14.sp)
                    }
                    Text(stringResource(R.string.paciente_disp_fecha, date), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                if (disp.origenMontura.isNotBlank() || disp.tipoAro.isNotBlank() || disp.materialMontura.isNotBlank() || disp.descripcionMontura.isNotBlank()) {
                    InfoSection(stringResource(R.string.paciente_disp_info_montura)) {
                        if (disp.origenMontura.isNotBlank()) Text(stringResource(R.string.paciente_disp_origen, disp.origenMontura), fontSize = 14.sp)
                        if (disp.tipoAro.isNotBlank()) Text(stringResource(R.string.paciente_disp_tipo_aro, disp.tipoAro), fontSize = 14.sp)
                        if (disp.materialMontura.isNotBlank()) Text(stringResource(R.string.paciente_disp_material, disp.materialMontura), fontSize = 14.sp)
                        if (disp.descripcionMontura.isNotBlank()) Text(stringResource(R.string.paciente_disp_descripcion, disp.descripcionMontura), fontSize = 14.sp)
                    }
                }

                if (disp.tipoLente.isNotBlank() || disp.materialLente.isNotBlank() || disp.colorLente.isNotBlank() || disp.tratamientos.isNotEmpty() || disp.notasDiseno.isNotBlank()) {
                    InfoSection(stringResource(R.string.paciente_disp_info_lente)) {
                        if (disp.tipoLente.isNotBlank()) {
                            val conSubtipo = if (disp.tipoLente == "Bifocal" && disp.subTipoBifocal.isNotBlank()) {
                                stringResource(R.string.paciente_disp_tipo_subtipo, disp.tipoLente, disp.subTipoBifocal)
                            } else {
                                disp.tipoLente
                            }
                            val tipo = if (disp.tipoLente == "Monofocal" && disp.distanciaLente.isNotBlank()) {
                                stringResource(R.string.paciente_disp_tipo_distancia, conSubtipo, disp.distanciaLente)
                            } else {
                                conSubtipo
                            }
                            Text(stringResource(R.string.paciente_disp_tipo, tipo), fontSize = 14.sp)
                        }
                        if ((disp.tipoLente == "Bifocal" || disp.tipoLente == "Multifocal" || disp.tipoLente == "Ocupacional") && disp.altura.isNotBlank()) {
                            Text(stringResource(R.string.paciente_disp_altura, disp.altura), fontSize = 14.sp)
                        }
                        if (disp.materialLente.isNotBlank()) Text(stringResource(R.string.paciente_disp_material, disp.materialLente), fontSize = 14.sp)
                        if (disp.colorLente.isNotBlank()) Text(stringResource(R.string.paciente_disp_color, disp.colorLente), fontSize = 14.sp)
                        if (disp.tratamientos.isNotEmpty()) {
                            Text(stringResource(R.string.paciente_disp_tratamientos, disp.tratamientos.joinToString(", ")), fontSize = 14.sp)
                        }
                        if (disp.notasDiseno.isNotBlank()) Text(stringResource(R.string.paciente_disp_notas, disp.notasDiseno), fontSize = 14.sp)
                    }
                }

                InfoSection(stringResource(R.string.paciente_disp_resumen_financiero)) {
                    val saldo = disp.montoTotal - pagosSum

                    Text(stringResource(R.string.paciente_disp_monto_total, MoneyFormatter.format(disp.montoTotal)), fontSize = 14.sp)
                    if (disp.metodoPago.isNotBlank()) Text(stringResource(R.string.paciente_disp_metodo_pago, disp.metodoPago), fontSize = 14.sp)
                    Text(stringResource(R.string.paciente_disp_a_cuenta, MoneyFormatter.format(pagosSum)), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.paciente_disp_saldo_restante, MoneyFormatter.format(saldo)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = saldoColor(saldo),
                    )
                }

                InfoSection(stringResource(R.string.paciente_disp_estado)) {
                    Text(
                        text = disp.estadoEntrega,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (disp.estadoEntrega == "Entregado") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    )
                    if (disp.fechaEntrega != null) {
                        Text(
                            text = stringResource(
                                R.string.paciente_disp_entregado_el_dia,
                                com.example.optoapp.util.DateUtils.formatLocalized(disp.fechaEntrega),
                            ),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(onClick = {
                    onDismiss()
                    onEdit()
                }) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.paciente_disp_editar_completo))
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onGoToFinanciero != null) {
                    TextButton(onClick = { onGoToFinanciero(disp) }) {
                        Text(stringResource(R.string.paciente_disp_ir_financiero))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
            }
        },
    )
}
