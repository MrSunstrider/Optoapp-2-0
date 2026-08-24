package com.example.optoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoKpiCard
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.FileShareUtils
import com.example.optoapp.util.ReporteFinancieroPdfGenerator
import com.example.optoapp.util.fmt
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.ReportesUiPolicy
import com.example.optoapp.viewmodel.ReportesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    drawerState: DrawerState,
    viewModel: ReportesViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val dispensaciones by viewModel.allDispensaciones.collectAsState()
    val serviciosExtra by viewModel.allServiciosDelPeriodo.collectAsState()
    val pagosSumByDispensacion by viewModel.pagosSumByDispensacion.collectAsState()
    val aCuentaSumByServicio by viewModel.aCuentaSumByServicio.collectAsState()
    val scope = rememberCoroutineScope()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = null)
    val access = ReportesUiPolicy.resolveAccess(opticaRol)
    val canView = !access.isRestricted && access.showTotals

    val periodo by viewModel.periodo.collectAsState()
    val fechaDiario by viewModel.fechaDiario.collectAsState()
    val showsPeriodChrome by viewModel.showsPeriodChrome.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = fechaDiario,
            onDateSelected = { viewModel.setFechaDiario(it) },
            onDismiss = { showDatePicker = false },
        )
    }

    val periodoLabel by viewModel.periodoLabel.collectAsState()

    val totalVendido by viewModel.totalVendido.collectAsState()
    val totalCobrado by viewModel.totalCobrado.collectAsState()
    val cobrosPeriodo by viewModel.cobrosPeriodo.collectAsState()
    val totalTransacciones by viewModel.totalTransacciones.collectAsState()
    val dispensacionesCount by viewModel.dispensacionesCount.collectAsState()
    val serviciosCount by viewModel.serviciosCount.collectAsState()
    val isDataLoading by viewModel.isLoading.collectAsState()
    var isPdfLoading by remember { mutableStateOf(false) }

    val ventasPeriodo = totalCobrado - cobrosPeriodo
    val porCobrar by viewModel.porCobrar.collectAsState()
    val ticketPromedio = if (totalTransacciones > 0) totalVendido / totalTransacciones else 0.0
    val headlineKpiIds = ReportesUiPolicy.headlineKpiIds

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
                    if (canView) {
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
                                        aCuentaSumByServicio = aCuentaSumByServicio,
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
                },
            )
        },
    ) { padding ->
        if (opticaRol == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            return@Scaffold
        }

        if (!canView) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Bloqueado", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Acceso restringido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Tu rol no tiene permiso para ver esta sección.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isDataLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        com.example.optoapp.ui.components.OptoDropdownMenuField(
                            label = "Período",
                            selected = periodo,
                            options = listOf("Diario", "Semanal", "Mensual", "Anual", "Total"),
                            onSelected = { viewModel.setPeriodo(it) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (showsPeriodChrome) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                IconButton(onClick = { viewModel.previous() }) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior")
                                }
                                Text(
                                    periodoLabel,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                )
                                Row {
                                    if (periodo == "Diario" || periodo == "Semanal") {
                                        IconButton(onClick = { showDatePicker = true }) {
                                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    IconButton(onClick = { viewModel.next() }) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if ("vendido" in headlineKpiIds) {
                        OptoKpiCard("Vendido", "s/. ${totalVendido.fmt()}", MaterialTheme.colorScheme.primary, Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
                    }
                    if ("cobrado" in headlineKpiIds) {
                        OptoKpiCard("Cobrado", "s/. ${totalCobrado.fmt()}", MaterialTheme.colorScheme.positiveGreen, Icons.Default.Payments, Modifier.weight(1f))
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if ("porCobrar" in headlineKpiIds) {
                        OptoKpiCard("Por Cobrar", "s/. ${porCobrar.fmt()}", if (porCobrar > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.positiveGreen, Icons.Default.AccountBalanceWallet, Modifier.weight(1f))
                    }
                    if ("ticketPromedio" in headlineKpiIds) {
                        OptoKpiCard("Ticket Prom.", "s/. ${ticketPromedio.fmt()}", MaterialTheme.colorScheme.secondary, Icons.Default.Receipt, Modifier.weight(1f))
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if ("transacciones" in headlineKpiIds) {
                        OptoKpiCard("Transacciones", "$totalTransacciones", MaterialTheme.colorScheme.tertiary, Icons.Default.ShoppingCart, Modifier.weight(1f))
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
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
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Composición", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            val maxVal = maxOf(dispensacionesCount, serviciosCount, 1).toFloat()
                            BarRow("Dispensaciones", dispensacionesCount, maxVal, MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            BarRow("Servicios Extra", serviciosCount, maxVal, MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            item {
                Text("Detalle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (!isDataLoading && dispensaciones.isEmpty() && serviciosExtra.isEmpty()) {
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
                Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
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
                                color = if (saldo > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.positiveGreen,
                            )
                        }
                    }
                }
            }

            items(serviciosExtra) { serv ->
                val date = DateUtils.formatLocalized(serv.fecha)
                val aCuenta = aCuentaSumByServicio[serv.id] ?: 0.0
                val saldo = serv.montoTotal - aCuenta
                Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
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
                                color = if (saldo > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.positiveGreen,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CobroRow(label: String, amount: Double, color: Color, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text("s/. ${amount.fmt()}", fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = color, fontSize = 13.sp)
    }
}

@Composable
private fun BarRow(label: String, count: Int, maxVal: Float, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, modifier = Modifier.width(100.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (count / maxVal).coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(4.dp)),
            )
        }
        Text(" $count", fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
    }
}
