package com.example.optoapp.viewmodel.diagnostico

import kotlin.math.log10

/** Pure functions for refraction parsing and clinical diagnosis calculation. */
object DiagnosticoCalculator {

    /**
     * Parses a refraction string value into a [Double].
     *
     * Recognized keywords: "plano", "neutro", "pl", "nt" → 0.0.
     * Commas are treated as decimal separators.
     * Returns null for blank or unparseable input.
     */
    fun parseRefraction(v: String): Double? {
        val clean = v.lowercase().trim()
        if (clean in listOf("plano", "neutro", "pl", "nt")) return 0.0
        return clean.replace(",", ".").toDoubleOrNull()
    }

    /**
     * Calculates a clinical diagnosis label from spherical and cylindrical values.
     *
     * Uses negative cylinder convention (transposes positive cylinder internally).
     * Returns "" when both inputs are null/empty.
     */
    fun calcularDiagnostico(esferaStr: String, cilindroStr: String): String {
        val esf = parseRefraction(esferaStr)
        val cil = parseRefraction(cilindroStr)

        if (esf == null && cil == null) return ""

        var e = esf ?: 0.0
        var c = cil ?: 0.0

        // Transpose to negative cylinder
        if (c > 0) {
            e += c
            c = -c
        }

        val meridian1 = e
        val meridian2 = e + c

        return when {
            meridian1 == 0.0 && meridian2 == 0.0 -> "Emetropía"
            meridian1 < 0.0 && c == 0.0 -> "Miopía"
            meridian1 > 0.0 && c == 0.0 -> "Hipermetropía"
            (meridian1 == 0.0 && meridian2 < 0.0) || (meridian1 < 0.0 && meridian2 == 0.0) -> "Astigmatismo miópico simple"
            (meridian1 == 0.0 && meridian2 > 0.0) || (meridian1 > 0.0 && meridian2 == 0.0) -> "Astigmatismo hipermetrópico simple"
            meridian1 < 0.0 && meridian2 < 0.0 -> "Astigmatismo miópico compuesto"
            meridian1 > 0.0 && meridian2 > 0.0 -> "Astigmatismo hipermetrópico compuesto"
            (meridian1 > 0.0 && meridian2 < 0.0) || (meridian1 < 0.0 && meridian2 > 0.0) -> "Astigmatismo mixto"
            else -> "Astigmatismo mixto"
        }
    }

    /**
     * Converts a Snellen visual acuity string (e.g. "20/20") to LogMAR.
     *
     * Returns null for invalid formats, missing slash, zero denominator, or
     * unparseable input.
     */
    fun parseSnellenToLogMar(snellen: String): Double? {
        try {
            val clean = snellen.trim()
            if (!clean.contains("/")) return null
            val parts = clean.split("/")
            if (parts.size != 2) return null
            val denominator = parts[1].trim().toDoubleOrNull() ?: return null
            if (denominator <= 0) return null
            val decimalAV = 20.0 / denominator
            return -log10(decimalAV)
        } catch (e: Exception) { return null }
    }
}
