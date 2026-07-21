package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaFiscalSettingsStore
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.opticasettings.OpticaSettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class OpticaHeaderUi(
    val nombreOptica: String = "Óptica",
    val fiscalEtiqueta: String = "Sin documento fiscal",
    val horarioAtencion: String = "",
)

@HiltViewModel
class OpticaHeaderViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val membershipRepository: MembershipRepository,
    private val fiscalStore: OpticaFiscalSettingsStore,
    private val opticaSettingsDao: OpticaSettingsDao,
) : ViewModel() {

    companion object {
        private const val TAG = "OpticaHeaderVM"
    }

    private val _uiState = MutableStateFlow(OpticaHeaderUi())
    val uiState: StateFlow<OpticaHeaderUi> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.opticaId.collectLatest { opticaId ->
                val summary = membershipRepository.fetchOpticaHeaderSummary(opticaId)
                val base = if (summary == null) {
                    val local = fiscalStore.settingsFlow(opticaId).first()
                    OpticaHeaderUi(
                        nombreOptica = local.nombreComercial.trim()
                            .ifBlank { local.razonSocial.trim() }
                            .ifBlank { "Óptica sin nombre" },
                        fiscalEtiqueta = if (local.docTipo.isBlank() || local.docNumero.isBlank()) {
                            "Sin documento fiscal"
                        } else {
                            "${local.docTipo} ${local.docNumero}"
                        },
                    )
                } else {
                    OpticaHeaderUi(
                        nombreOptica = summary.nombreOptica,
                        fiscalEtiqueta = summary.fiscalEtiqueta,
                    )
                }
                _uiState.value = base
            }
        }

        // Observe optica_settings to extract business_hours for horarioAtencion
        viewModelScope.launch {
            sessionManager.opticaId.collectLatest { opticaId ->
                opticaSettingsDao.getByOpticaId(opticaId).collectLatest { settings ->
                    val hours = if (settings != null) {
                        try {
                            JSONObject(settings.configJson)
                                .optString("business_hours", "")
                                .trim()
                        } catch (e: Exception) {
                            Log.w(TAG, "Malformed configJson for optica $opticaId: ${e.message}")
                            ""
                        }
                    } else {
                        ""
                    }
                    _uiState.value = _uiState.value.copy(horarioAtencion = hours)
                }
            }
        }
    }
}
