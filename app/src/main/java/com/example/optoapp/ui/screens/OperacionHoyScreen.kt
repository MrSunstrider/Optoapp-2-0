package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.util.FileShareUtils
import com.example.optoapp.util.InventarioMonturasPdfGenerator
import com.example.optoapp.util.OperacionExportUtils
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OperacionHoyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperacionHoyScreen(
    navController: NavController,
    viewModel: OperacionHoyViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val canView = AppRoles.canViewOperacionHoy(opticaRol)
    val canExportPendientes = AppRoles.canExportPendientes(opticaRol)
    val canExportCierre = AppRoles.canExportCierreCaja(opticaRol)
    val canExportInventario = AppRoles.canExportInventario(opticaRol)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Operación de Hoy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!canView) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Acceso restringido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Tu rol actual no tiene permiso para consultar Operación de Hoy.")
                    }
                }
                return@Column
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tablero operativo", fontWeight = FontWeight.Bold)
                    Text("Fecha: ${uiState.fecha}")
                    Text("Citas hoy: ${uiState.citasHoy}")
                    Text("Entregas pendientes: ${uiState.entregasPendientes}")
                    Text("Cobros del día: s/. ${String.format(Locale.getDefault(), "%.2f", uiState.cobrosHoy)}")
                    Text("Stock crítico: ${uiState.stockCritico}")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Alertas internas", fontWeight = FontWeight.Bold)
                    if (uiState.alertas.isEmpty()) {
                        Text("Sin alertas críticas para hoy.")
                    } else {
                        uiState.alertas.forEach { a -> Text("- $a") }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Exportaciones rápidas", fontWeight = FontWeight.Bold)
                    Text(
                        "Genera y comparte reportes operativos para pendientes, cierre de caja e inventario.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!canExportPendientes && !canExportCierre && !canExportInventario) {
                        Text(
                            "Tu rol no tiene permiso para exportar reportes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = canExportPendientes,
                            onClick = {
                                val file = OperacionExportUtils.exportPendientesCsv(
                                    context,
                                    uiState.dispensacionesPendientes,
                                    uiState.serviciosPendientes
                                )
                                FileShareUtils.shareFile(context, file, "text/csv", "Compartir pendientes")
                            }
                        ) { Text("Pendientes CSV") }
                        OutlinedButton(
                            enabled = canExportCierre,
                            onClick = {
                                val file = OperacionExportUtils.exportCierreDiaCsv(
                                    context,
                                    uiState.fecha,
                                    uiState.pagosHoy
                                )
                                FileShareUtils.shareFile(context, file, "text/csv", "Compartir cierre de caja")
                            }
                        ) { Text("Cierre CSV") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = canExportInventario,
                            onClick = {
                                val file = OperacionExportUtils.exportInventarioCsv(context, uiState.monturas)
                                FileShareUtils.shareFile(context, file, "text/csv", "Compartir inventario CSV")
                            }
                        ) { Text("Inventario CSV") }
                        OutlinedButton(
                            enabled = canExportInventario,
                            onClick = {
                                val file = InventarioMonturasPdfGenerator.generate(context, uiState.monturas)
                                FileShareUtils.sharePdf(context, file, "Compartir inventario PDF")
                            }
                        ) { Text("Inventario PDF") }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedButton(onClick = { viewModel.refresh() }) { Text("Actualizar") }
                }
            }
        }
    }
}
