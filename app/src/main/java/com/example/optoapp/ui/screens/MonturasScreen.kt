package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.Montura
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.util.FileShareUtils
import com.example.optoapp.util.InventarioMonturasPdfGenerator
import com.example.optoapp.viewmodel.MonturasViewModel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonturasScreen(
    navController: NavController,
    viewModel: MonturasViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val monturas by viewModel.monturas.collectAsState()
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!uiState.error.isNullOrBlank()) {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OptoTextField(
                        value = uiState.form.sku, 
                        onValueChange = { v -> viewModel.updateForm { it.copy(sku = v) } }, 
                        label = "SKU * (Código único)",
                        isError = uiState.error?.contains("SKU", ignoreCase = true) == true
                    )
                    OptoTextField(
                        value = uiState.form.marca, 
                        onValueChange = { v -> viewModel.updateForm { it.copy(marca = v) } }, 
                        label = "Marca / Fabricante *",
                        isError = uiState.error?.contains("Marca", ignoreCase = true) == true
                    )
                    OptoTextField(
                        value = uiState.form.modelo, 
                        onValueChange = { v -> viewModel.updateForm { it.copy(modelo = v) } }, 
                        label = "Modelo / Nombre Producto *",
                        isError = uiState.error?.contains("Modelo", ignoreCase = true) == true
                    )
                    OptoTextField(uiState.form.color, { v -> viewModel.updateForm { it.copy(color = v) } }, "Color / Variedad")
                    OptoTextField(uiState.form.talla, { v -> viewModel.updateForm { it.copy(talla = v) } }, "Talla / Tamaño")
                    DropdownField(label = "Tipo de Aro *", selected = uiState.form.tipoAro, options = listOf("Aro Completo", "Semi al aire", "Al aire")) { opt ->
                        viewModel.updateForm { it.copy(tipoAro = opt) }
                    }
                    DropdownField(label = "Material *", selected = uiState.form.materialMontura, options = listOf("Acetato", "Metal", "Carey", "Econ")) { opt ->
                        viewModel.updateForm { it.copy(materialMontura = opt) }
                    }
                    OptoTextField(uiState.form.costo, { v -> viewModel.updateForm { it.copy(costo = v) } }, "Costo unitario", keyboardType = KeyboardType.Decimal)
                    OptoTextField(uiState.form.precio, { v -> viewModel.updateForm { it.copy(precio = v) } }, "Precio venta", keyboardType = KeyboardType.Decimal)
                    OptoTextField(uiState.form.stockActual, { v -> viewModel.updateForm { it.copy(stockActual = v) } }, "Stock inicial", keyboardType = KeyboardType.Number)
                    OptoTextField(uiState.form.stockMinimo, { v -> viewModel.updateForm { it.copy(stockMinimo = v) } }, "Alerta stock mínimo", keyboardType = KeyboardType.Number)
                    
                    Text("* Campos obligatorios", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.save() }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { viewModel.cancelEdit() }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario General") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startCreate() }) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo producto")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OptoTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = "Buscar por SKU, marca o modelo"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (porReponer.isEmpty()) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Alertas de stock bajo: ${porReponer.size}",
                        fontWeight = FontWeight.Bold,
                        color = if (porReponer.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    if (porReponer.isEmpty()) {
                        Text("No hay productos críticos por reposición.")
                    } else {
                        porReponer.take(5).forEach { m ->
                            Text("- ${m.sku} ${m.marca} ${m.modelo}: ${m.stockActual}/${m.stockMinimo}")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Resumen inventario", fontWeight = FontWeight.Bold)
                    Text("Productos listados: ${filtradas.size}")
                    Text("Stock total: $stockTotal")
                    Text("Valor costo: s/. ${String.format(Locale.getDefault(), "%.2f", valorCosto)}")
                    Text("Valor venta: s/. ${String.format(Locale.getDefault(), "%.2f", valorVenta)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val pdf = InventarioMonturasPdfGenerator.generate(context, filtradas)
                                lastGeneratedPdf = pdf
                                FileShareUtils.openPdf(context, pdf, "Abrir reporte de inventario")
                            }
                        ) {
                            Text("Generar PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                val file = lastGeneratedPdf ?: InventarioMonturasPdfGenerator.generate(context, filtradas)
                                lastGeneratedPdf = file
                                FileShareUtils.sharePdf(context, file, "Compartir reporte de inventario")
                            }
                        ) {
                            Text("Compartir PDF")
                        }
                    }
                }
            }

            if (!uiState.error.isNullOrBlank()) {
                Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
            }
            if (!uiState.success.isNullOrBlank()) {
                Text(uiState.success ?: "", color = MaterialTheme.colorScheme.tertiary)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (porReponer.isNotEmpty()) {
                    item {
                        Text(
                            "Por reponer",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    items(porReponer, key = { "low-${it.id}" }) { m ->
                        MonturaItem(
                            montura = m,
                            onEdit = { viewModel.startEdit(m) },
                            onDelete = { viewModel.delete(m) },
                            onEntrada = { viewModel.registrarEntrada(m, 1) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Todos los productos",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                items(restantes, key = { it.id }) { m ->
                    MonturaItem(
                        montura = m,
                        onEdit = { viewModel.startEdit(m) },
                        onDelete = { viewModel.delete(m) },
                        onEntrada = { viewModel.registrarEntrada(m, 1) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonturaItem(
    montura: Montura,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEntrada: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${montura.marca} ${montura.modelo}", fontWeight = FontWeight.Bold)
                    Text("SKU: ${montura.sku}")
                }
                Row {
                    IconButton(onClick = onEntrada) { Icon(Icons.Default.Add, contentDescription = "Entrada +1") }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
                }
            }
            Text("Color/Talla: ${montura.color.ifBlank { "-" }} / ${montura.talla.ifBlank { "-" }}")
            Text("Costo: s/. ${String.format(Locale.getDefault(), "%.2f", montura.costo)}")
            Text("Precio: s/. ${String.format(Locale.getDefault(), "%.2f", montura.precio)}")
            Text("Stock: ${montura.stockActual}  |  Mínimo: ${montura.stockMinimo}")
            if (montura.stockActual <= montura.stockMinimo) {
                Text("Stock bajo", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar ${montura.marca} ${montura.modelo}?") },
            confirmButton = {
                Button(onClick = {
                    onDelete()
                    showDelete = false
                }) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDelete = false }) { Text("Cancelar") }
            }
        )
    }
}
