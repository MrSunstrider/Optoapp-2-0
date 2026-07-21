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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.domain.Prioridad
import com.example.optoapp.domain.Recomendacion
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.theme.AlertRed
import com.example.optoapp.ui.theme.PositiveGreen
import com.example.optoapp.ui.theme.WarningAmber
import com.example.optoapp.viewmodel.AnalisisNegocioViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.GastosViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnalisisNegocioScreen(
    navController: NavController,
    viewModel: AnalisisNegocioViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    gastosViewModel: GastosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val gastos by gastosViewModel.allGastos.collectAsState()
    val gastosUiState by gastosViewModel.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val canView = AppRoles.canViewBiAndReports(opticaRol)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = "Análisis Financiero",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        if (!canView) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Bloqueado", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text("Acceso restringido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Tu rol no tiene permiso para ver esta sección.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Advertencia", tint = WarningAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Datos limitados — sin conexión", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                    }
                }
            }

            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = AlertRed, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = AlertRed, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.refresh() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            MonthSwitcher(
                currentMonth = uiState.mesSeleccionado,
                onPrevious = { viewModel.navigateMonth(-1) },
                onNext = { viewModel.navigateMonth(1) },
            )

            uiState.analisis?.let { analisis ->
                ResumenCard(analisis = analisis)
            }

            if (uiState.isSeasonalityWarning) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Advertencia", tint = WarningAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Este cálculo se basa en pocos meses. Podría no ser preciso.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningAmber,
                        )
                    }
                }
            }

            Text(
                "Recomendaciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (uiState.recomendaciones.isNotEmpty()) {
                uiState.recomendaciones.take(3).forEach { rec ->
                    RecomendacionCard(
                        rec = rec,
                        feedbacksEnviados = uiState.feedbacksEnviados,
                        feedbackErrorRecId = uiState.feedbackErrorRecId,
                        onFeedback = { fueUtil -> viewModel.onFeedback(rec.id, fueUtil) },
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
                        Icon(Icons.Default.Lightbulb, contentDescription = "Recomendacion", tint = WarningAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sin recomendaciones por ahora — tus métricas están dentro de lo esperado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (!uiState.isLoading && uiState.analisis == null && uiState.error == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Sin datos para este mes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                var deleteTarget by remember { mutableStateOf<String?>(null) }
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MoneyOff, contentDescription = "Sin costo", tint = AlertRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Gastos del mes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            IconButton(
                                modifier = Modifier.size(28.dp),
                                onClick = { gastosViewModel.refreshGastos() },
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Actualizar gastos", modifier = Modifier.size(16.dp))
                            }
                        }
                        Text("S/ ${formatNumber(totalGastos)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AlertRed)
                    }
                    if (gastos.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        gastos.sortedByDescending { it.fecha }.forEach { g ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { gastosViewModel.editGasto(g) },
                                        onLongClick = { deleteTarget = g.id },
                                    )
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${g.categoria}${if (!g.descripcion.isNullOrBlank()) " · ${g.descripcion}" else ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (g.isRecurring) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                                Text("Recurrente", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    Text(
                                        com.example.optoapp.util.DateUtils.formatLocalized(g.fecha),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    )
                                }
                                Text("S/ ${formatNumber(g.monto.toDouble())}", fontSize = 12.sp, color = AlertRed, fontWeight = FontWeight.Medium)
                                IconButton(
                                    modifier = Modifier.size(28.dp),
                                    onClick = { gastosViewModel.editGasto(g) },
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(14.dp))
                                }
                                IconButton(
                                    modifier = Modifier.size(28.dp),
                                    onClick = { deleteTarget = g.id },
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(14.dp), tint = AlertRed)
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "No hay gastos registrados",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { gastosViewModel.showNewGasto() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar", fontSize = 12.sp)
                        }
                        if (gastosMes.isNotEmpty()) {
                            OutlinedButton(onClick = { navController.navigate("gastos") }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lista", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ver todos", fontSize = 12.sp)
                            }
                        }
                    }

                    if (deleteTarget != null) {
                        val target = gastos.find { it.id == deleteTarget } ?: return@Column
                        AlertDialog(
                            onDismissRequest = { deleteTarget = null },
                            title = { Text("Eliminar gasto") },
                            text = { Text("¿Eliminar \"${target.categoria}${if (!target.descripcion.isNullOrBlank()) " · ${target.descripcion}" else ""}\" por S/ ${formatNumber(target.monto.toDouble())}?") },
                            confirmButton = {
                                Button(onClick = {
                                    gastosViewModel.delete(target)
                                    deleteTarget = null
                                }, colors = ButtonDefaults.buttonColors(containerColor = AlertRed)) {
                                    Text("Eliminar")
                                }
                            },
                            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } },
                        )
                    }
                }
            }

            if (uiState.analisis != null) {
                Button(
                    onClick = { navController.navigate("analisis_detalle") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    @Suppress("DEPRECATION")
                    Icon(Icons.Default.TrendingUp, contentDescription = "Tendencia")
                    Spacer(Modifier.width(8.dp))
                    Text("Ver análisis completo")
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
                title = { Text(if (gastosUiState.editingGasto != null) "Editar Gasto" else "Nuevo Gasto", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = gastosUiState.categoria,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoría") },
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
                            label = { Text("Monto") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(value = gastosUiState.descripcion, onValueChange = { gastosViewModel.updateDescripcion(it) }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
                        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.DateRange, contentDescription = "Fecha", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(com.example.optoapp.util.DateUtils.formatLocalized(gastosUiState.fecha))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = gastosUiState.isRecurring, onCheckedChange = { gastosViewModel.toggleRecurrente() })
                            Spacer(Modifier.width(8.dp))
                            Text("Gasto recurrente mensual", fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = { Button(onClick = { gastosViewModel.save() }) { Text("Guardar") } },
                dismissButton = { TextButton(onClick = { gastosViewModel.dismissDialog() }) { Text("Cancelar") } },
            )
        }
    }
}

@Composable
private fun MonthSwitcher(
    currentMonth: java.time.LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-PE"))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                @Suppress("DEPRECATION")
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mes anterior")
            }
            Text(
                text = currentMonth.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onNext) {
                @Suppress("DEPRECATION")
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mes siguiente")
            }
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
            Text("Resumen del mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Vendiste",
                    value = "S/ ${formatNumber(analisis.ventasMes)}",
                    color = PositiveGreen,
                )
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Cobraste",
                    value = "S/ ${formatNumber(analisis.cobrosMes)}",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            MetricItem(
                modifier = Modifier.fillMaxWidth(),
                label = "Saldo pendiente",
                value = "S/ ${formatNumber(saldo)}",
                color = if (saldo > 0) AlertRed else PositiveGreen,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Margen neto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "De cada S/ 100 que vendés, te quedan S/ ${Math.round(analisis.margenNetoPct)} (margen neto)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (analisis.margenNetoPct >= 25) PositiveGreen else WarningAmber,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Ticket prom.",
                    value = "S/ ${formatNumber(analisis.ticketPromedio)}",
                    color = MaterialTheme.colorScheme.primary,
                )
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Ventas",
                    value = "${analisis.cantidadVentas}",
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

@androidx.annotation.VisibleForTesting
@Composable
internal fun RecomendacionCard(
    rec: Recomendacion,
    feedbacksEnviados: Map<String, Boolean>,
    feedbackErrorRecId: String?,
    onFeedback: (Boolean) -> Unit,
) {
    val feedbackSent = feedbacksEnviados.containsKey(rec.id)
    val hasError = feedbackErrorRecId == rec.id

    val bgColor = when (rec.prioridad) {
        Prioridad.ALTA -> AlertRed.copy(alpha = 0.08f)
        Prioridad.MEDIA -> WarningAmber.copy(alpha = 0.08f)
        Prioridad.BAJA -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val accentColor = when (rec.prioridad) {
        Prioridad.ALTA -> AlertRed
        Prioridad.MEDIA -> WarningAmber
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
                    if (feedbackSent) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = "Retroalimentacion",
                    tint = if (feedbackSent) PositiveGreen else accentColor,
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
                    rec.accion?.let { accion ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "→ $accion",
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        rec.prioridad.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (feedbackSent) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completado",
                        tint = PositiveGreen,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Gracias por tu valoración",
                        style = MaterialTheme.typography.bodySmall,
                        color = PositiveGreen,
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = { onFeedback(true) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.ThumbUp, contentDescription = "Positivo", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Útil")
                    }
                    FilledTonalButton(
                        onClick = { onFeedback(false) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.ThumbDown, contentDescription = "Negativo", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("No me sirve")
                    }
                }
            }
            if (hasError) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "No se pudo enviar tu valoración. Intentalo de nuevo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AlertRed,
                )
            }
        }
    }
}

private fun formatNumber(value: Double): String = com.example.optoapp.util.NumberFormatter.formatCurrency(value)
