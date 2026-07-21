package com.example.optoapp.domain

import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * P0-T4: reduce fallos de sync por JWT cercano a expirar; no sustituye el auto-refresh del plugin Auth.
 *
 * [refreshSessionBeforeSync] retorna `true` si la sesión está OK (recién refrescada o aún válida),
 * `false` si no hay sesión, el refresh falló, o el server devolvió una sesión anónima
 * (refresh token inválido/expirado) — los callers DEBEN abortar la sync si retorna false.
 */
object SyncSessionHelper {
    private const val TAG = "SyncSession"

    /**
     * Refresca la sesión Supabase antes de sincronizar y verifica que la sesión
     * post-refresh corresponda a un usuario autenticado (no anónimo).
     *
     * @return `true` si la sesión es válida para usar, `false` si debe abortarse la sync.
     */
    suspend fun refreshSessionBeforeSync(supabase: SupabaseClient): Boolean {
        val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
        if (session == null) {
            AppLogger.w(TAG, "Sin sesión Supabase; la sync debe abortarse")
            return false
        }

        return try {
            supabase.auth.refreshCurrentSession()
            AppLogger.d(TAG, "Sesión refrescada explícitamente antes de sincronizar")

            val refreshedSession = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
            if (refreshedSession?.accessToken.isNullOrBlank()) {
                AppLogger.w(TAG, "Sesión sin accessToken tras refresh; abortando sync")
                return false
            }

            // Server returns anon session instead of throwing when refresh token expired
            val currentUser = runCatching { supabase.auth.currentUserOrNull() }.getOrNull()
            if (currentUser == null) {
                AppLogger.w(TAG, "Refresh devolvió sesión anónima (refresh token inválido/expirado); abortando sync")
                return false
            }

            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppLogger.w(TAG, "Error en red refrescando sesión: ${e.message}")
            // No podemos confirmar que el token es válido — abortar para evitar 401s masivos
            false
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error inesperado refrescando sesión: ${e.message}")
            false
        }
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
