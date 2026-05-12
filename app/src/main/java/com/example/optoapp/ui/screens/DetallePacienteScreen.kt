package com.example.optoapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.Paciente
import com.example.optoapp.ui.components.paciente.DispensacionesList
import com.example.optoapp.ui.components.paciente.EvaluacionesList
import com.example.optoapp.ui.components.paciente.PacienteInfoHeader
import com.example.optoapp.ui.components.paciente.ServiciosExtraList
import com.example.optoapp.util.DispensacionLaboratorioTicket
import com.example.optoapp.util.LaboratorioTicketContext
import com.example.optoapp.util.RecetaEvaluacionPdfGenerator
import com.example.optoapp.util.WhatsAppUtils
import com.example.optoapp.viewmodel.DeletePacienteResult
import com.example.optoapp.viewmodel.DispensacionViewModel
import com.example.optoapp.viewmodel.EvaluacionViewModel
import com.example.optoapp.viewmodel.LaboratorioConfigViewModel
import com.example.optoapp.viewmodel.PacienteViewModel
import com.example.optoapp.viewmodel.ServiciosViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePacienteScreen(
    navController: NavController,
    id: String,
    pacienteViewModel: PacienteViewModel = hiltViewModel(),
    evaluacionViewModel: EvaluacionViewModel = hiltViewModel(),
    dispensacionViewModel: DispensacionViewModel = hiltViewModel(),
    serviciosViewModel: ServiciosViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var paciente by remember { mutableStateOf<Paciente?>(null) }
    val evaluaciones by evaluacionViewModel.getEvaluacionesByPaciente(id).collectAsState(initial = emptyList())
    val dispensaciones by dispensacionViewModel.getDispensacionesByPaciente(id).collectAsState(initial = emptyList())
    val servicios by remember(serviciosViewModel.allServicios, id) {
        serviciosViewModel.allServicios.map { list -> list.filter { it.pacienteId == id } }
    }.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Evaluaciones", "Dispensaciones", "Servicios")
    var showWhatsAppMenu by remember { mutableStateOf(false) }
    var showDeletePacienteDialog by remember { mutableStateOf(false) }
    var deletingPaciente by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        paciente = pacienteViewModel.getPaciente(id)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Ficha del Paciente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showWhatsAppMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp")
                        }
                        DropdownMenu(
                            expanded = showWhatsAppMenu,
                            onDismissRequest = { showWhatsAppMenu = false }
                        ) {
                            paciente?.let { p ->
                                val nombre = p.nombreCompleto.split(" ").firstOrNull() ?: ""

                                DropdownMenuItem(
                                    text = { Text("Mensaje Libre (Saludo)") },
                                    onClick = {
                                        showWhatsAppMenu = false
                                        WhatsAppUtils.sendWhatsAppMessage(context, p.telefono, "Hola $nombre,")
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Invitación Control Anual") },
                                    onClick = {
                                        showWhatsAppMenu = false
                                        val msg = "Hola $nombre, te saludamos de Óptica Sersa Visual y Preventiva. Nos preocupamos por tu salud visual; te recordamos que ya se cumplió un año desde tu última evaluación y te invitamos a visitarnos para tu control optométrico anual. ¡Esperamos verte pronto!"
                                        WhatsAppUtils.sendWhatsAppMessage(context, p.telefono, msg)
                                    }
                                )

                                val ultimaEval = evaluaciones.maxByOrNull { it.fecha }
                                if (ultimaEval?.proximaCita != null) {
                                    val proxFecha = com.example.optoapp.util.DateUtils.formatLocalized(ultimaEval.proximaCita)
                                    DropdownMenuItem(
                                        text = { Text("Recordar Próxima Cita ($proxFecha)") },
                                        onClick = {
                                            showWhatsAppMenu = false
                                            val msg = "Hola $nombre, te saludamos de Óptica Sersa Visual y Preventiva. Te recordamos cordialmente que tienes una cita de control optométrico programada con nosotros para el día $proxFecha. ¡Te esperamos!"
                                            WhatsAppUtils.sendWhatsAppMessage(context, p.telefono, msg)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            val p = paciente
                            if (p == null) {
                                Toast.makeText(context, "Esperando datos del paciente…", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            val ultima = evaluaciones.maxByOrNull { it.fecha }
                            if (ultima == null) {
                                Toast.makeText(context, "No hay evaluaciones para generar la fórmula en PDF", Toast.LENGTH_LONG).show()
                                return@IconButton
                            }
                            try {
                                val file = RecetaEvaluacionPdfGenerator.generate(context, p, ultima)
                                RecetaEvaluacionPdfGenerator.openPdf(context, file)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo generar el PDF: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar fórmula en PDF")
                    }
                    IconButton(onClick = { navController.navigate("editarPaciente/${id}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Perfil")
                    }
                    IconButton(
                        onClick = { showDeletePacienteDialog = true },
                        enabled = !deletingPaciente
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar Paciente")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when(selectedTab) {
                        0 -> navController.navigate("nuevaEvaluacion/${id}")
                        1 -> navController.navigate("nuevaDispensacion/${id}")
                        2 -> navController.navigate("nuevo_servicio/${id}")
                    }
                },
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
    ) { padding ->
        paciente?.let { p ->
            if (showDeletePacienteDialog) {
                AlertDialog(
                    onDismissRequest = { if (!deletingPaciente) showDeletePacienteDialog = false },
                    title = { Text("¿Eliminar paciente?") },
                    text = {
                        Text(
                            "Esta acción eliminará también evaluaciones, dispensaciones y servicios relacionados. " +
                                "Existe un límite diario de seguridad."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (deletingPaciente) return@TextButton
                                deletingPaciente = true
                                val pacienteToDelete = p
                                scope.launch {
                                    try {
                                        when (val result = pacienteViewModel.deletePacienteGuarded(pacienteToDelete)) {
                                            is DeletePacienteResult.Success -> {
                                                Toast.makeText(
                                                    context,
                                                    "Paciente eliminado. Quedan ${result.remainingDeletesToday} eliminaciones hoy.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                showDeletePacienteDialog = false
                                                navController.popBackStack()
                                            }
                                            is DeletePacienteResult.Error -> {
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } finally {
                                        deletingPaciente = false
                                    }
                                }
                            },
                            enabled = !deletingPaciente
                        ) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeletePacienteDialog = false },
                            enabled = !deletingPaciente
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                PacienteInfoHeader(paciente = p)

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> EvaluacionesList(evaluaciones, p, evaluacionViewModel) { evalId ->
                            navController.navigate("editarEvaluacion/${id}/${evalId}")
                        }
                        1 -> DispensacionesList(
                            dispensaciones = dispensaciones,
                            paciente = p,
                            evaluaciones = evaluaciones,
                            onEdit = { dispId ->
                                navController.navigate("editarDispensacion/${id}/${dispId}")
                            }
                        )
                        2 -> ServiciosExtraList(servicios) { servId ->
                            navController.navigate("editar_servicio/${servId}")
                        }
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
