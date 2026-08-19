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
    private val sessionManager: ISessionManager,
) {
    // ── Estado PIN ────────────────────────────────────────────────────────────

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    val pinHasBeenSet: Flow<Boolean> = securityManager.pinHasBeenSet
    val isPinRequired: Flow<Boolean> = sessionManager.isPinRequired

    // ── Brute-force protection ────────────────────────────────────────────────

    private var failedAttempts = 0
    private var cooldownUntil: Long = 0L

    /**
     * Retorna los segundos restantes de cooldown, o 0 si no hay bloqueo activo.
     * M5: El ViewModel puede exponer esto para feedback visual.
     */
    fun remainingCooldownSeconds(): Int {
        val remaining = cooldownUntil - System.currentTimeMillis()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }

    // ── Digit input ───────────────────────────────────────────────────────────

    fun onPinDigit(digit: String) {
        if (_pinInput.value.length < SecurityManager.PIN_LENGTH) {
            _pinInput.value += digit
        }
    }

    fun clearPin() {
        _pinInput.value = ""
    }

    // ── Validación ────────────────────────────────────────────────────────────

    suspend fun validatePin(): Boolean {
        val now = System.currentTimeMillis()
        if (now < cooldownUntil) return false
        if (_pinInput.value.isEmpty()) return false
        if (!securityManager.pinHasBeenSet.first()) return false

        val isValid = _pinInput.value == securityManager.userPin.first()
        if (isValid) {
            failedAttempts = 0
        } else {
            failedAttempts++
            cooldownUntil = when {
                failedAttempts >= 10 -> now + 300_000L // 5 min
                failedAttempts >= 5 -> now + 30_000L // 30 s
                else -> now // no cooldown
            }
        }
        return isValid
    }

    // ── Creación / Actualización ──────────────────────────────────────────────

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
