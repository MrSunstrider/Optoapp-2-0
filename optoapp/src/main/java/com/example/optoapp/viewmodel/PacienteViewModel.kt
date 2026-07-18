package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.Resource
import com.example.optoapp.domain.auth.AuthorizationGuard
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DeletePacienteResult {
    data class Success(val remainingDeletesToday: Int) : DeletePacienteResult()
    data class Error(val message: String) : DeletePacienteResult()
}

@HiltViewModel
class PacienteViewModel @Inject constructor(
    private val repository: com.example.optoapp.data.OptoRepository,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val supabase: SupabaseClient
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
            // Wait until opticaId is known and pacientes flow emits first real data
            sessionManager.opticaId.first { it.isNotBlank() }
            // Small delay to let Room query settle
            kotlinx.coroutines.delay(100)
            _isLoading.value = false
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)

    fun refresh() {
        _refreshTrigger.value = System.currentTimeMillis()
        _isLoading.value = true
        viewModelScope.launch {
            try {
                sessionManager.opticaId.first { it.isNotBlank() }
                kotlinx.coroutines.delay(100)
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
        sessionManager.opticaId
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

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun setFilter(filter: String?) { _activeFilter.value = if (_activeFilter.value == filter) null else filter }
    fun setSort(sort: String) { _sortOrder.value = sort }

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

    fun resetLastEvaluacion() { _lastEvaluacion.value = null }
    fun resetLastDispensacion() { _lastDispensacion.value = null }

    suspend fun savePaciente(paciente: Paciente) {
        val oid = sessionManager.opticaId.first()
        val toSave = paciente.copy(opticaId = oid)
        val historiaNorm = toSave.historiaOptometrica?.trim().orEmpty()
        if (historiaNorm.isNotEmpty()) {
            val duplicated = repository.existsDuplicateHistoriaOptometrica(
                opticaId = oid,
                historia = historiaNorm,
                excludePacienteId = toSave.id
            )
            if (duplicated) {
                throw IllegalArgumentException(
                    "Ya existe una historia optometrica con ese numero en esta optica."
                )
            }
        }
        repository.insertPaciente(toSave)
        postSaveSyncScheduler.schedulePacientesSync(oid)
    }
    
    suspend fun getPaciente(id: String): Paciente? {
        val result = repository.getPacienteById(id)
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
                "Límite diario de eliminaciones alcanzado ($DAILY_DELETE_LIMIT). Contacta al administrador."
            )
        }

        return try {
            // Delete from Room first. If local delete fails, we never touch Supabase.
            repository.deletePaciente(paciente)
            try {
                supabase.postgrest["pacientes"].delete {
                    filter {
                        eq("id", paciente.id)
                        eq("optica_id", oid)
                    }
                }
            } catch (e: IOException) {
                // Remote delete failed but local succeeded; the sync pipeline will propagate
                // the deletion on next cycle.
            }
            val used = sessionManager.incrementPacienteDeleteCountToday(oid)
            postSaveSyncScheduler.schedulePacientesSync(oid)
            DeletePacienteResult.Success((DAILY_DELETE_LIMIT - used).coerceAtLeast(0))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "deletePaciente failed: IO error", e)
            DeletePacienteResult.Error(
                "Error inesperado. Reintente más tarde."
            )
        } catch (e: Exception) {
            Log.e(TAG, "deletePaciente failed", e)
            DeletePacienteResult.Error(
                "Error inesperado. Reintente más tarde."
            )
        }
    }
}
