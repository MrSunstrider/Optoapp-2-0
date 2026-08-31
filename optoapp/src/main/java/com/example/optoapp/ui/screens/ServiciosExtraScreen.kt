package com.example.optoapp.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoKpiCard
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.ui.theme.LocalOptoDensity
import com.example.optoapp.ui.theme.warningAmber
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.ServiciosViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*

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
    val density = LocalOptoDensity.current

    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val servicioToDelete by viewModel.servicioToDelete.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()

    val filteredServicios = servicios.filter { servicio ->
        val matchesSearch = searchQuery.isEmpty() ||
            servicio.descripcion.contains(searchQuery, ignoreCase = true) ||
            servicio.ot.contains(searchQuery, ignoreCase = true)
        val matchesDate = selectedDate == null || servicio.fecha == selectedDate
        val matchesEstado = estadoFilter == "Todos" || servicio.estado == estadoFilter
        matchesSearch && matchesDate && matchesEstado
    }

    val aCuentaSumByServicio by viewModel.aCuentaSumByServicio.collectAsState()
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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        )
    }

    LaunchedEffect(deleteError) {
        deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteError()
        }
    }

    if (showDeleteDialog && servicioToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("¿Eliminar servicio?", fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar ${servicioToDelete!!.descripcion}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.dismissDeleteDialog() }) { Text("Cancelar") } },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
        topBar = {
            OptoTopAppBar(
                title = "Servicios Extra",
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Filtrar por fecha",
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
                Icon(Icons.Default.Add, contentDescription = "Añadir Servicio")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = density.screenPadding, vertical = density.sectionGap),
            verticalArrangement = Arrangement.spacedBy(density.sectionGap),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptoKpiCard("Facturado", "s/. ${fmt(totalFacturado)}", MaterialTheme.colorScheme.primary, Icons.Default.Receipt, Modifier.weight(1f))
                    OptoKpiCard("Pendiente", "s/. ${fmt(totalPendiente)}", if (totalPendiente > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.positiveGreen, Icons.Default.Schedule, Modifier.weight(1f))
                    OptoKpiCard("Cantidad", "$pendientesCount", MaterialTheme.colorScheme.warningAmber, Icons.Default.Handyman, Modifier.weight(1f))
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por OT o descripción...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todos", "Pendiente", "Entregado").forEach { estado ->
                        FilterChip(
                            selected = estadoFilter == estado,
                            onClick = { estadoFilter = estado },
                            label = { Text(estado, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (estado) {
                                    "Pendiente" -> MaterialTheme.colorScheme.alertRed.copy(alpha = 0.15f)
                                    "Entregado" -> MaterialTheme.colorScheme.positiveGreen.copy(alpha = 0.15f)
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
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Quitar filtro", modifier = Modifier.size(14.dp)) },
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
                        Box(modifier = Modifier.fillMaxWidth().padding(density.emptyStatePadding), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Handyman, contentDescription = "Sin servicios", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Spacer(Modifier.height(12.dp))
                                Text("No hay servicios", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text("Toca + para agregar un servicio extra", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
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
                        onDelete = { viewModel.showDeleteConfirmation(servicio) },
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ServicioCard(servicio: ServicioExtra, aCuenta: Double = 0.0, onEdit: () -> Unit, onDelete: () -> Unit) {
    val density = LocalOptoDensity.current
    val saldo = servicio.montoTotal - aCuenta
    val estadoColor = when (servicio.estado) {
        "Entregado" -> MaterialTheme.colorScheme.positiveGreen
        "Pendiente" -> if (saldo > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.warningAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = estadoColor.copy(alpha = 0.04f)),
    ) {
        Column(modifier = Modifier.padding(density.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (servicio.ot.isNotBlank()) {
                        Text("OT ${servicio.ot}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        servicio.descripcion.ifBlank { "Sin descripción" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = estadoColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        servicio.estado,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = estadoColor,
                    )
                }
            }

            Spacer(Modifier.height(density.blockGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text("Total: s/. ${fmt(servicio.montoTotal)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (saldo > 0) {
                        Text("Saldo: s/. ${fmt(saldo)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.alertRed, fontWeight = FontWeight.Medium)
                    } else {
                        Text("Pagado", fontSize = 13.sp, color = MaterialTheme.colorScheme.positiveGreen, fontWeight = FontWeight.Medium)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(DateUtils.formatLocalized(servicio.fecha), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (servicio.fechaEntrega != null) {
                        Text("Entregado: ${DateUtils.formatLocalized(servicio.fechaEntrega)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.positiveGreen)
                    }
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.alertRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun fmt(value: Double): String = if (value == value.toLong().toDouble()) {
    String.format(Locale.getDefault(), "%,.0f", value)
} else {
    String.format(Locale.getDefault(), "%,.2f", value)
}
