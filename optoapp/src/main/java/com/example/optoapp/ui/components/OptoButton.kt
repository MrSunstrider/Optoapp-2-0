package com.example.optoapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.theme.OptoTokens

/**
 * OptoButtonVariant enum for button appearance.
 */
enum class OptoButtonVariant {
    Filled, Outlined, Text
}

/**
 * OptoButton - Wrapper around Button (Filled), OutlinedButton (Outlined), TextButton (Text).
 * 
 * Support loading parameter showing CircularProgressIndicator inside button.
 * Support icon @Composable (() -> Unit)? shown before text.
 * Support fullWidth filling available width.
 */
@Composable
fun OptoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: OptoButtonVariant = OptoButtonVariant.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: @Composable (() -> Unit)? = null,
    fullWidth: Boolean = false
) {
    val buttonModifier = if (fullWidth) modifier.fillMaxWidth() else modifier
    
    when (variant) {
        OptoButtonVariant.Filled -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = OptoTokens.elevation.level1)
            ) {
                ButtonContent(text, loading, icon)
            }
        }
        OptoButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                ButtonContent(text, loading, icon)
            }
        }
        OptoButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                ButtonContent(text, loading, icon)
            }
        }
    }
}

@Composable
fun ButtonContent(
    text: String,
    loading: Boolean,
    icon: @Composable (() -> Unit)?
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.let { IconComposable ->
                IconComposable()
            }
            Text(text = text)
        }
    }
}