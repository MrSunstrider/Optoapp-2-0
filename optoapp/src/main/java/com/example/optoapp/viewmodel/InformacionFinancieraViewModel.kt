package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.ContextoFinanciero
import com.example.optoapp.data.DispensacionFinancieraRepository
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
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
    val regalos: List<RegaloDispensacionUi> = emptyList(),
    val estadoEntrega: String = "Pendiente",
    val fechaEntrega: LocalDate? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
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
    private val optoRepository: OptoRepository,
    private val stockHelper: DispensacionStockHelper,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancieraUiState())
    val uiState: StateFlow<FinancieraUiState> = _uiState.asStateFlow()

    private val _monturasActivas = MutableStateFlow<List<Montura>>(emptyList())
    val monturasActivas: StateFlow<List<Montura>> = _monturasActivas.asStateFlow()

    private var initialPagoIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            sessionManager.opticaId.collect { opticaId ->
                optoRepository.getMonturasByOptica(opticaId).collect { items ->
                    _monturasActivas.value = items.filter { it.activo }
                }
            }
        }
    }

    fun loadFinanciera(dispensacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, dispensacionId = dispensacionId) }

            val contexto = repository.obtenerContexto(dispensacionId)
            val pagos = repository.obtenerPagos(dispensacionId).filter { it.tipo != "Anulación" }
            initialPagoIds = pagos.map { it.id }.toSet()

            when (val result = repository.obtenerDispensacion(dispensacionId)) {
                is Resource.Success -> {
                    val d = result.data ?: return@launch
                    val loadedRegalos = optoRepository.getRegalosByDispensacionId(dispensacionId)
                    val regalosUi = loadedRegalos.map { entity ->
                        RegaloDispensacionUi(
                            id = entity.id,
                            productoId = entity.productoId,
                            descripcion = entity.descripcion,
                            cantidad = entity.cantidad,
                            costoUnitario = entity.costoUnitario,
                            motivo = entity.motivo,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            contexto = contexto,
                            montoTotal = d.montoTotal.toString(),
                            pagos = pagos,
                            regalos = regalosUi,
                            estadoEntrega = d.estadoEntrega,
                            fechaEntrega = d.fechaEntrega,
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

    fun updateFechaEntrega(fecha: LocalDate?) {
        _uiState.update { it.copy(fechaEntrega = fecha, error = null) }
    }

    fun addRegalo(regalo: RegaloDispensacionUi) {
        _uiState.update { it.copy(regalos = it.regalos + regalo, error = null) }
    }

    fun removeRegalo(index: Int) {
        _uiState.update { s ->
            val updated = s.regalos.toMutableList()
            if (index in updated.indices) updated.removeAt(index)
            s.copy(regalos = updated, error = null)
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
                pagosToDelete = s.pagosToDelete + pago,
            )
        }
    }

    fun save(onComplete: () -> Unit) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val s = _uiState.value
                val opticaId = sessionManager.opticaId.first()
                val dispId = s.dispensacionId

                val montoTotal = s.montoTotal.toDoubleOrNull() ?: 0.0
                val existingRegalos = optoRepository.getRegalosByDispensacionId(dispId)

                repository.withTransaction {
                    repository.actualizarMontoTotal(dispId, montoTotal, opticaId)
                    repository.actualizarEstado(dispId, s.estadoEntrega, s.fechaEntrega, opticaId)

                    s.pagos.forEach { pago ->
                        val pagoToSave = pago.copy(dispensacionId = dispId, opticaId = opticaId, ventaId = "v_disp_$dispId")
                        if (pago.id in initialPagoIds) {
                            repository.editarPago(pagoToSave)
                        } else {
                            repository.agregarPago(pagoToSave)
                        }
                    }

                    s.pagosToDelete.forEach { pago ->
                        repository.eliminarPago(pago, opticaId)
                    }

                    val montoPagado = s.pagos.filter { it.tipo != "Anulación" }.sumOf { it.monto }
                    repository.actualizarMontoPagado(dispId, montoPagado, opticaId)

                    existingRegalos.forEach { regalo ->
                        stockHelper.adjustStockAndRegistrarMovimiento(
                            regalo.productoId,
                            opticaId,
                            regalo.cantidad,
                            "AJUSTE",
                            dispId,
                            "Reversión por edición de regalos",
                        )
                    }
                    optoRepository.deleteRegalosByDispensacionId(dispId, opticaId)
                    s.regalos.forEach { regaloUi ->
                        val entity = RegaloDispensacionEntity(
                            id = regaloUi.id,
                            dispensacionId = dispId,
                            productoId = regaloUi.productoId,
                            cantidad = regaloUi.cantidad,
                            costoUnitario = regaloUi.costoUnitario,
                            descripcion = regaloUi.descripcion,
                            motivo = regaloUi.motivo,
                            opticaId = opticaId,
                        )
                        optoRepository.insertRegalo(entity)
                        if (regaloUi.productoId.isNotBlank()) {
                            val stockResult = stockHelper.adjustStockAndRegistrarMovimiento(
                                regaloUi.productoId,
                                opticaId,
                                -regaloUi.cantidad,
                                "SALIDA_VENTA",
                                dispId,
                                "Salida por regalo de dispensación",
                            )
                            if (stockResult.isFailure) {
                                throw RuntimeException("Stock insuficiente para regalo: ${regaloUi.descripcion}")
                            }
                        }
                    }
                }

                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
                if (existingRegalos.isNotEmpty() || s.regalos.any { it.productoId.isNotBlank() }) {
                    postSaveSyncScheduler.scheduleInventarioSync(opticaId)
                }

                _uiState.update { it.copy(isLoading = false, pagosToDelete = emptyList(), error = null) }
                onComplete()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Error al guardar: ${e.localizedMessage ?: "Error desconocido"}")
                }
            }
        }
    }
}
