package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PacienteViewModel @Inject constructor(
    private val repository: com.example.optoapp.data.OptoRepository,
    private val sessionManager: SessionManager,
    private val syncPacientesUseCase: com.example.optoapp.domain.SyncPacientesUseCase
) : ViewModel() {

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
                    it.telefono.contains(query) 
                }
            } else {
                list
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun setFilter(filter: String?) { _activeFilter.value = if (_activeFilter.value == filter) null else filter }

    fun savePaciente(paciente: Paciente) = viewModelScope.launch { 
        repository.insertPaciente(paciente)
        // Silent sync en segundo plano
        viewModelScope.launch { 
            try {
                syncPacientesUseCase(paciente.opticaId)
            } catch (e: Exception) {}
        }
    }
    
    suspend fun getPaciente(id: String): Paciente? {
        val result = repository.getPacienteById(id)
        return if (result is Resource.Success) result.data else null
    }
}
