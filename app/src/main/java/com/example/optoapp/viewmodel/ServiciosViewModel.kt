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
import com.example.optoapp.data.Pago

data class ServiciosUiState(
    val id: String = UUID.randomUUID().toString(),
    val ot: String = "",
    val descripcion: String = "",
    val montoTotal: String = "",
    val estado: String = "Pendiente",
    val fecha: Long = System.currentTimeMillis(),
    val pacienteId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    val pagos: List<Pago> = emptyList(),
    val pagosToDelete: List<Pago> = emptyList(),
    val generatedId: String = UUID.randomUUID().toString(),
    val isEdit: Boolean = false
)

@HiltViewModel
class ServiciosViewModel @Inject constructor(
    private val repository: com.example.optoapp.data.OptoRepository,
    private val sessionManager: com.example.optoapp.data.SessionManager,
    private val syncFinanzasUseCase: com.example.optoapp.domain.SyncFinanzasUseCase
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
            _uiState.value = _uiState.value.copy(isLoading = true, generatedId = id)
            when (val result = repository.getServicioById(id)) {
                is Resource.Success -> {
                    val s = result.data!!
                    val loadedPagos = repository.getPagosByServicioExtra(id).first()
                    _uiState.value = ServiciosUiState(
                        id = s.id,
                        ot = s.ot,
                        descripcion = s.descripcion,
                        montoTotal = s.montoTotal.toString(),
                        estado = s.estado,
                        fecha = s.fecha,
                        pacienteId = s.pacienteId,
                        pagos = loadedPagos,
                        generatedId = id,
                        isEdit = true
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> { }
            }
        }
    }

    fun addPago(pago: Pago) {
        _uiState.update { it.copy(pagos = it.pagos + pago) }
    }

    fun updatePagoLocal(pago: Pago) {
        _uiState.update { s ->
            val updatedPagos = s.pagos.map { if (it.id == pago.id) pago else it }
            s.copy(pagos = updatedPagos)
        }
    }

    fun removePagoLocal(pago: Pago) {
        _uiState.update { s ->
            val updatedPagos = s.pagos.filter { it.id != pago.id }
            val updatedToDelete = if (pago.id.isNotEmpty()) s.pagosToDelete + pago else s.pagosToDelete
            s.copy(pagos = updatedPagos, pagosToDelete = updatedToDelete)
        }
    }

    fun saveServicio(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.descripcion.isBlank() || state.montoTotal.isBlank()) return@launch

            val currentOpticaId = sessionManager.opticaId.first()
            val finalId = if (state.id.isNotBlank()) state.id else state.generatedId
            val finalACuenta = state.pagos.sumOf { it.monto }

            val servicio = ServicioExtra(
                id = finalId,
                ot = state.ot,
                descripcion = state.descripcion,
                montoTotal = state.montoTotal.toDoubleOrNull() ?: 0.0,
                aCuenta = finalACuenta,
                estado = state.estado,
                fecha = state.fecha,
                pacienteId = state.pacienteId,
                metodoPago = "",
                opticaId = currentOpticaId
            )

            if (state.isEdit) {
                repository.updateServicio(servicio)
            } else {
                repository.insertServicio(servicio)
            }
            
            // Guardar pagos vinculados a este servicio
            state.pagos.forEach { pago ->
                val pagoToSave = pago.copy(servicioExtraId = finalId, opticaId = currentOpticaId)
                repository.insertPago(pagoToSave)
            }

            // Eliminar pagos marcados
            state.pagosToDelete.forEach { pago ->
                repository.deletePago(pago)
            }

            // Silent Sync en segundo plano
            viewModelScope.launch { 
                try {
                    syncFinanzasUseCase(currentOpticaId)
                } catch (e: Exception) {}
            }

            onSuccess()
        }
    }

    fun deleteServicio(servicio: ServicioExtra) {
        viewModelScope.launch {
            repository.deleteServicio(servicio)
        }
    }
}
