package com.example.optoapp.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
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
import com.example.optoapp.domain.AnalisisDecisionLayout
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.domain.ConfianzaInterpretacion
import com.example.optoapp.domain.CtaRecomendacion
import com.example.optoapp.domain.AlertaNegocio
import com.example.optoapp.domain.AlertasDesdeRecomendaciones
import com.example.optoapp.domain.EsfuerzoRecomendacion
import com.example.optoapp.domain.ImpactoRecomendacion
import com.example.optoapp.domain.InterpretacionMes
import com.example.optoapp.domain.Prioridad
import com.example.optoapp.domain.Recomendacion
import com.example.optoapp.domain.ResultadoOperativoConfiabilidad
import com.example.optoapp.domain.SeguimientoEstado
import com.example.optoapp.domain.SeveridadAlerta
import com.example.optoapp.domain.SupuestosImpacto
import com.example.optoapp.domain.WaterfallStep
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.common.CostCoverageBadge
import com.example.optoapp.ui.components.common.MonthSelector
import com.example.optoapp.ui.components.common.MonthSelectorState
import com.example.optoapp.ui.navigation.CostosTab
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.ui.theme.warningAmber
import com.example.optoapp.util.AnalisisPercentFormatter
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.AnalisisNegocioViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import java.time.YearMonth

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

            val alertas = remember(uiState.recomendaciones) {
                AlertasDesdeRecomendaciones.from(uiState.recomendaciones)
            }
            if (alertas.isNotEmpty()) {
                Text(
                    stringResource(R.string.analisis_alertas),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                alertas.forEach { alerta ->
                    AlertaTipadaCard(alerta = alerta)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.analisis_recomendaciones),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = { viewModel.showSupuestosDialog() }) {
                    Text(stringResource(R.string.analisis_supuestos_editar))
                }
            }
            if (uiState.recomendaciones.isNotEmpty()) {
                uiState.recomendaciones.take(5).forEach { rec ->
                    RecomendacionCard(
                        rec = rec,
                        seguimiento = uiState.seguimientos[rec.id],
                        feedbackErrorRecId = uiState.feedbackErrorRecId,
                        onSeguimiento = { estado -> viewModel.onSeguimiento(rec.id, estado) },
                        onCta = { cta ->
                            when (cta) {
                                CtaRecomendacion.DEUDORES,
                                CtaRecomendacion.DETALLE,
                                -> navController.navigate(
                                    Route.AnalisisDetalle.createRoute(YearMonth.from(uiState.mesSeleccionado)),
                                )
                                CtaRecomendacion.COSTOS -> navController.navigate(
                                    rutaVerTodosGastos(YearMonth.from(uiState.mesSeleccionado)),
                                )
                                CtaRecomendacion.NINGUNA -> Unit
                            }
                        },
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
                DecisionHomeSection(analisis = analisis)
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

        }

        if (uiState.supuestosDialogVisible) {
            SupuestosImpactoDialog(
                iniciales = uiState.supuestosImpacto,
                error = uiState.supuestosError,
                onDismiss = { viewModel.dismissSupuestosDialog() },
                onSave = { viewModel.saveSupuestos(it) },
            )
        }
    }
}

internal fun rutaVerTodosGastos(mes: YearMonth): String = Route.CostosYGastos.createRoute(CostosTab.GASTOS, mes)

