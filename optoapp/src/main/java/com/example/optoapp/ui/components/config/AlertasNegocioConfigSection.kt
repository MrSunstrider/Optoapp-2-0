package com.example.optoapp.ui.components.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.optoapp.R
import com.example.optoapp.viewmodel.AlertasNegocioConfigUi

@Composable
fun AlertasNegocioConfigSection(
    ui: AlertasNegocioConfigUi,
    onUpdate: ((AlertasNegocioConfigUi) -> AlertasNegocioConfigUi) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(R.string.config_alertas_titulo),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.config_alertas_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        UmbralField(
            label = stringResource(R.string.config_alertas_caida_ventas),
            value = ui.caidaVentasAlertaPct,
            onChange = { v -> onUpdate { it.copy(caidaVentasAlertaPct = v) } },
        )
        UmbralField(
            label = stringResource(R.string.config_alertas_deuda_dias),
            value = ui.deudaViejaAlertaDias,
            onChange = { v -> onUpdate { it.copy(deudaViejaAlertaDias = v) } },
        )
        UmbralField(
            label = stringResource(R.string.config_alertas_deuda_monto),
            value = ui.deudaTotalAlertaMonto,
            onChange = { v -> onUpdate { it.copy(deudaTotalAlertaMonto = v) } },
        )
        UmbralField(
            label = stringResource(R.string.config_alertas_stock_dias),
            value = ui.stockEstancadoAlertaDias,
            onChange = { v -> onUpdate { it.copy(stockEstancadoAlertaDias = v) } },
        )
        UmbralField(
            label = stringResource(R.string.config_alertas_min_ventas),
            value = ui.minVentasParaRecomendar,
            onChange = { v -> onUpdate { it.copy(minVentasParaRecomendar = v) } },
        )
        UmbralField(
            label = stringResource(R.string.config_alertas_margen_bajo),
            value = ui.margenBajoAlertaPct,
            onChange = { v -> onUpdate { it.copy(margenBajoAlertaPct = v) } },
        )
        UmbralField(
            label = stringResource(R.string.config_alertas_margen_alto),
            value = ui.margenAltoAlertaPct,
            onChange = { v -> onUpdate { it.copy(margenAltoAlertaPct = v) } },
        )
        UmbralField(
            label = stringResource(R.string.config_alertas_contribucion),
            value = ui.contribucionAltaRatio,
            onChange = { v -> onUpdate { it.copy(contribucionAltaRatio = v) } },
        )
        UmbralField(
            label = stringResource(R.string.config_alertas_gasto_ratio),
            value = ui.gastoFueraRangoRatio,
            onChange = { v -> onUpdate { it.copy(gastoFueraRangoRatio = v) } },
        )
        Button(
            onClick = onSave,
            enabled = !ui.guardando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (ui.guardando) {
                    stringResource(R.string.config_alertas_guardando)
                } else {
                    stringResource(R.string.config_alertas_guardar)
                },
            )
        }
        ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun UmbralField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}
