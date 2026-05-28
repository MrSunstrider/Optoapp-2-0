package com.example.optoapp.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import java.io.IOException
import com.example.optoapp.data.OpticaMembership
import com.example.optoapp.viewmodel.auth.AuthDelegate
import com.example.optoapp.viewmodel.auth.BackupDelegate
import com.example.optoapp.viewmodel.auth.PinDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle       : AuthState()
    object Loading    : AuthState()
    object Success    : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel orquestador que delega la lógica de negocio a tres delegates especializados.
 *
 * ADR-2: [AuthDelegate] (login/register/sesión), [PinDelegate] (PIN),
 * [BackupDelegate] (backup/restore). Cada método público mantiene firma idéntica
 * para backward compatibility.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authDelegate: AuthDelegate,
    private val pinDelegate: PinDelegate,
    private val backupDelegate: BackupDelegate
) : ViewModel() {

    companion object { private const val TAG = "AuthViewModel" }

    //── Estado PIN ─────────────────────────────────────────────────────────────

    val pinInput: StateFlow<String> = pinDelegate.pinInput

    fun onPinDigit(digit: String) = pinDelegate.onPinDigit(digit)
    fun clearPin() = pinDelegate.clearPin()
    suspend fun validatePin(): Boolean = pinDelegate.validatePin()

    fun updatePin(oldPin: String, newPin: String) = viewModelScope.launch {
        pinDelegate.updatePin(oldPin, newPin)
    }

    fun createPin(pin: String) = viewModelScope.launch {
        pinDelegate.createPin(pin)
    }

    fun togglePinRequired(enabled: Boolean) = viewModelScope.launch {
        pinDelegate.togglePinRequired(enabled)
    }

    //── Estado Supabase Auth ───────────────────────────────────────────────────

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isLoggedIn    = authDelegate.isLoggedIn
    val opticaId      = authDelegate.opticaId
    val opticaRol     = authDelegate.opticaRol
    val userEmail     = authDelegate.userEmail
    val userName      = authDelegate.userName
    val userTimeZone  = authDelegate.userTimeZone
    val pinHasBeenSet = pinDelegate.pinHasBeenSet
    val isPinRequired = pinDelegate.isPinRequired

    private val _isAuthChecked = MutableStateFlow(false)
    val isAuthChecked = _isAuthChecked.asStateFlow()
    private val _needsOnboarding = MutableStateFlow(false)
    val needsOnboarding = _needsOnboarding.asStateFlow()

    private val _pendingMemberships = MutableStateFlow<List<OpticaMembership>>(emptyList())
    val pendingMemberships: StateFlow<List<OpticaMembership>> = _pendingMemberships.asStateFlow()

    suspend fun isSessionTimeValid(): Boolean = authDelegate.isSessionTimeValid()

    //── Login ──────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        _pendingMemberships.value = emptyList()
        try {
            authDelegate.login(email, password)
            val result = authDelegate.resolvePostLogin(
                emailFallback = email,
                nameFallback = email.substringBefore("@")
            )
            _pendingMemberships.value = result.memberships
            if (result.requiresOnboarding) _needsOnboarding.value = true
            _authState.value = AuthState.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red de login: ${e.message}", e)
            _authState.value = AuthState.Error("Sin conexión a internet")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado de login: ${e.message}", e)
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

    fun loginWithGoogle() = viewModelScope.launch {
        _authState.value = AuthState.Loading
        _pendingMemberships.value = emptyList()
        try {
            authDelegate.loginWithGoogle()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red iniciando login con Google: ${e.message}", e)
            _authState.value = AuthState.Error("No se pudo iniciar Google: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado iniciando login con Google: ${e.message}", e)
            _authState.value = AuthState.Error("No se pudo iniciar Google: ${e.localizedMessage}")
        }
    }

    fun handleAuthDeepLinkIntent(intent: Intent?) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        val error = authDelegate.handleAuthDeepLinkIntent(intent)
        if (error != null) {
            _authState.value = AuthState.Error(error)
            Log.w(TAG, "OAuth completado sin sesión activa.")
            return@launch
        }
        runCatching {
            val r = authDelegate.resolvePostLogin()
            _pendingMemberships.value = r.memberships
            if (r.requiresOnboarding) _needsOnboarding.value = true
            _authState.value = AuthState.Success
        }.onFailure { e ->
            Log.e(TAG, "Error cerrando OAuth Google", e)
            _authState.value = AuthState.Error("No se pudo completar Google: ${e.localizedMessage}")
        }
    }

    fun register(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        _pendingMemberships.value = emptyList()
        val error = authDelegate.register(email, password)
        if (error != null) {
            _authState.value = AuthState.Error(error)
            return@launch
        }
        // Sesión activa, resolver post-login (membresías u onboarding)
        try {
            val r = authDelegate.resolvePostLogin(
                emailFallback = email,
                nameFallback = email.substringBefore("@")
            )
            _pendingMemberships.value = r.memberships
            if (r.requiresOnboarding) _needsOnboarding.value = true
            _authState.value = AuthState.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error post-registro: ${e.message}", e)
            _authState.value = AuthState.Error("Cuenta creada. Inicia sesión para continuar.")
        }
    }

    suspend fun selectOptica(membership: OpticaMembership) {
        _needsOnboarding.value = false
        authDelegate.selectOptica(membership)
        _pendingMemberships.value = emptyList()
        _authState.value = AuthState.Success
    }

    suspend fun prepareOpticaSelection(): Boolean {
        val memberships = authDelegate.prepareOpticaSelection()
        _pendingMemberships.value = memberships
        return memberships.size > 1
    }

    //── Recordar Cuenta ────────────────────────────────────────────────────────

    fun saveRememberedEmail(email: String) = viewModelScope.launch {
        authDelegate.saveRememberedEmail(email)
    }

    suspend fun getRememberedEmail(): String = authDelegate.getRememberedEmail()

    fun clearRememberedEmail() = viewModelScope.launch {
        authDelegate.clearRememberedEmail()
    }

    //── Logout ─────────────────────────────────────────────────────────────────

    suspend fun logout() {
        authDelegate.logout()
        _pendingMemberships.value = emptyList()
        _authState.value = AuthState.Idle
    }

    //── Restaurar sesión al inicio ─────────────────────────────────────────────

    fun checkExistingSession() = viewModelScope.launch {
        try {
            val valid = authDelegate.checkExistingSession()
            if (!valid) {
                logout()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Error en red verificando sesión existente: ${e.localizedMessage}")
            logout()
        } catch (e: Exception) {
            Log.w(TAG, "Error inesperado verificando sesión existente: ${e.localizedMessage}")
            logout()
        } finally {
            _isAuthChecked.value = true
        }
    }

    //── Onboarding ─────────────────────────────────────────────────────────────

    fun completeOnboardingOptica(
        nombreOptica: String, fiscalDocTipo: String, fiscalDocNumero: String,
        razonSocial: String, direccionFiscal: String,
        onFinished: (Boolean, String) -> Unit
    ) = viewModelScope.launch {
        val result = authDelegate.completeOnboardingOptica(
            nombreOptica, fiscalDocTipo, fiscalDocNumero, razonSocial, direccionFiscal
        )
        if (result.isSuccess) {
            _needsOnboarding.value = false
            onFinished(true, "Óptica creada en plan gratuito.")
        } else {
            val raw = result.exceptionOrNull()?.localizedMessage.orEmpty()
            onFinished(false, friendlyOpticaError(raw))
        }
    }

    fun createAdditionalOptica(nombreOptica: String, onFinished: (Boolean, String) -> Unit) = viewModelScope.launch {
        val result = authDelegate.createAdditionalOptica(nombreOptica)
        if (result.isSuccess) {
            val created = result.getOrNull()
            onFinished(true, "Sucursal creada: ${created?.nombre ?: nombreOptica.trim()}. Cierra y vuelve a iniciar sesión para elegirla.")
        } else {
            val raw = result.exceptionOrNull()?.localizedMessage.orEmpty()
            onFinished(false, friendlyOpticaError(raw))
        }
    }

    fun resolveDuplicateHistorias(onFinished: (String) -> Unit) = viewModelScope.launch {
        onFinished(authDelegate.resolveDuplicateHistorias())
    }

    //── Backup ─────────────────────────────────────────────────────────────────

    suspend fun getBackupJson(): String = backupDelegate.getBackupJson()

    fun restoreBackup(json: String, onFinished: (String) -> Unit) = viewModelScope.launch {
        onFinished(backupDelegate.restoreBackup(json))
    }

    //── Helpers ────────────────────────────────────────────────────────────────

    private fun friendlyOpticaError(raw: String): String = when {
        raw.contains("límite de ópticas", ignoreCase = true) ||
            raw.contains("max_opticas", ignoreCase = true) ->
            "Has alcanzado el límite de ópticas de tu plan. Actualiza tu plan para crear más sedes."
        raw.contains("permission", ignoreCase = true) ||
            raw.contains("policy", ignoreCase = true) ||
            raw.contains("RLS", ignoreCase = true) ->
            "No tienes permisos para crear una nueva óptica con esta cuenta."
        raw.contains("Sesión requerida", ignoreCase = true) ||
            raw.contains("sesión requerida", ignoreCase = true) ->
            "Revisa tu correo electrónico y confirma la cuenta. Luego inicia sesión para crear tu óptica."
        raw.isBlank() -> "No se pudo crear la óptica."
        else -> raw
    }
}
