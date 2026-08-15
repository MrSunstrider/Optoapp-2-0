package com.example.optoapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Resource
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.FormActions
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoFormShell
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.components.paciente.PacienteFormSections
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.PacienteViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

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
    var pacienteLoadError by remember { mutableStateOf(false) }

    LaunchedEffect(pacienteId) {
        if (pacienteId != null) {
            when (val result = viewModel.getPaciente(pacienteId)) {
                is Resource.Success -> result.data?.let { it ->
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
                is Resource.Error -> {
                    Toast.makeText(ctx, "No se pudo cargar el paciente: ${result.message}", Toast.LENGTH_LONG).show()
                    pacienteLoadError = true
                }
                is Resource.Loading -> { }
            }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = fechaCreacion,
            onDateSelected = { fechaCreacion = it },
            onDismiss = { showDatePicker = false },
        )
    }

    val saveAction: () -> Unit = {
        if (!saving) {
            if (pacienteLoadError && pacienteId != null) {
                Toast.makeText(ctx, "No se pudo cargar el paciente para editar", Toast.LENGTH_LONG).show()
            } else if (nombreCompleto.isNotBlank() && edad.isNotBlank() && telefono.isNotBlank()) {
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
                    hobbies = hobbies,
                )
                scope.launch {
                    saving = true
                    try {
                        viewModel.savePaciente(p)
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        navController.navigate(Route.DetallePaciente(p.id).route) {
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
        }
    }

    OptoFormShell(
        title = if (pacienteId == null) "Nuevo Paciente" else "Editar Paciente",
        onNavigateBack = { navController.popBackStack() },
        modifier = Modifier.testTag(TestTags.PACIENTE_SCREEN_ROOT),
        onSave = saveAction,
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                FormActions(
                    onSave = saveAction,
                    onCancel = { navController.popBackStack() },
                    saveEnabled = !saving,
                    cancelEnabled = !saving,
                    saveLoading = saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        },
    ) {
        PacienteFormSections(
            nombreCompleto = nombreCompleto,
            onNombreCompletoChange = { nombreCompleto = it },
            edad = edad,
            onEdadChange = {
                edad = it
                fechaNacimiento = ""
            },
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
                        } else {
                            edad
                        }
                    } else {
                        edad
                    }
                } catch (_: Exception) {
                    edad
                }
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
            },
        )
    }
}
