package com.example.optoapp.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SuggestionCard(k1Od: String, k2Od: String, k1Oi: String, k2Oi: String) {
    val odDiff = abs((k1Od.toFloatOrNull() ?: 0f) - (k2Od.toFloatOrNull() ?: 0f))
    val oiDiff = abs((k1Oi.toFloatOrNull() ?: 0f) - (k2Oi.toFloatOrNull() ?: 0f))
    
    if (k1Od.isNotBlank() && k2Od.isNotBlank()) {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Sugerencia Lente de Contacto (OD):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                SuggestionText(odDiff)
            }
        }
    }
    
    if (k1Oi.isNotBlank() && k2Oi.isNotBlank()) {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Sugerencia Lente de Contacto (OI):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                SuggestionText(oiDiff)
            }
        }
    }
}

@Composable
fun SuggestionText(diff: Float) {
    val (text, color) = when {
        diff >= 4.00f -> "Sugerencia: Lente RGP (rígido gas permeable) – Astigmatismo corneal alto." to MaterialTheme.colorScheme.error
        diff >= 2.50f -> "Sugerencia: Valorar RGP o lente tórico blando – Astigmatismo corneal moderado." to MaterialTheme.colorScheme.secondary
        else -> "Sugerencia: Lente blando (esférico o tórico según refracción) – Astigmatismo corneal bajo." to MaterialTheme.colorScheme.tertiary
    }
    
    Text(text = text, color = color, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    Text(
        text = "Considerar también comodidad del paciente, estilo de vida y regularidad corneal.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
}

@Composable
fun CalculationCard(
    title: String,
    sphStr: String,
    cylStr: String,
    recorteChecked: Boolean,
    onRecorteChange: (Boolean) -> Unit,
    onCalculation: (String, String) -> Unit
) {
    val sph = parseRefraction(sphStr)
    val cyl = parseRefraction(cylStr)
    
    val canRecortar = abs(cyl) <= abs(sph) / 4f
    
    val resultSph: Float
    val resultCyl: Float
    
    if (recorteChecked && canRecortar) {
        val sphTemp = sph + (cyl / 2f)
        resultSph = applyVertexDistance(sphTemp)
        resultCyl = 0f
    } else {
        resultSph = applyVertexDistance(sph)
        resultCyl = cyl
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = recorteChecked, onCheckedChange = onRecorteChange)
                Text("Aplicar recorte de cilindro", fontSize = 14.sp)
            }
            if (recorteChecked && !canRecortar && cyl != 0f) {
                Text("El cilindro supera el límite para recorte, se requiere lente tórica.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            
            Text("Recomendación LC: Esf ${formatDiopter(resultSph)}${if (resultCyl != 0f) " / Cil ${formatDiopter(resultCyl)}" else ""}", 
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Button(
                onClick = { onCalculation(formatDiopter(resultSph), formatDiopter(resultCyl)) },
                modifier = Modifier.align(Alignment.End).height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Cargar", fontSize = 12.sp)
            }
        }
    }
}

private fun applyVertexDistance(power: Float): Float {
    if (power == 0f) return 0f
    val result = power / (1f - (0.012f * power))
    return (result * 4).roundToInt() / 4f
}

private fun parseRefraction(v: String): Float {
    val clean = v.lowercase().trim()
    if (clean in listOf("plano", "neutro", "pl", "nt")) return 0f
    return clean.replace(",", ".").toFloatOrNull() ?: 0f
}

private fun formatDiopter(value: Float): String {
    return String.format(Locale.getDefault(), "%.2f", value)
}
