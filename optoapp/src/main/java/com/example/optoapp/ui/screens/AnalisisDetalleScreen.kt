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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.optoapp.R
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.theme.alertRed
import com.example.optoapp.ui.theme.positiveGreen
import com.example.optoapp.ui.theme.warningAmber
import com.example.optoapp.util.AnalisisPercentFormatter
import com.example.optoapp.util.MoneyFormatter
import com.example.optoapp.viewmodel.AnalisisNegocioViewModel

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalisisDetalleScreen(
    navController: NavController,
    viewModel: AnalisisNegocioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val analisis = uiState.analisis

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = stringResource(R.string.analisis_detalle_title),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.analisis_atras_cd))
                    }
                },
            )
        },
    ) { padding ->
        if (analisis == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) {
                    LinearProgressIndicator()
                } else {
                    Text(stringResource(R.string.analisis_sin_datos_disponibles), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            ExpandableSection(
                title = stringResource(R.string.analisis_seccion_flujo),
                defaultExpanded = true,
                icon = Icons.Default.AccountBalance,
            ) {
                BarraIngresosEgresos(analisis = analisis)
            }

            ExpandableSection(
                title = stringResource(R.string.analisis_seccion_ranking),
                icon = Icons.Default.StarBorder,
            ) {
                if (analisis.margenPorCategoria.isEmpty()) {
                    EmptyPlaceholder(stringResource(R.string.analisis_sin_categorias))
                } else {
                    analisis.margenPorCategoria.sortedByDescending { it.ventas - it.costos }.forEach { cat ->
                        CategoriaRankingRow(
                            nombre = cat.categoria,
                            ventas = cat.ventas,
                            costos = cat.costos,
                            margenPct = cat.margenPct,
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            ExpandableSection(
                title = stringResource(R.string.analisis_seccion_deudores),
                icon = Icons.Default.People,
            ) {
                val deudoresTotales = uiState.deudores
                if (deudoresTotales.isEmpty()) {
                    EmptyPlaceholder(stringResource(R.string.analisis_sin_deudores))
                } else {
                    deudoresTotales.forEach { deudor ->
                        DeudorRow(
                            nombre = deudor.pacienteNombre,
                            telefono = deudor.pacienteTelefono,
                            saldo = deudor.saldo,
                            diasDeuda = deudor.diasDeuda,
                            onClick = {
                                rutaEdicionDeudor(deudor.ventaId, deudor.pacienteId)?.let { navController.navigate(it) }
                            },
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            ExpandableSection(
                title = stringResource(R.string.analisis_seccion_stock),
                icon = Icons.Default.Inventory2,
            ) {
                if (analisis.stockEstancado.isEmpty()) {
                    EmptyPlaceholder(stringResource(R.string.analisis_sin_stock_estancado))
                } else {
                    analisis.stockEstancado.forEach { item ->
                        StockRow(
                            modelo = item.modelo,
                            sku = item.sku,
                            stock = item.stockActual,
                            diasSinVenta = item.diasSinVenta,
                            costo = item.costo,
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            ExpandableSection(
                title = stringResource(R.string.analisis_seccion_proyeccion),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
            ) {
                val proyeccion = analisis.proyeccionCaja
                if (proyeccion == null) {
                    EmptyPlaceholder(stringResource(R.string.analisis_sin_proyeccion))
                } else {
                    ProyeccionCard(proyeccion = proyeccion)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    defaultExpanded: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.analisis_contraer_cd else R.string.analisis_expandir_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun BarraIngresosEgresos(analisis: AnalisisMensual) {
    val ganancia = analisis.ventasMes - analisis.costoDeVentas() - analisis.gastosMes
    val maxValor = maxOf(analisis.ventasMes, analisis.cobrosMes, analisis.costoDeVentas(), analisis.gastosMes, ganancia, 1.0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BarItem(label = stringResource(R.string.analisis_ventas), value = analisis.ventasMes, max = maxValor, color = MaterialTheme.colorScheme.positiveGreen)
        BarItem(label = stringResource(R.string.analisis_cobros), value = analisis.cobrosMes, max = maxValor, color = MaterialTheme.colorScheme.primary)
        BarItem(label = stringResource(R.string.analisis_costos), value = analisis.costoDeVentas(), max = maxValor, color = MaterialTheme.colorScheme.warningAmber)
        BarItem(label = stringResource(R.string.analisis_gastos), value = analisis.gastosMes, max = maxValor, color = MaterialTheme.colorScheme.alertRed)
        HorizontalDivider()
        BarItem(label = stringResource(R.string.analisis_ganancia), value = ganancia, max = maxValor, color = if (ganancia >= 0) MaterialTheme.colorScheme.positiveGreen else MaterialTheme.colorScheme.alertRed)
    }
}

@Composable
private fun BarItem(label: String, value: Double, max: Double, color: Color) {
    val fraction = (value / max).toFloat().coerceIn(0f, 1f)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(MoneyFormatter.format(value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun CategoriaRankingRow(nombre: String, ventas: Double, costos: Double, margenPct: Double?) {
    val margen = ventas - costos
    val pct = margenPct ?: if (ventas > 0) (margen / ventas * 100) else 0.0
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(formatPercent(pct), style = MaterialTheme.typography.bodyMedium, color = if (pct >= 25) MaterialTheme.colorScheme.positiveGreen else MaterialTheme.colorScheme.warningAmber)
        }
        Text(
            stringResource(R.string.analisis_categoria_ventas_margen, MoneyFormatter.format(ventas), MoneyFormatter.format(margen)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeudorRow(nombre: String, telefono: String, saldo: Double, diasDeuda: Int, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        Spacer(Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(telefono, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(MoneyFormatter.format(saldo), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.alertRed)
        }
        Text(pluralStringResource(R.plurals.analisis_dias_deuda, diasDeuda, diasDeuda), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.warningAmber)
    }
}

@Composable
private fun StockRow(modelo: String, sku: String, stock: Int, diasSinVenta: Int, costo: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(modelo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.analisis_stock_sku, sku, stock), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.analisis_dias_sin_vender, diasSinVenta), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.alertRed)
        }
        Text(stringResource(R.string.analisis_stock_costo_unitario, MoneyFormatter.format(costo)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProyeccionCard(proyeccion: com.example.optoapp.domain.ProyeccionCaja) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.analisis_ingresos_esperados), style = MaterialTheme.typography.bodyMedium)
            Text(MoneyFormatter.format(proyeccion.ingresosEsperados), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.positiveGreen)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.analisis_egresos_programados), style = MaterialTheme.typography.bodyMedium)
            Text(MoneyFormatter.format(proyeccion.egresosProgramados), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.alertRed)
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.analisis_saldo_neto_proyectado), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(MoneyFormatter.format(proyeccion.saldoNeto), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (proyeccion.saldoNeto >= 0) MaterialTheme.colorScheme.positiveGreen else MaterialTheme.colorScheme.alertRed)
        }

        if (proyeccion.mesesHistoricos < 3) {
            Spacer(Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.warningAmber.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.warningAmber,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (proyeccion.mesesHistoricos == 0) {
                            stringResource(R.string.analisis_proyeccion_sin_historial)
                        } else {
                            pluralStringResource(R.plurals.analisis_proyeccion_meses, proyeccion.mesesHistoricos, proyeccion.mesesHistoricos)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.warningAmber,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPlaceholder(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

private const val VENTA_DISPENSACION = "v_disp_"
private const val VENTA_SERVICIO = "v_serv_"

internal fun rutaEdicionDeudor(ventaId: String, pacienteId: String): String? {
    if (pacienteId.isBlank()) return null
    return if (ventaId.startsWith(VENTA_DISPENSACION)) {
        Route.EditarDispensacion(pacienteId, ventaId.removePrefix(VENTA_DISPENSACION)).route
    } else {
        Route.EditarServicio(ventaId.removePrefix(VENTA_SERVICIO)).route
    }
}
// WHY: a device-locale format would print "25,0%" next to amounts that MoneyFormatter always prints with '.'.
private fun formatPercent(value: Double): String = AnalisisPercentFormatter.formatWithSymbol(value)
