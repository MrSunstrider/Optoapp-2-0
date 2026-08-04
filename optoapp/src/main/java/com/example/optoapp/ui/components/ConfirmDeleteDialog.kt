package com.example.optoapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ConfirmDeleteDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    deleting: Boolean = false,
    title: String = "Eliminar",
) {
    OptoDialog(
        title = title,
        confirmText = if (deleting) "Eliminando..." else "Eliminar",
        dismissText = "Cancelar",
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
    ) {
        Text(
            text = "¿Estás seguro de que deseas eliminar \"$itemName\"? Esta acción no se puede deshacer.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
        )
    }
}
