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

private sealed class DialogState {
    data object Ready : DialogState()
    data object Downloading : DialogState()
    data class Error(val message: String) : DialogState()
    data object NeedsPermission : DialogState()
}

@Composable
fun UpdateDialog(updateInfo: UpdateChecker.UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<DialogState>(DialogState.Ready) }

    AlertDialog(
        onDismissRequest = {
            if (state !is DialogState.Downloading) onDismiss()
        },
        icon = {
            Text(
                when (state) {
                    is DialogState.Error -> "⚠️"
                    is DialogState.NeedsPermission -> "🔒"
                    else -> "🔄"
                },
                fontSize = 28.sp,
            )
        },
        title = {
            Text(
                when (state) {
                    is DialogState.Error -> "Error de actualización"
                    is DialogState.NeedsPermission -> "Permiso necesario"
                    is DialogState.Downloading -> "Nueva versión disponible"
                    is DialogState.Ready -> "Nueva versión disponible"
                },
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            when (val s = state) {
                is DialogState.Ready -> Text("Versión ${updateInfo.latestVersion} disponible para descargar.")
                is DialogState.Downloading -> Text("Descargando actualización…")
                is DialogState.Error -> Text(s.message)
                is DialogState.NeedsPermission -> Text(
                    "Para instalar la actualización, esta app necesita permiso para instalar " +
                        "aplicaciones. Tocá \"Abrir Ajustes\" y activá \"Permitir de esta fuente\".",
                )
            }
        },
        confirmButton = {
            when (val s = state) {
                is DialogState.Ready -> TextButton(
                    onClick = {
                        state = DialogState.Downloading
                        scope.launch {
                            val result = UpdateChecker.downloadAndInstall(context, updateInfo.downloadUrl)
                            state = when (result) {
                                is UpdateChecker.DownloadResult.Success -> {
                                    onDismiss()
                                    DialogState.Ready // no se usa, onDismiss ya limpió
                                }
                                is UpdateChecker.DownloadResult.Error -> DialogState.Error(result.message)
                                is UpdateChecker.DownloadResult.NeedsInstallPermission -> DialogState.NeedsPermission
                            }
                        }
                    },
                ) {
                    Text("Descargar e instalar")
                }
                is DialogState.Downloading -> TextButton(onClick = {}, enabled = false) {
                    Text("Descargando…")
                }
                is DialogState.Error -> TextButton(
                    onClick = {
                        UpdateChecker.openDownloadInBrowser(context, updateInfo.downloadUrl)
                    },
                ) {
                    Text("Descargar en navegador")
                }
                is DialogState.NeedsPermission -> TextButton(
                    onClick = {
                        UpdateChecker.openInstallPermissionSettings(context)
                    },
                ) {
                    Text("Abrir Ajustes")
                }
            }
        },
        dismissButton = {
            when (state) {
                is DialogState.Ready -> TextButton(onClick = onDismiss) {
                    Text("Más tarde")
                }
                is DialogState.Downloading -> TextButton(onClick = {}, enabled = false) {
                    Text("Más tarde")
                }
                is DialogState.Error -> TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
                is DialogState.NeedsPermission -> TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        },
    )
}
