package com.example.optoapp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.optoapp.ui.theme.OptoAppTheme

@Composable
fun OptoDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String = "OK",
    onConfirm: () -> Unit,
    dismissText: String? = null,
    content: @Composable () -> Unit,
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
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun OptoDialogPreview() {
    OptoAppTheme {
        OptoDialog(
            onDismissRequest = {},
            title = "Título",
            confirmText = "OK",
            onConfirm = {},
            dismissText = "Cancelar",
        ) { Text("Contenido del diálogo") }
    }
}
