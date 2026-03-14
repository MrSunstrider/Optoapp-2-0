package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PacienteViewModel @Inject constructor(
    private val repository: OptoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _activeFilter = MutableStateFlow<String?>(null)
    val activeFilter: StateFlow<String?> = _activeFilter

    val pacientes = combine(_searchQuery, _activeFilter, repository.allPacientes) { query, filter, list ->
        var filteredList = if (query.isEmpty()) list
        else list.filter { 
            it.nombreCompleto.contains(query, ignoreCase = true) || 
            it.id.contains(query, ignoreCase = true) || 
            it.telefono.contains(query) 
        }
        
        when (filter) {
            "Fotocromáticos" -> filteredList.filter { it.ultimasEtiquetas.contains("Fotocromático") }
            "Multifocales" -> filteredList.filter { it.ultimasEtiquetas.contains("Multifocal") }
            else -> filteredList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun setFilter(filter: String?) { _activeFilter.value = if (_activeFilter.value == filter) null else filter }

    fun savePaciente(paciente: Paciente) = viewModelScope.launch { repository.insertPaciente(paciente) }
    
    suspend fun getPaciente(id: String): Paciente? {
        val result = repository.getPacienteById(id)
        return if (result is Resource.Success) result.data else null
    }
}
