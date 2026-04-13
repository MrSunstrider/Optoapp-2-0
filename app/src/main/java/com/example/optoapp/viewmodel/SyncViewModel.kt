package com.example.optoapp.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
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

    /**
     * Dispara la sincronización manual completa (Bidireccional).
     */
    fun performFullSync() = viewModelScope.launch {
        _syncState.value = SyncState.Loading
        
        val opticaId = sessionManager.opticaId.first()
        
        // 1. Sincronizar Pacientes (Bidireccional por defecto en Fase 3)
        val pacientesResult = syncPacientesUseCase(opticaId)
        if (pacientesResult is Resource.Error) {
            _syncState.value = SyncState.Error(pacientesResult.message ?: "Error en pacientes")
            return@launch
        }

        // 2. Sincronizar Historial Clínico
        val historialResult = syncHistorialUseCase(opticaId)
        if (historialResult is Resource.Error) {
            _syncState.value = SyncState.Error(historialResult.message ?: "Error en historial")
            return@launch
        }

        // 3. Sincronizar Finanzas (Dispensaciones y Pagos)
        val finanzasResult = syncFinanzasUseCase(opticaId)
        if (finanzasResult is Resource.Error) {
            _syncState.value = SyncState.Error(finanzasResult.message ?: "Error en finanzas")
            return@launch
        }

        _syncState.value = SyncState.Success("Sincronización completada con éxito")
    }

    /**
     * Sincronización automática silenciosa (solo subida).
     * No cambia el estado global de UI para no interrumpir al usuario.
     */
    fun performSilentSync() = viewModelScope.launch {
        if (_isSilentSyncing.value || !isNetworkAvailable()) return@launch
        _isSilentSyncing.value = true
        try {
            val opticaId = sessionManager.opticaId.first()
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
