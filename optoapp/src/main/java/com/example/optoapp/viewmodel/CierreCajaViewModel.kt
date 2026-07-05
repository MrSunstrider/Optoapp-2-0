package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.arqueo.ArqueoCaja
import com.example.optoapp.data.venta.Venta
import com.example.optoapp.data.venta.VentaDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
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
    val serviciosExtraHoy: List<Venta> = emptyList(),
    val dispensacionesHoy: List<Venta> = emptyList(),
    val totalServiciosExtra: Double = 0.0,
    val totalGeneral: Double = 0.0,
    val ventasHoy: Double = 0.0,
    val cobrosAtrasados: Double = 0.0,
    val saldoPendiente: Double = 0.0,
    val isLoading: Boolean = false,
    val arqueoForFecha: ArqueoCaja? = null,
    val dispOtMap: Map<String, String> = emptyMap()
)

private data class CierreCajaResult(
    val pagos: List<Pago>,
    val totalVentasHoy: Double,
    val serviciosExtraHoy: List<Venta>,
    val dispensacionesHoy: List<Venta>,
    val totalServiciosExtra: Double,
    val totalGeneral: Double,
    val ventasHoy: Double,
    val cobrosAtrasados: Double,
    val saldoPendiente: Double,
    val dispOtMap: Map<String, String>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CierreCajaViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val ventaDao: VentaDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CierreCajaUiState())
    val uiState: StateFlow<CierreCajaUiState> = _uiState.asStateFlow()


    private val _arqueoKey = MutableStateFlow<Pair<LocalDate, String>?>(null)

    init {
        viewModelScope.launch {
            sessionManager.userTimeZone.collect { _ ->
                _uiState.update { it.copy(fecha = DateUtils.today()) }
            }
        }
        observePagos()
        observeArqueo()
    }

    private fun observeArqueo() {
        _arqueoKey
            .filterNotNull()
            .flatMapLatest { (fecha, opticaId) ->
                repository.getArqueoByFecha(fecha, opticaId)
            }
            .onEach { arqueo ->
                _uiState.update { it.copy(arqueoForFecha = arqueo) }
            }
            .launchIn(viewModelScope)
    }

    fun setFecha(fecha: LocalDate) {
        _uiState.update { it.copy(fecha = fecha) }
    }

    private fun observePagos() {
        combine(
            _uiState.map { it.fecha }.distinctUntilChanged(),
            sessionManager.opticaId
        ) { fecha, opticaId -> fecha to opticaId }
            .distinctUntilChanged()
            .flatMapLatest { (fecha, opticaId) ->
                combine(
                    repository.getPagosByDateRangeForOptica(fecha, fecha, opticaId),
                    repository.getAllDispensacionesForOptica(opticaId),
                    repository.getAllServiciosForOptica(opticaId),
                    ventaDao.getVentasByOpticaAndDateRange(opticaId, fecha, fecha)
                ) { pagos: List<Pago>, dispensaciones: List<DispensacionOptica>, servicios: List<ServicioExtra>, ventas: List<Venta> ->
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
                    val ventasDelDia = ventas.filter { it.fecha == fecha }
                    val resolvedVentas = ventasDelDia.map { v ->
                        if (v.ot.isBlank() && v.origen == "dispensacion") {
                            val resolvedOt = dispMap[v.origenId]?.ot ?: ""
                            v.copy(ot = resolvedOt)
                        } else v
                    }
                    val totalVentasHoy = resolvedVentas.filter { it.origen == "dispensacion" }.sumOf { it.montoTotal }
                    val serviciosExtraHoy = resolvedVentas.filter { it.origen == "servicio_extra" }
                    val totalServiciosExtra = serviciosExtraHoy.sumOf { it.montoTotal }
                    val dispensacionesHoy = resolvedVentas.filter { it.origen == "dispensacion" }
                    val totalGeneral = totalVentasHoy + totalServiciosExtra
                    val saldoPendiente = totalGeneral - ventasHoy
                    val dispOtMap = dispMap.mapValues { it.value.ot }
                    CierreCajaResult(pagos, totalVentasHoy, serviciosExtraHoy, dispensacionesHoy, totalServiciosExtra, totalGeneral, ventasHoy, cobrosAtrasados, saldoPendiente, dispOtMap)
                }
            }
            .onEach { (pagos, totalVentasHoy, serviciosExtraHoy, dispensacionesHoy, totalServiciosExtra, totalGeneral, ventasHoy, cobrosAtrasados, saldoPendiente, dispOtMap) ->
                _uiState.update {
                    it.copy(
                        pagos = pagos,
                        totalVentasHoy = totalVentasHoy,
                        serviciosExtraHoy = serviciosExtraHoy,
                        dispensacionesHoy = dispensacionesHoy,
                        totalServiciosExtra = totalServiciosExtra,
                        totalGeneral = totalGeneral,
                        ventasHoy = ventasHoy,
                        cobrosAtrasados = cobrosAtrasados,
                        saldoPendiente = saldoPendiente,
                        dispOtMap = dispOtMap,
                        isLoading = false
                    )
                }
            }.launchIn(viewModelScope)
    }


    fun getTotalesPorMetodo(): Map<String, Double> {
        return _uiState.value.pagos.groupBy {
            if (it.metodoPago == "Sin especificar") "" else it.metodoPago
        }.mapValues { entry -> entry.value.sumOf { it.monto } }
    }

    fun loadArqueoForDate(fecha: LocalDate, opticaId: String): Flow<ArqueoCaja?> =
        repository.getArqueoByFecha(fecha, opticaId)

    fun observeArqueoForDate(fecha: LocalDate, opticaId: String) {
        _arqueoKey.value = fecha to opticaId
    }
}
