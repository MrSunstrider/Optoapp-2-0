package com.example.optoapp.domain

import java.text.Normalizer
import java.util.Locale

// The server CHECK only accepts these three values; legacy enum values and free text are mapped, never guessed.
object TipoLcMapper {

    const val COSMETICO = "cosmetico"
    const val GRADUADO = "graduado"
    const val TERAPEUTICO = "terapeutico"

    fun map(raw: String?): String? {
        val texto = normalizar(raw) ?: return null
        return when {
            texto == "lente_contacto_cosmetico" || texto.startsWith("cosmet") -> COSMETICO
            texto == "lente_contacto_medida" || texto.startsWith("medida") || texto.startsWith("graduad") -> GRADUADO
            texto.startsWith("terap") -> TERAPEUTICO
            else -> null
        }
    }

    fun normalizar(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replace(MARCAS_COMBINANTES, "")
            .lowercase(Locale.ROOT)
    }

    private val MARCAS_COMBINANTES = Regex("\\p{Mn}+")
}
