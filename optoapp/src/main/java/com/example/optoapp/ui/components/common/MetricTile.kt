package com.example.optoapp.ui.components.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.optoapp.R
import com.example.optoapp.ui.theme.LocalOptoDarkTheme
import com.example.optoapp.ui.theme.SemanticPalette

enum class MetricStatus { BUENO, ATENCION, CRITICO }

// Each status pairs a word and an icon with its color so the state never depends on color perception.
data class MetricStatusStyle(@StringRes val labelRes: Int, val icon: ImageVector, val color: Color) {
    companion object {
        fun of(status: MetricStatus, palette: SemanticPalette): MetricStatusStyle = when (status) {
            MetricStatus.BUENO -> MetricStatusStyle(R.string.metric_estado_bueno, Icons.Filled.CheckCircle, palette.positiveGreen)
            MetricStatus.ATENCION -> MetricStatusStyle(R.string.metric_estado_atencion, Icons.Filled.Warning, palette.warningAmber)
            MetricStatus.CRITICO -> MetricStatusStyle(R.string.metric_estado_critico, Icons.Filled.Error, palette.alertRed)
        }
    }
}

data class MetricTileContent(val label: String, val value: String, val delta: String?, val status: MetricStatus?) {
    val visibleDelta: String? = delta?.trim()?.takeIf { it.isNotEmpty() }

    fun contentDescription(statusLabel: String?): String =
        listOfNotNull(label, value, visibleDelta, statusLabel).joinToString(", ")
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    status: MetricStatus? = null,
) {
    val content = MetricTileContent(label, value, delta, status)
    val style = status?.let { MetricStatusStyle.of(it, SemanticPalette.forDarkTheme(LocalOptoDarkTheme.current)) }
    val statusLabel = style?.let { stringResource(it.labelRes) }
    val description = content.contentDescription(statusLabel)

    OutlinedCard(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
            content.visibleDelta?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (style != null && statusLabel != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(16.dp))
                    Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = style.color)
                }
            }
        }
    }
}
