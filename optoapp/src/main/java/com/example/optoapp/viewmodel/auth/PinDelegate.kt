package com.example.optoapp.viewmodel.auth

import com.example.optoapp.data.ISecurityManager
import com.example.optoapp.data.ISessionManager
import com.example.optoapp.data.SecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Encapsula toda la lógica de PIN: entrada, validación, creación y actualización.
 *
 * ADR-2: Delegado separado inyectado → SRP, testeable con fakes.
 * Reutiliza [SecurityManager] para validación y almacenamiento seguro.
 */
class PinDelegate @Inject constructor(
    private val securityManager: ISecurityManager,
    private val sessionManager: ISessionManager
) {
    //── Estado PIN ────────────────────────────────────────────────────────────

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    val pinHasBeenSet: Flow<Boolean> = sessionManager.pinHasBeenSet
    val isPinRequired: Flow<Boolean> = sessionManager.isPinRequired

    fun onPinDigit(digit: String) {
        if (_pinInput.value.length < SecurityManager.PIN_LENGTH) {
            _pinInput.value += digit
        }
    }

    fun clearPin() {
        _pinInput.value = ""
    }

    //── Validación ────────────────────────────────────────────────────────────

    suspend fun validatePin(): Boolean {
        return _pinInput.value == securityManager.userPin.first()
    }

    //── Creación / Actualización ──────────────────────────────────────────────

    suspend fun updatePin(oldPin: String, newPin: String) {
        if (oldPin != securityManager.userPin.first()) return
        if (!SecurityManager.isValidPin(newPin)) return
        securityManager.savePin(newPin)
    }

    suspend fun createPin(pin: String) {
        if (!SecurityManager.isValidPin(pin)) return
        securityManager.savePin(pin)
    }

    suspend fun togglePinRequired(enabled: Boolean) {
        sessionManager.setPinRequired(enabled)
    }
}
