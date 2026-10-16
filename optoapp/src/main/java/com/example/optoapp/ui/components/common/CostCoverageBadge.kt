package com.example.optoapp.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.optoapp.R
import com.example.optoapp.domain.CoberturaCosto
import com.example.optoapp.domain.FuenteDatos
import java.math.BigDecimal
import java.math.RoundingMode

data class CoverageShares(val real: String, val estimado: String, val faltante: String)

data class CostCoverageCopy(
    val title: String,
    val sinDatos: String,
    val offlineNote: String,
    val realTemplate: String,
    val estimadoTemplate: String,
    val faltanteTemplate: String,
)

data class CostCoverageBadgeModel(val shares: CoverageShares?, val offline: Boolean) {
    val showsSinDatos: Boolean get() = shares == null

    fun visibleLabel(copy: CostCoverageCopy): String? = if (shares == null) copy.sinDatos else null

    fun shareLabels(copy: CostCoverageCopy): List<String> = shares?.let {
        listOf(
            copy.realTemplate.format(it.real),
            copy.estimadoTemplate.format(it.estimado),
            copy.faltanteTemplate.format(it.faltante),
        )
    }.orEmpty()

    fun contentDescription(copy: CostCoverageCopy): String {
        val detail = when {
            shares != null -> shareLabels(copy).joinToString(", ")
            offline -> "${copy.sinDatos}, ${copy.offlineNote}"
            else -> copy.sinDatos
        }
        return "${copy.title}: $detail"
    }

    companion object {
        // Offline facts never carry coverage counts, so any percentage there would be a fabricated zero.
        fun from(cobertura: CoberturaCosto, fuente: FuenteDatos): CostCoverageBadgeModel {
            val offline = fuente == FuenteDatos.OFFLINE
            val shares = if (offline || cobertura.sinDatos) {
                null
            } else {
                CoverageShares(
                    real = wholePercent(cobertura.lineasReales, cobertura.lineasTotal),
                    estimado = wholePercent(cobertura.lineasEstimadas, cobertura.lineasTotal),
                    faltante = wholePercent(cobertura.lineasFaltantes, cobertura.lineasTotal),
                )
            }
            return CostCoverageBadgeModel(shares, offline)
        }

        private fun wholePercent(part: Int, total: Int): String =
            BigDecimal(part * 100).divide(BigDecimal(total), 0, RoundingMode.HALF_UP).toPlainString() + " %"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CostCoverageBadge(
    cobertura: CoberturaCosto,
    fuente: FuenteDatos,
    modifier: Modifier = Modifier,
) {
    val copy = CostCoverageCopy(
        title = stringResource(R.string.cobertura_titulo),
        sinDatos = stringResource(R.string.cobertura_sin_datos),
        offlineNote = stringResource(R.string.cobertura_offline_nota),
        realTemplate = stringResource(R.string.cobertura_real),
        estimadoTemplate = stringResource(R.string.cobertura_estimado),
        faltanteTemplate = stringResource(R.string.cobertura_faltante),
    )
    val model = CostCoverageBadgeModel.from(cobertura, fuente)
    val description = model.contentDescription(copy)

    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(copy.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val sinDatos = model.visibleLabel(copy)
            if (sinDatos != null) {
                Text(sinDatos, style = MaterialTheme.typography.bodyMedium)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    model.shareLabels(copy).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}
