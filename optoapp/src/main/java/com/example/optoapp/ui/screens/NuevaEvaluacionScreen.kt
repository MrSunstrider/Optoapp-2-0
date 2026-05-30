package com.example.optoapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.OSDIDialog
import com.example.optoapp.ui.components.evaluacion.AnamnesisSection
import com.example.optoapp.ui.components.evaluacion.CierreSection
import com.example.optoapp.ui.components.evaluacion.ContactologiaSection
import com.example.optoapp.ui.components.evaluacion.ExamenVisualSection
import com.example.optoapp.ui.components.evaluacion.RefraccionSection
import com.example.optoapp.viewmodel.EvaluacionViewModel
import com.example.optoapp.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaEvaluacionScreen(
    navController: NavController,
    pacienteId: String,
    evaluacionId: String? = null,
    viewModel: EvaluacionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Anamnesis", "Examen Visual", "Refracción", "Contactología", "Cierre")

    var aplicarRecorteOd by remember { mutableStateOf(false) }
    var aplicarRecorteOi by remember { mutableStateOf(false) }

    LaunchedEffect(evaluacionId) {
        if (evaluacionId != null) {
            viewModel.loadEvaluacion(evaluacionId)
        } else {
            viewModel.updateUiState { it.copy(
                diagnosticoOd = emptyList(),
                diagnosticoOi = emptyList(),
                otrosPresbicia = false,
                otrosAnisometropia = false,
                otrosAmbliopia = false
            ) }
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
        uiState.avCcOdLejos, uiState.avCcOiLejos,
        uiState.isAddAo,
        uiState.recetaOdEsf, uiState.recetaOdCil, uiState.recetaOdEje,
        uiState.recetaOiEsf, uiState.recetaOiCil, uiState.recetaOiEje,
        uiState.balanceOd, uiState.balanceOi
    ) {
        viewModel.updateOtrosAuto()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showProximaDatePicker by remember { mutableStateOf(false) }
    var showLcDatePicker by remember { mutableStateOf(false) }
    var showOsdiDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // ─── DIP measurement from camera (iris-based) ────────────────────────
    val dipPhotoUri = remember { mutableStateOf<Uri?>(null) }
    val dipCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            dipPhotoUri.value?.let { uri ->
                viewModel.measureDipFromPhoto(uri)
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted && dipPhotoUri.value != null) {
            dipCameraLauncher.launch(dipPhotoUri.value)
        } else if (!granted) {
            viewModel.updateUiState { it.copy(error = "Permiso de cámara requerido para medir DIP") }
        }
    }
    val onMeasureDipClick: () -> Unit = {
        val photoFile = java.io.File(context.cacheDir, "dip_photos/dip_${System.currentTimeMillis()}.jpg")
        photoFile.parentFile?.mkdirs()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        dipPhotoUri.value = uri
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dipCameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(uiState.fecha),
        yearRange = 1920..2080
    )
    val proximaDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(uiState.proximaCita ?: DateUtils.today()),
        yearRange = 1920..2080
    )
    val lcDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(uiState.lcFechaAdaptacion ?: DateUtils.today()),
        yearRange = 1920..2080
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { mills ->
                        viewModel.updateUiState { it.copy(fecha = DateUtils.pickerMillisToLocalDate(mills)) }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showProximaDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showProximaDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    proximaDatePickerState.selectedDateMillis?.let { mills ->
                        viewModel.updateUiState { it.copy(proximaCita = DateUtils.pickerMillisToLocalDate(mills)) }
                    }
                    showProximaDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.updateUiState { it.copy(proximaCita = null, citaEstado = "programada") }
                    showProximaDatePicker = false
                }) { Text("Limpiar") }
            }
        ) {
            DatePicker(state = proximaDatePickerState)
        }
    }

    if (showLcDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showLcDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    lcDatePickerState.selectedDateMillis?.let { mills ->
                        viewModel.updateUiState { it.copy(lcFechaAdaptacion = DateUtils.pickerMillisToLocalDate(mills)) }
                    }
                    showLcDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = lcDatePickerState)
        }
    }

    if (showOsdiDialog) {
        OSDIDialog(
            onDismissRequest = { showOsdiDialog = false },
            onSave = { puntuacion, clasificacion ->
                viewModel.updateUiState {
                    it.copy(osdiPuntuacion = puntuacion, osdiClasificacion = clasificacion)
                }
                showOsdiDialog = false
            }
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
            }
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
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(if (evaluacionId == null) "Nueva Evaluación" else "Editar Evaluación") },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    0 -> AnamnesisSection(
                        uiState = uiState,
                        onUpdate = { s -> viewModel.updateUiState { s } },
                        onShowDatePicker = { showDatePicker = true }
                    )
                    1 -> ExamenVisualSection(
                        uiState = uiState,
                        onUpdate = { s -> viewModel.updateUiState { s } },
                        onShowOsdiDialog = { showOsdiDialog = true }
                    )
                     2 -> RefraccionSection(
                        uiState = uiState,
                        onUpdate = { s -> viewModel.updateUiState { s } },
                        viewModel = viewModel,
                        onMeasureDipClick = onMeasureDipClick
                    )
                    3 -> ContactologiaSection(
                        uiState = uiState,
                        onUpdate = { s -> viewModel.updateUiState { s } },
                        aplicarRecorteOd = aplicarRecorteOd,
                        aplicarRecorteOi = aplicarRecorteOi,
                        onRecorteOdChange = { aplicarRecorteOd = it },
                        onRecorteOiChange = { aplicarRecorteOi = it },
                        onShowLcDatePicker = { showLcDatePicker = true }
                    )
                    4 -> CierreSection(
                        uiState = uiState,
                        onUpdate = { s -> viewModel.updateUiState { s } },
                        onShowProximaDatePicker = { showProximaDatePicker = true },
                        onSave = { saveAction() },
                        evaluacionId = evaluacionId
                    )
                }
            }
        }
    }
}
