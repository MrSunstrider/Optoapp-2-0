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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.R
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.domain.GastoTipoResolver
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.common.MonthSelector
import com.example.optoapp.ui.components.common.MonthSelectorState
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.COST_BLOCKS
import com.example.optoapp.viewmodel.CostosYGastosViewModel
import com.example.optoapp.viewmodel.GastoFila
import com.example.optoapp.viewmodel.GrupoGastos
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostosYGastosScreen(
    navController: NavController,
    drawerState: DrawerState,
    viewModel: CostosYGastosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(R.string.costos_tab_lentes),
        stringResource(R.string.costos_tab_biselado),
        stringResource(R.string.costos_tab_lentes_contacto),
        stringResource(R.string.costos_tab_gastos),
    )
    var showDatePicker by remember { mutableStateOf(false) }

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
                title = stringResource(R.string.costos_titulo),
                navigationIcon = {
                    IconButton(onClick = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            scope.launch { drawerState.open() }
                        }
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.costos_menu_cd))
                    }
                },
            )
        },
    ) { padding ->
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
                0 -> MatrizDeCostosTab(uiState = uiState, viewModel = viewModel)
                1 -> Text(stringResource(R.string.costos_biselado_proximamente), modifier = Modifier.padding(16.dp))
                2 -> Text(stringResource(R.string.costos_lentes_contacto_proximamente), modifier = Modifier.padding(16.dp))
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
                    value = uiState.selectedBlock ?: stringResource(R.string.costos_seleccionar_bloque),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.costos_bloque_label)) },
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
                        Text(stringResource(R.string.costos_bloque_titulo, block), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            pluralStringResource(R.plurals.costos_registros, costos.size, costos.size),
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
                                stringResource(R.string.costos_bloque_vacio),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                    title = { Text(stringResource(R.string.costos_editar_titulo), fontWeight = FontWeight.Bold) },
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
                                label = { Text(stringResource(R.string.costos_costo_unitario)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            uiState.error?.let { e ->
                                Text(e, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = { Button(onClick = { viewModel.saveCostoEdit() }) { Text(stringResource(R.string.common_save)) } },
                    dismissButton = { TextButton(onClick = { viewModel.dismissEditCosto() }) { Text(stringResource(R.string.common_cancel)) } },
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.costos_anadir_cd))
            }
        }

        if (uiState.isCostoDialogVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCostoDialog() },
                title = { Text(stringResource(R.string.costos_nuevo_titulo), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        var materialExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = materialExpanded, onExpandedChange = { materialExpanded = !materialExpanded }) {
                            OutlinedTextField(
                                value = uiState.costoMaterial,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.costos_material_requerido)) },
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
                                label = { Text(stringResource(R.string.costos_tipo_lente_requerido)) },
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

                        OutlinedTextField(
                            value = uiState.selectedBlock ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.costos_stock_fabricacion)) },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        var tratExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = tratExpanded, onExpandedChange = { tratExpanded = !tratExpanded }) {
                            OutlinedTextField(
                                value = uiState.costoTratamiento,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.costos_tratamiento_opcional)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tratExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            )
                            ExposedDropdownMenu(expanded = tratExpanded, onDismissRequest = { tratExpanded = false }) {
                                val ningunoLabel = stringResource(R.string.costos_tratamiento_ninguno)
                                viewModel.tratamientos.forEach { tr ->
                                    val display = tr.ifBlank { ningunoLabel }
                                    DropdownMenuItem(text = { Text(display) }, onClick = {
                                        viewModel.updateCostoTratamiento(tr)
                                        tratExpanded = false
                                    })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.costoSerie,
                            onValueChange = { viewModel.updateCostoSerie(it) },
                            label = { Text(stringResource(R.string.costos_serie_opcional)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = uiState.costoCostoUnitario,
                            onValueChange = { viewModel.updateCostoCostoUnitario(it) },
                            label = { Text(stringResource(R.string.costos_costo_unitario_requerido)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        uiState.costoSaveError?.let { e ->
                            Text(e, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = { Button(onClick = { viewModel.saveCosto() }) { Text(stringResource(R.string.common_save)) } },
                dismissButton = { TextButton(onClick = { viewModel.dismissCostoDialog() }) { Text(stringResource(R.string.common_cancel)) } },
            )
        }

        uiState.deletingCosto?.let { costo ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                title = { Text(stringResource(R.string.costos_eliminar_titulo), fontWeight = FontWeight.Bold) },
                text = {
                    Text(stringResource(R.string.costos_eliminar_confirmacion, costo.material, costo.tipoLente))
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteCosto() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.alertRed),
                    ) { Text(stringResource(R.string.common_delete)) }
                },
                dismissButton = { TextButton(onClick = { viewModel.dismissDeleteDialog() }) { Text(stringResource(R.string.common_cancel)) } },
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
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                MonthSelector(
                    state = MonthSelectorState(uiState.mesGastos, YearMonth.from(DateUtils.today())),
                    onMonthSelected = viewModel::selectMesGastos,
                )
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
                        Text(stringResource(R.string.gastos_total_mes), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            MoneyFormatter.format(uiState.totalGastosMes.toDouble()),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (!uiState.puedeEditarGastos) {
                item {
                    Text(
                        stringResource(R.string.gastos_solo_lectura),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.gruposGastos.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Decorative because the message below already announces the empty month to screen readers.
                        Icon(
                            Icons.Default.MoneyOff,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.gastos_vacio_mes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (uiState.puedeEditarGastos) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.showNewGasto() }) {
                                Text(stringResource(R.string.gastos_agregar))
                            }
                        }
                    }
                }
            }

            uiState.gruposGastos.forEach { grupo ->
                item(key = "grupo-${grupo.categoria}") {
                    GrupoGastosHeader(grupo)
                }
                items(grupo.filas, key = { it.gasto.id }) { fila ->
                    GastoOperativoCard(
                        fila = fila,
                        puedeEditar = uiState.puedeEditarGastos,
                        onEdit = { viewModel.editGasto(fila.gasto) },
                        onDelete = { viewModel.confirmDeleteGasto(fila.gasto) },
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        if (uiState.puedeEditarGastos) {
            FloatingActionButton(
                onClick = { viewModel.showNewGasto() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .navigationBarsPadding(),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.gastos_agregar))
            }
        }
    }

    if (uiState.isDialogVisible) {
        GastoFormularioDialog(uiState = uiState, viewModel = viewModel, showDatePicker = showDatePicker)
    }

    uiState.deletingGasto?.let { gasto ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteGasto() },
            title = { Text(stringResource(R.string.analisis_eliminar_gasto_titulo), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.analisis_eliminar_gasto_confirmacion,
                            gasto.descripcion?.takeIf { it.isNotBlank() } ?: gasto.categoria,
                            MoneyFormatter.format(gasto.monto.toDouble()),
                        ),
                    )
                    uiState.gastoDeleteError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteGasto() },
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.alertRed),
                ) { Text(stringResource(R.string.analisis_eliminar)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteGasto() }) { Text(stringResource(R.string.analisis_cancelar)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GastoFormularioDialog(
    uiState: com.example.optoapp.viewmodel.CostosYGastosUiState,
    viewModel: CostosYGastosViewModel,
    showDatePicker: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissDialog() },
        title = {
            val tituloRes = if (uiState.editingGasto != null) R.string.analisis_editar_gasto else R.string.analisis_nuevo_gasto
            Text(stringResource(tituloRes), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = uiState.categoria,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.analisis_categoria)) },
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
                OutlinedTextField(
                    value = uiState.monto,
                    onValueChange = { viewModel.updateMonto(it) },
                    label = { Text(stringResource(R.string.analisis_monto)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.descripcion,
                    onValueChange = { viewModel.updateDescripcion(it) },
                    label = { Text(stringResource(R.string.analisis_descripcion)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { showDatePicker() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(DateUtils.formatLocalized(uiState.fecha))
                }
                OutlinedTextField(
                    value = uiState.nota,
                    onValueChange = { viewModel.updateNota(it) },
                    label = { Text(stringResource(R.string.gastos_nota)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = uiState.isRecurring, onCheckedChange = { viewModel.toggleRecurrente() })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.analisis_gasto_recurrente), fontSize = 13.sp)
                }
                uiState.gastoFormError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.saveGasto() }, enabled = !uiState.isLoading) {
                Text(stringResource(R.string.analisis_guardar))
            }
        },
        dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text(stringResource(R.string.analisis_cancelar)) } },
    )
}

