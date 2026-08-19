package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.ContextoFinanciero
import com.example.optoapp.data.DispensacionFinancieraRepository
import com.example.optoapp.data.Montura
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.domain.CalcularMontoPagadoUseCase
import com.example.optoapp.domain.estadoAfterFechaEntrega
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.domain.movimientoReferenciaForRegalo
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
    val loadFailed: Boolean = false,
    val error: String? = null,
) {
    val saldoRestante: Double
        get() {
            val total = montoTotal.toDoubleOrNull() ?: 0.0
            val pagado = pagos.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }
            return total - pagado
        }
}

@HiltViewModel
class InformacionFinancieraViewModel @Inject constructor(
    private val repository: DispensacionFinancieraRepository,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val calcularMontoPagadoUseCase: CalcularMontoPagadoUseCase,
    private val stockHelper: DispensacionStockHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancieraUiState())
    val uiState: StateFlow<FinancieraUiState> = _uiState.asStateFlow()

    private val _monturas = MutableStateFlow<List<Montura>>(emptyList())
    val monturas: StateFlow<List<Montura>> = _monturas.asStateFlow()

    private var initialPagoIds: Set<String> = emptySet()
    private var initialRegalos: List<RegaloDispensacionEntity> = emptyList()

    fun loadFinanciera(dispensacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false, dispensacionId = dispensacionId, error = null) }

            val contexto = repository.obtenerContexto(dispensacionId)
            val pagos = repository.obtenerPagos(dispensacionId)
            val regalosEntities = repository.obtenerRegalos(dispensacionId)
            val regalosUi = regalosEntities.map { it.toUi() }

            when (val result = repository.obtenerDispensacion(dispensacionId)) {
                is Resource.Success -> {
                    val d = result.data
                    if (d == null) {
                        initialPagoIds = emptySet()
                        initialRegalos = emptyList()
                        _uiState.update {
                            it.copy(isLoading = false, loadFailed = true, error = "Dispensación no encontrada")
                        }
                        return@launch
                    }
                    initialPagoIds = pagos.map { it.id }.toSet()
                    initialRegalos = regalosEntities
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadFailed = false,
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
                    initialPagoIds = emptySet()
                    initialRegalos = emptyList()
                    _uiState.update {
                        it.copy(isLoading = false, loadFailed = true, error = result.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun loadMonturas() {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            val result = repository.obtenerMonturas(opticaId)
            _monturas.value = result.filter { it.activo }
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
        _uiState.update { s ->
            s.copy(
                fechaEntrega = fecha,
                estadoEntrega = estadoAfterFechaEntrega(s.estadoEntrega, fecha),
            )
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

    fun addRegalo(regalo: RegaloDispensacionUi) {
        _uiState.update { it.copy(regalos = it.regalos + regalo, error = null) }
    }

    fun removeRegalo(index: Int) {
        _uiState.update { s ->
            s.copy(regalos = s.regalos.filterIndexed { i, _ -> i != index })
        }
    }

    fun save(onComplete: () -> Unit) {
        if (_uiState.value.isLoading || _uiState.value.loadFailed) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val s = _uiState.value
                val opticaId = sessionManager.opticaId.first()
                val dispId = s.dispensacionId

                val montoTotal = s.montoTotal.toDoubleOrNull() ?: 0.0

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

                    val montoPagado = calcularMontoPagadoUseCase(dispId)
                    repository.actualizarMontoPagado(dispId, montoPagado, opticaId)

                    initialRegalos.forEach { regalo ->
                        stockHelper.adjustStockAndRegistrarMovimiento(
                            regalo.productoId,
                            opticaId,
                            regalo.cantidad,
                            "AJUSTE",
                            movimientoReferenciaForRegalo(regalo.id),
                            "Reversión por edición de regalos",
                        )
                    }
                    repository.eliminarRegalosByDispensacionId(dispId, opticaId)
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
                        repository.insertarRegalo(entity)
                        if (regaloUi.productoId.isNotBlank()) {
                            val stockResult = stockHelper.adjustStockAndRegistrarMovimiento(
                                regaloUi.productoId,
                                opticaId,
                                -regaloUi.cantidad,
                                "SALIDA_VENTA",
                                movimientoReferenciaForRegalo(regaloUi.id),
                                "Salida por regalo de dispensación",
                            )
                            if (stockResult.isFailure) {
                                throw RuntimeException("Stock insuficiente para regalo: ${regaloUi.descripcion}")
                            }
                        }
                    }
                }

                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)

                _uiState.update { it.copy(isLoading = false, pagosToDelete = emptyList(), error = null) }
                initialPagoIds = s.pagos.map { it.id }.toSet()
                initialRegalos = s.regalos.map { it.toEntity(dispId, opticaId) }
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

private fun RegaloDispensacionEntity.toUi() = RegaloDispensacionUi(
    id = id,
    productoId = productoId,
    descripcion = descripcion,
    cantidad = cantidad,
    costoUnitario = costoUnitario,
    motivo = motivo,
)

private fun RegaloDispensacionUi.toEntity(dispensacionId: String, opticaId: String) = RegaloDispensacionEntity(
    id = id,
    dispensacionId = dispensacionId,
    productoId = productoId,
    cantidad = cantidad,
    costoUnitario = costoUnitario,
    descripcion = descripcion,
    motivo = motivo,
    opticaId = opticaId,
)
