package com.example.optoapp.ui.screens.costos

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.util.MoneyFormatter

@StringRes
private fun etiquetaCodigoMatriz(codigo: String): Int? = when (codigo) {
    "aro_completo" -> R.string.costos_aro_completo
    "semi_aire" -> R.string.costos_aro_semi_aire
    "al_aire" -> R.string.costos_aro_al_aire
    "stock" -> R.string.costos_modalidad_stock
    "fabricacion" -> R.string.costos_modalidad_fabricacion
    "cosmetico" -> R.string.costos_lc_cosmetico
    "graduado" -> R.string.costos_lc_graduado
    "terapeutico" -> R.string.costos_lc_terapeutico
    "diario" -> R.string.costos_lc_diario
    "quincenal" -> R.string.costos_lc_quincenal
    "mensual" -> R.string.costos_lc_mensual
    "anual" -> R.string.costos_lc_anual
    else -> null
}

// Rows synced from the web may carry codes this build does not know yet; showing the raw code beats hiding the row.
@Composable
fun etiquetaMatriz(codigo: String): String = etiquetaCodigoMatriz(codigo)?.let { stringResource(it) } ?: codigo

@Composable
fun MatrizRow(
    titulo: String,
    detalle: String?,
    costo: Double,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!detalle.isNullOrEmpty()) {
                Text(detalle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(MoneyFormatter.format(costo), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        trailing()
    }
    HorizontalDivider()
}

@Composable
fun MatrizVacia(puedeEditar: Boolean, onAgregar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.costos_matriz_vacia), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (puedeEditar) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAgregar) { Text(stringResource(R.string.costos_agregar_precio)) }
        }
    }
}

@Composable
fun MatrizSoloLectura(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.costos_matrices_solo_lectura),
        modifier = modifier,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun <T> MatrizLista(
    filas: List<T>,
    puedeEditar: Boolean,
    onAgregar: () -> Unit,
    clave: (T) -> String,
    modifier: Modifier = Modifier,
    fila: @Composable (T) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            if (!puedeEditar) {
                item { MatrizSoloLectura(Modifier.padding(bottom = 8.dp)) }
            }
            if (filas.isEmpty()) {
                item { MatrizVacia(puedeEditar = puedeEditar, onAgregar = onAgregar) }
            }
            items(filas, key = clave) { fila(it) }
            item { Spacer(Modifier.height(80.dp)) }
        }
        if (puedeEditar && filas.isNotEmpty()) {
            MatrizAgregarFab(onAgregar, Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
fun MatrizAgregarFab(onAgregar: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(onClick = onAgregar, modifier = modifier.padding(16.dp).navigationBarsPadding()) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.costos_agregar_precio))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MatrizDropdown(
    label: String,
    seleccion: T,
    opciones: List<T>,
    etiqueta: @Composable (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = etiqueta(seleccion),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(etiqueta(opcion)) },
                    onClick = {
                        onSeleccion(opcion)
                        expanded = false
                    },
                )
            }
        }
    }
}
