package com.example.optoapp.domain

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * P0-T4: reduce fallos de sync por JWT cercano a expirar; no sustituye el auto-refresh del plugin Auth.
 */
object SyncSessionHelper {
    private const val TAG = "SyncSession"

    suspend fun refreshSessionBeforeSync(supabase: SupabaseClient) {
        try {
            if (supabase.auth.currentSessionOrNull() == null) {
                Log.w(TAG, "Sin sesión Supabase; la sync probablemente fallará por RLS")
                return
            }
            supabase.auth.refreshCurrentSession()
            Log.d(TAG, "Sesión refrescada explícitamente antes de sincronizar")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Error en red refrescando sesión (se intenta sync igual): ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Error inesperado refrescando sesión (se intenta sync igual): ${e.message}")
        }
    }

    fun looksLikeAuthError(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        val m = message.lowercase()
        return m.contains("jwt") || m.contains("401") ||
            m.contains("unauthorized") || m.contains("invalid_grant") ||
            m.contains("session") && m.contains("expired")
    }
}
