package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.DispensacionViewModel
import com.example.optoapp.util.DateUtils
import java.util.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import com.example.optoapp.data.Pago
import com.example.optoapp.ui.components.AbonoDialog


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaDispensacionScreen(navController: NavController, pacienteId: String, dispensacionId: String? = null, viewModel: DispensacionViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(dispensacionId) {
        if (dispensacionId != null) {
            viewModel.loadDispensacion(dispensacionId)
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

    val saveAction = {
        viewModel.saveDispensacion(pacienteId, dispensacionId) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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

            // Lente Card
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Información del Lente", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    DropdownField(label = "Tipo de Lente", selected = uiState.tipoLente, options = listOf("Monofocal", "Bifocal", "Progresivo", "Ocupacional")) { 
                        viewModel.updateUiState { s -> 
                            val nextS = s.copy(tipoLente = it)
                            if (it != "Bifocal") nextS.copy(subTipoBifocal = "") else nextS
                        } 
                    }
                    
                    if (uiState.tipoLente == "Bifocal") {
                        DropdownField(label = "Sub-tipo Bifocal", selected = uiState.subTipoBifocal, options = listOf("Flaptop", "Invisible")) { 
                            viewModel.updateUiState { s -> s.copy(subTipoBifocal = it) } 
                        }
                    }
                    
                    if (uiState.tipoLente == "Monofocal") {
                        DropdownField(label = "Distancia", selected = uiState.distanciaLente, options = listOf("Lejos", "Intermedia", "Cerca")) { 
                            viewModel.updateUiState { s -> s.copy(distanciaLente = it) } 
                        }
                    }
                    
                    DropdownField(label = "Material", selected = uiState.materialLente, options = listOf("Resina", "Policarbonato", "Cristal", "Trivex")) { 
                        viewModel.updateUiState { s -> s.copy(materialLente = it) } 
                    }
                    Text("Tratamientos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val opts = listOf("Ninguno", "Antireflejo", "Antirayas", "Filtro UV 400", "Fotocromático", "AR Blue Defense")
                    
                    // Lógica para dropdowns dinámicos
                    val currentTrats = if (uiState.tratamientos.isEmpty()) listOf("Ninguno") else uiState.tratamientos + "Ninguno"
                    
                    currentTrats.distinct().forEachIndexed { index, selectedValue ->
                        if (index == 0 || currentTrats[index-1] != "Ninguno") {
                            DropdownField(
                                label = if (index == 0) "Tratamiento Principal" else "Tratamiento Adicional",
                                selected = selectedValue,
                                options = opts
                            ) { selected ->
                                val newList = uiState.tratamientos.toMutableList()
                                if (index < uiState.tratamientos.size) {
                                    if (selected == "Ninguno") newList.removeAt(index)
                                    else newList[index] = selected
                                } else if (selected != "Ninguno") {
                                    newList.add(selected)
                                }
                                viewModel.updateUiState { it.copy(tratamientos = newList.distinct().filter { t -> t != "Ninguno" }) }
                            }
                        }
                    }
                    OptoTextField(value = uiState.colorLente, onValueChange = { viewModel.updateUiState { s -> s.copy(colorLente = it) } }, label = "Color")
                    OptoTextField(value = uiState.notasDiseno, onValueChange = { viewModel.updateUiState { s -> s.copy(notasDiseno = it) } }, label = "Notas de Diseño")
                }
            }

            // Montura Card
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Información de Montura", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    DropdownField(label = "Origen", selected = uiState.origenMontura, options = listOf("Nueva de Tienda", "Traída por paciente")) { 
                        viewModel.updateUiState { s -> s.copy(origenMontura = it) } 
                    }
                    DropdownField(label = "Tipo de Aro", selected = uiState.tipoAro, options = listOf("Aro Completo", "Semi al aire", "Al aire")) { 
                        viewModel.updateUiState { s -> s.copy(tipoAro = it) } 
                    }
                    DropdownField(label = "Material", selected = uiState.materialMontura, options = listOf("Acetato", "Metal", "Carey", "Econ")) { 
                        viewModel.updateUiState { s -> s.copy(materialMontura = it) } 
                    }
                    OptoTextField(value = uiState.descripcionMontura, onValueChange = { viewModel.updateUiState { s -> s.copy(descripcionMontura = it) } }, label = "Descripción (Marca, Modelo)")
                }
            }

            // Financiero Card
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Información Financiera", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
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
                            onDismiss = { showAddDialog = false },
                            onConfirm = { nuevoPago: Pago ->
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
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SALDO RESTANTE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                        Text(
                            text = "s/. " + formattedSaldo,
                            color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary, 
                            fontSize = 32.sp, 
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    DropdownField(label = "Estado de Entrega", selected = uiState.estadoEntrega, options = listOf("Pendiente", "Entregado")) { 
                        viewModel.updateUiState { s -> s.copy(estadoEntrega = it) } 
                    }
                }
            }
            
            Button(onClick = { saveAction() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (dispensacionId == null) "Confirmar Orden" else "Actualizar Orden")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

