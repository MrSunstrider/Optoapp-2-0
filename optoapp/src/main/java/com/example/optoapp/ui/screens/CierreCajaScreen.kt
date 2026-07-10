package com.example.optoapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.CierreCajaViewModel
import com.example.optoapp.util.DateUtils
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoCard
import com.example.optoapp.ui.components.cierre_caja.ResumenCard
import com.example.optoapp.ui.components.cierre_caja.TransactionItem
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreCajaScreen(
    navController: NavController,
    viewModel: CierreCajaViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val opticaId by authViewModel.opticaId.collectAsState(initial = "")
    val canView = AppRoles.canViewCierreCaja(opticaRol)
    var showDatePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = uiState.fecha,
            onDateSelected = { viewModel.setFecha(it) },
            onDismiss = { showDatePicker = false }
        )
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
                .navigationBarsPadding()
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

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
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

            Spacer(modifier = Modifier.height(16.dp))
            Text("Movimientos del día", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val hasDispensaciones = uiState.dispensacionesHoy.isNotEmpty()
            val hasServicios = uiState.serviciosExtraHoy.isNotEmpty()
            if (!hasDispensaciones && !hasServicios && uiState.pagos.isEmpty()) {
                OptoCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Sin movimientos este día", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                if (hasDispensaciones) {
                    Text("Dispensaciones", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    uiState.dispensacionesHoy.forEach { disp ->
                        val label = if (disp.ot.isNotBlank()) "OT ${disp.ot}" else "Dispensación ${disp.id.take(8)}"
                        val pagosVenta = uiState.pagos.filter { it.dispensacionId == disp.id }
                        val totalPagado = pagosVenta.sumOf { it.monto }
                        val saldo = if (disp.estadoEntrega == "Anulado") 0.0 else disp.montoTotal - totalPagado

                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    if (disp.pacienteId.isNotBlank())
                                        navController.navigate("editarDispensacion/${disp.pacienteId}/${disp.id}")
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                    Text("s/. ${String.format(Locale.getDefault(), "%.2f", disp.montoTotal)}",
                                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Pagado: s/. ${String.format(Locale.getDefault(), "%.2f", totalPagado)}", fontSize = 12.sp)
                                    Text("Saldo: s/. ${String.format(Locale.getDefault(), "%.2f", saldo)}",
                                        fontSize = 12.sp, color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                                }
                                pagosVenta.forEach { pago ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("  ${pago.metodoPago}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("s/. ${String.format(Locale.getDefault(), "%.2f", pago.monto)}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (hasServicios) {
                    Text("Servicios Extra", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    uiState.serviciosExtraHoy.forEach { serv ->
                        val label = "Servicio: ${serv.descripcion.take(32)}"
                        val pagosVenta = uiState.pagos.filter { it.servicioExtraId == serv.id }
                        val totalPagado = pagosVenta.sumOf { it.monto }
                        val saldo = if (serv.estado == "Anulado") 0.0 else serv.montoTotal - totalPagado

                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    navController.navigate("editar_servicio/${serv.id}")
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                    Text("s/. ${String.format(Locale.getDefault(), "%.2f", serv.montoTotal)}",
                                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Pagado: s/. ${String.format(Locale.getDefault(), "%.2f", totalPagado)}", fontSize = 12.sp)
                                    Text("Saldo: s/. ${String.format(Locale.getDefault(), "%.2f", saldo)}",
                                        fontSize = 12.sp, color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                                }
                                pagosVenta.forEach { pago ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("  ${pago.metodoPago}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("s/. ${String.format(Locale.getDefault(), "%.2f", pago.monto)}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

        }
    }
}
