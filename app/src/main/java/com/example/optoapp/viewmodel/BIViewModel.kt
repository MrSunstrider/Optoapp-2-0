package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class Periodo(val label: String) {
    MES_ACTUAL("Mes Actual"),
    TRIMESTRE("Trimestre"),
    SEMESTRE("Semestre"),
    ANIO("Año")
}

data class BIUiState(
    val periodo: Periodo = Periodo.MES_ACTUAL,
    val examenesActual: Int = 0,
    val examenesAnterior: Int = 0,
    val recaudacionProyectada: Double = 0.0,
    val recaudacionCobrada: Double = 0.0,
    val topProductos: List<ProductoRanking> = emptyList(),
    val isLoading: Boolean = false
)

data class ProductoRanking(
    val nombre: String,
    val cantidad: Int
)

@HiltViewModel
class BIViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BIUiState())
    val uiState: StateFlow<BIUiState> = _uiState.asStateFlow()

    init {
        observeStats()
    }

    fun setPeriodo(periodo: Periodo) {
        _uiState.update { it.copy(periodo = periodo) }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeStats() {
        combine(
            _uiState.map { it.periodo }.distinctUntilChanged(),
            sessionManager.opticaId
        ) { periodo, opticaId -> periodo to opticaId }
            .distinctUntilChanged()
            .flatMapLatest { (periodo, opticaId) ->
                val ranges = getRangesForPeriod(periodo)
                val prevRanges = getPreviousRangesForPeriod(periodo)

                combine(
                    repository.countEvaluacionesInRangeForOptica(ranges.first, ranges.second, opticaId),
                    repository.countEvaluacionesInRangeForOptica(prevRanges.first, prevRanges.second, opticaId),
                    repository.getDispensacionesByDateRangeForOptica(ranges.first, ranges.second, opticaId),
                    repository.getPagosByDateRangeForOptica(ranges.first, ranges.second, opticaId)
                ) { examsActual, examsAnterior, dispensaciones, pagos ->
                    
                    val proyectada = dispensaciones.sumOf { it.montoTotal }
                    val cobrada = pagos.sumOf { it.monto }
                    
                    val ranking = dispensaciones
                        .map { d -> 
                            val material = if (d.materialLente.isNotBlank()) d.materialLente else "Sin Material"
                            val trats = if (d.tratamientos.isNotEmpty()) " (${d.tratamientos.joinToString(", ")})" else ""
                            "$material$trats"
                        }
                        .groupBy { it }
                        .map { (name, list) -> ProductoRanking(name, list.size) }
                        .sortedByDescending { it.cantidad }
                        .take(5)

                    BIUiState(
                        periodo = periodo,
                        examenesActual = examsActual,
                        examenesAnterior = examsAnterior,
                        recaudacionProyectada = proyectada,
                        recaudacionCobrada = cobrada,
                        topProductos = ranking,
                        isLoading = false
                    )
                }
            }
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { state -> _uiState.value = state }
            .launchIn(viewModelScope)
    }

    private fun getRangesForPeriod(periodo: Periodo): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        
        when (periodo) {
            Periodo.MES_ACTUAL -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
            Periodo.TRIMESTRE -> {
                cal.add(Calendar.MONTH, -3)
            }
            Periodo.SEMESTRE -> {
                cal.add(Calendar.MONTH, -6)
            }
            Periodo.ANIO -> {
                cal.add(Calendar.YEAR, -1)
            }
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        
        return cal.timeInMillis to end
    }

    private fun getPreviousRangesForPeriod(periodo: Periodo): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        
        when (periodo) {
            Periodo.MES_ACTUAL -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                return start to cal.timeInMillis
            }
            else -> {
                // Para otros periodos usamos el mismo periodo inmediatamente anterior
                val currentRange = getRangesForPeriod(periodo)
                val diff = currentRange.second - currentRange.first
                return (currentRange.first - diff) to currentRange.first
            }
        }
    }
}
