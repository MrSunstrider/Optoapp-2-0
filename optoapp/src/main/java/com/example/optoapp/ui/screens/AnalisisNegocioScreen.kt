package com.example.optoapp.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.domain.Prioridad
import com.example.optoapp.domain.Recomendacion
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.theme.AlertRed
import com.example.optoapp.ui.theme.PositiveGreen
import com.example.optoapp.ui.theme.TextDark
import com.example.optoapp.ui.theme.WarningAmber
import com.example.optoapp.viewmodel.AnalisisNegocioViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalisisNegocioScreen(
    navController: NavController,
    viewModel: AnalisisNegocioViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val canView = AppRoles.canViewBiAndReports(opticaRol)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OptoTopAppBar(
                title = "Mi Negocio",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (!canView) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.analisis?.esOffline == true) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Datos limitados — sin conexión", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                    }
                }
            }

            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = AlertRed, modifier = Modifier.size(32.dp))
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
                onNext = { viewModel.navigateMonth(1) }
            )

            uiState.analisis?.let { analisis ->
                ResumenCard(analisis = analisis)
            }

            if (uiState.recomendaciones.isNotEmpty()) {
                Text(
                    "Recomendaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                uiState.recomendaciones.take(3).forEach { rec ->
                    RecomendacionCard(rec)
                }
            }

            if (!uiState.isLoading && uiState.analisis == null && uiState.error == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Sin datos para este mes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (uiState.analisis != null) {
                Button(
                    onClick = { navController.navigate("analisis_detalle") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
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
    onNext: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "PE"))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mes anterior")
            }
            Text(
                text = currentMonth.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mes siguiente")
            }
        }
    }
}

@Composable
private fun ResumenCard(analisis: AnalisisMensual) {
    val saldo = analisis.ventasMes - analisis.cobrosMes
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Resumen del mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Vendiste",
                    value = "S/ ${formatNumber(analisis.ventasMes)}",
                    color = PositiveGreen
                )
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Cobraste",
                    value = "S/ ${formatNumber(analisis.cobrosMes)}",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Saldo pendiente",
                    value = "S/ ${formatNumber(saldo)}",
                    color = if (saldo > 0) AlertRed else PositiveGreen
                )
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Margen",
                    value = "${formatNumber(analisis.margenNetoPct)}%",
                    color = if (analisis.margenNetoPct >= 25) PositiveGreen else WarningAmber
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
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun RecomendacionCard(rec: Recomendacion) {
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rec.titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    rec.detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                rec.accion?.let { accion ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "→ $accion",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Text(
                    rec.prioridad.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatNumber(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        String.format(Locale.getDefault(), "%,.0f", value)
    } else {
        String.format(Locale.getDefault(), "%,.1f", value)
    }
}
