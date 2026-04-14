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
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import com.example.optoapp.data.Pago
import java.time.LocalDate
import com.example.optoapp.util.DateUtils

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
    val estadoEntrega: String = "Pendiente",
    val fecha: LocalDate = DateUtils.today(),
    val fechaVencimientoGarantia: LocalDate? = null,
    
    val subTipoBifocal: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    
    val pagos: List<Pago> = emptyList(),
    val pagosToDelete: List<Pago> = emptyList(),
    val generatedId: String = UUID.randomUUID().toString()
)

@HiltViewModel
class DispensacionViewModel @Inject constructor(
    private val repository: com.example.optoapp.data.OptoRepository,
    private val sessionManager: com.example.optoapp.data.SessionManager,
    private val syncFinanzasUseCase: com.example.optoapp.domain.SyncFinanzasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DispensacionUiState())
    val uiState: StateFlow<DispensacionUiState> = _uiState.asStateFlow()

    fun getDispensacionesByPaciente(pacienteId: String) = repository.getDispensacionesByPaciente(pacienteId)

    fun loadDispensacion(dispensacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generatedId = dispensacionId) }
            when (val result = repository.getDispensacionById(dispensacionId)) {
                is Resource.Success -> {
                    val d = result.data!!
                    val loadedPagos = repository.getPagosByDispensacion(dispensacionId).first()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tipoLente = d.tipoLente,
                            distanciaLente = d.distanciaLente,
                            materialLente = d.materialLente,
                            tratamientos = d.tratamientos,
                            colorLente = d.colorLente,
                            notasDiseno = d.notasDiseno,
                            subTipoBifocal = d.subTipoBifocal,
                            origenMontura = d.origenMontura,
                            tipoAro = d.tipoAro,
                            materialMontura = d.materialMontura,
                            descripcionMontura = d.descripcionMontura,
                            tipoMontura = d.tipoMontura,
                            montoTotal = d.montoTotal.toString(),
                            estadoEntrega = d.estadoEntrega,
                            fecha = d.fecha,
                            fechaVencimientoGarantia = d.fechaVencimientoGarantia,
                            pagos = loadedPagos
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

    fun updateUiState(update: (DispensacionUiState) -> DispensacionUiState) {
        _uiState.update(update)
    }

    fun saveDispensacion(pacienteId: String, dispensacionId: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            val currentOpticaId = sessionManager.opticaId.first()
            val finalId = dispensacionId ?: s.generatedId
            val finalMontoPagado = s.pagos.sumOf { it.monto }

            val disp = DispensacionOptica(
                id = finalId,
                pacienteId = pacienteId,
                fecha = s.fecha,
                opticaId = currentOpticaId,
                tipoLente = s.tipoLente,
                materialLente = s.materialLente,
                tratamientos = s.tratamientos,
                colorLente = s.colorLente,
                notasDiseno = s.notasDiseno,
                subTipoBifocal = if (s.tipoLente == "Bifocal") s.subTipoBifocal else "",
                origenMontura = s.origenMontura,
                tipoAro = s.tipoAro,
                materialMontura = s.materialMontura,
                descripcionMontura = s.descripcionMontura,
                tipoMontura = s.tipoMontura,
                montoTotal = s.montoTotal.toDoubleOrNull() ?: 0.0,
                montoPagado = finalMontoPagado,
                metodoPago = "",
                estadoEntrega = s.estadoEntrega,
                fechaVencimientoGarantia = s.fechaVencimientoGarantia,
                distanciaLente = if (s.tipoLente == "Monofocal") s.distanciaLente else ""
            )
            if (dispensacionId != null && dispensacionId != "null") {
                repository.updateDispensacion(disp)
            } else {
                repository.insertDispensacion(disp)
            }

            // Guardar pagos vinculados a esta dispensación
            s.pagos.forEach { pago ->
                val pagoToSave = pago.copy(dispensacionId = finalId, opticaId = currentOpticaId)
                repository.insertPago(pagoToSave)
            }

            // Eliminar pagos marcados
            s.pagosToDelete.forEach { pago ->
                repository.deletePago(pago)
            }

            // Silent Sync en segundo plano
            viewModelScope.launch { 
                try {
                    syncFinanzasUseCase(currentOpticaId)
                } catch (e: Exception) {}
            }
            
            onComplete()
        }
    }
}
