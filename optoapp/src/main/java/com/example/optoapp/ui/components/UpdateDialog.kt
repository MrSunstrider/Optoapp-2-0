package com.example.optoapp.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.optoapp.util.UpdateChecker

@Composable
fun UpdateDialog(updateInfo: UpdateChecker.UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("🔄", fontSize = 28.sp) },
        title = {
            Text("Nueva versión disponible", fontWeight = FontWeight.Bold)
        },
        text = {
            Text("Versión ${updateInfo.latestVersion} disponible para descargar.")
        },
        confirmButton = {
            TextButton(onClick = {
                UpdateChecker.downloadAndInstall(context, updateInfo.downloadUrl)
                onDismiss()
            }) {
                Text("Descargar e instalar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Más tarde")
            }
        }
    )
}
