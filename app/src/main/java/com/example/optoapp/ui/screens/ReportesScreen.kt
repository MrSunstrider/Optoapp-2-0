package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Info
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
import com.example.optoapp.viewmodel.OptoViewModel
import com.example.optoapp.viewmodel.OptoViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(drawerState: DrawerState) {
    val context = LocalContext.current
    val app = context.applicationContext as OptoApplication
    val viewModel: OptoViewModel = viewModel(
        factory = OptoViewModelFactory(app.repository, app.securityManager)
    )
    val dispensaciones by viewModel.allDispensaciones.collectAsState()
    val scope = rememberCoroutineScope()
    
    var periodo by remember { mutableStateOf("Este mes") }
    
    val totalVendido = dispensaciones.sumOf { d: DispensacionOptica -> d.montoTotal }
    val totalPagado = dispensaciones.sumOf { d: DispensacionOptica -> d.montoPagado }
    val porCobrar = totalVendido - totalPagado
    val ticketPromedio = if (dispensaciones.isNotEmpty()) totalVendido / dispensaciones.size else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes Financieros") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Print logic */ }) {
                        Icon(Icons.Default.Info, contentDescription = "Info")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Re-adding DropdownField inline to avoid unresolved reference
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = periodo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Período") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("Este mes", "Este año", "Todo").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                periodo = opt
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportCard(
                    title = "Total Vendido",
                    value = "$${String.format("%.2f", totalVendido)}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ReportCard(
                    title = "Por Cobrar",
                    value = "$${String.format("%.2f", porCobrar)}",
                    color = Color.Red,
                    modifier = Modifier.weight(1f)
                )
            }
            
            ReportCard(
                title = "Ticket Promedio",
                value = "$${String.format("%.2f", ticketPromedio)}",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Detalle de Ventas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dispensaciones) { disp: DispensacionOptica ->
                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(disp.fecha))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(date, fontWeight = FontWeight.Bold)
                                Text("Pago: ${disp.metodoPago}", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$${disp.montoTotal}", fontWeight = FontWeight.Bold)
                                Text("Saldo: $${String.format("%.2f", disp.montoTotal - disp.montoPagado)}", fontSize = 12.sp, color = if (disp.montoTotal - disp.montoPagado > 0) Color.Red else Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
