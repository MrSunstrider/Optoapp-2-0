package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.domain.CxcAging
import com.example.optoapp.domain.CxcAgingResumen
import com.example.optoapp.domain.Deudor
import com.example.optoapp.domain.FeedbackRecomendacionUseCase
import com.example.optoapp.domain.GenerarRecomendacionesUseCase
import com.example.optoapp.domain.InterpretacionAnalista
import com.example.optoapp.domain.InterpretacionMes
import com.example.optoapp.domain.ObtenerAnalisisMensualUseCase
import com.example.optoapp.domain.ObtenerDeudoresUseCase
import com.example.optoapp.domain.Recomendacion
import com.example.optoapp.domain.ResultadoDrivers
import com.example.optoapp.domain.ResultadoDriversMom
import com.example.optoapp.domain.SeguimientoEstado
import com.example.optoapp.domain.SupuestosImpacto
import com.example.optoapp.util.CurrentMonthProvider
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.MesArg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AnalisisNegocioUiState(
    val mesSeleccionado: LocalDate = DateUtils.today().withDayOfMonth(1),
    val analisis: AnalisisMensual? = null,
    val interpretacion: InterpretacionMes? = null,
    val deudores: List<Deudor> = emptyList(),
    val recomendaciones: List<Recomendacion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSeasonalityWarning: Boolean = false,
    val deudoresStale: Boolean = false,
    val seguimientos: Map<String, SeguimientoEstado> = emptyMap(),
    val feedbackErrorRecId: String? = null,
    val supuestosImpacto: SupuestosImpacto = SupuestosImpacto(),
    val supuestosDialogVisible: Boolean = false,
    val supuestosError: String? = null,
    val driversMom: ResultadoDriversMom? = null,
    val cxcAging: CxcAgingResumen? = null,
)

