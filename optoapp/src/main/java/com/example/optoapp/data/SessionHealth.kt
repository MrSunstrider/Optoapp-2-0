package com.example.optoapp.data

/**
 * Estado de salud de la sesión Supabase, visible en el panel de diagnóstico.
 * El ViewModel lo actualiza reactivamente y expone el state para la UI.
 */
data class SessionHealth(
    /** true si hay un JWT válido y no expirado */
    val hasValidSession: Boolean = false,
    /** Timestamp del último refresh exitoso (ms) */
    val lastRefreshAtMs: Long = 0L,
    /** true si el último refreshIntent fue exitoso */
    val lastRefreshSuccessful: Boolean = false,
    /** Mensaje del último error de refresh, si lo hubo */
    val lastRefreshError: String = "",
    /** Cuántos refresh fallaron consecutivamente */
    val consecutiveRefreshFailures: Int = 0,
    /** Timestamp de expiración del token actual (epoch ms, 0 = desconocido) */
    val tokenExpiresAtMs: Long = 0L,
    /** Errores recientes del background sync/auth (últimos 20) */
    val recentBackgroundErrors: List<BackgroundError> = emptyList(),
)

/**
 * Un error ocurrido en background (sync, auth refresh, etc.) que normalmente
 * el usuario no vería. Se almacena en memoria (máximo 50).
 */
data class BackgroundError(
    val source: String,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis(),
)
