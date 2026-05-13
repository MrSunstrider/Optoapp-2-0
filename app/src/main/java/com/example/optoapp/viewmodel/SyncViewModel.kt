package com.example.optoapp.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncTelemetry
import com.example.optoapp.data.SyncTelemetryRemoteRow
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.subscription.SubscriptionManager
import com.example.optoapp.sync.errorLabelForException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import com.example.optoapp.util.SyncErrorSanitizer
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncSessionHelper
import com.example.optoapp.domain.sync.SyncManager
import com.example.optoapp.domain.sync.SyncResult
import com.example.optoapp.sync.SyncGate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Paso 5.1: SyncViewModel
 * Orquestador global de la sincronización de datos local <-> nube.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val membershipRepository: MembershipRepository,
    private val repository: OptoRepository,
    private val syncTelemetry: SyncTelemetry,
    private val subscriptionManager: SubscriptionManager,
    private val supabase: SupabaseClient,
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase,
    private val syncInventarioUseCase: SyncInventarioUseCase,
    private val syncGate: SyncGate,
    private val syncManager: SyncManager,
    private val supabaseObserver: com.example.optoapp.domain.observer.SupabaseObserver
) : ViewModel() {

    companion object {
        private const val TAG = "SyncViewModel"
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isSilentSyncing = MutableStateFlow(false)
    val isSilentSyncing: StateFlow<Boolean> = _isSilentSyncing.asStateFlow()

    /** Vuelve el estado de UI de sync a inactivo (tras mensaje o cierre de diálogo). */
    fun clearSyncUiState() {
        _syncState.value = SyncState.Idle
    }

    /**
     * Dispara la sincronización manual completa (Bidireccional) usando el patrón Strategy.
     */
    fun performFullSync() = viewModelScope.launch {
        _syncState.value = SyncState.Loading
        if (!isNetworkAvailable()) {
            _syncState.value = SyncState.Error("Sin conexión a internet.")
            return@launch
        }

        SyncSessionHelper.refreshSessionBeforeSync(supabase)
        val contextCheck = ensureSyncContext()
        if (contextCheck != null) {
            _syncState.value = SyncState.Error(contextCheck)
            return@launch
        }

        val opticaId = sessionManager.opticaId.first()
        repository.reassignLegacyMiOpticaBaseTo(opticaId)

        val outcome = syncGate.mutex.withLock {
            syncManager.switchToFullSync()
            syncManager.performSync(opticaId)
        }

        when (outcome) {
            is SyncResult.Error -> {
                val msg = SyncErrorSanitizer.forUserMessage(outcome.message)
                syncTelemetry.recordFullSyncError(outcome.message)
                _syncState.value = SyncState.Error(msg)
            }
            is SyncResult.Success -> {
                syncTelemetry.recordFullSyncSuccess()
                recordRemoteSyncTelemetry(opticaId, "ok", "finalizado", null)
                runCatching { subscriptionManager.refreshPlanFromServer(opticaId) }
                _syncState.value = SyncState.Success("Sincronización completada con éxito")
            }
            else -> { /* Otros estados si existieran */ }
        }
    }

    /**
     * Sincronización automática silenciosa (solo subida).
     * No cambia el estado global de UI para no interrumpir al usuario.
     */
    fun performSilentSync() = viewModelScope.launch {
        val contextCheck = ensureSyncContext()
        if (contextCheck != null) {
            Log.w(TAG, "Sync silenciosa cancelada: $contextCheck")
            return@launch
        }
        val opticaId = sessionManager.opticaId.first()
        // Sin red también: migrar datos legacy a la óptica de sesión para que las listas no queden vacías.
        repository.reassignLegacyMiOpticaBaseTo(opticaId)
        if (_isSilentSyncing.value || !isNetworkAvailable()) return@launch
        _isSilentSyncing.value = true
        try {
            syncGate.mutex.withLock {
                SyncSessionHelper.refreshSessionBeforeSync(supabase)
                var hasErrors = false
                when (val p = syncPacientesUseCase(opticaId, downloadAfterUpload = false)) {
                    is Resource.Error -> {
                        hasErrors = true
                        Log.w(TAG, "Sync silenciosa (pacientes): ${p.message}")
                        recordRemoteSyncTelemetry(opticaId, "error", "pacientes", p.message)
                    }
                    else -> {}
                }
                when (val h = syncHistorialUseCase(opticaId, downloadAfterUpload = false)) {
                    is Resource.Error -> {
                        hasErrors = true
                        Log.w(TAG, "Sync silenciosa (historial): ${h.message}")
                        recordRemoteSyncTelemetry(opticaId, "error", "historial", h.message)
                    }
                    else -> {}
                }
                when (val f = syncFinanzasUseCase(opticaId, downloadAfterUpload = false)) {
                    is Resource.Error -> {
                        hasErrors = true
                        Log.w(TAG, "Sync silenciosa (finanzas): ${f.message}")
                        recordRemoteSyncTelemetry(opticaId, "error", "finanzas", f.message)
                    }
                    else -> {}
                }
                when (val i = syncInventarioUseCase(opticaId, downloadAfterUpload = false)) {
                    is Resource.Error -> {
                        hasErrors = true
                        Log.w(TAG, "Sync silenciosa (inventario): ${i.message}")
                        recordRemoteSyncTelemetry(opticaId, "error", "inventario", i.message)
                    }
                    else -> {}
                }
                if (!hasErrors) {
                    recordRemoteSyncTelemetry(opticaId, "ok", "inventario", null)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red en sync silenciosa: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado en sync silenciosa: ${e.message}", e)
        } finally {
            _isSilentSyncing.value = false
        }
    }

    private suspend fun recordRemoteSyncTelemetry(
        opticaId: String,
        status: String,
        stage: String,
        rawError: String?
    ) {
        runCatching {
            val safeError = SyncErrorSanitizer.forUserMessage(rawError).take(500)
            val row = SyncTelemetryRemoteRow(
                opticaId = opticaId,
                lastSyncAt = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString(),
                lastStatus = status,
                lastStage = stage,
                lastError = if (status == "error") safeError else ""
            )
            supabase.postgrest["sync_telemetry_optica"].upsert(row)
        }.onFailure { e ->
            Log.w(TAG, "No se pudo guardar telemetría remota de sync: ${e.message}")
        }
    }

    /**
     * Ejemplo de uso del patrón Observer: Escucha cambios en la telemetría en tiempo real
     * desde otros dispositivos para la misma óptica.
     */
    fun observeRemoteTelemetry(opticaId: String) {
        viewModelScope.launch {
            supabaseObserver.observeTable("sync_telemetry_optica", opticaId)
                .collect { action ->
                    Log.d(TAG, "Cambio detectado en telemetría remota: $action")
                    // Aquí se podría disparar una actualización de UI si otro dispositivo finalizó sync
                }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private suspend fun ensureSyncContext(): String? {
        val currentUser = runCatching { supabase.auth.currentUserOrNull() }.getOrNull()
            ?: return "Tu sesión de Supabase no está activa. Vuelve a iniciar sesión."
        val opticaId = sessionManager.opticaId.first()
        if (opticaId.isBlank() || opticaId == SessionManager.LEGACY_OPTICA_ID) {
            return "Debes seleccionar o crear una óptica antes de sincronizar."
        }
        val memberships = membershipRepository.fetchMembershipsForCurrentUser()
        val belongsToOptica = memberships.any { it.opticaId == opticaId }
        if (!belongsToOptica) {
            return "Tu cuenta (${currentUser.email ?: "usuario"}) no tiene acceso a la óptica actual. Reingresa y selecciona una óptica válida."
        }
        return null
    }
}
