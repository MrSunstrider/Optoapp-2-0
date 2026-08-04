package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.optoapp.ui.theme.OptoAppTheme

@Composable
fun OptoCard(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 1.dp,
    elevation: Dp = 0.dp,
    shape: Shape = MaterialTheme.shapes.large,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(tonalElevation),
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardElevation = CardDefaults.cardElevation(defaultElevation = elevation)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = cardElevation,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = cardElevation,
            content = content,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OptoCardPreview() {
    OptoAppTheme {
        OptoCard { androidx.compose.material3.Text("Card content") }
    }
}
