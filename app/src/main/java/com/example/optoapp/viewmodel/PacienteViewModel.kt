package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.Resource
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
        private const val DAILY_DELETE_LIMIT = 10
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _activeFilter = MutableStateFlow<String?>(null)
    val activeFilter: StateFlow<String?> = _activeFilter

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pacientes: StateFlow<List<Paciente>> = combine(
        _searchQuery,
        _activeFilter,
        sessionManager.opticaId
    ) { query, filter, opticaId ->
        Triple(query, filter, opticaId)
    }.flatMapLatest { (query, filter, opticaId) ->
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
            if (query.isNotEmpty() && (filter != null)) {
                // Si hay filtro Y búsqueda, aplicamos la búsqueda sobre el resultado del filtro
                list.filter { 
                    it.nombreCompleto.contains(query, ignoreCase = true) || 
                    it.id.contains(query, ignoreCase = true) || 
                    it.telefono.contains(query) ||
                    it.historiaOptometrica.orEmpty().contains(query, ignoreCase = true)
                }
            } else {
                list
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun setFilter(filter: String?) { _activeFilter.value = if (_activeFilter.value == filter) null else filter }

    /**
     * Persiste el paciente con el [SessionManager.opticaId] activo y encola sync de pacientes (scope de aplicación).
     */
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

    /** Sugerencia correlativa para historia optométrica en la óptica activa. */
    suspend fun suggestHistoriaOptometrica(): String {
        val oid = sessionManager.opticaId.first()
        return repository.suggestNextHistoriaOptometrica(oid)
    }

    /** Valida duplicados de historia optométrica en la óptica activa. */
    suspend fun existsDuplicateHistoriaOptometrica(historia: String, excludePacienteId: String?): Boolean {
        val oid = sessionManager.opticaId.first()
        return repository.existsDuplicateHistoriaOptometrica(oid, historia, excludePacienteId)
    }

    suspend fun deletePacienteGuarded(paciente: Paciente): DeletePacienteResult {
        val oid = sessionManager.opticaId.first()
        val role = sessionManager.opticaRol.first().trim().lowercase()
        if (role !in setOf("admin", "gerente")) {
            return DeletePacienteResult.Error("Solo admin o gerente pueden eliminar pacientes.")
        }

        val deletesToday = sessionManager.getPacienteDeleteCountToday(oid)
        if (deletesToday >= DAILY_DELETE_LIMIT) {
            return DeletePacienteResult.Error(
                "Límite diario de eliminaciones alcanzado ($DAILY_DELETE_LIMIT). Contacta al administrador."
            )
        }

        return try {
            // Eliminar primero en remoto para mantener consistencia entre dispositivos.
            supabase.postgrest["pacientes"].delete {
                filter {
                    eq("id", paciente.id)
                    eq("optica_id", oid)
                }
            }
            repository.deletePaciente(paciente)
            val used = sessionManager.incrementPacienteDeleteCountToday(oid)
            postSaveSyncScheduler.schedulePacientesSync(oid)
            DeletePacienteResult.Success((DAILY_DELETE_LIMIT - used).coerceAtLeast(0))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            DeletePacienteResult.Error(
                "No se pudo eliminar el paciente: ${e.localizedMessage ?: "error desconocido"}"
            )
        } catch (e: Exception) {
            DeletePacienteResult.Error(
                "No se pudo eliminar el paciente: ${e.localizedMessage ?: "error desconocido"}"
            )
        }
    }
}
