package com.example.optoapp.ui.screens.costos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoDialog
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.VersionPrecio
import java.time.LocalDate

@Composable
fun EditarPrecioDialog(
    descripcion: String,
    etiquetaCosto: String,
    costo: String,
    onCostoChange: (String) -> Unit,
    vigenteDesde: LocalDate?,
    onVigenteDesdeChange: (LocalDate) -> Unit,
    vigenteDesdeError: String?,
    error: String?,
    historial: List<VersionPrecio>,
    onGuardar: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    if (showPicker && vigenteDesde != null) {
        OptoDatePickerDialog(
            initialDate = vigenteDesde,
            onDateSelected = onVigenteDesdeChange,
            onDismiss = { showPicker = false },
        )
    }
    OptoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.costos_editar_titulo),
        confirmText = stringResource(R.string.common_save),
        onConfirm = onGuardar,
        dismissText = stringResource(R.string.common_cancel),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(descripcion, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = costo,
                onValueChange = onCostoChange,
                label = { Text(etiquetaCosto) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            Text(stringResource(R.string.costos_vigente_desde), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(vigenteDesde?.let { DateUtils.formatLocalized(it) }.orEmpty())
            }
            Text(
                vigenteDesdeError ?: stringResource(R.string.costos_vigente_desde_ayuda),
                color = if (vigenteDesdeError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            if (historial.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.costos_historial_titulo), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                historial.forEach { HistorialPrecioRow(it) }
            }
        }
    }
}

@Composable
private fun HistorialPrecioRow(version: VersionPrecio) {
    val monto = MoneyFormatter.format(version.costo)
    val desde = DateUtils.formatLocalized(LocalDate.parse(version.vigenteDesde))
    val texto = version.vigenteHasta?.let { hasta ->
        stringResource(R.string.costos_historial_cerrado, monto, desde, DateUtils.formatLocalized(LocalDate.parse(hasta)))
    } ?: stringResource(R.string.costos_historial_vigente, monto, desde)
    Text(texto, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
