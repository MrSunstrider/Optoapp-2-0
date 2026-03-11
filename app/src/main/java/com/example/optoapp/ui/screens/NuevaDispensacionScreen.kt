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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.optoapp.OptoApplication
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.viewmodel.OptoViewModel
import com.example.optoapp.viewmodel.OptoViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaDispensacionScreen(navController: NavController, pacienteId: String, dispensacionId: String? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as OptoApplication
    val viewModel: OptoViewModel = viewModel(
        factory = OptoViewModelFactory(app.repository, app.securityManager)
    )
    val scope = rememberCoroutineScope()

    // Lente
    var tipoLente by remember { mutableStateOf("") }
    var distanciaLente by remember { mutableStateOf("") }
    var materialLente by remember { mutableStateOf("") }
    val tratamientos = remember { mutableStateListOf<String>() }
    var colorLente by remember { mutableStateOf("") }
    var notasDiseno by remember { mutableStateOf("") }

    // Montura
    var origenMontura by remember { mutableStateOf("") }
    var tipoAro by remember { mutableStateOf("") }
    var materialMontura by remember { mutableStateOf("") }
    var descripcionMontura by remember { mutableStateOf("") }

    // Financiero
    var montoTotal by remember { mutableStateOf("") }
    var tipoMovimiento by remember { mutableStateOf("") }
    var metodoPago by remember { mutableStateOf("") }
    var montoPagado by remember { mutableStateOf("") }
    var estadoEntrega by remember { mutableStateOf("") }
    var fechaRegistro by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val saldo = (montoTotal.toDoubleOrNull() ?: 0.0) - (montoPagado.toDoubleOrNull() ?: 0.0)

    LaunchedEffect(dispensacionId) {
        if (dispensacionId != null) {
            viewModel.getDispensacion(dispensacionId)?.let { disp ->
                tipoLente = disp.tipoLente
                distanciaLente = disp.distanciaLente
                materialLente = disp.materialLente
                tratamientos.clear()
                tratamientos.addAll(disp.tratamientos)
                colorLente = disp.colorLente
                notasDiseno = disp.notasDiseno
                origenMontura = disp.origenMontura
                tipoAro = disp.tipoAro
                materialMontura = disp.materialMontura
                descripcionMontura = disp.descripcionMontura
                montoTotal = disp.montoTotal.toString()
                montoPagado = disp.montoPagado.toString()
                metodoPago = disp.metodoPago
                estadoEntrega = if (disp.estadoEntrega == "Parcial") "Pendiente" else disp.estadoEntrega
                fechaRegistro = disp.fecha
            }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = fechaRegistro,
        yearRange = 1920..2080
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { fechaRegistro = it }
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
                title = { Text(if (dispensacionId == null) "Nueva Dispensación" else "Editar Dispensación") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val disp = DispensacionOptica(
                            id = dispensacionId ?: UUID.randomUUID().toString(),
                            pacienteId = pacienteId,
                            fecha = fechaRegistro,
                            tipoLente = tipoLente,
                            materialLente = materialLente,
                            tratamientos = tratamientos.toList(),
                            colorLente = colorLente,
                            notasDiseno = notasDiseno,
                            origenMontura = origenMontura,
                            tipoAro = tipoAro,
                            materialMontura = materialMontura,
                            descripcionMontura = descripcionMontura,
                            montoTotal = montoTotal.toDoubleOrNull() ?: 0.0,
                            montoPagado = montoPagado.toDoubleOrNull() ?: 0.0,
                            metodoPago = metodoPago,
                            estadoEntrega = estadoEntrega,
                            fechaVencimientoGarantia = null,
                            distanciaLente = if (tipoLente == "Monofocal") distanciaLente else ""
                        )
                        scope.launch {
                            viewModel.saveDispensacion(disp)
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
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Text("Fecha: ${fmt.format(Date(fechaRegistro))}")
            }

            // Lente Card
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Información del Lente", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    DropdownField(label = "Tipo de Lente", selected = tipoLente, options = listOf("Monofocal", "Bifocal", "Progresivo", "Ocupacional")) { tipoLente = it }
                    
                    if (tipoLente == "Monofocal") {
                        DropdownField(label = "Distancia", selected = distanciaLente, options = listOf("Lejos", "Intermedia", "Cerca")) { distanciaLente = it }
                    }
                    
                    DropdownField(label = "Material", selected = materialLente, options = listOf("Resina", "Policarbonato", "Cristal", "Trivex")) { materialLente = it }
                    Text("Tratamientos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val opts = listOf("Antireflejo", "Antirayas", "Filtro UV 400", "Fotocromático", "AR Blue Defense")
                    opts.forEach { opt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = tratamientos.contains(opt), onCheckedChange = { if (it) tratamientos.add(opt) else tratamientos.remove(opt) })
                            Text(opt)
                        }
                    }
                    OutlinedTextField(value = colorLente, onValueChange = { colorLente = it }, label = { Text("Color") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notasDiseno, onValueChange = { notasDiseno = it }, label = { Text("Notas de Diseño") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Montura Card
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Información de Montura", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    DropdownField(label = "Origen", selected = origenMontura, options = listOf("Nueva de Tienda", "Traída por paciente")) { origenMontura = it }
                    DropdownField(label = "Tipo de Aro", selected = tipoAro, options = listOf("Aro Completo", "Semi al aire", "Al aire")) { tipoAro = it }
                    DropdownField(label = "Material", selected = materialMontura, options = listOf("Acetato", "Metal", "Carey", "Econ")) { materialMontura = it }
                    OutlinedTextField(value = descripcionMontura, onValueChange = { descripcionMontura = it }, label = { Text("Descripción (Marca, Modelo)") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Financiero Card
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Información Financiera", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    OutlinedTextField(
                        value = montoTotal, 
                        onValueChange = { montoTotal = it }, 
                        label = { Text("Monto Total") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    
                    DropdownField(label = "Tipo de Movimiento", selected = tipoMovimiento, options = listOf("Anticipo", "Cancelacion de saldo")) { tipoMovimiento = it }
                    DropdownField(label = "Método de Pago", selected = metodoPago, options = listOf("Efectivo", "Tarjeta", "Transferencia")) { metodoPago = it }
                    
                    OutlinedTextField(
                        value = montoPagado, 
                        onValueChange = { montoPagado = it }, 
                        label = { Text("A cuenta / Monto Pagado") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    HorizontalDivider()
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SALDO RESTANTE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                        Text(
                            text = "$" + formattedSaldo,
                            color = if (saldo > 0) Color.Red else Color(0xFF4CAF50), 
                            fontSize = 32.sp, 
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    DropdownField(label = "Estado de Entrega", selected = estadoEntrega, options = listOf("Pendiente", "Entregado")) { estadoEntrega = it }
                }
            }
            
            Button(onClick = { 
                val disp = DispensacionOptica(
                    id = dispensacionId ?: UUID.randomUUID().toString(),
                    pacienteId = pacienteId,
                    fecha = fechaRegistro,
                    tipoLente = tipoLente,
                    materialLente = materialLente,
                    tratamientos = tratamientos.toList(),
                    colorLente = colorLente,
                    notasDiseno = notasDiseno,
                    origenMontura = origenMontura,
                    tipoAro = tipoAro,
                    materialMontura = materialMontura,
                    descripcionMontura = descripcionMontura,
                    montoTotal = montoTotal.toDoubleOrNull() ?: 0.0,
                    montoPagado = montoPagado.toDoubleOrNull() ?: 0.0,
                    metodoPago = metodoPago,
                    estadoEntrega = estadoEntrega,
                    fechaVencimientoGarantia = null,
                    distanciaLente = if (tipoLente == "Monofocal") distanciaLente else ""
                )
                scope.launch {
                    viewModel.saveDispensacion(disp)
                    navController.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (dispensacionId == null) "Confirmar Orden" else "Actualizar Orden")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