@Composable
private fun GrupoGastosHeader(grupo: GrupoGastos) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(grupo.etiqueta, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(MoneyFormatter.format(grupo.subtotal.toDouble()), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GastoOperativoCard(fila: GastoFila, puedeEditar: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val gasto = fila.gasto
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tipoRes = if (fila.tipo == GastoTipoResolver.FIJO) R.string.gastos_tipo_fijo else R.string.gastos_tipo_variable
                    Text(stringResource(tipoRes), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (gasto.isRecurring) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Text(
                                stringResource(R.string.analisis_gasto_recurrente_chip),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (!gasto.descripcion.isNullOrBlank()) {
                    Text(gasto.descripcion, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(DateUtils.formatLocalized(gasto.fecha), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                MoneyFormatter.format(gasto.monto.toDouble()),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.alertRed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (puedeEditar) {
                Spacer(Modifier.width(8.dp))
                IconButton(modifier = Modifier.size(48.dp), onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.gastos_editar_cd), modifier = Modifier.size(18.dp))
                }
                IconButton(modifier = Modifier.size(48.dp), onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.gastos_eliminar_cd),
                        tint = MaterialTheme.colorScheme.alertRed,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
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
            val serieDetalle = costo.serie?.let { stringResource(R.string.costos_serie_detalle, it) }.orEmpty()
            val specs = costo.tratamiento.orEmpty() + serieDetalle
            if (specs.isNotEmpty()) {
                Text(specs, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                MoneyFormatter.format(costo.costoUnitario),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.width(4.dp))
            IconButton(modifier = Modifier.size(48.dp), onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.costos_eliminar_cd),
                    tint = MaterialTheme.colorScheme.alertRed,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
    HorizontalDivider()
}
