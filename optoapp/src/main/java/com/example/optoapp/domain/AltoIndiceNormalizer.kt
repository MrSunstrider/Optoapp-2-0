package com.example.optoapp.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.util.Locale

/**
 * Canonical form for `alto_indice` so the item value and the biselado matrix compare equal regardless of
 * how each was typed ("1,5" vs "1.50"). Android items carry no index field of their own; the only signal is
 * the selected high-index treatment, so [fromTratamientos] derives it from there.
 */
object AltoIndiceNormalizer {

    private val NUMERIC = Regex("""\d+(\.\d+)?""")
    private val TRAILING_DECIMAL = Regex("""(\d+[.,]\d+)\s*$""")
    private val DIACRITICS = Regex("""\p{Mn}+""")
    private const val ALTO_INDICE = "alto indice"

    fun normalize(raw: String?): String? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val dotted = trimmed.replace(',', '.')
        return if (NUMERIC.matches(dotted)) {
            BigDecimal(dotted).setScale(2, RoundingMode.HALF_UP).toPlainString()
        } else {
            stripAccents(trimmed).uppercase(Locale.ROOT)
        }
    }

    fun matches(a: String?, b: String?): Boolean = normalize(a) == normalize(b)

    // More than one high-index treatment cannot be resolved to a single price, so it is left undetermined.
    fun fromTratamientos(tratamientos: Collection<String>): String? {
        val altoIndice = tratamientos.filter { stripAccents(it).lowercase(Locale.ROOT).contains(ALTO_INDICE) }
        val unico = altoIndice.singleOrNull() ?: return null
        return TRAILING_DECIMAL.find(unico.trim())?.groupValues?.get(1)?.let(::normalize)
    }

    private fun stripAccents(value: String): String =
        DIACRITICS.replace(Normalizer.normalize(value, Normalizer.Form.NFD), "")
}
