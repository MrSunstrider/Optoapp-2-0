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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.optoapp.data.Paciente
import com.example.optoapp.testing.TestTags
import com.example.optoapp.viewmodel.PacienteViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.ui.components.paciente.PacienteFormSections
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.InputFormatters
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.launch
import com.example.optoapp.ui.components.OptoTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoPacienteScreen(navController: NavController, pacienteId: String? = null, viewModel: PacienteViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

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

    var saving by remember { mutableStateOf(false) }
    var showDuplicateHoWarning by remember { mutableStateOf(false) }
    var duplicateHoWarningText by remember { mutableStateOf("") }

    LaunchedEffect(pacienteId) {
        if (pacienteId != null) {
            val p = viewModel.getPaciente(pacienteId)
            p?.let {
                nombreCompleto = it.nombreCompleto
                edad = it.edad.toString()
                telefono = it.telefono.filter { c -> c.isDigit() }
                dni = it.dni ?: ""
                historiaOptometrica = it.historiaOptometrica ?: ""
                fechaNacimiento = it.fechaNacimiento?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))?.filter { c -> c.isDigit() } ?: ""
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

    if (showDuplicateHoWarning) {
        AlertDialog(
            onDismissRequest = { showDuplicateHoWarning = false },
            title = { Text("Advertencia de HO duplicada") },
            text = { Text(duplicateHoWarningText) },
            confirmButton = {
                TextButton(onClick = { showDuplicateHoWarning = false }) { Text("Entendido") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.testTag(TestTags.PACIENTE_SCREEN_ROOT),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = if (pacienteId == null) "Nuevo Paciente" else "Editar Paciente",
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
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PacienteFormSections(
                nombreCompleto = nombreCompleto,
                onNombreCompletoChange = { nombreCompleto = it },
                edad = edad,
                onEdadChange = {},
                telefono = telefono,
                onTelefonoChange = { telefono = it },
                dni = dni,
                onDniChange = { dni = it },
                historiaOptometrica = historiaOptometrica,
                onHistoriaOptometricaChange = { historiaOptometrica = it },
                fechaNacimiento = fechaNacimiento,
                onFechaNacimientoChange = { digits ->
                    fechaNacimiento = digits
                    val formatted = DateUtils.formatDateInput(digits)
                    edad = try {
                        val parts = formatted.split("/")
                        if (parts.size == 3) {
                            val d = parts[0].toIntOrNull() ?: 0
                            val m = parts[1].toIntOrNull() ?: 0
                            val y = parts[2].toIntOrNull() ?: 0
                            if (d in 1..31 && m in 1..12 && y in 1900..2100) {
                                val nac = LocalDate.of(y, m, d)
                                val hoy = DateUtils.today()
                                (hoy.year - nac.year - if (hoy.dayOfYear < nac.dayOfYear) 1 else 0).toString()
                            } else edad
                        } else edad
                    } catch (_: Exception) { edad }
                },
                sexo = sexo,
                onSexoChange = { sexo = it },
                email = email,
                onEmailChange = { email = it },
                direccion = direccion,
                onDireccionChange = { direccion = it },
                distrito = distrito,
                onDistritoChange = { distrito = it },
                ocupacion = ocupacion,
                onOcupacionChange = { ocupacion = it },
                acompanante = acompanante,
                onAcompananteChange = { acompanante = it },
                hobbies = hobbies,
                onHobbiesChange = { hobbies = it },
                fechaCreacion = fechaCreacion,
                onShowDatePicker = { showDatePicker = true },
                onSuggestHo = {
                    scope.launch {
                        historiaOptometrica = viewModel.suggestHistoriaOptometrica()
                    }
                }
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
                        if (nombreCompleto.isNotBlank() && edad.isNotBlank() && telefono.isNotBlank()) {
                            val p = Paciente(
                                id = pacienteId ?: UUID.randomUUID().toString(),
                                nombreCompleto = nombreCompleto,
                                edad = edad.toIntOrNull() ?: 0,
                                telefono = telefono,
                                fechaCreacion = fechaCreacion,
                                dni = dni,
                                historiaOptometrica = historiaOptometrica,
                                fechaNacimiento = fechaNacimiento.takeIf { it.isNotBlank() }?.let { DateUtils.fromDisplayFormat(DateUtils.formatDateInput(it)) },
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
                                            duplicateHoWarningText =
                                                "Ya existe una historia optométrica con ese número en esta óptica."
                                            showDuplicateHoWarning = true
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
                                } catch (e: Exception) {
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    Toast.makeText(ctx, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.weight(1f).testTag(TestTags.PACIENTE_GUARDAR_BTN)
                ) {
                    Text(if (saving) "Guardando…" else "Guardar")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
