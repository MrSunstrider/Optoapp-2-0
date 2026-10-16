package com.example.optoapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.R
import com.example.optoapp.data.AppRoles
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.domain.ConfianzaInterpretacion
import com.example.optoapp.domain.ImpactoRecomendacion
import com.example.optoapp.domain.InterpretacionMes
import com.example.optoapp.domain.Prioridad
import com.example.optoapp.domain.Recomendacion
import com.example.optoapp.domain.SeguimientoEstado
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.common.CostCoverageBadge
import com.example.optoapp.ui.components.common.MonthSelector
import com.example.optoapp.ui.components.common.MonthSelectorState
import com.example.optoapp.ui.navigation.CostosTab
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.ui.theme.warningAmber
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.AnalisisNegocioViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.CostosYGastosViewModel
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnalisisNegocioScreen(
    navController: NavController,
    viewModel: AnalisisNegocioViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    gastosViewModel: CostosYGastosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val gastosUiState by gastosViewModel.uiState.collectAsState()
    val gastos = gastosUiState.gastosOperativos
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = null)
    val canView = opticaRol != null && AppRoles.canViewBiAndReports(opticaRol!!)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = stringResource(R.string.analisis_title),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.analisis_atras_cd))
                    }
                },
            )
        },
    ) { padding ->
        if (opticaRol == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (!canView) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.analisis_acceso_restringido), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.analisis_acceso_restringido_mensaje), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.analisis?.esOffline == true) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.warningAmber.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.warningAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.analisis_datos_limitados), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.warningAmber)
                    }
                }
            }

            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.alertRed.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.alertRed, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.alertRed, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.analisis_reintentar))
                        }
                    }
                }
            }

            var metricasDetalleExpanded by remember { mutableStateOf(false) }

            MonthSelector(
                state = MonthSelectorState(YearMonth.from(uiState.mesSeleccionado), viewModel.mesActual),
                onMonthSelected = viewModel::selectMonth,
            )

            uiState.interpretacion?.let { interpretacion ->
                InterpretacionCoachCard(interpretacion = interpretacion)
            }

            if (uiState.isSeasonalityWarning) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.warningAmber.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.warningAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.analisis_estacionalidad_aviso),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.warningAmber,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.analisis_recomendaciones),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (uiState.recomendaciones.isNotEmpty()) {
                uiState.recomendaciones.take(5).forEach { rec ->
                    RecomendacionCard(
                        rec = rec,
                        seguimiento = uiState.seguimientos[rec.id],
                        feedbackErrorRecId = uiState.feedbackErrorRecId,
                        onSeguimiento = { estado -> viewModel.onSeguimiento(rec.id, estado) },
                    )
                }
            } else if (!uiState.isLoading && uiState.analisis != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.warningAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.analisis_sin_recomendaciones), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            uiState.analisis?.let { analisis ->
                ContextoBreveCard(analisis = analisis)
            }

            if (!uiState.isLoading && uiState.analisis == null && uiState.error == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.analisis_sin_datos_mes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (uiState.analisis != null) {
                Text(
                    stringResource(R.string.analisis_atajos),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            navController.navigate(Route.AnalisisDetalle.createRoute(YearMonth.from(uiState.mesSeleccionado)))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.analisis_atajo_detalle))
                    }
                    OutlinedButton(
                        onClick = {
                            navController.navigate(
                                Route.CostosYGastos.createRoute(
                                    CostosTab.GASTOS,
                                    YearMonth.from(uiState.mesSeleccionado),
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.analisis_atajo_costos))
                    }
                }
                OutlinedButton(
                    onClick = { metricasDetalleExpanded = !metricasDetalleExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (metricasDetalleExpanded) {
                                R.string.analisis_metricas_ocultar
                            } else {
                                R.string.analisis_metricas_detalle
                            },
                        ),
                    )
                }
                if (metricasDetalleExpanded) {
                    ResumenCard(analisis = uiState.analisis!!)
                }
            }

            val mesActual = uiState.mesSeleccionado
            val gastosMes = remember(gastos, mesActual) {
                gastos.filter { it.fecha.month == mesActual.month && it.fecha.year == mesActual.year }
            }
            val totalGastos = remember(uiState.analisis, gastosMes) {
                uiState.analisis?.gastosMes ?: gastosMes.sumOf { it.monto }.toDouble()
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MoneyOff, contentDescription = null, tint = MaterialTheme.colorScheme.alertRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.analisis_gastos_mes), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            IconButton(
                                modifier = Modifier.size(28.dp),
                                onClick = { gastosViewModel.refreshGastos() },
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.analisis_actualizar_gastos_cd), modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(MoneyFormatter.format(totalGastos), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.alertRed)
                    }
                    if (gastosMes.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        gastosMes.sortedByDescending { it.fecha }.forEach { g ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { gastosViewModel.editGasto(g) },
                                        onLongClick = { gastosViewModel.confirmDeleteGasto(g) },
                                    )
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(gastoEtiqueta(g.categoria, g.descripcion), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (g.isRecurring) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                                Text(stringResource(R.string.analisis_gasto_recurrente_chip), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    Text(
                                        com.example.optoapp.util.DateUtils.formatLocalized(g.fecha),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    )
                                }
                                Text(MoneyFormatter.format(g.monto.toDouble()), fontSize = 12.sp, color = MaterialTheme.colorScheme.alertRed, fontWeight = FontWeight.Medium)
                                IconButton(
                                    modifier = Modifier.size(28.dp),
                                    onClick = { gastosViewModel.editGasto(g) },
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.gastos_editar_cd), modifier = Modifier.size(14.dp))
                                }
                                IconButton(
                                    modifier = Modifier.size(28.dp),
                                    onClick = { gastosViewModel.confirmDeleteGasto(g) },
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.gastos_eliminar_cd), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.alertRed)
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.analisis_sin_gastos),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { gastosViewModel.showNewGasto() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.analisis_agregar), fontSize = 12.sp)
                        }
                        if (gastosMes.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { navController.navigate(rutaVerTodosGastos(YearMonth.from(uiState.mesSeleccionado))) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.analisis_ver_todos), fontSize = 12.sp)
                            }
                        }
                    }

                    gastosUiState.deletingGasto?.let { target ->
                        AlertDialog(
                            onDismissRequest = { gastosViewModel.dismissDeleteGasto() },
                            title = { Text(stringResource(R.string.analisis_eliminar_gasto_titulo)) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val etiqueta = gastoEtiqueta(target.categoria, target.descripcion)
                                    Text(stringResource(R.string.analisis_eliminar_gasto_confirmacion, etiqueta, MoneyFormatter.format(target.monto.toDouble())))
                                    gastosUiState.gastoDeleteError?.let {
                                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { gastosViewModel.deleteGasto() },
                                    enabled = !gastosUiState.isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.alertRed),
                                ) {
                                    Text(stringResource(R.string.analisis_eliminar))
                                }
                            },
                            dismissButton = { TextButton(onClick = { gastosViewModel.dismissDeleteGasto() }) { Text(stringResource(R.string.analisis_cancelar)) } },
                        )
                    }
                }
            }

        }

        if (gastosUiState.isDialogVisible) {
            var showDatePicker by remember { mutableStateOf(false) }
            if (showDatePicker) {
                com.example.optoapp.ui.components.OptoDatePickerDialog(
                    initialDate = gastosUiState.fecha,
                    onDateSelected = { gastosViewModel.updateFecha(it) },
                    onDismiss = { showDatePicker = false },
                )
            }
            AlertDialog(
                onDismissRequest = { gastosViewModel.dismissDialog() },
                title = {
                    val tituloRes = if (gastosUiState.editingGasto != null) R.string.analisis_editar_gasto else R.string.analisis_nuevo_gasto
                    Text(stringResource(tituloRes), fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = gastosUiState.categoria,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.analisis_categoria)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                gastosViewModel.categorias.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat) }, onClick = {
                                        gastosViewModel.updateCategoria(cat)
                                        expanded = false
                                    })
                                }
                            }
                        }
                        OutlinedTextField(
                            value = gastosUiState.monto,
                            onValueChange = { gastosViewModel.updateMonto(it) },
                            label = { Text(stringResource(R.string.analisis_monto)) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(value = gastosUiState.descripcion, onValueChange = { gastosViewModel.updateDescripcion(it) }, label = { Text(stringResource(R.string.analisis_descripcion)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(com.example.optoapp.util.DateUtils.formatLocalized(gastosUiState.fecha))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = gastosUiState.isRecurring, onCheckedChange = { gastosViewModel.toggleRecurrente() })
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.analisis_gasto_recurrente), fontSize = 13.sp)
                        }
                        gastosUiState.gastoFormError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { gastosViewModel.saveGasto() }, enabled = !gastosUiState.isLoading) {
                        Text(stringResource(R.string.analisis_guardar))
                    }
                },
                dismissButton = { TextButton(onClick = { gastosViewModel.dismissDialog() }) { Text(stringResource(R.string.analisis_cancelar)) } },
            )
        }
    }
}

