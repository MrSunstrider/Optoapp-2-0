package com.example.optoapp.ui.components.cierre_caja

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

@Composable
fun ResumenCard(label: String, monto: Double, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                "s/. ${String.format(Locale.getDefault(), "%.0f", monto)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}
