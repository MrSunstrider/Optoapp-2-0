package com.example.optoapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri

object WhatsAppUtils {

    fun sendWhatsAppMessage(
        context: Context,
        phoneNumber: String,
        message: String,
        emptyNumberMessage: String = "El paciente no tiene un número telefónico registrado."
    ) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, emptyNumberMessage, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            var cleanPhone = phoneNumber.replace(" ", "").replace("+", "").replace("-", "")
            
            // Autocompletado del código de país para Perú
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
}
