package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.optoapp.ui.components.OptoTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.monturas.MonturaEditForm
import com.example.optoapp.ui.components.monturas.MonturaListSection
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.util.FileShareUtils
import com.example.optoapp.util.InventarioMonturasPdfGenerator
import com.example.optoapp.data.AppRoles
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.MonturasViewModel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonturasScreen(
    navController: NavController,
    viewModel: MonturasViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val monturas by viewModel.monturas.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val canEdit = AppRoles.canEditInventory(opticaRol)
    var lastGeneratedPdf by remember { mutableStateOf<File?>(null) }

    val filtradas = monturas.filter {
        if (uiState.query.isBlank()) true
        else {
            it.sku.contains(uiState.query, ignoreCase = true) ||
                it.marca.contains(uiState.query, ignoreCase = true) ||
                it.modelo.contains(uiState.query, ignoreCase = true)
        }
    }
    val porReponer = monturas
        .filter { it.activo && it.stockActual <= it.stockMinimo }
        .sortedBy { it.stockActual - it.stockMinimo }
    val stockTotal = filtradas.sumOf { it.stockActual }
    val valorCosto = filtradas.sumOf { it.stockActual * it.costo }
    val valorVenta = filtradas.sumOf { it.stockActual * it.precio }
    val restantes = if (porReponer.isEmpty()) filtradas else filtradas.filter { f -> porReponer.none { it.id == f.id } }

    if (uiState.editing) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEdit() },
            title = { Text(if (uiState.form.id == null) "Nuevo Producto" else "Editar Producto") },
            text = {
                MonturaEditForm(
                    form = uiState.form,
                    onUpdate = { newForm -> viewModel.updateForm { newForm } },
                    error = uiState.error
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.save() }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { viewModel.cancelEdit() }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OptoTopAppBar(
                title = "Inventario General",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (canEdit) {
                        IconButton(onClick = { viewModel.startCreate() }) {
                            Icon(Icons.Default.Add, contentDescription = "Nuevo producto")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = OptoTokens.spacing.lg, vertical = OptoTokens.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)
        ) {
            OptoTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = "Buscar por SKU, marca o modelo",
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                }
            )

            StockAlertCard(porReponer = porReponer)

            SummaryCard(
                filtradasSize = filtradas.size,
                stockTotal = stockTotal,
                valorCosto = valorCosto,
                valorVenta = valorVenta,
                onGeneratePdf = {
                    val pdf = InventarioMonturasPdfGenerator.generate(context, filtradas)
                    lastGeneratedPdf = pdf
                    FileShareUtils.openPdf(context, pdf, "Abrir reporte de inventario")
                },
                onSharePdf = {
                    val file = lastGeneratedPdf ?: InventarioMonturasPdfGenerator.generate(context, filtradas)
                    lastGeneratedPdf = file
                    FileShareUtils.sharePdf(context, file, "Compartir reporte de inventario")
                }
            )

            if (!uiState.error.isNullOrBlank()) {
                Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
            }
            if (!uiState.success.isNullOrBlank()) {
                Text(uiState.success ?: "", color = MaterialTheme.colorScheme.tertiary)
            }

            MonturaListSection(
                porReponer = porReponer,
                restantes = restantes,
                onEdit = { viewModel.startEdit(it) },
                onDelete = { viewModel.delete(it) },
                onEntrada = { viewModel.registrarEntrada(it, 1) }
            )
        }
    }
}

@Composable
private fun StockAlertCard(porReponer: List<com.example.optoapp.data.Montura>) {
    val hasAlerts = porReponer.isNotEmpty()
    val containerColor = if (hasAlerts) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (hasAlerts) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = if (hasAlerts) Icons.Default.Warning else Icons.Default.CheckCircle

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = OptoTokens.elevation.level1),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(OptoTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor
                )
                Text(
                    text = "Alertas de stock bajo: ${porReponer.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor
                )
            }
            if (!hasAlerts) {
                Text(
                    "No hay productos críticos por reposición.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            } else {
                porReponer.take(5).forEach { m ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "${m.sku} ${m.marca} ${m.modelo}: ${m.stockActual}/${m.stockMinimo}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    filtradasSize: Int,
    stockTotal: Int,
    valorCosto: Double,
    valorVenta: Double,
    onGeneratePdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = OptoTokens.elevation.level1),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(OptoTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Resumen inventario",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.md)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    KpiItem("Productos", "$filtradasSize")
                    Spacer(modifier = Modifier.height(OptoTokens.spacing.xs))
                    KpiItem("Stock total", "$stockTotal")
                }
                Column(modifier = Modifier.weight(1f)) {
                    KpiItem("Valor costo", "S/. ${String.format(Locale.getDefault(), "%.2f", valorCosto)}")
                    Spacer(modifier = Modifier.height(OptoTokens.spacing.xs))
                    KpiItem("Valor venta", "S/. ${String.format(Locale.getDefault(), "%.2f", valorVenta)}")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)) {
                Button(onClick = onGeneratePdf) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(OptoTokens.spacing.xs))
                    Text("Generar PDF")
                }
                OutlinedButton(onClick = onSharePdf) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(OptoTokens.spacing.xs))
                    Text("Compartir PDF")
                }
            }
        }
    }
}

@Composable
private fun KpiItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
