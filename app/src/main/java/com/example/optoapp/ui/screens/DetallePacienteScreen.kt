package com.example.optoapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
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
import android.widget.Toast
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.optoapp.util.WhatsAppUtils
import com.example.optoapp.util.RecetaEvaluacionPdfGenerator
import com.example.optoapp.util.DispensacionLaboratorioTicket
import com.example.optoapp.util.LaboratorioTicketContext
import com.example.optoapp.ui.components.LaboratorioTicketAlertDialog
import com.example.optoapp.viewmodel.LaboratorioConfigViewModel
import com.example.optoapp.viewmodel.DeletePacienteResult
import androidx.compose.material.icons.filled.Science

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
    val scope = rememberCoroutineScope()
    var paciente by remember { mutableStateOf<Paciente?>(null) }
    val evaluaciones by evaluacionViewModel.getEvaluacionesByPaciente(id).collectAsState(initial = emptyList())
    val dispensaciones by dispensacionViewModel.getDispensacionesByPaciente(id).collectAsState(initial = emptyList())
    val servicios by remember(serviciosViewModel.allServicios, id) {
        serviciosViewModel.allServicios.map { list -> list.filter { it.pacienteId == id } }
    }.collectAsState(initial = emptyList())
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Evaluaciones", "Dispensaciones", "Servicios")
    var showWhatsAppMenu by remember { mutableStateOf(false) }
    var showDeletePacienteDialog by remember { mutableStateOf(false) }
    var deletingPaciente by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        paciente = pacienteViewModel.getPaciente(id)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Ficha del Paciente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showWhatsAppMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp")
                        }
                        DropdownMenu(
                            expanded = showWhatsAppMenu,
                            onDismissRequest = { showWhatsAppMenu = false }
                        ) {
                            paciente?.let { p ->
                                val nombre = p.nombreCompleto.split(" ").firstOrNull() ?: ""
                                
                                DropdownMenuItem(
                                    text = { Text("Mensaje Libre (Saludo)") },
                                    onClick = { 
                                        showWhatsAppMenu = false
                                        WhatsAppUtils.sendWhatsAppMessage(context, p.telefono, "Hola $nombre,")
                                    }
                                )
                                
                                DropdownMenuItem(
                                    text = { Text("Invitación Control Anual") },
                                    onClick = {
                                        showWhatsAppMenu = false
                                        val msg = "Hola $nombre, te saludamos de Óptica Sersa Visual y Preventiva. Nos preocupamos por tu salud visual; te recordamos que ya se cumplió un año desde tu última evaluación y te invitamos a visitarnos para tu control optométrico anual. ¡Esperamos verte pronto!"
                                        WhatsAppUtils.sendWhatsAppMessage(context, p.telefono, msg)
                                    }
                                )
                                
                                val ultimaEval = evaluaciones.maxByOrNull { it.fecha }
                                if (ultimaEval?.proximaCita != null) {
                                    val proxFecha = com.example.optoapp.util.DateUtils.formatLocalized(ultimaEval.proximaCita)
                                    DropdownMenuItem(
                                        text = { Text("Recordar Próxima Cita ($proxFecha)") },
                                        onClick = {
                                            showWhatsAppMenu = false
                                            val msg = "Hola $nombre, te saludamos de Óptica Sersa Visual y Preventiva. Te recordamos cordialmente que tienes una cita de control optométrico programada con nosotros para el día $proxFecha. ¡Te esperamos!"
                                            WhatsAppUtils.sendWhatsAppMessage(context, p.telefono, msg)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            val p = paciente
                            if (p == null) {
                                Toast.makeText(context, "Esperando datos del paciente…", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            val ultima = evaluaciones.maxByOrNull { it.fecha }
                            if (ultima == null) {
                                Toast.makeText(context, "No hay evaluaciones para generar la fórmula en PDF", Toast.LENGTH_LONG).show()
                                return@IconButton
                            }
                            try {
                                val file = RecetaEvaluacionPdfGenerator.generate(context, p, ultima)
                                RecetaEvaluacionPdfGenerator.openPdf(context, file)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo generar el PDF: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar fórmula en PDF")
                    }
                    IconButton(onClick = { navController.navigate("editarPaciente/${id}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Perfil")
                    }
                    IconButton(
                        onClick = { showDeletePacienteDialog = true },
                        enabled = !deletingPaciente
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar Paciente")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when(selectedTab) {
                        0 -> navController.navigate("nuevaEvaluacion/${id}")
                        1 -> navController.navigate("nuevaDispensacion/${id}")
                        2 -> navController.navigate("nuevo_servicio/${id}")
                    }
                },
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
    ) { padding ->
        paciente?.let { p ->
            if (showDeletePacienteDialog) {
                AlertDialog(
                    onDismissRequest = { if (!deletingPaciente) showDeletePacienteDialog = false },
                    title = { Text("¿Eliminar paciente?") },
                    text = {
                        Text(
                            "Esta acción eliminará también evaluaciones, dispensaciones y servicios relacionados. " +
                                "Existe un límite diario de seguridad."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (deletingPaciente) return@TextButton
                                deletingPaciente = true
                                val pacienteToDelete = p
                                scope.launch {
                                    try {
                                        when (val result = pacienteViewModel.deletePacienteGuarded(pacienteToDelete)) {
                                            is DeletePacienteResult.Success -> {
                                                Toast.makeText(
                                                    context,
                                                    "Paciente eliminado. Quedan ${result.remainingDeletesToday} eliminaciones hoy.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                showDeletePacienteDialog = false
                                                navController.popBackStack()
                                            }
                                            is DeletePacienteResult.Error -> {
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } finally {
                                        deletingPaciente = false
                                    }
                                }
                            },
                            enabled = !deletingPaciente
                        ) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeletePacienteDialog = false },
                            enabled = !deletingPaciente
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }
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
                            Text(text = "ID: ${p.id.take(8)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        1 -> DispensacionesList(
                            dispensaciones = dispensaciones,
                            paciente = p,
                            evaluaciones = evaluaciones,
                            onEdit = { dispId ->
                                navController.navigate("editarDispensacion/${id}/${dispId}")
                            }
                        )
                        2 -> ServiciosExtraList(servicios) { servId ->
                            navController.navigate("editar_servicio/${servId}")
                        }
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
                val date = com.example.optoapp.util.DateUtils.formatLocalized(eval.fecha)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error) 
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
                                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
                        val formulaStr = buildString {
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

                        if (formulaStr.isNotBlank()) {
                            Text(text = "Fórmula $formulaStr", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (diagStr.isNotBlank()) {
                                Text(text = "Diag: $diagStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (diagStr.isNotBlank()) {
                            Text("Diagnóstico: $diagStr", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            Text("Sin fórmula ni diagnóstico", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val date = com.example.optoapp.util.DateUtils.formatLocalized(eval.fecha)
    val proxima = eval.proximaCita?.let { com.example.optoapp.util.DateUtils.formatLocalized(it) } ?: "No programada"

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
                    if (!paciente.historiaOptometrica.isNullOrBlank()) Text("Historia Optométrica: ${paciente.historiaOptometrica}", fontSize = 14.sp)
                    Text("Fecha de Eval: $date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                val showOd = eval.recetaOdEsf.isNotBlank() || eval.recetaOdCil.isNotBlank()
                val showOi = eval.recetaOiEsf.isNotBlank() || eval.recetaOiCil.isNotBlank()
                if (showOd || showOi) {
                    InfoSection("Fórmula final (gafas)") {
                        if (showOd) {
                            Text("OD: ${eval.recetaOdEsf} / ${eval.recetaOdCil} x ${eval.recetaOdEje}°", fontSize = 14.sp)
                        }
                        if (showOi) {
                            Text("OI: ${eval.recetaOiEsf} / ${eval.recetaOiCil} x ${eval.recetaOiEje}°", fontSize = 14.sp)
                        }
                    }
                }

                val avccOd = eval.recetaOdAv.ifBlank { eval.avCcOdLejos }
                val avccOi = eval.recetaOiAv.ifBlank { eval.avCcOiLejos }
                val avccAo = eval.avCcAoPx
                val hasAvcc = avccOd.isNotBlank() || avccOi.isNotBlank() || avccAo.isNotBlank()
                if (hasAvcc) {
                    InfoSection("Agudeza visual (AV CC)") {
                        Text("AVCC OD: ${if (avccOd.isBlank()) "—" else avccOd}       AV CC AO", fontSize = 14.sp)
                        Text("AVCC OI: ${if (avccOi.isBlank()) "—" else avccOi}       ${if (avccAo.isBlank()) "—" else avccAo}", fontSize = 14.sp)
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
            Row {
                val context = LocalContext.current
                IconButton(onClick = {
                    val formulaStr = buildString {
                        append("Hola ${paciente.nombreCompleto.split(" ").firstOrNull() ?: ""}, esta es tu fórmula optométrica:\n\n")
                        val showOd = eval.recetaOdEsf.isNotBlank() || eval.recetaOdCil.isNotBlank()
                        val showOi = eval.recetaOiEsf.isNotBlank() || eval.recetaOiCil.isNotBlank()
                        if (showOd) {
                            append("👁️ Ojo Derecho (OD): ${eval.recetaOdEsf} / ${eval.recetaOdCil} x ${eval.recetaOdEje}°\n")
                        }
                        if (showOi) {
                            append("👁️ Ojo Izquierdo (OI): ${eval.recetaOiEsf} / ${eval.recetaOiCil} x ${eval.recetaOiEje}°\n")
                        }
                        if (eval.addCercaOd.isNotBlank() || eval.addCercaOi.isNotBlank() || eval.addAv.isNotBlank()) {
                            append("\n👓 Adición:\n")
                            if (eval.addCercaOd.isNotBlank()) append("OD: ${eval.addCercaOd}\n")
                            if (eval.addCercaOi.isNotBlank()) append("OI: ${eval.addCercaOi}\n")
                            if (eval.addAv.isNotBlank()) append("AV: ${eval.addAv}\n")
                        }
                        if (eval.proximaCita != null) {
                            val prox = com.example.optoapp.util.DateUtils.formatLocalized(eval.proximaCita)
                            append("\n📅 Próximo Control: $prox")
                        }
                    }
                    WhatsAppUtils.sendWhatsAppMessage(context, paciente.telefono, formulaStr)
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar fórmula a WhatsApp", tint = Color(0xFF25D366))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onDismiss(); onEdit() }) { 
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar Completo") 
                } 
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
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun DispensacionesList(
    dispensaciones: List<DispensacionOptica>,
    paciente: Paciente,
    evaluaciones: List<EvaluacionClinica>,
    onEdit: (String) -> Unit,
    laboratorioVm: LaboratorioConfigViewModel = hiltViewModel(),
) {
    val selectedDispForResumen = remember { mutableStateOf<DispensacionOptica?>(null) }
    val selectedDispForTicket = remember { mutableStateOf<DispensacionOptica?>(null) }
    val labCfg by laboratorioVm.uiState.collectAsState()

    if (dispensaciones.isEmpty()) {
        EmptyListMessage("No hay dispensaciones.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(dispensaciones) { disp ->
                val date = com.example.optoapp.util.DateUtils.formatLocalized(disp.fecha)
                val saldo = disp.montoTotal - disp.montoPagado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = { onEdit(disp.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = date, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(modifier = Modifier.size(24.dp), onClick = { selectedDispForTicket.value = disp }) {
                                    Icon(
                                        Icons.Filled.Science,
                                        contentDescription = "Ticket de laboratorio",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(modifier = Modifier.size(24.dp), onClick = { selectedDispForResumen.value = disp }) {
                                    Icon(Icons.Default.Visibility, contentDescription = "Ver Resumen", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }
                                Surface(
                                    color = if (disp.estadoEntrega == "Entregado") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        disp.estadoEntrega, 
                                        color = if (disp.estadoEntrega == "Entregado") MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondary, 
                                        fontSize = 10.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${disp.tipoLente} - ${disp.materialLente}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(text = "Saldo:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                            Text(text = "s/. $formattedSaldo", color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
    
    selectedDispForTicket.value?.let { dispTicket ->
        val ultimaEval = evaluaciones.maxByOrNull { it.fecha }
        val ticketCtx = LaboratorioTicketContext.fromDispensacion(dispTicket, paciente.nombreCompleto)
        LaboratorioTicketAlertDialog(
            onDismiss = { selectedDispForTicket.value = null },
            ticketTextoCompleto = DispensacionLaboratorioTicket.textoCompleto(ticketCtx, ultimaEval),
            ticketTextoCompacto = DispensacionLaboratorioTicket.textoCompacto(ticketCtx, ultimaEval),
            laboratorioNombre = labCfg.laboratorioNombre,
            laboratorioContacto = labCfg.laboratorioContacto,
        )
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
    val date = com.example.optoapp.util.DateUtils.formatLocalized(disp.fecha)

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
                    if (disp.ot.isNotBlank()) Text("OT: ${disp.ot}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Nombre: ${paciente.nombreCompleto}", fontSize = 14.sp)
                    Text("Edad: ${paciente.edad} años", fontSize = 14.sp)
                    if (!paciente.historiaOptometrica.isNullOrBlank()) Text("Historia Optométrica: ${paciente.historiaOptometrica}", fontSize = 14.sp)
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
                    if (disp.tipoLente.isNotBlank()) {
                        val subtipo = if (disp.tipoLente == "Bifocal" && disp.subTipoBifocal.isNotBlank()) " (${disp.subTipoBifocal})" else ""
                        val distancia = if (disp.tipoLente == "Monofocal" && disp.distanciaLente.isNotBlank()) " - ${disp.distanciaLente}" else ""
                        Text("Tipo: ${disp.tipoLente}$subtipo$distancia", fontSize = 14.sp)
                    }
                    if ((disp.tipoLente == "Bifocal" || disp.tipoLente == "Progresivo" || disp.tipoLente == "Ocupacional") && disp.altura.isNotBlank()) {
                        Text("Altura: ${disp.altura} mm", fontSize = 14.sp)
                    }
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
                        color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }
                
                InfoSection("Estado") {
                    Text(
                        text = disp.estadoEntrega,
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (disp.estadoEntrega == "Entregado") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = { 
            Row {
                val context = LocalContext.current
                IconButton(onClick = {
                    val saldo = disp.montoTotal - disp.montoPagado
                    val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                    val nombre = paciente.nombreCompleto.split(" ").firstOrNull() ?: ""
                    
                    val msg = buildString {
                        append("Hola $nombre, tus lentes ya están listos para recoger.")
                        if (saldo > 0) {
                            append(" Te recordamos que tienes un saldo pendiente de s/. $formattedSaldo.")
                        }
                    }
                    WhatsAppUtils.sendWhatsAppMessage(context, paciente.telefono, msg)
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onDismiss(); onEdit() }) { 
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar Completo") 
                } 
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
                val date = com.example.optoapp.util.DateUtils.formatLocalized(serv.fecha)
                val saldo = serv.montoTotal - serv.aCuenta
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = { onEdit(serv.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = date, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Surface(
                                color = if (serv.estado == "Entregado") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    serv.estado, 
                                    color = if (serv.estado == "Entregado") MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondary, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(serv.descripcion, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Saldo:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}
