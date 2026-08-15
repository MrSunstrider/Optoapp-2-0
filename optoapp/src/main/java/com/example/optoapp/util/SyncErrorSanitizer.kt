package com.example.optoapp.util

/**
 * Evita mostrar en la UI mensajes de error de red que incluyen tokens JWT o cabeceras sensibles.
 */
object SyncErrorSanitizer {

    private const val REDACTED = "[omitido]"

    private val authHeaderRegex = Regex("(Authorization|apikey)\\s*[:=]\\s*Bearer\\s+\\S+", RegexOption.IGNORE_CASE)
    private val bearerRegex = Regex("Bearer\\s+\\S+", RegexOption.IGNORE_CASE)
    private val apiKeyHeaderRegex = Regex("(apikey)\\s*[:=]\\s*\\S+", RegexOption.IGNORE_CASE)
    private val apiKeyValueRegex = Regex(
        "(apikey|api_key)\"?\\s*[:=]\\s*\"?[A-Za-z0-9._\\-]+",
        RegexOption.IGNORE_CASE,
    )
    private val secretFieldRegex = Regex(
        "\"(access_token|refresh_token|provider_token|password|apikey|api_key)\"\\s*:\\s*\"[^\"]*\"",
        RegexOption.IGNORE_CASE,
    )
    private val jwtRegex = Regex("eyJ[A-Za-z0-9_\\-]{4,}\\.[A-Za-z0-9_\\-]+\\.?[A-Za-z0-9_\\-]*")

    fun forUserMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Error de sincronización."
        val rawLower = raw.lowercase()
        if (
            rawLower.contains("connect timeout") ||
            rawLower.contains("timed out") ||
            rawLower.contains("unable to resolve host") ||
            rawLower.contains("network is unreachable")
        ) {
            return "Sincronización interrumpida por conexión inestable. Verifica internet y vuelve a intentar."
        }
        var s = raw
        s = authHeaderRegex.replace(s, "Authorization: Bearer [omitido]")
        s = bearerRegex.replace(s, "Bearer [omitido]")
        s = apiKeyHeaderRegex.replace(s, "[cabecera omitida]")
        if (s.length > 4000) s = s.take(4000) + "…"
        return s
    }

    /**
     * Versión para diagnóstico/soporte: sólo redacta secretos. A diferencia de [forUserMessage]
     * conserva el detalle técnico (status HTTP, SQLSTATE, nombre de constraint, IDs y conteos)
     * porque es lo único que permite clasificar una falla desde un copy-all.
     */
    fun forDiagnostics(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = raw
        s = secretFieldRegex.replace(s) { m -> "\"${m.groupValues[1]}\":\"$REDACTED\"" }
        s = authHeaderRegex.replace(s, "Authorization: Bearer $REDACTED")
        s = bearerRegex.replace(s, "Bearer $REDACTED")
        s = apiKeyValueRegex.replace(s) { m -> "${m.groupValues[1]}=$REDACTED" }
        s = jwtRegex.replace(s, REDACTED)
        if (s.length > 8000) s = s.take(8000) + "…"
        return s
    }
}
