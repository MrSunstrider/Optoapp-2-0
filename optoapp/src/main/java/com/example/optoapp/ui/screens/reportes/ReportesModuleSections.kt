package com.example.optoapp.ui.screens.reportes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.domain.ClinicoReporte
import com.example.optoapp.domain.EquipoReporte
import com.example.optoapp.domain.InventarioReporte
import com.example.optoapp.domain.PacientesReporte
import com.example.optoapp.domain.PeriodComparison
import com.example.optoapp.ui.components.OptoKpiCard
import com.example.optoapp.ui.components.charts.OptoBarChart
import com.example.optoapp.ui.components.charts.OptoDonutChart
import com.example.optoapp.ui.theme.warningAmber
import com.example.optoapp.util.MoneyFormatter

@Composable
private fun ReportesEmptyBlock(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ReportesVentasVisuals(
    comparison: PeriodComparison?,
    dispensacionesMonto: Double,
    serviciosMonto: Double,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        comparison?.let { cmp ->
            OptoBarChart(
                title = stringResource(R.string.reportes_chart_vs_periodo),
                series = ReportesChartsModel.ventasComparativo(
                    cmp = cmp,
                    actualLabel = stringResource(R.string.reportes_chart_periodo_actual),
                    anteriorLabel = stringResource(R.string.reportes_chart_periodo_anterior),
                    vendidoLabel = stringResource(R.string.reportes_vendido),
                    margenLabel = stringResource(R.string.reportes_chart_margen),
                ),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.reportes_vs_periodo_anterior), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val vendidoPct = cmp.vendidoDeltaPct()?.let { String.format("%.1f%%", it) } ?: "—"
                    val margenPct = cmp.margenDeltaPct()?.let { String.format("%.1f%%", it) } ?: "—"
                    Text(stringResource(R.string.reportes_delta_vendido, MoneyFormatter.format(cmp.vendidoDelta), vendidoPct), fontSize = 13.sp)
                    Text(stringResource(R.string.reportes_delta_margen, MoneyFormatter.format(cmp.margenDelta), margenPct), fontSize = 13.sp)
                    Text(stringResource(R.string.reportes_delta_tx, cmp.transaccionesDelta), fontSize = 13.sp)
                }
            }
        }
        OptoDonutChart(
            title = stringResource(R.string.reportes_chart_mix_ventas),
            slices = ReportesChartsModel.ventasMix(
                dispensacionesMonto = dispensacionesMonto,
                serviciosMonto = serviciosMonto,
                otLabel = stringResource(R.string.reportes_dispensaciones),
                serviciosLabel = stringResource(R.string.reportes_servicios_extra),
            ),
        )
    }
}

@Composable
fun ReportesPacientesVisuals(reporte: PacientesReporte, modifier: Modifier = Modifier) {
    if (reporte.altas == 0) {
        ReportesEmptyBlock(
            icon = Icons.Default.PersonAdd,
            title = stringResource(R.string.reportes_pacientes_empty_title),
            subtitle = stringResource(R.string.reportes_pacientes_empty_subtitle),
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OptoKpiCard(
                title = stringResource(R.string.reportes_kpi_altas),
                value = reporte.altas.toString(),
                color = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.PersonAdd,
                modifier = Modifier.weight(1f),
            )
            OptoKpiCard(
                title = stringResource(R.string.reportes_kpi_hombres),
                value = reporte.hombres.toString(),
                color = MaterialTheme.colorScheme.tertiary,
                icon = Icons.Default.Groups,
                modifier = Modifier.weight(1f),
            )
            OptoKpiCard(
                title = stringResource(R.string.reportes_kpi_mujeres),
                value = reporte.mujeres.toString(),
                color = MaterialTheme.colorScheme.secondary,
                icon = Icons.Default.Groups,
                modifier = Modifier.weight(1f),
            )
        }
        OptoDonutChart(
            title = stringResource(R.string.reportes_chart_sexo),
            slices = ReportesChartsModel.sexoSlices(
                reporte = reporte,
                hombresLabel = stringResource(R.string.reportes_kpi_hombres),
                mujeresLabel = stringResource(R.string.reportes_kpi_mujeres),
                sinDatoLabel = stringResource(R.string.reportes_kpi_sin_sexo),
            ),
        )
    }
}

