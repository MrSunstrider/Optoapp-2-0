package com.example.optoapp.domain

object PagoEffect {
    fun signedAmount(tipo: String, monto: Double): Double = when (tipo.trim()) {
        "Abono", "Pago completo" -> monto
        "Reembolso", "Reverso" -> -monto
        else -> 0.0
    }
}
