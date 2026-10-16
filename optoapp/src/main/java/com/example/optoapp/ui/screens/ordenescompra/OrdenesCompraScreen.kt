package com.example.optoapp.ui.screens.ordenescompra

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.OrdenCompra
import com.example.optoapp.data.OrdenCompraItem
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OrdenesCompraViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdenesCompraScreen(
    navController: NavController,
    viewModel: OrdenesCompraViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val ordenesCompra by viewModel.ordenesCompra.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val canEdit = AppRoles.canEditInventory(opticaRol)

    val filtradas = ordenesCompra.filter {
        if (uiState.query.isBlank()) {
            true
        } else {
            it.numero.contains(uiState.query, ignoreCase = true) ||
                it.proveedorId.contains(uiState.query, ignoreCase = true)
        }
    }

    if (uiState.editing) {
        OrdenCompraEditDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.cancelEdit() },
        )
    }

    if (uiState.viewingId != null) {
        val oc = ordenesCompra.find { it.id == uiState.viewingId }
        if (oc != null) {
            OrdenCompraDetailDialog(
                oc = oc,
                canEdit = canEdit,
                receiving = uiState.receiving,
                viewModel = viewModel,
                onDismiss = { viewModel.closeDetail() },
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OptoTopAppBar(
                title = "Pedidos a proveedor",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (canEdit) {
                        IconButton(onClick = { viewModel.startCreate() }) {
                            Icon(Icons.Default.Add, contentDescription = "Nueva OC")
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
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OptoTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = "Buscar por número o proveedor",
            )

            if (!uiState.error.isNullOrBlank()) {
                Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
            }
            if (!uiState.success.isNullOrBlank()) {
                Text(uiState.success ?: "", color = MaterialTheme.colorScheme.tertiary)
            }

            if (filtradas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No hay órdenes de compra registradas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtradas, key = { it.id }) { oc ->
                        OrdenCompraCard(
                            oc = oc,
                            canEdit = canEdit,
                            onView = { viewModel.viewDetail(it.id) },
                            onEdit = { viewModel.startEdit(it) },
                            onCancel = { viewModel.delete(it.id) },
                            onApprove = { viewModel.updateEstado(it.id, "APROBADA") },
                            onReceive = { viewModel.viewDetail(it.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdenCompraCard(
    oc: OrdenCompra,
    canEdit: Boolean,
    onView: (OrdenCompra) -> Unit,
    onEdit: (OrdenCompra) -> Unit,
    onCancel: (OrdenCompra) -> Unit,
    onApprove: (OrdenCompra) -> Unit,
    onReceive: (OrdenCompra) -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val statusColor = when (oc.estado) {
        "PENDIENTE" -> MaterialTheme.colorScheme.tertiary
        "APROBADA" -> MaterialTheme.colorScheme.primary
        "PARCIAL" -> MaterialTheme.colorScheme.secondary
        "COMPLETADA" -> MaterialTheme.colorScheme.error
        "CANCELADA" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView(oc) },
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = oc.numero,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = oc.estado,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                    )
                }
            }
            Text(
                "Fecha: ${oc.fecha.format(dateFormatter)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Total: S/. ${String.format("%.2f", oc.total)}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (canEdit) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onView(oc) }) { Text("Ver") }
                    if (oc.estado == "PENDIENTE") {
                        TextButton(onClick = { onEdit(oc) }) { Text("Editar") }
                        TextButton(onClick = { onApprove(oc) }) { Text("Aprobar") }
                        TextButton(
                            onClick = { onCancel(oc) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) { Text("Cancelar") }
                    }
                    if (oc.estado == "APROBADA" || oc.estado == "PARCIAL") {
                        TextButton(onClick = { onReceive(oc) }) { Text("Recibir") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrdenCompraDetailDialog(
    oc: OrdenCompra,
    canEdit: Boolean,
    receiving: Boolean,
    viewModel: OrdenesCompraViewModel,
    onDismiss: () -> Unit,
) {
    var items by remember { mutableStateOf<List<OrdenCompraItem>>(emptyList()) }
    var itemsLoaded by remember { mutableStateOf(false) }
    var receivedTextById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val canReceive = oc.estado == "APROBADA" || oc.estado == "PARCIAL"
    val canEditReceive = canEdit && canReceive
    val receiveInputsEnabled = canEditReceive && !receiving

    LaunchedEffect(oc.id) {
        itemsLoaded = false
        val loaded = viewModel.loadItems(oc.id)
        items = loaded
        receivedTextById = loaded.associate { it.id to it.recibido.toString() }
        itemsLoaded = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(oc.numero, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Estado: ${oc.estado}")
                Text("Total: S/. ${String.format("%.2f", oc.total)}")
                val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                Text("Fecha: ${oc.fecha.format(dateFormatter)}")

                if (!itemsLoaded) {
                    Text(
                        "Cargando ítems…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (items.isEmpty()) {
                    Text(
                        "Esta orden no tiene ítems.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Ítems",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    items.forEach { item ->
                        OrdenCompraReceiveItemRow(
                            item = item,
                            receivedText = receivedTextById[item.id].orEmpty(),
                            canEditReceive = canEditReceive,
                            editorsEnabled = receiveInputsEnabled,
                            onReceivedTextChange = { value ->
                                receivedTextById = receivedTextById + (item.id to value)
                            },
                        )
                    }
                }

                if (canEditReceive) {
                    Text(
                        "El stock y el costo se actualizan solo cuando la orden queda completamente recibida.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = {
                            val filled = ReceiveQuantities.fillPending(
                                items.map { ReceiveQuantities.Line(it.id, it.cantidad) },
                            )
                            receivedTextById = filled.mapValues { it.value.toString() }
                        },
                        enabled = items.isNotEmpty() && receiveInputsEnabled,
                    ) {
                        Text("Recibir todo lo pendiente")
                    }
                }
            }
        },
        confirmButton = {
            if (canEditReceive) {
                TextButton(
                    onClick = {
                        val cantidadById = items.associate { it.id to it.cantidad }
                        val minRecibidoById = items.associate { it.id to it.recibido }
                        val absolute = ReceiveQuantities.toAbsoluteMap(
                            receivedTextById,
                            cantidadById,
                            minRecibidoById,
                        )
                        viewModel.receiveItems(oc.id, absolute)
                    },
                    enabled = items.isNotEmpty() && receiveInputsEnabled,
                ) {
                    Text("Confirmar recepción")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

@Composable
private fun OrdenCompraReceiveItemRow(
    item: OrdenCompraItem,
    receivedText: String,
    canEditReceive: Boolean,
    editorsEnabled: Boolean,
    onReceivedTextChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = item.monturaId,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Pedido: ${item.cantidad} · Ya recibido: ${item.recibido}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (canEditReceive) {
            OptoTextField(
                value = receivedText,
                onValueChange = onReceivedTextChange,
                label = "Recibido total",
                keyboardType = KeyboardType.Number,
                enabled = editorsEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
