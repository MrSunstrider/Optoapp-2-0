package com.example.optoapp.ui.screens.costos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.R
import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import com.example.optoapp.ui.components.OptoDialog
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.FormServicioTipo
import com.example.optoapp.viewmodel.ServicioTiposViewModel

@Composable
fun ServicioTiposTab(viewModel: ServicioTiposViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            if (!uiState.puedeEditar) {
                item { MatrizSoloLectura(Modifier.padding(bottom = 8.dp)) }
            }
            uiState.error?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
            if (uiState.tipos.isEmpty()) {
                item { ServicioTiposVacio(uiState.muestraCrearCta, viewModel::nuevo) }
            }
            items(uiState.tipos, key = { it.id }) { tipo ->
                ServicioTipoRow(
                    tipo = tipo,
                    puedeEditar = uiState.puedeEditar,
                    onEditar = { viewModel.editar(tipo) },
                    onCambiarActivo = { if (tipo.activo) viewModel.desactivar(tipo) else viewModel.reactivar(tipo) },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        if (uiState.puedeEditar && uiState.tipos.isNotEmpty()) {
            MatrizAgregarFab(viewModel::nuevo, Modifier.align(Alignment.BottomEnd))
        }
    }

    uiState.form?.let { form ->
        FormServicioTipoDialog(
            form = form,
            onCambio = viewModel::cambiarForm,
            onGuardar = viewModel::guardar,
            onDismiss = viewModel::cerrarForm,
        )
    }
}

@Composable
private fun ServicioTiposVacio(muestraCrear: Boolean, onCrear: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.servicio_tipos_vacio), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (muestraCrear) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onCrear) { Text(stringResource(R.string.servicio_tipos_crear)) }
        }
    }
}

@Composable
private fun ServicioTipoRow(
    tipo: ServicioTipoEntity,
    puedeEditar: Boolean,
    onEditar: () -> Unit,
    onCambiarActivo: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (puedeEditar) Modifier.clickable(onClick = onEditar) else Modifier)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val titulo = if (tipo.activo) {
                tipo.nombre
            } else {
                stringResource(R.string.servicio_tipos_nombre_inactivo, tipo.nombre)
            }
            Text(titulo, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(
                stringResource(
                    R.string.servicio_tipos_detalle,
                    MoneyFormatter.format(tipo.costoPropio),
                    MoneyFormatter.format(tipo.costoTercero),
                ),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (puedeEditar) {
            TextButton(onClick = onCambiarActivo) {
                Text(
                    stringResource(
                        if (tipo.activo) R.string.servicio_tipos_desactivar else R.string.servicio_tipos_reactivar,
                    ),
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun FormServicioTipoDialog(
    form: FormServicioTipo,
    onCambio: ((FormServicioTipo) -> FormServicioTipo) -> Unit,
    onGuardar: () -> Unit,
    onDismiss: () -> Unit,
) {
    OptoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(
            if (form.original == null) R.string.servicio_tipos_nuevo_titulo else R.string.servicio_tipos_editar_titulo,
        ),
        confirmText = stringResource(R.string.common_save),
        onConfirm = onGuardar,
        dismissText = stringResource(R.string.common_cancel),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = form.nombre,
                onValueChange = { valor -> onCambio { it.copy(nombre = valor, nombreError = null) } },
                label = { Text(stringResource(R.string.servicio_tipos_nombre)) },
                isError = form.nombreError != null,
                supportingText = form.nombreError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.costoPropio,
                onValueChange = { valor -> onCambio { it.copy(costoPropio = valor, costoError = null) } },
                label = { Text(stringResource(R.string.servicio_tipos_costo_propio)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = form.costoError != null,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.costoTercero,
                onValueChange = { valor -> onCambio { it.copy(costoTercero = valor, costoError = null) } },
                label = { Text(stringResource(R.string.servicio_tipos_costo_tercero)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = form.costoError != null,
                supportingText = form.costoError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
