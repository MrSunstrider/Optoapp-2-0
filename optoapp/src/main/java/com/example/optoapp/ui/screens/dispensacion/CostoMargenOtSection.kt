package com.example.optoapp.ui.screens.dispensacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.optoapp.R
import com.example.optoapp.domain.ClaseCobertura
import com.example.optoapp.domain.CoberturaCostoClassifier
import com.example.optoapp.domain.ComponenteCosto
import com.example.optoapp.domain.CostoOrigenResolver
import com.example.optoapp.domain.OrigenComponente
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.DispensacionItemUi
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

internal const val PASO_ORDEN = 0
internal const val PASO_PRODUCTOS = 1
internal const val PASO_GESTION = 2
internal const val PASO_COSTOS = 3

internal fun pasoInicialDispensacion(seccion: String?): Int =
    if (seccion == Route.EditarDispensacion.SECCION_COSTO) PASO_COSTOS else PASO_ORDEN

data class FilaCostoOt(
    val componente: ComponenteCosto,
    val valor: Double,
    val origen: String?,
    val editado: Boolean,
)

data class ItemCostoOt(
    val filas: List<FilaCostoOt>,
    val origenGuardado: String?,
    val incompleto: Boolean,
    val ojosLente: String = com.example.optoapp.domain.OjosLente.AMBOS,
)

data class CostoMargenOtModel(
    val items: List<ItemCostoOt>,
    val costoRegalos: Double,
    val costoTotal: Double,
    val margen: Double,
    val margenPct: Double?,
) {
    val incompleto: Boolean get() = items.any { it.incompleto }
    val costoTotalTexto: String get() = MoneyFormatter.format(costoTotal)
    val margenTexto: String get() = MoneyFormatter.format(margen)
    val margenPctTexto: String
        get() = margenPct?.let { BigDecimal.valueOf(it).setScale(1, RoundingMode.HALF_UP).toPlainString() + " %" }
            ?: MoneyFormatter.NOT_AVAILABLE

    companion object {

        // Limited to single-origin values because mixto or faltante cannot tell which component they describe.
        private val ORIGENES_UNIFORMES = setOf(CostoOrigenResolver.MANUAL, CostoOrigenResolver.MATRIZ)

        fun from(
            montoTotal: Double?,
            items: List<DispensacionItemUi>,
            costoRegalos: Double = 0.0,
        ): CostoMargenOtModel {
            val itemsOt = items.map(::itemOt)
            val costoItems = itemsOt.sumOf { item -> item.filas.sumOf { it.valor } }
            val regalos = costoRegalos.coerceAtLeast(0.0)
            val costoTotal = costoItems + regalos
            val total = montoTotal ?: 0.0
            val margen = total - costoTotal
            return CostoMargenOtModel(
                items = itemsOt,
                costoRegalos = regalos,
                costoTotal = costoTotal,
                margen = margen,
                margenPct = if (total > 0.0) margen / total * 100 else null,
            )
        }

        private fun itemOt(item: DispensacionItemUi): ItemCostoOt {
            val filas = ComponenteCosto.entries.mapNotNull { fila(item, it) }
            val incompleto = if (item.costoOrigenes.isNotEmpty()) {
                filas.any { it.origen == CostoOrigenResolver.FALTANTE }
            } else {
                CoberturaCostoClassifier.claseItem(item.costoOrigen, filas.sumOf { it.valor }) == ClaseCobertura.FALTANTE
            }
            return ItemCostoOt(
                filas = filas,
                origenGuardado = item.costoOrigen,
                incompleto = incompleto,
                ojosLente = com.example.optoapp.domain.OjosLente.normalize(item.ojosLente),
            )
        }

        private fun fila(item: DispensacionItemUi, componente: ComponenteCosto): FilaCostoOt? {
            item.costoOverrides[componente]?.let {
                return FilaCostoOt(componente, it, CostoOrigenResolver.MANUAL, editado = true)
            }
            val valor = item.costo(componente) ?: return null
            val origen = when (item.costoOrigenes[componente]) {
                OrigenComponente.MANUAL -> CostoOrigenResolver.MANUAL
                OrigenComponente.MATRIZ -> CostoOrigenResolver.MATRIZ
                OrigenComponente.FALTANTE -> CostoOrigenResolver.FALTANTE
                OrigenComponente.PRESERVADA, null -> item.costoOrigen?.takeIf { it in ORIGENES_UNIFORMES }
            }
            return FilaCostoOt(componente, valor, origen, editado = false)
        }

        private fun DispensacionItemUi.costo(componente: ComponenteCosto): Double? = when (componente) {
            ComponenteCosto.OD -> costoRealOd
            ComponenteCosto.OI -> costoRealOi
            ComponenteCosto.MONTURA -> costoRealMontura
            ComponenteCosto.BISELADO -> costoRealBiselado
            ComponenteCosto.LC -> costoRealLc
        }
    }
}

