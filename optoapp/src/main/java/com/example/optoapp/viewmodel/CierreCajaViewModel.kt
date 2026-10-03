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
import com.example.optoapp.domain.NOTA_COMPENSACION_PREFIX
import com.example.optoapp.domain.OrderStatusPolicy
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.ui.screens.cierreVentaPagado
import com.example.optoapp.ui.screens.pagosEffectByDispensacion
import com.example.optoapp.ui.screens.pagosEffectByServicio
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
    val esReversion: Boolean = false,
    val etiquetaReversion: String? = null,
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
    val pagadoLedgerByDispensacion: Map<String, Double> = emptyMap(),
    val pagadoLedgerByServicio: Map<String, Double> = emptyMap(),
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
                    val missingOrigenIds = pagos
                        .filter { it.tipo.trim() == TIPO_REEMBOLSO }
                        .mapNotNull { pago -> pago.dispensacionId?.let { dispMap[it]?.reclamoOrigenId } }
                        .filter { it !in dispMap }.distinct()
                    if (missingOrigenIds.isNotEmpty()) {
                        repository.getDispensacionesByIds(missingOrigenIds, opticaId)
                            .forEach { dispMap[it.id] = it }
                    }
                    var ventasHoy = 0.0
                    var cobrosAtrasados = 0.0
                    var pagosFuturos = 0.0
                    pagos.forEach { pago ->
                        val effect = PagoEffect.signedAmount(pago.tipo, pago.monto)
                        val dispFecha = pago.dispensacionId?.let { id -> dispMap[id]?.fecha }
                        val servFecha = pago.servicioExtraId?.let { id -> servMap[id]?.fecha }
                        when {
                            dispFecha != null && dispFecha == fecha -> ventasHoy += effect
                            dispFecha != null && dispFecha < fecha -> cobrosAtrasados += effect
                            dispFecha != null && dispFecha > fecha -> {
                                Log.w(TAG, "Future-dated disp ${pago.dispensacionId} for pago ${pago.id}")
                                pagosFuturos += effect
                            }
                            servFecha != null && servFecha == fecha -> ventasHoy += effect
                            servFecha != null && servFecha < fecha -> cobrosAtrasados += effect
                            servFecha != null && servFecha > fecha -> {
                                Log.w(TAG, "Future-dated serv ${pago.servicioExtraId} for pago ${pago.id}")
                                pagosFuturos += effect
                            }
                            else -> {
                                Log.w(TAG, "Orphan pago ${pago.id}: disp=${pago.dispensacionId} serv=${pago.servicioExtraId} not resolvable")
                                ventasHoy += effect
                            }
                        }
                    }
                    val dispensacionesHoy = dispensaciones.filter {
                        it.estadoEntrega != ESTADO_ANULADO && it.estadoEntrega != ESTADO_RECLAMADA
                    }
                    val serviciosExtraHoy = servicios.filter { it.estado != ESTADO_ANULADO }
                    val totalDispensacionesHoy = dispensacionesHoy.sumOf { it.montoTotal }
                    val totalServiciosExtra = serviciosExtraHoy.sumOf { it.montoTotal }
                    val totalGeneral = totalDispensacionesHoy + totalServiciosExtra
                    val pagadoLedgerByDispensacion = pagosEffectByDispensacion(pagos)
                    val pagadoLedgerByServicio = pagosEffectByServicio(pagos)
                    val saldoPendiente = dispensacionesHoy.sumOf {
                        it.montoTotal - cierreVentaPagado(it.montoPagado, it.id, pagadoLedgerByDispensacion)
                    } + serviciosExtraHoy.sumOf {
                        it.montoTotal - cierreVentaPagado(it.aCuenta, it.id, pagadoLedgerByServicio)
                    }
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
                        pagadoLedgerByDispensacion = pagadoLedgerByDispensacion,
                        pagadoLedgerByServicio = pagadoLedgerByServicio,
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
                            errorMessage = "Error al cargar datos. Intenta de nuevo.",
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
        return _uiState.value.pagos.groupBy {
            if (it.metodoPago == defaultLabel) "" else it.metodoPago
        }.mapValues { entry -> entry.value.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
    }

    fun getCobradoHoy(): Double =
        _uiState.value.pagos.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }

    companion object {
        private const val TAG = "CierreCajaVM"
        private const val ESTADO_ANULADO = "Anulado"
        private const val ESTADO_RECLAMADA = "Reclamada"
        private const val TIPO_ABONO = "Abono"
        private const val TIPO_REVERSO = "Reverso"
        private const val TIPO_REEMBOLSO = "Reembolso"
        private const val ETIQUETA_PAGO_ANULADO = "Pago anulado"
        private const val SIN_MOTIVO = "Sin motivo registrado"

        private fun etiqueta(estado: String, motivo: String?): String =
            "$estado · ${motivo?.takeIf { it.isNotBlank() } ?: SIN_MOTIVO}"

        private fun estadoTerminalLabel(estado: String, esServicio: Boolean): String = when {
            esServicio -> ESTADO_ANULADO
            estado == ESTADO_ANULADO -> "Anulada"
            else -> ESTADO_RECLAMADA
        }

        /**
         * First match wins: terminal parent (from its cancellation day on, so lines of days before
         * the cancellation keep their original label), then claim refund on a replacement (reason of
         * the original), then a Reverso on an active parent (single deleted pago).
         */
        internal fun etiquetaReversion(
            pago: Pago,
            disp: DispensacionOptica?,
            serv: ServicioExtra?,
            dispMap: Map<String, DispensacionOptica>,
        ): String? {
            val tipo = pago.tipo.trim()
            val esCompensacion = tipo == TIPO_ABONO && pago.nota.startsWith(NOTA_COMPENSACION_PREFIX)
            if (tipo != TIPO_REVERSO && tipo != TIPO_REEMBOLSO && !esCompensacion) return null
            val estado = (disp?.estadoEntrega ?: serv?.estado)?.trim()
            val fechaAnulacion = if (disp != null) disp.fechaAnulacion else serv?.fechaAnulacion
            if (estado != null && OrderStatusPolicy.isTerminal(estado) &&
                (fechaAnulacion == null || pago.fecha >= fechaAnulacion)
            ) {
                val motivo = if (disp != null) disp.motivoAnulacion else serv?.motivoAnulacion
                return etiqueta(estadoTerminalLabel(estado, esServicio = disp == null), motivo)
            }
            val reclamoOrigenId = disp?.reclamoOrigenId
            return when {
                esCompensacion -> null
                tipo == TIPO_REEMBOLSO && reclamoOrigenId != null ->
                    etiqueta(ESTADO_RECLAMADA, dispMap[reclamoOrigenId]?.motivoAnulacion)
                tipo == TIPO_REVERSO && estado != null -> ETIQUETA_PAGO_ANULADO
                else -> null
            }
        }

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
                serv != null -> Triple(
                    serv.descripcion.take(32),
                    "Servicio Extra",
                    serv.pacienteId?.takeIf { it.isNotBlank() },
                )
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
                esReversion = pago.tipo.trim().let { it == TIPO_REVERSO || it == TIPO_REEMBOLSO },
                etiquetaReversion = etiquetaReversion(pago, disp, serv, dispMap),
            )
        }
    }
}
