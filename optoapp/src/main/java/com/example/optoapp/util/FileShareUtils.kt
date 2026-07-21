package com.example.optoapp.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

object FileShareUtils {
    fun getUri(context: Context, file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun openPdf(context: Context, file: File, chooserTitle: String = "Abrir PDF") {
        val uri = getUri(context, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No hay app para abrir PDF.", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePdf(context: Context, file: File, chooserTitle: String = "Compartir PDF") {
        shareFile(context, file, "application/pdf", chooserTitle)
    }

    fun sendWhatsAppMessage(
        context: Context,
        phoneNumber: String,
        message: String,
        emptyNumberMessage: String = "El paciente no tiene un número telefónico registrado.",
    ) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, emptyNumberMessage, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            var cleanPhone = phoneNumber.replace(" ", "").replace("+", "").replace("-", "")
            if (cleanPhone.length == 9 && cleanPhone.startsWith("9")) {
                cleanPhone = "51$cleanPhone"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}".toUri()
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir WhatsApp. Verifica que esté instalado.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String = "Compartir archivo",
    ) {
        val uri = getUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No hay apps disponibles para compartir.", Toast.LENGTH_LONG).show()
        }
    }
}
