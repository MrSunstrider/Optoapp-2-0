package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Evaluaciones", "Dispensaciones")

    LaunchedEffect(id) {
        paciente = viewModel.getPaciente(id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Paciente") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Implement PDF logic */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar a PDF")
                    }
                    IconButton(onClick = { navController.navigate("editarPaciente/${id}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            )
        }
    ) { padding ->
        paciente?.let { p ->
            Column(modifier = Modifier.padding(padding)) {
                // Patient Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = p.nombreCompleto, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(text = "ID: ${p.id}", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Edad: ${p.edad} años")
                            Text(text = "Tel: ${p.telefono}")
                        }
                        if (!p.dni.isNullOrBlank()) Text(text = "DNI: ${p.dni}")
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
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
                    }
                    
                    FloatingActionButton(
                        onClick = {
                            if (selectedTab == 0) navController.navigate("nuevaEvaluacion/${id}")
                            else navController.navigate("nuevaDispensacion/${id}")
                        },
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
    if (evaluaciones.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay evaluaciones registradas.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(evaluaciones) { eval ->
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(eval.fecha))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEdit(eval.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = date, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                        }
                        Text(text = "Diagnóstico: ${eval.diagnostico.ifBlank { "Sin especificar" }}", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun DispensacionesList(dispensaciones: List<DispensacionOptica>, onEdit: (String) -> Unit) {
    if (dispensaciones.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay órdenes de dispensación.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dispensaciones) { disp ->
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(disp.fecha))
                val saldo = disp.montoTotal - disp.montoPagado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onEdit(disp.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = date, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(containerColor = if (disp.estadoEntrega == "Entregado") Color(0xFF4CAF50) else Color(0xFFFF9800)) {
                                    Text(disp.estadoEntrega, color = Color.Black)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${disp.tipoLente} - ${disp.materialLente}", fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Total: $${disp.montoTotal}")
                            Text(text = "Saldo: $${String.format("%.2f", saldo)}", color = if (saldo > 0) Color.Red else Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
