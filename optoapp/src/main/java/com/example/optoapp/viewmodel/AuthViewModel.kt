package com.example.optoapp.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OpticaMembership
import com.example.optoapp.data.membership.MembershipFetch
import com.example.optoapp.viewmodel.auth.AuthDelegate
import com.example.optoapp.viewmodel.auth.BackupDelegate
import com.example.optoapp.viewmodel.auth.GoogleAuthAbandon
import com.example.optoapp.viewmodel.auth.PinDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class AuthError(val userMessage: String) {
    data object InvalidCredentials : AuthError("Email o contraseña incorrectos")
    data object Network : AuthError("Sin conexión a internet")
    data class Unknown(val raw: String) : AuthError("Error: $raw")
}

sealed class RecoveryState {
    data object Idle : RecoveryState()
    data object Loading : RecoveryState()
    data object EmailSent : RecoveryState()
    data object LinkReceived : RecoveryState()
    data object PasswordUpdated : RecoveryState()
    data class Error(val message: String) : RecoveryState()
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
    private val backupDelegate: BackupDelegate,
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

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

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isLoggedIn = authDelegate.isLoggedIn
    val opticaId = authDelegate.opticaId
    val opticaRol = authDelegate.opticaRol
    val userEmail = authDelegate.userEmail
    val userName = authDelegate.userName
    val userTimeZone = authDelegate.userTimeZone
    val pinHasBeenSet = pinDelegate.pinHasBeenSet
    val isPinRequired = pinDelegate.isPinRequired

    private val _isAuthChecked = MutableStateFlow(false)
    val isAuthChecked = _isAuthChecked.asStateFlow()
    private val _needsOnboarding = MutableStateFlow(false)
    val needsOnboarding = _needsOnboarding.asStateFlow()

    private val _pendingMemberships = MutableStateFlow<List<OpticaMembership>>(emptyList())
    val pendingMemberships: StateFlow<List<OpticaMembership>> = _pendingMemberships.asStateFlow()

    suspend fun isSessionTimeValid(): Boolean = authDelegate.isSessionTimeValid()

    private val _recoveryState = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()

    fun sendRecoveryEmail(email: String) = viewModelScope.launch {
        _recoveryState.value = RecoveryState.Loading
        try {
            authDelegate.sendRecoveryEmail(email)
            _recoveryState.value = RecoveryState.EmailSent
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando recovery email", e)
            _recoveryState.value = RecoveryState.Error(
                "No se pudo enviar el correo. Intentá de nuevo más tarde.",
            )
        }
    }

    fun handleRecoveryDeepLink(intent: Intent?) = viewModelScope.launch {
        _recoveryState.value = RecoveryState.Loading
        val error = authDelegate.handleRecoveryDeepLink(intent)
        if (error != null) {
            _recoveryState.value = RecoveryState.Error(error)
            return@launch
        }
        _recoveryState.value = RecoveryState.LinkReceived
    }

    fun updatePassword(newPassword: String) = viewModelScope.launch {
        _recoveryState.value = RecoveryState.Loading
        val error = authDelegate.updatePassword(newPassword)
        if (error != null) {
            _recoveryState.value = RecoveryState.Error(error)
            return@launch
        }
        _recoveryState.value = RecoveryState.PasswordUpdated
    }

    fun resetRecoveryState() {
        _recoveryState.value = RecoveryState.Idle
    }

