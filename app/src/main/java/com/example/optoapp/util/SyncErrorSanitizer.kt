package com.example.optoapp.util

/**
 * Evita mostrar en la UI mensajes de error de red que incluyen tokens JWT o cabeceras sensibles.
 */
object SyncErrorSanitizer {

    private val bearerRegex = Regex("Bearer\\s+\\S+", RegexOption.IGNORE_CASE)
    private val apiKeyHeaderRegex = Regex("(apikey|authorization)\\s*[:=]\\s*\\S+", RegexOption.IGNORE_CASE)

    fun forUserMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Error de sincronización."
        var s = raw
        s = bearerRegex.replace(s, "Bearer [omitido]")
        s = apiKeyHeaderRegex.replace(s, "[cabecera omitida]")
        if (s.length > 4000) s = s.take(4000) + "…"
        return s
    }
}