@Composable
fun ReportesInventarioVisuals(reporte: InventarioReporte, modifier: Modifier = Modifier) {
    if (reporte.modelos == 0) {
        ReportesEmptyBlock(
            icon = Icons.Default.Inventory2,
            title = stringResource(R.string.reportes_inv_empty_title),
            subtitle = stringResource(R.string.reportes_inv_empty_subtitle),
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OptoKpiCard(
                title = stringResource(R.string.reportes_kpi_modelos),
                value = reporte.modelos.toString(),
                color = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Inventory2,
                modifier = Modifier.weight(1f),
            )
            OptoKpiCard(
                title = stringResource(R.string.reportes_kpi_stock),
                value = reporte.stockTotal.toString(),
                color = MaterialTheme.colorScheme.tertiary,
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OptoKpiCard(
                title = stringResource(R.string.reportes_kpi_bajo),
                value = reporte.lowStock.toString(),
                color = MaterialTheme.colorScheme.warningAmber,
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f),
            )
            OptoKpiCard(
                title = stringResource(R.string.reportes_kpi_salidas),
                value = reporte.salidasVentaPeriodo.toString(),
                color = MaterialTheme.colorScheme.secondary,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f),
            )
        }
        OptoDonutChart(
            title = stringResource(R.string.reportes_chart_stock_estado),
            slices = ReportesChartsModel.inventarioEstado(
                reporte = reporte,
                okLabel = stringResource(R.string.reportes_inv_ok),
                bajoLabel = stringResource(R.string.reportes_inv_critico),
            ),
        )
        if (reporte.criticos.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.reportes_inv_criticos_titulo), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    reporte.criticos.forEach { row ->
                        Text(
                            stringResource(R.string.reportes_inv_critico_fila, row.label, row.stockActual, row.stockMinimo),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportesClinicoVisuals(reporte: ClinicoReporte, modifier: Modifier = Modifier) {
    if (reporte.evaluaciones == 0) {
        ReportesEmptyBlock(
            icon = Icons.Default.MedicalServices,
            title = stringResource(R.string.reportes_clinico_empty_title),
            subtitle = stringResource(R.string.reportes_clinico_empty_subtitle),
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OptoKpiCard(
            title = stringResource(R.string.reportes_kpi_evals),
            value = reporte.evaluaciones.toString(),
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.MedicalServices,
            modifier = Modifier.fillMaxWidth(),
        )
        OptoDonutChart(
            title = stringResource(R.string.reportes_chart_diagnosticos),
            slices = ReportesChartsModel.clinicoDiagnosticos(
                reporte = reporte,
                otrosLabel = stringResource(R.string.reportes_clinico_otros),
            ),
        )
    }
}

@Composable
fun ReportesEquipoVisuals(
    reporte: EquipoReporte,
    comisionPct: Double,
    canEditComision: Boolean,
    onEditComision: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasData = reporte.filas.isNotEmpty() || reporte.sinAtribuirCount > 0
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { if (canEditComision) onEditComision() },
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.reportes_equipo_comision_pct, comisionPct), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(
                        if (canEditComision) R.string.reportes_equipo_pct_hint else R.string.reportes_equipo_pct_readonly_hint,
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!hasData) {
            ReportesEmptyBlock(
                icon = Icons.Default.Groups,
                title = stringResource(R.string.reportes_equipo_empty_title),
                subtitle = stringResource(R.string.reportes_equipo_empty_subtitle),
            )
            return
        }
        val series = ReportesChartsModel.equipoVendido(
            reporte = reporte,
            serieName = stringResource(R.string.reportes_vendido),
            sinAtribuirLabel = stringResource(R.string.reportes_equipo_sin_atribuir_corto),
        )
        if (series.isNotEmpty()) {
            OptoBarChart(
                title = stringResource(R.string.reportes_chart_equipo_vendido),
                series = series,
            )
        }
        reporte.filas.forEach { row ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        stringResource(
                            R.string.reportes_equipo_fila_detalle,
                            MoneyFormatter.format(row.vendido),
                            row.transacciones,
                            MoneyFormatter.format(row.comision),
                        ),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (reporte.sinAtribuirCount > 0) {
            Text(
                stringResource(
                    R.string.reportes_equipo_sin_atribuir,
                    MoneyFormatter.format(reporte.sinAtribuirVendido),
                    reporte.sinAtribuirCount,
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.warningAmber,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}
