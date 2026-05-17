package com.example.optoapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri

@Deprecated(
    "Migrated to FileShareUtils.sendWhatsAppMessage. Use FileShareUtils instead.",
    ReplaceWith("FileShareUtils.sendWhatsAppMessage(context, phoneNumber, message, emptyNumberMessage)")
)
object WhatsAppUtils {

    fun sendWhatsAppMessage(
        context: Context,
        phoneNumber: String,
        message: String,
        emptyNumberMessage: String = "El paciente no tiene un número telefónico registrado."
    ) {
        FileShareUtils.sendWhatsAppMessage(context, phoneNumber, message, emptyNumberMessage)
    }
}
