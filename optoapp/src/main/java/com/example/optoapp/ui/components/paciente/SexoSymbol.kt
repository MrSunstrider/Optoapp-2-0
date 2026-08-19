package com.example.optoapp.ui.components.paciente

import androidx.compose.ui.graphics.Color
import com.example.optoapp.ui.theme.OptoTokens

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

/** Null = caller should use theme primary (sexo no registrado). */
fun sexoAvatarColor(symbol: SexoSymbol, darkTheme: Boolean): Color? = when (symbol) {
    SexoSymbol.MARTE -> if (darkTheme) OptoTokens.semantic.maleBlueDark else OptoTokens.semantic.maleBlueLight
    SexoSymbol.VENUS -> if (darkTheme) OptoTokens.semantic.femaleRoseDark else OptoTokens.semantic.femaleRoseLight
    SexoSymbol.DESCONOCIDO -> null
}
