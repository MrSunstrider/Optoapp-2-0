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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.theme.OptoTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.ui.components.OptoDialog
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.monturas.MonturaEditForm
import com.example.optoapp.ui.components.monturas.MonturaListSection
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
        OptoDialog(
            onDismissRequest = { viewModel.cancelEdit() },
            title = if (uiState.form.id == null) "Nuevo Producto" else "Editar Producto",
            confirmText = "Guardar",
            onConfirm = { viewModel.save() },
            dismissText = "Cancelar",
            content = {
                MonturaEditForm(
                    form = uiState.form,
                    onUpdate = { newForm -> viewModel.updateForm { newForm } },
                    error = uiState.error
                )
            }
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
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)
        ) {
            OptoTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = "Buscar por SKU, marca o modelo"
            )

            if (monturas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No hay monturas en inventario",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (canEdit) {
                            Button(onClick = { viewModel.startCreate() }) {
                                Text("Añadir primer producto")
                            }
                        }
                    }
                }
            } else {

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
                Column(modifier = Modifier.padding(OptoTokens.spacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Column(modifier = Modifier.padding(OptoTokens.spacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Resumen inventario", fontWeight = FontWeight.Bold)
                    Text("Productos listados: ${filtradas.size}")
                    Text("Stock total: $stockTotal")
                    Text("Valor costo: s/. ${String.format(Locale.getDefault(), "%.2f", valorCosto)}")
                    Text("Valor venta: s/. ${String.format(Locale.getDefault(), "%.2f", valorVenta)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)) {
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
}
