package com.example.optoapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import com.example.optoapp.data.Paciente
import com.example.optoapp.viewmodel.OptoViewModel
import com.example.optoapp.viewmodel.OptoViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacientesListScreen(navController: NavController, drawerState: DrawerState) {
    val context = LocalContext.current
    val app = context.applicationContext as OptoApplication
    val viewModel: OptoViewModel = viewModel(
        factory = OptoViewModelFactory(app.repository, app.securityManager)
    )
    val pacientes by viewModel.pacientes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OptoApp", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Lista de Pacientes", fontSize = 14.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("nuevoPaciente") }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Paciente")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre, ID o teléfono...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFilter == "Saldo Pendiente",
                    onClick = { viewModel.setFilter("Saldo Pendiente") },
                    label = { Text("Saldo Pendiente") }
                )
                FilterChip(
                    selected = activeFilter == "Fotocromáticos",
                    onClick = { viewModel.setFilter("Fotocromáticos") },
                    label = { Text("Fotocromáticos") }
                )
                FilterChip(
                    selected = activeFilter == "Multifocales",
                    onClick = { viewModel.setFilter("Multifocales") },
                    label = { Text("Multifocales") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pacientes) { paciente ->
                    PacienteRow(paciente) {
                        navController.navigate("detallePaciente/${paciente.id}")
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun PacienteRow(paciente: Paciente, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = paciente.nombreCompleto,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "ID: ${paciente.id.take(8)}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Edad: ${paciente.edad}", fontSize = 14.sp)
            Text(text = "Tel: ${paciente.telefono}", fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Creado: ${dateFormat.format(Date(paciente.fechaCreacion))}",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            paciente.ultimasEtiquetas.take(2).forEach { etiqueta ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(etiqueta, fontSize = 10.sp) },
                    modifier = Modifier.height(24.dp).padding(horizontal = 2.dp)
                )
            }
        }
    }
}
