package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.venta.Venta
import com.example.optoapp.data.venta.VentaDao
import com.example.optoapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.Year
import javax.inject.Inject

@HiltViewModel
class ReportesViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val ventaDao: VentaDao
) : ViewModel() {

    private val _periodo = MutableStateFlow("Mensual")
    val periodo: StateFlow<String> = _periodo
    
    private val _anio = MutableStateFlow(Year.now().value.toString())
    val anio: StateFlow<String> = _anio

    private val _fechaDiario = MutableStateFlow(LocalDate.now())
    val fechaDiario: StateFlow<LocalDate> = _fechaDiario

    fun setPeriodo(p: String) { _periodo.value = p }
    fun setAnio(a: String) { _anio.value = a }
    fun setFechaDiario(fecha: LocalDate) { _fechaDiario.value = fecha }

    // ── Navigation ──
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

    // ── Period labels for display ──
    val periodoLabel: StateFlow<String> = combine(_periodo, _fechaDiario, _anio) { p, fd, a ->
        when (p) {
            "Diario" -> DateUtils.formatLocalized(fd)
            "Semanal" -> {
                val start = fd.minusDays((fd.dayOfWeek.value - 1).toLong())
                val end = start.plusDays(6)
                "${DateUtils.formatLocalized(start)} - ${DateUtils.formatLocalized(end)}"
            }
            "Mensual" -> {
                val months = arrayOf("Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre")
                "${months[fd.monthValue - 1]} ${fd.year}"
            }
            "Anual" -> a
            else -> "Total"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private fun dentroDelPeriodo(date: LocalDate, p: String, a: String, fechaDiario: LocalDate, now: LocalDate): Boolean {
        return when (p) {
            "Diario" -> date.isEqual(fechaDiario)
            "Semanal" -> {
                val dayOfWeek = fechaDiario.dayOfWeek.value
                val startOfWeek = fechaDiario.minusDays(dayOfWeek.toLong() - 1)
                val endOfWeek = startOfWeek.plusDays(6)
                !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
            }
            "Mensual" -> date.year == fechaDiario.year && date.month == fechaDiario.month
            "Este año" -> date.year == fechaDiario.year
            "Anual" -> date.year.toString() == a
            else -> true
        }
    }

    private fun periodDateRange(p: String, a: String, fd: LocalDate, now: LocalDate): Pair<LocalDate, LocalDate> = when (p) {
        "Diario" -> fd to fd
        "Semanal" -> {
            val startOfWeek = fd.minusDays((fd.dayOfWeek.value - 1).toLong())
            startOfWeek to startOfWeek.plusDays(6)
        }
        "Mensual" -> fd.withDayOfMonth(1) to fd.withDayOfMonth(fd.lengthOfMonth())
        "Este año" -> fd.withDayOfYear(1) to fd.withDayOfYear(fd.lengthOfYear())
        "Anual" -> LocalDate.of(a.toInt(), 1, 1) to LocalDate.of(a.toInt(), 12, 31)
        else -> LocalDate.MIN to LocalDate.MAX
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allDispensaciones: StateFlow<List<DispensacionOptica>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(
                repository.getAllDispensacionesForOptica(opticaId),
                _periodo,
                _anio,
                _fechaDiario
            ) { list, p, a, fd ->
                val now = LocalDate.now()
                list.filter { disp -> dentroDelPeriodo(disp.fecha, p, a, fd, now) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allServiciosDelPeriodo: StateFlow<List<ServicioExtra>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(
                repository.getAllServiciosForOptica(opticaId),
                _periodo,
                _anio,
                _fechaDiario
            ) { list, p, a, fd ->
                val now = LocalDate.now()
                list.filter { servicio -> dentroDelPeriodo(servicio.fecha, p, a, fd, now) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allVentasDelPeriodo: StateFlow<List<Venta>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(_periodo, _anio, _fechaDiario) { p, a, fd ->
                val now = LocalDate.now()
                val (start, end) = periodDateRange(p, a, fd, now)
                Triple(opticaId, start, end)
            }.flatMapLatest { (opticaId, start, end) ->
                combine(
                    ventaDao.getVentasByOpticaAndDateRange(opticaId, start, end),
                    allDispensaciones,
                    allServiciosDelPeriodo
                ) { ventas, disps, servs ->
                    if (ventas.isNotEmpty()) ventas
                    else {
                        // Fallback: derive from legacy entities when ventas table is not yet populated.
                        // This ensures backward compatibility during the transition.
                        val dispVentas = disps.map { d ->
                            Venta(id = "venta-${d.id}", opticaId = d.opticaId, origen = "dispensacion",
                                origenId = d.id, pacienteId = d.pacienteId, fecha = d.fecha,
                                montoTotal = d.montoTotal, estado = "Completado")
                        }
                        val servVentas = servs.map { s ->
                            Venta(id = "venta-${s.id}", opticaId = s.opticaId, origen = "servicio_extra",
                                origenId = s.id, pacienteId = "", fecha = s.fecha,
                                montoTotal = s.montoTotal, estado = "Completado")
                        }
                        dispVentas + servVentas
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalVendido: StateFlow<Double> = allVentasDelPeriodo
        .map { ventas -> ventas.sumOf { it.montoTotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPagado: StateFlow<Double> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(_periodo, _anio, _fechaDiario) { p, a, fd ->
                val now = LocalDate.now()
                val (start, end) = periodDateRange(p, a, fd, now)
                Triple(opticaId, start, end)
            }.flatMapLatest { (opticaId, start, end) ->
                repository.getPagosByDateRangeForOptica(start, end, opticaId)
                    .map { pagos -> pagos.filter { it.tipo != "Anulación" }.sumOf { it.monto } }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalTransacciones: StateFlow<Int> = allVentasDelPeriodo
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dispensacionesCount: StateFlow<Int> = allDispensaciones
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val serviciosCount: StateFlow<Int> = allServiciosDelPeriodo
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalCobrado: StateFlow<Double> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(
                _periodo,
                _anio,
                _fechaDiario
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
                            .sumOf { it.monto }
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
                    repository.getAllServiciosForOptica(opticaId)
                ) { pagos, todasDisp, todasServ ->
                    val dispMap = todasDisp.associateBy { it.id }
                    val servMap = todasServ.associateBy { it.id }
                    pagos.filter { pago -> dentroDelPeriodo(pago.fecha, p, a, fd, now) }
                        .sumOf { pago ->
                            val dispFecha = pago.dispensacionId?.let { dispMap[it]?.fecha }
                            when {
                                dispFecha != null && dentroDelPeriodo(dispFecha, p, a, fd, now) -> 0.0
                                pago.dispensacionId == null && pago.servicioExtraId?.let { servMap[it]?.fecha }
                                    ?.let { dentroDelPeriodo(it, p, a, fd, now) } == true -> 0.0
                                else -> pago.monto
                            }
                        }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
