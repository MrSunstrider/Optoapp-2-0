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
import com.example.optoapp.ui.components.servicio.ServicioForm
import com.example.optoapp.util.DateUtils
import com.example.optoapp.ui.components.OptoTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoServicioScreen(navController: NavController, pacienteId: String? = null, servicioId: String? = null, viewModel: com.example.optoapp.viewmodel.ServiciosViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pacientes by viewModel.pacientes.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(uiState.fecha),
        yearRange = 1920..2080
    )
    var showDatePicker by remember { mutableStateOf(false) }

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

    val monturas by viewModel.monturas.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            OptoTopAppBar(
                title = if (servicioId == null || servicioId == "null") "Nuevo Servicio" else "Editar Servicio",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveServicio {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ServicioForm(
                uiState = uiState,
                onUpdate = { s -> viewModel.updateUiState { s } },
                monturas = monturas,
                pacientes = pacientes,
                onAddPago = { viewModel.addPago(it) },
                onUpdatePago = { viewModel.updatePagoLocal(it) },
                onRemovePago = { viewModel.removePagoLocal(it) },
                onShowDatePicker = { showDatePicker = true }
            )
        }
    }
}
