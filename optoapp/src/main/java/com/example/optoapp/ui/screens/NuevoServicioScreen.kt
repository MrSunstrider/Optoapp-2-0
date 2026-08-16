package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.WizardStepHeader
import com.example.optoapp.ui.components.servicio.ServicioForm

private val WIZARD_STEPS = listOf("Datos", "Pagos")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoServicioScreen(
    navController: NavController,
    pacienteId: String? = null,
    servicioId: String? = null,
    viewModel: com.example.optoapp.viewmodel.ServiciosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val pacientes by viewModel.pacientes.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentStep by remember { mutableIntStateOf(0) }

    val isPacienteLocked = pacienteId != null && pacienteId != "null" && (servicioId == null || servicioId == "null")

    LaunchedEffect(uiState.error) {
        val msg = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearServicioError()
    }

    LaunchedEffect(servicioId) {
        if (servicioId != null && servicioId != "null") {
            viewModel.loadServicio(servicioId)
        } else if (pacienteId != null && pacienteId != "null") {
            viewModel.updateUiState { it.copy(pacienteId = pacienteId) }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = uiState.fecha,
            onDateSelected = { date ->
                viewModel.updateUiState { it.copy(fecha = date) }
            },
            onDismiss = { showDatePicker = false },
        )
    }

    val monturas by viewModel.monturas.collectAsState()
    val isEdit = servicioId != null && servicioId != "null"

    val saveAction = {
        viewModel.saveServicio {
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
        topBar = {
            OptoTopAppBar(
                title = if (!isEdit) "Nuevo Servicio" else "Editar Servicio",
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (currentStep == WIZARD_STEPS.lastIndex) {
                        IconButton(onClick = { saveAction() }) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f),
                    ) { Text("Anterior") }
                }
                if (currentStep < WIZARD_STEPS.lastIndex) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier.weight(1f),
                    ) { Text("Siguiente") }
                } else {
                    Button(
                        onClick = { saveAction() },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (!isEdit) "Guardar Servicio" else "Actualizar Servicio") }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            WizardStepHeader(
                labels = WIZARD_STEPS,
                currentStep = currentStep,
                totalSteps = WIZARD_STEPS.size,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            ServicioForm(
                uiState = uiState,
                onUpdate = { s -> viewModel.updateUiState { s } },
                onUpdateEstado = { viewModel.updateEstado(it) },
                monturas = monturas,
                pacientes = pacientes,
                onAddPago = { viewModel.addPago(it) },
                onUpdatePago = { viewModel.updatePagoLocal(it) },
                onRemovePago = { viewModel.removePagoLocal(it) },
                onShowDatePicker = { showDatePicker = true },
                step = currentStep,
                isPacienteLocked = isPacienteLocked,
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
