package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.viewmodel.ServiciosViewModel
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoServicioScreen(navController: NavController, pacienteId: String? = null, servicioId: String? = null, viewModel: ServiciosViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val pacientes by viewModel.pacientes.collectAsState()

    var pSearchQuery by remember { mutableStateOf("") }
    val filteredPacientes = if (pSearchQuery.isEmpty()) pacientes 
    else pacientes.filter { it.nombreCompleto.contains(pSearchQuery, ignoreCase = true) }

    val saldo = (uiState.montoTotal.toDoubleOrNull() ?: 0.0) - (uiState.aCuenta.toDoubleOrNull() ?: 0.0)

    LaunchedEffect(servicioId) {
        if (servicioId != null && servicioId != "null") {
            viewModel.loadServicio(servicioId)
        } else if (pacienteId != null && pacienteId != "null") {
            viewModel.updateUiState { it.copy(pacienteId = pacienteId) }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.dateToLong(DateUtils.longToDate(uiState.fecha)),
        yearRange = 1920..2080
    )
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { mills ->
                        viewModel.updateUiState { it.copy(fecha = DateUtils.dateToLong(DateUtils.longToDate(mills))) }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (servicioId == null || servicioId == "null") "Nuevo Servicio" else "Editar Servicio") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OptoTextField(value = uiState.ot, onValueChange = { viewModel.updateUiState { s -> s.copy(ot = it) } }, label = "OT (Opcional)")
            OptoTextField(value = uiState.descripcion, onValueChange = { viewModel.updateUiState { s -> s.copy(descripcion = it) } }, label = "Descripción")
            
            OptoTextField(
                value = uiState.montoTotal, 
                onValueChange = { viewModel.updateUiState { s -> s.copy(montoTotal = it) } }, 
                label = "Monto Total"
            )
            
            OptoTextField(
                value = uiState.aCuenta, 
                onValueChange = { viewModel.updateUiState { s -> s.copy(aCuenta = it) } }, 
                label = "A cuenta"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Saldo Restante:", fontWeight = FontWeight.Bold)
                    Text(
                        text = "s/. ${String.format(Locale.getDefault(), "%.2f", saldo)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (saldo > 0) Color.Red else Color(0xFF4CAF50)
                    )
                }
            }

            DropdownField(
                label = "Estado", 
                selected = uiState.estado, 
                options = listOf("Pendiente", "Entregado"), 
                onSelected = { viewModel.updateUiState { s -> s.copy(estado = it) } }
            )

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Text("Fecha: ${fmt.format(Date(uiState.fecha))}")
            }

            Text("Asociar a Paciente (Opcional)", fontWeight = FontWeight.Bold)
            var pExpanded by remember { mutableStateOf(false) }
            val currentPacienteName = pacientes.find { it.id == uiState.pacienteId }?.nombreCompleto ?: "Ninguno"
            
            ExposedDropdownMenuBox(expanded = pExpanded, onExpandedChange = { pExpanded = !pExpanded }) {
                OutlinedTextField(
                    value = pSearchQuery.ifBlank { if (pExpanded) "" else currentPacienteName },
                    onValueChange = { pSearchQuery = it },
                    label = { Text("Buscar Paciente...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(expanded = pExpanded, onDismissRequest = { pExpanded = false }) {
                    DropdownMenuItem(text = { Text("Ninguno") }, onClick = { 
                        viewModel.updateUiState { it.copy(pacienteId = null) }
                        pSearchQuery = ""
                        pExpanded = false 
                    })
                    filteredPacientes.forEach { p ->
                        DropdownMenuItem(text = { Text(p.nombreCompleto) }, onClick = { 
                            viewModel.updateUiState { it.copy(pacienteId = p.id) }
                            pSearchQuery = ""
                            pExpanded = false 
                        })
                    }
                }
            }
        }
    }
}

