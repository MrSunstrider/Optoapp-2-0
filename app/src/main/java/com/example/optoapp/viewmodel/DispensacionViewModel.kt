package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DispensacionUiState(
    val tipoLente: String = "",
    val distanciaLente: String = "",
    val materialLente: String = "",
    val tratamientos: List<String> = emptyList(),
    val colorLente: String = "",
    val notasDiseno: String = "",
    
    val origenMontura: String = "",
    val tipoAro: String = "",
    val materialMontura: String = "",
    val descripcionMontura: String = "",
    val tipoMontura: String = "", // Added to match entity
    
    val montoTotal: String = "",
    val tipoMovimiento: String = "",
    val metodoPago: String = "",
    val montoPagado: String = "",
    val estadoEntrega: String = "Pendiente",
    val fecha: Long = System.currentTimeMillis(),
    val fechaVencimientoGarantia: String? = null,
    
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DispensacionViewModel @Inject constructor(
    private val repository: OptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DispensacionUiState())
    val uiState: StateFlow<DispensacionUiState> = _uiState.asStateFlow()

    fun getDispensacionesByPaciente(pacienteId: String) = repository.getDispensacionesByPaciente(pacienteId)

    fun loadDispensacion(dispensacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getDispensacionById(dispensacionId)) {
                is Resource.Success -> {
                    val d = result.data!!
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tipoLente = d.tipoLente,
                            distanciaLente = d.distanciaLente,
                            materialLente = d.materialLente,
                            tratamientos = d.tratamientos,
                            colorLente = d.colorLente,
                            notasDiseno = d.notasDiseno,
                            origenMontura = d.origenMontura,
                            tipoAro = d.tipoAro,
                            materialMontura = d.materialMontura,
                            descripcionMontura = d.descripcionMontura,
                            tipoMontura = d.tipoMontura,
                            montoTotal = d.montoTotal.toString(),
                            montoPagado = d.montoPagado.toString(),
                            metodoPago = d.metodoPago,
                            estadoEntrega = d.estadoEntrega,
                            fecha = d.fecha,
                            fechaVencimientoGarantia = d.fechaVencimientoGarantia
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> { }
            }
        }
    }

    fun updateUiState(update: (DispensacionUiState) -> DispensacionUiState) {
        _uiState.update(update)
    }

    fun saveDispensacion(pacienteId: String, dispensacionId: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            val disp = DispensacionOptica(
                id = dispensacionId ?: UUID.randomUUID().toString(),
                pacienteId = pacienteId,
                fecha = s.fecha,
                tipoLente = s.tipoLente,
                materialLente = s.materialLente,
                tratamientos = s.tratamientos,
                colorLente = s.colorLente,
                notasDiseno = s.notasDiseno,
                origenMontura = s.origenMontura,
                tipoAro = s.tipoAro,
                materialMontura = s.materialMontura,
                descripcionMontura = s.descripcionMontura,
                tipoMontura = s.tipoMontura,
                montoTotal = s.montoTotal.toDoubleOrNull() ?: 0.0,
                montoPagado = s.montoPagado.toDoubleOrNull() ?: 0.0,
                metodoPago = s.metodoPago,
                estadoEntrega = s.estadoEntrega,
                fechaVencimientoGarantia = s.fechaVencimientoGarantia,
                distanciaLente = if (s.tipoLente == "Monofocal") s.distanciaLente else ""
            )
            repository.insertDispensacion(disp)
            onComplete()
        }
    }
}
