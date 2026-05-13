package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaFiscalSettingsStore
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class OpticaHeaderUi(
    val nombreOptica: String = "Óptica",
    val fiscalEtiqueta: String = "Sin documento fiscal"
)

@HiltViewModel
class OpticaHeaderViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val membershipRepository: MembershipRepository,
    private val fiscalStore: OpticaFiscalSettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpticaHeaderUi())
    val uiState: StateFlow<OpticaHeaderUi> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.opticaId.collectLatest { opticaId ->
                val summary = membershipRepository.fetchOpticaHeaderSummary(opticaId)
                _uiState.value = if (summary == null) {
                    val local = fiscalStore.settingsFlow(opticaId).first()
                    val nombreLocal = local.nombreComercial.trim()
                        .ifBlank { local.razonSocial.trim() }
                        .ifBlank { "Óptica sin nombre" }
                    val fiscalLocal = if (local.docTipo.isBlank() || local.docNumero.isBlank()) {
                        "Sin documento fiscal"
                    } else {
                        "${local.docTipo} ${local.docNumero}"
                    }
                    OpticaHeaderUi(nombreOptica = nombreLocal, fiscalEtiqueta = fiscalLocal)
                } else {
                    OpticaHeaderUi(
                        nombreOptica = summary.nombreOptica,
                        fiscalEtiqueta = summary.fiscalEtiqueta
                    )
                }
            }
        }
    }
}
