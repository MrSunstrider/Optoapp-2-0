package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.notifications.NotificationHelper
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EvaluacionViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val notificationHelper: NotificationHelper
) : ViewModel() {
    private val _uiState = MutableStateFlow(EvaluacionUiState())
    val uiState: StateFlow<EvaluacionUiState> = _uiState.asStateFlow()

    fun getEvaluacionesByPaciente(pacienteId: String) = repository.getEvaluacionesByPaciente(pacienteId)

    fun loadEvaluacion(evaluacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getEvaluacionById(evaluacionId)) {
                is Resource.Success -> {
                    val e = result.data!!
                    val dipFormatted = formatDipForUi(e.dipLejos, e.dipTotalMm, e.dnpOdMm, e.dnpOiMm)
                    _uiState.update {
                        e.toEvaluacionUiState().copy(dipLejos = dipFormatted)
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> { }
            }
        }
    }

    fun updateUiState(update: (EvaluacionUiState) -> EvaluacionUiState) {
        _uiState.update(update)
    }

    fun saveEvaluacion(pacienteId: String, evaluacionId: String?, onComplete: (String, String) -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            val currentOpticaId = sessionManager.opticaId.first()
            val dipParsed = parseDipOrDnp(s.dipLejos)

            val ev = s.toEvaluacionClinica(evaluacionId, pacienteId, currentOpticaId, dipParsed)
            if (evaluacionId != null && evaluacionId != "null") {
                repository.updateEvaluacion(ev)
            } else {
                repository.insertEvaluacion(ev)
            }
            postSaveSyncScheduler.scheduleHistorialSync(currentOpticaId)
            val pResult = repository.getPacienteById(pacienteId)
            val pName = if (pResult is Resource.Success) pResult.data?.nombreCompleto ?: "Paciente" else "Paciente"
            onComplete(ev.id, pName)
        }
    }

    fun saveAndScheduleReminder(
        pacienteId: String,
        evaluacionId: String?,
        programarRecordatorio: Boolean,
        onComplete: (String) -> Unit
    ) {
        saveEvaluacion(pacienteId, evaluacionId) { savedId, pName ->
            if (programarRecordatorio && _uiState.value.proximaCita != null) {
                notificationHelper.scheduleWorkManagerReminder(pName, _uiState.value.proximaCita!!, savedId)
            } else {
                notificationHelper.cancelReminder(savedId)
            }
            onComplete(savedId)
        }
    }

    fun deleteEvaluacion(evaluacionId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = repository.getEvaluacionById(evaluacionId)
            if (result is Resource.Success) {
                repository.deleteEvaluacion(result.data!!)
                val oid = sessionManager.opticaId.first()
                postSaveSyncScheduler.scheduleHistorialSync(oid)
                onComplete()
            }
        }
    }

    // --- Diagnóstico Automático ---

    fun updateDiagnosticAuto() {
        _uiState.update { computeDiagnosticoAuto(it) }
    }

    fun updateOtrosAuto() {
        _uiState.update { computeOtrosAuto(it) }
    }

    fun normalizeAndTranspose(ojo: String) {
        _uiState.update { normalizeAndTranspose(it, ojo) }
    }
}
