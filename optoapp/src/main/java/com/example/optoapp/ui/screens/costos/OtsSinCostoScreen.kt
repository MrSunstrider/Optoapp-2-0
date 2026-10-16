package com.example.optoapp.ui.screens.costos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.R
import com.example.optoapp.data.OtSinCostoRow
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.charts.ChartEmptyState
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.OtsSinCostoUiState
import com.example.optoapp.viewmodel.OtsSinCostoViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_PE = Locale.forLanguageTag("es-PE")
private val MES_SUBTITULO = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_PE)

data class OtSinCostoFilaTexto(val identificacion: String, val paciente: String, val fecha: String) {
    companion object {
        private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        // Legacy OTs may have no code or a deleted patient; the row must still be identifiable.
        fun from(fila: OtSinCostoRow, sinPaciente: String) = OtSinCostoFilaTexto(
            identificacion = fila.ot.ifBlank { fila.id },
            paciente = fila.pacienteNombre?.takeIf { it.isNotBlank() } ?: sinPaciente,
            fecha = fila.fecha.format(FECHA),
        )
    }
}

enum class OtsSinCostoContenido {
    CARGANDO,
    VACIO,
    LISTA,
    ;

    companion object {
        fun de(state: OtsSinCostoUiState): OtsSinCostoContenido = when {
            state.isLoading -> CARGANDO
            state.filas.isEmpty() -> VACIO
            else -> LISTA
        }
    }
}

@Composable
fun OtsSinCostoScreen(navController: NavController, viewModel: OtsSinCostoViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            OptoTopAppBar(
                title = stringResource(R.string.ots_sin_costo_titulo),
                subtitle = state.mes.format(MES_SUBTITULO).replaceFirstChar { it.titlecase(LOCALE_PE) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ots_sin_costo_atras_cd))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (OtsSinCostoContenido.de(state)) {
                OtsSinCostoContenido.CARGANDO -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                OtsSinCostoContenido.VACIO -> ChartEmptyState(message = stringResource(R.string.ots_sin_costo_vacio))
                OtsSinCostoContenido.LISTA -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.filas, key = { it.id }) { fila ->
                        OtSinCostoItem(fila = fila, onClick = { navController.navigate(viewModel.rutaEdicion(fila)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OtSinCostoItem(fila: OtSinCostoRow, onClick: () -> Unit) {
    val texto = OtSinCostoFilaTexto.from(fila, sinPaciente = stringResource(R.string.ots_sin_costo_sin_paciente))
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(texto.identificacion, fontWeight = FontWeight.Bold)
                Text(texto.fecha, style = MaterialTheme.typography.bodySmall)
            }
            Text(texto.paciente, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    pluralStringResource(R.plurals.ots_sin_costo_items_faltantes, fila.itemsFaltantes, fila.itemsFaltantes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(MoneyFormatter.format(fila.montoTotal), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
