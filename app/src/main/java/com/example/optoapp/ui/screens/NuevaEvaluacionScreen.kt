package com.example.optoapp.ui.screens

import android.content.Context

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.ui.components.CalculationCard
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.SuggestionCard
import com.example.optoapp.ui.components.OSDIDialog
import com.example.optoapp.viewmodel.EvaluacionViewModel
import com.example.optoapp.util.DateUtils
import java.util.Locale

val basesPrisma = listOf("Nasal", "Temporal", "Superior", "Inferior")
val diagnosticosRefraccion = listOf(
    "Emetropía", 
    "Miopía", 
    "Hipermetropía", 
    "Astigmatismo miópico simple", 
    "Astigmatismo miópico compuesto", 
    "Astigmatismo hipermetrópico simple",
    "Astigmatismo hipermetrópico compuesto", 
    "Astigmatismo mixto",
    "Balance"
)

val tiposLC = listOf("Blanda", "Rígida (RGP)", "Tórica", "Multifocal", "Cosmética")
val materialesLC = listOf("Hidrogel", "Silicona Hidrogel", "PMMA", "Gas Permeable")

val estereopsisOptions = listOf("Normal", "Reducida", "Ausente")
val langOptions = listOf("Positivo", "Negativo")
val worthOptions = listOf("Fusión normal", "Supresión OD", "Supresión OI", "Diplopía")
val farnsworthOptions = listOf("Normal", "Deutan", "Protan", "Tritan")
val sensibilidadOptions = listOf("Normal", "Disminuida")
val campoVisualOptions = listOf("Normal", "Anomalía detectada")

