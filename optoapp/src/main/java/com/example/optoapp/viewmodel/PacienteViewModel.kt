package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.auth.AuthorizationGuard
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException
import javax.inject.Inject

sealed class DeletePacienteResult {
    data class Success(val remainingDeletesToday: Int) : DeletePacienteResult()
    data class Error(val message: String) : DeletePacienteResult()
}

@HiltViewModel
class PacienteViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
) : ViewModel() {
    companion object {
        private const val TAG = "PacienteViewModel"
        private const val DAILY_DELETE_LIMIT = 10
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _activeFilter = MutableStateFlow<String?>(null)
    val activeFilter: StateFlow<String?> = _activeFilter

    private val _sortOrder = MutableStateFlow("reciente")
    val sortOrder: StateFlow<String> = _sortOrder

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                withTimeout(5_000) {
                    pacientes.dropWhile { it.isEmpty() }.first()
                }
            } catch (_: Exception) { }
            _isLoading.value = false
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)

    fun refresh() {
        _refreshTrigger.value = System.currentTimeMillis()
        _isLoading.value = true
        viewModelScope.launch {
            try {
                pacientes.dropWhile { it.isEmpty() }.first()
            } catch (_: Exception) { }
            _isLoading.value = false
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pacientes: StateFlow<List<Paciente>> = combine(
        _searchQuery,
        _activeFilter,
        _sortOrder,
        _refreshTrigger,
        sessionManager.opticaId,
    ) { query, filter, sort, _, opticaId ->
        arrayOf(query, filter ?: "", sort, opticaId)
    }.flatMapLatest { (query, filter, sort, opticaId) ->
        val baseFlow = when (filter) {
            "Saldo Pendiente" -> repository.getPacientesWithPendingBalanceForOptica(opticaId)
            "Estado de entrega" -> repository.getPacientesWithPendingDeliveryForOptica(opticaId)
            else -> if (query.isEmpty()) {
                repository.pacientesFlowForOptica(opticaId)
            } else {
                repository.searchPacientesForOptica(opticaId, query)
            }
        }

        baseFlow.map { list ->
            if (query.isNotEmpty() && filter.isNotEmpty()) {
                list.filter {
                    it.nombreCompleto.contains(query, ignoreCase = true) ||
                        it.id.contains(query, ignoreCase = true) ||
                        it.telefono.contains(query) ||
                        it.historiaOptometrica.orEmpty().contains(query, ignoreCase = true)
                }
            } else {
                list
            }
        }.map { list ->
            when (sort) {
                "reciente" -> list.sortedByDescending { it.fechaCreacion }
                "antiguo" -> list.sortedBy { it.fechaCreacion }
                else -> list.sortedBy { it.nombreCompleto.lowercase() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    fun setFilter(filter: String?) {
        _activeFilter.value = if (_activeFilter.value == filter) null else filter
    }
    fun setSort(sort: String) {
        _sortOrder.value = sort
    }

    private val _lastEvaluacion = MutableStateFlow<Resource<EvaluacionClinica>?>(null)
    val lastEvaluacion: StateFlow<Resource<EvaluacionClinica>?> = _lastEvaluacion

    private val _lastDispensacion = MutableStateFlow<Resource<DispensacionOptica>?>(null)
    val lastDispensacion: StateFlow<Resource<DispensacionOptica>?> = _lastDispensacion

    fun loadLastEvaluacion(pacienteId: String) {
        viewModelScope.launch {
            _lastEvaluacion.value = Resource.Loading()
            _lastEvaluacion.value = repository.getLastEvaluacionByPacienteId(pacienteId)
        }
    }

    fun loadLastDispensacion(pacienteId: String) {
        viewModelScope.launch {
            _lastDispensacion.value = Resource.Loading()
            _lastDispensacion.value = repository.getLastDispensacionByPacienteId(pacienteId)
        }
    }

    fun resetLastEvaluacion() {
        _lastEvaluacion.value = null
    }
    fun resetLastDispensacion() {
        _lastDispensacion.value = null
    }

    // Reactive pagos sum by dispensacion for ResumenDispensacionDialog balance
    // Anulaciones (negative monto) are INCLUDED so they net out correctly.
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagosSumByDispensacion: StateFlow<Map<String, Double>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            repository.getAllPagosFlowForOptica(opticaId)
                .map { pagos ->
                    pagos.filter { it.dispensacionId != null }
                        .groupBy { it.dispensacionId!! }
                        .mapValues { (_, pags) -> pags.sumOf { it.monto } }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    suspend fun savePaciente(paciente: Paciente) {
        val oid = sessionManager.opticaId.first()
        val role = sessionManager.opticaRol.first()
        try {
            AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "guardar paciente")
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("No autorizado para guardar pacientes")
        }
        val toSave = paciente.copy(opticaId = oid)
        val historiaNorm = toSave.historiaOptometrica?.trim().orEmpty()
        if (historiaNorm.isNotEmpty()) {
            val duplicated = repository.existsDuplicateHistoriaOptometrica(
                opticaId = oid,
                historia = historiaNorm,
                excludePacienteId = toSave.id,
            )
            if (duplicated) {
                throw IllegalArgumentException(
                    "Ya existe una historia optometrica con ese numero en esta optica.",
                )
            }
        }
        repository.insertPaciente(toSave)
        postSaveSyncScheduler.schedulePacientesSync(oid)
    }

    suspend fun getPaciente(id: String): Paciente? {
        val oid = sessionManager.opticaId.first()
        val result = repository.getPacienteByIdScoped(id, oid)
        return if (result is Resource.Success) result.data else null
    }

    suspend fun suggestHistoriaOptometrica(): String {
        val oid = sessionManager.opticaId.first()
        return repository.suggestNextHistoriaOptometrica(oid)
    }

    suspend fun existsDuplicateHistoriaOptometrica(historia: String, excludePacienteId: String?): Boolean {
        val oid = sessionManager.opticaId.first()
        return repository.existsDuplicateHistoriaOptometrica(oid, historia, excludePacienteId)
    }

    suspend fun deletePacienteGuarded(paciente: Paciente): DeletePacienteResult {
        val oid = sessionManager.opticaId.first()
        val role = sessionManager.opticaRol.first()
        try {
            AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "eliminar paciente")
        } catch (e: IllegalArgumentException) {
            return DeletePacienteResult.Error(e.message ?: "No autorizado")
        }

        val deletesToday = sessionManager.getPacienteDeleteCountToday(oid)
        if (deletesToday >= DAILY_DELETE_LIMIT) {
            return DeletePacienteResult.Error(
                "Límite diario de eliminaciones alcanzado ($DAILY_DELETE_LIMIT). Contacta al administrador.",
            )
        }

        return try {
            // Room delete and tombstone must complete before the remote call.
            // If the remote delete succeeds but we crash before creating the
            // tombstone, download Phase 1 has nothing to retry against.
            repository.deletePaciente(paciente)
            try {
                repository.deletePacienteRemoto(paciente.id, oid)
            } catch (e: IOException) {
                // Local delete succeeded but remote delete failed. The patient will be
                // re-downloaded from Supabase on the next sync cycle. Surface the error
                // so the user knows the operation was incomplete.
                Log.e(TAG, "deletePaciente: remote delete failed after local delete", e)
                return DeletePacienteResult.Error(
                    "El paciente se eliminó localmente pero no se pudo eliminar en " +
                        "el servidor. Se reintentará automáticamente en la próxima " +
                        "sincronización. Si el problema persiste, contacta al administrador.",
                )
            }
            val used = sessionManager.incrementPacienteDeleteCountToday(oid)
            postSaveSyncScheduler.schedulePacientesSync(oid)
            DeletePacienteResult.Success((DAILY_DELETE_LIMIT - used).coerceAtLeast(0))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "deletePaciente failed: IO error", e)
            DeletePacienteResult.Error(
                "Error inesperado. Reintente más tarde.",
            )
        } catch (e: Exception) {
            Log.e(TAG, "deletePaciente failed", e)
            DeletePacienteResult.Error(
                "Error inesperado. Reintente más tarde.",
            )
        }
    }
}