@Composable
fun CostoMargenOtSection(
    modelo: CostoMargenOtModel,
    onEditar: (itemIndex: Int, componente: ComponenteCosto, valor: Double) -> Unit,
    onRestablecer: (itemIndex: Int, componente: ComponenteCosto) -> Unit,
    editable: Boolean,
    modifier: Modifier = Modifier,
    onGuardarBiseladoMatriz: ((itemIndex: Int) -> Unit)? = null,
    onGuardarCostoLenteMatriz: ((itemIndex: Int, ComponenteCosto) -> Unit)? = null,
) {
    var edicion by remember { mutableStateOf<Pair<Int, FilaCostoOt?>?>(null) }
    var componenteNuevo by remember { mutableStateOf<ComponenteCosto?>(null) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.ot_costo_titulo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            modelo.items.forEachIndexed { index, item ->
                ItemCosto(
                    numero = index + 1,
                    item = item,
                    editable = editable,
                    onEditar = { fila -> edicion = index to fila },
                    onAgregar = { componente ->
                        componenteNuevo = componente
                        edicion = index to null
                    },
                    onRestablecer = { componente -> onRestablecer(index, componente) },
                )
                if (editable && onGuardarCostoLenteMatriz != null) {
                    val od = item.filas.find { it.componente == ComponenteCosto.OD }
                    val oi = item.filas.find { it.componente == ComponenteCosto.OI }
                    if (od != null && od.valor > 0.0) {
                        TextButton(onClick = { onGuardarCostoLenteMatriz(index, ComponenteCosto.OD) }) {
                            Text(stringResource(R.string.ot_costo_guardar_od_matriz))
                        }
                    }
                    if (oi != null && oi.valor > 0.0) {
                        TextButton(onClick = { onGuardarCostoLenteMatriz(index, ComponenteCosto.OI) }) {
                            Text(stringResource(R.string.ot_costo_guardar_oi_matriz))
                        }
                    }
                }
                val biselado = item.filas.find { it.componente == ComponenteCosto.BISELADO }
                if (editable && onGuardarBiseladoMatriz != null && biselado != null && biselado.valor > 0.0) {
                    TextButton(onClick = { onGuardarBiseladoMatriz(index) }) {
                        Text(stringResource(R.string.ot_costo_guardar_biselado_matriz))
                    }
                }
                HorizontalDivider()
            }
            if (modelo.costoRegalos > 0.0) {
                FilaTotal(stringResource(R.string.ot_costo_regalos), MoneyFormatter.format(modelo.costoRegalos))
            }
            FilaTotal(stringResource(R.string.ot_costo_total), modelo.costoTotalTexto)
            FilaTotal(stringResource(R.string.ot_costo_margen), modelo.margenTexto)
            FilaTotal(stringResource(R.string.ot_costo_margen_pct), modelo.margenPctTexto)
            if (modelo.incompleto) {
                Text(
                    stringResource(R.string.ot_costo_margen_incompleto),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    edicion?.let { (itemIndex, fila) ->
        val componente = fila?.componente ?: componenteNuevo ?: return@let
        EditarCostoDialog(
            componente = componente,
            valorInicial = fila?.valor,
            onConfirmar = { valor ->
                onEditar(itemIndex, componente, valor)
                edicion = null
            },
            onCancelar = { edicion = null },
        )
    }
}

@Composable
private fun ItemCosto(
    numero: Int,
    item: ItemCostoOt,
    editable: Boolean,
    onEditar: (FilaCostoOt) -> Unit,
    onAgregar: (ComponenteCosto) -> Unit,
    onRestablecer: (ComponenteCosto) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.ot_costo_producto, numero), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        item.origenGuardado?.let {
            Text(
                stringResource(R.string.ot_costo_origen_guardado, origenTexto(it)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (item.filas.isEmpty()) {
        Text(
            stringResource(R.string.ot_costo_sin_costos),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item.filas.forEach { fila ->
        val nombre = componenteTexto(fila.componente)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(nombre, style = MaterialTheme.typography.bodyMedium)
                fila.origen?.let {
                    Text(
                        origenTexto(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (it == CostoOrigenResolver.FALTANTE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(MoneyFormatter.format(fila.valor), style = MaterialTheme.typography.bodyMedium)
            if (editable) {
                IconButton(onClick = { onEditar(fila) }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.ot_costo_editar_cd, nombre))
                }
            }
            if (editable && fila.editado) {
                IconButton(onClick = { onRestablecer(fila.componente) }) {
                    Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.ot_costo_restablecer_cd, nombre))
                }
            }
        }
    }
    val faltantes = ComponenteCosto.entries.filter { componente ->
        item.filas.none { it.componente == componente } && when (componente) {
            ComponenteCosto.OD -> com.example.optoapp.domain.OjosLente.includesOd(item.ojosLente)
            ComponenteCosto.OI -> com.example.optoapp.domain.OjosLente.includesOi(item.ojosLente)
            else -> true
        }
    }
    if (editable && faltantes.isNotEmpty()) {
        var menuAbierto by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { menuAbierto = true }) { Text(stringResource(R.string.ot_costo_agregar_manual)) }
            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                faltantes.forEach { componente ->
                    DropdownMenuItem(
                        text = { Text(componenteTexto(componente)) },
                        onClick = {
                            menuAbierto = false
                            onAgregar(componente)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaTotal(etiqueta: String, valor: String) {
    Row {
        Text(etiqueta, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text(valor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditarCostoDialog(
    componente: ComponenteCosto,
    valorInicial: Double?,
    onConfirmar: (Double) -> Unit,
    onCancelar: () -> Unit,
) {
    var texto by remember(componente) { mutableStateOf(valorInicial?.let { "%.2f".format(Locale.US, it) } ?: "") }
    val valor = texto.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.ot_costo_editar_titulo, componenteTexto(componente))) },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text(stringResource(R.string.ot_costo_valor_label)) },
                singleLine = true,
                isError = texto.isNotEmpty() && valor == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(onClick = { valor?.let(onConfirmar) }, enabled = valor != null) {
                Text(stringResource(R.string.ot_costo_aplicar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.ot_costo_cancelar)) } },
    )
}

@Composable
private fun componenteTexto(componente: ComponenteCosto): String = stringResource(
    when (componente) {
        ComponenteCosto.OD -> R.string.ot_costo_componente_od
        ComponenteCosto.OI -> R.string.ot_costo_componente_oi
        ComponenteCosto.MONTURA -> R.string.ot_costo_componente_montura
        ComponenteCosto.BISELADO -> R.string.ot_costo_componente_biselado
        ComponenteCosto.LC -> R.string.ot_costo_componente_lc
    },
)

@Composable
private fun origenTexto(origen: String): String = when (origen) {
    CostoOrigenResolver.MANUAL -> stringResource(R.string.ot_costo_origen_manual)
    CostoOrigenResolver.MATRIZ -> stringResource(R.string.ot_costo_origen_matriz)
    CostoOrigenResolver.MIXTO -> stringResource(R.string.ot_costo_origen_mixto)
    else -> stringResource(R.string.ot_costo_origen_faltante)
}
