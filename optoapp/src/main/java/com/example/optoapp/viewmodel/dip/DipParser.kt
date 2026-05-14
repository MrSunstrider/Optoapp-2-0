package com.example.optoapp.viewmodel.dip

/**
 * Result of parsing a DIP (Distancia Interpupilar) or DNP (Distancia Naso-Pupilar) string.
 *
 * @property dipTotalMm Total DIP in mm (single value, or OD+OI sum)
 * @property dnpOdMm DNP of right eye in mm (when parsed as "OD/OI" pair)
 * @property dnpOiMm DNP of left eye in mm (when parsed as "OD/OI" pair)
 */
data class DipParseResult(
    val dipTotalMm: Double? = null,
    val dnpOdMm: Double? = null,
    val dnpOiMm: Double? = null
)

/** Pure parser/formatter for DIP (Distancia Interpupilar) and DNP values. */
object DipParser {

    /**
     * Parses a raw DIP/DNP string input into structured components.
     *
     * Accepted formats:
     * - Single number: "62", "61.5", "61,5" → [dipTotalMm]
     * - OD/OI pair: "30/31", "29,5/30,5" → [dnpOdMm], [dnpOiMm], [dipTotalMm] (sum)
     *
     * Commas are treated as decimal separators. Spaces are trimmed.
     * Invalid/unparseable inputs return [DipParseResult] with all fields null.
     */
    fun parseDipOrDnp(input: String): DipParseResult {
        val normalized = input.trim().replace(" ", "")
        if (normalized.isBlank()) return DipParseResult()

        if (normalized.contains("/")) {
            val parts = normalized.split("/")
            if (parts.size == 2) {
                val od = parts[0].replace(",", ".").toDoubleOrNull()
                val oi = parts[1].replace(",", ".").toDoubleOrNull()
                if (od != null && oi != null) {
                    return DipParseResult(
                        dipTotalMm = od + oi,
                        dnpOdMm = od,
                        dnpOiMm = oi
                    )
                }
            }
            return DipParseResult()
        }

        val dipTotal = normalized.replace(",", ".").toDoubleOrNull()
        return DipParseResult(dipTotalMm = dipTotal)
    }

    /**
     * Formats DIP/DNP values for UI display.
     *
     * Priority:
     * 1. If [dipLejosRaw] is not blank, return it as-is
     * 2. If both [dnpOdMm] and [dnpOiMm] are present, return "OD/OI"
     * 3. If [dipTotalMm] is present, return it as a pretty string
     * 4. Otherwise return ""
     */
    fun formatDipForUi(
        dipLejosRaw: String,
        dipTotalMm: Double?,
        dnpOdMm: Double?,
        dnpOiMm: Double?
    ): String {
        fun pretty(value: Double): String {
            val asLong = value.toLong()
            return if (value == asLong.toDouble()) asLong.toString() else value.toString()
        }
        if (dipLejosRaw.isNotBlank()) return dipLejosRaw
        if (dnpOdMm != null && dnpOiMm != null) return "${pretty(dnpOdMm)}/${pretty(dnpOiMm)}"
        if (dipTotalMm != null) return pretty(dipTotalMm)
        return ""
    }
}
