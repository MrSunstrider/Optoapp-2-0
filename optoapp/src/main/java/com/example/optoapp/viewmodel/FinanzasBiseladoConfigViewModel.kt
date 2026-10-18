package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.domain.OrigenBiselado
import com.example.optoapp.util.MontoDraftFormatting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinanzasBiseladoConfigUi(
    val origenDefault: String = OrigenBiselado.TERCERO,
    val costoPropioDraft: String = "0",
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null,
)

@HiltViewModel
class FinanzasBiseladoConfigViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao,
) : ViewModel() {

    private val _ui = MutableStateFlow(FinanzasBiseladoConfigUi())
    val ui: StateFlow<FinanzasBiseladoConfigUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch { cargar() }
    }

    fun cambiarOrigenDefault(origen: String) {
        _ui.update { it.copy(origenDefault = OrigenBiselado.normalize(origen), mensaje = null, error = null) }
    }

    fun cambiarCostoPropio(draft: String) {
        _ui.update { it.copy(costoPropioDraft = draft, mensaje = null, error = null) }
    }

    fun guardar() {
        viewModelScope.launch {
            val costo = MontoDraftFormatting.parseDraft(_ui.value.costoPropioDraft)
            if (costo == null || costo < 0.0) {
                _ui.update { it.copy(error = "Ingresa un costo propio válido (≥ 0).") }
                return@launch
            }
            _ui.update { it.copy(guardando = true, error = null, mensaje = null) }
            val opticaId = sessionManager.opticaId.first()
            val actual = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
                ?: ConfiguracionFinancieraEntity(opticaId = opticaId)
            configuracionFinancieraDao.upsert(
                actual.copy(
                    origenBiseladoDefault = OrigenBiselado.normalize(_ui.value.origenDefault),
                    costoBiseladoPropio = costo,
                ),
            )
            _ui.update { it.copy(guardando = false, mensaje = "Configuración de biselado guardada.") }
        }
    }

    private suspend fun cargar() {
        val opticaId = sessionManager.opticaId.first()
        val config = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
        _ui.update {
            it.copy(
                origenDefault = OrigenBiselado.normalize(config?.origenBiseladoDefault),
                costoPropioDraft = MontoDraftFormatting.formatDraft(config?.costoBiseladoPropio ?: 0.0),
            )
        }
    }
}
