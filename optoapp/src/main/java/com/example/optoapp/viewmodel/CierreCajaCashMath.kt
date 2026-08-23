package com.example.optoapp.viewmodel

import java.time.LocalDate

/**
 * Counted-cash helpers for Cierre. Persistence is prefs/session only — never arqueo_caja.
 */
object CierreCajaCashMath {
    fun diferencia(contado: Double?, efectivoNet: Double): Double? =
        contado?.let { it - efectivoNet }

    fun prefsKey(opticaId: String, fecha: LocalDate): String =
        "cierre_contado_${opticaId}_$fecha"

    fun parseContado(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        return raw.toDoubleOrNull()
    }

    fun serializeContado(contado: Double?): String? =
        contado?.toString()
}