@Composable
private fun AlertaTipadaCard(alerta: AlertaNegocio) {
    val color = when (alerta.severidad) {
        SeveridadAlerta.ROJO -> MaterialTheme.colorScheme.alertRed
        SeveridadAlerta.AMBAR -> MaterialTheme.colorScheme.warningAmber
    }
    val badge = when (alerta.severidad) {
        SeveridadAlerta.ROJO -> stringResource(R.string.analisis_alerta_rojo)
        SeveridadAlerta.AMBAR -> stringResource(R.string.analisis_alerta_ambar)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
            Text(
                alerta.titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (alerta.detalle.isNotBlank()) {
                Text(
                    alerta.detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

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
            Text(
                stringResource(R.string.analisis_disclaimer_igv),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val margenPct = AnalisisPercentFormatter.format(facts.margenNetoPct)
    val objetivoPct = AnalisisPercentFormatter.format(facts.margenNetoObjetivo)
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
            if (ResultadoOperativoConfiabilidad.esIncompleto(facts.cobertura)) {
                Text(
                    stringResource(R.string.analisis_resultado_incompleto_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.warningAmber,
                )
            }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.analisis_margen_neto),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (ResultadoOperativoConfiabilidad.esIncompleto(analisis.marginFacts.cobertura)) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.analisis_resultado_incompleto),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.warningAmber,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(
                            R.string.analisis_margen_neto_explicacion,
                            AnalisisPercentFormatter.format(analisis.margenNetoPct),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (analisis.margenNetoPct >= 25) MaterialTheme.colorScheme.positiveGreen else MaterialTheme.colorScheme.warningAmber,
                    )
                    if (ResultadoOperativoConfiabilidad.esIncompleto(analisis.marginFacts.cobertura)) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.analisis_resultado_incompleto_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.warningAmber,
                        )
                    }
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
private fun DecisionHomeSection(analisis: AnalisisMensual) {
    val layout = AnalisisDecisionLayout.from(analisis)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiStrip(layout = layout)
        WaterfallPlCard(steps = layout.waterfall, incompleto = layout.kpis.resultadoIncompleto)
        CajaSectionCard(layout = layout)
    }
}

@Composable
private fun KpiStrip(layout: AnalisisDecisionLayout) {
    val kpis = layout.kpis
    val incompletoSuffix = if (kpis.resultadoIncompleto) {
        " · " + stringResource(R.string.analisis_resultado_incompleto)
    } else {
        ""
    }
    val brutoLabel = MoneyFormatter.format(kpis.utilidadBruta) + incompletoSuffix
    val roLabel = AnalisisPercentFormatter.formatWithSymbol(kpis.resultadoOperativoPct) + incompletoSuffix
    val incompletoColor = MaterialTheme.colorScheme.warningAmber
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricItem(
            modifier = Modifier.widthIn(min = 120.dp),
            label = stringResource(R.string.analisis_kpi_utilidad_bruta),
            value = brutoLabel,
            color = if (kpis.resultadoIncompleto) incompletoColor else MaterialTheme.colorScheme.positiveGreen,
        )
        MetricItem(
            modifier = Modifier.widthIn(min = 120.dp),
            label = stringResource(R.string.analisis_kpi_ro),
            value = roLabel,
            color = if (kpis.resultadoIncompleto) incompletoColor else MaterialTheme.colorScheme.primary,
        )
        MetricItem(
            modifier = Modifier.widthIn(min = 120.dp),
            label = stringResource(R.string.analisis_ticket_promedio),
            value = MoneyFormatter.format(kpis.ticket),
            color = MaterialTheme.colorScheme.primary,
        )
        MetricItem(
            modifier = Modifier.widthIn(min = 120.dp),
            label = stringResource(R.string.analisis_kpi_cxc),
            value = MoneyFormatter.format(kpis.cxc),
            color = if (kpis.cxc > 0) MaterialTheme.colorScheme.alertRed else MaterialTheme.colorScheme.positiveGreen,
        )
        kpis.puntoEquilibrio?.takeIf { it > 0.0 }?.let { pe ->
            MetricItem(
                modifier = Modifier.widthIn(min = 120.dp),
                label = stringResource(R.string.analisis_kpi_pe),
                value = MoneyFormatter.format(pe),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun WaterfallPlCard(steps: List<WaterfallStep>, incompleto: Boolean) {
    val labelRes = mapOf(
        "ventas" to R.string.analisis_wf_ventas,
        "cogs" to R.string.analisis_wf_cogs,
        "bruto" to R.string.analisis_wf_bruto,
        "opex" to R.string.analisis_wf_opex,
        "resultado" to R.string.analisis_wf_resultado,
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.analisis_seccion_pl),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            steps.forEach { step ->
                val labelId = labelRes[step.key] ?: return@forEach
                val isResultado = step.key == "resultado"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(labelId),
                        style = if (isResultado) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                        fontWeight = if (isResultado) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        MoneyFormatter.format(step.monto),
                        style = if (isResultado) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                        fontWeight = if (isResultado) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isResultado && incompleto -> MaterialTheme.colorScheme.warningAmber
                            isResultado && step.monto >= 0 -> MaterialTheme.colorScheme.positiveGreen
                            isResultado -> MaterialTheme.colorScheme.alertRed
                            step.key == "cogs" || step.key == "opex" -> MaterialTheme.colorScheme.alertRed
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
            if (incompleto) {
                Text(
                    stringResource(R.string.analisis_resultado_incompleto_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.warningAmber,
                )
            }
        }
    }
}

@Composable
private fun CajaSectionCard(layout: AnalisisDecisionLayout) {
    val caja = layout.caja
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.analisis_seccion_caja),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            MetricItem(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.analisis_cobraste),
                value = MoneyFormatter.format(caja.cobrado),
                color = MaterialTheme.colorScheme.primary,
            )
            MetricItem(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.analisis_saldo_pendiente),
                value = MoneyFormatter.format(caja.saldoPendiente),
                color = if (caja.saldoPendiente > 0) {
                    MaterialTheme.colorScheme.alertRed
                } else {
                    MaterialTheme.colorScheme.positiveGreen
                },
            )
            caja.proyeccionSaldoNeto?.let { neto ->
                MetricItem(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.analisis_caja_proyeccion_neta),
                    value = MoneyFormatter.format(neto),
                    color = if (neto >= 0) {
                        MaterialTheme.colorScheme.positiveGreen
                    } else {
                        MaterialTheme.colorScheme.alertRed
                    },
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

@Composable
private fun SupuestosImpactoDialog(
    iniciales: SupuestosImpacto,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (SupuestosImpacto) -> Unit,
) {
    var margen by remember { mutableStateOf(iniciales.margenObjetivoPct.toString()) }
    var stock by remember { mutableStateOf(iniciales.recuperoStockPct.toString()) }
    var upside by remember { mutableStateOf(iniciales.upsideVentasPct.toString()) }
    var corte by remember { mutableStateOf(iniciales.corteGastoPct.toString()) }
    var cobranza by remember { mutableStateOf(iniciales.recuperoCobranzaPct.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.analisis_supuestos_titulo), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = margen,
                    onValueChange = { margen = it },
                    label = { Text(stringResource(R.string.analisis_supuestos_margen)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text(stringResource(R.string.analisis_supuestos_stock)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = upside,
                    onValueChange = { upside = it },
                    label = { Text(stringResource(R.string.analisis_supuestos_ventas)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = corte,
                    onValueChange = { corte = it },
                    label = { Text(stringResource(R.string.analisis_supuestos_gasto)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cobranza,
                    onValueChange = { cobranza = it },
                    label = { Text(stringResource(R.string.analisis_supuestos_cobranza)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(
                        when (error) {
                            AnalisisNegocioViewModel.SUPUESTOS_ERROR_FRACCION ->
                                stringResource(R.string.analisis_supuestos_error_fraccion)
                            else -> stringResource(R.string.analisis_supuestos_error)
                        },
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        SupuestosImpacto(
                            margenObjetivoPct = margen.toDoubleOrNull() ?: iniciales.margenObjetivoPct,
                            recuperoStockPct = stock.toDoubleOrNull() ?: iniciales.recuperoStockPct,
                            upsideVentasPct = upside.toDoubleOrNull() ?: iniciales.upsideVentasPct,
                            corteGastoPct = corte.toDoubleOrNull() ?: iniciales.corteGastoPct,
                            recuperoCobranzaPct = cobranza.toDoubleOrNull() ?: iniciales.recuperoCobranzaPct,
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.analisis_supuestos_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.analisis_cancelar)) }
        },
    )
}

@androidx.annotation.VisibleForTesting
@Composable
internal fun RecomendacionCard(
    rec: Recomendacion,
    seguimiento: SeguimientoEstado?,
    feedbackErrorRecId: String?,
    onSeguimiento: (SeguimientoEstado) -> Unit,
    onCta: (CtaRecomendacion) -> Unit = {},
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
    val esfuerzoLabel = when (rec.esfuerzo) {
        EsfuerzoRecomendacion.BAJO -> stringResource(R.string.analisis_esfuerzo_bajo)
        EsfuerzoRecomendacion.MEDIO -> stringResource(R.string.analisis_esfuerzo_medio)
        EsfuerzoRecomendacion.ALTO -> stringResource(R.string.analisis_esfuerzo_alto)
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
                    if (rec.hallazgo.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.analisis_recomendacion_hallazgo, rec.hallazgo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            rec.detalle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (rec.causaProbable.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.analisis_recomendacion_causa, rec.causaProbable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    rec.accion?.let { accion ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.analisis_recomendacion_accion, accion),
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    val impactoLabel = rec.impactoEstimado ?: ImpactoRecomendacion.SIN_IMPACTO
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.analisis_recomendacion_impacto, impactoLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    if (rec.supuestoVisible.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.analisis_recomendacion_supuesto, rec.supuestoVisible),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.analisis_recomendacion_esfuerzo, esfuerzoLabel),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (rec.cta != CtaRecomendacion.NINGUNA && !rec.ctaLabel.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { onCta(rec.cta) }) {
                            Text(rec.ctaLabel)
                        }
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
                    label = stringResource(R.string.analisis_seguimiento_en_curso),
                    selected = seguimiento == SeguimientoEstado.EN_CURSO,
                    onClick = { onSeguimiento(SeguimientoEstado.EN_CURSO) },
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
