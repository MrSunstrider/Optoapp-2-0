package com.example.optoapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * FASE 4 – Paso 4.3
 * Persiste los datos de sesión SaaS en DataStore.
 * Reutiliza el mismo dataStore que SecurityManager (mismo archivo "settings").
 */
class SessionManager(private val context: Context) {
    
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "secure_session_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _opticaIdFlow = MutableStateFlow(getSecureOpticaId())
    private val _opticaRolFlow = MutableStateFlow(getSecureOpticaRol())

    companion object {
        /** Valor por defecto histórico en Room; las filas con este tenant deben reasignarse a la óptica de sesión cuando el usuario inicia sesión con SaaS. */
        const val LEGACY_OPTICA_ID = "mi_optica_base"

        private val IS_LOGGED_IN  = booleanPreferencesKey("saas_logged_in")
        private val USER_NAME     = stringPreferencesKey("saas_user_name")
        private val LAST_LOGIN_TS = longPreferencesKey("saas_last_login_ts")
        private val IS_PIN_REQUIRED = booleanPreferencesKey("pref_is_pin_required")
    }

    // ─── Lectura reactiva ─────────────────────────────────────────────────────

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { prefs: Preferences -> prefs[IS_LOGGED_IN] ?: false }

    val opticaId: Flow<String> = _opticaIdFlow

    /** Rol en la óptica activa (tabla usuario_optica). */
    val opticaRol: Flow<String> = _opticaRolFlow

    private fun getSecureOpticaId(): String {
        return encryptedPrefs.getString("saas_optica_id", LEGACY_OPTICA_ID) ?: LEGACY_OPTICA_ID
    }

    private fun getSecureOpticaRol(): String {
        return encryptedPrefs.getString("saas_optica_rol", "admin") ?: "admin"
    }

    val userEmail: Flow<String> = context.dataStore.data
        .map { _: Preferences -> encryptedPrefs.getString("saas_user_email", "") ?: "" }

    val userName: Flow<String> = context.dataStore.data
        .map { prefs: Preferences -> prefs[USER_NAME] ?: "" }

    val lastLoginTimestamp: Flow<Long> = context.dataStore.data
        .map { prefs: Preferences -> prefs[LAST_LOGIN_TS] ?: 0L }

    val isPinRequired: Flow<Boolean> = context.dataStore.data
        .map { prefs: Preferences -> prefs[IS_PIN_REQUIRED] ?: true }

    // ─── Escritura ────────────────────────────────────────────────────────────

    suspend fun saveSession(opticaId: String, email: String, name: String = "", rol: String = "admin") {
        encryptedPrefs.edit().apply {
            putString("saas_optica_id", opticaId)
            putString("saas_user_email", email)
            putString("saas_optica_rol", rol)
            apply()
        }
        _opticaIdFlow.value = opticaId
        _opticaRolFlow.value = rol

        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = true
            prefs[USER_NAME]    = name
            prefs[LAST_LOGIN_TS] = System.currentTimeMillis()
        }
    }

    suspend fun setPinRequired(required: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_PIN_REQUIRED] = required }
    }

    suspend fun clearSession() {
        encryptedPrefs.edit().clear().apply()
        _opticaIdFlow.value = LEGACY_OPTICA_ID
        _opticaRolFlow.value = "admin"
        
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = false
            prefs[USER_NAME]    = ""
            prefs[LAST_LOGIN_TS] = 0L
        }
    }
}
