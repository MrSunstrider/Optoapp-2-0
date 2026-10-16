package com.example.optoapp.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.domain.GastoTipoResolver
import com.example.optoapp.ui.components.OptoDialog
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.ui.components.OptoSegmentedSelector
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.util.DateUtils
import java.time.LocalDate

private val TIPOS_GASTO = listOf(GastoTipoResolver.FIJO, GastoTipoResolver.VARIABLE)

internal fun indiceTipoGasto(tipoGasto: String): Int = TIPOS_GASTO.indexOf(tipoGasto)

internal fun tipoGastoEnIndice(indice: Int): String = TIPOS_GASTO[indice]

data class GastoFormValues(
    val editando: Boolean,
    val categoria: String,
    val tipoGasto: String,
    val monto: String,
    val descripcion: String,
    val fecha: LocalDate,
    val nota: String,
    val isRecurring: Boolean,
    val error: String?,
)

@Composable
fun GastoFormDialog(
    values: GastoFormValues,
    categorias: List<String>,
    onCategoria: (String) -> Unit,
    onTipoGasto: (String) -> Unit,
    onMonto: (String) -> Unit,
    onDescripcion: (String) -> Unit,
    onElegirFecha: () -> Unit,
    onNota: (String) -> Unit,
    onToggleRecurrente: () -> Unit,
    onGuardar: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tituloRes = if (values.editando) R.string.analisis_editar_gasto else R.string.analisis_nuevo_gasto
    OptoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(tituloRes),
        confirmText = stringResource(R.string.analisis_guardar),
        onConfirm = onGuardar,
        dismissText = stringResource(R.string.analisis_cancelar),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OptoDropdownMenuField(
                label = stringResource(R.string.analisis_categoria),
                selected = values.categoria,
                options = categorias,
                onSelected = onCategoria,
            )
            Text(stringResource(R.string.gastos_tipo), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            OptoSegmentedSelector(
                options = listOf(stringResource(R.string.gastos_tipo_fijo), stringResource(R.string.gastos_tipo_variable)),
                selectedIndex = indiceTipoGasto(values.tipoGasto),
                onSelect = { onTipoGasto(tipoGastoEnIndice(it)) },
            )
            OptoTextField(
                value = values.monto,
                onValueChange = onMonto,
                label = stringResource(R.string.analisis_monto),
                keyboardType = KeyboardType.Decimal,
            )
            OptoTextField(
                value = values.descripcion,
                onValueChange = onDescripcion,
                label = stringResource(R.string.analisis_descripcion),
            )
            OutlinedButton(onClick = onElegirFecha, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(DateUtils.formatLocalized(values.fecha))
            }
            OptoTextField(
                value = values.nota,
                onValueChange = onNota,
                label = stringResource(R.string.gastos_nota),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = values.isRecurring, onCheckedChange = { onToggleRecurrente() })
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.analisis_gasto_recurrente), fontSize = 13.sp)
            }
            values.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
    }
}
