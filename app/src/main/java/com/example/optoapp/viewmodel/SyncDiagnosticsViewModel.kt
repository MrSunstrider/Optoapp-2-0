package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

/** P0-T5: lectura de filas con error de sync por óptica. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SyncDiagnosticsViewModel @Inject constructor(
    private val syncEntityStateDao: SyncEntityStateDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    val errorRows: StateFlow<List<SyncEntityState>> = sessionManager.opticaId
        .flatMapLatest { oid -> syncEntityStateDao.observeErrorsForOptica(oid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
