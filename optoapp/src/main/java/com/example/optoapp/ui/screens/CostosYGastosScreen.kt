package com.example.optoapp.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.optoapp.ui.components.costo.CostoLenteCatalogForm
import com.example.optoapp.ui.components.common.GastoFormDialog
import com.example.optoapp.ui.components.common.GastoFormValues
import com.example.optoapp.ui.components.common.MonthSelector
import com.example.optoapp.ui.components.common.MonthSelectorState
import com.example.optoapp.ui.navigation.CostosTab
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.screens.costos.BiseladoTab
import com.example.optoapp.ui.screens.costos.CostosCharts
import com.example.optoapp.ui.screens.costos.CostosHeader
import com.example.optoapp.ui.screens.costos.EditarPrecioDialog
import com.example.optoapp.ui.screens.costos.FormBiseladoDialog
import com.example.optoapp.ui.screens.costos.FormLcDialog
import com.example.optoapp.ui.screens.costos.LcTab
import com.example.optoapp.ui.screens.costos.MatrizAgregarFab
import com.example.optoapp.ui.screens.costos.MatrizDropdown
import com.example.optoapp.ui.screens.costos.MatrizRow
import com.example.optoapp.ui.screens.costos.MatrizSoloLectura
import com.example.optoapp.ui.screens.costos.MatrizVacia
import com.example.optoapp.ui.screens.costos.ServicioTiposTab
import com.example.optoapp.ui.screens.costos.etiquetaMatriz
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.COST_BLOCKS
import com.example.optoapp.viewmodel.CostosHeaderUiState
import com.example.optoapp.viewmodel.CostosHeaderViewModel
import com.example.optoapp.viewmodel.CostosYGastosUiState
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
    headerViewModel: CostosHeaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val headerState by headerViewModel.uiState.collectAsState()
    LaunchedEffect(uiState.mesGastos) { headerViewModel.seleccionarMes(uiState.mesGastos) }
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(R.string.costos_tab_gastos),
        stringResource(R.string.costos_tab_lentes),
        stringResource(R.string.costos_tab_biselado),
        stringResource(R.string.costos_tab_lentes_contacto),
        stringResource(R.string.costos_tab_servicios),
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.costos_atras_cd))
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
            ScrollableTabRow(selectedTabIndex = uiState.selectedTab, edgePadding = 0.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title, fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                    )
                }
            }

            when (uiState.selectedTab) {
                CostosTab.GASTOS.ordinal -> GastosOperativosTab(
                    uiState = uiState,
                    headerState = headerState,
                    onVerOtsSinCosto = { navController.navigate(Route.OtsSinCosto.createRoute(headerState.mes)) },
                    viewModel = viewModel,
                    showDatePicker = { showDatePicker = true },
                )
                CostosTab.LENTES.ordinal -> MatrizDeCostosTab(uiState = uiState, viewModel = viewModel)
                CostosTab.BISELADO.ordinal -> BiseladoTab(
                    filas = uiState.costosBiselado,
                    puedeEditar = uiState.puedeEditarMatrices,
                    onEditar = viewModel::showEditBiselado,
                    onAgregar = viewModel::showNewBiselado,
                )
                CostosTab.LC.ordinal -> LcTab(
                    filas = uiState.costosLc,
                    puedeEditar = uiState.puedeEditarMatrices,
                    onEditar = viewModel::showEditLc,
                    onAgregar = viewModel::showNewLc,
                )
                CostosTab.SERVICIOS.ordinal -> ServicioTiposTab()
            }
        }
    }

    MatricesDialogs(uiState = uiState, viewModel = viewModel)
}

private data class EdicionPrecio(val descripcion: String, @StringRes val etiquetaCosto: Int, val guardar: () -> Unit)

