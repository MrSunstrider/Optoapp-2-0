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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!ot.isNullOrBlank()) {
                Text("OT: $ot", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
