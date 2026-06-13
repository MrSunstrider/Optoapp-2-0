package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.ui.theme.OptoTokens

@Composable
fun OptoQuickAddChip(
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = OptoTokens.shapes.small,
        color = if (isSelected) 
            OptoTokens.colors.primary.copy(alpha = 0.2f) 
        else 
            OptoTokens.colors.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) 
            OptoTokens.colors.primary 
        else 
            OptoTokens.colors.onSurfaceVariant
    ) {
        Text(
            text = value,
            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}