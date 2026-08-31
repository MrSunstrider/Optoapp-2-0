package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.optoapp.ui.theme.LocalOptoDensity
import com.example.optoapp.util.DateUtils
import java.time.LocalDate

@Composable
fun PatientContextCard(
    pacienteNombre: String,
    modifier: Modifier = Modifier,
    ot: String? = null,
    fecha: LocalDate? = null,
    descripcion: String? = null,
) {
    val density = LocalOptoDensity.current
    val isCompact = density.isCompact
    val otFontSize = if (isCompact) 16.sp else 18.sp

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(density.cardPadding),
            verticalArrangement = Arrangement.spacedBy(density.blockGap / 2),
        ) {
            if (!ot.isNullOrBlank()) {
                Text("OT: $ot", fontWeight = FontWeight.Bold, fontSize = otFontSize)
            }
            Text("Paciente: $pacienteNombre", fontSize = 14.sp)
            if (fecha != null) {
                Text(
                    "Fecha: ${DateUtils.formatLocalized(fecha)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!descripcion.isNullOrBlank()) {
                Text(descripcion, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
