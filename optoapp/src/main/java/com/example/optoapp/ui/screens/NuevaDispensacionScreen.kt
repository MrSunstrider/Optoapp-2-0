package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.DispensacionViewModel
import com.example.optoapp.util.DateUtils


@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun NuevaDispensacionScreen(navController: NavController, pacienteId: String, dispensacionId: String? = null, viewModel: DispensacionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val monturasActivas by viewModel.monturasActivas.collectAsState()
    var showDuplicateOtWarning by remember { mutableStateOf(false) }
    var duplicateOtWarningText by remember { mutableStateOf("") }
    LaunchedEffect(dispensacionId) {
        if (dispensacionId != null) {
            viewModel.loadDispensacion(dispensacionId)
        }
    }
    LaunchedEffect(pacienteId) {
        viewModel.loadPacienteNombre(pacienteId)
    }
    LaunchedEffect(uiState.error) {
        val msg = uiState.error.orEmpty()
        if (msg.contains("Ya existe una dispensación con esta OT", ignoreCase = true)) {
            duplicateOtWarningText = msg
            showDuplicateOtWarning = true
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(uiState.fecha),
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

    if (showDuplicateOtWarning) {
        AlertDialog(
            onDismissRequest = { showDuplicateOtWarning = false },
            title = { Text("Advertencia de OT duplicada") },
            text = { Text(duplicateOtWarningText) },
            confirmButton = {
                TextButton(onClick = { showDuplicateOtWarning = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    val saveAction = {
        viewModel.saveDispensacion(pacienteId, dispensacionId) {
            navController.popBackStack()
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(if (dispensacionId == null) "Nueva Dispensación" else "Editar Dispensación") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { saveAction() }) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Fecha: ${DateUtils.formatLocalized(uiState.fecha)}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OptoTextField(
                    value = uiState.ot,
                    onValueChange = { viewModel.updateUiState { s -> s.copy(ot = it) } },
                    label = "N° OT (OT-AAAA-####)",
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.suggestOt() }) {
                    Text("Sugerir OT", fontSize = 13.sp)
                }
            }

            LenteInfoSection(
                uiState = uiState,
                onUpdate = { newState -> viewModel.updateUiState { newState } }
            )

            MonturaInfoSection(
                uiState = uiState,
                monturasActivas = monturasActivas,
                onUpdate = { newState -> viewModel.updateUiState { newState } }
            )

            FinancieraInfoSection(
                uiState = uiState,
                onUpdate = { newState -> viewModel.updateUiState { newState } },
                onAddPago = { pago -> viewModel.addPago(pago) },
                onUpdatePago = { pago -> viewModel.updatePagoLocal(pago) },
                onRemovePago = { pago -> viewModel.removePagoLocal(pago) }
            )
            
            Button(onClick = { saveAction() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (dispensacionId == null) "Confirmar Orden" else "Actualizar Orden")
            }
            if (!uiState.error.isNullOrBlank()) {
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
