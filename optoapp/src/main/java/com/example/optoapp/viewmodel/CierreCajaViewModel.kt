package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

data class PagoDisplayItem(
    val pago: Pago,
    val label: String,
    val tipoEntidad: String,
    val esCobroAtrasado: Boolean,
    val dispensacionId: String?,
    val servicioExtraId: String?,
    val pacienteId: String?,
)

data class CierreCajaUiState(
    val fecha: LocalDate = DateUtils.today(),
    val pagos: List<Pago> = emptyList(),
    val pagosDisplay: List<PagoDisplayItem> = emptyList(),
    val totalDispensacionesHoy: Double = 0.0,
    val dispensacionesHoy: List<DispensacionOptica> = emptyList(),
    val serviciosExtraHoy: List<ServicioExtra> = emptyList(),
    val totalServiciosExtra: Double = 0.0,
    val totalGeneral: Double = 0.0,
    val ventasHoy: Double = 0.0,
    val cobrosAtrasados: Double = 0.0,
    val saldoPendiente: Double = 0.0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val pagosFuturos: Double = 0.0,
    val pacienteNombres: Map<String, String> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CierreCajaViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CierreCajaUiState())
    val uiState: StateFlow<CierreCajaUiState> = _uiState.asStateFlow()

    init {
        observePagos()
    }

    fun setFecha(fecha: LocalDate) {
        _uiState.update {
            CierreCajaUiState(fecha = fecha, isLoading = true)
        }
    }

    private fun observePagos() {
        combine(
            _uiState.map { it.fecha }.distinctUntilChanged(),
            sessionManager.opticaId,
            sessionManager.opticaRol,
        ) { fecha, opticaId, rol -> Triple(fecha, opticaId, rol) }
            .distinctUntilChanged()
            .flatMapLatest { (fecha, opticaId, rol) ->
                if (!AppRoles.canViewCierreCaja(rol)) {
                    _uiState.update {
                        CierreCajaUiState(fecha = fecha, isLoading = false)
                    }
                    return@flatMapLatest flowOf()
                }
                combine(
                    repository.getPagosByDateRangeForOptica(fecha, fecha, opticaId),
                    repository.getDispensacionesByDateRangeForOptica(fecha, fecha, opticaId),
                    repository.getServiciosByDateRangeForOptica(fecha, fecha, opticaId),
                    repository.pacientesFlowForOptica(opticaId).map { pacientes ->
                        pacientes.associate { it.id to it.nombreCompleto }
                    },
                ) { pagos, dispensaciones, servicios, pacienteNombres ->
                    val dispMap = dispensaciones.associateBy { it.id }.toMutableMap()
                    val servMap = servicios.associateBy { it.id }.toMutableMap()
                    val missingDispIds = pagos.mapNotNull { it.dispensacionId }
                        .filter { it !in dispMap }.distinct()
                    val missingServIds = pagos.mapNotNull { it.servicioExtraId }
                        .filter { it !in servMap }.distinct()
                    if (missingDispIds.isNotEmpty()) {
                        repository.getDispensacionesByIds(missingDispIds, opticaId)
                            .forEach { dispMap[it.id] = it }
                    }
                    if (missingServIds.isNotEmpty()) {
                        repository.getServiciosByIds(missingServIds, opticaId)
                            .forEach { servMap[it.id] = it }
                    }
                    var ventasHoy = 0.0
                    var cobrosAtrasados = 0.0
                    var pagosFuturos = 0.0
                    pagos.filter { it.tipo != TIPO_ANULACION }.forEach { pago ->
                        val dispFecha = pago.dispensacionId?.let { id -> dispMap[id]?.fecha }
                        val servFecha = pago.servicioExtraId?.let { id -> servMap[id]?.fecha }
                        when {
                            dispFecha != null && dispFecha == fecha -> ventasHoy += pago.monto
                            dispFecha != null && dispFecha < fecha -> cobrosAtrasados += pago.monto
                            dispFecha != null && dispFecha > fecha -> {
                                Log.w(TAG, "Future-dated disp ${pago.dispensacionId} for pago ${pago.id}")
                                pagosFuturos += pago.monto
                            }
                            servFecha != null && servFecha == fecha -> ventasHoy += pago.monto
                            servFecha != null && servFecha < fecha -> cobrosAtrasados += pago.monto
                            servFecha != null && servFecha > fecha -> {
                                Log.w(TAG, "Future-dated serv ${pago.servicioExtraId} for pago ${pago.id}")
                                pagosFuturos += pago.monto
                            }
                            else -> {
                                Log.w(TAG, "Orphan pago ${pago.id}: disp=${pago.dispensacionId} serv=${pago.servicioExtraId} not resolvable")
                                ventasHoy += pago.monto
                            }
                        }
                    }
                    val dispensacionesHoy = dispensaciones.filter { it.estadoEntrega != ESTADO_ANULADO }
                    val serviciosExtraHoy = servicios.filter { it.estado != ESTADO_ANULADO }
                    val totalDispensacionesHoy = dispensacionesHoy.sumOf { it.montoTotal }
                    val totalServiciosExtra = serviciosExtraHoy.sumOf { it.montoTotal }
                    val totalGeneral = totalDispensacionesHoy + totalServiciosExtra
                    val saldoPendiente = dispensacionesHoy.sumOf { it.montoTotal - it.montoPagado } +
                        serviciosExtraHoy.sumOf { it.montoTotal - it.aCuenta }
                    val pagosDisplay = buildPagosDisplay(pagos, dispMap, servMap, fecha)
                    CierreCajaUiState(
                        fecha = fecha,
                        pagos = pagos,
                        pagosDisplay = pagosDisplay,
                        totalDispensacionesHoy = totalDispensacionesHoy,
                        dispensacionesHoy = dispensacionesHoy,
                        serviciosExtraHoy = serviciosExtraHoy,
                        totalServiciosExtra = totalServiciosExtra,
                        totalGeneral = totalGeneral,
                        ventasHoy = ventasHoy,
                        cobrosAtrasados = cobrosAtrasados,
                        saldoPendiente = saldoPendiente,
                        isLoading = false,
                        pagosFuturos = pagosFuturos,
                        pacienteNombres = pacienteNombres,
                    )
                }.catch { e ->
                    Log.e(TAG, "observePagos inner flow failed", e)
                    emit(
                        CierreCajaUiState(
                            fecha = fecha,
                            isLoading = false,
                            errorMessage = "Error al cargar datos: ${e.message}",
                        ),
                    )
                }
            }
            .onEach { state ->
                _uiState.value = state
            }
            .launchIn(viewModelScope)
    }

    fun getTotalesPorMetodo(): Map<String, Double> {
        val defaultLabel = FinanzasRemoteDefaults.ServicioExtra.METODO_PAGO_ROW
        return pagosExcludingAnulacion(_uiState.value.pagos).groupBy {
            if (it.metodoPago == defaultLabel) "" else it.metodoPago
        }.mapValues { entry -> entry.value.sumOf { it.monto } }
    }

    fun getCobradoHoy(): Double =
        pagosExcludingAnulacion(_uiState.value.pagos).sumOf { it.monto }

    companion object {
        private const val TAG = "CierreCajaVM"
        private const val ESTADO_ANULADO = "Anulado"
        private const val TIPO_ANULACION = "Anulación"

        internal fun pagosExcludingAnulacion(pagos: List<Pago>): List<Pago> =
            pagos.filter { it.tipo != TIPO_ANULACION }

        internal fun buildPagosDisplay(
            pagos: List<Pago>,
            dispMap: Map<String, DispensacionOptica>,
            servMap: Map<String, ServicioExtra>,
            fecha: LocalDate,
        ): List<PagoDisplayItem> = pagos.map { pago ->
            val disp = pago.dispensacionId?.let { dispMap[it] }
            val serv = pago.servicioExtraId?.let { servMap[it] }
            val dispFecha = disp?.fecha
            val servFecha = serv?.fecha
            val esCobroAtrasado = when {
                dispFecha != null -> dispFecha < fecha
                servFecha != null -> servFecha < fecha
                else -> false
            }
            val (label, tipoEntidad, pacienteId) = when {
                disp != null -> {
                    val otLabel = if (disp.ot.isNotBlank()) "OT ${disp.ot}" else "Dispensación ${disp.id.take(8)}"
                    Triple(otLabel, "Dispensación", disp.pacienteId.takeIf { it.isNotBlank() })
                }
                serv != null -> Triple(serv.descripcion.take(32), "Servicio Extra", null)
                else -> Triple("Pago", "Pago", null)
            }
            PagoDisplayItem(
                pago = pago,
                label = label,
                tipoEntidad = tipoEntidad,
                esCobroAtrasado = esCobroAtrasado,
                dispensacionId = pago.dispensacionId,
                servicioExtraId = pago.servicioExtraId,
                pacienteId = pacienteId,
            )
        }
    }
}
