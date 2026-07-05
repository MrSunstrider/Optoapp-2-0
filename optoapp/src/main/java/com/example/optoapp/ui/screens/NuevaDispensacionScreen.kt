package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.dispensacion.LenteForm
import com.example.optoapp.viewmodel.DispensacionViewModel
import com.example.optoapp.util.DateUtils
import com.example.optoapp.ui.components.OptoTopAppBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaDispensacionScreen(navController: NavController, pacienteId: String, dispensacionId: String? = null, viewModel: DispensacionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val monturasActivas by viewModel.monturasActivas.collectAsState()
    LaunchedEffect(dispensacionId) {
        if (dispensacionId != null) {
            viewModel.loadDispensacion(dispensacionId)
        }
    }
    LaunchedEffect(pacienteId) {
        viewModel.loadPacienteNombre(pacienteId)
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
            if (dispensacionId == null) {
                navController.navigate("informacion_financiera/${viewModel.uiState.value.generatedId}") {
                    popUpTo("nuevaDispensacion/$pacienteId") { inclusive = true }
                }
            } else {
                navController.popBackStack()
            }
        }
    }
    Scaffold(
        modifier = Modifier.testTag(TestTags.DISPENSACION_SCREEN_ROOT),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            OptoTopAppBar(
                title = if (dispensacionId == null) "Nueva Dispensación" else "Editar Dispensación",
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    modifier = Modifier.weight(1f).testTag(TestTags.DISPENSACION_OT_FIELD)
                )
                TextButton(onClick = { viewModel.suggestOt() }) {
                    Text("Sugerir OT", fontSize = 13.sp)
                }
            }

            Text(
                "Productos",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )

            uiState.items.forEachIndexed { index, item ->
                LenteForm(
                    item = item,
                    index = index,
                    isOnlyItem = uiState.items.size <= 1,
                    monturasActivas = monturasActivas,
                    onUpdate = { updated -> viewModel.updateItem(index, updated) },
                    onRemove = { viewModel.removeItem(index) }
                )
            }

            OutlinedButton(
                onClick = { viewModel.addItem() },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.DISPENSACION_AGREGAR_ITEM_BTN)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar otro producto (lente + montura)")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Información Financiera", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    if (dispensacionId == null) {
                        OptoTextField(
                            value = uiState.montoTotal,
                            onValueChange = { value -> viewModel.updateUiState { it.copy(montoTotal = value) } },
                            label = "Monto Total",
                            keyboardType = KeyboardType.Decimal
                        )
                        Text("Estado: ${uiState.estadoEntrega}", fontWeight = FontWeight.Bold)
                    } else {
                        val monto = uiState.montoTotal.toDoubleOrNull() ?: 0.0
                        val pagado = uiState.pagos.sumOf { it.monto }
                        val saldo = monto - pagado
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Monto Total:", fontWeight = FontWeight.Medium)
                            Text("s/. ${"%.2f".format(monto)}", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Saldo:", fontWeight = FontWeight.Medium)
                            Text("s/. ${"%.2f".format(saldo)}", fontWeight = FontWeight.Bold,
                                color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estado:", fontWeight = FontWeight.Medium)
                            Text(uiState.estadoEntrega, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { navController.navigate("informacion_financiera/$dispensacionId") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Gestionar Pagos")
                        }
                    }
                }
            }

            Button(onClick = { saveAction() }, modifier = Modifier.fillMaxWidth().testTag(TestTags.DISPENSACION_GUARDAR_BTN)) {
                Text(if (dispensacionId == null) "Confirmar Orden" else "Actualizar Orden")
            }
            if (dispensacionId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.deleteDispensacion(dispensacionId) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar Orden")
                }
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
