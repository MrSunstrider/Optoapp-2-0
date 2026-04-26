package com.example.optoapp.ui.screens

import android.app.Activity
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
import com.example.optoapp.viewmodel.PacienteViewModel
import com.example.optoapp.viewmodel.SubscriptionViewModel
import com.example.optoapp.subscription.SubscriptionTier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacientesListScreen(navController: NavController, drawerState: DrawerState, viewModel: PacienteViewModel = hiltViewModel(), subscriptionVm: SubscriptionViewModel = hiltViewModel()) {
    val pacientes by viewModel.pacientes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val canAddPaciente by subscriptionVm.canAddPaciente.collectAsState()
    val tier by subscriptionVm.tier.collectAsState()
    val context = LocalContext.current
    var showPaywall by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        subscriptionVm.refreshPlanFromServer()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text("Pacientes", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (canAddPaciente) navController.navigate("nuevoPaciente")
                else showPaywall = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Paciente")
            }
        }
    ) { padding ->
        if (showPaywall) {
            AlertDialog(
                onDismissRequest = { showPaywall = false },
                title = { Text("Límite del plan gratuito") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Has alcanzado el máximo de pacientes del plan gratuito. Pasa a PRO para registros ilimitados.")
                        Text("Plan actual: ${if (tier == SubscriptionTier.PRO) "PRO" else "Gratuito"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val act = context as? Activity
                            if (act != null) {
                                subscriptionVm.launchProPurchase(act) { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() }
                            }
                            showPaywall = false
                        }
                    ) { Text("Actualizar plan") }
                },
                dismissButton = {
                    TextButton(onClick = { showPaywall = false }) { Text("Cerrar") }
                }
            )
        }
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
                    selected = activeFilter == "Estado de entrega",
                    onClick = { viewModel.setFilter("Estado de entrega") },
                    label = { Text("Estado de entrega: Pendiente") }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                text = "Creado: ${DateUtils.formatLocalized(paciente.fechaCreacion)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
