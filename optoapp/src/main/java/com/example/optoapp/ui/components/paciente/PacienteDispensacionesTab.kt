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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import com.example.optoapp.ui.components.LaboratorioTicketAlertDialog
import com.example.optoapp.util.DispensacionLaboratorioTicket
import com.example.optoapp.util.LaboratorioTicketContext
import com.example.optoapp.viewmodel.LaboratorioConfigViewModel
import java.util.Locale

@Composable
fun DispensacionesList(
    dispensaciones: List<DispensacionOptica>,
    paciente: Paciente,
    evaluaciones: List<EvaluacionClinica>,
    onEdit: (String) -> Unit,
    laboratorioVm: LaboratorioConfigViewModel = hiltViewModel(),
) {
    val selectedDispForResumen = remember { mutableStateOf<DispensacionOptica?>(null) }
    val selectedDispForTicket = remember { mutableStateOf<DispensacionOptica?>(null) }
    val labCfg by laboratorioVm.uiState.collectAsState()

    if (dispensaciones.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                Text("No hay dispensaciones.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(dispensaciones) { disp ->
                val date = com.example.optoapp.util.DateUtils.formatLocalized(disp.fecha)
                val saldo = disp.montoTotal - disp.montoPagado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = { onEdit(disp.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = date, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(modifier = Modifier.size(24.dp), onClick = { selectedDispForTicket.value = disp }) {
                                    Icon(
                                        Icons.Filled.Science,
                                        contentDescription = "Ticket de laboratorio",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(modifier = Modifier.size(24.dp), onClick = { selectedDispForResumen.value = disp }) {
                                    Icon(Icons.Default.Visibility, contentDescription = "Ver Resumen", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }
                                Surface(
                                    color = if (disp.estadoEntrega == "Entregado") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        disp.estadoEntrega,
                                        color = if (disp.estadoEntrega == "Entregado") MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        if (disp.fechaEntrega != null) {
                            Text(
                                text = "Entregado el ${com.example.optoapp.util.DateUtils.formatLocalized(disp.fechaEntrega)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${disp.tipoLente} - ${disp.materialLente}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(text = "Saldo:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                            Text(text = "s/. $formattedSaldo", color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
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

    selectedDispForResumen.value?.let { currentDisp ->
        ResumenDispensacionDialog(
            disp = currentDisp,
            paciente = paciente,
            onDismiss = { selectedDispForResumen.value = null },
            onEdit = { onEdit(currentDisp.id) }
        )
    }
}

@Composable
fun ResumenDispensacionDialog(disp: DispensacionOptica, paciente: Paciente, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val date = com.example.optoapp.util.DateUtils.formatLocalized(disp.fecha)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resumen de Dispensación")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoSection("Datos del Paciente") {
                    if (disp.ot.isNotBlank()) Text("OT: ${disp.ot}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Nombre: ${paciente.nombreCompleto}", fontSize = 14.sp)
                    Text("Edad: ${paciente.edad} años", fontSize = 14.sp)
                    if (!paciente.historiaOptometrica.isNullOrBlank()) Text("Historia Optométrica: ${paciente.historiaOptometrica}", fontSize = 14.sp)
                    Text("Fecha: $date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                if (disp.origenMontura.isNotBlank() || disp.tipoAro.isNotBlank() || disp.materialMontura.isNotBlank() || disp.descripcionMontura.isNotBlank()) {
                    InfoSection("Información de Montura") {
                        if (disp.origenMontura.isNotBlank()) Text("Origen: ${disp.origenMontura}", fontSize = 14.sp)
                        if (disp.tipoAro.isNotBlank()) Text("Tipo de Aro: ${disp.tipoAro}", fontSize = 14.sp)
                        if (disp.materialMontura.isNotBlank()) Text("Material: ${disp.materialMontura}", fontSize = 14.sp)
                        if (disp.descripcionMontura.isNotBlank()) Text("Descripción: ${disp.descripcionMontura}", fontSize = 14.sp)
                    }
                }

                if (disp.tipoLente.isNotBlank() || disp.materialLente.isNotBlank() || disp.colorLente.isNotBlank() || disp.tratamientos.isNotEmpty() || disp.notasDiseno.isNotBlank()) {
                    InfoSection("Información del Lente") {
                        if (disp.tipoLente.isNotBlank()) {
                            val subtipo = if (disp.tipoLente == "Bifocal" && disp.subTipoBifocal.isNotBlank()) " (${disp.subTipoBifocal})" else ""
                            val distancia = if (disp.tipoLente == "Monofocal" && disp.distanciaLente.isNotBlank()) " - ${disp.distanciaLente}" else ""
                            Text("Tipo: ${disp.tipoLente}$subtipo$distancia", fontSize = 14.sp)
                        }
                        if ((disp.tipoLente == "Bifocal" || disp.tipoLente == "Progresivo" || disp.tipoLente == "Ocupacional") && disp.altura.isNotBlank()) {
                            Text("Altura: ${disp.altura} mm", fontSize = 14.sp)
                        }
                        if (disp.materialLente.isNotBlank()) Text("Material: ${disp.materialLente}", fontSize = 14.sp)
                        if (disp.colorLente.isNotBlank()) Text("Color: ${disp.colorLente}", fontSize = 14.sp)
                        if (disp.tratamientos.isNotEmpty()) {
                            Text("Tratamientos: ${disp.tratamientos.joinToString(", ")}", fontSize = 14.sp)
                        }
                        if (disp.notasDiseno.isNotBlank()) Text("Notas: ${disp.notasDiseno}", fontSize = 14.sp)
                    }
                }

                InfoSection("Resumen Financiero") {
                    val saldo = disp.montoTotal - disp.montoPagado
                    val formattedTotal = String.format(Locale.getDefault(), "%.2f", disp.montoTotal)
                    val formattedPagado = String.format(Locale.getDefault(), "%.2f", disp.montoPagado)
                    val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)

                    Text("Monto Total: s/. $formattedTotal", fontSize = 14.sp)
                    if (disp.metodoPago.isNotBlank()) Text("Método de Pago: ${disp.metodoPago}", fontSize = 14.sp)
                    Text("A Cuenta: s/. $formattedPagado", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Saldo Restante: s/. $formattedSaldo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }

                InfoSection("Estado") {
                    Text(
                        text = disp.estadoEntrega,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (disp.estadoEntrega == "Entregado") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    )
                    if (disp.fechaEntrega != null) {
                        Text(
                            text = "Entregado el día ${com.example.optoapp.util.DateUtils.formatLocalized(disp.fechaEntrega)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(onClick = { onDismiss(); onEdit() }) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar Completo")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}


