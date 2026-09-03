package com.example.optoapp.ui.components.servicio

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Montura
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Pago
import com.example.optoapp.domain.estadoAfterFechaEntrega
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.ui.components.FechaEntregaEditButton
import com.example.optoapp.ui.components.MonturaSearchField
import com.example.optoapp.domain.inventario.monturaLabel
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

    if (monturas.none { it.stockActual > 0 }) {
        Text(
            "No hay productos con stock en el inventario.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        MonturaSearchField(
            monturas = monturas,
            selectedMonturaId = uiState.monturaId,
            onMonturaSelected = { montura ->
                onUpdate(
                    uiState.copy(
                        monturaId = montura.id,
                        descripcion = monturaLabel(montura),
                        montoTotal = String.format(java.util.Locale.US, "%.2f", montura.precio),
                    ),
                )
            },
            onClear = {
                onUpdate(
                    uiState.copy(
                        monturaId = null,
                        descripcion = "",
                        montoTotal = "",
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            label = "Buscar producto (marca, modelo o SKU)",
            placeholder = "Ej: líquido, cofre, Ray-Ban...",
        )
    }

    OptoTextField(
        value = uiState.descripcion,
        onValueChange = { onUpdate(uiState.copy(descripcion = it)) },
        label = "Descripción",
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
                onUpdate(
                    uiState.copy(
                        fechaEntrega = nuevaFecha,
                        estado = estadoAfterFechaEntrega(uiState.estado, nuevaFecha),
                    ),
                )
            },
        )
    }
}
