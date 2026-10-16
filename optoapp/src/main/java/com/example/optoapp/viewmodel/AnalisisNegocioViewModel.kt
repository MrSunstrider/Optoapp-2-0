package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.domain.Deudor
import com.example.optoapp.domain.FeedbackRecomendacionUseCase
import com.example.optoapp.domain.GenerarRecomendacionesUseCase
import com.example.optoapp.domain.InterpretacionAnalista
import com.example.optoapp.domain.InterpretacionMes
import com.example.optoapp.domain.ObtenerAnalisisMensualUseCase
import com.example.optoapp.domain.ObtenerDeudoresUseCase
import com.example.optoapp.domain.Recomendacion
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
import java.time.LocalDate
import java.time.YearMonth
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
    val debtorsStale: Boolean = false,
    val feedbacksEnviados: Map<String, Boolean> = emptyMap(),
    val feedbackErrorRecId: String? = null,
)

@HiltViewModel
class AnalisisNegocioViewModel @Inject constructor(
    private val obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase,
    private val obtenerDeudores: ObtenerDeudoresUseCase,
    private val generarRecomendaciones: GenerarRecomendacionesUseCase,
    private val feedbackRecomendacion: FeedbackRecomendacionUseCase,
    private val sessionManager: SessionManager,
    private val currentMonthProvider: CurrentMonthProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val TAG = "AnalisisNegocioVM"
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

    fun onFeedback(recomendacionId: String, fueUtil: Boolean) {
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                if (fueUtil) {
                    feedbackRecomendacion.marcarUtil(recomendacionId, opticaId)
                } else {
                    feedbackRecomendacion.marcarNoUtil(recomendacionId, opticaId)
                }
                _uiState.value = _uiState.value.copy(
                    feedbacksEnviados = _uiState.value.feedbacksEnviados + (recomendacionId to fueUtil),
                    feedbackErrorRecId = null,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sending recommendation feedback", e)
                _uiState.value = _uiState.value.copy(feedbackErrorRecId = recomendacionId)
            }
        }
    }

    private fun loadData(mes: LocalDate) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val thisJob = coroutineContext[Job]
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val opticaId = sessionManager.opticaId.first()

                try {
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
                    val debtorsStale = (deudoresResult as? Resource.Success)?.stale ?: false

                    val recomendacionesResult = if (analisis != null || deudores.isNotEmpty()) {
                        generarRecomendaciones(analisis, deudores, opticaId)
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

                    _uiState.value = _uiState.value.copy(
                        analisis = analisis,
                        interpretacion = interpretacion,
                        deudores = deudores,
                        recomendaciones = recomendaciones,
                        isLoading = false,
                        error = errors.joinToString("; ").ifEmpty { null },
                        isSeasonalityWarning = analisis?.esOffline == true || (analisis != null && analisis.ventasMesAnterior == 0.0),
                        debtorsStale = debtorsStale,
                    )
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
