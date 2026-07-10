package com.example.optoapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.viewmodel.ReportesViewModel
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.FileShareUtils
import com.example.optoapp.util.fmt
import com.example.optoapp.util.ReporteFinancieroPdfGenerator
import com.example.optoapp.ui.components.OptoDatePickerDialog
import java.time.Year
import kotlinx.coroutines.launch
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OptoCard
import com.example.optoapp.ui.components.OptoKpiCard
import com.example.optoapp.ui.theme.PositiveGreen
import com.example.optoapp.ui.theme.AlertRed
import com.example.optoapp.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(drawerState: DrawerState, viewModel: ReportesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val dispensaciones by viewModel.allDispensaciones.collectAsState()
    val serviciosExtra by viewModel.allServiciosDelPeriodo.collectAsState()
    val pagosSumByDispensacion by viewModel.pagosSumByDispensacion.collectAsState()
    val aCuentaSumByServicio by viewModel.aCuentaSumByServicio.collectAsState()
    val scope = rememberCoroutineScope()

    val periodo by viewModel.periodo.collectAsState()
    val anio by viewModel.anio.collectAsState()
    val fechaDiario by viewModel.fechaDiario.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = fechaDiario,
            onDateSelected = { viewModel.setFechaDiario(it) },
            onDismiss = { showDatePicker = false }
        )
    }

    val periodoLabel by viewModel.periodoLabel.collectAsState()

    val totalVendido by viewModel.totalVendido.collectAsState()
    val totalPagado by viewModel.totalPagado.collectAsState()
    val totalCobrado by viewModel.totalCobrado.collectAsState()
    val cobrosPeriodo by viewModel.cobrosPeriodo.collectAsState()
    val totalTransacciones by viewModel.totalTransacciones.collectAsState()
    val dispensacionesCount by viewModel.dispensacionesCount.collectAsState()
    val serviciosCount by viewModel.serviciosCount.collectAsState()
    val isDataLoading by viewModel.isLoading.collectAsState()
    var isPdfLoading by remember { mutableStateOf(false) }

    val ventasPeriodo = totalCobrado - cobrosPeriodo
    val porCobrar = totalVendido - ventasPeriodo
    val ticketPromedio = if (totalTransacciones > 0) totalVendido / totalTransacciones else 0.0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = "Reportes",
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isPdfLoading = true
                            try {
                                val pdf = ReporteFinancieroPdfGenerator.generate(
                                    context = context,
                                    dispensaciones = dispensaciones,
                                    serviciosExtra = serviciosExtra,
                                    periodo = periodo,
                                    totalVendido = totalVendido,
                                    porCobrar = porCobrar,
                                    ticketPromedio = ticketPromedio,
                                    pagosSumByDispensacion = pagosSumByDispensacion,
                                    aCuentaSumByServicio = aCuentaSumByServicio
                                )
                                FileShareUtils.openPdf(context, pdf, "Abrir reporte financiero")
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Error al generar PDF", android.widget.Toast.LENGTH_SHORT).show()
                            } finally {
                                isPdfLoading = false
                            }
                        }
                    }) {
                        if (isPdfLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Generar PDF")
                        }
                    }
                }
            )
        }
        ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isDataLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DropdownField(
                            label = "Período",
                            selected = periodo,
                            options = listOf("Diario", "Semanal", "Mensual", "Anual", "Total"),
                            onSelected = { viewModel.setPeriodo(it) }
                        )
                        Spacer(Modifier.height(8.dp))
                        if (periodo != "Todo") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = { viewModel.previous() }) {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Anterior")
                                }
                                Text(
                                    periodoLabel,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha", modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { viewModel.next() }) {
                                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Siguiente")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptoKpiCard("Vendido", "s/. ${totalVendido.fmt()}", MaterialTheme.colorScheme.primary, Icons.Default.TrendingUp, Modifier.weight(1f))
                    OptoKpiCard("Cobrado", "s/. ${totalCobrado.fmt()}", PositiveGreen, Icons.Default.Payments, Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptoKpiCard("Por Cobrar", "s/. ${porCobrar.fmt()}", if (porCobrar > 0) AlertRed else PositiveGreen, Icons.Default.AccountBalanceWallet, Modifier.weight(1f))
                    OptoKpiCard("Ticket Prom.", "s/. ${ticketPromedio.fmt()}", MaterialTheme.colorScheme.secondary, Icons.Default.Receipt, Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptoKpiCard("Transacciones", "$totalTransacciones", MaterialTheme.colorScheme.tertiary, Icons.Default.ShoppingCart, Modifier.weight(1f))
                    OptoKpiCard("Pendiente", "s/. ${porCobrar.fmt()}", WarningAmber, Icons.Default.Schedule, Modifier.weight(1f))
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Desglose de Cobros", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        CobroRow("Ventas del período", ventasPeriodo, MaterialTheme.colorScheme.tertiary)
                        CobroRow("Cobros atrasados", cobrosPeriodo, MaterialTheme.colorScheme.secondary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        CobroRow("Total cobrado", totalCobrado, MaterialTheme.colorScheme.primary, bold = true)
                    }
                }
            }

            if (dispensacionesCount + serviciosCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Composición", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            val maxVal = maxOf(dispensacionesCount, serviciosCount, 1).toFloat()
                            BarRow("Dispensaciones", dispensacionesCount, maxVal, MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            BarRow("Servicios Extra", serviciosCount, maxVal, MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            item {
                Text("Detalle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (dispensaciones.isEmpty() && serviciosExtra.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Sin movimientos en este período", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(dispensaciones) { disp ->
                val date = DateUtils.formatLocalized(disp.fecha)
                val montoPagado = pagosSumByDispensacion[disp.id] ?: 0.0
                val saldo = disp.montoTotal - montoPagado
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(date, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Dispensación · OT ${disp.ot.ifBlank { "-" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("s/. ${disp.montoTotal.fmt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                if (saldo > 0) "Saldo: s/. ${saldo.fmt()}" else "Pagado",
                                fontSize = 11.sp,
                                color = if (saldo > 0) AlertRed else PositiveGreen
                            )
                        }
                    }
                }
            }

            items(serviciosExtra) { serv ->
                val date = DateUtils.formatLocalized(serv.fecha)
                val aCuenta = aCuentaSumByServicio[serv.id] ?: 0.0
                val saldo = serv.montoTotal - aCuenta
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(date, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Servicio · ${serv.descripcion.ifBlank { "Sin descripción" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("s/. ${serv.montoTotal.fmt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                if (saldo > 0) "Saldo: s/. ${saldo.fmt()}" else "Pagado",
                                fontSize = 11.sp,
                                color = if (saldo > 0) AlertRed else PositiveGreen
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CobroRow(label: String, amount: Double, color: Color, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text("s/. ${amount.fmt()}", fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = color, fontSize = 13.sp)
    }
}

@Composable
private fun BarRow(label: String, count: Int, maxVal: Float, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, modifier = Modifier.width(100.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (count / maxVal).coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
        Text(" $count", fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
    }
}


