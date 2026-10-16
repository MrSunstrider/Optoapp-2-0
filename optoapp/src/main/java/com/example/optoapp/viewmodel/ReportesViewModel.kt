package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.opticasettings.CommissionConfigJson
import com.example.optoapp.data.opticasettings.OpticaSettingsEntity
import com.example.optoapp.domain.ClinicoReporte
import com.example.optoapp.domain.CostoVentaCalculator
import com.example.optoapp.domain.EquipoReporte
import com.example.optoapp.domain.InventarioReporte
import com.example.optoapp.domain.MovimientoFinanciero
import com.example.optoapp.domain.Origen
import com.example.optoapp.domain.PacientesReporte
import com.example.optoapp.domain.PagoEffect
import com.example.optoapp.domain.PeriodComparison
import com.example.optoapp.domain.ReportesModuleStats
import com.example.optoapp.domain.TipoMovimiento
import com.example.optoapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportesViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val membershipRepository: MembershipRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.opticaId.first { it.isNotBlank() }
            delay(200)
            _isLoading.value = false
        }
    }

    private val _periodo = MutableStateFlow("Mensual")
    val periodo: StateFlow<String> = _periodo

    private val _anio = MutableStateFlow(Year.now().value.toString())
    val anio: StateFlow<String> = _anio

    private val _fechaDiario = MutableStateFlow(LocalDate.now())
    val fechaDiario: StateFlow<LocalDate> = _fechaDiario

    fun setPeriodo(p: String) {
        _periodo.value = p
    }
    fun setAnio(a: String) {
        _anio.value = a
    }
    fun setFechaDiario(fecha: LocalDate) {
        _fechaDiario.value = fecha
    }

    fun previous() {
        when (_periodo.value) {
            "Diario" -> _fechaDiario.value = _fechaDiario.value.minusDays(1)
            "Semanal" -> _fechaDiario.value = _fechaDiario.value.minusWeeks(1)
            "Mensual" -> _fechaDiario.value = _fechaDiario.value.minusMonths(1)
            "Anual" -> _anio.value = (_anio.value.toInt() - 1).toString()
        }
    }

    fun next() {
        when (_periodo.value) {
            "Diario" -> _fechaDiario.value = _fechaDiario.value.plusDays(1)
            "Semanal" -> _fechaDiario.value = _fechaDiario.value.plusWeeks(1)
            "Mensual" -> _fechaDiario.value = _fechaDiario.value.plusMonths(1)
            "Anual" -> _anio.value = (_anio.value.toInt() + 1).toString()
        }
    }

    val periodoLabel: StateFlow<String> = combine(_periodo, _fechaDiario, _anio) { p, fd, a ->
        when (p) {
            "Diario" -> DateUtils.formatLocalized(fd)
            "Semanal" -> {
                val start = fd.minusDays((fd.dayOfWeek.value - 1).toLong())
                val end = start.plusDays(6)
                "${DateUtils.formatLocalized(start)} - ${DateUtils.formatLocalized(end)}"
            }
            "Mensual" -> {
                val months = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
                "${months[fd.monthValue - 1]} ${fd.year}"
            }
            "Anual" -> a
            else -> "Total"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private fun dentroDelPeriodo(date: LocalDate, p: String, a: String, fechaDiario: LocalDate, now: LocalDate): Boolean = when (p) {
        "Diario" -> date.isEqual(fechaDiario)
        "Semanal" -> {
            val dayOfWeek = fechaDiario.dayOfWeek.value
            val startOfWeek = fechaDiario.minusDays(dayOfWeek.toLong() - 1)
            val endOfWeek = startOfWeek.plusDays(6)
            !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
        }
        "Mensual" -> date.year == fechaDiario.year && date.month == fechaDiario.month
        "Anual" -> date.year.toString() == a
        else -> true
    }

    private fun periodDateRange(p: String, a: String, fd: LocalDate, now: LocalDate): Pair<LocalDate, LocalDate> = when (p) {
        "Diario" -> fd to fd
        "Semanal" -> {
            val startOfWeek = fd.minusDays((fd.dayOfWeek.value - 1).toLong())
            startOfWeek to startOfWeek.plusDays(6)
        }
        "Mensual" -> fd.withDayOfMonth(1) to fd.withDayOfMonth(fd.lengthOfMonth())
        "Anual" -> LocalDate.of(a.toInt(), 1, 1) to LocalDate.of(a.toInt(), 12, 31)
        else -> LocalDate.MIN to LocalDate.MAX
    }

    private fun previousPeriodDateRange(p: String, a: String, fd: LocalDate, now: LocalDate): Pair<LocalDate, LocalDate>? = when (p) {
        "Diario" -> fd.minusDays(1) to fd.minusDays(1)
        "Semanal" -> {
            val startOfWeek = fd.minusDays((fd.dayOfWeek.value - 1).toLong()).minusWeeks(1)
            startOfWeek to startOfWeek.plusDays(6)
        }
        "Mensual" -> {
            val prev = fd.minusMonths(1)
            prev.withDayOfMonth(1) to prev.withDayOfMonth(prev.lengthOfMonth())
        }
        "Anual" -> {
            val y = a.toInt() - 1
            LocalDate.of(y, 1, 1) to LocalDate.of(y, 12, 31)
        }
        else -> null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allDispensaciones: StateFlow<List<DispensacionOptica>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(
                repository.getAllDispensacionesForOptica(opticaId),
                _periodo,
                _anio,
                _fechaDiario,
            ) { list, p, a, fd ->
                val now = LocalDate.now()
                list.filter { disp ->
                    disp.estadoEntrega != "Anulado" &&
                        disp.estadoEntrega != "Reclamada" &&
                        dentroDelPeriodo(disp.fecha, p, a, fd, now)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allServiciosDelPeriodo: StateFlow<List<ServicioExtra>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(
                repository.getAllServiciosForOptica(opticaId),
                _periodo,
                _anio,
                _fechaDiario,
            ) { list, p, a, fd ->
                val now = LocalDate.now()
                list.filter { servicio -> servicio.estado != "Anulado" && dentroDelPeriodo(servicio.fecha, p, a, fd, now) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allMovimientosDelPeriodo: StateFlow<List<MovimientoFinanciero>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(_periodo, _anio, _fechaDiario) { p, a, fd ->
                val now = LocalDate.now()
                val (start, end) = periodDateRange(p, a, fd, now)
                Triple(opticaId, start, end)
            }.flatMapLatest { (opticaId, start, end) ->
                combine(
                    repository.getAllDispensacionesForOptica(opticaId),
                    repository.getAllServiciosForOptica(opticaId),
                    repository.getAllPagosFlowForOptica(opticaId),
                ) { disps, servs, pagos ->
                    val pagosSumByDisp = pagos
                        .filter { it.dispensacionId != null }
                        .groupBy { it.dispensacionId!! }
                        .mapValues { (_, pags) -> pags.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
                    val aCuentaSumByServ = pagos
                        .filter { it.servicioExtraId != null }
                        .groupBy { it.servicioExtraId!! }
                        .mapValues { (_, pags) -> pags.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
                    val itemsByDisp = repository.getDispensacionItemsSnapshotForOptica(opticaId).groupBy { it.dispensacionId }
                    val regalosByDisp = repository.getRegalosSnapshotForOptica(opticaId).groupBy { it.dispensacionId }
                    val itemsByServ = repository.getServicioExtraItemsSnapshotForOptica(opticaId).groupBy { it.servicioExtraId }
                    val regalosByServ = repository.getRegalosServicioExtraSnapshotForOptica(opticaId).groupBy { it.servicioExtraId }
                    val dispMovs = disps
                        .filter { it.fecha >= start && it.fecha <= end }
                        .map { d ->
                            val costo = CostoVentaCalculator.dispensacion(
                                items = itemsByDisp[d.id].orEmpty(),
                                costoUnitarioSnapshot = null,
                                regalos = regalosByDisp[d.id].orEmpty(),
                            )
                            MovimientoFinanciero(
                                id = d.id,
                                fecha = d.fecha,
                                tipo = TipoMovimiento.VENTA,
                                origen = Origen.DISPENSACION,
                                origenId = d.id,
                                montoTotal = d.montoTotal,
                                montoPagado = pagosSumByDisp[d.id] ?: 0.0,
                                costo = costo.costo,
                                pacienteId = d.pacienteId,
                                opticaId = d.opticaId,
                                descripcion = "OT ${d.ot}",
                                vinculadoA = d.ot.takeIf { it.isNotBlank() },
                                costoIncompleto = costo.costoIncompleto,
                            )
                        }
                    val servMovs = servs
                        .filter { it.fecha >= start && it.fecha <= end }
                        .map { s ->
                            val costo = CostoVentaCalculator.servicio(s, itemsByServ[s.id].orEmpty(), regalosByServ[s.id].orEmpty())
                            MovimientoFinanciero(
                                id = s.id,
                                fecha = s.fecha,
                                tipo = TipoMovimiento.VENTA,
                                origen = Origen.SERVICIO,
                                origenId = s.id,
                                montoTotal = s.montoTotal,
                                montoPagado = aCuentaSumByServ[s.id] ?: 0.0,
                                costo = costo.costo,
                                pacienteId = s.pacienteId ?: "",
                                opticaId = s.opticaId,
                                descripcion = s.descripcion.takeIf { it.isNotBlank() } ?: "Servicio OT ${s.ot}",
                                vinculadoA = s.ot.takeIf { it.isNotBlank() },
                                costoIncompleto = costo.costoIncompleto,
                            )
                        }
                    dispMovs + servMovs
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movimientosPorOrigenId: StateFlow<Map<String, MovimientoFinanciero>> = allMovimientosDelPeriodo
        .map { movs -> movs.associateBy { it.origenId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalVendido: StateFlow<Double> = allMovimientosDelPeriodo
        .map { movs -> movs.sumOf { it.montoTotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val costoTotal: StateFlow<Double> = allMovimientosDelPeriodo
        .map { movs -> movs.sumOf { it.costo } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val margenTotal: StateFlow<Double> = allMovimientosDelPeriodo
        .map { movs -> movs.sumOf { it.montoTotal - it.costo } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPagado: StateFlow<Double> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(_periodo, _anio, _fechaDiario) { p, a, fd ->
                val now = LocalDate.now()
                val (start, end) = periodDateRange(p, a, fd, now)
                Triple(opticaId, start, end)
            }.flatMapLatest { (opticaId, start, end) ->
                repository.getPagosByDateRangeForOptica(start, end, opticaId)
                    .map { pagos -> pagos.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalTransacciones: StateFlow<Int> = allMovimientosDelPeriodo
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dispensacionesCount: StateFlow<Int> = allDispensaciones
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val serviciosCount: StateFlow<Int> = allServiciosDelPeriodo
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // montoPagado/aCuenta are @Ignore in entity; cash net via PagoEffect.

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagosSumByDispensacion: StateFlow<Map<String, Double>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            repository.getAllPagosFlowForOptica(opticaId)
                .map { pagos ->
                    pagos.filter { it.dispensacionId != null }
                        .groupBy { it.dispensacionId!! }
                        .mapValues { (_, pags) -> pags.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val aCuentaSumByServicio: StateFlow<Map<String, Double>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            repository.getAllPagosFlowForOptica(opticaId)
                .map { pagos ->
                    pagos.filter { it.servicioExtraId != null }
                        .groupBy { it.servicioExtraId!! }
                        .mapValues { (_, pags) -> pags.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalCobrado: StateFlow<Double> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(
                _periodo,
                _anio,
                _fechaDiario,
            ) { p, a, fd ->
                val now = LocalDate.now()
                val (start, end) = periodDateRange(p, a, fd, now)
                Triple(p, a, fd) to (start to end)
            }.flatMapLatest { (params, range) ->
                val (p, a, fd) = params
                val now = LocalDate.now()
                repository.getPagosByDateRangeForOptica(range.first, range.second, opticaId)
                    .map { pagos ->
                        pagos.filter { pago -> dentroDelPeriodo(pago.fecha, p, a, fd, now) }
                            .sumOf { PagoEffect.signedAmount(it.tipo, it.monto) }
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val cobrosPeriodo: StateFlow<Double> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(_periodo, _anio, _fechaDiario) { p, a, fd ->
                Triple(p, a, fd)
            }.flatMapLatest { (p, a, fd) ->
                val now = LocalDate.now()
                val (start, end) = periodDateRange(p, a, fd, now)
                combine(
                    repository.getPagosByDateRangeForOptica(start, end, opticaId),
                    repository.getAllDispensacionesForOptica(opticaId),
                    repository.getAllServiciosForOptica(opticaId),
                ) { pagos, todasDisp, todasServ ->
                    val dispMap = todasDisp.associateBy { it.id }
                    val servMap = todasServ.associateBy { it.id }
                    pagos
                        .filter { pago -> dentroDelPeriodo(pago.fecha, p, a, fd, now) }
                        .sumOf { pago ->
                            val dispFecha = pago.dispensacionId?.let { dispMap[it]?.fecha }
                            val servFecha = pago.servicioExtraId?.let { servMap[it]?.fecha }
                            val dispInPeriod = dispFecha != null && dentroDelPeriodo(dispFecha, p, a, fd, now)
                            val servInPeriod = servFecha != null && dentroDelPeriodo(servFecha, p, a, fd, now)
                            if (dispInPeriod || servInPeriod) 0.0 else PagoEffect.signedAmount(pago.tipo, pago.monto)
                        }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val periodComparison: StateFlow<PeriodComparison?> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(_periodo, _anio, _fechaDiario, allMovimientosDelPeriodo) { p, a, fd, current ->
                val now = LocalDate.now()
                val prevRange = previousPeriodDateRange(p, a, fd, now)
                Triple(opticaId, current, prevRange)
            }.flatMapLatest { (opticaId, current, prevRange) ->
                flow {
                    if (prevRange == null) {
                        emit(null)
                        return@flow
                    }
                    val (start, end) = prevRange
                    val disps = repository.getDispensacionesByDateRangeForOptica(start, end, opticaId).first()
                        .filter { it.estadoEntrega != "Anulado" && it.estadoEntrega != "Reclamada" }
                    val servs = repository.getServiciosByDateRangeForOptica(start, end, opticaId).first()
                        .filter { it.estado != "Anulado" }
                    val itemsByDisp = repository.getDispensacionItemsSnapshotForOptica(opticaId).groupBy { it.dispensacionId }
                    val regalosByDisp = repository.getRegalosSnapshotForOptica(opticaId).groupBy { it.dispensacionId }
                    val itemsByServ = repository.getServicioExtraItemsSnapshotForOptica(opticaId).groupBy { it.servicioExtraId }
                    val regalosByServ = repository.getRegalosServicioExtraSnapshotForOptica(opticaId).groupBy { it.servicioExtraId }
                    val pagos = repository.getAllPagosFlowForOptica(opticaId).first()
                    val pagosSumByDisp = pagos.filter { it.dispensacionId != null }
                        .groupBy { it.dispensacionId!! }
                        .mapValues { (_, pags) -> pags.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
                    val aCuentaSumByServ = pagos.filter { it.servicioExtraId != null }
                        .groupBy { it.servicioExtraId!! }
                        .mapValues { (_, pags) -> pags.sumOf { PagoEffect.signedAmount(it.tipo, it.monto) } }
                    val previous = disps.map { d ->
                        val costo = CostoVentaCalculator.dispensacion(
                            items = itemsByDisp[d.id].orEmpty(),
                            costoUnitarioSnapshot = null,
                            regalos = regalosByDisp[d.id].orEmpty(),
                        )
                        MovimientoFinanciero(
                            id = d.id, fecha = d.fecha, tipo = TipoMovimiento.VENTA, origen = Origen.DISPENSACION,
                            origenId = d.id, montoTotal = d.montoTotal, montoPagado = pagosSumByDisp[d.id] ?: 0.0,
                            costo = costo.costo, pacienteId = d.pacienteId, opticaId = d.opticaId,
                            descripcion = "OT ${d.ot}", costoIncompleto = costo.costoIncompleto,
                        )
                    } + servs.map { s ->
                        val costo = CostoVentaCalculator.servicio(s, itemsByServ[s.id].orEmpty(), regalosByServ[s.id].orEmpty())
                        MovimientoFinanciero(
                            id = s.id, fecha = s.fecha, tipo = TipoMovimiento.VENTA, origen = Origen.SERVICIO,
                            origenId = s.id, montoTotal = s.montoTotal, montoPagado = aCuentaSumByServ[s.id] ?: 0.0,
                            costo = costo.costo, pacienteId = s.pacienteId.orEmpty(), opticaId = s.opticaId,
                            descripcion = s.descripcion, costoIncompleto = costo.costoIncompleto,
                        )
                    }
                    emit(PeriodComparison.fromMovimientos(current, previous))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pacientesReporte: StateFlow<PacientesReporte> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(_periodo, _anio, _fechaDiario) { p, a, fd ->
                val (start, end) = periodDateRange(p, a, fd, LocalDate.now())
                Triple(opticaId, start, end)
            }.flatMapLatest { (opticaId, start, end) ->
                flow {
                    val pacientes = repository.getPacientesSnapshotForOptica(opticaId)
                    emit(ReportesModuleStats.pacientes(pacientes, start, end))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PacientesReporte(0, 0, 0, 0))

    val inventarioReporte: StateFlow<InventarioReporte> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(_periodo, _anio, _fechaDiario) { p, a, fd ->
                val (start, end) = periodDateRange(p, a, fd, LocalDate.now())
                Triple(opticaId, start, end)
            }.flatMapLatest { (opticaId, start, end) ->
                flow {
                    val monturas = repository.getMonturasSnapshotForOptica(opticaId)
                    val movimientos = repository.getMovimientosMonturaSnapshotForOptica(opticaId)
                    emit(ReportesModuleStats.inventario(monturas, movimientos, start, end))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventarioReporte(0, 0, 0, 0))

    val clinicoReporte: StateFlow<ClinicoReporte> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(_periodo, _anio, _fechaDiario) { p, a, fd ->
                val (start, end) = periodDateRange(p, a, fd, LocalDate.now())
                Triple(opticaId, start, end)
            }.flatMapLatest { (opticaId, start, end) ->
                flow {
                    val evaluaciones = repository.getEvaluacionesSnapshotForOptica(opticaId)
                    emit(ReportesModuleStats.clinico(evaluaciones, start, end))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClinicoReporte(0, emptyList()))

    val comisionPct: StateFlow<Double> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            membershipRepository.getOpticaSettingsFlow(opticaId)
                .map { CommissionConfigJson.extractPct(it?.configJson.orEmpty()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val equipoReporte: StateFlow<EquipoReporte> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(allDispensaciones, allServiciosDelPeriodo, comisionPct) { disps, servs, pct ->
                Triple(disps, servs, pct) to opticaId
            }.flatMapLatest { (triple, opticaId) ->
                val (disps, servs, pct) = triple
                flow {
                    val members = runCatching { membershipRepository.fetchMembersForOptica(opticaId) }.getOrDefault(emptyList())
                    val names = members.associate { it.userId to it.email }
                    emit(ReportesModuleStats.equipo(disps, servs, names, pct))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EquipoReporte(emptyList(), 0.0, 0, 0.0))

    fun setComisionPct(pct: Double) {
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            if (opticaId.isBlank()) return@launch
            val current = membershipRepository.getOpticaSettingsFlow(opticaId).first()
            val merged = CommissionConfigJson.mergePct(current?.configJson.orEmpty(), pct)
            val entity = OpticaSettingsEntity(opticaId = opticaId, configJson = merged)
            membershipRepository.upsertOpticaSettings(entity)
            membershipRepository.upsertOpticaSettingsRemote(opticaId, merged)
        }
    }
}
