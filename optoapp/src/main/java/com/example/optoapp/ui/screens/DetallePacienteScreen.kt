package com.example.optoapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.activity.ComponentActivity
import com.example.optoapp.data.Paciente
import com.example.optoapp.ui.components.paciente.DeletePacienteDialog
import com.example.optoapp.ui.components.paciente.DispensacionesList
import com.example.optoapp.ui.components.paciente.EvaluacionesList
import com.example.optoapp.ui.components.paciente.PacienteInfoHeader
import com.example.optoapp.ui.components.paciente.PacienteWhatsAppMenu
import com.example.optoapp.ui.components.paciente.ServiciosExtraList
import com.example.optoapp.testing.TestTags
import com.example.optoapp.util.RecetaEvaluacionPdfGenerator
import com.example.optoapp.util.FileShareUtils
import com.example.optoapp.viewmodel.DeletePacienteResult
import com.example.optoapp.viewmodel.DispensacionViewModel
import com.example.optoapp.viewmodel.EvaluacionViewModel
import com.example.optoapp.viewmodel.OpticaHeaderViewModel
import com.example.optoapp.viewmodel.PacienteViewModel
import com.example.optoapp.viewmodel.ServiciosViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.example.optoapp.ui.components.OptoTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePacienteScreen(
    navController: NavController,
    id: String,
    pacienteViewModel: PacienteViewModel = hiltViewModel(),
    evaluacionViewModel: EvaluacionViewModel = hiltViewModel(),
    dispensacionViewModel: DispensacionViewModel = hiltViewModel(),
    serviciosViewModel: ServiciosViewModel = hiltViewModel(),
    opticaHeaderVm: OpticaHeaderViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
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

    // Dynamic deuda computed from pagos (montoPagado/aCuenta are @Ignore in entities)
    val pagosSumByDisp by dispensacionViewModel.pagosSumByDispensacion.collectAsState()
    val aCuentaSumByServ by dispensacionViewModel.aCuentaSumByServicio.collectAsState()
    val deudaDisp = dispensaciones.sumOf { it.montoTotal - (pagosSumByDisp[it.id] ?: 0.0) }
    val deudaServ = servicios.sumOf { it.montoTotal - (aCuentaSumByServ[it.id] ?: 0.0) }
    val deudaTotal = deudaDisp + deudaServ

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = "Ficha del Paciente",
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
                        paciente?.let { p ->
                            PacienteWhatsAppMenu(
                                expanded = showWhatsAppMenu,
                                paciente = p,
                                evaluaciones = evaluaciones,
                                onDismiss = { showWhatsAppMenu = false },
                                onSendMessage = { msg ->
                                    FileShareUtils.sendWhatsAppMessage(context, p.telefono, msg)
                                }
                            )
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
                                val opticaNombre = opticaHeaderVm.uiState.value.nombreOptica
                                val file = RecetaEvaluacionPdfGenerator.generate(context, p, ultima, opticaNombre)
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
                }
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
                DeletePacienteDialog(
                    deleting = deletingPaciente,
                    onDismiss = { showDeletePacienteDialog = false },
                    onConfirm = {
                        deletingPaciente = true
                        scope.launch {
                            try {
                                when (val result = pacienteViewModel.deletePacienteGuarded(p)) {
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
                    onCancel = { showDeletePacienteDialog = false }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
            ) {
                PacienteInfoHeader(paciente = p, deudaTotal = deudaTotal)

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
                        val count = when (index) {
                            0 -> evaluaciones.size
                            1 -> dispensaciones.size
                            2 -> servicios.size
                            else -> 0
                        }
                        val label = if (count > 0) "$title ($count)" else title
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(label, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, maxLines = 1) }
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
                            },
                            pagosSumMap = pagosSumByDisp
                        )
                        2 -> ServiciosExtraList(
                            servicios = servicios,
                            onEdit = { servId -> navController.navigate("editar_servicio/${servId}") },
                            aCuentaSumMap = aCuentaSumByServ
                        )
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
