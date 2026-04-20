package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val sessionManager: SessionManager
) : ViewModel() {

    val errorRows: StateFlow<List<SyncEntityState>> = sessionManager.opticaId
        .flatMapLatest { oid -> syncEntityStateDao.observeErrorsForOptica(oid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Elimina solo el historial de errores mostrado en la app (no borra datos clínicos). */
    fun clearErrorHistory() = viewModelScope.launch {
        val oid = sessionManager.opticaId.first()
        syncEntityStateDao.deleteErrorsForOptica(oid)
    }
}
