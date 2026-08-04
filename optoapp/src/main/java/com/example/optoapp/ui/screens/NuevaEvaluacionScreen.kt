package com.example.optoapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.OSDIDialog
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.StepIndicator
import com.example.optoapp.ui.components.evaluacion.AnamnesisSection
import com.example.optoapp.ui.components.evaluacion.CierreSection
import com.example.optoapp.ui.components.evaluacion.ContactologiaSection
import com.example.optoapp.ui.components.evaluacion.ExamenVisualSection
import com.example.optoapp.ui.components.evaluacion.RefraccionSection
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.EvaluacionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaEvaluacionScreen(
    navController: NavController,
    pacienteId: String,
    evaluacionId: String? = null,
    viewModel: EvaluacionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableIntStateOf(0) }
    val stepLabels = listOf("Anamnesis", "Examen", "Refracción", "LC", "Cierre")

    var aplicarRecorteOd by remember { mutableStateOf(false) }
    var aplicarRecorteOi by remember { mutableStateOf(false) }

    LaunchedEffect(evaluacionId) {
        if (evaluacionId != null) {
            viewModel.loadEvaluacion(evaluacionId)
        } else {
            viewModel.updateUiState {
                it.copy(
                    diagnosticoOd = emptyList(),
                    diagnosticoOi = emptyList(),
                    otrosPresbicia = false,
                    otrosAnisometropia = false,
                    otrosAmbliopia = false,
                )
            }
            viewModel.loadPacienteEdadAndCalculateAdd(pacienteId)
        }
    }

    LaunchedEffect(uiState.recetaOdEsf, uiState.recetaOdCil, uiState.balanceOd) {
        viewModel.updateDiagnosticAuto()
    }
    LaunchedEffect(uiState.recetaOiEsf, uiState.recetaOiCil, uiState.balanceOi) {
        viewModel.updateDiagnosticAuto()
    }

    LaunchedEffect(
        uiState.addCercaOd, uiState.addCercaOi,
        uiState.addIntermediaOd, uiState.addIntermediaOi,
        uiState.avCcOdLejos, uiState.avCcOiLejos,
        uiState.isAddAo,
        uiState.recetaOdEsf, uiState.recetaOdCil, uiState.recetaOdEje,
        uiState.recetaOiEsf, uiState.recetaOiCil, uiState.recetaOiEje,
        uiState.balanceOd, uiState.balanceOi,
    ) {
        viewModel.updateOtrosAuto()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showProximaDatePicker by remember { mutableStateOf(false) }
    var showLcDatePicker by remember { mutableStateOf(false) }
    var showOsdiDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = uiState.fecha,
            onDateSelected = { date ->
                viewModel.updateUiState { it.copy(fecha = date) }
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showProximaDatePicker) {
        OptoDatePickerDialog(
            initialDate = uiState.proximaCita ?: DateUtils.today(),
            onDateSelected = { date ->
                viewModel.updateUiState { it.copy(proximaCita = date) }
            },
            onDismiss = { showProximaDatePicker = false },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.updateUiState { it.copy(proximaCita = null, citaEstado = "programada") }
                    showProximaDatePicker = false
                }) { Text("Limpiar") }
            },
        )
    }

    if (showLcDatePicker) {
        OptoDatePickerDialog(
            initialDate = uiState.lcFechaAdaptacion ?: DateUtils.today(),
            onDateSelected = { date ->
                viewModel.updateUiState { it.copy(lcFechaAdaptacion = date) }
            },
            onDismiss = { showLcDatePicker = false },
        )
    }

    if (showOsdiDialog) {
        OSDIDialog(
            onDismissRequest = { showOsdiDialog = false },
            onSave = { puntuacion, clasificacion ->
                viewModel.updateUiState {
                    it.copy(osdiPuntuacion = puntuacion, osdiClasificacion = clasificacion)
                }
                showOsdiDialog = false
            },
        )
    }

    // AlertDialog para errores visibles
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.updateUiState { it.copy(error = null) } },
            title = { Text("Aviso") },
            text = { Text(uiState.error ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.updateUiState { it.copy(error = null) } }) {
                    Text("OK")
                }
            },
        )
    }

    val saveAction = {
        val sharedPreferences = context.getSharedPreferences("optoapp_prefs", Context.MODE_PRIVATE)
        val programarRecordatorioGlobal = sharedPreferences.getBoolean("pref_enable_reminders", true)

        viewModel.saveAndScheduleReminder(pacienteId, evaluacionId, programarRecordatorioGlobal) {
            navController.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier.testTag(TestTags.EVALUACION_SCREEN_ROOT),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = if (evaluacionId == null) "Nueva Evaluación" else "Editar Evaluación",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { saveAction() }, modifier = Modifier.testTag(TestTags.EVALUACION_GUARDAR_BTN)) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(onClick = { currentStep-- }) {
                            Text("Anterior")
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    if (currentStep < 4) {
                        Button(onClick = { currentStep++ }) {
                            Text("Siguiente")
                        }
                    } else {
                        Button(onClick = { saveAction() }) {
                            Text("Finalizar")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            StepIndicator(
                currentStep = currentStep,
                totalSteps = 5,
                labels = stepLabels,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            HorizontalDivider()

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (currentStep) {
                    0 -> AnamnesisSection(
                        uiState = uiState,
                        onUpdate = { s -> viewModel.updateUiState { s } },
                        onShowDatePicker = { showDatePicker = true },
                    )
                    1 -> ExamenVisualSection(
                        uiState = uiState,
                        onUpdate = { s -> viewModel.updateUiState { s } },
                        onShowOsdiDialog = { showOsdiDialog = true },
                    )
                    2 -> RefraccionSection(
                        uiState = uiState,
                        onUpdate = { s -> viewModel.updateUiState { s } },
                        viewModel = viewModel,
                    )
                    3 -> ContactologiaSection(
                        uiState = uiState,
                        onUpdate = { s -> viewModel.updateUiState { s } },
                        aplicarRecorteOd = aplicarRecorteOd,
                        aplicarRecorteOi = aplicarRecorteOi,
                        onRecorteOdChange = { aplicarRecorteOd = it },
                        onRecorteOiChange = { aplicarRecorteOi = it },
                        onShowLcDatePicker = { showLcDatePicker = true },
                    )
                    4 -> CierreSection(
                        uiState = uiState,
                        onUpdate = { s -> viewModel.updateUiState { s } },
                        onShowProximaDatePicker = { showProximaDatePicker = true },
                        onSave = { saveAction() },
                        evaluacionId = evaluacionId,
                    )
                }
            }
        }
    }
}
