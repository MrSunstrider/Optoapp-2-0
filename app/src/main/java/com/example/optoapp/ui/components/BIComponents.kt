package com.example.optoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
fun BarChart(
    actual: Int,
    anterior: Int,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(actual, anterior, 1).toFloat()
    val animState = remember { Animatable(0f) }
    
    LaunchedEffect(actual, anterior) {
        animState.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        ChartBar("Mes Ant.", anterior, anterior / maxVal * animState.value, MaterialTheme.colorScheme.outline)
        ChartBar("Este Mes", actual, actual / maxVal * animState.value, MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ChartBar(label: String, value: Int, ratio: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value.toString(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(150.dp * ratio)
                .graphicsLayer(clip = true, shape = MaterialTheme.shapes.small)
                .drawWithContent {
                    drawRect(color)
                    drawRect(
                        brush = Brush.verticalGradient(listOf(onSurfaceColor.copy(0.1f), Color.Transparent)),
                        size = size
                    )
                    drawContent()
                }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DonutChart(
    proyectado: Double,
    cobrado: Double,
    modifier: Modifier = Modifier
) {
    val percentage = if (proyectado > 0) (cobrado / proyectado).toFloat() else 0f
    val animState = remember { Animatable(0f) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    
    LaunchedEffect(percentage) {
        animState.animateTo(percentage, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }
    
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val strokeWidth = 20.dp.toPx()
            val innerRadius = (size.minDimension - strokeWidth) / 2
            
            // Background track
            drawCircle(
                color = outlineVariant.copy(alpha = 0.2f),
                radius = innerRadius,
                style = Stroke(width = strokeWidth)
            )
            
            // Progress
            drawArc(
                color = if (percentage >= 0.9f) tertiary else secondary,
                startAngle = -90f,
                sweepAngle = 360f * animState.value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryColor
            )
            Text("Cobrado", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HorizontalRankingChart(
    items: List<com.example.optoapp.viewmodel.ProductoRanking>,
    modifier: Modifier = Modifier
) {
    val maxQty = items.maxOfOrNull { it.cantidad } ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, item ->
            val animState = remember { Animatable(0f) }
            LaunchedEffect(item) {
                animState.animateTo(item.cantidad.toFloat() / maxQty, animationSpec = tween(800 + (index * 100)))
            }
            
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.nombre, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("${item.cantidad}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animState.value)
                        .height(8.dp)
                        .graphicsLayer(clip = true, shape = CircleShape)
                        .drawWithContent {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(primaryColor, secondaryColor)
                                )
                            )
                            drawContent()
                        }
                )
            }
        }
    }
}
