package com.example.optoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.theme.LocalOptoDensity
import com.example.optoapp.domain.estadoAfterFechaEntrega
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.ui.components.FechaEntregaEditButton
import com.example.optoapp.ui.components.OptoDatePickerDialog
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.PatientContextCard
import com.example.optoapp.ui.components.WizardStepHeader
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.components.dispensacion.LenteForm
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.DispensacionItemUi
import com.example.optoapp.viewmodel.DispensacionViewModel
import java.time.LocalDate

internal fun wizardStepsForMode(isEditMode: Boolean): List<String> =
    if (isEditMode) listOf("Orden", "Productos", "Gestión") else listOf("Orden", "Productos")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaDispensacionScreen(navController: NavController, pacienteId: String, dispensacionId: String? = null, viewModel: DispensacionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val monturasActivas by viewModel.monturasActivas.collectAsState()
    val expandedItems = remember { mutableStateMapOf<Int, Boolean>() }
    var showDatePicker by remember { mutableStateOf(false) }
    var evaluacionExpanded by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }
    val isEditMode = dispensacionId != null
    val wizardSteps = remember(isEditMode) { wizardStepsForMode(isEditMode) }
    val lastStepIndex = wizardSteps.lastIndex

    val density = LocalOptoDensity.current

    LaunchedEffect(dispensacionId) {
        if (dispensacionId != null) {
            viewModel.loadDispensacion(dispensacionId)
        }
    }
    LaunchedEffect(pacienteId) {
        viewModel.loadPacienteNombre(pacienteId)
        viewModel.loadEvaluacionesDisponibles(pacienteId)
        viewModel.loadUltimaEvaluacionParaTicket(pacienteId)
    }

    val selectedEvaluacion = remember(uiState.evaluacionId, uiState.evaluacionesDisponibles) {
        uiState.evaluacionesDisponibles.find { it.id == uiState.evaluacionId }
    }

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = uiState.fecha,
            onDateSelected = { date ->
                viewModel.updateUiState { it.copy(fecha = date) }
            },
            onDismiss = { showDatePicker = false },
        )
    }

    val saveAction = {
        viewModel.saveDispensacion(pacienteId, dispensacionId) {
            if (dispensacionId == null) {
                navController.navigate(Route.InformacionFinanciera(viewModel.uiState.value.generatedId).route) {
                    popUpTo(Route.NuevaDispensacion(pacienteId).route) { inclusive = true }
                }
            } else {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        modifier = Modifier.testTag(TestTags.DISPENSACION_SCREEN_ROOT),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            OptoTopAppBar(
                title = if (dispensacionId == null) "Nueva Dispensación" else "Editar Dispensación",
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (currentStep == lastStepIndex) {
                        IconButton(
                            onClick = { saveAction() },
                            enabled = !uiState.isLoading,
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = density.screenPadding, vertical = density.blockGap),
                horizontalArrangement = Arrangement.spacedBy(density.sectionGap),
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f),
                    ) { Text("Anterior") }
                }
                if (currentStep < lastStepIndex) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier.weight(1f),
                    ) { Text("Siguiente") }
                } else {
                    Button(
                        onClick = { saveAction() },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.weight(1f).testTag(TestTags.DISPENSACION_GUARDAR_BTN),
                    ) {
                        Text(
                            when {
                                isEditMode -> "Actualizar Orden"
                                else -> "Crear Orden"
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = density.screenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(density.blockGap),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(density.blockGap))

            WizardStepHeader(
                labels = wizardSteps,
                currentStep = currentStep,
                totalSteps = wizardSteps.size,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(density.blockGap))

            if (uiState.pacienteNombre.isNotBlank()) {
                PatientContextCard(
                    pacienteNombre = uiState.pacienteNombre,
                    ot = uiState.ot.ifBlank { null },
                    fecha = uiState.fecha,
                )
            }

            when (currentStep) {
                0 -> StepOrden(
                    uiState = uiState,
                    viewModel = viewModel,
                    selectedEvaluacion = selectedEvaluacion,
                    evaluacionExpanded = evaluacionExpanded,
                    onEvaluacionExpandedChange = { evaluacionExpanded = it },
                    onShowDatePicker = { showDatePicker = true },
                )
                1 -> StepProductos(
                    uiState = uiState,
                    viewModel = viewModel,
                    monturasActivas = monturasActivas,
                    expandedItems = expandedItems,
                )
                2 -> if (isEditMode) {
                    StepGestion(
                        uiState = uiState,
                        viewModel = viewModel,
                        dispensacionId = dispensacionId!!,
                        navController = navController,
                    )
                }
            }

            if (!uiState.error.isNullOrBlank()) {
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
            }
            Spacer(modifier = Modifier.height(density.sectionGap))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepOrden(
    uiState: com.example.optoapp.viewmodel.DispensacionUiState,
    viewModel: DispensacionViewModel,
    selectedEvaluacion: com.example.optoapp.data.EvaluacionClinica?,
    evaluacionExpanded: Boolean,
    onEvaluacionExpandedChange: (Boolean) -> Unit,
    onShowDatePicker: () -> Unit,
) {
    val density = LocalOptoDensity.current
    OutlinedButton(onClick = onShowDatePicker, modifier = Modifier.fillMaxWidth()) {
        Text("Fecha: ${DateUtils.formatLocalized(uiState.fecha)}")
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(density.blockGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OptoTextField(
            value = uiState.ot,
            onValueChange = { viewModel.updateUiState { s -> s.copy(ot = it) } },
            label = "N° OT (OT-AAAA-####)",
            modifier = Modifier.weight(1f).testTag(TestTags.DISPENSACION_OT_FIELD),
        )
        TextButton(onClick = { viewModel.suggestOt() }) {
            Text("Sugerir OT", fontSize = 13.sp)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(density.cardPadding), verticalArrangement = Arrangement.spacedBy(density.blockGap)) {
            Text("Evaluación Vinculada", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

            if (uiState.evaluacionesDisponibles.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = evaluacionExpanded,
                    onExpandedChange = { onEvaluacionExpandedChange(!evaluacionExpanded) },
                ) {
                    OutlinedTextField(
                        value = if (uiState.evaluacionId != null) {
                            val eval = uiState.evaluacionesDisponibles.find { it.id == uiState.evaluacionId }
                            if (eval != null) DateUtils.formatLocalized(eval.fecha) else "Seleccionar evaluación"
                        } else {
                            "Sin evaluación"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Evaluación") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = evaluacionExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                    )
                    ExposedDropdownMenu(
                        expanded = evaluacionExpanded,
                        onDismissRequest = { onEvaluacionExpandedChange(false) },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin evaluación", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                viewModel.setEvaluacionId(null)
                                onEvaluacionExpandedChange(false)
                            },
                        )
                        uiState.evaluacionesDisponibles.sortedByDescending { it.fecha }.forEach { eval ->
                            DropdownMenuItem(
                                text = { Text(DateUtils.formatLocalized(eval.fecha)) },
                                onClick = {
                                    viewModel.setEvaluacionId(eval.id)
                                    onEvaluacionExpandedChange(false)
                                },
                            )
                        }
                    }
                }
            } else {
                Text("No hay evaluaciones para este paciente.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (selectedEvaluacion != null &&
                (selectedEvaluacion.prismaOdValor.orEmpty().isNotBlank() || selectedEvaluacion.prismaOiValor.orEmpty().isNotBlank())
            ) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Prisma (solo lectura)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("OD:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        buildPrismaDisplay(selectedEvaluacion.prismaOdValor.orEmpty(), selectedEvaluacion.prismaOdBase.orEmpty()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("OI:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        buildPrismaDisplay(selectedEvaluacion.prismaOiValor.orEmpty(), selectedEvaluacion.prismaOiBase.orEmpty()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepProductos(
    uiState: com.example.optoapp.viewmodel.DispensacionUiState,
    viewModel: DispensacionViewModel,
    monturasActivas: List<com.example.optoapp.data.Montura>,
    expandedItems: MutableMap<Int, Boolean>,
) {
    Text(
        "Productos",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary,
    )

    uiState.items.forEachIndexed { index, item ->
        val isExpanded = expandedItems[index] ?: (uiState.items.size <= 1)
        CollapsibleItemCard(
            item = item,
            index = index,
            isExpanded = isExpanded,
            isOnlyItem = uiState.items.size <= 1,
            monturasActivas = monturasActivas,
            onToggle = { expandedItems[index] = !isExpanded },
            onUpdate = { updated -> viewModel.updateItem(index, updated) },
            onRemove = { viewModel.removeItem(index) },
            onCalculateCosts = { viewModel.calculateCosts(index) },
        )
    }

    OutlinedButton(
        onClick = { viewModel.addItem() },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.DISPENSACION_AGREGAR_ITEM_BTN),
    ) {
        Icon(Icons.Default.Add, contentDescription = "Agregar")
        Spacer(Modifier.width(8.dp))
        Text("Agregar otro producto (lente + montura)")
    }
}

@Composable
private fun StepGestion(
    uiState: com.example.optoapp.viewmodel.DispensacionUiState,
    viewModel: DispensacionViewModel,
    dispensacionId: String,
    navController: NavController,
) {
    val density = LocalOptoDensity.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(density.cardPadding), verticalArrangement = Arrangement.spacedBy(density.blockGap)) {
            Text("Información Financiera", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            val monto = uiState.montoTotal.toDoubleOrNull() ?: 0.0
            val pagado = uiState.pagos.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }
            val saldo = monto - pagado
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monto Total:", fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("s/. ${"%.2f".format(monto)}", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Saldo:", fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "s/. ${"%.2f".format(saldo)}",
                    fontWeight = FontWeight.Bold,
                    color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Estado:", fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(uiState.estadoEntrega, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { navController.navigate(Route.InformacionFinanciera(dispensacionId).route) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Gestionar Pagos") }
            OutlinedButton(
                onClick = { navController.navigate(Route.CostosYGastosDisp(dispensacionId).route) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Receipt, contentDescription = "Recibo", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Gestionar costos →")
            }
            if (uiState.fechaEntrega != null) {
                FechaEntregaEditButton(
                    fechaEntrega = uiState.fechaEntrega,
                    onFechaChanged = { nueva ->
                        viewModel.updateUiState {
                            it.copy(
                                fechaEntrega = nueva,
                                estadoEntrega = estadoAfterFechaEntrega(it.estadoEntrega, nueva),
                            )
                        }
                    },
                )
            } else {
                TextButton(onClick = {
                    val fecha = LocalDate.now()
                    viewModel.updateUiState {
                        it.copy(
                            fechaEntrega = fecha,
                            estadoEntrega = estadoAfterFechaEntrega(it.estadoEntrega, fecha),
                        )
                    }
                }) {
                    Text("Asignar fecha de entrega", fontSize = 12.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = {
            viewModel.deleteDispensacion(dispensacionId) {
                navController.popBackStack()
            }
        },
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Text("Eliminar Orden")
    }
}

@Composable
private fun CollapsibleItemCard(
    item: DispensacionItemUi,
    index: Int,
    isExpanded: Boolean,
    isOnlyItem: Boolean,
    monturasActivas: List<com.example.optoapp.data.Montura>,
    onToggle: () -> Unit,
    onUpdate: (DispensacionItemUi) -> Unit,
    onRemove: () -> Unit,
    onCalculateCosts: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (index % 2 == 0) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lente ${index + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            buildItemSummary(item),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (item.costoRealOd != null || item.costoRealOi != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Costos: ${item.costoRealOd?.let { "OD s/. ${fmt(it)}" } ?: ""}${if (item.costoRealOd != null && item.costoRealOi != null) " | " else ""}${item.costoRealOi?.let { "OI s/. ${fmt(it)}" } ?: ""}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    modifier = Modifier.size(20.dp),
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    LenteForm(
                        item = item,
                        index = index,
                        isOnlyItem = isOnlyItem,
                        monturasActivas = monturasActivas,
                        onUpdate = onUpdate,
                        onRemove = onRemove,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onCalculateCosts,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = "Recibo", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Calcular costos desde evaluación")
                    }
                }
            }
        }
    }
}

private fun buildItemSummary(item: DispensacionItemUi): String {
    val parts = mutableListOf<String>()
    if (item.tipoLente.isNotBlank()) parts.add(item.tipoLente)
    if (item.materialLente.isNotBlank()) parts.add(item.materialLente)
    if (item.tratamientos.isNotEmpty()) parts.add(item.tratamientos.first())
    if (item.origenMontura.isNotBlank()) parts.add("Montura: ${item.origenMontura}")
    if (item.descripcionMontura.isNotBlank()) parts.add(item.descripcionMontura)
    return if (parts.isNotEmpty()) {
        parts.joinToString(" · ")
    } else {
        "Sin especificar"
    }
}

private fun buildPrismaDisplay(valor: String, base: String): String {
    val v = valor.replace(",", ".").trim()
    val b = base.trim()
    return when {
        v.isNotBlank() && b.isNotBlank() -> "${v}Δ base $b"
        v.isNotBlank() -> "${v}Δ"
        else -> "—"
    }
}

private fun fmt(value: Double): String = if (value == value.toLong().toDouble()) {
    String.format(java.util.Locale.getDefault(), "%,.0f", value)
} else {
    String.format(java.util.Locale.getDefault(), "%,.2f", value)
}
