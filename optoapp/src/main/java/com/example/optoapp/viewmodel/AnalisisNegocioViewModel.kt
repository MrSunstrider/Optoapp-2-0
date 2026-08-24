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
import com.example.optoapp.domain.ObtenerAnalisisMensualUseCase
import com.example.optoapp.domain.ObtenerDeudoresUseCase
import com.example.optoapp.domain.Recomendacion
import com.example.optoapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import javax.inject.Inject

data class AnalisisNegocioUiState(
    val mesSeleccionado: LocalDate = DateUtils.today().withDayOfMonth(1),
    val analisis: AnalisisMensual? = null,
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val TAG = "AnalisisNegocioVM"
        const val YEAR_MONTH_ARG = "yearMonth"

        fun resolveInitialMonth(
            yearMonth: String?,
            fallback: LocalDate = DateUtils.today().withDayOfMonth(1),
        ): LocalDate {
            if (yearMonth.isNullOrBlank()) return fallback
            return try {
                YearMonth.parse(yearMonth).atDay(1)
            } catch (e: DateTimeParseException) {
                Log.w(TAG, "Invalid yearMonth='$yearMonth', falling back to current month")
                fallback
            }
        }
    }

    private val _uiState = MutableStateFlow(
        AnalisisNegocioUiState(
            mesSeleccionado = resolveInitialMonth(savedStateHandle.get<String>(YEAR_MONTH_ARG)),
        ),
    )
    val uiState: StateFlow<AnalisisNegocioUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadData(_uiState.value.mesSeleccionado)
    }

    fun navigateMonth(delta: Int) {
        val newMonth = _uiState.value.mesSeleccionado.plusMonths(delta.toLong())
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
                    supervisorScope {
                    val analisisDeferred = async {
                        try {
                            obtenerAnalisisMensual(opticaId, mes)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "obtenerAnalisisMensual threw", e)
                            Resource.Error(e.message ?: "Error en análisis")
                        }
                    }
                    val deudoresDeferred = async {
                        try {
                            obtenerDeudores(opticaId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "obtenerDeudores threw", e)
                            Resource.Error(e.message ?: "Error en deudores")
                        }
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

                    _uiState.value = _uiState.value.copy(
                        analisis = analisis,
                        deudores = deudores,
                        recomendaciones = (recomendacionesResult as? Resource.Success)?.data ?: emptyList(),
                        isLoading = false,
                        error = errors.joinToString("; ").ifEmpty { null },
                        isSeasonalityWarning = analisis?.esOffline == true || (analisis != null && analisis.ventasMesAnterior == 0.0),
                        debtorsStale = debtorsStale,
                    )
                    } // end supervisorScope
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
