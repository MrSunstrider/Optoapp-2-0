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
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.ui.components.OptoDialog
import com.example.optoapp.viewmodel.FormLc
import com.example.optoapp.viewmodel.MODALIDADES_LC
import com.example.optoapp.viewmodel.TIPOS_LC

@Composable
fun LcTab(
    filas: List<CostoLcEntity>,
    puedeEditar: Boolean,
    onEditar: (CostoLcEntity) -> Unit,
    onAgregar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MatrizLista(filas = filas, puedeEditar = puedeEditar, onAgregar = onAgregar, clave = { it.id }, modifier = modifier) { fila ->
        MatrizRow(
            titulo = "${etiquetaMatriz(fila.tipoLc)} · ${fila.materialLc}",
            detalle = etiquetaMatriz(fila.modalidad),
            costo = fila.costoUnitario,
            onClick = if (puedeEditar) ({ onEditar(fila) }) else null,
        )
    }
}

@Composable
fun FormLcDialog(
    form: FormLc,
    onCambio: ((FormLc) -> FormLc) -> Unit,
    onGuardar: () -> Unit,
    onDismiss: () -> Unit,
) {
    OptoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.costos_nuevo_lc_titulo),
        confirmText = stringResource(R.string.common_save),
        onConfirm = onGuardar,
        dismissText = stringResource(R.string.common_cancel),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MatrizDropdown(
                label = stringResource(R.string.costos_tipo_lc_requerido),
                seleccion = form.tipoLc,
                opciones = TIPOS_LC,
                etiqueta = { etiquetaMatriz(it) },
                onSeleccion = { valor -> onCambio { it.copy(tipoLc = valor) } },
            )
            OutlinedTextField(
                value = form.material,
                onValueChange = { valor -> onCambio { it.copy(material = valor) } },
                label = { Text(stringResource(R.string.costos_material_requerido)) },
                modifier = Modifier.fillMaxWidth(),
            )
            MatrizDropdown(
                label = stringResource(R.string.costos_modalidad_requerida),
                seleccion = form.modalidad,
                opciones = MODALIDADES_LC,
                etiqueta = { etiquetaMatriz(it) },
                onSeleccion = { valor -> onCambio { it.copy(modalidad = valor) } },
            )
            OutlinedTextField(
                value = form.costo,
                onValueChange = { valor -> onCambio { it.copy(costo = valor) } },
                label = { Text(stringResource(R.string.costos_costo_unitario_requerido)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        }
    }
}
