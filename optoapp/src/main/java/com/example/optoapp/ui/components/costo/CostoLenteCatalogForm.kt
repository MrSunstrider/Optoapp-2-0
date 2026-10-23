package com.example.optoapp.ui.components.costo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.optoapp.domain.costo.LensCatalogRules
import com.example.optoapp.domain.costo.LensCatalogSelection
import com.example.optoapp.domain.costo.LensProductCatalog
import com.example.optoapp.domain.costo.LensTreatments
import com.example.optoapp.ui.components.OptoDropdownMenuField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.MODALIDADES_COSTO_LENTE

private val DISCROMATOPSIA_TIPOS = listOf(
    "", "550 Rojo", "550 Rojo-Violeta", "550 Rojo-Marrón", "585 nm", "600 Rojo",
    "Amarillo 450 nm (tritan)", "Amarillo 500 nm (tritan)",
    "EnChroma Indoor", "EnChroma Outdoor",
    "Pilestone A: rojo-verde leve/moderado", "Pilestone B: rojo-verde fuerte", "Pilestone C: interior",
    "Pilestone D: protan (deficiencia roja)", "Pilestone E: tritan (azul-amarillo)",
)

private val CURVAS_SOLAR = listOf("", "4", "6", "8")
// WHY: OT lookup keys use the same IR labels dispensación writes on the item.
private val INDICES_IR = listOf("", "1.6", "1.7", "1.7 rose", "1.8")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostoLenteCatalogForm(
    tipoLente: String,
    subtipoBifocal: String,
    material: String,
    tratamientos: List<String>,
    discromatopsiaTipo: String,
    curvaBase: String,
    indiceRefraccion: String,
    serie: String,
    costoUnitario: String,
    stockOFabricacion: String,
    onTipoLenteChange: (String) -> Unit,
    onSubtipoBifocalChange: (String) -> Unit,
    onMaterialChange: (String) -> Unit,
    onToggleTratamiento: (String) -> Unit,
    onDiscromatopsiaTipoChange: (String) -> Unit,
    onCurvaBaseChange: (String) -> Unit,
    onIndiceRefraccionChange: (String) -> Unit,
    onStockOFabricacionChange: (String) -> Unit,
    onSerieChange: (String) -> Unit,
    onCostoUnitarioChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // WHY: Distancia is not a lookup key; null keeps foto families available for matrix pricing.
    val familias = remember(tipoLente, subtipoBifocal) {
        if (tipoLente.isBlank()) {
            emptyList()
        } else {
            LensCatalogRules.familias(
                tipoLente,
                distancia = null,
                subTipoBifocal = subtipoBifocal.takeIf { it.isNotBlank() },
            )
        }
    }
    val tratamientosPermitidos = remember(tipoLente, material, subtipoBifocal) {
        if (tipoLente.isBlank() || material.isBlank()) {
            emptyList()
        } else {
            LensCatalogRules.tratamientosPermitidos(
                tipoLente,
                material,
                distancia = null,
                subTipoBifocal = subtipoBifocal.takeIf { it.isNotBlank() },
            )
        }
    }
    val preview = remember(
        tipoLente, subtipoBifocal, material, tratamientos, discromatopsiaTipo, curvaBase, indiceRefraccion,
    ) {
        if (tipoLente.isBlank() || material.isBlank()) {
            null
        } else {
            LensCatalogRules.resolve(
                LensCatalogSelection(
                    tipoLente = tipoLente,
                    distancia = null,
                    subTipoBifocal = subtipoBifocal.takeIf { it.isNotBlank() },
                    familia = material,
                    tratamientos = tratamientos,
                    discromatopsiaTipo = discromatopsiaTipo.takeIf { it.isNotBlank() },
                    curvaBase = curvaBase.takeIf { it.isNotBlank() },
                    indiceRefraccion = indiceRefraccion.takeIf { it.isNotBlank() },
                    forzarStockIgnorandoAstigmatismo = false,
                ),
                null,
            )
        }
    }
    val esStock = stockOFabricacion == "stock"

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OptoDropdownMenuField(
            label = stringResource(R.string.costos_tipo_lente_requerido),
            selected = tipoLente,
            options = LensProductCatalog.TIPOS.filter { it != LensProductCatalog.TIPO_LC },
        ) { onTipoLenteChange(it) }

        OptoDropdownMenuField(
            label = stringResource(R.string.costos_stock_fabricacion_requerido),
            selected = stockOFabricacion,
            options = MODALIDADES_COSTO_LENTE,
        ) { onStockOFabricacionChange(it) }

        if (tipoLente == LensProductCatalog.TIPO_BIFOCAL) {
            OptoDropdownMenuField(
                label = stringResource(R.string.costos_subtipo_bifocal),
                selected = subtipoBifocal,
                options = LensProductCatalog.SUBTIPOS_BIFOCAL,
            ) { onSubtipoBifocalChange(it) }
        }

        if (familias.isNotEmpty()) {
            OptoDropdownMenuField(
                label = stringResource(R.string.costos_material_requerido),
                selected = material,
                options = familias,
            ) { onMaterialChange(it) }
        }

        if (material.isNotBlank()) {
            Text(
                stringResource(R.string.costos_tratamientos_adicionales),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )

            if (tratamientos.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tratamientos.forEach { trat ->
                        AssistChip(
                            onClick = { onToggleTratamiento(trat) },
                            label = { Text(trat, fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )
                    }
                }
            }

            val available = tratamientosPermitidos.filter { it !in tratamientos }
            if (available.isNotEmpty()) {
                var addExpanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { addExpanded = true },
                        label = { Text(stringResource(R.string.costos_anadir_tratamiento), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                    DropdownMenu(expanded = addExpanded, onDismissRequest = { addExpanded = false }) {
                        available.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    onToggleTratamiento(opt)
                                    addExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            if (tratamientos.contains(LensTreatments.DISCROMATOPSIA)) {
                Text(
                    stringResource(R.string.costos_discromatopsia_eval_hint),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OptoDropdownMenuField(
                    label = stringResource(R.string.costos_filtro_discromatopsia),
                    selected = discromatopsiaTipo,
                    options = DISCROMATOPSIA_TIPOS,
                ) { onDiscromatopsiaTipoChange(it) }
            }

            if (tratamientos.contains(LensTreatments.SOLAR)) {
                OptoDropdownMenuField(
                    label = stringResource(R.string.costos_curva_base),
                    selected = curvaBase,
                    options = CURVAS_SOLAR,
                ) { onCurvaBaseChange(it) }
            }

            if (preview?.requiresManualIrCost == true) {
                OptoDropdownMenuField(
                    label = stringResource(R.string.costos_indice_refraccion),
                    selected = indiceRefraccion,
                    options = INDICES_IR,
                ) { onIndiceRefraccionChange(it) }
                Text(
                    stringResource(R.string.costos_alto_indice_manual_hint),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OptoTextField(
            value = serie,
            onValueChange = onSerieChange,
            label = stringResource(
                if (esStock) R.string.costos_serie_requerida else R.string.costos_serie_opcional,
            ),
            keyboardType = KeyboardType.Number,
        )
        OptoTextField(
            value = costoUnitario,
            onValueChange = onCostoUnitarioChange,
            label = stringResource(R.string.costos_costo_unitario_requerido),
            keyboardType = KeyboardType.Decimal,
        )
    }
}