@HiltViewModel
class AnalisisNegocioViewModel @Inject constructor(
    private val obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase,
    private val obtenerDeudores: ObtenerDeudoresUseCase,
    private val generarRecomendaciones: GenerarRecomendacionesUseCase,
    private val feedbackRecomendacion: FeedbackRecomendacionUseCase,
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao,
    private val sessionManager: SessionManager,
    private val currentMonthProvider: CurrentMonthProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val TAG = "AnalisisNegocioVM"
        private val PERIODO_FMT = DateTimeFormatter.ofPattern("yyyy-MM")
        const val SUPUESTOS_ERROR_FRACCION = "fraccion"
        const val SUPUESTOS_ERROR_NO_CONFIG = "no_config"
    }

    private val _uiState = MutableStateFlow(
        AnalisisNegocioUiState(
            mesSeleccionado = MesArg.parseOrCurrent(
                savedStateHandle.get<String>(MesArg.KEY),
                currentMonthProvider.current(),
            ).atDay(1),
        ),
    )
    val uiState: StateFlow<AnalisisNegocioUiState> = _uiState.asStateFlow()

    val mesActual: YearMonth get() = currentMonthProvider.current()

    private var loadJob: Job? = null
    private val seguimientoMutex = Mutex()

    init {
        loadData(_uiState.value.mesSeleccionado)
    }

    fun selectMonth(mes: YearMonth) {
        val newMonth = minOf(mes, mesActual).atDay(1)
        if (newMonth == _uiState.value.mesSeleccionado) return
        _uiState.value = _uiState.value.copy(mesSeleccionado = newMonth)
        loadData(newMonth)
    }

    fun refresh() {
        loadData(_uiState.value.mesSeleccionado)
    }

    fun onSeguimiento(recomendacionId: String, estado: SeguimientoEstado) {
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                val periodo = _uiState.value.mesSeleccionado.format(PERIODO_FMT)
                seguimientoMutex.withLock {
                    feedbackRecomendacion.marcarSeguimiento(recomendacionId, opticaId, periodo, estado)
                    _uiState.value = _uiState.value.copy(
                        seguimientos = _uiState.value.seguimientos + (recomendacionId to estado),
                        feedbackErrorRecId = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error saving recommendation seguimiento", e)
                _uiState.value = _uiState.value.copy(feedbackErrorRecId = recomendacionId)
            }
        }
    }

    fun showSupuestosDialog() {
        _uiState.value = _uiState.value.copy(supuestosDialogVisible = true, supuestosError = null)
    }

    fun dismissSupuestosDialog() {
        _uiState.value = _uiState.value.copy(supuestosDialogVisible = false, supuestosError = null)
    }

    fun saveSupuestos(supuestos: SupuestosImpacto) {
        viewModelScope.launch {
            try {
                if (!fractionsInRange(supuestos)) {
                    _uiState.value = _uiState.value.copy(supuestosError = SUPUESTOS_ERROR_FRACCION)
                    return@launch
                }
                val opticaId = sessionManager.opticaId.first()
                val current = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
                if (current == null) {
                    _uiState.value = _uiState.value.copy(supuestosError = SUPUESTOS_ERROR_NO_CONFIG)
                    return@launch
                }
                configuracionFinancieraDao.upsert(
                    current.copy(
                        impactoMargenObjetivoPct = supuestos.margenObjetivoPct,
                        impactoRecuperoStockPct = supuestos.recuperoStockPct,
                        impactoUpsideVentasPct = supuestos.upsideVentasPct,
                        impactoCorteGastoPct = supuestos.corteGastoPct,
                        impactoRecuperoCobranzaPct = supuestos.recuperoCobranzaPct,
                    ),
                )
                _uiState.value = _uiState.value.copy(
                    supuestosImpacto = supuestos,
                    supuestosDialogVisible = false,
                    supuestosError = null,
                )
                loadData(_uiState.value.mesSeleccionado)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error saving impact assumptions", e)
                _uiState.value = _uiState.value.copy(supuestosError = e.message ?: "error")
            }
        }
    }

    private fun fractionsInRange(supuestos: SupuestosImpacto): Boolean =
        listOf(
            supuestos.recuperoStockPct,
            supuestos.upsideVentasPct,
            supuestos.corteGastoPct,
            supuestos.recuperoCobranzaPct,
        ).all { it in 0.0..1.0 }

    private fun loadData(mes: LocalDate) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val thisJob = coroutineContext[Job]
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val opticaId = sessionManager.opticaId.first()

                try {
                    val config = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
                    val supuestos = config?.let { SupuestosImpacto.from(it) } ?: SupuestosImpacto()
                    val analisisDeferred = async {
                        obtenerAnalisisMensual(opticaId, mes)
                    }
                    val deudoresDeferred = async {
                        obtenerDeudores(opticaId)
                    }

                    val analisisResult = analisisDeferred.await()
                    val deudoresResult = deudoresDeferred.await()

                    val analisis = (analisisResult as? Resource.Success)?.data
                    val deudores = (deudoresResult as? Resource.Success)?.data ?: emptyList()
                    val deudoresStale = (deudoresResult as? Resource.Success)?.stale ?: false

                    val recomendacionesResult = if (analisis != null || deudores.isNotEmpty()) {
                        generarRecomendaciones(analisis, deudores, opticaId, mes)
                    } else {
                        Resource.Error("Datos insuficientes")
                    }

                    val rpcErrors = listOfNotNull(
                        (analisisResult as? Resource.Error)?.message,
                        (deudoresResult as? Resource.Error)?.message,
                    )
                    val recError = (recomendacionesResult as? Resource.Error)?.message
                    val errors = (rpcErrors + listOfNotNull(recError)).toSet()
                    val recomendaciones = (recomendacionesResult as? Resource.Success)?.data ?: emptyList()
                    val interpretacion = analisis?.let {
                        InterpretacionAnalista.fromAnalisis(it, recomendaciones.firstOrNull()?.titulo)
                    }
                    val driversMom = analisis?.let { ResultadoDrivers.fromAnalisis(it) }
                    val cxcAging = analisis?.let {
                        CxcAging.from(
                            deudores = deudores,
                            saldoPendiente = it.saldoPendiente,
                            ventasMes = it.ventasMes,
                            mes = YearMonth.from(mes),
                        )
                    }
                    val periodo = mes.format(PERIODO_FMT)
                    if (_uiState.value.mesSeleccionado != mes) return@launch

                    seguimientoMutex.withLock {
                        val seguimientos = feedbackRecomendacion.listarPorPeriodo(opticaId, periodo)
                        if (_uiState.value.mesSeleccionado != mes) return@withLock
                        _uiState.value = _uiState.value.copy(
                            analisis = analisis,
                            interpretacion = interpretacion,
                            deudores = deudores,
                            recomendaciones = recomendaciones,
                            isLoading = false,
                            error = errors.joinToString("; ").ifEmpty { null },
                            isSeasonalityWarning = analisis?.esOffline == true || (analisis != null && analisis.ventasMesAnterior == 0.0),
                            deudoresStale = deudoresStale,
                            seguimientos = seguimientos,
                            feedbackErrorRecId = null,
                            supuestosImpacto = supuestos,
                            driversMom = driversMom,
                            cxcAging = cxcAging,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error loading data", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error inesperado al cargar datos",
                    )
                }
            } finally {
                if (loadJob == thisJob) loadJob = null
            }
        }
    }
}