private val estadosCitaOpciones = listOf(
    "programada" to "Programada",
    "confirmada" to "Confirmada",
    "asistio" to "Asistió",
    "no_asistio" to "No asistió",
    "reprogramada" to "Reprogramada"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaEvaluacionScreen(
    navController: NavController,
    pacienteId: String,
    evaluacionId: String? = null,
    viewModel: EvaluacionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Anamnesis", "Examen Visual", "Refracción", "Contactología", "Cierre")

    var aplicarRecorteOd by remember { mutableStateOf(false) }
    var aplicarRecorteOi by remember { mutableStateOf(false) }

    LaunchedEffect(evaluacionId) {
        if (evaluacionId != null) {
            viewModel.loadEvaluacion(evaluacionId)
        } else {
            // Inicializar con valores por defecto para nuevas evaluaciones
            viewModel.updateUiState { it.copy(
                diagnosticoOd = emptyList(),
                diagnosticoOi = emptyList(),
                otrosPresbicia = false,
                otrosAnisometropia = false,
                otrosAmbliopia = false
            ) }
        }
    }

    LaunchedEffect(uiState.recetaOdEsf, uiState.recetaOdCil, uiState.balanceOd) {
        viewModel.updateDiagnosticAuto()
    }
    LaunchedEffect(uiState.recetaOiEsf, uiState.recetaOiCil, uiState.balanceOi) {
        viewModel.updateDiagnosticAuto()
    }

    LaunchedEffect(
        uiState.addCercaOd, uiState.addCercaOi, 
        uiState.avCcOdLejos, uiState.avCcOiLejos, 
        uiState.isAddAo,
        uiState.recetaOdEsf, uiState.recetaOdCil, uiState.recetaOdEje,
        uiState.recetaOiEsf, uiState.recetaOiCil, uiState.recetaOiEje,
        uiState.balanceOd, uiState.balanceOi
    ) {
        viewModel.updateOtrosAuto()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showProximaDatePicker by remember { mutableStateOf(false) }
    var showLcDatePicker by remember { mutableStateOf(false) }
    var showOsdiDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationHelper = remember { com.example.optoapp.notifications.NotificationHelper(context) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(uiState.fecha),
        yearRange = 1920..2080
    )
    val proximaDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(uiState.proximaCita ?: DateUtils.today()),
        yearRange = 1920..2080
    )
    val lcDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(uiState.lcFechaAdaptacion ?: DateUtils.today()),
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

    if (showProximaDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showProximaDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    proximaDatePickerState.selectedDateMillis?.let { mills ->
                        viewModel.updateUiState { it.copy(proximaCita = DateUtils.pickerMillisToLocalDate(mills)) }
                    }
                    showProximaDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.updateUiState { it.copy(proximaCita = null, citaEstado = "programada") }
                    showProximaDatePicker = false
                }) { Text("Limpiar") }
            }
        ) {
            DatePicker(state = proximaDatePickerState)
        }
    }

    if (showLcDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showLcDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    lcDatePickerState.selectedDateMillis?.let { mills ->
                        viewModel.updateUiState { it.copy(lcFechaAdaptacion = DateUtils.pickerMillisToLocalDate(mills)) }
                    }
                    showLcDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = lcDatePickerState)
        }
    }

    if (showOsdiDialog) {
        OSDIDialog(
            onDismissRequest = { showOsdiDialog = false },
            onSave = { puntuacion, clasificacion ->
                viewModel.updateUiState {
                    it.copy(osdiPuntuacion = puntuacion, osdiClasificacion = clasificacion)
                }
                showOsdiDialog = false
            }
        )
    }

    val saveAction = {
        val sharedPreferences = context.getSharedPreferences("optoapp_prefs", Context.MODE_PRIVATE)
        val programarRecordatorioGlobal = sharedPreferences.getBoolean("pref_enable_reminders", true)

        viewModel.saveEvaluacion(pacienteId, evaluacionId) { savedId, pName ->
            if (programarRecordatorioGlobal && uiState.proximaCita != null) {
                notificationHelper.scheduleWorkManagerReminder(pName, uiState.proximaCita!!, savedId)
            } else {
                notificationHelper.cancelReminder(savedId)
            }
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (evaluacionId == null) "Nueva Evaluación" else "Editar Evaluación") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { saveAction() }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> { // Anamnesis
                        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Fecha Registro: ${DateUtils.formatLocalized(uiState.fecha)}")
                        }
                        OptoTextField(value = uiState.motivoConsulta, onValueChange = { viewModel.updateUiState { s -> s.copy(motivoConsulta = it) } }, label = "Motivo de consulta")
                        OptoTextField(value = uiState.sintomas, onValueChange = { viewModel.updateUiState { s -> s.copy(sintomas = it) } }, label = "Síntomas y signos")
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Antecedentes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        OptoTextField(value = uiState.antecedentesPersonalesOculares, onValueChange = { viewModel.updateUiState { s -> s.copy(antecedentesPersonalesOculares = it) } }, label = "Pers. Oculares")
                        OptoTextField(value = uiState.antecedentesPersonalesSistemicos, onValueChange = { viewModel.updateUiState { s -> s.copy(antecedentesPersonalesSistemicos = it) } }, label = "Pers. Sistémicos")
                        OptoTextField(value = uiState.antecedentesFamiliaresOculares, onValueChange = { viewModel.updateUiState { s -> s.copy(antecedentesFamiliaresOculares = it) } }, label = "Fam. Oculares")
                        OptoTextField(value = uiState.antecedentesFamiliaresSistemicos, onValueChange = { viewModel.updateUiState { s -> s.copy(antecedentesFamiliaresSistemicos = it) } }, label = "Fam. Sistémicos")
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(value = uiState.medicacion, onValueChange = { viewModel.updateUiState { s -> s.copy(medicacion = it) } }, label = "Medicación", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.alergias, onValueChange = { viewModel.updateUiState { s -> s.copy(alergias = it) } }, label = "Alergias", modifier = Modifier.weight(1f))
                        }
                    }
                    1 -> { // Examen Visual
                        Text("Agudeza Visual SIN corrección", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OptoTextField(value = uiState.avScAo, onValueChange = { viewModel.updateUiState { s -> s.copy(avScAo = it) } }, label = "Ambos ojos lejos")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(value = uiState.avScOdLejos, onValueChange = { viewModel.updateUiState { s -> s.copy(avScOdLejos = it) } }, label = "OD lejos", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.avScOiLejos, onValueChange = { viewModel.updateUiState { s -> s.copy(avScOiLejos = it) } }, label = "OI lejos", modifier = Modifier.weight(1f))
                        }
                        OptoTextField(value = uiState.avScAoCerca, onValueChange = { viewModel.updateUiState { s -> s.copy(avScAoCerca = it) } }, label = "Ambos ojos cerca")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(value = uiState.avScOdCerca, onValueChange = { viewModel.updateUiState { s -> s.copy(avScOdCerca = it) } }, label = "OD cerca", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.avScOiCerca, onValueChange = { viewModel.updateUiState { s -> s.copy(avScOiCerca = it) } }, label = "OI cerca", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Agudeza Visual CON corrección PX", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OptoTextField(value = uiState.avCcAoPx, onValueChange = { viewModel.updateUiState { s -> s.copy(avCcAoPx = it) } }, label = "Ambos ojos lejos")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(value = uiState.avCcOdLejos, onValueChange = { viewModel.updateUiState { s -> s.copy(avCcOdLejos = it) } }, label = "OD lejos", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.avCcOiLejos, onValueChange = { viewModel.updateUiState { s -> s.copy(avCcOiLejos = it) } }, label = "OI lejos", modifier = Modifier.weight(1f))
                        }
                        OptoTextField(value = uiState.avCcAoCerca, onValueChange = { viewModel.updateUiState { s -> s.copy(avCcAoCerca = it) } }, label = "Ambos ojos cerca")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(value = uiState.avCcOdCerca, onValueChange = { viewModel.updateUiState { s -> s.copy(avCcOdCerca = it) } }, label = "OD cerca", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.avCcOiCerca, onValueChange = { viewModel.updateUiState { s -> s.copy(avCcOiCerca = it) } }, label = "OI cerca", modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Visión Binocular y Percepción", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                DropdownField(label = "Estereopsis", selected = uiState.estereopsisValor, options = estereopsisOptions, onSelected = { viewModel.updateUiState { s -> s.copy(estereopsisValor = it) } })
                                OptoTextField(value = uiState.estereopsisSegundos, onValueChange = { viewModel.updateUiState { s -> s.copy(estereopsisSegundos = it) } }, label = "Segundos de arco (opcional)")
                                DropdownField(label = "Test de Lang", selected = uiState.lang, options = langOptions, onSelected = { viewModel.updateUiState { s -> s.copy(lang = it) } })
                                DropdownField(label = "Test de Worth", selected = uiState.worth, options = worthOptions, onSelected = { viewModel.updateUiState { s -> s.copy(worth = it) } })
                            }
                        }

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Percepción del Color", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                OptoTextField(value = uiState.ishihara, onValueChange = { viewModel.updateUiState { s -> s.copy(ishihara = it) } }, label = "Test de Ishihara")
                                DropdownField(label = "Test de Farnsworth", selected = uiState.farnsworth, options = farnsworthOptions, onSelected = { viewModel.updateUiState { s -> s.copy(farnsworth = it) } })
                            }
                        }

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Salud de la Superficie Ocular y Función Visual", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Test de Schirmer (mm)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.schirmerOd, onValueChange = { viewModel.updateUiState { s -> s.copy(schirmerOd = it) } }, label = "OD", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.schirmerOi, onValueChange = { viewModel.updateUiState { s -> s.copy(schirmerOi = it) } }, label = "OI", modifier = Modifier.weight(1f))
                                }
                                
                                Text("Test OSDI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { showOsdiDialog = true }, modifier = Modifier.weight(1f)) {
                                        Text("Realizar test OSDI")
                                    }
                                    if (uiState.osdiPuntuacion != null) {
                                        Text("${uiState.osdiPuntuacion} - ${uiState.osdiClasificacion}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                DropdownField(label = "Sensibilidad al contraste", selected = uiState.sensibilidadContraste, options = sensibilidadOptions, onSelected = { viewModel.updateUiState { s -> s.copy(sensibilidadContraste = it) } })
                                OptoTextField(value = uiState.sensibilidadFrecuencia, onValueChange = { viewModel.updateUiState { s -> s.copy(sensibilidadFrecuencia = it) } }, label = "Frecuencia espacial (opcional)")
                            }
                        }

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Otras Pruebas y Exámenes Previos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                OptoTextField(value = uiState.amsler, onValueChange = { viewModel.updateUiState { s -> s.copy(amsler = it) } }, label = "Test de Amsler")
                                DropdownField(label = "Campo visual por confrontación", selected = uiState.campoVisual, options = campoVisualOptions, onSelected = { viewModel.updateUiState { s -> s.copy(campoVisual = it) } })
                                if (uiState.campoVisual == "Anomalía detectada") {
                                    OptoTextField(value = uiState.campoVisualDescripcion, onValueChange = { viewModel.updateUiState { s -> s.copy(campoVisualDescripcion = it) } }, label = "Descripción de anomalía (Campo Visual)")
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Exámenes Previos", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.phOd, onValueChange = { viewModel.updateUiState { s -> s.copy(phOd = it) } }, label = "PH OD", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.phOi, onValueChange = { viewModel.updateUiState { s -> s.copy(phOi = it) } }, label = "PH OI", modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.kappaOd, onValueChange = { viewModel.updateUiState { s -> s.copy(kappaOd = it) } }, label = "Kappa OD", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.kappaOi, onValueChange = { viewModel.updateUiState { s -> s.copy(kappaOi = it) } }, label = "Kappa OI", modifier = Modifier.weight(1f))
                                }
                                OptoTextField(value = uiState.hirshberg, onValueChange = { viewModel.updateUiState { s -> s.copy(hirshberg = it) } }, label = "Hirshberg")
                                
                                Text("Ducciones:", fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.duccionesOd, onValueChange = { viewModel.updateUiState { s -> s.copy(duccionesOd = it) } }, label = "OD", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.duccionesOi, onValueChange = { viewModel.updateUiState { s -> s.copy(duccionesOi = it) } }, label = "OI", modifier = Modifier.weight(1f))
                                }
                                OptoTextField(value = uiState.versionesAo, onValueChange = { viewModel.updateUiState { s -> s.copy(versionesAo = it) } }, label = "Versiones Ambos Ojos")

                                Text("Cover Test", color = MaterialTheme.colorScheme.secondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.coverTest6m, onValueChange = { viewModel.updateUiState { s -> s.copy(coverTest6m = it) } }, label = "6m", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.coverTest40cm, onValueChange = { viewModel.updateUiState { s -> s.copy(coverTest40cm = it) } }, label = "40cm", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.coverTest10cm, onValueChange = { viewModel.updateUiState { s -> s.copy(coverTest10cm = it) } }, label = "10cm", modifier = Modifier.weight(1f))
                                }

                                Text("PPC", color = MaterialTheme.colorScheme.secondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.ppcOr, onValueChange = { viewModel.updateUiState { s -> s.copy(ppcOr = it) } }, label = "OR", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.ppcLuz, onValueChange = { viewModel.updateUiState { s -> s.copy(ppcLuz = it) } }, label = "Luz", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.ppcFrl, onValueChange = { viewModel.updateUiState { s -> s.copy(ppcFrl = it) } }, label = "FR + L", modifier = Modifier.weight(1f))
                                }

                                Text("Reflejos Pupilares", color = MaterialTheme.colorScheme.secondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.reflejoFotomotor, onValueChange = { viewModel.updateUiState { s -> s.copy(reflejoFotomotor = it) } }, label = "Fotomotor", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.reflejoConsensual, onValueChange = { viewModel.updateUiState { s -> s.copy(reflejoConsensual = it) } }, label = "Consensual", modifier = Modifier.weight(1f))
                                }
                                OptoTextField(value = uiState.reflejoAcomodativo, onValueChange = { viewModel.updateUiState { s -> s.copy(reflejoAcomodativo = it) } }, label = "Acomodativo")
                            }
                        }
                    }
                    2 -> { // Refracción
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Refracción Objetiva", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(value = uiState.objOdEsf, onValueChange = { viewModel.updateUiState { s -> s.copy(objOdEsf = it) } }, label = "OD Esf", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.objOdCil, onValueChange = { viewModel.updateUiState { s -> s.copy(objOdCil = it) } }, label = "OD Cil", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.objOdEje, onValueChange = { viewModel.updateUiState { s -> s.copy(objOdEje = it) } }, label = "OD Eje", modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(value = uiState.objOiEsf, onValueChange = { viewModel.updateUiState { s -> s.copy(objOiEsf = it) } }, label = "OI Esf", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.objOiCil, onValueChange = { viewModel.updateUiState { s -> s.copy(objOiCil = it) } }, label = "OI Cil", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.objOiEje, onValueChange = { viewModel.updateUiState { s -> s.copy(objOiEje = it) } }, label = "OI Eje", modifier = Modifier.weight(1f))
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Refracción Subjetiva", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(value = uiState.subjOdEsf, onValueChange = { viewModel.updateUiState { s -> s.copy(subjOdEsf = it) } }, label = "OD Esf", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.subjOdCil, onValueChange = { viewModel.updateUiState { s -> s.copy(subjOdCil = it) } }, label = "OD Cil", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.subjOdEje, onValueChange = { viewModel.updateUiState { s -> s.copy(subjOdEje = it) } }, label = "OD Eje", modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(value = uiState.subjOiEsf, onValueChange = { viewModel.updateUiState { s -> s.copy(subjOiEsf = it) } }, label = "OI Esf", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.subjOiCil, onValueChange = { viewModel.updateUiState { s -> s.copy(subjOiCil = it) } }, label = "OI Cil", modifier = Modifier.weight(1f))
                            OptoTextField(value = uiState.subjOiEje, onValueChange = { viewModel.updateUiState { s -> s.copy(subjOiEje = it) } }, label = "OI Eje", modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Receta Final (Gafas)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(
                                value = uiState.recetaOdEsf, 
                                onValueChange = { viewModel.updateUiState { s -> s.copy(recetaOdEsf = it) } }, 
                                label = "OD Esf", 
                                modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") }
                            )
                            OptoTextField(
                                value = uiState.recetaOdCil, 
                                onValueChange = { viewModel.updateUiState { s -> s.copy(recetaOdCil = it) } }, 
                                label = "OD Cil", 
                                modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") }
                            )
                            OptoTextField(
                                value = uiState.recetaOdEje, 
                                onValueChange = { viewModel.updateUiState { s -> s.copy(recetaOdEje = it) } }, 
                                label = "OD Eje", 
                                modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OD") }
                            )
                            OptoTextField(value = uiState.recetaOdAv, onValueChange = { viewModel.updateUiState { s -> s.copy(recetaOdAv = it) } }, label = "AV", modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptoTextField(
                                value = uiState.recetaOiEsf, 
                                onValueChange = { viewModel.updateUiState { s -> s.copy(recetaOiEsf = it) } }, 
                                label = "OI Esf", 
                                modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OI") }
                            )
                            OptoTextField(
                                value = uiState.recetaOiCil, 
                                onValueChange = { viewModel.updateUiState { s -> s.copy(recetaOiCil = it) } }, 
                                label = "OI Cil", 
                                modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OI") }
                            )
                            OptoTextField(
                                value = uiState.recetaOiEje, 
                                onValueChange = { viewModel.updateUiState { s -> s.copy(recetaOiEje = it) } }, 
                                label = "OI Eje", 
                                modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) viewModel.normalizeAndTranspose("OI") }
                            )
                            OptoTextField(value = uiState.recetaOiAv, onValueChange = { viewModel.updateUiState { s -> s.copy(recetaOiAv = it) } }, label = "AV", modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text("Adición (ADD)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                    Text("A/O", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Switch(
                                        checked = uiState.isAddAo,
                                        onCheckedChange = { newVal -> viewModel.updateUiState { it.copy(isAddAo = newVal) } },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.addCercaOd, onValueChange = { viewModel.updateUiState { s -> s.copy(addCercaOd = it) } }, label = "Add OD", modifier = Modifier.weight(1f))
                                    OptoTextField(
                                        value = uiState.addCercaOi, 
                                        onValueChange = { viewModel.updateUiState { s -> s.copy(addCercaOi = it) } }, 
                                        label = "Add OI", 
                                        modifier = Modifier.weight(1f),
                                        enabled = !uiState.isAddAo
                                    )
                                    OptoTextField(value = uiState.addAv, onValueChange = { viewModel.updateUiState { s -> s.copy(addAv = it) } }, label = "AV Add", modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("DIP (Distancia Interpupilar)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Puedes ingresar DIP total (ej. 62) o DNP OD/OI (ej. 31/31).",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.dipLejos, onValueChange = { viewModel.updateUiState { s -> s.copy(dipLejos = it) } }, label = "DIP Lejos", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.dipCerca, onValueChange = { viewModel.updateUiState { s -> s.copy(dipCerca = it) } }, label = "DIP Cerca", modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Prismas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OptoTextField(value = uiState.prismaOdValor, onValueChange = { viewModel.updateUiState { s -> s.copy(prismaOdValor = it) } }, label = "Prisma OD", modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.weight(1f)) {
                                        DropdownField(label = "Base", selected = uiState.prismaOdBase, options = basesPrisma, onSelected = { viewModel.updateUiState { s -> s.copy(prismaOdBase = it) } })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OptoTextField(value = uiState.prismaOiValor, onValueChange = { viewModel.updateUiState { s -> s.copy(prismaOiValor = it) } }, label = "Prisma OI", modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.weight(1f)) {
                                        DropdownField(label = "Base", selected = uiState.prismaOiBase, options = basesPrisma, onSelected = { viewModel.updateUiState { s -> s.copy(prismaOiBase = it) } })
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // Contactología
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Queratometría", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("OD", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                                    OptoTextField(value = uiState.k1Od, onValueChange = { viewModel.updateUiState { s -> s.copy(k1Od = it) } }, label = "K1", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.k2Od, onValueChange = { viewModel.updateUiState { s -> s.copy(k2Od = it) } }, label = "K2", modifier = Modifier.weight(1f))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("OI", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                                    OptoTextField(value = uiState.k1Oi, onValueChange = { viewModel.updateUiState { s -> s.copy(k1Oi = it) } }, label = "K1", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.k2Oi, onValueChange = { viewModel.updateUiState { s -> s.copy(k2Oi = it) } }, label = "K2", modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Sugerencias y Cálculos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                SuggestionCard(uiState.k1Od, uiState.k2Od, uiState.k1Oi, uiState.k2Oi)
                                
                                CalculationCard("Auto-Cálculo OD", uiState.recetaOdEsf, uiState.recetaOdCil, aplicarRecorteOd, { aplicarRecorteOd = it }, { esf, cil -> 
                                    viewModel.updateUiState { it.copy(lcOdEsf = esf, lcOdCil = cil) }
                                })
                                
                                CalculationCard("Auto-Cálculo OI", uiState.recetaOiEsf, uiState.recetaOiCil, aplicarRecorteOi, { aplicarRecorteOi = it }, { esf, cil -> 
                                    viewModel.updateUiState { it.copy(lcOiEsf = esf, lcOiCil = cil) }
                                })
                            }
                        }

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Prueba / Adaptación Final", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                
                                Text("Poder del Lente", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.lcOdEsf, onValueChange = { viewModel.updateUiState { s -> s.copy(lcOdEsf = it) } }, label = "OD Esf", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.lcOdCil, onValueChange = { viewModel.updateUiState { s -> s.copy(lcOdCil = it) } }, label = "OD Cil", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.lcOdEje, onValueChange = { viewModel.updateUiState { s -> s.copy(lcOdEje = it) } }, label = "OD Eje", modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.lcOiEsf, onValueChange = { viewModel.updateUiState { s -> s.copy(lcOiEsf = it) } }, label = "OI Esf", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.lcOiCil, onValueChange = { viewModel.updateUiState { s -> s.copy(lcOiCil = it) } }, label = "OI Cil", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.lcOiEje, onValueChange = { viewModel.updateUiState { s -> s.copy(lcOiEje = it) } }, label = "OI Eje", modifier = Modifier.weight(1f))
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Parámetros Físicos", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.lcRadioBaseOd, onValueChange = { viewModel.updateUiState { s -> s.copy(lcRadioBaseOd = it) } }, label = "Curva Base (CB) OD", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.lcRadioBaseOi, onValueChange = { viewModel.updateUiState { s -> s.copy(lcRadioBaseOi = it) } }, label = "Curva Base (CB) OI", modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OptoTextField(value = uiState.lcOdDia, onValueChange = { viewModel.updateUiState { s -> s.copy(lcOdDia = it) } }, label = "DIA OD", modifier = Modifier.weight(1f))
                                    OptoTextField(value = uiState.lcOiDia, onValueChange = { viewModel.updateUiState { s -> s.copy(lcOiDia = it) } }, label = "DIA OI", modifier = Modifier.weight(1f))
                                }

                                OptoTextField(value = uiState.lcLaboratorio, onValueChange = { viewModel.updateUiState { s -> s.copy(lcLaboratorio = it) } }, label = "Laboratorio / Marca")
                                DropdownField(label = "Tipo de Lente", selected = uiState.lcTipoLente, options = tiposLC, onSelected = { viewModel.updateUiState { s -> s.copy(lcTipoLente = it) } })
                                DropdownField(label = "Material", selected = uiState.lcMaterial, options = materialesLC, onSelected = { viewModel.updateUiState { s -> s.copy(lcMaterial = it) } })
                                
                                OutlinedButton(onClick = { showLcDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                                    val dText = uiState.lcFechaAdaptacion?.let { DateUtils.formatLocalized(it) } ?: "Seleccionar Fecha"
                                    Text("Fecha Adaptación: $dText")
                                }
                                OptoTextField(value = uiState.lcObservaciones, onValueChange = { viewModel.updateUiState { s -> s.copy(lcObservaciones = it) } }, label = "Notas Contactología")
                            }
                        }
                    }
                    4 -> { // Cierre
                        Text("Diagnóstico y Plan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Diagnóstico", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        DropdownField(
                                            label = "Diagnóstico OD",
                                            selected = uiState.diagnosticoOd.firstOrNull() ?: "",
                                            options = diagnosticosRefraccion,
                                            onSelected = { viewModel.updateUiState { s -> s.copy(diagnosticoOd = listOf(it), balanceOd = it == "Balance") } }
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = uiState.balanceOd, onCheckedChange = { viewModel.updateUiState { s -> s.copy(balanceOd = it) } })
                                        Text("Balance", fontSize = 12.sp)
                                    }
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        DropdownField(
                                            label = "Diagnóstico OI",
                                            selected = uiState.diagnosticoOi.firstOrNull() ?: "",
                                            options = diagnosticosRefraccion,
                                            onSelected = { viewModel.updateUiState { s -> s.copy(diagnosticoOi = listOf(it), balanceOi = it == "Balance") } }
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = uiState.balanceOi, onCheckedChange = { viewModel.updateUiState { s -> s.copy(balanceOi = it) } })
                                        Text("Balance", fontSize = 12.sp)
                                    }
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Otros Diagnósticos", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                
                                // Fila Presbicia
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = uiState.otrosPresbicia, 
                                        onCheckedChange = { val newVal = it; viewModel.updateUiState { s -> s.copy(otrosPresbicia = newVal, autoPresbicia = false) } }
                                    )
                                    Text("Presbicia", modifier = Modifier.weight(1f), fontSize = 14.sp)
                                    TextButton(onClick = { viewModel.updateUiState { s -> s.copy(autoPresbicia = !s.autoPresbicia) } }) {
                                        Text(if (uiState.autoPresbicia) "Auto" else "Man", fontSize = 10.sp, color = if(uiState.autoPresbicia) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                                    }
                                }

                                // Fila Anisometropía
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = uiState.otrosAnisometropia, 
                                        onCheckedChange = { val newVal = it; viewModel.updateUiState { s -> s.copy(otrosAnisometropia = newVal, autoAnisometropia = false) } }
                                    )
                                    Text("Anisometropía", modifier = Modifier.weight(1f), fontSize = 14.sp)
                                    TextButton(onClick = { viewModel.updateUiState { s -> s.copy(autoAnisometropia = !s.autoAnisometropia) } }) {
                                        Text(if (uiState.autoAnisometropia) "Auto" else "Man", fontSize = 10.sp, color = if(uiState.autoAnisometropia) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                                    }
                                }

                                // Fila Ambliopía
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = uiState.otrosAmbliopia, 
                                        onCheckedChange = { val newVal = it; viewModel.updateUiState { s -> s.copy(otrosAmbliopia = newVal, autoAmbliopia = false) } }
                                    )
                                    Text("Ambliopía", modifier = Modifier.weight(1f), fontSize = 14.sp)
                                    TextButton(onClick = { viewModel.updateUiState { s -> s.copy(autoAmbliopia = !s.autoAmbliopia) } }) {
                                        Text(if (uiState.autoAmbliopia) "Auto" else "Man", fontSize = 10.sp, color = if(uiState.autoAmbliopia) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Tratamiento", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                OptoTextField(value = uiState.planTratamiento, onValueChange = { viewModel.updateUiState { s -> s.copy(planTratamiento = it) } }, label = "Plan de Tratamiento")
                                OptoTextField(value = uiState.observaciones, onValueChange = { viewModel.updateUiState { s -> s.copy(observaciones = it) } }, label = "Observaciones Adicionales Generales")
                            }
                        }
                        
                        OutlinedButton(onClick = { showProximaDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            val labelText = if (uiState.proximaCita == null) "Programar Próxima Cita" 
                            else {
                                "Próxima Cita: ${DateUtils.formatLocalized(uiState.proximaCita!!)}"
                            }
                            Text(labelText)
                        }
                        if (uiState.proximaCita != null) {
                            val labelSeleccionado = estadosCitaOpciones
                                .find { it.first == uiState.citaEstado.ifBlank { "programada" } }
                                ?.second
                                ?: estadosCitaOpciones.first().second
                            DropdownField(
                                label = "Estado de la cita",
                                selected = labelSeleccionado,
                                options = estadosCitaOpciones.map { it.second },
                                onSelected = { etiqueta ->
                                    val codigo = estadosCitaOpciones.find { it.second == etiqueta }?.first
                                        ?: "programada"
                                    viewModel.updateUiState { it.copy(citaEstado = codigo) }
                                }
                            )
                        }
                        
                        // Nota: Para integrarse al calendario de Google en el futuro, usar Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { saveAction() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (evaluacionId == null) "Guardar Evaluación" else "Actualizar Evaluación", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
