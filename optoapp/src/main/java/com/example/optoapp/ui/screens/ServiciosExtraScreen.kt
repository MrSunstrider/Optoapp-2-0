package com.example.optoapp.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.R
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.components.MotivoDialog
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoKpiCard
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OrderEstadoChip
import com.example.optoapp.ui.components.orderEstadoColor
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.ui.theme.warningAmber
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.ServiciosViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiciosExtraScreen(navController: NavController, drawerState: DrawerState, viewModel: ServiciosViewModel = hiltViewModel()) {
    val servicios by viewModel.allServicios.collectAsState()
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var estadoFilter by remember { mutableStateOf("Todos") }
    val snackbarHostState = remember { SnackbarHostState() }

    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val servicioToDelete by viewModel.servicioToDelete.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()
    val anulando by viewModel.anulando.collectAsState()

    val filteredServicios = servicios.filter { servicio ->
        val matchesSearch = searchQuery.isEmpty() ||
            servicio.descripcion.contains(searchQuery, ignoreCase = true) ||
            servicio.ot.contains(searchQuery, ignoreCase = true)
        val matchesDate = selectedDate == null || servicio.fecha == selectedDate
        val matchesEstado = estadoFilter == "Todos" || servicio.estado == estadoFilter
        matchesSearch && matchesDate && matchesEstado
    }

    val aCuentaSumByServicio by viewModel.aCuentaSumByServicio.collectAsState()
    val cancelableServicioIds by viewModel.cancelableServicioIds.collectAsState()
    val totalFacturado = servicios.sumOf { it.montoTotal }
    val totalPendiente = servicios.filter { it.estado == "Pendiente" }.sumOf {
        it.montoTotal - (aCuentaSumByServicio[it.id] ?: 0.0)
    }
    val pendientesCount = servicios.count { it.estado == "Pendiente" }

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = selectedDate ?: DateUtils.today(),
            onDateSelected = { selectedDate = it },
            onDismiss = { showDatePicker = false },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    LaunchedEffect(deleteError) {
        deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteError()
        }
    }

    LaunchedEffect(infoMessage) {
        infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    servicioToDelete?.takeIf { showDeleteDialog }?.let { servicio ->
        AnularServicioDialog(
            descripcion = servicio.descripcion,
            submitting = anulando,
            onConfirm = { viewModel.confirmAnular(it) },
            onDismiss = { viewModel.dismissDeleteDialog() },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
        topBar = {
            OptoTopAppBar(
                title = stringResource(R.string.servicios_extra_title),
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.servicios_extra_menu_cd))
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = stringResource(R.string.servicios_extra_filtrar_fecha_cd),
                            tint = if (selectedDate != null) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Route.NuevoServicio.route) },
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.servicios_extra_anadir_cd))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptoKpiCard(stringResource(R.string.servicios_extra_kpi_facturado), MoneyFormatter.format(totalFacturado), MaterialTheme.colorScheme.primary, Icons.Default.Receipt, Modifier.weight(1f))
                    OptoKpiCard(stringResource(R.string.servicios_extra_kpi_pendiente), MoneyFormatter.format(totalPendiente), if (totalPendiente > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.positiveGreen, Icons.Default.Schedule, Modifier.weight(1f))
                    OptoKpiCard(stringResource(R.string.servicios_extra_kpi_cantidad), "$pendientesCount", MaterialTheme.colorScheme.warningAmber, Icons.Default.Handyman, Modifier.weight(1f))
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.servicios_extra_buscar_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.servicios_extra_buscar_cd)) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todos", "Pendiente", "Entregado", "Anulado").forEach { estado ->
                        FilterChip(
                            selected = estadoFilter == estado,
                            onClick = { estadoFilter = estado },
                            label = { Text(stringResource(estadoFilterLabel(estado)), fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (estado) {
                                    "Pendiente" -> MaterialTheme.colorScheme.alertRed.copy(alpha = 0.15f)
                                    "Entregado" -> MaterialTheme.colorScheme.positiveGreen.copy(alpha = 0.15f)
                                    "Anulado" -> orderEstadoColor(estado, null).copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                },
                            ),
                        )
                    }
                    if (selectedDate != null) {
                        FilterChip(
                            selected = true,
                            onClick = { selectedDate = null },
                            label = { Text(DateUtils.formatLocalized(selectedDate!!), fontSize = 12.sp) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.servicios_extra_quitar_filtro_cd), modifier = Modifier.size(14.dp)) },
                        )
                    }
                }
            }

            if (filteredServicios.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Handyman, contentDescription = stringResource(R.string.servicios_extra_sin_servicios_cd), modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.servicios_extra_empty_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.servicios_extra_empty_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            } else {
                items(filteredServicios) { servicio ->
                    ServicioCard(
                        servicio = servicio,
                        aCuenta = aCuentaSumByServicio[servicio.id] ?: 0.0,
                        onEdit = { navController.navigate(Route.EditarServicio(servicio.id).route) },
                        onAnular = if (servicio.id in cancelableServicioIds) {
                            { viewModel.showDeleteConfirmation(servicio) }
                        } else {
                            null
                        },
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@StringRes
private fun estadoFilterLabel(estado: String): Int = when (estado) {
    "Pendiente" -> R.string.servicios_extra_filtro_pendiente
    "Entregado" -> R.string.servicios_extra_filtro_entregado
    "Anulado" -> R.string.servicios_extra_filtro_anulado
    else -> R.string.servicios_extra_filtro_todos
}

@Composable
private fun ServicioCard(servicio: ServicioExtra, aCuenta: Double = 0.0, onEdit: () -> Unit, onAnular: (() -> Unit)?) {
    val saldo = servicio.montoTotal - aCuenta
    val estadoColor = orderEstadoColor(servicio.estado, saldo)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = estadoColor.copy(alpha = 0.04f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (servicio.ot.isNotBlank()) {
                        Text(stringResource(R.string.servicios_extra_ot, servicio.ot), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val sinDescripcion = stringResource(R.string.servicios_extra_sin_descripcion)
                    Text(
                        servicio.descripcion.ifBlank { sinDescripcion },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OrderEstadoChip(servicio.estado, saldo = saldo, servicio = true)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(stringResource(R.string.servicios_extra_total, MoneyFormatter.format(servicio.montoTotal)), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (saldo > 0) {
                        Text(stringResource(R.string.servicios_extra_saldo, MoneyFormatter.format(saldo)), fontSize = 13.sp, color = MaterialTheme.colorScheme.alertRed, fontWeight = FontWeight.Medium)
                    } else {
                        Text(stringResource(R.string.servicios_extra_pagado), fontSize = 13.sp, color = MaterialTheme.colorScheme.positiveGreen, fontWeight = FontWeight.Medium)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(DateUtils.formatLocalized(servicio.fecha), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (servicio.fechaEntrega != null) {
                        Text(stringResource(R.string.servicios_extra_entregado_fecha, DateUtils.formatLocalized(servicio.fechaEntrega)), fontSize = 10.sp, color = MaterialTheme.colorScheme.positiveGreen)
                    }
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.servicios_extra_editar_cd), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        if (onAnular != null) {
                            ServicioOverflowMenu(onAnular = onAnular)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServicioOverflowMenu(onAnular: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.servicios_extra_mas_acciones_cd), modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.servicios_extra_anular), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onAnular()
                },
            )
        }
    }
}

@Composable
internal fun AnularServicioDialog(
    descripcion: String,
    submitting: Boolean,
    onConfirm: (motivo: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val descripcionFallback = stringResource(R.string.servicios_extra_anular_dialog_fallback)
    MotivoDialog(
        title = stringResource(R.string.servicios_extra_anular_dialog_title),
        confirmText = stringResource(R.string.servicios_extra_anular),
        message = stringResource(
            R.string.servicios_extra_anular_dialog_message,
            descripcion.ifBlank { descripcionFallback },
        ),
        submitting = submitting,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
