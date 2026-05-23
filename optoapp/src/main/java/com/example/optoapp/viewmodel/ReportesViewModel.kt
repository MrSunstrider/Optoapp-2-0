package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReportesViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _periodo = MutableStateFlow("Este mes")
    val periodo: StateFlow<String> = _periodo
    
    private val _anio = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR).toString())
    val anio: StateFlow<String> = _anio

    fun setPeriodo(p: String) { _periodo.value = p }
    fun setAnio(a: String) { _anio.value = a }

    private fun dentroDelPeriodo(date: LocalDate, p: String, a: String, now: LocalDate): Boolean {
        return when (p) {
            "Diario" -> date.isEqual(now)
            "Semanal" -> {
                val dayOfWeek = now.dayOfWeek.value
                val startOfWeek = now.minusDays(dayOfWeek.toLong() - 1)
                val endOfWeek = startOfWeek.plusDays(7)
                !date.isBefore(startOfWeek) && date.isBefore(endOfWeek)
            }
            "Este mes" -> date.year == now.year && date.month == now.month
            "Este año" -> date.year == now.year
            "Anual" -> date.year.toString() == a
            else -> true
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allDispensaciones: StateFlow<List<DispensacionOptica>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(
                repository.getAllDispensacionesForOptica(opticaId),
                _periodo,
                _anio
            ) { list, p, a ->
                val now = LocalDate.now()
                list.filter { disp -> dentroDelPeriodo(disp.fecha, p, a, now) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalVendido: StateFlow<Double> = allDispensaciones
        .map { list -> list.sumOf { it.montoTotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPagado: StateFlow<Double> = allDispensaciones
        .map { list -> list.sumOf { it.montoPagado } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalCobrado: StateFlow<Double> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(
                repository.getPagosByDateRangeForOptica(LocalDate.MIN, LocalDate.MAX, opticaId),
                _periodo,
                _anio,
                allDispensaciones
            ) { pagos, p, a, dispensaciones ->
                val now = LocalDate.now()
                val dispMap = dispensaciones.associateBy { it.id }
                pagos.filter { pago -> dentroDelPeriodo(pago.fecha, p, a, now) }
                    .sumOf { pago ->
                        // Clasificar: si la dispensación es del período → venta, si no → cobro atrasado
                        pago.monto
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val cobrosPeriodo: StateFlow<Double> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(
                repository.getPagosByDateRangeForOptica(LocalDate.MIN, LocalDate.MAX, opticaId),
                repository.getAllDispensacionesForOptica(opticaId),
                _periodo,
                _anio
            ) { pagos, todasDisp, p, a ->
                val now = LocalDate.now()
                val dispMap = todasDisp.associateBy { it.id }
                pagos.filter { pago -> dentroDelPeriodo(pago.fecha, p, a, now) }
                    .sumOf { pago ->
                        val dispFecha = pago.dispensacionId?.let { dispMap[it]?.fecha }
                        if (dispFecha != null && dentroDelPeriodo(dispFecha, p, a, now)) 0.0
                        else pago.monto
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
