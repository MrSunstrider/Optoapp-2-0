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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.R
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OrderReadOnlyBanner
import com.example.optoapp.ui.components.WizardStepHeader
import com.example.optoapp.ui.components.servicio.ServicioEjecutorSection
import com.example.optoapp.ui.components.servicio.ServicioForm

private val WIZARD_STEPS = listOf(R.string.servicio_nuevo_step_datos, R.string.servicio_nuevo_step_pagos)

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
    val allServicios by viewModel.allServicios.collectAsState()
    val cancelableServicioIds by viewModel.cancelableServicioIds.collectAsState()
    val showAnularDialog by viewModel.showDeleteDialog.collectAsState()
    val anulando by viewModel.anulando.collectAsState()
    val anularError by viewModel.deleteError.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentStep by remember { mutableIntStateOf(0) }

    val isPacienteLocked = pacienteId != null && pacienteId != "null" && (servicioId == null || servicioId == "null")

    LaunchedEffect(uiState.error) {
        val msg = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearServicioError()
    }

    LaunchedEffect(anularError) {
        val msg = anularError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearDeleteError()
    }

    LaunchedEffect(infoMessage) {
        val msg = infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearInfoMessage()
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
    val tiposServicio by viewModel.tiposServicio.collectAsState()
    val isEdit = servicioId != null && servicioId != "null"
    val servicioAnulable = allServicios.firstOrNull { it.id == uiState.id }
        ?.takeIf { isEdit && it.id in cancelableServicioIds }
    val canSave = !uiState.isReadOnly

    if (showAnularDialog) {
        AnularServicioDialog(
            descripcion = uiState.descripcion,
            submitting = anulando,
            onConfirm = { motivo -> viewModel.confirmAnular(motivo) { navController.popBackStack() } },
            onDismiss = { viewModel.dismissDeleteDialog() },
        )
    }

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
                title = stringResource(if (!isEdit) R.string.servicio_nuevo_title else R.string.servicio_nuevo_title_editar),
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.servicio_nuevo_atras_cd))
                    }
                },
                actions = {
                    if (currentStep == WIZARD_STEPS.lastIndex) {
                        IconButton(onClick = { saveAction() }, enabled = canSave) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.common_save))
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
                    ) { Text(stringResource(R.string.servicio_nuevo_anterior)) }
                }
                if (currentStep < WIZARD_STEPS.lastIndex) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.servicio_nuevo_siguiente)) }
                } else {
                    Button(
                        onClick = { saveAction() },
                        enabled = canSave,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(if (!isEdit) R.string.servicio_nuevo_guardar else R.string.servicio_nuevo_actualizar)) }
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
                labels = WIZARD_STEPS.map { stringResource(it) },
                currentStep = currentStep,
                totalSteps = WIZARD_STEPS.size,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isReadOnly) {
                OrderReadOnlyBanner(
                    estado = uiState.estado,
                    motivo = uiState.motivoAnulacion,
                    fecha = uiState.fechaAnulacion,
                    servicio = true,
                )
            }

            ServicioForm(
                uiState = uiState,
                onUpdate = { s -> viewModel.updateUiState { s } },
                onUpdateEstado = { viewModel.updateEstado(it) },
                monturas = monturas,
                pacientes = pacientes,
                onAddPago = { viewModel.addPago(it) },
                onUpdatePago = { viewModel.updatePagoLocal(it) },
                onRemovePago = { viewModel.removePagoLocal(it) },
                onAddItem = { viewModel.addItem() },
                onUpdateItem = { index, item -> viewModel.updateItem(index, item) },
                onRemoveItem = { viewModel.removeItem(it) },
                onAddRegalo = { viewModel.addRegalo(it) },
                onRemoveRegalo = { viewModel.removeRegalo(it) },
                onShowDatePicker = { showDatePicker = true },
                step = currentStep,
                isPacienteLocked = isPacienteLocked,
                ejecutorSection = {
                    ServicioEjecutorSection(
                        costo = uiState.costo,
                        tipos = tiposServicio,
                        habilitado = !uiState.isReadOnly,
                        onTipo = viewModel::seleccionarTipoServicio,
                        onRealizadoPor = viewModel::seleccionarRealizadoPor,
                        onNombre = viewModel::updateRealizadoPorNombre,
                        onCosto = viewModel::updateCostoServicio,
                    )
                },
            )

            if (servicioAnulable != null) {
                OutlinedButton(
                    onClick = { viewModel.showDeleteConfirmation(servicioAnulable) },
                    enabled = !anulando,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.servicio_nuevo_anular)) }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
