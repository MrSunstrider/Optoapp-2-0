package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.AgendaFiltro
import com.example.optoapp.viewmodel.AgendaViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val estadosCitaInternas = listOf(
    "programada",
    "confirmada",
    "asistio",
    "no_asistio",
    "reprogramada",
)

private fun etiquetaEstadoCita(codigo: String): String = when (codigo) {
    "programada" -> "Programada"
    "confirmada" -> "Confirmada"
    "asistio" -> "Asistió"
    "no_asistio" -> "No asistió"
    "reprogramada" -> "Reprogramada"
    else -> codigo
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    navController: NavController,
    drawerState: DrawerState,
    viewModel: AgendaViewModel = hiltViewModel(),
) {
    val filas by viewModel.filas.collectAsState()
    val filtro by viewModel.filtroState.collectAsState()
    val mesCursor by viewModel.mesCursorState.collectAsState()
    val scope = rememberCoroutineScope()

    var reprogramarEvalId by remember { mutableStateOf<String?>(null) }
    val filaReprogramar = remember(reprogramarEvalId, filas) {
        filas.find { it.evaluacion.id == reprogramarEvalId }
    }

    if (reprogramarEvalId != null && filaReprogramar != null) {
        key(reprogramarEvalId) {
            OptoDatePickerDialog(
                initialDate = filaReprogramar.evaluacion.proximaCita ?: LocalDate.now(),
                onDateSelected = { date ->
                    val id = reprogramarEvalId ?: return@OptoDatePickerDialog
                    viewModel.reprogramar(id, date)
                },
                onDismiss = { reprogramarEvalId = null },
                dismissButton = { TextButton(onClick = { reprogramarEvalId = null }) { Text("Cancelar") } },
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OptoTopAppBar(
                title = "OptoApp - Agenda (próximas citas)",
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filtro == AgendaFiltro.HOY,
                    onClick = { viewModel.setFiltro(AgendaFiltro.HOY) },
                    label = { Text("Hoy") },
                )
                FilterChip(
                    selected = filtro == AgendaFiltro.SEMANA,
                    onClick = { viewModel.setFiltro(AgendaFiltro.SEMANA) },
                    label = { Text("Semana") },
                )
                FilterChip(
                    selected = filtro == AgendaFiltro.MES,
                    onClick = { viewModel.setFiltro(AgendaFiltro.MES) },
                    label = { Text("Mes") },
                )
            }
            if (filtro == AgendaFiltro.MES) {
                Spacer(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(onClick = { viewModel.mesAnterior() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                    }
                    Text(
                        text = mesCursor.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = { viewModel.mesSiguiente() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                    }
                }
            }
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            if (filas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No hay citas con fecha en este período.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filas, key = { it.evaluacion.id }) { fila ->
                        val e = fila.evaluacion
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(fila.nombrePaciente, fontWeight = FontWeight.Bold)
                                Text(
                                    "Cita: ${e.proximaCita?.let { DateUtils.formatLocalized(it) } ?: "—"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                DropdownField(
                                    label = "Estado",
                                    selected = etiquetaEstadoCita(e.citaEstado.orEmpty().ifBlank { "programada" }),
                                    options = estadosCitaInternas.map(::etiquetaEstadoCita),
                                    onSelected = { label ->
                                        val codigo = estadosCitaInternas.find { etiquetaEstadoCita(it) == label }
                                            ?: "programada"
                                        viewModel.actualizarEstado(e.id, codigo)
                                    },
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    TextButton(onClick = { reprogramarEvalId = e.id }) {
                                        Text("Reprogramar")
                                    }
                                    TextButton(
                                        onClick = {
                                            navController.navigate(Route.EditarEvaluacion(e.pacienteId, e.id).route)
                                        },
                                    ) {
                                        Text("Abrir evaluación")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
