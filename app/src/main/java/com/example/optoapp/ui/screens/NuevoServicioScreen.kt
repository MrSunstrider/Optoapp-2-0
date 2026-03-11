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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.optoapp.OptoApplication
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.viewmodel.OptoViewModel
import com.example.optoapp.viewmodel.OptoViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoServicioScreen(navController: NavController, pacienteId: String? = null, servicioId: String? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as OptoApplication
    val viewModel: OptoViewModel = viewModel(
        factory = OptoViewModelFactory(app.repository, app.securityManager)
    )
    val scope = rememberCoroutineScope()
    val pacientes by viewModel.pacientes.collectAsState()

    var ot by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var montoTotal by remember { mutableStateOf("") }
    var aCuenta by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }
    var fecha by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedPacienteId by remember { mutableStateOf(pacienteId?.takeIf { it != "null" }) }

    var pSearchQuery by remember { mutableStateOf("") }
    val filteredPacientes = if (pSearchQuery.isEmpty()) pacientes 
    else pacientes.filter { it.nombreCompleto.contains(pSearchQuery, ignoreCase = true) }

    val saldo = (montoTotal.toDoubleOrNull() ?: 0.0) - (aCuenta.toDoubleOrNull() ?: 0.0)

    LaunchedEffect(servicioId) {
        if (servicioId != null) {
            viewModel.getServicioById(servicioId)?.let { s ->
                ot = s.ot
                descripcion = s.descripcion
                montoTotal = s.montoTotal.toString()
                aCuenta = s.aCuenta.toString()
                estado = s.estado
                fecha = s.fecha
                selectedPacienteId = s.pacienteId
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = fecha,
        yearRange = 1920..2080
    )
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { fecha = it }
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
                title = { Text(if (servicioId == null) "Nuevo Servicio" else "Editar Servicio") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (descripcion.isBlank() || montoTotal.isBlank()) return@IconButton
                        val s = ServicioExtra(
                            id = servicioId ?: UUID.randomUUID().toString(),
                            ot = ot,
                            descripcion = descripcion,
                            montoTotal = montoTotal.toDoubleOrNull() ?: 0.0,
                            aCuenta = aCuenta.toDoubleOrNull() ?: 0.0,
                            estado = estado,
                            fecha = fecha,
                            pacienteId = selectedPacienteId
                        )
                        scope.launch {
                            viewModel.saveServicio(s)
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
            OutlinedTextField(value = ot, onValueChange = { ot = it }, label = { Text("OT (Opcional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
            
            OutlinedTextField(
                value = montoTotal, 
                onValueChange = { montoTotal = it }, 
                label = { Text("Monto Total") }, 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = aCuenta, 
                onValueChange = { aCuenta = it }, 
                label = { Text("A cuenta") }, 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Saldo Restante:", fontWeight = FontWeight.Bold)
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%.2f", saldo)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (saldo > 0) Color.Red else Color(0xFF4CAF50)
                    )
                }
            }

            DropdownField(
                label = "Estado", 
                selected = estado, 
                options = listOf("Pendiente", "Entregado"), 
                onSelected = { estado = it }
            )

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Text("Fecha: ${fmt.format(Date(fecha))}")
            }

            Text("Asociar a Paciente (Opcional)", fontWeight = FontWeight.Bold)
            var pExpanded by remember { mutableStateOf(false) }
            val currentPacienteName = pacientes.find { it.id == selectedPacienteId }?.nombreCompleto ?: "Ninguno"
            
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
                        selectedPacienteId = null
                        pSearchQuery = ""
                        pExpanded = false 
                    })
                    filteredPacientes.forEach { p ->
                        DropdownMenuItem(text = { Text(p.nombreCompleto) }, onClick = { 
                            selectedPacienteId = p.id
                            pSearchQuery = ""
                            pExpanded = false 
                        })
                    }
                }
            }
        }
    }
}
