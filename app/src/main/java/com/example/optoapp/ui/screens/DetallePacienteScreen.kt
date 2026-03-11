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
import com.example.optoapp.viewmodel.OptoViewModel
import com.example.optoapp.viewmodel.OptoViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePacienteScreen(navController: NavController, id: String) {
    val context = LocalContext.current
    val app = context.applicationContext as OptoApplication
    val viewModel: OptoViewModel = viewModel(
        factory = OptoViewModelFactory(app.repository, app.securityManager)
    )
    
    var paciente by remember { mutableStateOf<Paciente?>(null) }
    val evaluaciones by viewModel.getEvaluaciones(id).collectAsState(initial = emptyList())
    val dispensaciones by viewModel.getDispensaciones(id).collectAsState(initial = emptyList())
    val servicios by viewModel.getServiciosByPaciente(id).collectAsState(initial = emptyList())
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Evaluaciones", "Dispensaciones", "Servicios")

    LaunchedEffect(id) {
        paciente = viewModel.getPaciente(id)
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
                        0 -> EvaluacionesList(evaluaciones) { evalId ->
                            navController.navigate("editarEvaluacion/${id}/${evalId}")
                        }
                        1 -> DispensacionesList(dispensaciones) { dispId ->
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
fun EvaluacionesList(evaluaciones: List<EvaluacionClinica>, onEdit: (String) -> Unit) {
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
                            }
                        }
                        Text(
                            text = "Receta OD: ${eval.recetaOdEsf}/${eval.recetaOdCil}x${eval.recetaOdEje}° | OI: ${eval.recetaOiEsf}/${eval.recetaOiCil}x${eval.recetaOiEje}°",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
    
    selectedEvalForResumen.value?.let { currentEval ->
        ResumenEvaluacionDialog(
            eval = currentEval,
            onDismiss = { selectedEvalForResumen.value = null },
            onEdit = { onEdit(currentEval.id) }
        )
    }
}

@Composable
fun ResumenEvaluacionDialog(eval: EvaluacionClinica, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(eval.fecha))
    val proxima = eval.proximaCita?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)) } ?: "No programada"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resumen Clínico - $date") 
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoSection("Refracción Final (Gafas)") {
                    Text("OD: ${eval.recetaOdEsf} / ${eval.recetaOdCil} x ${eval.recetaOdEje}°", fontSize = 14.sp)
                    Text("OI: ${eval.recetaOiEsf} / ${eval.recetaOiCil} x ${eval.recetaOiEje}°", fontSize = 14.sp)
                    if (eval.addCercaOd.isNotBlank()) Text("ADD Cerca: OD ${eval.addCercaOd} / OI ${eval.addCercaOi}", fontSize = 14.sp)
                }
                
                InfoSection("Queratometría") {
                    Text("OD: ${eval.k1Od} / ${eval.k2Od}", fontSize = 14.sp)
                    Text("OI: ${eval.k1Oi} / ${eval.k2Oi}", fontSize = 14.sp)
                }
                
                val tieneLC = eval.lcOdEsf.isNotBlank() || eval.lcOiEsf.isNotBlank()
                InfoSection("Contactología") {
                    Text(if (tieneLC) "Contiene datos de adaptación" else "Sin datos de contacto", color = if(tieneLC) MaterialTheme.colorScheme.tertiary else Color.Gray, fontSize = 14.sp)
                }
                
                InfoSection("Próxima Cita") {
                    Text(proxima, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
fun DispensacionesList(dispensaciones: List<DispensacionOptica>, onEdit: (String) -> Unit) {
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
                            Surface(
                                color = if (disp.estadoEntrega == "Entregado") MaterialTheme.colorScheme.tertiary else Color(0xFFFF9800),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(disp.estadoEntrega, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${disp.tipoLente} - ${disp.materialLente}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(text = "Saldo:", fontSize = 12.sp, color = Color.Gray)
                            val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                            Text(text = "S/. $formattedSaldo", color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
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
                            Text("S/. $formattedSaldo", color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
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
