package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SyncTelemetryRemoteRow
import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

/** Lectura de filas con error de sync por óptica (diagnóstico en Configuración). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SyncDiagnosticsViewModel @Inject constructor(
    private val syncEntityStateDao: SyncEntityStateDao,
    private val sessionManager: SessionManager,
    private val supabase: SupabaseClient
) : ViewModel() {

    val errorRows: StateFlow<List<SyncEntityState>> = sessionManager.opticaId
        .flatMapLatest { oid -> syncEntityStateDao.observeErrorsForOptica(oid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _remoteTelemetry = MutableStateFlow<SyncTelemetryRemoteRow?>(null)
    val remoteTelemetry: StateFlow<SyncTelemetryRemoteRow?> = _remoteTelemetry.asStateFlow()

    private val _remoteTelemetryLoading = MutableStateFlow(false)
    val remoteTelemetryLoading: StateFlow<Boolean> = _remoteTelemetryLoading.asStateFlow()

    private val _remoteTelemetryError = MutableStateFlow<String?>(null)
    val remoteTelemetryError: StateFlow<String?> = _remoteTelemetryError.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.opticaId.collectLatest { oid ->
                fetchRemoteTelemetry(oid)
            }
        }
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
            supabase.postgrest["sync_telemetry_optica"]
                .select {
                    filter { eq("optica_id", opticaId) }
                }
                .decodeList<SyncTelemetryRemoteRow>()
                .firstOrNull()
        }.onSuccess { row ->
            _remoteTelemetry.value = row
        }.onFailure { e ->
            _remoteTelemetryError.value = e.localizedMessage ?: "No se pudo consultar telemetría remota."
        }
        _remoteTelemetryLoading.value = false
    }

    /** Elimina solo el historial de errores mostrado en la app (no borra datos clínicos). */
    fun clearErrorHistory() = viewModelScope.launch {
        val oid = sessionManager.opticaId.first()
        syncEntityStateDao.deleteErrorsForOptica(oid)
    }
}
