package com.example.optoapp.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.util.WhatsAppUtils

@Composable
fun LaboratorioTicketAlertDialog(
    onDismiss: () -> Unit,
    ticketTextoCompleto: String,
    ticketTextoCompacto: String,
    laboratorioNombre: String,
    laboratorioContacto: String,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val mensajeWhatsappLab = buildString {
        if (laboratorioNombre.isNotBlank()) {
            appendLine("Hola $laboratorioNombre,")
            appendLine()
        }
        append(ticketTextoCompacto)
    }.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ticket de Laboratorio") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = ticketTextoCompleto,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (laboratorioContacto.isNotBlank()) {
                    TextButton(onClick = {
                        WhatsAppUtils.sendWhatsAppMessage(
                            context,
                            laboratorioContacto,
                            mensajeWhatsappLab,
                            "Configura el contacto del laboratorio en Configuración."
                        )
                    }) { Text("WhatsApp laboratorio") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(ticketTextoCompacto))
                    }) { Text("Copiar") }
                    TextButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, ticketTextoCompacto)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir Ticket"))
                    }) { Text("Compartir") }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
