package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.monturas.MonturaEditForm
import com.example.optoapp.ui.components.monturas.MonturaItem
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.util.FileShareUtils
import com.example.optoapp.util.InventarioMonturasPdfGenerator
import com.example.optoapp.data.AppRoles
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.MonturasUiState
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

    val marcasDistintas = remember(monturas) {
        monturas.map { it.marca }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val materialesDistintos = remember(monturas) {
        monturas.map { it.materialMontura }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filtradas = monturas.filter { m ->
        (uiState.query.isBlank() ||
            m.sku.contains(uiState.query, ignoreCase = true) ||
            m.marca.contains(uiState.query, ignoreCase = true) ||
            m.modelo.contains(uiState.query, ignoreCase = true)) &&
        (uiState.filterMarca == null || m.marca == uiState.filterMarca) &&
        (uiState.filterMaterial == null || m.materialMontura == uiState.filterMaterial) &&
        (!uiState.filterStockBajo || m.stockActual <= m.stockMinimo)
    }
    val sortedFiltradas = when (uiState.sortBy) {
        "name" -> filtradas.sortedWith(compareBy({ it.marca }, { it.modelo }))
        "stock_desc" -> filtradas.sortedByDescending { it.stockActual }
        "precio_desc" -> filtradas.sortedByDescending { it.precio }
        else -> filtradas
    }
    val porReponer = monturas
        .filter { it.activo && it.stockActual <= it.stockMinimo }
        .sortedBy { it.stockActual - it.stockMinimo }
    val stockTotal = filtradas.sumOf { it.stockActual }
    val valorCosto = filtradas.sumOf { it.stockActual * it.costo }
    val valorVenta = filtradas.sumOf { it.stockActual * it.precio }
    val restantes = if (porReponer.isEmpty()) sortedFiltradas
        else sortedFiltradas.filter { f -> porReponer.none { it.id == f.id } }

    if (uiState.editing) {
        MonturaEditFullScreen(viewModel = viewModel, uiState = uiState)
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                OptoTopAppBar(
                    title = "Inventario General",
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atr\u00E1s")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = OptoTokens.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)
            ) {
                item {
                    OptoTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        label = "Buscar por SKU, marca o modelo",
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    )
                }

                item {
                    SortFilterRow(
                        currentSortBy = uiState.sortBy,
                        onSortBy = viewModel::setSortBy,
                        currentFilterMarca = uiState.filterMarca,
                        currentFilterMaterial = uiState.filterMaterial,
                        currentFilterStockBajo = uiState.filterStockBajo,
                        onFilterMarca = viewModel::setFilterMarca,
                        onFilterMaterial = viewModel::setFilterMaterial,
                        onToggleStockBajo = viewModel::toggleFilterStockBajo,
                        onClearFilters = viewModel::clearFilters,
                        marcas = marcasDistintas,
                        materiales = materialesDistintos
                    )
                }

                item {
                    StockAlertCard(porReponer = porReponer)
                }

                item {
                    SummaryCard(
                        filtradasSize = filtradas.size,
                        stockTotal = stockTotal,
                        valorCosto = valorCosto,
                        valorVenta = valorVenta,
                        onGeneratePdf = {
                            val pdf = InventarioMonturasPdfGenerator.generate(context, sortedFiltradas)
                            lastGeneratedPdf = pdf
                            FileShareUtils.openPdf(context, pdf, "Abrir reporte de inventario")
                        },
                        onSharePdf = {
                            val file = lastGeneratedPdf
                                ?: InventarioMonturasPdfGenerator.generate(context, sortedFiltradas)
                            lastGeneratedPdf = file
                            FileShareUtils.sharePdf(context, file, "Compartir reporte de inventario")
                        }
                    )
                }

                if (!uiState.error.isNullOrBlank()) {
                    item {
                        Text(
                            uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = OptoTokens.spacing.xs)
                        )
                    }
                }
                if (!uiState.success.isNullOrBlank()) {
                    item {
                        Text(
                            uiState.success ?: "",
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = OptoTokens.spacing.xs)
                        )
                    }
                }

                if (porReponer.isNotEmpty()) {
                    item {
                        Text(
                            "Por reponer",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = OptoTokens.spacing.xs)
                        )
                    }
                    items(porReponer, key = { "low-${it.id}" }) { m ->
                        MonturaItem(
                            montura = m,
                            onEdit = { viewModel.startEdit(m) },
                            onDelete = { viewModel.delete(m) },
                            onEntrada = { viewModel.registrarEntrada(m, 1) },
                            onSalida = { viewModel.registrarSalida(m, 1) }
                        )
                    }
                    item {
                        Text(
                            "Todos los productos",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = OptoTokens.spacing.xs)
                        )
                    }
                }
                items(restantes, key = { it.id }) { m ->
                    MonturaItem(
                        montura = m,
                        onEdit = { viewModel.startEdit(m) },
                        onDelete = { viewModel.delete(m) },
                        onEntrada = { viewModel.registrarEntrada(m, 1) },
                        onSalida = { viewModel.registrarSalida(m, 1) }
                    )
                }

                item { Spacer(modifier = Modifier.height(OptoTokens.spacing.xl)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonturaEditFullScreen(
    viewModel: MonturasViewModel,
    uiState: MonturasUiState
) {
    val form = uiState.form
    val isNew = form.id == null

    Scaffold(
        topBar = {
            OptoTopAppBar(
                title = if (isNew) "Nuevo Producto" else "Editar Producto",
                navigationIcon = {
                    IconButton(onClick = { viewModel.cancelEdit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonturaEditForm(
                form = form,
                onUpdate = { newForm -> viewModel.updateForm { newForm } },
                error = uiState.error
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isNew) "Crear Producto" else "Guardar Cambios")
            }
        }
    }
}

@Composable
private fun SortFilterRow(
    currentSortBy: String?,
    onSortBy: (String?) -> Unit,
    currentFilterMarca: String?,
    currentFilterMaterial: String?,
    currentFilterStockBajo: Boolean,
    onFilterMarca: (String?) -> Unit,
    onFilterMaterial: (String?) -> Unit,
    onToggleStockBajo: () -> Unit,
    onClearFilters: () -> Unit,
    marcas: List<String>,
    materiales: List<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var sortExpanded by remember { mutableStateOf(false) }
        Box {
            FilterChip(
                selected = currentSortBy != null,
                onClick = { sortExpanded = true },
                label = {
                    Text(
                        when (currentSortBy) {
                            "name" -> "Nombre A-Z"
                            "stock_desc" -> "Stock \u2193"
                            "precio_desc" -> "Precio \u2193"
                            else -> "Ordenar"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Sin ordenar") },
                    onClick = { onSortBy(null); sortExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Nombre A-Z") },
                    onClick = { onSortBy("name"); sortExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Stock \u2193") },
                    onClick = { onSortBy("stock_desc"); sortExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Precio \u2193") },
                    onClick = { onSortBy("precio_desc"); sortExpanded = false }
                )
            }
        }

        var marcaExpanded by remember { mutableStateOf(false) }
        Box {
            FilterChip(
                selected = currentFilterMarca != null,
                onClick = { marcaExpanded = true },
                label = {
                    Text(
                        currentFilterMarca ?: "Marca",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
            DropdownMenu(expanded = marcaExpanded, onDismissRequest = { marcaExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Todas") },
                    onClick = { onFilterMarca(null); marcaExpanded = false }
                )
                marcas.forEach { marca ->
                    DropdownMenuItem(
                        text = { Text(marca) },
                        onClick = { onFilterMarca(marca); marcaExpanded = false }
                    )
                }
            }
        }

        var materialExpanded by remember { mutableStateOf(false) }
        Box {
            FilterChip(
                selected = currentFilterMaterial != null,
                onClick = { materialExpanded = true },
                label = {
                    Text(
                        currentFilterMaterial ?: "Material",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
            DropdownMenu(expanded = materialExpanded, onDismissRequest = { materialExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Todos") },
                    onClick = { onFilterMaterial(null); materialExpanded = false }
                )
                materiales.forEach { mat ->
                    DropdownMenuItem(
                        text = { Text(mat) },
                        onClick = { onFilterMaterial(mat); materialExpanded = false }
                    )
                }
            }
        }

        FilterChip(
            selected = currentFilterStockBajo,
            onClick = onToggleStockBajo,
            label = { Text("Stock bajo", style = MaterialTheme.typography.labelSmall) }
        )

        val hasFilters = currentFilterMarca != null || currentFilterMaterial != null || currentFilterStockBajo
        if (hasFilters) {
            IconButton(onClick = onClearFilters) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Limpiar filtros",
                    tint = MaterialTheme.colorScheme.error
                )
            }
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
                    "No hay productos cr\u00EDticos por reposici\u00F3n.",
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
