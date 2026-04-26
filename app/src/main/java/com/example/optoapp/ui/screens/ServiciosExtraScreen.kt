package com.example.optoapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.viewmodel.ServiciosViewModel
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.util.DateUtils
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiciosExtraScreen(navController: NavController, drawerState: DrawerState, viewModel: ServiciosViewModel = hiltViewModel()) {
    val servicios by viewModel.allServicios.collectAsState()
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val filteredServicios = if (searchQuery.isEmpty()) servicios 
    else servicios.filter { it.descripcion.contains(searchQuery, ignoreCase = true) || it.ot.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Servicios Varios") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("nuevo_servicio") }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Servicio")
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
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por descripción u OT...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (filteredServicios.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron servicios.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredServicios) { servicio ->
                        ServicioRow(servicio, 
                            onEdit = { navController.navigate("editar_servicio/${servicio.id}") },
                            onDelete = { viewModel.deleteServicio(servicio) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ServicioRow(servicio: ServicioExtra, onEdit: () -> Unit, onDelete: () -> Unit) {
    val saldo = servicio.montoTotal - servicio.aCuenta

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (servicio.ot.isNotBlank()) {
                    Text(text = "OT: ${servicio.ot}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = servicio.descripcion,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Total: s/. ${String.format(Locale.getDefault(), "%.2f", servicio.montoTotal)}", fontSize = 14.sp)
                Text(text = "Saldo: s/. ${String.format(Locale.getDefault(), "%.2f", saldo)}", 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold,
                    color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Badge(containerColor = if (servicio.estado == "Entregado") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary) {
                    Text(servicio.estado, color = if (servicio.estado == "Entregado") MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondary)
                }
                Text(text = DateUtils.formatLocalized(servicio.fecha), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
