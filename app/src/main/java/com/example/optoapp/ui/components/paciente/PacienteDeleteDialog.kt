package com.example.optoapp.ui.components.paciente

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DeletePacienteDialog(
    deleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text("¿Eliminar paciente?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Esta acción eliminará también evaluaciones, dispensaciones y servicios relacionados. " +
                    "Existe un límite diario de seguridad."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!deleting) onConfirm()
                },
                enabled = !deleting
            ) {
                Text(
                    "Eliminar",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!deleting) onCancel()
                },
                enabled = !deleting
            ) {
                Text("Cancelar")
            }
        }
    )
}
