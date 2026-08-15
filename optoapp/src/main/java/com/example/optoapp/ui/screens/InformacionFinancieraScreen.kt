package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.ui.components.FechaEntregaEditButton
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.ui.components.OptoFormShell
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.PatientContextCard
import com.example.optoapp.ui.components.financiera.FinancieraPagosSection
import com.example.optoapp.ui.components.financiera.PagosSectionState
import com.example.optoapp.viewmodel.InformacionFinancieraViewModel

@Composable
fun InformacionFinancieraScreen(
    navController: NavController,
    dispensacionId: String?,
    onComplete: () -> Unit = { navController.popBackStack() },
    viewModel: InformacionFinancieraViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val monturas by viewModel.monturas.collectAsState()

    LaunchedEffect(dispensacionId) {
        if (!dispensacionId.isNullOrBlank()) {
            viewModel.loadFinanciera(dispensacionId)
            viewModel.loadMonturas()
        }
    }

    val saveAction = { viewModel.save { onComplete() } }

    OptoFormShell(
        title = "Información Financiera",
        onNavigateBack = { navController.popBackStack() },
        onSave = saveAction,
        isLoading = uiState.isLoading,
    ) {
        uiState.contexto?.let { ctx ->
            PatientContextCard(
                pacienteNombre = ctx.pacienteNombre,
                ot = ctx.ot,
                fecha = ctx.fecha,
                descripcion = ctx.descripcion,
            )
        }

        OptoTextField(
            value = uiState.montoTotal,
            onValueChange = { viewModel.updateMontoTotal(it) },
            label = "Monto Total",
            keyboardType = KeyboardType.Decimal,
        )

        HorizontalDivider()

        FinancieraPagosSection(
            state = PagosSectionState(
                montoTotal = uiState.montoTotal.toDoubleOrNull() ?: 0.0,
                pagos = uiState.pagos,
            ),
            onAddPago = { viewModel.addPago(it) },
            onUpdatePago = { viewModel.updatePago(it) },
            onRemovePago = { viewModel.removePago(it) },
        )

        HorizontalDivider()

        RegalosSection(
            regalos = uiState.regalos,
            monturas = monturas,
            onAddRegalo = { viewModel.addRegalo(it) },
            onRemoveRegalo = { viewModel.removeRegalo(it) },
        )

        OptoDropdownMenuField(
            label = "Estado de Entrega",
            selected = uiState.estadoEntrega,
            options = listOf("Pendiente", "Entregado"),
            onSelected = { viewModel.updateEstado(it) },
        )

        if (uiState.fechaEntrega != null) {
            FechaEntregaEditButton(
                fechaEntrega = uiState.fechaEntrega,
                onFechaChanged = { viewModel.updateFechaEntrega(it) },
            )
        }

        Button(
            onClick = { saveAction() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Guardar Cambios")
        }

        if (!uiState.error.isNullOrBlank()) {
            Text(
                text = uiState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
