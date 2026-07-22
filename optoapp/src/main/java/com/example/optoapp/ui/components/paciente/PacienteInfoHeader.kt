package com.example.optoapp.ui.components.paciente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.data.Paciente
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PacienteInfoHeader(paciente: Paciente, deudaTotal: Double = 0.0) {
    val deudaColor = if (deudaTotal > 0) Color(0xFFD32F2F) else Color(0xFF388E3C)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = "Persona", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = paciente.nombreCompleto, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    if (paciente.dni?.isNotBlank() == true) {
                        Text(text = "DNI: ${paciente.dni}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            // Deuda total
            if (deudaTotal > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = deudaColor.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Billetera", tint = deudaColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Deuda total: s/. ${String.format(Locale.US, "%,.2f", deudaTotal)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = deudaColor)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Datos organizados en grilla
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem(label = "Edad", value = "${paciente.edad} años", modifier = Modifier.weight(1f))
                InfoItem(label = "Teléfono", value = paciente.telefono.ifBlank { "—" }, modifier = Modifier.weight(1f))
                InfoItem(label = "Sexo", value = paciente.sexo ?: "—", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                paciente.distrito?.let { dist ->
                    if (dist.isNotBlank()) {
                        InfoItem(label = "Distrito", value = dist, modifier = Modifier.weight(1f))
                    }
                }
                paciente.email?.let { em ->
                    if (em.isNotBlank()) {
                        InfoItem(label = "Email", value = em, modifier = Modifier.weight(1f))
                    }
                }
                InfoItem(label = "HO", value = paciente.historiaOptometrica ?: "—", modifier = Modifier.weight(1f))
            }

            if (paciente.ultimasEtiquetas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    paciente.ultimasEtiquetas.forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(tag, fontSize = 11.sp) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Text(text = value.ifBlank { "—" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
