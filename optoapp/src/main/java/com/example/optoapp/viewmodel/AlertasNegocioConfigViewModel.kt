package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertasNegocioConfigUi(
    val caidaVentasAlertaPct: String = "10",
    val deudaViejaAlertaDias: String = "30",
    val deudaTotalAlertaMonto: String = "3000",
    val stockEstancadoAlertaDias: String = "180",
    val minVentasParaRecomendar: String = "5",
    val margenBajoAlertaPct: String = "10",
    val margenAltoAlertaPct: String = "35",
    val contribucionAltaRatio: String = "0.25",
    val gastoFueraRangoRatio: String = "0.4",
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AlertasNegocioConfigViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao,
) : ViewModel() {

    private val _ui = MutableStateFlow(AlertasNegocioConfigUi())
    val ui: StateFlow<AlertasNegocioConfigUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch { cargar() }
    }

    fun updateField(transform: (AlertasNegocioConfigUi) -> AlertasNegocioConfigUi) {
        _ui.update { transform(it).copy(mensaje = null, error = null) }
    }

    fun guardar() {
        viewModelScope.launch {
            val draft = _ui.value
            val caida = draft.caidaVentasAlertaPct.toDoubleOrNull()
            val deudaDias = draft.deudaViejaAlertaDias.toIntOrNull()
            val deudaMonto = draft.deudaTotalAlertaMonto.toDoubleOrNull()
            val stockDias = draft.stockEstancadoAlertaDias.toIntOrNull()
            val minVentas = draft.minVentasParaRecomendar.toIntOrNull()
            val margenBajo = draft.margenBajoAlertaPct.toDoubleOrNull()
            val margenAlto = draft.margenAltoAlertaPct.toDoubleOrNull()
            val contrib = draft.contribucionAltaRatio.toDoubleOrNull()
            val gastoRatio = draft.gastoFueraRangoRatio.toDoubleOrNull()
            if (caida == null || caida < 0 ||
                deudaDias == null || deudaDias < 0 ||
                deudaMonto == null || deudaMonto < 0 ||
                stockDias == null || stockDias < 0 ||
                minVentas == null || minVentas < 0 ||
                margenBajo == null || margenBajo < 0 ||
                margenAlto == null || margenAlto < 0 ||
                contrib == null || contrib !in 0.0..1.0 ||
                gastoRatio == null || gastoRatio !in 0.0..1.0
            ) {
                _ui.update { it.copy(error = "Revisa los umbrales (ratios ≥ 0; ratios 0–1).") }
                return@launch
            }
            _ui.update { it.copy(guardando = true, error = null, mensaje = null) }
            val opticaId = sessionManager.opticaId.first()
            val actual = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
                ?: ConfiguracionFinancieraEntity(opticaId = opticaId)
            configuracionFinancieraDao.upsert(
                actual.copy(
                    caidaVentasAlertaPct = caida,
                    deudaViejaAlertaDias = deudaDias,
                    deudaTotalAlertaMonto = deudaMonto,
                    stockEstancadoAlertaDias = stockDias,
                    minVentasParaRecomendar = minVentas,
                    margenBajoAlertaPct = margenBajo,
                    margenAltoAlertaPct = margenAlto,
                    contribucionAltaRatio = contrib,
                    gastoFueraRangoRatio = gastoRatio,
                ),
            )
            _ui.update { it.copy(guardando = false, mensaje = "Umbrales de alerta guardados.") }
        }
    }

    private suspend fun cargar() {
        val opticaId = sessionManager.opticaId.first()
        val c = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
        _ui.update {
            it.copy(
                caidaVentasAlertaPct = (c?.caidaVentasAlertaPct ?: 10.0).toPlain(),
                deudaViejaAlertaDias = (c?.deudaViejaAlertaDias ?: 30).toString(),
                deudaTotalAlertaMonto = (c?.deudaTotalAlertaMonto ?: 3000.0).toPlain(),
                stockEstancadoAlertaDias = (c?.stockEstancadoAlertaDias ?: 180).toString(),
                minVentasParaRecomendar = (c?.minVentasParaRecomendar ?: 5).toString(),
                margenBajoAlertaPct = (c?.margenBajoAlertaPct ?: 10.0).toPlain(),
                margenAltoAlertaPct = (c?.margenAltoAlertaPct ?: 35.0).toPlain(),
                contribucionAltaRatio = (c?.contribucionAltaRatio ?: 0.25).toPlain(),
                gastoFueraRangoRatio = (c?.gastoFueraRangoRatio ?: 0.4).toPlain(),
            )
        }
    }

    private fun Double.toPlain(): String =
        if (this == toLong().toDouble()) toLong().toString() else toString()
}
