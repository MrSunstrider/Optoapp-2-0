package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.optoapp.util.DateUtils
import javax.inject.Inject

data class CierreCajaUiState(
    val fecha: LocalDate = DateUtils.today(),
    val pagos: List<Pago> = emptyList(),
    val totalVentasHoy: Double = 0.0,
    val ventasHoy: Double = 0.0,
    val cobrosAtrasados: Double = 0.0,
    val saldoPendiente: Double = 0.0,
    val isLoading: Boolean = false
)

/** Resultado interno para combinar pagos + ventas + saldo pendiente. */
private data class CierreCajaResult(
    val pagos: List<Pago>,
    val totalVentasHoy: Double,
    val ventasHoy: Double,
    val cobrosAtrasados: Double,
    val saldoPendiente: Double
)

@HiltViewModel
class CierreCajaViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CierreCajaUiState())
    val uiState: StateFlow<CierreCajaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.userTimeZone.collectLatest { _ ->
                _uiState.update { it.copy(fecha = DateUtils.today()) }
            }
        }
        observePagos()
    }

    fun setFecha(fecha: LocalDate) {
        _uiState.update { it.copy(fecha = fecha) }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observePagos() {
        kotlinx.coroutines.flow.combine(
            _uiState.map { it.fecha }.distinctUntilChanged(),
            sessionManager.opticaId
        ) { fecha, opticaId -> fecha to opticaId }
            .distinctUntilChanged()
            .flatMapLatest { (fecha, opticaId) ->
                combine(
                    repository.getPagosByDateRangeForOptica(fecha, fecha, opticaId),
                    repository.getAllDispensacionesForOptica(opticaId)
                ) { pagos: List<Pago>, dispensaciones: List<DispensacionOptica> ->
                    val dispMap = dispensaciones.associateBy { it.id }
                    var ventasHoy = 0.0
                    var cobrosAtrasados = 0.0
                    pagos.forEach { pago ->
                        val dispFecha = pago.dispensacionId?.let { id -> dispMap[id]?.fecha }
                        when {
                            dispFecha == fecha -> ventasHoy += pago.monto
                            dispFecha != null && dispFecha < fecha -> cobrosAtrasados += pago.monto
                            else -> ventasHoy += pago.monto
                        }
                    }
                    // Total de ventas registradas en esta fecha (monto completo de la dispensación)
                    val ventasDelDia = dispensaciones.filter { it.fecha == fecha }
                    val totalVentasHoy = ventasDelDia.sumOf { it.montoTotal }
                    val saldoPendiente = totalVentasHoy - ventasHoy
                    CierreCajaResult(pagos, totalVentasHoy, ventasHoy, cobrosAtrasados, saldoPendiente)
                }
            }
            .onEach { (pagos, totalVentasHoy, ventasHoy, cobrosAtrasados, saldoPendiente) ->
                _uiState.update { it.copy(
                    pagos = pagos,
                    totalVentasHoy = totalVentasHoy,
                    ventasHoy = ventasHoy,
                    cobrosAtrasados = cobrosAtrasados,
                    saldoPendiente = saldoPendiente,
                    isLoading = false
                )}
            }.launchIn(viewModelScope)
    }
    
    // Helper to get totals by method
    fun getTotalesPorMetodo(): Map<String, Double> {
        return _uiState.value.pagos.groupBy { it.metodoPago }
            .mapValues { entry -> entry.value.sumOf { it.monto } }
    }
}
