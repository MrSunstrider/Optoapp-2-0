package com.example.optoapp.viewmodel

data class DipParseResult(
    val dipTotalMm: Double? = null,
    val dnpOdMm: Double? = null,
    val dnpOiMm: Double? = null
)

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
