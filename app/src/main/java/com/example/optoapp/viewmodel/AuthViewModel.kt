package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.BuildConfig
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaMembership
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.data.SessionManager
import com.example.optoapp.util.BackupImportValidator
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
    private val membershipRepository: MembershipRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    companion object { private const val TAG = "AuthViewModel" }

    // ─── Estado PIN (sin cambios) ─────────────────────────────────────────────

    private val userPin = securityManager.userPin
    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput

    fun onPinDigit(digit: String) {
        if (_pinInput.value.length < SecurityManager.PIN_LENGTH) _pinInput.value += digit
    }

    fun clearPin() { _pinInput.value = "" }

    suspend fun validatePin(): Boolean = _pinInput.value == userPin.first()

    fun updatePin(oldPin: String, newPin: String) = viewModelScope.launch {
        if (oldPin != userPin.first()) return@launch
        if (newPin.length != SecurityManager.PIN_LENGTH) return@launch
        securityManager.savePin(newPin)
    }

    // ─── Estado Supabase Auth ─────────────────────────────────────────────────

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Flujo reactivo: ¿hay sesión SaaS activa? */
    val isLoggedIn = sessionManager.isLoggedIn

    /** opticaId activo, usado por los Use Cases de sincronización. */
    val opticaId = sessionManager.opticaId

    /** Rol en la óptica activa (`usuario_optica`), para ocultar secciones en el drawer. */
    val opticaRol = sessionManager.opticaRol

    private val _isAuthChecked = MutableStateFlow(false)
    val isAuthChecked = _isAuthChecked.asStateFlow()

    val userEmail = sessionManager.userEmail
    val userName  = sessionManager.userName

    /** Flujo para la pantalla de configuración: ¿el PIN es obligatorio? */
    val isPinRequired = sessionManager.isPinRequired

    /** Si hay más de una óptica, el usuario debe elegir en [SeleccionOpticaScreen]. */
    private val _pendingMemberships = MutableStateFlow<List<OpticaMembership>>(emptyList())
    val pendingMemberships: StateFlow<List<OpticaMembership>> = _pendingMemberships.asStateFlow()

    private var pendingLoginEmail: String = ""
    private var pendingLoginName: String = ""

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
     * Óptica activa: tabla `usuario_optica` (multi-óptica). Si no hay filas, fallback a metadata/uid (legado).
     */
    fun login(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        _pendingMemberships.value = emptyList()
        try {
            supabase.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }

            val user = supabase.auth.currentUserOrNull()
            val uid  = user?.id ?: "unknown"
            val meta = user?.userMetadata

            val nombre = meta?.get("nombre")?.toString()
                ?.removePrefix("\"")?.removeSuffix("\"")
                ?: email.substringBefore("@")

            pendingLoginEmail = email
            pendingLoginName = nombre

            val memberships = membershipRepository.fetchMembershipsForCurrentUser()

            when {
                memberships.size > 1 -> {
                    _pendingMemberships.value = memberships
                    Log.d(TAG, "Login: ${memberships.size} ópticas; se requiere selección")
                    _authState.value = AuthState.Success
                }
                memberships.size == 1 -> {
                    val m = memberships.first()
                    sessionManager.saveSession(
                        opticaId = m.opticaId,
                        email = email,
                        name = nombre,
                        rol = m.rol
                    )
                    repository.reassignLegacyMiOpticaBaseTo(m.opticaId)
                    Log.d(TAG, "Login exitoso. opticaId=${m.opticaId} uid=$uid (única membresía)")
                    _authState.value = AuthState.Success
                }
                else -> {
                    val opticaLegacy = meta?.get("optica_id")?.toString()
                        ?.removePrefix("\"")?.removeSuffix("\"")
                        ?: uid
                    sessionManager.saveSession(
                        opticaId = opticaLegacy,
                        email = email,
                        name = nombre,
                        rol = "admin"
                    )
                    repository.reassignLegacyMiOpticaBaseTo(opticaLegacy)
                    Log.d(TAG, "Login exitoso (legado sin usuario_optica). opticaId=$opticaLegacy uid=$uid (verificar fila en usuario_optica si falla RLS)")
                    _authState.value = AuthState.Success
                }
            }

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

    /** Tras elegir óptica en pantalla de selección (solo si hay varias membresías). */
    suspend fun selectOptica(membership: OpticaMembership) {
        sessionManager.saveSession(
            opticaId = membership.opticaId,
            email = pendingLoginEmail,
            name = pendingLoginName,
            rol = membership.rol
        )
        repository.reassignLegacyMiOpticaBaseTo(membership.opticaId)
        _pendingMemberships.value = emptyList()
        if (BuildConfig.DEBUG) {
            val uid = try { supabase.auth.currentUserOrNull()?.id } catch (_: Exception) { null }
            Log.d(TAG, "Óptica seleccionada: ${membership.opticaId} rol=${membership.rol} uid=$uid")
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    /** Cierra sesión en Supabase y borra datos locales; debe llamarse desde una corrutina. */
    suspend fun logout() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error en signOut (ignorado): ${e.localizedMessage}")
        } finally {
            _pendingMemberships.value = emptyList()
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
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Sesión inexistente o expirada por tiempo (24h). Limpiando...")
                }
                logout()
            } else {
                val oid = sessionManager.opticaId.first()
                repository.reassignLegacyMiOpticaBaseTo(oid)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo verificar sesión existente: ${e.localizedMessage}")
            sessionManager.clearSession()
        } finally {
            _isAuthChecked.value = true
        }
    }

    // ─── Backup local (sin cambios) ───────────────────────────────────────────

    suspend fun getBackupJson(): String {
        val oid = sessionManager.opticaId.first()
        return com.google.gson.Gson().toJson(repository.getBackupDataForOptica(oid))
    }

    fun restoreBackup(json: String, onFinished: (String) -> Unit) = viewModelScope.launch {
        val data = BackupImportValidator.parse(json).getOrElse { e ->
            onFinished(e.message ?: "No se pudo validar el respaldo.")
            return@launch
        }
        try {
            val currentOpticaId = sessionManager.opticaId.first()
            repository.restoreBackup(data, currentOpticaId)
            onFinished("Base de datos restaurada correctamente.")
        } catch (e: Exception) {
            onFinished("Error al restaurar: ${e.message ?: "desconocido"}")
        }
    }
}