    private fun applyPostLogin(result: AuthDelegate.PostLoginResult) {
        if (result.membershipFetchError) {
            _authState.value = AuthState.Error("No se pudieron cargar las ópticas. Reintente más tarde.")
            return
        }
        _pendingMemberships.value = result.memberships
        if (result.requiresOnboarding) _needsOnboarding.value = true
        _authState.value = AuthState.Success
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        _pendingMemberships.value = emptyList()
        try {
            authDelegate.login(email, password)
            applyPostLogin(authDelegate.resolvePostLogin(
                emailFallback = email,
                nameFallback = email.substringBefore("@"),
            ))
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
                    e is IOException ->
                        "Sin conexión a internet"
                    else -> "Error inesperado. Reintente más tarde."
                },
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
            _authState.value = AuthState.Error("Error inesperado. Reintente más tarde.")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado iniciando login con Google: ${e.message}", e)
            _authState.value = AuthState.Error("Error inesperado. Reintente más tarde.")
        }
    }

    fun onGoogleAuthAbandoned() {
        _authState.value = GoogleAuthAbandon.nextState(_authState.value)
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
            applyPostLogin(authDelegate.resolvePostLogin())
        }.onFailure { e ->
            Log.e(TAG, "Error cerrando OAuth Google", e)
            _authState.value = AuthState.Error("Error inesperado. Reintente más tarde.")
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
        try {
            applyPostLogin(authDelegate.resolvePostLogin(
                emailFallback = email,
                nameFallback = email.substringBefore("@"),
            ))
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
        val fetch = authDelegate.prepareOpticaSelection()
        val memberships = if (fetch is MembershipFetch.Ok) fetch.memberships else emptyList()
        _pendingMemberships.value = memberships
        return memberships.size > 1
    }

    /**
     * Returns the membership count after refresh, or -1 if a network error occurred.
     * Callers must treat -1 as a fetch error (distinct from 0 = not yet added).
     */
    suspend fun refreshMembershipsForWaitScreen(): Int {
        return when (val fetch = authDelegate.prepareOpticaSelection()) {
            is MembershipFetch.Error -> -1
            MembershipFetch.Empty -> {
                _pendingMemberships.value = emptyList()
                0
            }
            is MembershipFetch.Ok -> {
                val memberships = fetch.memberships
                _pendingMemberships.value = memberships
                when {
                    memberships.size == 1 -> {
                        authDelegate.selectOptica(memberships.first())
                        _needsOnboarding.value = false
                        _pendingMemberships.value = emptyList()
                        _authState.value = AuthState.Success
                        1
                    }
                    memberships.size > 1 -> {
                        _needsOnboarding.value = false
                        memberships.size
                    }
                    else -> 0
                }
            }
        }
    }

    fun saveRememberedEmail(email: String) = viewModelScope.launch {
        authDelegate.saveRememberedEmail(email)
    }

    suspend fun getRememberedEmail(): String = authDelegate.getRememberedEmail()

    fun clearRememberedEmail() = viewModelScope.launch {
        authDelegate.clearRememberedEmail()
    }

    suspend fun logout() {
        authDelegate.logout()
        _pendingMemberships.value = emptyList()
        _authState.value = AuthState.Idle
    }

    fun checkExistingSession() = viewModelScope.launch {
        try {
            val valid = authDelegate.checkExistingSession()
            if (!valid) {
                // authDelegate.checkExistingSession already called logout() internally.
                // Only clean up ViewModel-owned state here to avoid a second GLOBAL signOut.
                _pendingMemberships.value = emptyList()
                _authState.value = AuthState.Idle
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

    fun completeOnboardingOptica(
        nombreOptica: String,
        fiscalDocTipo: String,
        fiscalDocNumero: String,
        razonSocial: String,
        direccionFiscal: String,
        onFinished: (Boolean, String) -> Unit,
    ) = viewModelScope.launch {
        val result = authDelegate.completeOnboardingOptica(
            nombreOptica,
            fiscalDocTipo,
            fiscalDocNumero,
            razonSocial,
            direccionFiscal,
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

    suspend fun getBackupJson(): String = backupDelegate.getBackupJson()

    fun restoreBackup(json: String, onFinished: (String) -> Unit) = viewModelScope.launch {
        onFinished(backupDelegate.restoreBackup(json))
    }

    private fun friendlyOpticaError(raw: String): String = when {
        raw.contains("límite de 1", ignoreCase = true) ||
            raw.contains("límite de ópticas", ignoreCase = true) ||
            raw.contains("max_opticas", ignoreCase = true) ||
            raw.contains("límite de 2", ignoreCase = true) ->
            "Has alcanzado el límite de 1 óptica del plan gratuito."
        raw.contains("Sesión requerida", ignoreCase = true) ||
            raw.contains("sesión requerida", ignoreCase = true) ->
            "Revisa tu correo electrónico y confirma la cuenta. Luego inicia sesión para crear tu óptica."
        raw.isBlank() -> "No se pudo crear la óptica."
        else -> raw
    }
}
