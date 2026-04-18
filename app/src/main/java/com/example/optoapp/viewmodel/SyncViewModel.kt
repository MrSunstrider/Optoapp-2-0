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
import com.example.optoapp.subscription.SubscriptionManager
import com.example.optoapp.util.SyncErrorSanitizer
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncSessionHelper
import io.github.jan.supabase.SupabaseClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val repository: OptoRepository,
    private val syncTelemetry: SyncTelemetry,
    private val subscriptionManager: SubscriptionManager,
    private val supabase: SupabaseClient,
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase
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
     * Dispara la sincronización manual completa (Bidireccional).
     */
    fun performFullSync() = viewModelScope.launch {
        _syncState.value = SyncState.Loading
        if (!isNetworkAvailable()) {
            _syncState.value = SyncState.Error(
                "Sin conexión a internet. Comprueba tu red e inténtalo de nuevo."
            )
            return@launch
        }
        SyncSessionHelper.refreshSessionBeforeSync(supabase)

        val opticaId = sessionManager.opticaId.first()
        repository.reassignLegacyMiOpticaBaseTo(opticaId)

        suspend fun runStages(): Resource<Unit> {
            Log.d(TAG, "Sync etapa 1/3: inicio pacientes (opticaId=$opticaId)")
            val pacientesResult = syncPacientesUseCase(opticaId)
            if (pacientesResult is Resource.Error) {
                Log.e(TAG, "Sync etapa 1/3: fin pacientes ERROR")
                return Resource.Error(pacientesResult.message ?: "Error en pacientes")
            }
            Log.d(TAG, "Sync etapa 1/3: fin pacientes OK")

            Log.d(TAG, "Sync etapa 2/3: inicio historial (evaluaciones)")
            val historialResult = syncHistorialUseCase(opticaId)
            if (historialResult is Resource.Error) {
                Log.e(TAG, "Sync etapa 2/3: fin historial ERROR")
                return Resource.Error(historialResult.message ?: "Error en historial")
            }
            Log.d(TAG, "Sync etapa 2/3: fin historial OK")

            Log.d(TAG, "Sync etapa 3/3: inicio finanzas (dispensaciones → servicios_extra → pagos)")
            val finanzasResult = syncFinanzasUseCase(opticaId)
            if (finanzasResult is Resource.Error) {
                Log.e(TAG, "Sync etapa 3/3: fin finanzas ERROR")
                return Resource.Error(finanzasResult.message ?: "Error en finanzas")
            }
            Log.d(TAG, "Sync etapa 3/3: fin finanzas OK")
            return Resource.Success(Unit)
        }

        var outcome = runStages()
        if (outcome is Resource.Error && SyncSessionHelper.looksLikeAuthError(outcome.message)) {
            Log.w(TAG, "Reintento sync tras posible error de auth")
            SyncSessionHelper.refreshSessionBeforeSync(supabase)
            outcome = runStages()
        }

        when (outcome) {
            is Resource.Error -> {
                val msg = SyncErrorSanitizer.forUserMessage(outcome.message)
                syncTelemetry.recordFullSyncError(outcome.message)
                _syncState.value = SyncState.Error(msg)
            }
            else -> {
                syncTelemetry.recordFullSyncSuccess()
                runCatching { subscriptionManager.refreshPlanFromServer(opticaId) }
                _syncState.value = SyncState.Success("Sincronización completada con éxito")
            }
        }
    }

    /**
     * Sincronización automática silenciosa (solo subida).
     * No cambia el estado global de UI para no interrumpir al usuario.
     */
    fun performSilentSync() = viewModelScope.launch {
        if (_isSilentSyncing.value || !isNetworkAvailable()) return@launch
        _isSilentSyncing.value = true
        try {
            SyncSessionHelper.refreshSessionBeforeSync(supabase)
            val opticaId = sessionManager.opticaId.first()
            repository.reassignLegacyMiOpticaBaseTo(opticaId)
            when (val p = syncPacientesUseCase(opticaId)) {
                is Resource.Error -> Log.w(TAG, "Sync silenciosa (pacientes): ${p.message}")
                else -> {}
            }
            when (val h = syncHistorialUseCase(opticaId)) {
                is Resource.Error -> Log.w(TAG, "Sync silenciosa (historial): ${h.message}")
                else -> {}
            }
            when (val f = syncFinanzasUseCase(opticaId)) {
                is Resource.Error -> Log.w(TAG, "Sync silenciosa (finanzas): ${f.message}")
                else -> {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sync silenciosa: excepción no controlada", e)
        } finally {
            _isSilentSyncing.value = false
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
}
