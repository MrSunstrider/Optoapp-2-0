package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.SessionHealth
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SyncTelemetryRemoteRow
import com.example.optoapp.util.BackgroundErrorCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface SessionRepairState {
    data object Idle : SessionRepairState
    data object Working : SessionRepairState
    data class Success(val message: String) : SessionRepairState
    data class Error(val message: String) : SessionRepairState
}

/** Lectura de filas con error de sync por óptica (diagnóstico en Configuración). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SyncDiagnosticsViewModel @Inject constructor(
    private val syncEntityStateDao: SyncEntityStateDao,
    private val sessionManager: SessionManager,
    private val supabase: SupabaseClient,
    private val bgErrorCollector: BackgroundErrorCollector,
    private val membershipRepository: MembershipRepository,
) : ViewModel() {
    companion object {
        private const val TAG = "SyncDiagnosticsVM"
        private const val REMOTE_TELEMETRY_RETRY_ATTEMPTS = 3

        internal fun isValidUuid(value: String): Boolean {
            if (value.isBlank()) return false
            return try {
                UUID.fromString(value)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }
    }

    val errorRows: StateFlow<List<SyncEntityState>> = sessionManager.opticaId
        .flatMapLatest { oid -> syncEntityStateDao.observeErrorsForOptica(oid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _remoteTelemetry = MutableStateFlow<SyncTelemetryRemoteRow?>(null)
    val remoteTelemetry: StateFlow<SyncTelemetryRemoteRow?> = _remoteTelemetry.asStateFlow()

    private val _remoteTelemetryLoading = MutableStateFlow(false)
    val remoteTelemetryLoading: StateFlow<Boolean> = _remoteTelemetryLoading.asStateFlow()

    private val _remoteTelemetryError = MutableStateFlow<String?>(null)
    val remoteTelemetryError: StateFlow<String?> = _remoteTelemetryError.asStateFlow()

    // ── Sesión salud ─────────────────────────────────────────────────────────

    private val _sessionHealth = MutableStateFlow(SessionHealth())
    val sessionHealth: StateFlow<SessionHealth> = _sessionHealth.asStateFlow()

    val backgroundErrors = bgErrorCollector.errors

    private val _sessionRepairState = MutableStateFlow<SessionRepairState>(SessionRepairState.Idle)
    val sessionRepairState: StateFlow<SessionRepairState> = _sessionRepairState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.opticaId.collectLatest { oid ->
                fetchRemoteTelemetry(oid)
            }
        }
        viewModelScope.launch {
            refreshSessionHealth()
        }
    }

    /**
     * Actualiza el estado de salud de la sesión. Se llama al iniciar y
     * puede llamarse manualmente desde la UI.
     */
    fun refreshSessionHealth() {
        viewModelScope.launch {
            val session = runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
            val user = runCatching { supabase.auth.currentUserOrNull() }.getOrNull()
            val hasToken = session != null && !session.accessToken.isNullOrBlank()
            _sessionHealth.value = SessionHealth(
                hasValidSession = user != null && hasToken,
                tokenExpiresAtMs = 0L, // supabase-kt no expone expires_at directamente
                lastRefreshSuccessful = hasToken,
                recentBackgroundErrors = bgErrorCollector.errors.value,
            )
        }
    }

    /** Limpia los errores de background. */
    fun clearBackgroundErrors() {
        bgErrorCollector.clear()
        _sessionHealth.value = _sessionHealth.value.copy(recentBackgroundErrors = emptyList())
    }

    fun refreshRemoteTelemetry() = viewModelScope.launch {
        fetchRemoteTelemetry(sessionManager.opticaId.first())
    }

    private suspend fun fetchRemoteTelemetry(opticaId: String) {
        if (opticaId.isBlank() || opticaId == SessionManager.LEGACY_OPTICA_ID) {
            _remoteTelemetry.value = null
            _remoteTelemetryError.value = null
            return
        }
        _remoteTelemetryLoading.value = true
        _remoteTelemetryError.value = null
        runCatching {
            fetchRemoteTelemetryWithRetry(opticaId)
        }.onSuccess { row ->
            _remoteTelemetry.value = row
        }.onFailure { e ->
            Log.e(TAG, "Error fetching remote telemetry", e)
            _remoteTelemetryError.value = "Error inesperado. Reintente más tarde."
        }
        _remoteTelemetryLoading.value = false
    }

    private suspend fun fetchRemoteTelemetryWithRetry(opticaId: String): SyncTelemetryRemoteRow? {
        var lastError: Throwable? = null
        repeat(REMOTE_TELEMETRY_RETRY_ATTEMPTS) { attempt ->
            runCatching {
                supabase.postgrest["sync_telemetry_optica"]
                    .select {
                        filter { eq("optica_id", opticaId) }
                    }
                    .decodeList<SyncTelemetryRemoteRow>()
                    .firstOrNull()
            }.onSuccess { return it }
                .onFailure { e ->
                    lastError = e
                    if (!isTransientNetworkError(e) || attempt == REMOTE_TELEMETRY_RETRY_ATTEMPTS - 1) {
                        throw e
                    }
                    delay(300L * (attempt + 1))
                }
        }
        throw (lastError ?: IllegalStateException("No se pudo consultar telemetría remota."))
    }

    internal fun isTransientNetworkError(error: Throwable): Boolean {
        val m = (error.localizedMessage ?: error.message).orEmpty().lowercase()
        return m.contains("timeout") ||
            m.contains("timed out") ||
            m.contains("unable to resolve host") ||
            m.contains("network is unreachable") ||
            m.contains("connection reset")
    }

    /** Elimina solo el historial de errores mostrado en la app (no borra datos clínicos). */
    fun clearErrorHistory() = viewModelScope.launch {
        val oid = sessionManager.opticaId.first()
        syncEntityStateDao.deleteErrorsForOptica(oid)
    }

    /**
     * Re-fetches the membership from Supabase and fixes the session's opticaId
     * if the current value is not a valid UUID.
     */
    fun repairSessionOpticaId() = viewModelScope.launch {
        _sessionRepairState.value = SessionRepairState.Working
        try {
            val currentOid = sessionManager.opticaId.first()
            if (isValidUuid(currentOid)) {
                _sessionRepairState.value = SessionRepairState.Success(
                    "El ID de óptica ya es válido: $currentOid",
                )
                return@launch
            }

            val memberships = membershipRepository.fetchMembershipsForCurrentUser()
            when {
                memberships.isEmpty() -> {
                    _sessionRepairState.value = SessionRepairState.Error(
                        "No se encontraron membresías en el servidor. " +
                            "Probablemente no hay ópticas asociadas a esta cuenta. " +
                            "Probá cerrar sesión y volver a iniciar.",
                    )
                }
                memberships.size > 1 -> {
                    _sessionRepairState.value = SessionRepairState.Error(
                        "Tenés ${memberships.size} ópticas. " +
                            "Andá a Configuración > Cambiar de óptica para seleccionar la correcta.",
                    )
                }
                else -> {
                    val m = memberships.first()
                    val email = sessionManager.userEmail.first()
                    val name = sessionManager.userName.first()
                    val rol = sessionManager.opticaRol.first()
                    sessionManager.saveSession(
                        opticaId = m.opticaId,
                        email = email,
                        name = name,
                        rol = rol,
                    )
                    _sessionRepairState.value = SessionRepairState.Success(
                        "Sesión reparada. ID de óptica actualizado a: ${m.opticaId}",
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reparando sesión", e)
            _sessionRepairState.value = SessionRepairState.Error(
                "Error inesperado. Reintente más tarde.",
            )
        }
    }
}
