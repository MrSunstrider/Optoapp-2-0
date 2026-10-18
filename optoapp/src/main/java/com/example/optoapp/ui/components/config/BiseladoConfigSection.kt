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
import androidx.compose.ui.unit.dp
import com.example.optoapp.domain.OrigenBiselado
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.viewmodel.FinanzasBiseladoConfigUi

@Composable
fun BiseladoConfigSection(
    ui: FinanzasBiseladoConfigUi,
    onOrigenChange: (String) -> Unit,
    onCostoPropioChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Biselado (costos)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Propio usa la tasa interna (0 = no descuenta). Tercero usa la matriz y genera egreso vinculado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OptoDropdownMenuField(
            label = "Origen por defecto",
            selected = OrigenBiselado.normalize(ui.origenDefault),
            options = OrigenBiselado.OPTIONS,
        ) { onOrigenChange(it) }
        OutlinedTextField(
            value = ui.costoPropioDraft,
            onValueChange = onCostoPropioChange,
            label = { Text("Costo propio (par)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(
            onClick = onSave,
            enabled = !ui.guardando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (ui.guardando) "Guardando…" else "Guardar biselado")
        }
        ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
