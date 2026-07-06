package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.ContextoFinanciero
import com.example.optoapp.data.DispensacionFinancieraRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.venta.Venta
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class FinancieraUiState(
    val dispensacionId: String = "",
    val contexto: ContextoFinanciero? = null,
    val montoTotal: String = "",
    val pagos: List<Pago> = emptyList(),
    val pagosToDelete: List<Pago> = emptyList(),
    val estadoEntrega: String = "Pendiente",
    val fechaEntrega: LocalDate? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val saldoRestante: Double
        get() {
            val total = montoTotal.toDoubleOrNull() ?: 0.0
            val pagado = pagos.sumOf { it.monto }
            return total - pagado
        }
}

@HiltViewModel
class InformacionFinancieraViewModel @Inject constructor(
    private val repository: DispensacionFinancieraRepository,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancieraUiState())
    val uiState: StateFlow<FinancieraUiState> = _uiState.asStateFlow()

    private var initialPagoIds: Set<String> = emptySet()

    fun loadFinanciera(dispensacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, dispensacionId = dispensacionId) }

            val contexto = repository.obtenerContexto(dispensacionId)
            val pagos = repository.obtenerPagos(dispensacionId)
            initialPagoIds = pagos.map { it.id }.toSet()

            when (val result = repository.obtenerDispensacion(dispensacionId)) {
                is Resource.Success -> {
                    val d = result.data ?: return@launch
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            contexto = contexto,
                            montoTotal = d.montoTotal.toString(),
                            pagos = pagos,
                            estadoEntrega = d.estadoEntrega,
                            fechaEntrega = d.fechaEntrega
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateMontoTotal(monto: String) {
        _uiState.update { it.copy(montoTotal = monto, error = null) }
    }

    fun updateEstado(estado: String) {
        _uiState.update { s ->
            val nuevaFechaEntrega = when (estado) {
                "Entregado" -> s.fechaEntrega ?: LocalDate.now()
                else -> null
            }
            s.copy(estadoEntrega = estado, fechaEntrega = nuevaFechaEntrega, error = null)
        }
    }

    fun addPago(pago: Pago) {
        _uiState.update { it.copy(pagos = it.pagos + pago, error = null) }
    }

    fun updatePago(pago: Pago) {
        _uiState.update { s ->
            s.copy(pagos = s.pagos.map { if (it.id == pago.id) pago else it })
        }
    }

    fun removePago(pago: Pago) {
        _uiState.update { s ->
            s.copy(
                pagos = s.pagos.filter { it.id != pago.id },
                pagosToDelete = s.pagosToDelete + pago
            )
        }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            val opticaId = sessionManager.opticaId.first()
            val dispId = s.dispensacionId

            val montoTotal = s.montoTotal.toDoubleOrNull() ?: 0.0

            _uiState.update { it.copy(error = null, isLoading = true) }

            repository.actualizarMontoTotal(dispId, montoTotal, opticaId)
            repository.actualizarEstado(dispId, s.estadoEntrega, s.fechaEntrega, opticaId)

            s.pagos.forEach { pago ->
                val pagoToSave = pago.copy(dispensacionId = dispId, opticaId = opticaId)
                if (pago.id in initialPagoIds) {
                    repository.editarPago(pagoToSave)
                } else {
                    repository.agregarPago(pagoToSave)
                }
            }

            s.pagosToDelete.forEach { pago ->
                repository.eliminarPago(pago, opticaId)
            }

            val montoPagado = s.pagos.sumOf { it.monto }
            repository.actualizarMontoPagado(dispId, montoPagado, opticaId)

            val venta = Venta(
                id = "v_disp_$dispId",
                opticaId = opticaId,
                origen = "dispensacion",
                origenId = dispId,
                pacienteId = "",
                ot = s.contexto?.ot ?: "",
                fecha = s.contexto?.fecha ?: LocalDate.now(),
                fechaEntrega = s.fechaEntrega,
                montoTotal = montoTotal,
                estado = s.estadoEntrega
            )
            repository.upsertVenta(venta)

            postSaveSyncScheduler.scheduleFinanzasSync(opticaId)

            _uiState.update { it.copy(isLoading = false, pagosToDelete = emptyList(), error = null) }
            onComplete()
        }
    }
}
