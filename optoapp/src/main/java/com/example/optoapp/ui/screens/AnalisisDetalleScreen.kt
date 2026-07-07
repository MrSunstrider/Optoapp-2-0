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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.theme.AlertRed
import com.example.optoapp.ui.theme.PositiveGreen
import com.example.optoapp.ui.theme.TextDark
import com.example.optoapp.ui.theme.WarningAmber
import com.example.optoapp.viewmodel.AnalisisNegocioViewModel
import java.util.Locale

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalisisDetalleScreen(
    navController: NavController,
    viewModel: AnalisisNegocioViewModel = hiltViewModel()
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
                title = "Análisis Completo",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (analisis == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) {
                    LinearProgressIndicator()
                } else {
                    Text("Sin datos disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpandableSection(
                title = "Plata que entró y salió",
                defaultExpanded = true,
                icon = Icons.Default.AccountBalance
            ) {
                BarraIngresosEgresos(analisis = analisis)
            }

            ExpandableSection(
                title = "Lo que más te deja",
                icon = Icons.Default.StarBorder
            ) {
                if (analisis.margenPorCategoria.isEmpty()) {
                    EmptyPlaceholder("Sin datos de categorías")
                } else {
                    analisis.margenPorCategoria.sortedByDescending { it.ventas - it.costos }.forEach { cat ->
                        CategoriaRankingRow(
                            nombre = cat.categoria,
                            ventas = cat.ventas,
                            costos = cat.costos,
                            margenPct = cat.margenPct
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            ExpandableSection(
                title = "Pacientes con deuda pendiente",
                icon = Icons.Default.People
            ) {
                val deudoresTotales = uiState.deudores
                if (deudoresTotales.isEmpty()) {
                    EmptyPlaceholder("Sin deudores pendientes")
                } else {
                    deudoresTotales.forEach { deudor ->
                        DeudorRow(
                            nombre = deudor.pacienteNombre,
                            telefono = deudor.pacienteTelefono,
                            saldo = deudor.saldo,
                            diasDeuda = deudor.diasDeuda,
                            onClick = {
                                if (deudor.pacienteId.isNotBlank()) {
                                    val origenId = deudor.ventaId
                                        .removePrefix("v_disp_")
                                        .removePrefix("v_serv_")
                                    if (deudor.ventaId.startsWith("v_disp_")) {
                                        navController.navigate("editarDispensacion/${deudor.pacienteId}/$origenId")
                                    } else {
                                        navController.navigate("editar_servicio/$origenId")
                                    }
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            ExpandableSection(
                title = "Productos sin vender",
                icon = Icons.Default.Inventory2
            ) {
                if (analisis.stockEstancado.isEmpty()) {
                    EmptyPlaceholder("Sin productos estancados")
                } else {
                    analisis.stockEstancado.forEach { item ->
                        StockRow(
                            modelo = item.modelo,
                            sku = item.sku,
                            stock = item.stockActual,
                            diasSinVenta = item.diasSinVenta,
                            costo = item.costo
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            ExpandableSection(
                title = "Plata que vas a tener",
                icon = Icons.Default.TrendingUp
            ) {
                val proyeccion = analisis.proyeccionCaja
                if (proyeccion == null) {
                    EmptyPlaceholder("Sin datos de proyección")
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
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.SemiBold, color = TextDark)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Contraer" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
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
        BarItem(label = "Ventas", value = analisis.ventasMes, max = maxValor, color = PositiveGreen)
        BarItem(label = "Cobros", value = analisis.cobrosMes, max = maxValor, color = MaterialTheme.colorScheme.primary)
        BarItem(label = "Costos", value = analisis.costoDeVentas(), max = maxValor, color = WarningAmber)
        BarItem(label = "Gastos", value = analisis.gastosMes, max = maxValor, color = AlertRed)
        HorizontalDivider()
        BarItem(label = "Ganancia", value = ganancia, max = maxValor, color = if (ganancia >= 0) PositiveGreen else AlertRed)
    }
}

@Composable
private fun BarItem(label: String, value: Double, max: Double, color: Color) {
    val fraction = (value / max).toFloat().coerceIn(0f, 1f)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("S/ ${formatNumber(value)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
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
            Text("${formatNumber(pct)}%", style = MaterialTheme.typography.bodyMedium, color = if (pct >= 25) PositiveGreen else WarningAmber)
        }
        Text("S/ ${formatNumber(ventas)} ventas · S/ ${formatNumber(margen)} margen",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DeudorRow(nombre: String, telefono: String, saldo: Double, diasDeuda: Int, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(telefono, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("S/ ${formatNumber(saldo)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AlertRed)
        }
        Text("${diasDeuda} días de deuda", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
    }
}

@Composable
private fun StockRow(modelo: String, sku: String, stock: Int, diasSinVenta: Int, costo: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(modelo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SKU: $sku · Stock: $stock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${diasSinVenta}d sin vender", style = MaterialTheme.typography.bodySmall, color = AlertRed)
        }
        Text("Costo: S/ ${formatNumber(costo)} c/u", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProyeccionCard(proyeccion: com.example.optoapp.domain.ProyeccionCaja) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ingresos esperados", style = MaterialTheme.typography.bodyMedium)
            Text("S/ ${formatNumber(proyeccion.ingresosEsperados)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PositiveGreen)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Egresos programados", style = MaterialTheme.typography.bodyMedium)
            Text("S/ ${formatNumber(proyeccion.egresosProgramados)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AlertRed)
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Saldo neto proyectado", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("S/ ${formatNumber(proyeccion.saldoNeto)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (proyeccion.saldoNeto >= 0) PositiveGreen else AlertRed)
        }
    }
}

@Composable
private fun EmptyPlaceholder(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatNumber(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        String.format(Locale.getDefault(), "%,.0f", value)
    } else {
        String.format(Locale.getDefault(), "%,.1f", value)
    }
}

private fun AnalisisMensual.costoDeVentas(): Double =
    margenPorCategoria.sumOf { it.costos }
