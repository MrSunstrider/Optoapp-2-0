@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.optoapp.domain

import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * Resultado del preflight de sesión antes de sync.
 * [cause] es corto para diagnóstico: `red`, `sin sesión`, `sin usuario`, `token vacío`.
 */
sealed class SessionPreflightResult {
    data object Ok : SessionPreflightResult()
    data class Failed(val cause: String) : SessionPreflightResult()
}

/**
 * P0-T4: reduce fallos de sync por JWT cercano a expirar; no sustituye el auto-refresh del plugin Auth.
 *
 * [refreshSessionBeforeSync] retorna `true` si la sesión está OK (recién refrescada o aún válida),
 * `false` si no hay sesión, el refresh falló, o el server devolvió una sesión anónima
 * (refresh token inválido/expirado) — los callers DEBEN abortar la sync si retorna false.
 */
object SyncSessionHelper {
    private const val TAG = "SyncSession"

    /** Minimum seconds of JWT lifetime required before forcing a refresh (REQ-JWT-001). */
    private const val MIN_TOKEN_LIFETIME_SECONDS = 300L

    /**
     * Refresca la sesión Supabase antes de sincronizar y verifica que la sesión
     * post-refresh corresponda a un usuario autenticado (no anónimo).
     *
     * Si el token actual tiene al menos [MIN_TOKEN_LIFETIME_SECONDS] de vida restante,
     * se saltea el refresh para evitar llamadas innecesarias al endpoint de Auth.
     *
     * @return `true` si la sesión es válida para usar, `false` si debe abortarse la sync.
     */
    suspend fun refreshSessionBeforeSync(supabase: SupabaseClient): Boolean =
        evaluateSessionBeforeSync(supabase) is SessionPreflightResult.Ok

    /**
     * Igual que [refreshSessionBeforeSync] pero con causa corta para mensajes de diagnóstico.
     */
    suspend fun evaluateSessionBeforeSync(supabase: SupabaseClient): SessionPreflightResult {
        val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
        if (session == null) {
            AppLogger.w(TAG, "Sin sesión Supabase; la sync debe abortarse")
            return SessionPreflightResult.Failed("sin sesión")
        }

        // REQ-JWT-001: check remaining token lifetime
        val nowEpoch = System.currentTimeMillis() / 1000
        val remaining = session.expiresAt.epochSeconds - nowEpoch

        if (remaining >= MIN_TOKEN_LIFETIME_SECONDS) {
            AppLogger.d(TAG, "Token válido por ${remaining}s más; no se requiere refresh")
            return validateSession(supabase)
        }

        AppLogger.d(TAG, "Token expira en ${remaining}s (< ${MIN_TOKEN_LIFETIME_SECONDS}s); forzando refresh")
        return try {
            supabase.auth.refreshCurrentSession()
            AppLogger.d(TAG, "Sesión refrescada explícitamente antes de sincronizar")

            val refreshedSession = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
            if (refreshedSession?.accessToken.isNullOrBlank()) {
                AppLogger.w(TAG, "Sesión sin accessToken tras refresh; abortando sync")
                return SessionPreflightResult.Failed("token vacío")
            }

            // Server returns anon session instead of throwing when refresh token expired
            val currentUser = runCatching { supabase.auth.currentUserOrNull() }.getOrNull()
            if (currentUser == null) {
                AppLogger.w(TAG, "Refresh devolvió sesión anónima (refresh token inválido/expirado); abortando sync")
                return SessionPreflightResult.Failed("sin usuario")
            }

            SessionPreflightResult.Ok
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppLogger.w(TAG, "Error en red refrescando sesión: ${e.message}")
            SessionPreflightResult.Failed("red")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error inesperado refrescando sesión: ${e.message}")
            SessionPreflightResult.Failed("error")
        }
    }

    /** Validates that the current session is authenticated (non-anonymous) without refreshing. */
    private suspend fun validateSession(supabase: SupabaseClient): SessionPreflightResult {
        val currentUser = runCatching { supabase.auth.currentUserOrNull() }.getOrNull()
        if (currentUser == null) {
            AppLogger.w(TAG, "Sesión anónima detectada; abortando sync")
            return SessionPreflightResult.Failed("sin usuario")
        }
        val currentSession = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
        if (currentSession?.accessToken.isNullOrBlank()) {
            AppLogger.w(TAG, "Sesión sin accessToken; abortando sync")
            return SessionPreflightResult.Failed("token vacío")
        }
        return SessionPreflightResult.Ok
    }

    fun looksLikeAuthError(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        val m = message.lowercase()
        return m.contains("jwt") ||
            m.contains("401") ||
            m.contains("unauthorized") ||
            m.contains("invalid_grant") ||
            m.contains("session") &&
            m.contains("expired")
    }
}