internal fun rutaVerTodosGastos(mes: YearMonth): String = Route.CostosYGastos.createRoute(CostosTab.GASTOS, mes)

@Composable
private fun gastoEtiqueta(categoria: String, descripcion: String?): String =
    if (descripcion.isNullOrBlank()) categoria else stringResource(R.string.analisis_gasto_etiqueta, categoria, descripcion)

@Composable
private fun InterpretacionCoachCard(interpretacion: InterpretacionMes) {
    val confianzaLabel = when (interpretacion.confianza) {
        ConfianzaInterpretacion.ALTA -> stringResource(R.string.analisis_confianza_alta)
        ConfianzaInterpretacion.MEDIA -> stringResource(R.string.analisis_confianza_media)
        ConfianzaInterpretacion.BAJA -> stringResource(R.string.analisis_confianza_baja)
    }
    val confianzaColor = when (interpretacion.confianza) {
        ConfianzaInterpretacion.ALTA -> MaterialTheme.colorScheme.positiveGreen
        ConfianzaInterpretacion.MEDIA -> MaterialTheme.colorScheme.warningAmber
        ConfianzaInterpretacion.BAJA -> MaterialTheme.colorScheme.alertRed
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.analisis_lectura_mes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                interpretacion.titular,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                confianzaLabel,
                style = MaterialTheme.typography.labelSmall,
                color = confianzaColor,
            )
            interpretacion.puntos.forEach { punto ->
                Text(
                    stringResource(R.string.analisis_interpretacion_punto, punto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            interpretacion.proximaAccion?.let { accion ->
                Text(
                    stringResource(R.string.analisis_proxima_accion, accion),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ContextoBreveCard(analisis: AnalisisMensual) {
    val facts = analisis.marginFacts
    val margenPct = Math.round(if (facts.margenNetoPct != 0.0) facts.margenNetoPct else analisis.margenNetoPct)
    val objetivoPct = Math.round(facts.margenNetoObjetivo)
    val pe = facts.puntoEquilibrio
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.analisis_contexto_breve),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.analisis_contexto_margen_vs_objetivo, margenPct, objetivoPct),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            MetricItem(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.analisis_vendiste),
                value = MoneyFormatter.format(analisis.ventasMes),
                color = MaterialTheme.colorScheme.positiveGreen,
            )
            if (pe != null && pe > 0.0) {
                val peLabel = if (analisis.ventasMes >= pe) {
                    stringResource(R.string.analisis_contexto_pe_alcanzado)
                } else {
                    stringResource(
                        R.string.analisis_contexto_pe_falta,
                        MoneyFormatter.format(pe - analisis.ventasMes),
                    )
                }
                Text(
                    peLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CostCoverageBadge(cobertura = facts.cobertura, fuente = facts.fuente)
        }
    }
}

@Composable
private fun ResumenCard(analisis: AnalisisMensual) {
    val saldo = analisis.saldoPendiente
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.analisis_resumen_mes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.analisis_vendiste),
                    value = MoneyFormatter.format(analisis.ventasMes),
                    color = MaterialTheme.colorScheme.positiveGreen,
                )
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.analisis_cobraste),
                    value = MoneyFormatter.format(analisis.cobrosMes),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            MetricItem(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.analisis_saldo_pendiente),
                value = MoneyFormatter.format(saldo),
                color = if (saldo > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.positiveGreen,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.analisis_margen_neto), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.analisis_margen_neto_explicacion, Math.round(analisis.margenNetoPct)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (analisis.margenNetoPct >= 25) MaterialTheme.colorScheme.positiveGreen else MaterialTheme.colorScheme.warningAmber,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.analisis_ticket_promedio),
                    value = MoneyFormatter.format(analisis.ticketPromedio),
                    color = MaterialTheme.colorScheme.primary,
                )
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.analisis_ventas),
                    value = analisis.cantidadVentas.toString(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun SeguimientoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    } else {
        FilledTonalButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

@androidx.annotation.VisibleForTesting
@Composable
internal fun RecomendacionCard(
    rec: Recomendacion,
    seguimiento: SeguimientoEstado?,
    feedbackErrorRecId: String?,
    onSeguimiento: (SeguimientoEstado) -> Unit,
) {
    val hasError = feedbackErrorRecId == rec.id
    val tracked = seguimiento != null

    val bgColor = when (rec.prioridad) {
        Prioridad.ALTA -> MaterialTheme.colorScheme.alertRed.copy(alpha = 0.08f)
        Prioridad.MEDIA -> MaterialTheme.colorScheme.warningAmber.copy(alpha = 0.08f)
        Prioridad.BAJA -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val accentColor = when (rec.prioridad) {
        Prioridad.ALTA -> MaterialTheme.colorScheme.alertRed
        Prioridad.MEDIA -> MaterialTheme.colorScheme.warningAmber
        Prioridad.BAJA -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    if (tracked) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (tracked) MaterialTheme.colorScheme.positiveGreen else accentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rec.titulo,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        rec.detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val impactoLabel = rec.impactoEstimado ?: ImpactoRecomendacion.SIN_IMPACTO
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.analisis_recomendacion_impacto, impactoLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    rec.accion?.let { accion ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.analisis_recomendacion_accion, accion),
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                val prioridadLabel = stringResource(rec.prioridad.labelRes())
                val prioridadDescription = stringResource(R.string.analisis_prioridad_descripcion, prioridadLabel)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = prioridadDescription },
                ) {
                    Text(
                        prioridadLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SeguimientoChip(
                    label = stringResource(R.string.analisis_seguimiento_hecho),
                    selected = seguimiento == SeguimientoEstado.HECHO,
                    onClick = { onSeguimiento(SeguimientoEstado.HECHO) },
                    modifier = Modifier.weight(1f),
                )
                SeguimientoChip(
                    label = stringResource(R.string.analisis_seguimiento_mas_tarde),
                    selected = seguimiento == SeguimientoEstado.MAS_TARDE,
                    onClick = { onSeguimiento(SeguimientoEstado.MAS_TARDE) },
                    modifier = Modifier.weight(1f),
                )
                SeguimientoChip(
                    label = stringResource(R.string.analisis_seguimiento_no_aplica),
                    selected = seguimiento == SeguimientoEstado.NO_APLICA,
                    onClick = { onSeguimiento(SeguimientoEstado.NO_APLICA) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (hasError) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.analisis_seguimiento_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.alertRed,
                )
            }
        }
    }
}
