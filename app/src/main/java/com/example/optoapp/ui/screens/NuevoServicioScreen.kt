package com.example.optoapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.viewmodel.ServiciosViewModel
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.util.DateUtils
import java.util.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import com.example.optoapp.data.Pago
import com.example.optoapp.ui.components.AbonoDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoServicioScreen(navController: NavController, pacienteId: String? = null, servicioId: String? = null, viewModel: ServiciosViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pacientes by viewModel.pacientes.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        val msg = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearServicioError()
    }

    var pSearchQuery by remember { mutableStateOf("") }
    val filteredPacientes = if (pSearchQuery.isEmpty()) pacientes 
    else pacientes.filter { it.nombreCompleto.contains(pSearchQuery, ignoreCase = true) }


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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
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
            
            var showMonturaDialog by remember { mutableStateOf(false) }
            val monturas by viewModel.monturas.collectAsState()

            if (showMonturaDialog) {
                AlertDialog(
                    onDismissRequest = { showMonturaDialog = false },
                    title = { Text("Seleccionar Producto") },
                    text = {
                        Column {
                            if (monturas.isEmpty()) {
                                Text("No hay monturas con stock en el inventario.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                    items(monturas) { montura ->
                                        ListItem(
                                            headlineContent = { Text("${montura.marca} ${montura.modelo}") },
                                            supportingContent = { Text("Color: ${montura.color} | SKU: ${montura.sku}") },
                                            trailingContent = { Text("s/. ${montura.precio}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                            modifier = Modifier.clickable {
                                                viewModel.updateUiState { s -> 
                                                    s.copy(
                                                        descripcion = "${montura.marca} ${montura.modelo} (${montura.sku})",
                                                        montoTotal = montura.precio.toString()
                                                    ) 
                                                }
                                                showMonturaDialog = false
                                            }
                                        )
                                        HorizontalDivider(thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMonturaDialog = false }) { Text("Cerrar") }
                    }
                )
            }

            OptoTextField(
                value = uiState.descripcion, 
                onValueChange = { viewModel.updateUiState { s -> s.copy(descripcion = it) } }, 
                label = "Descripción",
                trailingIcon = {
                    IconButton(onClick = { showMonturaDialog = true }) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Vincular Inventario", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            
            OptoTextField(
                value = uiState.montoTotal, 
                onValueChange = { viewModel.updateUiState { s -> s.copy(montoTotal = it) } }, 
                label = "Monto Total",
                keyboardType = KeyboardType.Decimal
            )
            
            HorizontalDivider()
            
            Text("Historial de Abonos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            val total = uiState.montoTotal.toDoubleOrNull() ?: 0.0
            val pagado = uiState.pagos.sumOf { it.monto }
            val saldo = total - pagado
            
            uiState.pagos.forEach { pago ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${pago.metodoPago}: s/. ${String.format(Locale.getDefault(), "%.2f", pago.monto)}", fontWeight = FontWeight.Bold)
                            if (pago.nota.isNotEmpty()) {
                                Text(pago.nota, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(DateUtils.formatLocalized(pago.fecha), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            var showEditDialog by remember { mutableStateOf(false) }
                            if (showEditDialog) {
                                AbonoDialog(
                                    pago = pago,
                                    onDismiss = { showEditDialog = false },
                                    onConfirm = { updatedPago: Pago ->
                                        val total = uiState.montoTotal.toDoubleOrNull() ?: 0.0
                                        val otrosAbonos = uiState.pagos
                                            .filter { it.id != pago.id }
                                            .sumOf { it.monto }
                                        if (total <= 0.0) {
                                            viewModel.updateUiState { it.copy(error = FinanzasRemoteDefaults.Messages.MONTO_TOTAL_INVALIDO) }
                                            return@AbonoDialog
                                        }
                                        if (otrosAbonos + updatedPago.monto > total) {
                                            viewModel.updateUiState { it.copy(error = FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL) }
                                            return@AbonoDialog
                                        }
                                        viewModel.updatePagoLocal(updatedPago)
                                        showEditDialog = false
                                    }
                                )
                            }
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.removePagoLocal(pago) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            
            var showAddDialog by remember { mutableStateOf(false) }
            if (showAddDialog) {
                AbonoDialog(
                    defaultFecha = DateUtils.today(),
                    onDismiss = { showAddDialog = false },
                    onConfirm = { nuevoPago: Pago ->
                        val total = uiState.montoTotal.toDoubleOrNull() ?: 0.0
                        val totalConNuevo = uiState.pagos.sumOf { it.monto } + nuevoPago.monto
                        if (total <= 0.0) {
                            viewModel.updateUiState { it.copy(error = FinanzasRemoteDefaults.Messages.MONTO_TOTAL_INVALIDO) }
                            return@AbonoDialog
                        }
                        if (totalConNuevo > total) {
                            viewModel.updateUiState { it.copy(error = FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL) }
                            return@AbonoDialog
                        }
                        viewModel.addPago(nuevoPago)
                        showAddDialog = false
                    }
                )
            }
            
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar Abono")
            }
            
            HorizontalDivider()

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
                        color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
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
                Text("Fecha: ${DateUtils.formatLocalized(uiState.fecha)}")
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