@Composable
private fun MatricesDialogs(uiState: CostosYGastosUiState, viewModel: CostosYGastosViewModel) {
    val costo = uiState.editingCosto
    val biselado = uiState.editingBiselado
    val lc = uiState.editingLc
    val edicion = when {
        costo != null -> EdicionPrecio(
            "${costo.material} · ${costo.tipoLente}",
            R.string.costos_costo_unitario,
            viewModel::saveCostoEdit,
        )
        biselado != null -> EdicionPrecio(
            "${biselado.material} · ${etiquetaMatriz(biselado.tipoAro)}",
            R.string.costos_costo_por_par,
            viewModel::saveBiseladoEdit,
        )
        lc != null -> EdicionPrecio(
            "${etiquetaMatriz(lc.tipoLc)} · ${lc.materialLc}",
            R.string.costos_costo_unitario,
            viewModel::saveLcEdit,
        )
        else -> null
    }
    edicion?.let { (descripcion, etiquetaCosto, guardar) ->
        EditarPrecioDialog(
            descripcion = descripcion,
            etiquetaCosto = stringResource(etiquetaCosto),
            costo = uiState.nuevoCostoUnitario,
            onCostoChange = viewModel::updateNuevoCostoUnitario,
            vigenteDesde = uiState.vigenteDesde,
            onVigenteDesdeChange = viewModel::updateVigenteDesde,
            vigenteDesdeError = uiState.vigenteDesdeError,
            error = uiState.error,
            historial = uiState.historialPrecios,
            onGuardar = guardar,
            onDismiss = viewModel::dismissEditPrecio,
        )
    }
    uiState.formBiselado?.let { form ->
        FormBiseladoDialog(
            form = form,
            materiales = com.example.optoapp.viewmodel.MATERIALES_BISELADO,
            onCambio = viewModel::updateFormBiselado,
            onGuardar = viewModel::saveBiselado,
            onDismiss = viewModel::dismissFormBiselado,
        )
    }
    uiState.formLc?.let { form ->
        FormLcDialog(
            form = form,
            onCambio = viewModel::updateFormLc,
            onGuardar = viewModel::saveLc,
            onDismiss = viewModel::dismissFormLc,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatrizDeCostosTab(
    uiState: CostosYGastosUiState,
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

            if (!uiState.puedeEditarMatrices) {
                MatrizSoloLectura()
            }

            uiState.selectedBlock?.let { block ->
                if (uiState.costosDelBloque.isNotEmpty()) {
                    FiltrosLentes(uiState = uiState, viewModel = viewModel)
                }
                BloqueLentesCard(block = block, uiState = uiState, viewModel = viewModel)
            }

            uiState.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(80.dp))
        }

        if (uiState.selectedBlock != null && uiState.puedeEditarMatrices) {
            MatrizAgregarFab(onAgregar = viewModel::showNewCosto, modifier = Modifier.align(Alignment.BottomEnd))
        }

        if (uiState.isCostoDialogVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCostoDialog() },
                title = { Text(stringResource(R.string.costos_nuevo_titulo), fontWeight = FontWeight.Bold) },
                text = {
                    CostoLenteCatalogForm(
                        tipoLente = uiState.costoTipoLente,
                        subtipoBifocal = uiState.costoSubtipoBifocal,
                        material = uiState.costoMaterial,
                        tratamientos = uiState.costoTratamientos,
                        discromatopsiaTipo = uiState.costoDiscromatopsiaTipo,
                        curvaBase = uiState.costoCurvaBase,
                        indiceRefraccion = uiState.costoIndiceRefraccion,
                        serie = uiState.costoSerie,
                        costoUnitario = uiState.costoCostoUnitario,
                        stockOFabricacion = uiState.costoStockOFabricacion,
                        onTipoLenteChange = viewModel::updateCostoTipoLente,
                        onSubtipoBifocalChange = viewModel::updateCostoSubtipoBifocal,
                        onMaterialChange = viewModel::updateCostoMaterial,
                        onToggleTratamiento = viewModel::toggleCostoTratamiento,
                        onDiscromatopsiaTipoChange = viewModel::updateCostoDiscromatopsiaTipo,
                        onCurvaBaseChange = viewModel::updateCostoCurvaBase,
                        onIndiceRefraccionChange = viewModel::updateCostoIndiceRefraccion,
                        onStockOFabricacionChange = viewModel::updateCostoStockOFabricacion,
                        onSerieChange = viewModel::updateCostoSerie,
                        onCostoUnitarioChange = viewModel::updateCostoCostoUnitario,
                    )
                    uiState.costoSaveError?.let { e ->
                        Text(e, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
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
    uiState: CostosYGastosUiState,
    headerState: CostosHeaderUiState,
    onVerOtsSinCosto: () -> Unit,
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

            item { CostosHeader(state = headerState, onVerOtsSinCosto = onVerOtsSinCosto) }

            item { CostosCharts(facts = headerState.facts) }

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
                    val esDerivadoOt = fila.gasto.origenDispensacionItemId != null
                    GastoOperativoCard(
                        fila = fila,
                        // WHY: Linked OT bevel rows are managed from dispensación, not this matrix screen.
                        puedeEditar = uiState.puedeEditarGastos && !esDerivadoOt,
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
        GastoFormDialog(
            values = GastoFormValues(
                editando = uiState.editingGasto != null,
                categoria = uiState.categoria,
                tipoGasto = uiState.tipoGasto,
                monto = uiState.monto,
                descripcion = uiState.descripcion,
                fecha = uiState.fecha,
                nota = uiState.nota,
                isRecurring = uiState.isRecurring,
                error = uiState.gastoFormError,
            ),
            categorias = viewModel.categorias,
            onCategoria = viewModel::updateCategoria,
            onTipoGasto = viewModel::updateTipoGasto,
            onMonto = viewModel::updateMonto,
            onDescripcion = viewModel::updateDescripcion,
            onElegirFecha = showDatePicker,
            onNota = viewModel::updateNota,
            onToggleRecurrente = viewModel::toggleRecurrente,
            onGuardar = viewModel::saveGasto,
            onDismiss = viewModel::dismissDialog,
        )
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
private fun FiltrosLentes(uiState: CostosYGastosUiState, viewModel: CostosYGastosViewModel) {
    val costos = uiState.costosDelBloque
    val todos = stringResource(R.string.costos_filtro_todos)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        MatrizDropdown(
            label = stringResource(R.string.costos_filtro_material),
            seleccion = uiState.filtroMaterial,
            opciones = listOf(null) + costos.map { it.material }.distinct().sorted(),
            etiqueta = { it ?: todos },
            onSeleccion = viewModel::updateFiltroMaterial,
            modifier = Modifier.fillMaxWidth(),
        )
        MatrizDropdown(
            label = stringResource(R.string.costos_filtro_tipo_lente),
            seleccion = uiState.filtroTipoLente,
            opciones = listOf(null) + costos.map { it.tipoLente }.distinct().sorted(),
            etiqueta = { it ?: todos },
            onSeleccion = viewModel::updateFiltroTipoLente,
            modifier = Modifier.fillMaxWidth(),
        )
        MatrizDropdown(
            label = stringResource(R.string.costos_filtro_serie),
            seleccion = uiState.filtroSerie,
            opciones = listOf(null) + costos.mapNotNull { it.serie }.distinct().sorted(),
            etiqueta = { it?.toString() ?: todos },
            onSeleccion = viewModel::updateFiltroSerie,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BloqueLentesCard(block: String, uiState: CostosYGastosUiState, viewModel: CostosYGastosViewModel) {
    val costos = uiState.costosVisibles
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
            when {
                costos.isNotEmpty() -> {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    costos.forEach { costo ->
                        CostoLenteRow(
                            costo = costo,
                            puedeEditar = uiState.puedeEditarMatrices,
                            onEditar = { viewModel.showEditCosto(costo) },
                            onEliminar = { viewModel.confirmDeleteCosto(costo) },
                        )
                    }
                }
                uiState.isLoading -> Unit
                uiState.costosDelBloque.isEmpty() -> MatrizVacia(uiState.puedeEditarMatrices, viewModel::showNewCosto)
                else -> Text(
                    stringResource(R.string.costos_filtro_sin_resultados),
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CostoLenteRow(costo: CostoProductoEntity, puedeEditar: Boolean, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val detalle = listOfNotNull(
        costo.tratamiento?.takeIf { it.isNotBlank() },
        costo.serie?.let { stringResource(R.string.costos_serie_corta, it) },
        costo.subtipoBifocal?.takeIf { it.isNotBlank() },
        costo.indiceRefraccion?.takeIf { it.isNotBlank() }?.let { "IR $it" },
        costo.curvaBase?.takeIf { it.isNotBlank() }?.let { "CB $it" },
    ).joinToString(" · ")
    MatrizRow(
        titulo = "${costo.material} · ${costo.tipoLente}",
        detalle = detalle,
        costo = costo.costoUnitario,
        onClick = if (puedeEditar) onEditar else null,
    ) {
        if (puedeEditar) {
            Spacer(Modifier.width(4.dp))
            IconButton(modifier = Modifier.size(48.dp), onClick = onEliminar) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.costos_eliminar_cd),
                    tint = MaterialTheme.colorScheme.alertRed,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
