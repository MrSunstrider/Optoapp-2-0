package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OptoViewModel(private val repository: OptoRepository, private val securityManager: SecurityManager) : ViewModel() {

    val userPin = securityManager.userPin
    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput

    fun onPinDigit(digit: String) {
        if (_pinInput.value.length < 6) _pinInput.value += digit
    }

    fun clearPin() { _pinInput.value = "" }

    suspend fun validatePin(): Boolean = _pinInput.value == userPin.first()

    fun updatePin(oldPin: String, newPin: String) = viewModelScope.launch {
        if (oldPin == userPin.first()) securityManager.savePin(newPin)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _activeFilter = MutableStateFlow<String?>(null)
    val activeFilter: StateFlow<String?> = _activeFilter

    val pacientes = combine(_searchQuery, _activeFilter, repository.allPacientes) { query, filter, list ->
        var filteredList = if (query.isEmpty()) list
        else list.filter { it.nombreCompleto.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true) || it.telefono.contains(query) }
        
        when (filter) {
            "Fotocromáticos" -> filteredList.filter { it.ultimasEtiquetas.contains("Fotocromático") }
            "Multifocales" -> filteredList.filter { it.ultimasEtiquetas.contains("Multifocal") }
            else -> filteredList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun setFilter(filter: String?) { _activeFilter.value = if (_activeFilter.value == filter) null else filter }

    fun savePaciente(paciente: Paciente) = viewModelScope.launch { repository.insertPaciente(paciente) }
    suspend fun getPaciente(id: String): Paciente? = repository.getPacienteById(id)

    fun getEvaluaciones(pacienteId: String) = repository.getEvaluacionesByPaciente(pacienteId)
    suspend fun getEvaluacion(id: String): EvaluacionClinica? = repository.getEvaluacionById(id)
    fun saveEvaluacion(evaluacion: EvaluacionClinica) = viewModelScope.launch { repository.insertEvaluacion(evaluacion) }

    fun getDispensaciones(pacienteId: String) = repository.getDispensacionesByPaciente(pacienteId)
    suspend fun getDispensacion(id: String): DispensacionOptica? = repository.getDispensacionById(id)
    
    val allDispensaciones: StateFlow<List<DispensacionOptica>> = repository.getAllDispensaciones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveDispensacion(dispensacion: DispensacionOptica) = viewModelScope.launch { repository.insertDispensacion(dispensacion) }

    // Servicios Extra
    val allServicios: StateFlow<List<ServicioExtra>> = repository.getAllServicios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getServiciosByPaciente(pacienteId: String) = repository.getServiciosByPaciente(pacienteId)
    
    suspend fun getServicioById(id: String) = repository.getServicioById(id)
    
    fun saveServicio(servicio: ServicioExtra) = viewModelScope.launch { repository.insertServicio(servicio) }
    
    fun deleteServicio(servicio: ServicioExtra) = viewModelScope.launch { repository.deleteServicio(servicio) }

    suspend fun getBackupJson(): String = com.google.gson.Gson().toJson(repository.getBackupData())

    fun restoreBackup(json: String) = viewModelScope.launch {
        try {
            val data = com.google.gson.Gson().fromJson(json, BackupData::class.java)
            repository.restoreBackup(data)
        } catch (e: Exception) { e.printStackTrace() }
    }
}

class OptoViewModelFactory(private val repository: OptoRepository, private val securityManager: SecurityManager) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OptoViewModel(repository, securityManager) as T
    }
}
