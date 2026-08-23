package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.ResumenDiarioViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenDiarioScreen(
    navController: NavController,
    viewModel: ResumenDiarioViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = null)
    val canView = opticaRol != null && AppRoles.canViewBiAndReports(opticaRol!!)
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-PE"))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OptoTopAppBar(
                title = "Resumen diario",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (canView) {
                        IconButton(onClick = { viewModel.refresh() }, enabled = !uiState.refreshing) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!canView) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Acceso restringido", fontWeight = FontWeight.Bold)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.navigateMonth(-1) }) {
                    @Suppress("DEPRECATION")
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mes anterior")
                }
                Text(
                    uiState.mesSeleccionado.format(monthFormatter).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { viewModel.navigateMonth(1) }) {
                    @Suppress("DEPRECATION")
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mes siguiente")
                }
            }

            if (uiState.rows.isEmpty()) {
                Text("Sin resumenes para este mes", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.rows, key = { it.id }) { row ->
                        ResumenDiarioRowCard(row)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenDiarioRowCard(row: ResumenDiarioEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.fecha, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Ventas: S/ ${"%.2f".format(row.ventasMontoTotal)} (${row.ventasCantidad})")
            Text("Cobros: S/ ${"%.2f".format(row.cobrosMontoTotal)} (${row.cobrosCantidad})")
            Text("Costo ventas: S/ ${"%.2f".format(row.ventasCostoTotal)}")
        }
    }
}
