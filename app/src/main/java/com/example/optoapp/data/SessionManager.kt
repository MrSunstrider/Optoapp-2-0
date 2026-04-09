package com.example.optoapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * FASE 4 – Paso 4.3
 * Persiste los datos de sesión SaaS en DataStore.
 * Reutiliza el mismo dataStore que SecurityManager (mismo archivo "settings").
 *
 * Campos guardados:
 *  - isLoggedIn: indica si hay una sesión SaaS activa.
 *  - opticaId: identificador único de la óptica (viene del JWT / metadatos de Supabase).
 *  - userEmail: email del usuario autenticado (solo para display).
 */
class SessionManager(private val context: Context) {

    companion object {
        private val IS_LOGGED_IN  = booleanPreferencesKey("saas_logged_in")
        private val OPTICA_ID     = stringPreferencesKey("saas_optica_id")
        private val USER_EMAIL    = stringPreferencesKey("saas_user_email")
        private val USER_NAME     = stringPreferencesKey("saas_user_name")
        private val LAST_LOGIN_TS = androidx.datastore.preferences.core.longPreferencesKey("saas_last_login_ts")
        private val IS_PIN_REQUIRED = booleanPreferencesKey("pref_is_pin_required")
    }

    // ─── Lectura reactiva ─────────────────────────────────────────────────────

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { it[IS_LOGGED_IN] ?: false }

    val opticaId: Flow<String> = context.dataStore.data
        .map { it[OPTICA_ID] ?: "mi_optica_base" }

    val userEmail: Flow<String> = context.dataStore.data
        .map { it[USER_EMAIL] ?: "" }

    val userName: Flow<String> = context.dataStore.data
        .map { it[USER_NAME] ?: "" }

    val lastLoginTimestamp: Flow<Long> = context.dataStore.data
        .map { it[LAST_LOGIN_TS] ?: 0L }

    val isPinRequired: Flow<Boolean> = context.dataStore.data
        .map { it[IS_PIN_REQUIRED] ?: true }

    // ─── Escritura ────────────────────────────────────────────────────────────

    suspend fun saveSession(opticaId: String, email: String, name: String = "") {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = true
            prefs[OPTICA_ID]    = opticaId
            prefs[USER_EMAIL]   = email
            prefs[USER_NAME]    = name
            prefs[LAST_LOGIN_TS] = System.currentTimeMillis()
        }
    }

    suspend fun setPinRequired(required: Boolean) {
        context.dataStore.edit { it[IS_PIN_REQUIRED] = required }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = false
            prefs[OPTICA_ID]    = "mi_optica_base"
            prefs[USER_EMAIL]   = ""
            prefs[USER_NAME]    = ""
            prefs[LAST_LOGIN_TS] = 0L
        }
    }
}
