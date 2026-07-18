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
import java.util.Locale

private fun formatSoles(monto: Double): String =
    "s/. ${String.format(Locale.getDefault(), "%,.2f", monto)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreCajaScreen(
    navController: NavController,
    viewModel: CierreCajaViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = null)
    val canView = opticaRol != null && AppRoles.canViewCierreCaja(opticaRol!!)
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
            // Wait for role resolution before rendering
            if (opticaRol == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                return@Column
            }

            if (!canView) {
                OptoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Acceso restringido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Tu rol actual no tiene permiso para consultar cierre de caja.")
                    }
                }
                return@Column
            }

            // Error state
            if (uiState.errorMessage != null) {
                OptoCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        uiState.errorMessage!!,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                    Text("TOTAL VENTAS DEL DÍA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        formatSoles(uiState.totalGeneral),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (uiState.saldoPendiente > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Saldo pendiente: ${formatSoles(uiState.saldoPendiente)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totales = remember(uiState.pagos) { viewModel.getTotalesPorMetodo() }
            val totalGeneral = remember(totales) { totales.values.sum() }
            val knownKeys = remember { setOf("Efectivo", "Transferencia", "Móvil", "Tarjeta") }
            val otros = remember(totales, knownKeys) {
                totales.filterKeys { it !in knownKeys && totales[it] != 0.0 }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResumenCard("Efectivo", totales["Efectivo"] ?: 0.0, Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                ResumenCard("Móvil/Trans", (totales["Transferencia"] ?: 0.0) + (totales["Móvil"] ?: 0.0), Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                ResumenCard("Tarjeta", totales["Tarjeta"] ?: 0.0, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            }

            // Fallback cards for unnamed/other payment methods
            if (otros.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    otros.entries.forEach { (key, monto) ->
                        val label = key.ifBlank { "Sin espec." }
                        ResumenCard(label, monto, Modifier.weight(1f), MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OptoCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL RECAUDADO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        formatSoles(totalGeneral),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (uiState.pagosFuturos != 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Incluye ${formatSoles(uiState.pagosFuturos)} de pagos con fecha futura",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Movimientos del día", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val hasDispensaciones = uiState.dispensacionesHoy.isNotEmpty()
            val hasServicios = uiState.serviciosExtraHoy.isNotEmpty()
            if (!uiState.isLoading && !hasDispensaciones && !hasServicios && uiState.pagos.isEmpty()) {
                OptoCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Sin movimientos este día", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                if (hasDispensaciones) {
                    Text("Dispensaciones", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    uiState.dispensacionesHoy.forEach { disp ->
                        val label = if (disp.ot.isNotBlank()) "OT ${disp.ot}" else "Dispensación ${disp.id.take(8)}"
                        val totalPagado = disp.montoPagado
                        val saldo = disp.montoTotal - totalPagado

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
                                    Text(formatSoles(disp.montoTotal),
                                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Pagado: ${formatSoles(totalPagado)}", fontSize = 12.sp)
                                    Text("Saldo: ${formatSoles(saldo)}",
                                        fontSize = 12.sp, color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
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
                        val totalPagado = serv.aCuenta
                        val saldo = serv.montoTotal - totalPagado

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
                                    Text(formatSoles(serv.montoTotal),
                                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Pagado: ${formatSoles(totalPagado)}", fontSize = 12.sp)
                                    Text("Saldo: ${formatSoles(saldo)}",
                                        fontSize = 12.sp, color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
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
