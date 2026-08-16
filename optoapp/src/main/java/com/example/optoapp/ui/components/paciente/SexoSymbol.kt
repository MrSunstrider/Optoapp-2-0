package com.example.optoapp.ui.components.paciente

/** Glifo de género mostrado en el avatar del paciente. */
enum class SexoSymbol { MARTE, VENUS, DESCONOCIDO }

/**
 * El campo `sexo` es texto libre y se comparte con el companion web, por lo que se
 * toleran variantes ("M", "Varón", "Mujer") además de "Masculino"/"Femenino".
 */
fun sexoSymbolOf(sexo: String?): SexoSymbol {
    val normalized = sexo?.trim()?.lowercase()?.replace('ó', 'o') ?: return SexoSymbol.DESCONOCIDO
    return when {
        normalized.isEmpty() -> SexoSymbol.DESCONOCIDO
        // "mujer" se evalúa antes que el prefijo masculino para que la "m" no lo capture.
        normalized == "f" || normalized.startsWith("femenino") || normalized.startsWith("mujer") -> SexoSymbol.VENUS
        normalized == "m" ||
            normalized.startsWith("masculino") ||
            normalized.startsWith("varon") ||
            normalized.startsWith("hombre") -> SexoSymbol.MARTE
        else -> SexoSymbol.DESCONOCIDO
    }
}
