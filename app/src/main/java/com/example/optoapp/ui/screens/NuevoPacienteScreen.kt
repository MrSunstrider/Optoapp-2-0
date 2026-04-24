package com.example.optoapp.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.optoapp.data.Paciente
import com.example.optoapp.viewmodel.PacienteViewModel
import com.example.optoapp.viewmodel.SubscriptionViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.optoapp.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoPacienteScreen(navController: NavController, pacienteId: String? = null, viewModel: PacienteViewModel = hiltViewModel(), subscriptionVm: SubscriptionViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val canAdd by subscriptionVm.canAddPaciente.collectAsState()
    var showPaywall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        subscriptionVm.refreshPlanFromServer()
    }

    var nombreCompleto by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var historiaOptometrica by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("Masculino") }
    var email by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var distrito by remember { mutableStateOf("") }
    var ocupacion by remember { mutableStateOf("") }
    var acompanante by remember { mutableStateOf("") }
    var hobbies by remember { mutableStateOf("") }
    var fechaCreacion by remember { mutableStateOf(DateUtils.today()) }

    var expandedSexo by remember { mutableStateOf(false) }
    val sexos = listOf("Masculino", "Femenino")

    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(pacienteId) {
        if (pacienteId != null) {
            val p = viewModel.getPaciente(pacienteId)
            p?.let {
                nombreCompleto = it.nombreCompleto
                edad = it.edad.toString()
                telefono = it.telefono
                dni = it.dni ?: ""
                historiaOptometrica = it.historiaOptometrica ?: ""
                fechaNacimiento = it.fechaNacimiento?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""
                sexo = it.sexo ?: "Masculino"
                email = it.email ?: ""
                direccion = it.direccion ?: ""
                distrito = it.distrito ?: ""
                ocupacion = it.ocupacion ?: ""
                acompanante = it.acompanante ?: ""
                hobbies = it.hobbies ?: ""
                fechaCreacion = it.fechaCreacion
            }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(fechaCreacion),
        yearRange = 1920..2080
    )

    if (showPaywall) {
        AlertDialog(
            onDismissRequest = { showPaywall = false },
            title = { Text("Límite del plan gratuito") },
            text = { Text("No puedes crear más pacientes en el plan gratuito. Actualiza a PRO desde Configuración o compra desde aquí.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val act = ctx as? Activity
                        if (act != null) {
                            subscriptionVm.launchProPurchase(act) { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show() }
                        }
                        showPaywall = false
                    }
                ) { Text("Actualizar plan") }
            },
            dismissButton = { TextButton(onClick = { showPaywall = false }) { Text("Cerrar") } }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        fechaCreacion = DateUtils.pickerMillisToLocalDate(it)
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
                title = { Text(if (pacienteId == null) "Nuevo Paciente" else "Editar Paciente") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fecha de Registro: ${DateUtils.formatLocalized(fechaCreacion)}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = historiaOptometrica,
                    onValueChange = { historiaOptometrica = it },
                    label = { Text("N° Historia Optométrica") },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            historiaOptometrica = viewModel.suggestHistoriaOptometrica()
                        }
                    }
                ) {
                    Text("Sugerir HO")
                }
            }

            OutlinedTextField(
                value = nombreCompleto,
                onValueChange = { nombreCompleto = it },
                label = { Text("Nombre Completo *") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = edad,
                    onValueChange = { if (it.all { char -> char.isDigit() }) edad = it },
                    label = { Text("Edad *") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono *") },
                    modifier = Modifier.weight(2f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
            OutlinedTextField(
                value = dni,
                onValueChange = { dni = it },
                label = { Text("DNI / Cédula") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = fechaNacimiento,
                onValueChange = { fechaNacimiento = it },
                label = { Text("Fecha de Nacimiento (dd/mm/aaaa)") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expandedSexo,
                onExpandedChange = { expandedSexo = !expandedSexo }
            ) {
                OutlinedTextField(
                    value = sexo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sexo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSexo) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedSexo,
                    onDismissRequest = { expandedSexo = false }
                ) {
                    sexos.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                sexo = selectionOption
                                expandedSexo = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = distrito,
                onValueChange = { distrito = it },
                label = { Text("Distrito") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ocupacion,
                onValueChange = { ocupacion = it },
                label = { Text("Ocupación") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = acompanante,
                onValueChange = { acompanante = it },
                label = { Text("Acompañante") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = hobbies,
                onValueChange = { hobbies = it },
                label = { Text("Hobbies / Hábitos") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    enabled = !saving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        if (saving) return@Button
                        if (pacienteId == null && !canAdd) {
                            showPaywall = true
                            return@Button
                        }
                        if (nombreCompleto.isNotBlank() && edad.isNotBlank() && telefono.isNotBlank()) {
                            val p = Paciente(
                                id = pacienteId ?: UUID.randomUUID().toString(),
                                nombreCompleto = nombreCompleto,
                                edad = edad.toIntOrNull() ?: 0,
                                telefono = telefono,
                                fechaCreacion = fechaCreacion,
                                dni = dni,
                                historiaOptometrica = historiaOptometrica,
                                fechaNacimiento = fechaNacimiento.takeIf { it.isNotBlank() }?.let(LocalDate::parse),
                                sexo = sexo,
                                email = email,
                                direccion = direccion,
                                distrito = distrito,
                                ocupacion = ocupacion,
                                acompanante = acompanante,
                                hobbies = hobbies
                            )
                            scope.launch {
                                saving = true
                                try {
                                    val historiaNorm = historiaOptometrica.trim()
                                    if (historiaNorm.isNotEmpty()) {
                                        val duplicated = viewModel.existsDuplicateHistoriaOptometrica(
                                            historia = historiaNorm,
                                            excludePacienteId = pacienteId
                                        )
                                        if (duplicated) {
                                            Toast.makeText(
                                                ctx,
                                                "Ya existe una historia optométrica con ese número en esta óptica.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }
                                    }
                                    viewModel.savePaciente(p)
                                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                                    navController.navigate("detallePaciente/${p.id}") {
                                        if (currentRoute != null) {
                                            popUpTo(currentRoute) { inclusive = true }
                                        }
                                    }
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (saving) "Guardando…" else "Guardar")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
