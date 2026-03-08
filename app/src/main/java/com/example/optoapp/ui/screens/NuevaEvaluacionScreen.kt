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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.optoapp.OptoApplication
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.viewmodel.OptoViewModel
import com.example.optoapp.viewmodel.OptoViewModelFactory
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaEvaluacionScreen(navController: NavController, pacienteId: String, evaluacionId: String? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as OptoApplication
    val viewModel: OptoViewModel = viewModel(
        factory = OptoViewModelFactory(app.repository, app.securityManager)
    )
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Anamnesis", "Examen Visual", "Refracción", "Cierre")

    // Anamnesis
    var motivoConsulta by remember { mutableStateOf("") }
    var sintomas by remember { mutableStateOf("") }
    var antPersOc by remember { mutableStateOf("") }
    var antPersSis by remember { mutableStateOf("") }
    var antFamOc by remember { mutableStateOf("") }
    var antFamSis by remember { mutableStateOf("") }
    var medicacion by remember { mutableStateOf("") }
    var alergias by remember { mutableStateOf("") }
    val necesidadVisual = remember { mutableStateListOf<String>() }

    // Examen Visual
    var avScOdLejos by remember { mutableStateOf("") }
    var avScOiLejos by remember { mutableStateOf("") }
    var avScOdCerca by remember { mutableStateOf("") }
    var avScOiCerca by remember { mutableStateOf("") }
    var avCcOdLejos by remember { mutableStateOf("") }
    var avCcOiLejos by remember { mutableStateOf("") }
    var avCcOdCerca by remember { mutableStateOf("") }
    var avCcOiCerca by remember { mutableStateOf("") }
    var phOd by remember { mutableStateOf("") }
    var phOi by remember { mutableStateOf("") }
    var kappaOd by remember { mutableStateOf("") }
    var kappaOi by remember { mutableStateOf("") }
    var cover6m by remember { mutableStateOf("") }
    var cover40cm by remember { mutableStateOf("") }
    var cover10cm by remember { mutableStateOf("") }
    var ppcOr by remember { mutableStateOf("") }
    var ppcLuz by remember { mutableStateOf("") }
    var ppcFrl by remember { mutableStateOf("") }
    var refFoto by remember { mutableStateOf("") }
    var refCons by remember { mutableStateOf("") }
    var refAcom by remember { mutableStateOf("") }

    // Refracción
    var k1Od by remember { mutableStateOf("") }
    var k2Od by remember { mutableStateOf("") }
    var k1Oi by remember { mutableStateOf("") }
    var k2Oi by remember { mutableStateOf("") }
    var objOdEsf by remember { mutableStateOf("") }
    var objOdCil by remember { mutableStateOf("") }
    var objOdEje by remember { mutableStateOf("") }
    var objOiEsf by remember { mutableStateOf("") }
    var objOiCil by remember { mutableStateOf("") }
    var objOiEje by remember { mutableStateOf("") }
    var subjOdEsf by remember { mutableStateOf("") }
    var subjOdCil by remember { mutableStateOf("") }
    var subjOdEje by remember { mutableStateOf("") }
    var subjOiEsf by remember { mutableStateOf("") }
    var subjOiCil by remember { mutableStateOf("") }
    var subjOiEje by remember { mutableStateOf("") }
    var recOdEsf by remember { mutableStateOf("") }
    var recOdCil by remember { mutableStateOf("") }
    var recOdEje by remember { mutableStateOf("") }
    var recOiEsf by remember { mutableStateOf("") }
    var recOiCil by remember { mutableStateOf("") }
    var recOiEje by remember { mutableStateOf("") }
    var addCercaOd by remember { mutableStateOf("") }
    var addCercaOi by remember { mutableStateOf("") }
    var addInterOd by remember { mutableStateOf("") }
    var addInterOi by remember { mutableStateOf("") }
    var dipLejos by remember { mutableStateOf("") }
    var dipCerca by remember { mutableStateOf("") }
    var dipInter by remember { mutableStateOf("") }
    var prismOdVal by remember { mutableStateOf("") }
    var prismOdBase by remember { mutableStateOf("") }
    var prismOiVal by remember { mutableStateOf("") }
    var prismOiBase by remember { mutableStateOf("") }

    // Cierre
    var diagnostico by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    var proximaCita by remember { mutableStateOf("") }

    val basesPrisma = listOf("Nasal", "Temporal", "Superior", "Inferior")
    val diagnosticosPredef = listOf("Miopia", "Hipermetropia", "Astigmatismo miopico", "Astigmatismo hipermetropico", "Astigmatismo mixto", "Presbicia")

    LaunchedEffect(evaluacionId) {
        if (evaluacionId != null) {
            viewModel.getEvaluacion(evaluacionId)?.let { eval ->
                motivoConsulta = eval.motivoConsulta
                sintomas = eval.sintomas
                antPersOc = eval.antecedentesPersonalesOculares
                antPersSis = eval.antecedentesPersonalesSistemicos
                antFamOc = eval.antecedentesFamiliaresOculares
                antFamSis = eval.antecedentesFamiliaresSistemicos
                medicacion = eval.medicacion
                alergias = eval.alergias
                necesidadVisual.clear()
                necesidadVisual.addAll(eval.necesidadVisual)
                avScOdLejos = eval.avScOdLejos
                avScOiLejos = eval.avScOiLejos
                avScOdCerca = eval.avScOdCerca
                avScOiCerca = eval.avScOiCerca
                avCcOdLejos = eval.avCcOdLejos
                avCcOiLejos = eval.avCcOiLejos
                avCcOdCerca = eval.avCcOdCerca
                avCcOiCerca = eval.avCcOiCerca
                phOd = eval.phOd
                phOi = eval.phOi
                kappaOd = eval.kappaOd
                kappaOi = eval.kappaOi
                cover6m = eval.coverTest6m
                cover40cm = eval.coverTest40cm
                cover10cm = eval.coverTest10cm
                ppcOr = eval.ppcOr
                ppcLuz = eval.ppcLuz
                ppcFrl = eval.ppcFrl
                k1Od = eval.k1Od
                k2Od = eval.k2Od
                k1Oi = eval.k1Oi
                k2Oi = eval.k2Oi
                refFoto = eval.reflejoFotomotor
                refCons = eval.reflejoConsensual
                refAcom = eval.reflejoAcomodativo
                objOdEsf = eval.objOdEsf
                objOdCil = eval.objOdCil
                objOdEje = eval.objOdEje
                objOiEsf = eval.objOiEsf
                objOiCil = eval.objOiCil
                objOiEje = eval.objOiEje
                subjOdEsf = eval.subjOdEsf
                subjOdCil = eval.subjOdCil
                subjOdEje = eval.subjOdEje
                subjOiEsf = eval.subjOiEsf
                subjOiCil = eval.subjOiCil
                subjOiEje = eval.subjOiEje
                recOdEsf = eval.recetaOdEsf
                recOdCil = eval.recetaOdCil
                recOdEje = eval.recetaOdEje
                recOiEsf = eval.recetaOiEsf
                recOiCil = eval.recetaOiCil
                recOiEje = eval.recetaOiEje
                addCercaOd = eval.addCercaOd
                addCercaOi = eval.addCercaOi
                addInterOd = eval.addIntermediaOd
                addInterOi = eval.addIntermediaOi
                dipLejos = eval.dipLejos
                dipCerca = eval.dipCerca
                dipInter = eval.dipIntermedio
                prismOdVal = eval.prismaOdValor
                prismOdBase = eval.prismaOdBase
                prismOiVal = eval.prismaOiValor
                prismOiBase = eval.prismaOiBase
                diagnostico = eval.diagnostico
                plan = eval.planTratamiento
                observaciones = eval.observaciones
                proximaCita = eval.proximaFechaControl
            }
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
                    IconButton(onClick = {
                        val eval = EvaluacionClinica(
                            id = evaluacionId ?: UUID.randomUUID().toString(),
                            pacienteId = pacienteId,
                            fecha = System.currentTimeMillis(),
                            motivoConsulta = motivoConsulta,
                            sintomas = sintomas,
                            antecedentesPersonalesOculares = antPersOc,
                            antecedentesPersonalesSistemicos = antPersSis,
                            antecedentesFamiliaresOculares = antFamOc,
                            antecedentesFamiliaresSistemicos = antFamSis,
                            medicacion = medicacion,
                            alergias = alergias,
                            necesidadVisual = necesidadVisual.toList(),
                            avScOdLejos = avScOdLejos, avScOiLejos = avScOiLejos,
                            avScOdCerca = avScOdCerca, avScOiCerca = avScOiCerca,
                            avCcOdLejos = avCcOdLejos, avCcOiLejos = avCcOiLejos,
                            avCcOdCerca = avCcOdCerca, avCcOiCerca = avCcOiCerca,
                            phOd = phOd, phOi = phOi, kappaOd = kappaOd, kappaOi = kappaOi,
                            coverTest6m = cover6m, coverTest40cm = cover40cm, coverTest10cm = cover10cm,
                            ppcOr = ppcOr, ppcLuz = ppcLuz, ppcFrl = ppcFrl,
                            reflejoFotomotor = refFoto, reflejoConsensual = refCons, reflejoAcomodativo = refAcom,
                            k1Od = k1Od, k2Od = k2Od, k1Oi = k1Oi, k2Oi = k2Oi,
                            objOdEsf = objOdEsf, objOdCil = objOdCil, objOdEje = objOdEje,
                            objOiEsf = objOiEsf, objOiCil = objOiCil, objOiEje = objOiEje,
                            subjOdEsf = subjOdEsf, subjOdCil = subjOdCil, subjOdEje = subjOdEje,
                            subjOiEsf = subjOiEsf, subjOiCil = subjOiCil, subjOiEje = subjOiEje,
                            recetaOdEsf = recOdEsf, recetaOdCil = recOdCil, recetaOdEje = recOdEje,
                            recetaOiEsf = recOiEsf, recetaOiCil = recOiCil, recetaOiEje = recOiEje,
                            addCercaOd = addCercaOd, addCercaOi = addCercaOi,
                            addIntermediaOd = addInterOd, addIntermediaOi = addInterOi,
                            dipLejos = dipLejos, dipCerca = dipCerca, dipIntermedio = dipInter,
                            prismaOdValor = prismOdVal, prismaOdBase = prismOdBase,
                            prismaOiValor = prismOiVal, prismaOiBase = prismOiBase,
                            diagnostico = diagnostico,
                            planTratamiento = plan,
                            observaciones = observaciones,
                            proximaFechaControl = proximaCita
                        )
                        scope.launch {
                            viewModel.saveEvaluacion(eval)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp) }
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
                        OutlinedTextField(value = motivoConsulta, onValueChange = { motivoConsulta = it }, label = { Text("Motivo de consulta") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sintomas, onValueChange = { sintomas = it }, label = { Text("Síntomas y signos") }, modifier = Modifier.fillMaxWidth())
                        Text("Antecedentes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(value = antPersOc, onValueChange = { antPersOc = it }, label = { Text("Pers. Oculares") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = antPersSis, onValueChange = { antPersSis = it }, label = { Text("Pers. Sistémicos") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = antFamOc, onValueChange = { antFamOc = it }, label = { Text("Fam. Oculares") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = antFamSis, onValueChange = { antFamSis = it }, label = { Text("Fam. Sistémicos") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = medicacion, onValueChange = { medicacion = it }, label = { Text("Medicación") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = alergias, onValueChange = { alergias = it }, label = { Text("Alergias") }, modifier = Modifier.fillMaxWidth())
                    }
                    1 -> { // Examen Visual
                        Text("Agudeza Visual SIN corrección", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = avScOdLejos, onValueChange = { avScOdLejos = it }, label = { Text("OD lejos") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = avScOiLejos, onValueChange = { avScOiLejos = it }, label = { Text("OI lejos") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = avScOdCerca, onValueChange = { avScOdCerca = it }, label = { Text("OD cerca") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = avScOiCerca, onValueChange = { avScOiCerca = it }, label = { Text("OI cerca") }, modifier = Modifier.weight(1f))
                        }

                        Text("Agudeza Visual CON corrección", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = avCcOdLejos, onValueChange = { avCcOdLejos = it }, label = { Text("OD lejos") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = avCcOiLejos, onValueChange = { avCcOiLejos = it }, label = { Text("OI lejos") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = avCcOdCerca, onValueChange = { avCcOdCerca = it }, label = { Text("OD cerca") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = avCcOiCerca, onValueChange = { avCcOiCerca = it }, label = { Text("OI cerca") }, modifier = Modifier.weight(1f))
                        }

                        Text("Otros Exámenes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = phOd, onValueChange = { phOd = it }, label = { Text("PH OD") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = phOi, onValueChange = { phOi = it }, label = { Text("PH OI") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = kappaOd, onValueChange = { kappaOd = it }, label = { Text("Kappa OD") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = kappaOi, onValueChange = { kappaOi = it }, label = { Text("Kappa OI") }, modifier = Modifier.weight(1f))
                        }

                        Text("Cover Test", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = cover6m, onValueChange = { cover6m = it }, label = { Text("6m") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = cover40cm, onValueChange = { cover40cm = it }, label = { Text("40cm") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = cover10cm, onValueChange = { cover10cm = it }, label = { Text("10cm") }, modifier = Modifier.weight(1f))
                        }

                        Text("PPC", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = ppcOr, onValueChange = { ppcOr = it }, label = { Text("OR") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = ppcLuz, onValueChange = { ppcLuz = it }, label = { Text("Luz") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = ppcFrl, onValueChange = { ppcFrl = it }, label = { Text("FR + L") }, modifier = Modifier.weight(1f))
                        }

                        Text("Reflejos Pupilares", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(value = refFoto, onValueChange = { refFoto = it }, label = { Text("Fotomotor") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = refCons, onValueChange = { refCons = it }, label = { Text("Consensual") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = refAcom, onValueChange = { refAcom = it }, label = { Text("Acomodativo") }, modifier = Modifier.fillMaxWidth())
                    }
                    2 -> { // Refracción
                        Text("Queratometria", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("OD", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = k1Od, onValueChange = { k1Od = it }, label = { Text("k1") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                            OutlinedTextField(value = k2Od, onValueChange = { k2Od = it }, label = { Text("k2") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                        Text("OI", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = k1Oi, onValueChange = { k1Oi = it }, label = { Text("k1") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                            OutlinedTextField(value = k2Oi, onValueChange = { k2Oi = it }, label = { Text("k2") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Refracción Objetiva", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("OD", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = objOdEsf, onValueChange = { objOdEsf = it }, label = { Text("Esf") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = objOdCil, onValueChange = { objOdCil = it }, label = { Text("Cil") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = objOdEje, onValueChange = { objOdEje = it }, label = { Text("Eje (°)") }, modifier = Modifier.weight(1f))
                        }
                        Text("OI", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = objOiEsf, onValueChange = { objOiEsf = it }, label = { Text("Esf") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = objOiCil, onValueChange = { objOiCil = it }, label = { Text("Cil") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = objOiEje, onValueChange = { objOiEje = it }, label = { Text("Eje (°)") }, modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Refracción Subjetiva", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("OD", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = subjOdEsf, onValueChange = { subjOdEsf = it }, label = { Text("Esf") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = subjOdCil, onValueChange = { subjOdCil = it }, label = { Text("Cil") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = subjOdEje, onValueChange = { subjOdEje = it }, label = { Text("Eje (°)") }, modifier = Modifier.weight(1f))
                        }
                        Text("OI", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = subjOiEsf, onValueChange = { subjOiEsf = it }, label = { Text("Esf") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = subjOiCil, onValueChange = { subjOiCil = it }, label = { Text("Cil") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = subjOiEje, onValueChange = { subjOiEje = it }, label = { Text("Eje (°)") }, modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Refracción Final (Receta) Lejos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("OD", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = recOdEsf, onValueChange = { recOdEsf = it }, label = { Text("Esf") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = recOdCil, onValueChange = { recOdCil = it }, label = { Text("Cil") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = recOdEje, onValueChange = { recOdEje = it }, label = { Text("Eje (°)") }, modifier = Modifier.weight(1f))
                        }
                        Text("OI", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = recOiEsf, onValueChange = { recOiEsf = it }, label = { Text("Esf") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = recOiCil, onValueChange = { recOiCil = it }, label = { Text("Cil") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = recOiEje, onValueChange = { recOiEje = it }, label = { Text("Eje (°)") }, modifier = Modifier.weight(1f))
                        }

                        Text("Adición (ADD)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = addCercaOd, onValueChange = { addCercaOd = it }, label = { Text("Cerca OD") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = addCercaOi, onValueChange = { addCercaOi = it }, label = { Text("Cerca OI") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = addInterOd, onValueChange = { addInterOd = it }, label = { Text("Intermedia OD") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = addInterOi, onValueChange = { addInterOi = it }, label = { Text("Intermedia OI") }, modifier = Modifier.weight(1f))
                        }

                        Text("DIP o DNP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = dipLejos, onValueChange = { dipLejos = it }, label = { Text("Lejos") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = dipCerca, onValueChange = { dipCerca = it }, label = { Text("Cerca") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = dipInter, onValueChange = { dipInter = it }, label = { Text("Intermedio") }, modifier = Modifier.weight(1f))
                        }

                        Text("Prismas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("OD", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = prismOdVal, onValueChange = { prismOdVal = it }, label = { Text("Valor") }, modifier = Modifier.weight(1f))
                            var expandedOd by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(onClick = { expandedOd = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(prismOdBase.ifEmpty { "Base" })
                                }
                                DropdownMenu(expanded = expandedOd, onDismissRequest = { expandedOd = false }) {
                                    basesPrisma.forEach { base ->
                                        DropdownMenuItem(text = { Text(base) }, onClick = { prismOdBase = base; expandedOd = false })
                                    }
                                }
                            }
                        }
                        Text("OI", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = prismOiVal, onValueChange = { prismOiVal = it }, label = { Text("Valor") }, modifier = Modifier.weight(1f))
                            var expandedOi by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(onClick = { expandedOi = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(prismOiBase.ifEmpty { "Base" })
                                }
                                DropdownMenu(expanded = expandedOi, onDismissRequest = { expandedOi = false }) {
                                    basesPrisma.forEach { base ->
                                        DropdownMenuItem(text = { Text(base) }, onClick = { prismOiBase = base; expandedOi = false })
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // Cierre
                        Text("Cierre", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        var expandedDiag by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = diagnostico,
                                onValueChange = { diagnostico = it },
                                label = { Text("Diagnóstico") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { expandedDiag = true }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.rotate(-90f))
                                    }
                                }
                            )
                            DropdownMenu(expanded = expandedDiag, onDismissRequest = { expandedDiag = false }) {
                                diagnosticosPredef.forEach { diag ->
                                    DropdownMenuItem(text = { Text(diag) }, onClick = { diagnostico = diag; expandedDiag = false })
                                }
                            }
                        }
                        
                        OutlinedTextField(value = plan, onValueChange = { plan = it }, label = { Text("Plan de Tratamiento") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        OutlinedTextField(value = observaciones, onValueChange = { observaciones = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        OutlinedTextField(value = proximaCita, onValueChange = { proximaCita = it }, label = { Text("Próxima Cita (dd/mm/aaaa)") }, modifier = Modifier.fillMaxWidth())
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val eval = EvaluacionClinica(
                                    id = evaluacionId ?: UUID.randomUUID().toString(),
                                    pacienteId = pacienteId,
                                    fecha = System.currentTimeMillis(),
                                    motivoConsulta = motivoConsulta,
                                    sintomas = sintomas,
                                    antecedentesPersonalesOculares = antPersOc,
                                    antecedentesPersonalesSistemicos = antPersSis,
                                    antecedentesFamiliaresOculares = antFamOc,
                                    antecedentesFamiliaresSistemicos = antFamSis,
                                    medicacion = medicacion,
                                    alergias = alergias,
                                    necesidadVisual = necesidadVisual.toList(),
                                    avScOdLejos = avScOdLejos, avScOiLejos = avScOiLejos,
                                    avScOdCerca = avScOdCerca, avScOiCerca = avScOiCerca,
                                    avCcOdLejos = avCcOdLejos, avCcOiLejos = avCcOiLejos,
                                    avCcOdCerca = avCcOdCerca, avCcOiCerca = avCcOiCerca,
                                    phOd = phOd, phOi = phOi, kappaOd = kappaOd, kappaOi = kappaOi,
                                    coverTest6m = cover6m, coverTest40cm = cover40cm, coverTest10cm = cover10cm,
                                    ppcOr = ppcOr, ppcLuz = ppcLuz, ppcFrl = ppcFrl,
                                    reflejoFotomotor = refFoto, reflejoConsensual = refCons, reflejoAcomodativo = refAcom,
                                    k1Od = k1Od, k2Od = k2Od, k1Oi = k1Oi, k2Oi = k2Oi,
                                    objOdEsf = objOdEsf, objOdCil = objOdCil, objOdEje = objOdEje,
                                    objOiEsf = objOiEsf, objOiCil = objOiCil, objOiEje = objOiEje,
                                    subjOdEsf = subjOdEsf, subjOdCil = subjOdCil, subjOdEje = subjOdEje,
                                    subjOiEsf = subjOiEsf, subjOiCil = subjOiCil, subjOiEje = subjOiEje,
                                    recetaOdEsf = recOdEsf, recetaOdCil = recOdCil, recetaOdEje = recOdEje,
                                    recetaOiEsf = recOiEsf, recetaOiCil = recOiCil, recetaOiEje = recOiEje,
                                    addCercaOd = addCercaOd, addCercaOi = addCercaOi,
                                    addIntermediaOd = addInterOd, addIntermediaOi = addInterOi,
                                    dipLejos = dipLejos, dipCerca = dipCerca, dipIntermedio = dipInter,
                                    prismaOdValor = prismOdVal, prismaOdBase = prismOdBase,
                                    prismaOiValor = prismOiVal, prismaOiBase = prismOiBase,
                                    diagnostico = diagnostico,
                                    planTratamiento = plan,
                                    observaciones = observaciones,
                                    proximaFechaControl = proximaCita
                                )
                                scope.launch {
                                    viewModel.saveEvaluacion(eval)
                                    navController.popBackStack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (evaluacionId == null) "Guardar Evaluación" else "Actualizar Evaluación")
                        }
                    }
                }
            }
        }
    }
}
