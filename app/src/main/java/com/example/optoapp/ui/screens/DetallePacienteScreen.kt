package com.example.optoapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.optoapp.OptoApplication
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.viewmodel.PacienteViewModel
import com.example.optoapp.viewmodel.EvaluacionViewModel
import com.example.optoapp.viewmodel.DispensacionViewModel
import com.example.optoapp.viewmodel.ServiciosViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePacienteScreen(
    navController: NavController, 
    id: String, 
    pacienteViewModel: PacienteViewModel = hiltViewModel(),
    evaluacionViewModel: EvaluacionViewModel = hiltViewModel(),
    dispensacionViewModel: DispensacionViewModel = hiltViewModel(),
    serviciosViewModel: ServiciosViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var paciente by remember { mutableStateOf<Paciente?>(null) }
    val evaluaciones by evaluacionViewModel.getEvaluacionesByPaciente(id).collectAsState(initial = emptyList())
    val dispensaciones by dispensacionViewModel.getDispensacionesByPaciente(id).collectAsState(initial = emptyList())
    val servicios by serviciosViewModel.allServicios.map { list -> list.filter { it.pacienteId == id } }.collectAsState(initial = emptyList())
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Evaluaciones", "Dispensaciones", "Servicios")

    LaunchedEffect(id) {
        paciente = pacienteViewModel.getPaciente(id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ficha del Paciente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { /* PDF Export */ }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF")
                    }
                    IconButton(onClick = { navController.navigate("editarPaciente/${id}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Perfil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        paciente?.let { p ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header Card - Resumen del Paciente
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(30.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = p.nombreCompleto, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Tel: ${p.telefono}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "ID: ${p.id.take(8)}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> EvaluacionesList(evaluaciones, p, evaluacionViewModel) { evalId ->
                            navController.navigate("editarEvaluacion/${id}/${evalId}")
                        }
                        1 -> DispensacionesList(dispensaciones, p) { dispId ->
                            navController.navigate("editarDispensacion/${id}/${dispId}")
                        }
                        2 -> ServiciosExtraList(servicios) { servId ->
                            navController.navigate("editar_servicio/${servId}")
                        }
                    }
                    
                    FloatingActionButton(
                        onClick = {
                            when(selectedTab) {
                                0 -> navController.navigate("nuevaEvaluacion/${id}")
                                1 -> navController.navigate("nuevaDispensacion/${id}")
                                2 -> navController.navigate("nuevo_servicio/${id}")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir")
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun EvaluacionesList(
    evaluaciones: List<EvaluacionClinica>, 
    paciente: Paciente, 
    evaluacionViewModel: com.example.optoapp.viewmodel.EvaluacionViewModel,
    onEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedEvalForResumen = remember { mutableStateOf<EvaluacionClinica?>(null) }

    if (evaluaciones.isEmpty()) {
        EmptyListMessage("No hay evaluaciones registradas.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(evaluaciones) { eval ->
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(eval.fecha))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = date, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Row {
                                IconButton(onClick = { selectedEvalForResumen.value = eval }) { 
                                    Icon(Icons.Default.Visibility, contentDescription = "Ver Resumen", tint = MaterialTheme.colorScheme.secondary) 
                                }
                                IconButton(onClick = { onEdit(eval.id) }) { 
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) 
                                }
                                var showDeleteDialog by remember { mutableStateOf(false) }
                                IconButton(onClick = { showDeleteDialog = true }) { 
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red) 
                                }
                                
                                if (showDeleteDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteDialog = false },
                                        title = { Text("¿Eliminar evaluación?") },
                                        text = { Text("Esta acción no se puede deshacer.") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    val notificationHelper = com.example.optoapp.notifications.NotificationHelper(context)
                                                    notificationHelper.cancelReminder(eval.id)
                                                    evaluacionViewModel.deleteEvaluacion(eval.id) {
                                                        showDeleteDialog = false
                                                    }
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                            ) {
                                                Text("Eliminar")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteDialog = false }) {
                                                Text("Cancelar")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        val recetaStr = buildString {
                            val hasOd = eval.recetaOdEsf.isNotBlank() || eval.recetaOdCil.isNotBlank()
                            val hasOi = eval.recetaOiEsf.isNotBlank() || eval.recetaOiCil.isNotBlank()
                            if (hasOd) {
                                append("OD: ${eval.recetaOdEsf}/${eval.recetaOdCil}x${eval.recetaOdEje}°")
                                if (eval.recetaOdAv.isNotBlank()) append(" (${eval.recetaOdAv}) ") else append(" ")
                            }
                            if (hasOi) {
                                append("OI: ${eval.recetaOiEsf}/${eval.recetaOiCil}x${eval.recetaOiEje}°")
                                if (eval.recetaOiAv.isNotBlank()) append(" (${eval.recetaOiAv})")
                            }
                        }
                        val diagStr = buildString {
                            val dOd = eval.diagnosticoOd.firstOrNull() ?: ""
                            val dOi = eval.diagnosticoOi.firstOrNull() ?: ""
                            if (dOd.isNotBlank()) append("OD: $dOd ")
                            if (dOi.isNotBlank()) append("OI: $dOi")
                        }.trim()

                        if (recetaStr.isNotBlank()) {
                            Text(text = "Receta $recetaStr", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (diagStr.isNotBlank()) {
                                Text(text = "Diag: $diagStr", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else if (diagStr.isNotBlank()) {
                            Text("Diagnóstico: $diagStr", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            Text("Sin receta ni diagnóstico", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
    
    selectedEvalForResumen.value?.let { currentEval ->
        ResumenEvaluacionDialog(
            eval = currentEval,
            paciente = paciente,
            onDismiss = { selectedEvalForResumen.value = null },
            onEdit = { onEdit(currentEval.id) }
        )
    }
}

@Composable
fun ResumenEvaluacionDialog(eval: EvaluacionClinica, paciente: Paciente, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(eval.fecha))
    val proxima = eval.proximaCita?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)) } ?: "No programada"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resumen Clínico") 
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoSection("Datos del Paciente") {
                    Text("Nombre: ${paciente.nombreCompleto}", fontSize = 14.sp)
                    Text("Edad: ${paciente.edad} años", fontSize = 14.sp)
                    if (paciente.telefono.isNotBlank()) Text("Teléfono: ${paciente.telefono}", fontSize = 14.sp)
                    Text("Fecha de Eval: $date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                val diagOd = eval.diagnosticoOd.firstOrNull() ?: ""
                val diagOi = eval.diagnosticoOi.firstOrNull() ?: ""
                
                if (diagOd.isNotBlank() || diagOi.isNotBlank() || eval.diagnostico.isNotBlank()) {
                    InfoSection("Diagnóstico") {
                        if (eval.diagnostico.isNotBlank()) Text(eval.diagnostico, fontSize = 14.sp)
                        if (diagOd.isNotBlank()) Text("OD: $diagOd", fontSize = 14.sp)
                        if (diagOi.isNotBlank()) Text("OI: $diagOi", fontSize = 14.sp)
                    }
                }

                val diagOtrosList = mutableListOf<String>()
                if (eval.otrosPresbicia) diagOtrosList.add("Presbicia")
                if (eval.otrosAnisometropia) diagOtrosList.add("Anisometropía")
                if (eval.otrosAmbliopia) diagOtrosList.add("Ambliopía")

                if (diagOtrosList.isNotEmpty()) {
                    InfoSection("Condiciones Asociadas") {
                        diagOtrosList.forEach { cond ->
                            Text(cond, fontSize = 14.sp)
                        }
                    }
                }

                val showOd = eval.recetaOdEsf.isNotBlank() || eval.recetaOdCil.isNotBlank()
                val showOi = eval.recetaOiEsf.isNotBlank() || eval.recetaOiCil.isNotBlank()
                if (showOd || showOi) {
                    InfoSection("Refracción Final (Gafas)") {
                        if (showOd) {
                            val avOd = if (eval.recetaOdAv.isNotBlank()) " AV: ${eval.recetaOdAv}" else ""
                            Text("OD: ${eval.recetaOdEsf} / ${eval.recetaOdCil} x ${eval.recetaOdEje}°$avOd", fontSize = 14.sp)
                        }
                        if (showOi) {
                            val avOi = if (eval.recetaOiAv.isNotBlank()) " AV: ${eval.recetaOiAv}" else ""
                            Text("OI: ${eval.recetaOiEsf} / ${eval.recetaOiCil} x ${eval.recetaOiEje}°$avOi", fontSize = 14.sp)
                        }
                    }
                }

                val hasAdd = eval.addCercaOd.isNotBlank() || eval.addCercaOi.isNotBlank() || eval.addAv.isNotBlank()
                if (hasAdd) {
                    InfoSection("Adición (ADD)") {
                        if (eval.addCercaOd.isNotBlank()) Text("OD: ${eval.addCercaOd}", fontSize = 14.sp)
                        if (eval.addCercaOi.isNotBlank()) Text("OI: ${eval.addCercaOi}", fontSize = 14.sp)
                        if (eval.addAv.isNotBlank()) Text("AV: ${eval.addAv}", fontSize = 14.sp)
                    }
                }
                
                val hasDip = eval.dipLejos.isNotBlank() || eval.dipIntermedio.isNotBlank() || eval.dipCerca.isNotBlank()
                if (hasDip) {
                    InfoSection("DIP (Distancia Interpupilar)") {
                        if (eval.dipLejos.isNotBlank()) Text("Lejos: ${eval.dipLejos}", fontSize = 14.sp)
                        if (eval.dipIntermedio.isNotBlank()) Text("Intermedio: ${eval.dipIntermedio}", fontSize = 14.sp)
                        if (eval.dipCerca.isNotBlank()) Text("Cerca: ${eval.dipCerca}", fontSize = 14.sp)
                    }
                }
                
                val hasPrisma = eval.prismaOdValor.isNotBlank() || eval.prismaOiValor.isNotBlank()
                if (hasPrisma) {
                    InfoSection("Prismas") {
                        if (eval.prismaOdValor.isNotBlank()) Text("OD: ${eval.prismaOdValor} (Base: ${eval.prismaOdBase})", fontSize = 14.sp)
                        if (eval.prismaOiValor.isNotBlank()) Text("OI: ${eval.prismaOiValor} (Base: ${eval.prismaOiBase})", fontSize = 14.sp)
                    }
                }
                
                val keratoOd = eval.k1Od.isNotBlank() || eval.k2Od.isNotBlank()
                val keratoOi = eval.k1Oi.isNotBlank() || eval.k2Oi.isNotBlank()
                if (keratoOd || keratoOi) {
                    InfoSection("Queratometría") {
                        if (keratoOd) Text("OD: ${eval.k1Od} / ${eval.k2Od}", fontSize = 14.sp)
                        if (keratoOi) Text("OI: ${eval.k1Oi} / ${eval.k2Oi}", fontSize = 14.sp)
                    }
                }
                
                val tieneLC = eval.lcOdEsf.isNotBlank() || eval.lcOiEsf.isNotBlank()
                if (tieneLC) {
                    InfoSection("Contactología") {
                        Text("Contiene datos de adaptación", color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp)
                    }
                }

                if (eval.proximaCita != null) {
                    InfoSection("Próxima Cita") {
                        Text(proxima, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = { 
            Button(onClick = { onDismiss(); onEdit() }) { 
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Completo") 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Cerrar") } 
        }
    )
}

@Composable
fun InfoSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}

@Composable
fun DispensacionesList(dispensaciones: List<DispensacionOptica>, paciente: Paciente, onEdit: (String) -> Unit) {
    val selectedDispForResumen = remember { mutableStateOf<DispensacionOptica?>(null) }

    if (dispensaciones.isEmpty()) {
        EmptyListMessage("No hay dispensaciones.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(dispensaciones) { disp ->
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(disp.fecha))
                val saldo = disp.montoTotal - disp.montoPagado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    onClick = { onEdit(disp.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = date, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(modifier = Modifier.size(24.dp), onClick = { selectedDispForResumen.value = disp }) { 
                                    Icon(Icons.Default.Visibility, contentDescription = "Ver Resumen", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }
                                Surface(
                                    color = if (disp.estadoEntrega == "Entregado") MaterialTheme.colorScheme.tertiary else Color(0xFFFF9800),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(disp.estadoEntrega, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${disp.tipoLente} - ${disp.materialLente}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(text = "Saldo:", fontSize = 12.sp, color = Color.Gray)
                            val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                            Text(text = "s/. $formattedSaldo", color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
    
    selectedDispForResumen.value?.let { currentDisp ->
        ResumenDispensacionDialog(
            disp = currentDisp,
            paciente = paciente,
            onDismiss = { selectedDispForResumen.value = null },
            onEdit = { onEdit(currentDisp.id) }
        )
    }
}

@Composable
fun ResumenDispensacionDialog(disp: DispensacionOptica, paciente: Paciente, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(disp.fecha))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resumen de Dispensación") 
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoSection("Datos del Paciente") {
                    Text("Nombre: ${paciente.nombreCompleto}", fontSize = 14.sp)
                    Text("Edad: ${paciente.edad} años", fontSize = 14.sp)
                    Text("Fecha: $date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                if (disp.origenMontura.isNotBlank() || disp.tipoAro.isNotBlank() || disp.materialMontura.isNotBlank() || disp.descripcionMontura.isNotBlank()) {
                    InfoSection("Información de Montura") {
                        if (disp.origenMontura.isNotBlank()) Text("Origen: ${disp.origenMontura}", fontSize = 14.sp)
                        if (disp.tipoAro.isNotBlank()) Text("Tipo de Aro: ${disp.tipoAro}", fontSize = 14.sp)
                        if (disp.materialMontura.isNotBlank()) Text("Material: ${disp.materialMontura}", fontSize = 14.sp)
                        if (disp.descripcionMontura.isNotBlank()) Text("Descripción: ${disp.descripcionMontura}", fontSize = 14.sp)
                    }
                }

                if (disp.tipoLente.isNotBlank() || disp.materialLente.isNotBlank() || disp.colorLente.isNotBlank() || disp.tratamientos.isNotEmpty() || disp.notasDiseno.isNotBlank()) {
                    InfoSection("Información del Lente") {
                        if (disp.tipoLente.isNotBlank()) Text("Tipo: ${disp.tipoLente}", fontSize = 14.sp)
                        if (disp.materialLente.isNotBlank()) Text("Material: ${disp.materialLente}", fontSize = 14.sp)
                        if (disp.colorLente.isNotBlank()) Text("Color: ${disp.colorLente}", fontSize = 14.sp)
                        if (disp.tratamientos.isNotEmpty()) {
                            Text("Tratamientos: ${disp.tratamientos.joinToString(", ")}", fontSize = 14.sp)
                        }
                        if (disp.notasDiseno.isNotBlank()) Text("Notas: ${disp.notasDiseno}", fontSize = 14.sp)
                    }
                }

                InfoSection("Resumen Financiero") {
                    val saldo = disp.montoTotal - disp.montoPagado
                    val formattedTotal = String.format(Locale.getDefault(), "%.2f", disp.montoTotal)
                    val formattedPagado = String.format(Locale.getDefault(), "%.2f", disp.montoPagado)
                    val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                    
                    Text("Monto Total: s/. $formattedTotal", fontSize = 14.sp)
                    if (disp.metodoPago.isNotBlank()) Text("Método de Pago: ${disp.metodoPago}", fontSize = 14.sp)
                    Text("A Cuenta: s/. $formattedPagado", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Saldo Restante: s/. $formattedSaldo", 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (saldo > 0) Color.Red else MaterialTheme.colorScheme.tertiary
                    )
                }
                
                InfoSection("Estado") {
                    Text(
                        disp.estadoEntrega, 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (disp.estadoEntrega == "Entregado") MaterialTheme.colorScheme.tertiary else Color(0xFFFF9800)
                    )
                }
            }
        },
        confirmButton = { 
            Button(onClick = { onDismiss(); onEdit() }) { 
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Completo") 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Cerrar") } 
        }
    )
}

@Composable
fun ServiciosExtraList(servicios: List<ServicioExtra>, onEdit: (String) -> Unit) {
    if (servicios.isEmpty()) {
        EmptyListMessage("No hay servicios varios.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(servicios) { serv ->
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(serv.fecha))
                val saldo = serv.montoTotal - serv.aCuenta
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    onClick = { onEdit(serv.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = date, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Surface(
                                color = if (serv.estado == "Entregado") MaterialTheme.colorScheme.tertiary else Color(0xFFFF9800),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(serv.estado, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Text(serv.descripcion, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Saldo:", fontSize = 12.sp, color = Color.Gray)
                            val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                            Text("s/. $formattedSaldo", color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyListMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
            Text(text, color = Color.Gray, fontSize = 14.sp)
        }
    }
}
