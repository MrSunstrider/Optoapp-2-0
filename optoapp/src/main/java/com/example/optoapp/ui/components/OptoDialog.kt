package com.example.optoapp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.theme.OptoTokens

@Composable
fun OptoDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String = "OK",
    onConfirm: () -> Unit,
    dismissText: String? = null,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = content,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = if (dismissText != null) {
            {
                TextButton(onClick = onDismissRequest) {
                    Text(dismissText)
                }
            }
        } else {
            null
        }
    )
}
