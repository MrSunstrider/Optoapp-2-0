package com.example.optoapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.COST_BLOCKS
import com.example.optoapp.viewmodel.CostosGastosUiPolicy
import com.example.optoapp.viewmodel.CostosYGastosViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostosYGastosScreen(
    navController: NavController,
    drawerState: DrawerState,
    dispensacionId: String? = null,
    initialTab: Int = 0,
    viewModel: CostosYGastosViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val tabs = listOf("Lentes", "Biselado", "Lentes Contacto", "Gastos Operativos")
    var showDatePicker by remember { mutableStateOf(false) }
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = null)
    val access = CostosGastosUiPolicy.resolveAccess(opticaRol)
    val canView = !access.isRestricted

    LaunchedEffect(initialTab) {
        viewModel.selectTab(initialTab)
    }

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = uiState.fecha,
            onDateSelected = { viewModel.updateFecha(it) },
            onDismiss = { showDatePicker = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = "Costos y Gastos",
                navigationIcon = {
                    IconButton(onClick = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            scope.launch { drawerState.open() }
                        }
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
            )
        },
    ) { padding ->
        if (opticaRol == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            return@Scaffold
        }

        if (!canView) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Bloqueado", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Acceso restringido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Tu rol no tiene permiso para ver esta sección.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
        ) {
            TabRow(selectedTabIndex = uiState.selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title, fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                    )
                }
            }

            when (uiState.selectedTab) {
                0 -> MatrizDeCostosTab(uiState = uiState, viewModel = viewModel, dispensacionId = dispensacionId)
                1 -> Text("Biselado — próximamente", modifier = Modifier.padding(16.dp))
                2 -> Text("Lentes de Contacto — próximamente", modifier = Modifier.padding(16.dp))
                3 -> GastosOperativosTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    showDatePicker = { showDatePicker = true },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatrizDeCostosTab(
    uiState: com.example.optoapp.viewmodel.CostosYGastosUiState,
    viewModel: CostosYGastosViewModel,
    dispensacionId: String?,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = uiState.selectedBlock ?: "Seleccionar bloque",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bloque de Costos") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    COST_BLOCKS.forEach { block ->
                        DropdownMenuItem(
                            text = { Text(block) },
                            onClick = {
                                viewModel.loadBlock(block)
                                expanded = false
                            },
                        )
                    }
                }
            }

            uiState.selectedBlock?.let { block ->
                val costos = uiState.costosDelBloque
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bloque: $block", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${costos.size} registros",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (costos.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            costos.forEach { costo ->
                                CostoProductoRow(
                                    costo,
                                    onClick = { viewModel.showEditCosto(costo) },
                                    onDelete = { viewModel.confirmDeleteCosto(costo) },
                                )
                            }
                        } else if (!uiState.isLoading) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No hay costos registrados en este bloque.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (!dispensacionId.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, contentDescription = "Recibo", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Costos de la Orden", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Dispensación #${dispensacionId.take(8)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            uiState.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(80.dp))

        
            uiState.editingCosto?.let { costo ->
                var editValue by remember { mutableStateOf(uiState.nuevoCostoUnitario) }
                AlertDialog(
                    onDismissRequest = { viewModel.dismissEditCosto() },
                    title = { Text("Editar Costo", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "${costo.material} · ${costo.tipoLente}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = editValue,
                                onValueChange = {
                                    editValue = it
                                    viewModel.updateNuevoCostoUnitario(it)
                                },
                                label = { Text("Costo unitario") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            uiState.error?.let { e ->
                                Text(e, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = { Button(onClick = { viewModel.saveCostoEdit() }) { Text("Guardar") } },
                    dismissButton = { TextButton(onClick = { viewModel.dismissEditCosto() }) { Text("Cancelar") } },
                )
            }
        }

        if (uiState.selectedBlock != null) {
            FloatingActionButton(
                onClick = { viewModel.showNewCosto() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .navigationBarsPadding(),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Costo")
            }
        }

    
        if (uiState.isCostoDialogVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCostoDialog() },
                title = { Text("Nuevo Costo", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        var materialExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = materialExpanded, onExpandedChange = { materialExpanded = !materialExpanded }) {
                            OutlinedTextField(
                                value = uiState.costoMaterial,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Material *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = materialExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            )
                            ExposedDropdownMenu(expanded = materialExpanded, onDismissRequest = { materialExpanded = false }) {
                                viewModel.materialesOpticos.forEach { mat ->
                                    DropdownMenuItem(text = { Text(mat) }, onClick = {
                                        viewModel.updateCostoMaterial(mat)
                                        materialExpanded = false
                                    })
                                }
                            }
                        }

                        var tipoExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = tipoExpanded, onExpandedChange = { tipoExpanded = !tipoExpanded }) {
                            OutlinedTextField(
                                value = uiState.costoTipoLente,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de lente *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            )
                            ExposedDropdownMenu(expanded = tipoExpanded, onDismissRequest = { tipoExpanded = false }) {
                                viewModel.tiposLente.forEach { tl ->
                                    DropdownMenuItem(text = { Text(tl) }, onClick = {
                                        viewModel.updateCostoTipoLente(tl)
                                        tipoExpanded = false
                                    })
                                }
                            }
                        }

                        // Stock/Fabricacion — auto-filled from selected block, read-only
                        OutlinedTextField(
                            value = uiState.selectedBlock ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Stock o fabricación") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Tratamiento dropdown
                        var tratExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = tratExpanded, onExpandedChange = { tratExpanded = !tratExpanded }) {
                            OutlinedTextField(
                                value = uiState.costoTratamiento,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tratamiento (opcional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tratExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            )
                            ExposedDropdownMenu(expanded = tratExpanded, onDismissRequest = { tratExpanded = false }) {
                                viewModel.tratamientos.forEach { tr ->
                                    val display = tr.ifBlank { "Ninguno" }
                                    DropdownMenuItem(text = { Text(display) }, onClick = {
                                        viewModel.updateCostoTratamiento(tr)
                                        tratExpanded = false
                                    })
                                }
                            }
                        }

                        // Serie
                        OutlinedTextField(
                            value = uiState.costoSerie,
                            onValueChange = { viewModel.updateCostoSerie(it) },
                            label = { Text("Serie (opcional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Costo unitario
                        OutlinedTextField(
                            value = uiState.costoCostoUnitario,
                            onValueChange = { viewModel.updateCostoCostoUnitario(it) },
                            label = { Text("Costo unitario *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        uiState.costoSaveError?.let { e ->
                            Text(e, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = { Button(onClick = { viewModel.saveCosto() }) { Text("Guardar") } },
                dismissButton = { TextButton(onClick = { viewModel.dismissCostoDialog() }) { Text("Cancelar") } },
            )
        }

    
        uiState.deletingCosto?.let { costo ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                title = { Text("Eliminar costo", fontWeight = FontWeight.Bold) },
                text = {
                    Text("¿Eliminar este costo de ${costo.material} · ${costo.tipoLente}?")
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteCosto() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.alertRed),
                    ) { Text("Eliminar") }
                },
                dismissButton = { TextButton(onClick = { viewModel.dismissDeleteDialog() }) { Text("Cancelar") } },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GastosOperativosTab(
    uiState: com.example.optoapp.viewmodel.CostosYGastosUiState,
    viewModel: CostosYGastosViewModel,
    showDatePicker: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val gastos = uiState.gastosOperativos
    val triad = CostosGastosUiPolicy.resolveGastosTriad(
        isLoading = uiState.gastosLoading,
        gastosCount = gastos.size,
        errorMessage = uiState.gastosError,
    )
    val totalMes = gastos
        .filter { it.fecha.month == java.time.LocalDate.now().month && it.fecha.year == java.time.LocalDate.now().year }
        .sumOf { it.monto }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                ) {
                    Text(
                        "⚠️ Esta sección reemplaza la pantalla Gastos anterior. Ahora los gastos se gestionan desde Costos y Gastos.",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Total del mes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("s/. ${fmt(totalMes.toDouble())}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (triad.showsLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                }
            }

            if (triad.showsError) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                uiState.gastosError.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                            )
                            if (triad.showsRetry) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { viewModel.retryGastos() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                }
            }

            if (triad.showsEmpty) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MoneyOff, contentDescription = "Sin costo", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Spacer(Modifier.height(8.dp))
                            Text("Sin gastos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (!triad.showsLoading && !triad.showsError) {
                items(gastos.sortedByDescending { it.fecha }) { gasto ->
                    GastoOperativoCard(
                        gasto = gasto,
                        onEdit = { viewModel.editGasto(gasto) },
                        onDelete = { viewModel.deleteGasto(gasto) },
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { viewModel.showNewGasto() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding(),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir Gasto")
        }
    }

    if (uiState.isDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text(if (uiState.editingGasto != null) "Editar Gasto" else "Nuevo Gasto", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = uiState.categoria,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            viewModel.categorias.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = {
                                    viewModel.updateCategoria(cat)
                                    expanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(value = uiState.monto, onValueChange = { viewModel.updateMonto(it) }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = uiState.descripcion, onValueChange = { viewModel.updateDescripcion(it) }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { showDatePicker() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DateRange, contentDescription = "Fecha", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(DateUtils.formatLocalized(uiState.fecha))
                    }
                    OutlinedTextField(value = uiState.nota, onValueChange = { viewModel.updateNota(it) }, label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth())

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = uiState.isRecurring, onCheckedChange = { viewModel.toggleRecurrente() })
                        Spacer(Modifier.width(8.dp))
                        Text("Gasto recurrente mensual", fontSize = 13.sp)
                    }

                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = { Button(onClick = { viewModel.saveGasto() }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun GastoOperativoCard(gasto: GastoOperativoEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(gasto.categoria, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (!gasto.descripcion.isNullOrBlank()) {
                    Text(gasto.descripcion, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(DateUtils.formatLocalized(gasto.fecha), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            Text("s/. ${fmt(gasto.monto.toDouble())}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.alertRed, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            IconButton(modifier = Modifier.size(48.dp), onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
            }
            IconButton(modifier = Modifier.size(48.dp), onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.alertRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun fmt(value: Double): String = if (value == value.toLong().toDouble()) {
    String.format(Locale.getDefault(), "%,.0f", value)
} else {
    String.format(Locale.getDefault(), "%,.2f", value)
}

@Composable
private fun CostoProductoRow(costo: CostoProductoEntity, onClick: () -> Unit = {}, onDelete: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onClick),
        ) {
            Text(
                "${costo.material} · ${costo.tipoLente}",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val specs = buildString {
                costo.tratamiento?.let { append(it) }
                costo.serie?.let { append(" · Serie $it") }
            }
            if (specs.isNotEmpty()) {
                Text(specs, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "s/. ${fmt(costo.costoUnitario)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.positiveGreen,
            )
            Spacer(Modifier.width(4.dp))
            IconButton(modifier = Modifier.size(40.dp), onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.alertRed, modifier = Modifier.size(18.dp))
            }
        }
    }
    HorizontalDivider()
}
