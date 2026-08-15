package com.example.optoapp.ui.components.servicio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.Montura
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Pago
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.ui.components.FechaEntregaEditButton
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.PatientContextCard
import com.example.optoapp.ui.components.financiera.FinancieraPagosSection
import com.example.optoapp.ui.components.financiera.PagosSectionState
import com.example.optoapp.ui.components.financiera.SaldoDisplayStyle
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.ServiciosUiState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicioForm(
    uiState: ServiciosUiState,
    onUpdate: (ServiciosUiState) -> Unit,
    onUpdateEstado: (String) -> Unit,
    monturas: List<Montura>,
    pacientes: List<Paciente>,
    onAddPago: (Pago) -> Unit,
    onUpdatePago: (Pago) -> Unit,
    onRemovePago: (Pago) -> Unit,
    onShowDatePicker: () -> Unit,
    step: Int = 0,
    isPacienteLocked: Boolean = false,
) {
    when (step) {
        0 -> StepDatos(
            uiState = uiState,
            onUpdate = onUpdate,
            monturas = monturas,
            pacientes = pacientes,
            onShowDatePicker = onShowDatePicker,
            isPacienteLocked = isPacienteLocked,
        )
        1 -> StepPagos(
            uiState = uiState,
            onUpdate = onUpdate,
            onUpdateEstado = onUpdateEstado,
            onAddPago = onAddPago,
            onUpdatePago = onUpdatePago,
            onRemovePago = onRemovePago,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepDatos(
    uiState: ServiciosUiState,
    onUpdate: (ServiciosUiState) -> Unit,
    monturas: List<Montura>,
    pacientes: List<Paciente>,
    onShowDatePicker: () -> Unit,
    isPacienteLocked: Boolean,
) {
    OutlinedButton(onClick = onShowDatePicker, modifier = Modifier.fillMaxWidth()) {
        Text("Fecha: ${DateUtils.formatLocalized(uiState.fecha)}")
    }

    OptoTextField(value = uiState.ot, onValueChange = { onUpdate(uiState.copy(ot = it)) }, label = "OT (Opcional)")

    var showMonturaDialog by remember { mutableStateOf(false) }

    if (showMonturaDialog) {
        AlertDialog(
            onDismissRequest = { showMonturaDialog = false },
            title = { Text("Seleccionar Producto") },
            text = {
                Column {
                    if (monturas.isEmpty()) {
                        Text("No hay monturas con stock en el inventario.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(monturas) { montura ->
                                ListItem(
                                    headlineContent = { Text("${montura.marca} ${montura.modelo}") },
                                    supportingContent = { Text("Color: ${montura.color} | SKU: ${montura.sku}") },
                                    trailingContent = { Text("s/. ${montura.precio}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.clickable {
                                        onUpdate(
                                            uiState.copy(
                                                descripcion = "${montura.marca} ${montura.modelo} (${montura.sku})",
                                                montoTotal = montura.precio.toString(),
                                            ),
                                        )
                                        showMonturaDialog = false
                                    },
                                )
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonturaDialog = false }) { Text("Cerrar") }
            },
        )
    }

    OptoTextField(
        value = uiState.descripcion,
        onValueChange = { onUpdate(uiState.copy(descripcion = it)) },
        label = "Descripción",
        trailingIcon = {
            IconButton(onClick = { showMonturaDialog = true }) {
                Icon(Icons.Default.Inventory2, contentDescription = "Vincular Inventario", tint = MaterialTheme.colorScheme.primary)
            }
        },
    )

    OptoTextField(
        value = uiState.montoTotal,
        onValueChange = { onUpdate(uiState.copy(montoTotal = it)) },
        label = "Monto Total",
        keyboardType = KeyboardType.Decimal,
    )

    HorizontalDivider()

    if (isPacienteLocked) {
        val pacienteName = pacientes.find { it.id == uiState.pacienteId }?.nombreCompleto ?: "Paciente"
        PatientContextCard(pacienteNombre = pacienteName)
    } else {
        PacienteSelector(uiState = uiState, onUpdate = onUpdate, pacientes = pacientes)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PacienteSelector(
    uiState: ServiciosUiState,
    onUpdate: (ServiciosUiState) -> Unit,
    pacientes: List<Paciente>,
) {
    Text("Asociar a Paciente (Opcional)", fontWeight = FontWeight.Bold)
    var pExpanded by remember { mutableStateOf(false) }
    var pSearchQuery by remember { mutableStateOf("") }
    val filteredPacientes = if (pSearchQuery.isEmpty()) {
        pacientes
    } else {
        pacientes.filter { it.nombreCompleto.contains(pSearchQuery, ignoreCase = true) }
    }
    val currentPacienteName = pacientes.find { it.id == uiState.pacienteId }?.nombreCompleto ?: "Ninguno"

    ExposedDropdownMenuBox(expanded = pExpanded, onExpandedChange = { pExpanded = !pExpanded }) {
        OutlinedTextField(
            value = pSearchQuery.ifBlank { if (pExpanded) "" else currentPacienteName },
            onValueChange = { pSearchQuery = it },
            label = { Text("Buscar Paciente...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pExpanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = pExpanded, onDismissRequest = { pExpanded = false }) {
            DropdownMenuItem(text = { Text("Ninguno") }, onClick = {
                onUpdate(uiState.copy(pacienteId = null))
                pSearchQuery = ""
                pExpanded = false
            })
            filteredPacientes.forEach { p ->
                DropdownMenuItem(text = { Text(p.nombreCompleto) }, onClick = {
                    onUpdate(uiState.copy(pacienteId = p.id))
                    pSearchQuery = ""
                    pExpanded = false
                })
            }
        }
    }
}

@Composable
private fun StepPagos(
    uiState: ServiciosUiState,
    onUpdate: (ServiciosUiState) -> Unit,
    onUpdateEstado: (String) -> Unit,
    onAddPago: (Pago) -> Unit,
    onUpdatePago: (Pago) -> Unit,
    onRemovePago: (Pago) -> Unit,
) {
    FinancieraPagosSection(
        state = PagosSectionState(
            montoTotal = uiState.montoTotal.toDoubleOrNull() ?: 0.0,
            pagos = uiState.pagos,
        ),
        onAddPago = onAddPago,
        onUpdatePago = onUpdatePago,
        onRemovePago = onRemovePago,
        saldoStyle = SaldoDisplayStyle.Card,
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    OptoDropdownMenuField(
        label = "Estado",
        selected = uiState.estado,
        options = listOf("Pendiente", "Entregado"),
        onSelected = { onUpdateEstado(it) },
    )

    if (uiState.fechaEntrega != null) {
        FechaEntregaEditButton(
            fechaEntrega = uiState.fechaEntrega,
            onFechaChanged = { nuevaFecha ->
                onUpdate(uiState.copy(fechaEntrega = nuevaFecha))
            },
        )
    }
}
