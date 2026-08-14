package com.example.optoapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.example.optoapp.ui.components.OptoCard
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.components.cierre_caja.ResumenCard
import com.example.optoapp.ui.components.cierre_caja.TransactionItem
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.ui.theme.warningAmber
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.CierreCajaViewModel
import com.example.optoapp.viewmodel.PagoDisplayItem
import java.util.Locale

private fun formatSoles(monto: Double): String = "s/. ${String.format(Locale.getDefault(), "%,.2f", monto)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreCajaScreen(
    navController: NavController,
    viewModel: CierreCajaViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = null)
    val canView = opticaRol != null && AppRoles.canViewCierreCaja(opticaRol!!)
    var showDatePicker by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val today = remember { DateUtils.today() }
    val yesterday = remember(today) { today.minusDays(1) }

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = uiState.fecha,
            onDateSelected = { viewModel.setFecha(it) },
            onDismiss = { showDatePicker = false },
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
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .navigationBarsPadding(),
        ) {
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

            if (uiState.errorMessage != null) {
                OptoCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        uiState.errorMessage!!,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
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
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = uiState.fecha == yesterday,
                    onClick = { viewModel.setFecha(yesterday) },
                    label = { Text("Ayer", fontSize = 12.sp) },
                )
                FilterChip(
                    selected = uiState.fecha == today,
                    onClick = { viewModel.setFecha(today) },
                    label = { Text("Hoy", fontSize = 12.sp) },
                )
                FilterChip(
                    selected = showSearch || searchQuery.isNotBlank(),
                    onClick = {
                        showSearch = !showSearch
                        if (!showSearch) searchQuery = ""
                    },
                    label = { Text("Buscar", fontSize = 12.sp) },
                )
            }

            if (showSearch) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("OT, paciente, descripción, método…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                )
            }

            val filteredPagosDisplay = remember(uiState.pagosDisplay, searchQuery, uiState.pacienteNombres) {
                filterPagoDisplayItems(uiState.pagosDisplay, searchQuery, uiState.pacienteNombres)
            }
            val filteredDispensaciones = remember(uiState.dispensacionesHoy, searchQuery, uiState.pacienteNombres) {
                filterDispensaciones(uiState.dispensacionesHoy, searchQuery, uiState.pacienteNombres)
            }
            val filteredServicios = remember(uiState.serviciosExtraHoy, searchQuery, uiState.pacienteNombres) {
                filterServiciosExtra(uiState.serviciosExtraHoy, searchQuery, uiState.pacienteNombres)
            }
            val isSearchActive = searchQuery.isNotBlank()

            Spacer(modifier = Modifier.height(16.dp))

            val cobradoHoy = remember(uiState.pagos) { viewModel.getCobradoHoy() }

            OptoCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        heroCobradoLabel(uiState.fecha, today),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        formatSoles(cobradoHoy),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    MetricSubLine("Ventas registradas", uiState.totalGeneral)
                    MetricSubLine("Cobros de ventas del día", uiState.ventasHoy)
                    if (uiState.cobrosAtrasados > 0) {
                        MetricSubLine(
                            label = "Cobros atrasados",
                            amount = uiState.cobrosAtrasados,
                            valueColor = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (uiState.saldoPendiente > 0) {
                        MetricSubLine(
                            label = "Pendiente (órdenes del día)",
                            amount = uiState.saldoPendiente,
                            valueColor = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (uiState.pagosFuturos != 0.0) {
                        Text(
                            "Incluye ${formatSoles(uiState.pagosFuturos)} de pagos con fecha futura",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totales = remember(uiState.pagos) { viewModel.getTotalesPorMetodo() }
            val knownKeys = remember { setOf("Efectivo", "Transferencia", "Móvil", "Tarjeta") }
            val otros = remember(totales, knownKeys) {
                totales.filterKeys { it !in knownKeys && totales[it] != 0.0 }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResumenCard("Efectivo", totales["Efectivo"] ?: 0.0, Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                ResumenCard("Móvil/Trans", (totales["Transferencia"] ?: 0.0) + (totales["Móvil"] ?: 0.0), Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                ResumenCard("Tarjeta", totales["Tarjeta"] ?: 0.0, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            }

            if (otros.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    otros.entries.forEach { (key, monto) ->
                        val label = key.ifBlank { "Sin espec." }
                        ResumenCard(label, monto, Modifier.weight(1f), MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val hasPagos = filteredPagosDisplay.isNotEmpty()
            val hasDispensaciones = filteredDispensaciones.isNotEmpty()
            val hasServicios = filteredServicios.isNotEmpty()
            val hasVentas = hasDispensaciones || hasServicios
            val ventasCount = filteredDispensaciones.size + filteredServicios.size

            if (!uiState.isLoading && !hasPagos && !hasVentas) {
                OptoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when {
                            isSearchActive -> "Sin resultados para \"$searchQuery\""
                            else -> "Sin movimientos este día"
                        },
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                if (hasPagos || (isSearchActive && uiState.pagosDisplay.isNotEmpty())) {
                    Text(
                        "Cobros recibidos (${filteredPagosDisplay.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (hasPagos) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredPagosDisplay.forEach { item ->
                                TransactionItem(
                                    pago = item.pago,
                                    label = item.label,
                                    tipoEntidad = item.tipoEntidad,
                                    esCobroAtrasado = item.esCobroAtrasado,
                                    onClick = pagoNavigationHandler(item, navController),
                                )
                            }
                        }
                    } else {
                        OptoCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Ningún cobro coincide con la búsqueda",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (!uiState.isLoading && !isSearchActive) {
                    Text(
                        "Cobros recibidos (0)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OptoCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Sin cobros registrados",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (!uiState.isLoading) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (hasVentas || (isSearchActive && (uiState.dispensacionesHoy.isNotEmpty() || uiState.serviciosExtraHoy.isNotEmpty()))) {
                    Text(
                        "Ventas registradas ($ventasCount)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (hasDispensaciones) {
                        Text("Dispensaciones", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                        filteredDispensaciones.forEach { disp ->
                            VentaDispensacionCard(disp, navController)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    if (hasServicios) {
                        Text("Servicios Extra", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                        filteredServicios.forEach { serv ->
                            VentaServicioCard(serv, navController)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    if (!hasDispensaciones && !hasServicios && isSearchActive) {
                        OptoCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Ninguna venta coincide con la búsqueda",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else if (!uiState.isLoading && !isSearchActive) {
                    Text(
                        "Ventas registradas (0)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OptoCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Sin ventas registradas",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSubLine(
    label: String,
    amount: Double,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
        Text(formatSoles(amount), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

private fun pagoNavigationHandler(
    item: PagoDisplayItem,
    navController: NavController,
): (() -> Unit)? {
    val dispensacionId = item.dispensacionId
    val pacienteId = item.pacienteId
    if (dispensacionId != null && !pacienteId.isNullOrBlank()) {
        return {
            navController.navigate(Route.EditarDispensacion(pacienteId, dispensacionId).route)
        }
    }
    val servicioId = item.servicioExtraId
    if (servicioId != null) {
        return { navController.navigate(Route.EditarServicio(servicioId).route) }
    }
    return null
}

@Composable
private fun VentaDispensacionCard(
    disp: com.example.optoapp.data.DispensacionOptica,
    navController: NavController,
) {
    val totalPagado = disp.montoPagado
    val saldo = disp.montoTotal - totalPagado
    val estadoColor = ventaEstadoColor(disp.estadoEntrega, saldo)

    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable {
                if (disp.pacienteId.isNotBlank()) {
                    navController.navigate(Route.EditarDispensacion(disp.pacienteId, disp.id).route)
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = estadoColor.copy(alpha = 0.04f)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        dispensacionVentaTitle(disp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        dispensacionVentaSubtitle(disp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                VentaEstadoChip(disp.estadoEntrega, saldo)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total: ${formatSoles(disp.montoTotal)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pagado: ${formatSoles(totalPagado)}", fontSize = 12.sp)
                    Text(
                        if (saldo > 0) "Saldo: ${formatSoles(saldo)}" else "Pagado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (saldo > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.positiveGreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun VentaServicioCard(
    serv: com.example.optoapp.data.ServicioExtra,
    navController: NavController,
) {
    val totalPagado = serv.aCuenta
    val saldo = serv.montoTotal - totalPagado
    val estadoColor = ventaEstadoColor(serv.estado, saldo)
    val otLine = servicioVentaOtLine(serv)

    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable {
                navController.navigate(Route.EditarServicio(serv.id).route)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = estadoColor.copy(alpha = 0.04f)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (otLine != null) {
                        Text(otLine, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        serv.descripcion.ifBlank { "Sin descripción" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                VentaEstadoChip(serv.estado, saldo)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total: ${formatSoles(serv.montoTotal)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pagado: ${formatSoles(totalPagado)}", fontSize = 12.sp)
                    Text(
                        if (saldo > 0) "Saldo: ${formatSoles(saldo)}" else "Pagado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (saldo > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.positiveGreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun ventaEstadoColor(estado: String, saldo: Double): androidx.compose.ui.graphics.Color =
    when (estado) {
        "Entregado" -> MaterialTheme.colorScheme.positiveGreen
        "Pendiente" -> if (saldo > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.warningAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun VentaEstadoChip(estado: String, saldo: Double) {
    val estadoColor = ventaEstadoColor(estado, saldo)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = estadoColor.copy(alpha = 0.15f),
    ) {
        Text(
            estado,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = estadoColor,
        )
    }
}
