package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val sessionManager: SessionManager,
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

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
        val opticaId = sessionManager.opticaId.first()
        syncPacientesUseCase(opticaId)
        syncHistorialUseCase(opticaId)
        syncFinanzasUseCase(opticaId)
    }
}
