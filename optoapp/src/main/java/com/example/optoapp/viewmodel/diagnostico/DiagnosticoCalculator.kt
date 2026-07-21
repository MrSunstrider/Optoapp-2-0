package com.example.optoapp.viewmodel.diagnostico

import kotlin.math.abs
import kotlin.math.log10

/** Pure functions for refraction parsing and clinical diagnosis calculation. */
object DiagnosticoCalculator {

    /** Epsilon for floating-point zero comparison (aligned with web). */
    private const val EPS = 1e-4

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
     * Uses epsilon tolerance against float drift (aligned with web classifyRefraccion).
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

        val m1 = e
        val m2 = e + c

        fun eq0(x: Double) = abs(x) < EPS

        return when {
            eq0(m1) && eq0(m2) -> "Emetropía"
            m1 < 0.0 && eq0(c) -> "Miopía"
            m1 > 0.0 && eq0(c) -> "Hipermetropía"
            (eq0(m1) && m2 < 0.0) || (eq0(m2) && m1 < 0.0) -> "Astigmatismo miópico simple"
            (eq0(m1) && m2 > 0.0) || (eq0(m2) && m1 > 0.0) -> "Astigmatismo hipermetrópico simple"
            m1 < 0.0 && m2 < 0.0 -> "Astigmatismo miópico compuesto"
            m1 > 0.0 && m2 > 0.0 -> "Astigmatismo hipermetrópico compuesto"
            (m1 > 0.0 && m2 < 0.0) || (m1 < 0.0 && m2 > 0.0) -> "Astigmatismo mixto"
            else -> "Astigmatismo mixto"
        }
    }

    /**
     * Converts a Snellen visual acuity string (e.g. "20/20") to LogMAR.
     *
     * Accepts optional spaces around the slash ("20 / 40") — aligned with web.
     * Returns null for invalid formats, missing slash, zero denominator, or
     * unparseable input.
     */
    fun parseSnellenToLogMar(snellen: String): Double? {
        try {
            val clean = snellen.trim()
            val regex = Regex("""(\d+)\s*/\s*(\d+)""")
            val m = regex.find(clean) ?: return null
            val numerator = m.groupValues[1].toDoubleOrNull() ?: return null
            val denominator = m.groupValues[2].toDoubleOrNull() ?: return null
            if (denominator <= 0) return null
            val decimalAV = numerator / denominator
            return -log10(decimalAV)
        } catch (e: Exception) {
            return null
        }
    }
}
