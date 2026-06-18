package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    val entregasPendientesPeriodo: Int = 0,
    val entregasCompletadasPeriodo: Int = 0,
    val ventasConMonturaCatalogo: Int = 0,
    val monturasStockBajo: Int = 0,
    val salidasInventarioVentas: Int = 0,
    val totalModelos: Int = 0,
    val inventarioStockBajo: Int = 0,
    val isLoading: Boolean = false
)

data class ProductoRanking(
    val nombre: String,
    val cantidad: Int
)

private data class BiCoreFlows(
    val examenesActual: Int,
    val examenesAnterior: Int,
    val dispensaciones: List<DispensacionOptica>,
    val pagos: List<Pago>
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
                    combine(
                        repository.countEvaluacionesInRangeForOptica(ranges.first, ranges.second, opticaId),
                        repository.countEvaluacionesInRangeForOptica(prevRanges.first, prevRanges.second, opticaId),
                        repository.getDispensacionesByDateRangeForOptica(ranges.first, ranges.second, opticaId),
                        repository.getPagosByDateRangeForOptica(ranges.first, ranges.second, opticaId)
                    ) { e1, e2, disp, pag ->
                        BiCoreFlows(e1, e2, disp, pag)
                    },
                    repository.getMonturasByOptica(opticaId),
                    repository.getMovimientosMonturaByOptica(opticaId)
                ) { core, monturas, movimientos ->
                    val dispensaciones = core.dispensaciones
                    val pagos = core.pagos
                    val proyectada = dispensaciones.sumOf { it.montoTotal }
                    val cobrada = pagos.sumOf { it.monto }

                    val entregasPendientes = dispensaciones.count { it.estadoEntrega.equals("Pendiente", ignoreCase = true) }
                    val entregasHechas = dispensaciones.count { it.estadoEntrega.equals("Entregado", ignoreCase = true) }
                    val conMontura = dispensaciones.count { it.monturaId.isNotBlank() }
                    val stockBajo = monturas.count { m -> m.activo && m.stockActual <= m.stockMinimo }
                    val salidasVentas = movimientos.count { m ->
                        m.fecha >= ranges.first && m.fecha <= ranges.second && m.tipo == "SALIDA_VENTA"
                    }

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

                    val totalModelos = monturas.count { m -> m.activo }
                    val inventarioStockBajo = monturas.count { m ->
                        m.activo && m.stockActual <= (if (m.stockMinimo > 0) m.stockMinimo else 2)
                    }

                    BIUiState(
                        periodo = periodo,
                        examenesActual = core.examenesActual,
                        examenesAnterior = core.examenesAnterior,
                        recaudacionProyectada = proyectada,
                        recaudacionCobrada = cobrada,
                        topProductos = ranking,
                        entregasPendientesPeriodo = entregasPendientes,
                        entregasCompletadasPeriodo = entregasHechas,
                        ventasConMonturaCatalogo = conMontura,
                        monturasStockBajo = stockBajo,
                        salidasInventarioVentas = salidasVentas,
                        totalModelos = totalModelos,
                        inventarioStockBajo = inventarioStockBajo,
                        isLoading = false
                    )
                }
            }
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { state -> _uiState.value = state }
            .launchIn(viewModelScope)
    }

    private fun getRangesForPeriod(periodo: Periodo): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (periodo) {
            Periodo.MES_ACTUAL -> {
                val start = today.withDayOfMonth(1)
                val end = start.plusMonths(1).minusDays(1)
                start to end
            }
            Periodo.TRIMESTRE -> {
                val quarterStartMonth = ((today.monthValue - 1) / 3) * 3 + 1
                val start = LocalDate.of(today.year, quarterStartMonth, 1)
                val end = start.plusMonths(3).minusDays(1)
                start to end
            }
            Periodo.SEMESTRE -> {
                val semesterStartMonth = if (today.monthValue <= 6) 1 else 7
                val start = LocalDate.of(today.year, semesterStartMonth, 1)
                val end = start.plusMonths(6).minusDays(1)
                start to end
            }
            Periodo.ANIO -> {
                val start = LocalDate.of(today.year, 1, 1)
                val end = LocalDate.of(today.year, 12, 31)
                start to end
            }
        }
    }

    private fun getPreviousRangesForPeriod(periodo: Periodo): Pair<LocalDate, LocalDate> {
        val (currentStart, _) = getRangesForPeriod(periodo)
        return when (periodo) {
            Periodo.MES_ACTUAL -> {
                val start = currentStart.minusMonths(1)
                val end = currentStart.minusDays(1)
                start to end
            }
            Periodo.TRIMESTRE -> {
                val start = currentStart.minusMonths(3)
                val end = currentStart.minusDays(1)
                start to end
            }
            Periodo.SEMESTRE -> {
                val start = currentStart.minusMonths(6)
                val end = currentStart.minusDays(1)
                start to end
            }
            Periodo.ANIO -> {
                val start = currentStart.minusYears(1)
                val end = currentStart.minusDays(1)
                start to end
            }
        }
    }
}
