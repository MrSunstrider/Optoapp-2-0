package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.viewmodel.CierreCajaViewModel
import com.example.optoapp.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreCajaScreen(navController: NavController, viewModel: CierreCajaViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.fecha
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setFecha(it) }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cierre de Caja") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Cambiar Fecha")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header con Fecha
            Text(
                text = "Reporte del ${SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "PE")).format(Date(uiState.fecha))}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Resumen de Totales
            val totales = viewModel.getTotalesPorMetodo()
            val totalGeneral = totales.values.sum()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResumenCard("Efectivo", totales["Efectivo"] ?: 0.0, Modifier.weight(1f), Color(0xFF4CAF50))
                ResumenCard("Móvil/Trans", (totales["Transferencia"] ?: 0.0) + (totales["Móvil"] ?: 0.0), Modifier.weight(1f), Color(0xFF2196F3))
                ResumenCard("Tarjeta", totales["Tarjeta"] ?: 0.0, Modifier.weight(1f), Color(0xFF9C27B0))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL RECAUDADO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "s/. ${String.format(Locale.getDefault(), "%.2f", totalGeneral)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Detalle de Transacciones", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (uiState.pagos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay transacciones registradas este día", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.pagos) { pago ->
                        TransactionItem(pago)
                    }
                }
            }
        }
    }
}

@Composable
fun ResumenCard(label: String, monto: Double, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                "s/. ${String.format(Locale.getDefault(), "%.0f", monto)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun TransactionItem(pago: com.example.optoapp.data.Pago) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (pago.dispensacionId != null) "Dispensación" else "Servicio Extra",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(pago.metodoPago, fontWeight = FontWeight.Bold)
                if (pago.nota.isNotEmpty()) {
                    Text(pago.nota, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Text(
                "s/. ${String.format(Locale.getDefault(), "%.2f", pago.monto)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
    }
}
