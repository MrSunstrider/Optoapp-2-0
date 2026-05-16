package com.example.optoapp.ui.components.paciente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente

@Composable
fun EvaluacionesList(
    evaluaciones: List<EvaluacionClinica>,
    paciente: Paciente,
    evaluacionViewModel: com.example.optoapp.viewmodel.EvaluacionViewModel,
    onEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedEvalForResumen = remember { mutableStateOf<EvaluacionClinica?>(null) }

    if (evaluaciones.isEmpty()) {
        EmptyListMessage("No hay evaluaciones registradas.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(evaluaciones, key = { it.id }) { eval ->
                EvaluacionListItem(
                    eval = eval,
                    onEdit = { onEdit(eval.id) },
                    onDelete = {
                        val notificationHelper = com.example.optoapp.notifications.NotificationHelper(context)
                        notificationHelper.cancelReminder(eval.id)
                        evaluacionViewModel.deleteEvaluacion(eval.id) { }
                    },
                    onResumen = { selectedEvalForResumen.value = eval }
                )
            }
        }
    }

    selectedEvalForResumen.value?.let { currentEval ->
        ResumenEvaluacionDialog(
            eval = currentEval,
            paciente = paciente,
            onDismiss = { selectedEvalForResumen.value = null },
            onEdit = { onEdit(currentEval.id) }
        )
    }
}

