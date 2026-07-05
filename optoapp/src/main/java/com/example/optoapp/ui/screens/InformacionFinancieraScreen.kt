package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.Pago
import com.example.optoapp.ui.components.AbonoDialog
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.InformacionFinancieraViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformacionFinancieraScreen(
    navController: NavController,
    dispensacionId: String?,
    onComplete: () -> Unit = { navController.popBackStack() },
    viewModel: InformacionFinancieraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(dispensacionId) {
        if (!dispensacionId.isNullOrBlank()) {
            viewModel.loadFinanciera(dispensacionId)
        }
    }

    val saveAction = {
        viewModel.save { onComplete() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            OptoTopAppBar(
                title = "Información Financiera",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { saveAction() }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.contexto?.let { ctx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("OT: ${ctx.ot}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Paciente: ${ctx.pacienteNombre}", fontSize = 14.sp)
                        Text("Fecha: ${DateUtils.formatLocalized(ctx.fecha)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (ctx.descripcion.isNotBlank()) {
                            Text(ctx.descripcion, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            OptoTextField(
                value = uiState.montoTotal,
                onValueChange = { viewModel.updateMontoTotal(it) },
                label = "Monto Total",
                keyboardType = KeyboardType.Decimal
            )

            HorizontalDivider()

            Text("Historial de Abonos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            val total = uiState.montoTotal.toDoubleOrNull() ?: 0.0
            val pagado = uiState.pagos.sumOf { it.monto }
            val saldo = total - pagado

            uiState.pagos.forEach { pago ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${pago.metodoPago}: s/. ${String.format(Locale.getDefault(), "%.2f", pago.monto)}",
                                fontWeight = FontWeight.Bold
                            )
                            if (pago.nota.isNotEmpty()) {
                                Text(pago.nota, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                DateUtils.formatLocalized(pago.fecha),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            var showEditDialog by remember { mutableStateOf(false) }
                            if (showEditDialog) {
                                val totalActual = uiState.montoTotal.toDoubleOrNull() ?: 0.0
                                val otrosAbonos = uiState.pagos
                                    .filter { it.id != pago.id }
                                    .sumOf { it.monto }
                                val maximo = (totalActual - otrosAbonos).coerceAtLeast(0.0)
                                AbonoDialog(
                                    pago = pago,
                                    montoMaximo = maximo,
                                    onDismiss = { showEditDialog = false },
                                    onConfirm = { updatedPago: Pago ->
                                        viewModel.updatePago(updatedPago)
                                        showEditDialog = false
                                    }
                                )
                            }
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.removePago(pago) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            var showAddDialog by remember { mutableStateOf(false) }
            if (showAddDialog) {
                val maximo = (total - pagado).coerceAtLeast(0.0)
                AbonoDialog(
                    defaultFecha = DateUtils.today(),
                    montoMaximo = maximo,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { nuevoPago: Pago ->
                        viewModel.addPago(nuevoPago)
                        showAddDialog = false
                    }
                )
            }

            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar Abono")
            }

            HorizontalDivider()

            // Saldo restante se calcula en el ViewModel para mantener una unica fuente de verdad
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "SALDO RESTANTE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val formattedSaldo = String.format(Locale.getDefault(), "%.2f", saldo)
                Text(
                    text = "s/. $formattedSaldo",
                    color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            DropdownField(
                label = "Estado de Entrega",
                selected = uiState.estadoEntrega,
                options = listOf("Pendiente", "Entregado")
            ) { newEstado ->
                viewModel.updateEstado(newEstado)
            }

            if (uiState.estadoEntrega == "Entregado" && uiState.fechaEntrega != null) {
                val fecha = uiState.fechaEntrega
                if (fecha != null) {
                    Text(
                        text = "Entregado el día ${DateUtils.formatLocalized(fecha)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = { saveAction() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Cambios")
            }

            if (!uiState.error.isNullOrBlank()) {
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
