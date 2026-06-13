package com.example.optoapp.ui.components

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.theme.OptoTokens

@Composable
fun OptoCard(
    modifier: Modifier = Modifier,
    elevation: Dp = OptoTokens.elevation.level1,
    shape: Shape = OptoTokens.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardElevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation)
    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = cardElevation
        ) { content() }
    } else {
        ElevatedCard(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = cardElevation
        ) { content() }
    }
}
