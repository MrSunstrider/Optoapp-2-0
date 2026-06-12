package com.example.optoapp.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.optoapp.util.UpdateChecker
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(updateInfo: UpdateChecker.UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        icon = { Text("🔄", fontSize = 28.sp) },
        title = {
            Text("Nueva versión disponible", fontWeight = FontWeight.Bold)
        },
        text = {
            if (isDownloading) {
                Text("Descargando actualización…")
            } else {
                Text("Versión ${updateInfo.latestVersion} disponible para descargar.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isDownloading) {
                        isDownloading = true
                        scope.launch {
                            UpdateChecker.downloadAndInstall(context, updateInfo.downloadUrl)
                            onDismiss()
                        }
                    }
                },
                enabled = !isDownloading
            ) {
                Text(if (isDownloading) "Descargando…" else "Descargar e instalar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading
            ) {
                Text("Más tarde")
            }
        }
    )
}
