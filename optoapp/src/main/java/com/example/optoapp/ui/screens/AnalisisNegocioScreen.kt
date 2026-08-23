package com.example.optoapp.ui.screens

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
import com.example.optoapp.domain.AnalisisPnLCalculator
import com.example.optoapp.domain.Prioridad
import com.example.optoapp.domain.Recomendacion
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.ui.theme.warningAmber
import com.example.optoapp.viewmodel.AnalisisNegocioViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.CostosYGastosViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Análisis shows read-only gastosMes; writes live on CostosYGastos tab 3. */
object AnalisisGastosPolicy {
    const val allowsWrites: Boolean = false
    val verTodosInitialTab: Int = CostosYGastosViewModel.TAB_GASTOS
    val verTodosRoute: String = Route.Gastos.route
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalisisNegocioScreen(
    navController: NavController,
    viewModel: AnalisisNegocioViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = null)
    val canView = opticaRol != null && AppRoles.canViewBiAndReports(opticaRol!!)

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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.warningAmber.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Advertencia", tint = MaterialTheme.colorScheme.warningAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Datos limitados — sin conexión", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.warningAmber)
                    }
                }
            }

            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.alertRed.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.alertRed, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.alertRed, style = MaterialTheme.typography.bodyMedium)
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
                PnLCard(analisis = analisis)
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
                        Icon(Icons.Default.Warning, contentDescription = "Advertencia", tint = MaterialTheme.colorScheme.warningAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Este cálculo se basa en pocos meses. Podría no ser preciso.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.warningAmber,
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
                        Icon(Icons.Default.Lightbulb, contentDescription = "Recomendacion", tint = MaterialTheme.colorScheme.warningAmber, modifier = Modifier.size(18.dp))
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

            val totalGastos = uiState.analisis?.gastosMes ?: 0.0

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MoneyOff,
                                contentDescription = "Sin costo",
                                tint = MaterialTheme.colorScheme.alertRed,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gastos del mes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            "S/ ${formatNumber(totalGastos)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.alertRed,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { navController.navigate(AnalisisGastosPolicy.verTodosRoute) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lista", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver todos", fontSize = 12.sp)
                    }
                }
            }

            if (uiState.analisis != null) {
                OutlinedButton(
                    onClick = { navController.navigate(Route.ResumenDiario.route) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lista", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver resumen diario", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        val yearMonth = "%04d-%02d".format(
                            uiState.mesSeleccionado.year,
                            uiState.mesSeleccionado.monthValue,
                        )
                        navController.navigate(Route.AnalisisDetalle(yearMonth).route)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    @Suppress("DEPRECATION")
                    Icon(Icons.Default.TrendingUp, contentDescription = "Tendencia")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver análisis completo")
                }
            }
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
                    color = MaterialTheme.colorScheme.positiveGreen,
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
                color = if (saldo > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.positiveGreen,
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
                        color =                         if (analisis.margenNetoPct >= 25) MaterialTheme.colorScheme.positiveGreen else MaterialTheme.colorScheme.warningAmber,
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
private fun PnLCard(analisis: AnalisisMensual) {
    val pnl = AnalisisPnLCalculator.fromAnalisis(analisis)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (analisis.esOffline) "P&L del mes (parcial / offline)" else "P&L del mes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (analisis.esOffline) {
                Text(
                    "Calculado desde resumen diario y gastos locales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.warningAmber,
                )
            }
            PnLLine(label = "Ventas", value = pnl.ventas)
            PnLLine(label = "COGS (costo de ventas)", value = -pnl.cogs)
            PnLLine(label = "Gastos operativos", value = -pnl.gastos)
            HorizontalDivider()
            PnLLine(label = "Utilidad", value = pnl.utilidad, bold = true)
        }
    }
}

@Composable
private fun PnLLine(label: String, value: Double, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "S/ ${formatNumber(value)}",
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = when {
                bold && value >= 0 -> MaterialTheme.colorScheme.positiveGreen
                bold && value < 0 -> MaterialTheme.colorScheme.alertRed
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
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
                    if (feedbackSent) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = "Retroalimentacion",
                    tint = if (feedbackSent) MaterialTheme.colorScheme.positiveGreen else accentColor,
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
                        tint = MaterialTheme.colorScheme.positiveGreen,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Gracias por tu valoración",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.positiveGreen,
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
                    color = MaterialTheme.colorScheme.alertRed,
                )
            }
        }
    }
}

private fun formatNumber(value: Double): String = com.example.optoapp.util.NumberFormatter.formatCurrency(value)
