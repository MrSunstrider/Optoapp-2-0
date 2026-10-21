package com.example.optoapp.ui.screens.costos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.ui.components.OptoDialog
import com.example.optoapp.viewmodel.FormBiselado
import com.example.optoapp.viewmodel.MODALIDADES_BISELADO
import com.example.optoapp.viewmodel.TIPOS_ARO_BISELADO

@Composable
fun BiseladoTab(
    filas: List<CostoBiseladoEntity>,
    puedeEditar: Boolean,
    onEditar: (CostoBiseladoEntity) -> Unit,
    onAgregar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MatrizLista(filas = filas, puedeEditar = puedeEditar, onAgregar = onAgregar, clave = { it.id }, modifier = modifier) { fila ->
        val detalle = listOfNotNull(
            etiquetaMatriz(fila.stockOFabricacion),
            fila.serie?.let { stringResource(R.string.costos_serie_corta, it) },
            fila.altoIndice?.let { stringResource(R.string.costos_alto_indice_corto, it) },
        ).joinToString(" · ")
        MatrizRow(
            titulo = "${fila.material} · ${etiquetaMatriz(fila.tipoAro)}",
            detalle = detalle,
            costo = fila.costoPorPar,
            onClick = if (puedeEditar) ({ onEditar(fila) }) else null,
        )
    }
}

@Composable
fun FormBiseladoDialog(
    form: FormBiselado,
    materiales: List<String>,
    onCambio: ((FormBiselado) -> FormBiselado) -> Unit,
    onGuardar: () -> Unit,
    onDismiss: () -> Unit,
) {
    OptoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.costos_nuevo_biselado_titulo),
        confirmText = stringResource(R.string.common_save),
        onConfirm = onGuardar,
        dismissText = stringResource(R.string.common_cancel),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MatrizDropdown(
                label = stringResource(R.string.costos_biselado_material_hint),
                seleccion = form.material,
                opciones = materiales,
                etiqueta = { it },
                onSeleccion = { valor -> onCambio { it.copy(material = valor) } },
            )
            MatrizDropdown(
                label = stringResource(R.string.costos_tipo_aro_requerido),
                seleccion = form.tipoAro,
                opciones = TIPOS_ARO_BISELADO,
                etiqueta = { etiquetaMatriz(it) },
                onSeleccion = { valor -> onCambio { it.copy(tipoAro = valor) } },
            )
            MatrizDropdown(
                label = stringResource(R.string.costos_modalidad_requerida),
                seleccion = form.modalidad,
                opciones = MODALIDADES_BISELADO,
                etiqueta = { etiquetaMatriz(it) },
                onSeleccion = { valor -> onCambio { it.copy(modalidad = valor) } },
            )
            OutlinedTextField(
                value = form.serie,
                onValueChange = { valor -> onCambio { it.copy(serie = valor) } },
                label = { Text(stringResource(R.string.costos_serie_opcional)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.altoIndice,
                onValueChange = { valor -> onCambio { it.copy(altoIndice = valor) } },
                label = { Text(stringResource(R.string.costos_alto_indice_opcional)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.costo,
                onValueChange = { valor -> onCambio { it.copy(costo = valor) } },
                label = { Text(stringResource(R.string.costos_costo_por_par_requerido)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        }
    }
}
