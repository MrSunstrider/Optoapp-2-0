package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.notifications.NotificationHelper
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EvaluacionViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val notificationHelper: NotificationHelper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EvaluacionUiState())
    val uiState: StateFlow<EvaluacionUiState> = _uiState.asStateFlow()

    fun getEvaluacionesByPaciente(pacienteId: String) = sessionManager.opticaId.flatMapLatest { opticaId ->
        repository.getEvaluacionesByPaciente(pacienteId, opticaId)
    }

    fun loadEvaluacion(evaluacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val opticaId = sessionManager.opticaId.first()
            when (val result = repository.getEvaluacionById(evaluacionId, opticaId)) {
                is Resource.Success -> {
                    val e = result.data ?: return@launch
                    val dipFormatted = DipParser.formatDipForUi(e.dipLejos.orEmpty(), e.dipTotalMm, e.dnpOdMm, e.dnpOiMm)
                    val pResult = repository.getPacienteByIdScoped(e.pacienteId, opticaId)
                    val nombre = if (pResult is Resource.Success) pResult.data?.nombreCompleto ?: "" else ""
                    _uiState.update {
                        e.toEvaluacionUiState().copy(dipLejos = dipFormatted, pacienteNombre = nombre)
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
            val dipParsed = DipParser.parseDipOrDnp(s.dipLejos)

            val ev = s.toEvaluacionClinica(evaluacionId, pacienteId, currentOpticaId, dipParsed)
            if (evaluacionId != null && evaluacionId != "null") {
                repository.updateEvaluacion(ev)
            } else {
                repository.insertEvaluacion(ev)
            }
            postSaveSyncScheduler.scheduleHistorialSync(currentOpticaId)
            val pResult = repository.getPacienteByIdScoped(pacienteId, currentOpticaId)
            val pName = if (pResult is Resource.Success) pResult.data?.nombreCompleto ?: "Paciente" else "Paciente"
            onComplete(ev.id, pName)
        }
    }

    fun saveAndScheduleReminder(
        pacienteId: String,
        evaluacionId: String?,
        programarRecordatorio: Boolean,
        onComplete: (String) -> Unit,
    ) {
        saveEvaluacion(pacienteId, evaluacionId) { savedId, pName ->
            if (programarRecordatorio && _uiState.value.proximaCita != null) {
                _uiState.value.proximaCita?.let { notificationHelper.scheduleWorkManagerReminder(pName, it, savedId) }
            } else {
                notificationHelper.cancelReminder(savedId)
            }
            onComplete(savedId)
        }
    }

    fun deleteEvaluacion(evaluacionId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = repository.getEvaluacionById(evaluacionId, sessionManager.opticaId.first())
            if (result is Resource.Success) {
                result.data?.let { repoData ->
                    repository.deleteEvaluacion(repoData)
                    val oid = sessionManager.opticaId.first()
                    postSaveSyncScheduler.scheduleHistorialSync(oid)
                }
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

    /**
     * Carga la edad del paciente y auto-calculla la ADD sugerida en la refracción.
     * Se llama al crear una nueva evaluación. La ADD se puede modificar manualmente después.
     */
    fun loadPacienteEdadAndCalculateAdd(pacienteId: String) {
        viewModelScope.launch {
            runCatching {
                val p = repository.getPacienteByIdScoped(pacienteId, sessionManager.opticaId.first())
                if (p is Resource.Success) {
                    val edad = p.data?.edad ?: 0
                    val nombre = p.data?.nombreCompleto ?: ""
                    val add = com.example.optoapp.util.calcularAddPorEdad(edad)
                    _uiState.update { state ->
                        val withEdad = state.copy(pacienteEdad = edad, pacienteNombre = nombre)
                        if (add.isNotBlank()) {
                            withEdad.copy(hasAdd = true, addCercaOd = add, addCercaOi = add)
                        } else {
                            withEdad
                        }
                    }
                }
            }
        }
    }

    fun normalizeAndTranspose(ojo: String) {
        _uiState.update { normalizeAndTranspose(it, ojo) }
    }
}
