package com.example.optoapp.viewmodel

import android.util.Log
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AnalisisNegocioUiState(
    val mesSeleccionado: LocalDate = DateUtils.today().withDayOfMonth(1),
    val analisis: AnalisisMensual? = null,
    val deudores: List<Deudor> = emptyList(),
    val recomendaciones: List<Recomendacion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mostrarAdvertenciaEstacionalidad: Boolean = false
)

@HiltViewModel
class AnalisisNegocioViewModel @Inject constructor(
    private val obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase,
    private val obtenerDeudores: ObtenerDeudoresUseCase,
    private val generarRecomendaciones: GenerarRecomendacionesUseCase,
    private val feedbackRecomendacion: FeedbackRecomendacionUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    companion object {
        private const val TAG = "AnalisisNegocioVM"
    }

    private val _uiState = MutableStateFlow(AnalisisNegocioUiState())
    val uiState: StateFlow<AnalisisNegocioUiState> = _uiState.asStateFlow()

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
            val opticaId = sessionManager.opticaId.first()
            if (fueUtil) {
                feedbackRecomendacion.marcarUtil(recomendacionId, opticaId)
            } else {
                feedbackRecomendacion.marcarNoUtil(recomendacionId, opticaId)
            }
        }
    }

    private fun loadData(mes: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val opticaId = sessionManager.opticaId.first()

            val result = runCatching {
                coroutineScope {
                    val analisisDeferred = async {
                        runCatching {
                            obtenerAnalisisMensual(opticaId, mes)
                        }.onFailure { e ->
                            Log.e(TAG, "Error fetching analisis mensual", e)
                        }.getOrNull()
                    }
                    val deudoresDeferred = async {
                        runCatching {
                            obtenerDeudores(opticaId)
                        }.onFailure { e ->
                            Log.e(TAG, "Error fetching deudores", e)
                        }.getOrNull()
                    }
                    val recomendacionesDeferred = async {
                        runCatching {
                            generarRecomendaciones(opticaId, mes)
                        }.onFailure { e ->
                            Log.e(TAG, "Error fetching recomendaciones", e)
                        }.getOrNull()
                    }

                    Triple(
                        analisisDeferred.await(),
                        deudoresDeferred.await(),
                        recomendacionesDeferred.await()
                    )
                }
            }

            result.onSuccess { (analisisResult, deudoresResult, recomendacionesResult) ->
                val errors = mutableListOf<String>()
                val analisis = if (analisisResult is Resource.Success) {
                    analisisResult.data
                } else {
                    if (analisisResult is Resource.Error) errors.add(analisisResult.message!!)
                    null
                }
                val deudores = if (deudoresResult is Resource.Success) {
                    deudoresResult.data!!
                } else {
                    if (deudoresResult is Resource.Error) errors.add(deudoresResult.message!!)
                    emptyList()
                }
                val recomendaciones = if (recomendacionesResult is Resource.Success) {
                    recomendacionesResult.data!!
                } else {
                    if (recomendacionesResult is Resource.Error) errors.add(recomendacionesResult.message!!)
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    analisis = analisis,
                    deudores = deudores,
                    recomendaciones = recomendaciones,
                    isLoading = false,
                    error = errors.takeIf { it.isNotEmpty() }?.joinToString("; "),
                    mostrarAdvertenciaEstacionalidad = analisis?.esOffline == true
                )
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error loading data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error inesperado: ${e.localizedMessage}"
                )
            }
        }
    }
}
