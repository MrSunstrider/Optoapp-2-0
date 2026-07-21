package com.example.optoapp.data

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.preferences.core.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

interface ISessionManager {
    val isLoggedIn: Flow<Boolean>
    val opticaId: Flow<String>
    val opticaRol: Flow<String>
    val userEmail: Flow<String>
    val userName: Flow<String>
    val userTimeZone: Flow<String?>
    val lastLoginTimestamp: Flow<Long>
    val pinHasBeenSet: Flow<Boolean>
    val isPinRequired: Flow<Boolean>
    suspend fun saveSession(opticaId: String, email: String, name: String = "", rol: String = "admin")
    suspend fun clearSession()
    suspend fun setPinRequired(required: Boolean)
    suspend fun saveRememberedEmail(email: String)
    suspend fun getRememberedEmail(): String
    suspend fun clearRememberedEmail()
}

/**
 * FASE 4 – Paso 4.3
 * Persiste los datos de sesión SaaS en DataStore.
 * Reutiliza el mismo dataStore que SecurityManager (mismo archivo "settings").
 */
class SessionManager(private val context: Context) : ISessionManager {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _opticaIdFlow = MutableStateFlow(getSecureOpticaId())
    private val _opticaRolFlow = MutableStateFlow(getSecureOpticaRol())

    companion object {
        const val LEGACY_OPTICA_ID = "mi_optica_base"

        private val IS_LOGGED_IN = booleanPreferencesKey("saas_logged_in")
        private val USER_NAME = stringPreferencesKey("saas_user_name")
        private val LAST_LOGIN_TS = longPreferencesKey("saas_last_login_ts")
        private val IS_PIN_REQUIRED = booleanPreferencesKey("pref_is_pin_required")
        private val USER_TIMEZONE = stringPreferencesKey("pref_user_timezone")
        private val PIN_HAS_BEEN_SET = booleanPreferencesKey("pref_pin_has_been_set")
    }

    // ─── Lectura reactiva ─────────────────────────────────────────────────────

    override val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { prefs: Preferences -> prefs[IS_LOGGED_IN] ?: false }

    override val opticaId: Flow<String> = _opticaIdFlow

    /** Rol en la óptica activa (tabla usuario_optica). */
    override val opticaRol: Flow<String> = _opticaRolFlow

    private fun getSecureOpticaId(): String = encryptedPrefs.getString("saas_optica_id", LEGACY_OPTICA_ID) ?: LEGACY_OPTICA_ID

    private fun getSecureOpticaRol(): String = encryptedPrefs.getString("saas_optica_rol", "admin") ?: "admin"

    override val userEmail: Flow<String> = context.dataStore.data
        .map { _: Preferences -> encryptedPrefs.getString("saas_user_email", "") ?: "" }

    override val userName: Flow<String> = context.dataStore.data
        .map { prefs: Preferences -> prefs[USER_NAME] ?: "" }

    override val lastLoginTimestamp: Flow<Long> = context.dataStore.data
        .map { prefs: Preferences -> prefs[LAST_LOGIN_TS] ?: 0L }

    override val pinHasBeenSet: Flow<Boolean> = context.dataStore.data
        .map { prefs: Preferences -> prefs[PIN_HAS_BEEN_SET] ?: false }

    override val isPinRequired: Flow<Boolean> = context.dataStore.data
        .map { prefs: Preferences -> prefs[IS_PIN_REQUIRED] ?: false }

    override val userTimeZone: Flow<String?> = context.dataStore.data
        .map { prefs: Preferences -> prefs[USER_TIMEZONE] }

    // ─── Escritura ────────────────────────────────────────────────────────────

    suspend fun setUserTimeZone(timeZoneId: String?) {
        context.dataStore.edit { prefs ->
            if (timeZoneId == null) {
                prefs.remove(USER_TIMEZONE)
            } else {
                prefs[USER_TIMEZONE] = timeZoneId
            }
        }
    }

    override suspend fun saveSession(opticaId: String, email: String, name: String, rol: String) {
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
            prefs[USER_NAME] = name
            prefs[LAST_LOGIN_TS] = System.currentTimeMillis()
        }
    }

    suspend fun setPinHasBeenSet(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[PIN_HAS_BEEN_SET] = value }
    }

    override suspend fun setPinRequired(required: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_PIN_REQUIRED] = required }
    }

    // ── Recordar Cuenta ────────────────────────────────────────────────────

    /**
     * Guarda el email para pre-cargarlo en el login si el usuario marcó
     * "Recordar Cuenta".
     */
    override suspend fun saveRememberedEmail(email: String) {
        encryptedPrefs.edit().putString("pref_remembered_email", email).apply()
    }

    /** Recupera el email guardado por "Recordar Cuenta". */
    override suspend fun getRememberedEmail(): String = encryptedPrefs.getString("pref_remembered_email", "") ?: ""

    /** Limpia el email recordado (cuando el usuario desmarca el checkbox). */
    override suspend fun clearRememberedEmail() {
        encryptedPrefs.edit().remove("pref_remembered_email").apply()
    }

    override suspend fun clearSession() {
        encryptedPrefs.edit { clear() }
        _opticaIdFlow.value = LEGACY_OPTICA_ID
        _opticaRolFlow.value = "admin"

        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = false
            prefs[USER_NAME] = ""
            prefs[LAST_LOGIN_TS] = 0L
            prefs[IS_PIN_REQUIRED] = false
            prefs.remove(USER_TIMEZONE)
        }
    }

    fun getPacienteDeleteCountToday(opticaId: String): Int {
        val key = dailyPacienteDeleteKey(opticaId)
        return encryptedPrefs.getInt(key, 0)
    }

    fun incrementPacienteDeleteCountToday(opticaId: String): Int {
        val key = dailyPacienteDeleteKey(opticaId)
        val current = encryptedPrefs.getInt(key, 0)
        val next = current + 1
        encryptedPrefs.edit().putInt(key, next).apply()
        return next
    }

    private fun dailyPacienteDeleteKey(opticaId: String): String {
        val day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return "paciente_delete_count_${opticaId}_$day"
    }
}
