package com.example.optoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.viewmodel.BIViewModel
import com.example.optoapp.viewmodel.Periodo
import com.example.optoapp.ui.components.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BIScreen(navController: NavController, viewModel: BIViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val comparativoLabel = when (uiState.periodo) {
        Periodo.MES_ACTUAL -> "Mes anterior"
        Periodo.TRIMESTRE -> "Trimestre anterior"
        Periodo.SEMESTRE -> "Semestre anterior"
        Periodo.ANIO -> "Año anterior"
    }
    val actualLabel = when (uiState.periodo) {
        Periodo.MES_ACTUAL -> "Mes actual"
        Periodo.TRIMESTRE -> "Trimestre actual"
        Periodo.SEMESTRE -> "Semestre actual"
        Periodo.ANIO -> "Año actual"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Panel de Estadísticas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selector de Periodo
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                Periodo.values().forEachIndexed { index, p ->
                    SegmentedButton(
                        selected = uiState.periodo == p,
                        onClick = { viewModel.setPeriodo(p) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Periodo.values().size)
                    ) {
                        Text(p.label, fontSize = 11.sp)
                    }
                }
            }
            Text(
                text = "Comparando con: $comparativoLabel",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Exámenes Section
            KPICard(
                title = "Exámenes Realizados",
                current = uiState.examenesActual.toString(),
                previous = uiState.examenesAnterior.toString(),
                icon = Icons.Default.Insights,
                color = MaterialTheme.colorScheme.primary,
                previousLabel = comparativoLabel
            ) {
                BarChart(
                    actual = uiState.examenesActual,
                    anterior = uiState.examenesAnterior,
                    labelActual = actualLabel,
                    labelAnterior = comparativoLabel,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Recaudación Section
            KPICard(
                title = "Recaudación vs. Proyección",
                current = "s/. ${String.format(Locale.getDefault(), "%.0f", uiState.recaudacionCobrada)}",
                previous = "Meta: s/. ${String.format(Locale.getDefault(), "%.0f", uiState.recaudacionProyectada)}",
                color = MaterialTheme.colorScheme.primary,
                previousLabel = comparativoLabel,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    DonutChart(uiState.recaudacionProyectada, uiState.recaudacionCobrada, Modifier.size(120.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        LegendItem("Cobrado", MaterialTheme.colorScheme.secondary)
                        LegendItem("Pendiente", MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                        val brecha = uiState.recaudacionProyectada - uiState.recaudacionCobrada
                        if (brecha > 0) {
                            Text("Faltan s/. ${String.format(Locale.getDefault(), "%.2f", brecha)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        } else if (uiState.recaudacionProyectada > 0) {
                            Text("¡Meta cumplida!", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // P3-T8: inventario, entregas y movimientos (misma óptica y período que arriba)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Operación e inventario",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                    BiMetricRow("Entregas pendientes (en período)", uiState.entregasPendientesPeriodo.toString())
                    BiMetricRow("Entregas completadas (en período)", uiState.entregasCompletadasPeriodo.toString())
                    BiMetricRow("Ventas con montura de catálogo", uiState.ventasConMonturaCatalogo.toString())
                    BiMetricRow("Referencias en stock bajo (ahora)", uiState.monturasStockBajo.toString())
                    BiMetricRow("Salidas de inventario por venta", uiState.salidasInventarioVentas.toString())
                    Text(
                        "Stock bajo y salidas usan inventario y movimientos de la óptica activa.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Best Sellers Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
        Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top 5 Productos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.topProductos.isEmpty()) {
                        Text("Sin datos en este periodo", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        HorizontalRankingChart(uiState.topProductos)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BiMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun KPICard(
    title: String,
    current: String,
    previous: String,
    icon: ImageVector? = null,
    color: Color,
    previousLabel: String = "Anterior",
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(current, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
                icon?.let {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = color.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(it, contentDescription = null, tint = color)
                        }
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                val isGrowth = try { 
                    val cur = current.replace(Regex("[^0-9.]"), "").toFloat()
                    val prev = previous.replace(Regex("[^0-9.]"), "").toFloat()
                    cur >= prev
                } catch(e: Exception) { true }
                
                Icon(
                    if (isGrowth) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isGrowth) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (title.contains("Recaudación")) previous else "$previousLabel: $previous",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            content()
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
