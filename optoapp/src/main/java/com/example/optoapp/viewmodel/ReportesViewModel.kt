package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val allDispensaciones: StateFlow<List<DispensacionOptica>> = sessionManager.opticaId
        .flatMapLatest { opticaId ->
            combine(
                repository.getAllDispensacionesForOptica(opticaId),
                _periodo,
                _anio
            ) { list, p, a ->
                val now = java.time.LocalDate.now()
                list.filter { disp ->
                    val date = disp.fecha
                    when (p) {
                        "Diario" -> date.isEqual(now)
                        "Semanal" -> {
                            val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
                            date.year == now.year && date.get(weekFields.weekOfYear()) == now.get(weekFields.weekOfYear())
                        }
                        "Este mes" -> date.year == now.year && date.month == now.month
                        "Este año" -> date.year == now.year
                        "Anual" -> date.year.toString() == a
                        "Todo" -> true
                        else -> true
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalVendido: StateFlow<Double> = allDispensaciones
        .map { list -> list.sumOf { it.montoTotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPagado: StateFlow<Double> = allDispensaciones
        .map { list -> list.sumOf { it.montoPagado } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
