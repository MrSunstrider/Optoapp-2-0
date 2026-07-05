package com.example.optoapp.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.arqueo.ArqueoCaja
import com.example.optoapp.viewmodel.ArqueoCajaViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.CierreCajaViewModel
import com.example.optoapp.util.ArqueoCajaPdfGenerator
import com.example.optoapp.util.DateUtils
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OptoCard
import com.example.optoapp.ui.components.cierre_caja.ArqueoSection
import com.example.optoapp.ui.components.cierre_caja.ResumenCard
import com.example.optoapp.ui.components.cierre_caja.TransactionItem
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreCajaScreen(
    navController: NavController,
    viewModel: CierreCajaViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    arqueoVM: ArqueoCajaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val arqueoUiState by arqueoVM.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val opticaId by authViewModel.opticaId.collectAsState(initial = "")
    val canView = AppRoles.canViewCierreCaja(opticaRol)
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(uiState.fecha)
    )

    LaunchedEffect(uiState.fecha, opticaId) {
        if (opticaId.isNotBlank()) {
            viewModel.observeArqueoForDate(uiState.fecha, opticaId)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setFecha(DateUtils.pickerMillisToLocalDate(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    fun exportPdf(arqueo: ArqueoCaja) {
        val pdfBytes = ArqueoCajaPdfGenerator.generate(arqueo, opticaId)
        val file = File(context.cacheDir, "arqueo_${arqueo.fecha}_${arqueo.opticaId}.pdf")
        file.writeBytes(pdfBytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Exportar Arqueo PDF"))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OptoTopAppBar(
                title = "Cierre de Caja",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (canView) {
                        uiState.arqueoForFecha?.let { arqueo ->
                            IconButton(onClick = { exportPdf(arqueo) }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF")
                            }
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Cambiar Fecha")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            if (!canView) {
                OptoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Acceso restringido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Tu rol actual no tiene permiso para consultar cierre de caja.")
                    }
                }
                return@Column
            }

            Text(
                text = "Reporte del ${DateUtils.formatLocalized(uiState.fecha)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OptoCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL VENTAS DEL DÍA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "s/. ${String.format(Locale.getDefault(), "%.2f", uiState.totalGeneral)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (uiState.saldoPendiente > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Saldo pendiente: s/. ${String.format(Locale.getDefault(), "%.2f", uiState.saldoPendiente)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totales = viewModel.getTotalesPorMetodo()
            val totalGeneral = totales.values.sum()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResumenCard("Efectivo", totales["Efectivo"] ?: 0.0, Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                ResumenCard("Móvil/Trans", (totales["Transferencia"] ?: 0.0) + (totales["Móvil"] ?: 0.0), Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                ResumenCard("Tarjeta", totales["Tarjeta"] ?: 0.0, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OptoCard(
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Desglose", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ventas de hoy", fontSize = 13.sp)
                        Text("s/. ${String.format(Locale.getDefault(), "%.2f", uiState.ventasHoy)}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cobros atrasados", fontSize = 13.sp)
                        Text("s/. ${String.format(Locale.getDefault(), "%.2f", uiState.cobrosAtrasados)}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total ingresado hoy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("s/. ${String.format(Locale.getDefault(), "%.2f", uiState.ventasHoy + uiState.cobrosAtrasados)}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (uiState.serviciosExtraHoy.isNotEmpty() || uiState.dispensacionesHoy.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Ventas del día (detalle)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        uiState.dispensacionesHoy.forEach { venta ->
                            val pacienteId = venta.pacienteId
                            val otLabel = if (venta.ot.isNotBlank()) "OT ${venta.ot}" else venta.origenId.take(8)
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        if (pacienteId.isNotBlank()) {
                                            navController.navigate("editarDispensacion/$pacienteId/${venta.origenId}")
                                        }
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(otLabel, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text("s/. ${String.format(Locale.getDefault(), "%.2f", venta.montoTotal)}",
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        uiState.serviciosExtraHoy.forEach { venta ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { navController.navigate("editar_servicio/${venta.origenId}") },
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Servicio ${venta.origenId.take(8)}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text("s/. ${String.format(Locale.getDefault(), "%.2f", venta.montoTotal)}",
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ArqueoSection(
                arqueoFromCierre = uiState.arqueoForFecha,
                arqueoUiState = arqueoUiState,
                systemTotals = totales,
                fecha = uiState.fecha,
                opticaId = opticaId,
                onFondoCajaChange = arqueoVM::setFondoCaja,
                onEfectivoContadoChange = arqueoVM::setEfectivoContado,
                onTarjetaContadoChange = arqueoVM::setTarjetaContado,
                onTransferenciaContadoChange = arqueoVM::setTransferenciaContado,
                onMovilContadoChange = arqueoVM::setMovilContado,
                onCerrarDia = { arqueoVM.cerrarDia(uiState.fecha, opticaId, totales) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Detalle de Transacciones", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.pagos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay transacciones registradas este día", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.pagos.forEach { pago ->
                        TransactionItem(pago)
                    }
                }
            }
        }
    }
}
