package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.optoapp.util.DateUtils
import javax.inject.Inject

data class CierreCajaUiState(
    val fecha: LocalDate = DateUtils.today(),
    val pagos: List<Pago> = emptyList(),
    val totalVentasHoy: Double = 0.0,
    val dispensacionesHoy: List<DispensacionOptica> = emptyList(),
    val serviciosExtraHoy: List<ServicioExtra> = emptyList(),
    val totalServiciosExtra: Double = 0.0,
    val totalGeneral: Double = 0.0,
    val ventasHoy: Double = 0.0,
    val cobrosAtrasados: Double = 0.0,
    val saldoPendiente: Double = 0.0,
    val isLoading: Boolean = true,
    val dispOtMap: Map<String, String> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CierreCajaViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CierreCajaUiState())
    val uiState: StateFlow<CierreCajaUiState> = _uiState.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    init {
        // userTimeZone collector removed: updating _uiState.fecha mid-flow races with
        // observePagos() cancelling the inner flatMapLatest on every zone change.
        observePagos()
    }

    fun setFecha(fecha: LocalDate) {
        _uiState.update { it.copy(fecha = fecha) }
    }

    private fun observePagos() {
        combine(
            _uiState.map { it.fecha }.distinctUntilChanged(),
            sessionManager.opticaId,
            _refreshTrigger
        ) { fecha, opticaId, trigger -> Triple(fecha, opticaId, trigger) }
            .distinctUntilChanged()
            .flatMapLatest { (fecha, opticaId, _) ->
                combine(
                    repository.getPagosByDateRangeForOptica(fecha, fecha, opticaId),
                    repository.getAllDispensacionesForOptica(opticaId),
                    repository.getAllServiciosForOptica(opticaId)
                ) { pagos, dispensaciones, servicios ->
                    val dispMap = dispensaciones.associateBy { it.id }
                    val servMap = servicios.associateBy { it.id }
                    var ventasHoy = 0.0
                    var cobrosAtrasados = 0.0
                    pagos.forEach { pago ->
                        val dispFecha = pago.dispensacionId?.let { id -> dispMap[id]?.fecha }
                        val servFecha = pago.servicioExtraId?.let { id -> servMap[id]?.fecha }
                        when {
                            dispFecha != null && dispFecha == fecha -> ventasHoy += pago.monto
                            dispFecha != null && dispFecha < fecha -> cobrosAtrasados += pago.monto
                            servFecha != null && servFecha == fecha -> ventasHoy += pago.monto
                            servFecha != null && servFecha < fecha -> cobrosAtrasados += pago.monto
                            else -> ventasHoy += pago.monto
                        }
                    }
                    val pagosPorDispId = pagos.mapNotNull { it.dispensacionId }.toSet()
                    val pagosPorServId = pagos.mapNotNull { it.servicioExtraId }.toSet()
                    val dispensacionesHoy = dispensaciones.filter {
                        it.fecha == fecha || it.fechaEntrega == fecha || it.id in pagosPorDispId
                    }
                    val serviciosExtraHoy = servicios.filter {
                        it.fecha == fecha || it.fechaEntrega == fecha || it.id in pagosPorServId
                    }
                    val totalDispensacionesHoy = dispensacionesHoy.sumOf { it.montoTotal }
                    val totalServiciosExtra = serviciosExtraHoy.sumOf { it.montoTotal }
                    val totalGeneral = totalDispensacionesHoy + totalServiciosExtra
                    val saldoPendiente = totalGeneral - ventasHoy
                    val dispOtMap = dispMap.mapValues { it.value.ot }
                    CierreCajaUiState(
                        fecha = fecha,
                        pagos = pagos,
                        totalVentasHoy = totalDispensacionesHoy,
                        dispensacionesHoy = dispensacionesHoy,
                        serviciosExtraHoy = serviciosExtraHoy,
                        totalServiciosExtra = totalServiciosExtra,
                        totalGeneral = totalGeneral,
                        ventasHoy = ventasHoy,
                        cobrosAtrasados = cobrosAtrasados,
                        saldoPendiente = saldoPendiente,
                        isLoading = false,
                        dispOtMap = dispOtMap
                    )
                }
            }
            .onEach { state ->
                _uiState.value = state
            }.launchIn(viewModelScope)
    }

    fun getTotalesPorMetodo(): Map<String, Double> {
        return _uiState.value.pagos.groupBy {
            if (it.metodoPago == "Sin especificar") "" else it.metodoPago
        }.mapValues { entry -> entry.value.sumOf { it.monto } }
    }

}
