package com.example.optoapp.ui.components.servicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Montura
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Pago
import com.example.optoapp.domain.estadoAfterFechaEntrega
import com.example.optoapp.domain.inventario.monturaLabel
import com.example.optoapp.ui.components.FechaEntregaEditButton
import com.example.optoapp.ui.components.MonturaSearchField
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.PatientContextCard
import com.example.optoapp.ui.components.financiera.FinancieraPagosSection
import com.example.optoapp.ui.components.financiera.PagosSectionState
import com.example.optoapp.ui.components.financiera.SaldoDisplayStyle
import com.example.optoapp.ui.screens.RegalosSection
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.MontoDraftFormatting
import com.example.optoapp.viewmodel.RegaloDispensacionUi
import com.example.optoapp.viewmodel.ServicioExtraItemUi
import com.example.optoapp.viewmodel.ServiciosUiState

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
    onAddItem: () -> Unit,
    onUpdateItem: (Int, ServicioExtraItemUi) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onAddRegalo: (RegaloDispensacionUi) -> Unit,
    onRemoveRegalo: (Int) -> Unit,
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
            onAddItem = onAddItem,
            onUpdateItem = onUpdateItem,
            onRemoveItem = onRemoveItem,
            onShowDatePicker = onShowDatePicker,
            isPacienteLocked = isPacienteLocked,
        )
        1 -> StepPagos(
            uiState = uiState,
            onUpdate = onUpdate,
            onUpdateEstado = onUpdateEstado,
            monturas = monturas,
            onAddPago = onAddPago,
            onUpdatePago = onUpdatePago,
            onRemovePago = onRemovePago,
            onAddRegalo = onAddRegalo,
            onRemoveRegalo = onRemoveRegalo,
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
    onAddItem: () -> Unit,
    onUpdateItem: (Int, ServicioExtraItemUi) -> Unit,
    onRemoveItem: (Int) -> Unit,
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
    }

    uiState.items.forEachIndexed { index, item ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Producto ${index + 1}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (uiState.items.size > 1) {
                        IconButton(onClick = { onRemoveItem(index) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Quitar producto",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                MonturaSearchField(
                    monturas = monturas,
                    selectedMonturaId = item.monturaId,
                    onMonturaSelected = { montura ->
                        onUpdateItem(
                            index,
                            item.copy(
                                monturaId = montura.id,
                                descripcion = monturaLabel(montura),
                                montoDraft = MontoDraftFormatting.formatDraftFromAutofill(montura.precio),
                            ),
                        )
                    },
                    onClear = {
                        onUpdateItem(
                            index,
                            item.copy(monturaId = null, descripcion = "", montoDraft = ""),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Buscar producto (marca, modelo o SKU)",
                    placeholder = "Ej: líquido, cofre, Ray-Ban...",
                )

                OptoTextField(
                    value = item.descripcion,
                    onValueChange = { onUpdateItem(index, item.copy(descripcion = it)) },
                    label = "Descripción",
                )

                OptoTextField(
                    value = item.montoDraft,
                    onValueChange = { onUpdateItem(index, item.copy(montoDraft = it)) },
                    label = "Monto",
                    keyboardType = KeyboardType.Decimal,
                )
            }
        }
    }

    OutlinedButton(
        onClick = onAddItem,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Agregar otro producto")
    }

    if (uiState.montoTotal.isNotBlank()) {
        Text(
            "Total: S/. ${uiState.montoTotal}",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }

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
    monturas: List<Montura>,
    onAddPago: (Pago) -> Unit,
    onUpdatePago: (Pago) -> Unit,
    onRemovePago: (Pago) -> Unit,
    onAddRegalo: (RegaloDispensacionUi) -> Unit,
    onRemoveRegalo: (Int) -> Unit,
) {
    val montoTotal = uiState.items.mapNotNull { MontoDraftFormatting.parseDraft(it.montoDraft) }.sum()

    FinancieraPagosSection(
        state = PagosSectionState(
            montoTotal = montoTotal,
            pagos = uiState.pagos,
        ),
        onAddPago = onAddPago,
        onUpdatePago = onUpdatePago,
        onRemovePago = onRemovePago,
        saldoStyle = SaldoDisplayStyle.Card,
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    RegalosSection(
        regalos = uiState.regalos,
        monturas = monturas,
        onAddRegalo = onAddRegalo,
        onRemoveRegalo = onRemoveRegalo,
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
