package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// WHY: shared by Reportes and ServiciosExtra so KPI styling does not drift (A13).
@Composable
fun OptoKpiCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconContentDescription: String? = title,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = iconContentDescription, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // WHY: three cards share one row; long money strings (S/ 6,205.00) must stay on one line
            Text(
                value,
                modifier = Modifier.fillMaxWidth(),
                fontSize = if (value.length > 10) 14.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
