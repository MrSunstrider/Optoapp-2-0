package com.example.optoapp.domain

import kotlin.math.abs
import kotlin.math.log10

object DiagnosticoCalculator {
    private const val EPS = 1e-4

    fun parseRefraction(v: String): Double? {
        val clean = v.lowercase().trim()
        if (clean in listOf("plano", "neutro", "pl", "nt")) return 0.0
        return clean.replace(",", ".").toDoubleOrNull()
    }

    fun calcularDiagnostico(esferaStr: String, cilindroStr: String): String {
        val esf = parseRefraction(esferaStr)
        val cil = parseRefraction(cilindroStr)

        if (esf == null && cil == null) return ""

        var e = esf ?: 0.0
        var c = cil ?: 0.0

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

    fun parseSnellenToLogMar(snellen: String): Double? {
        try {
            val clean = snellen.trim()
            val regex = Regex("""(\d+)\s*/\s*(\d+)""")
            val m = regex.find(clean) ?: return null
            val numerator = m.groupValues[1].toDoubleOrNull() ?: return null
            val denominator = m.groupValues[2].toDoubleOrNull() ?: return null
            if (numerator <= 0 || denominator <= 0) return null
            val decimalAV = numerator / denominator
            return -log10(decimalAV)
        } catch (_: Exception) {
            return null
        }
    }

    fun parseAvToLogMar(raw: String): Double? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        parseSnellenToLogMar(clean)?.let { return it }
        if ('.' !in clean && ',' !in clean) return null
        val value = clean.replace(',', '.').toDoubleOrNull() ?: return null
        if (!value.isFinite() || value <= 0.0 || value > 2.0) return null
        return -log10(value)
    }
}