@Composable
fun ResumenEvaluacionDialog(eval: EvaluacionClinica, paciente: Paciente, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val date = com.example.optoapp.util.DateUtils.formatLocalized(eval.fecha)
    val proxima = eval.proximaCita?.let { com.example.optoapp.util.DateUtils.formatLocalized(it) } ?: "No programada"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resumen Clínico")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoSection("Datos del Paciente") {
                    Text("Nombre: ${paciente.nombreCompleto}", fontSize = 14.sp)
                    Text("Edad: ${paciente.edad} años", fontSize = 14.sp)
                    if (paciente.telefono.isNotBlank()) Text("Teléfono: ${paciente.telefono}", fontSize = 14.sp)
                    if (!paciente.historiaOptometrica.isNullOrBlank()) Text("Historia Optométrica: ${paciente.historiaOptometrica}", fontSize = 14.sp)
                    Text("Fecha de Eval: $date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                val showOd = eval.recetaOdEsf.isNotBlank() || eval.recetaOdCil.isNotBlank()
                val showOi = eval.recetaOiEsf.isNotBlank() || eval.recetaOiCil.isNotBlank()
                if (showOd || showOi) {
                    InfoSection("VL Fórmula Optométrica") {
                        if (showOd) {
                            Text("OD: ${eval.recetaOdEsf} / ${eval.recetaOdCil} x ${eval.recetaOdEje}°", fontSize = 14.sp)
                        }
                        if (showOi) {
                            Text("OI: ${eval.recetaOiEsf} / ${eval.recetaOiCil} x ${eval.recetaOiEje}°", fontSize = 14.sp)
                        }
                    }
                }

                val avccOd = eval.recetaOdAv.ifBlank { eval.avCcOdLejos }
                val avccOi = eval.recetaOiAv.ifBlank { eval.avCcOiLejos }
                val avccAo = eval.avCcAoPx
                val hasAvcc = avccOd.isNotBlank() || avccOi.isNotBlank() || avccAo.isNotBlank()
                if (hasAvcc) {
                    InfoSection("VL AV CC") {
                        Text("AVCC OD: ${if (avccOd.isBlank()) "—" else avccOd}       AV CC AO", fontSize = 14.sp)
                        Text("AVCC OI: ${if (avccOi.isBlank()) "—" else avccOi}       ${if (avccAo.isBlank()) "—" else avccAo}", fontSize = 14.sp)
                    }
                }

                val hasAdd = eval.addCercaOd.isNotBlank() || eval.addCercaOi.isNotBlank() || eval.addAv.isNotBlank()
                if (hasAdd) {
                    InfoSection("Adición (ADD)") {
                        if (eval.addCercaOd.isNotBlank()) Text("OD: ${eval.addCercaOd}", fontSize = 14.sp)
                        if (eval.addCercaOi.isNotBlank()) Text("OI: ${eval.addCercaOi}", fontSize = 14.sp)
                        if (eval.addAv.isNotBlank()) Text("AV VP: ${eval.addAv}", fontSize = 14.sp)
                    }
                }

                val hasDip = eval.dipLejos.isNotBlank() || eval.dipIntermedio.isNotBlank() || eval.dipCerca.isNotBlank()
                if (hasDip) {
                    InfoSection("DIP (Distancia Interpupilar)") {
                        if (eval.dipLejos.isNotBlank()) Text("Lejos: ${eval.dipLejos}", fontSize = 14.sp)
                        if (eval.dipIntermedio.isNotBlank()) Text("Intermedio: ${eval.dipIntermedio}", fontSize = 14.sp)
                        if (eval.dipCerca.isNotBlank()) Text("Cerca: ${eval.dipCerca}", fontSize = 14.sp)
                    }
                }

                val diagOd = eval.diagnosticoOd.firstOrNull() ?: ""
                val diagOi = eval.diagnosticoOi.firstOrNull() ?: ""

                if (diagOd.isNotBlank() || diagOi.isNotBlank() || eval.diagnostico.isNotBlank()) {
                    InfoSection("Diagnóstico") {
                        if (eval.diagnostico.isNotBlank()) Text(eval.diagnostico, fontSize = 14.sp)
                        if (diagOd.isNotBlank()) Text("OD: $diagOd", fontSize = 14.sp)
                        if (diagOi.isNotBlank()) Text("OI: $diagOi", fontSize = 14.sp)
                    }
                }

                val diagOtrosList = mutableListOf<String>()
                if (eval.otrosPresbicia) diagOtrosList.add("Presbicia")
                if (eval.otrosAnisometropia) diagOtrosList.add("Anisometropía")
                if (eval.otrosAmbliopia) diagOtrosList.add("Ambliopía")

                if (diagOtrosList.isNotEmpty()) {
                    InfoSection("Condiciones Asociadas") {
                        diagOtrosList.forEach { cond ->
                            Text(cond, fontSize = 14.sp)
                        }
                    }
                }

                val hasPrisma = eval.prismaOdValor.isNotBlank() || eval.prismaOiValor.isNotBlank()
                if (hasPrisma) {
                    InfoSection("Prismas") {
                        if (eval.prismaOdValor.isNotBlank()) Text("OD: ${eval.prismaOdValor} (Base: ${eval.prismaOdBase})", fontSize = 14.sp)
                        if (eval.prismaOiValor.isNotBlank()) Text("OI: ${eval.prismaOiValor} (Base: ${eval.prismaOiBase})", fontSize = 14.sp)
                    }
                }

                val keratoOd = eval.k1Od.isNotBlank() || eval.k2Od.isNotBlank()
                val keratoOi = eval.k1Oi.isNotBlank() || eval.k2Oi.isNotBlank()
                if (keratoOd || keratoOi) {
                    InfoSection("Queratometría") {
                        if (keratoOd) Text("OD: ${eval.k1Od} / ${eval.k2Od}", fontSize = 14.sp)
                        if (keratoOi) Text("OI: ${eval.k1Oi} / ${eval.k2Oi}", fontSize = 14.sp)
                    }
                }

                val tieneLC = eval.lcOdEsf.isNotBlank() || eval.lcOiEsf.isNotBlank()
                if (tieneLC) {
                    InfoSection("Contactología") {
                        Text("Contiene datos de adaptación", color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp)
                    }
                }

                if (eval.proximaCita != null) {
                    InfoSection("Próxima Cita") {
                        Text(proxima, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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

@Composable
fun InfoSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun EmptyListMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}
