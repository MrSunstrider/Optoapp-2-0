package com.example.optoapp.ui.components.servicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.optoapp.R
import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import com.example.optoapp.domain.CostoServicioDraft
import com.example.optoapp.domain.CostoServicioResolver
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.ui.components.OptoSegmentedSelector
import com.example.optoapp.ui.components.OptoTextField

private val EJECUTORES = listOf(CostoServicioResolver.PROPIO, CostoServicioResolver.TERCERO)

internal fun indiceEjecutor(realizadoPor: String?): Int = EJECUTORES.indexOf(realizadoPor)

internal fun ejecutorEnIndice(indice: Int): String = EJECUTORES[indice]

internal fun muestraNombreTercero(costo: CostoServicioDraft): Boolean =
    costo.realizadoPor == CostoServicioResolver.TERCERO

// A deactivated type is no longer offered, but a servicio saved with it must still show and keep it.
internal fun opcionesTipo(costo: CostoServicioDraft, tipos: List<ServicioTipoEntity>): List<ServicioTipoEntity> =
    (listOfNotNull(costo.tipo) + tipos).distinctBy { it.id }

@Composable
fun ServicioEjecutorSection(
    costo: CostoServicioDraft,
    tipos: List<ServicioTipoEntity>,
    habilitado: Boolean,
    onTipo: (ServicioTipoEntity?) -> Unit,
    onRealizadoPor: (String) -> Unit,
    onNombre: (String) -> Unit,
    onCosto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val opciones = opcionesTipo(costo, tipos)
    val sinTipo = stringResource(R.string.servicio_sin_tipo)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.servicio_realizado_por), fontWeight = FontWeight.Bold)
        OptoSegmentedSelector(
            options = listOf(stringResource(R.string.servicio_realizado_yo), stringResource(R.string.servicio_realizado_otro)),
            selectedIndex = indiceEjecutor(costo.realizadoPor),
            onSelect = { if (habilitado) onRealizadoPor(ejecutorEnIndice(it)) },
        )
        OptoDropdownMenuField(
            label = stringResource(R.string.servicio_tipo),
            selected = costo.tipo?.nombre ?: sinTipo,
            options = listOf(sinTipo) + opciones.map { it.nombre },
            enabled = habilitado,
            onSelected = { nombre -> onTipo(opciones.firstOrNull { it.nombre == nombre }) },
        )
        if (muestraNombreTercero(costo)) {
            OptoTextField(
                value = costo.realizadoPorNombre,
                onValueChange = onNombre,
                label = stringResource(R.string.servicio_realizado_nombre),
                enabled = habilitado,
            )
        }
        OptoTextField(
            value = costo.costoDraft,
            onValueChange = onCosto,
            label = stringResource(R.string.servicio_costo),
            keyboardType = KeyboardType.Decimal,
            enabled = habilitado,
        )
    }
}
