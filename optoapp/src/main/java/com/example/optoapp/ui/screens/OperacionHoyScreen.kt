package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.util.FileShareUtils
import com.example.optoapp.util.InventarioMonturasPdfGenerator
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OperacionHoyViewModel
import java.util.Locale
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.theme.OptoTokens

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

    // Mostrar error si ocurrió al cargar datos
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val canView = AppRoles.canViewOperacionHoy(opticaRol)
    val canExportInventario = AppRoles.canExportInventario(opticaRol)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = "Operación de Hoy",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (!canView) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(OptoTokens.spacing.xl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(OptoTokens.spacing.lg))
                Text("Acceso restringido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Tu rol no tiene permiso para ver esta sección.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(OptoTokens.spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)
        ) {
            // KPI Cards Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.md)) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CalendarMonth,
                    label = "Citas Hoy",
                    value = uiState.citasHoy.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Inventory2,
                    label = "Entregas",
                    value = uiState.entregasPendientes.toString(),
                    color = if (uiState.entregasPendientes > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    highlight = uiState.entregasPendientes > 0
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.md)) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AttachMoney,
                    label = "Cobros Hoy",
                    value = "S/ ${String.format(Locale.getDefault(), "%.0f", uiState.cobrosHoy)}",
                    color = MaterialTheme.colorScheme.tertiary
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Warning,
                    label = "Stock Crítico",
                    value = uiState.stockCritico.toString(),
                    color = if (uiState.stockCritico > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    highlight = uiState.stockCritico > 0,
                    onClick = if (uiState.stockCritico > 0) {{ navController.navigate("monturas") }} else null
                )
            }

            // Alertas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(OptoTokens.spacing.lg), verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(OptoTokens.spacing.sm))
                        Text("Alertas", fontWeight = FontWeight.Bold)
                    }
                    if (uiState.alertas.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(OptoTokens.spacing.sm))
                            Text("Sin alertas críticas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        uiState.alertas.forEach { a ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(OptoTokens.spacing.sm))
                                Text(a, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Exportaciones
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(OptoTokens.spacing.lg), verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(OptoTokens.spacing.sm))
                        Text("Exportaciones", fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = canExportInventario,
                            onClick = {
                                val file = InventarioMonturasPdfGenerator.generate(context, uiState.monturas)
                                FileShareUtils.openPdf(context, file, "Abrir inventario PDF")
                            }
                        ) { Text("Inventario PDF", maxLines = 1) }
                    }
                    OutlinedButton(onClick = { viewModel.refresh() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Actualizar")
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = onClick ?: {},
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(OptoTokens.spacing.lg).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = color.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(OptoTokens.spacing.sm))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (highlight) {
                Spacer(Modifier.height(4.dp))
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)) {
                    Text("Pendiente", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
