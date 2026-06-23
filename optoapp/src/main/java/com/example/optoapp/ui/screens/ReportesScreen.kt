package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.viewmodel.ReportesViewModel
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.FileShareUtils
import com.example.optoapp.util.ReporteFinancieroPdfGenerator
import java.time.Year
import java.util.*
import kotlinx.coroutines.launch
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OptoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(drawerState: DrawerState, viewModel: ReportesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val dispensaciones by viewModel.allDispensaciones.collectAsState()
    val scope = rememberCoroutineScope()
    
    val periodo by viewModel.periodo.collectAsState()
    val anio by viewModel.anio.collectAsState()
    val fechaDiario by viewModel.fechaDiario.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(fechaDiario)
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setFechaDiario(DateUtils.pickerMillisToLocalDate(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    val totalVendido by viewModel.totalVendido.collectAsState()
    val totalPagado by viewModel.totalPagado.collectAsState()
    val totalCobrado by viewModel.totalCobrado.collectAsState()
    val cobrosPeriodo by viewModel.cobrosPeriodo.collectAsState()
    val ventasPeriodo = totalCobrado - cobrosPeriodo
    val porCobrar = totalVendido - totalPagado
    val ticketPromedio = if (dispensaciones.isNotEmpty()) totalVendido / dispensaciones.size else 0.0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = "Reportes Financieros",
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val pdf = ReporteFinancieroPdfGenerator.generate(
                                    context = context,
                                    dispensaciones = dispensaciones,
                                    periodo = periodo,
                                    totalVendido = totalVendido,
                                    porCobrar = porCobrar,
                                    ticketPromedio = ticketPromedio
                                )
                                FileShareUtils.openPdf(context, pdf, "Abrir reporte financiero")
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Error al generar PDF: ${e.localizedMessage}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Generar PDF")
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(if (periodo == "Anual" || periodo == "Diario") 1f else 2f)) {
                    DropdownField(
                        label = "Período",
                        selected = periodo,
                        options = listOf("Diario", "Semanal", "Este mes", "Este año", "Anual", "Todo"),
                        onSelected = { viewModel.setPeriodo(it) }
                    )
                }
                
                if (periodo == "Diario" || periodo == "Semanal") {
                    OutlinedButton(
                        onClick = {
                            datePickerState.selectedDateMillis = DateUtils.localDateToPickerMillis(fechaDiario)
                            showDatePicker = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(DateUtils.formatLocalized(fechaDiario), fontSize = 12.sp)
                    }
                }
                
                if (periodo == "Anual") {
                    Box(modifier = Modifier.weight(1f)) {
                        val currentYear = Year.now().value
                        val years = (2020..currentYear + 1).map { it.toString() }
                        DropdownField(
                            label = "Año",
                            selected = anio,
                            options = years,
                            onSelected = { viewModel.setAnio(it) }
                        )
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportCard(
                    title = "Total Vendido",
                    value = "s/. ${String.format(java.util.Locale.getDefault(), "%.2f", totalVendido)}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ReportCard(
                    title = "Por Cobrar",
                    value = "s/. ${String.format(java.util.Locale.getDefault(), "%.2f", porCobrar)}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
            
            ReportCard(
                title = "Ticket Promedio",
                value = "s/. ${String.format(java.util.Locale.getDefault(), "%.2f", ticketPromedio)}",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )

            OptoCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Cobros del período", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ventas del período", fontSize = 13.sp)
                        Text("s/. ${String.format(java.util.Locale.getDefault(), "%.2f", ventasPeriodo)}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cobros de períodos anteriores", fontSize = 13.sp)
                        Text("s/. ${String.format(java.util.Locale.getDefault(), "%.2f", cobrosPeriodo)}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total cobrado", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("s/. ${String.format(java.util.Locale.getDefault(), "%.2f", totalCobrado)}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text("Detalle de Ventas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dispensaciones.isEmpty()) {
                    item { Text("No hay transacciones registradas este día", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(dispensaciones) { disp ->
                        val date = DateUtils.formatLocalized(disp.fecha)
                        OptoCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(date, fontWeight = FontWeight.Bold)
                                    Text("Pago: ${disp.metodoPago}", fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("s/. ${disp.montoTotal}", fontWeight = FontWeight.Bold)
                                    Text("Saldo: s/. ${String.format(java.util.Locale.getDefault(), "%.2f", disp.montoTotal - disp.montoPagado)}", fontSize = 12.sp, color = if (disp.montoTotal - disp.montoPagado > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                }
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
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
