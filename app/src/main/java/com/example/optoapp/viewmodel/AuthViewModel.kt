package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.BackupData
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle       : AuthState()
    object Loading    : AuthState()
    object Success    : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val securityManager: SecurityManager,
    private val sessionManager: SessionManager,
    private val repository: OptoRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    companion object { private const val TAG = "AuthViewModel" }

    // ─── Estado PIN (sin cambios) ─────────────────────────────────────────────

    private val userPin = securityManager.userPin
    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput

    fun onPinDigit(digit: String) { if (_pinInput.value.length < 6) _pinInput.value += digit }
    fun clearPin() { _pinInput.value = "" }
    suspend fun validatePin(): Boolean = _pinInput.value == userPin.first()
    fun updatePin(oldPin: String, newPin: String) = viewModelScope.launch {
        if (oldPin == userPin.first()) securityManager.savePin(newPin)
    }

    // ─── Estado Supabase Auth ─────────────────────────────────────────────────

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Flujo reactivo: ¿hay sesión SaaS activa? */
    val isLoggedIn = sessionManager.isLoggedIn

    /** opticaId activo, usado por los Use Cases de sincronización. */
    val opticaId = sessionManager.opticaId

    private val _isAuthChecked = MutableStateFlow(false)
    val isAuthChecked = _isAuthChecked.asStateFlow()

    val userEmail = sessionManager.userEmail
    val userName  = sessionManager.userName

    /** Flujo para la pantalla de configuración: ¿el PIN es obligatorio? */
    val isPinRequired = sessionManager.isPinRequired

    fun togglePinRequired(enabled: Boolean) = viewModelScope.launch {
        sessionManager.setPinRequired(enabled)
    }

    /** 
     * Verifica si la sesión SaaS ha expirado basándose en tiempo (24h).
     * Retorna TRUE si la sesión SIGUE siendo válida.
     */
    suspend fun isSessionTimeValid(): Boolean {
        val lastTs = sessionManager.lastLoginTimestamp.first()
        if (lastTs == 0L) return false
        
        val now = System.currentTimeMillis()
        val diffHours = (now - lastTs) / (1000 * 60 * 60)
        
        return diffHours < 24
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    /**
     * Autentica con email/contraseña en Supabase.
     * El opticaId se extrae de los metadatos del usuario (campo "optica_id")
     * o se usa el <uid> directamente como fallback seguro.
     */
    fun login(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            supabase.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }

            val user    = supabase.auth.currentUserOrNull()
            val uid     = user?.id ?: "unknown"

            // Intentar leer opticaId desde metadatos del usuario en Supabase
            val meta    = user?.userMetadata
            val opticaId = meta?.get("optica_id")?.toString()
                ?.removePrefix("\"")?.removeSuffix("\"")
                ?: uid

            val nombre = meta?.get("nombre")?.toString()
                ?.removePrefix("\"")?.removeSuffix("\"")
                ?: email.substringBefore("@")

            sessionManager.saveSession(
                opticaId = opticaId,
                email    = email,
                name     = nombre
            )

            Log.d(TAG, "Login exitoso. opticaId=$opticaId uid=$uid")
            _authState.value = AuthState.Success

        } catch (e: Exception) {
            Log.e(TAG, "Error de login", e)
            _authState.value = AuthState.Error(
                when {
                    e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                        "Email o contraseña incorrectos"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Sin conexión a internet"
                    else -> "Error: ${e.localizedMessage}"
                }
            )
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    fun logout() = viewModelScope.launch {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error en signOut (ignorado): ${e.localizedMessage}")
        } finally {
            sessionManager.clearSession()
            _authState.value = AuthState.Idle
        }
    }

    // ─── Restaurar sesión al inicio ───────────────────────────────────────────

    /**
     * Verifica si el token guardado en Supabase SDK sigue siendo válido.
     * Si expiró, limpia la sesión local para forzar nuevo login.
     */
    fun checkExistingSession() = viewModelScope.launch {
        try {
            val session = supabase.auth.currentSessionOrNull()
            // Nueva validación: si la sesión no existe en Supabase O el tiempo de 24h expiró
            if (session == null || !isSessionTimeValid()) {
                Log.d(TAG, "Sesión inexistente o expirada por tiempo (24h). Limpiando...")
                logout() // Usa logout para limpiar Supabase y SessionManager
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo verificar sesión existente: ${e.localizedMessage}")
            sessionManager.clearSession()
        } finally {
            _isAuthChecked.value = true
        }
    }

    // ─── Backup local (sin cambios) ───────────────────────────────────────────

    suspend fun getBackupJson(): String = com.google.gson.Gson().toJson(repository.getBackupData())

    fun restoreBackup(json: String) = viewModelScope.launch {
        try {
            val data = com.google.gson.Gson().fromJson(json, BackupData::class.java)
            val currentOpticaId = sessionManager.opticaId.first()
            repository.restoreBackup(data, currentOpticaId)
        } catch (e: Exception) { e.printStackTrace() }
    }
}

