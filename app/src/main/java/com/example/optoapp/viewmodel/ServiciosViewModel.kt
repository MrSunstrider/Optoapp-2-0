package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.Paciente
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.optoapp.data.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ServiciosUiState(
    val id: String = UUID.randomUUID().toString(),
    val ot: String = "",
    val descripcion: String = "",
    val montoTotal: String = "",
    val aCuenta: String = "",
    val estado: String = "Pendiente",
    val fecha: Long = System.currentTimeMillis(),
    val pacienteId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ServiciosViewModel @Inject constructor(
    private val repository: OptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiciosUiState())
    val uiState: StateFlow<ServiciosUiState> = _uiState.asStateFlow()

    val allServicios: StateFlow<List<ServicioExtra>> = repository.getAllServicios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pacientes: StateFlow<List<Paciente>> = repository.allPacientes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateUiState(update: (ServiciosUiState) -> ServiciosUiState) {
        _uiState.value = update(_uiState.value)
    }

    fun loadServicio(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.getServicioById(id)) {
                is Resource.Success -> {
                    val s = result.data!!
                    _uiState.value = ServiciosUiState(
                        id = s.id,
                        ot = s.ot,
                        descripcion = s.descripcion,
                        montoTotal = s.montoTotal.toString(),
                        aCuenta = s.aCuenta.toString(),
                        estado = s.estado,
                        fecha = s.fecha,
                        pacienteId = s.pacienteId
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> { }
            }
        }
    }

    fun saveServicio(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.descripcion.isBlank() || state.montoTotal.isBlank()) return

        val servicio = ServicioExtra(
            id = state.id,
            ot = state.ot,
            descripcion = state.descripcion,
            montoTotal = state.montoTotal.toDoubleOrNull() ?: 0.0,
            aCuenta = state.aCuenta.toDoubleOrNull() ?: 0.0,
            estado = state.estado,
            fecha = state.fecha,
            pacienteId = state.pacienteId
        )

        viewModelScope.launch {
            repository.insertServicio(servicio)
            onSuccess()
        }
    }

    fun deleteServicio(servicio: ServicioExtra) {
        viewModelScope.launch {
            repository.deleteServicio(servicio)
        }
    }
}
