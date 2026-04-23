package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.PlanSettings
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanManagementUiState(
    val planCode: String = "free",
    val maxOpticasInput: String = "",
    val maxPacientesInput: String = "",
    val maxUsuariosInput: String = "",
    val planStatus: String = "active",
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class PlanManagementViewModel @Inject constructor(
    private val membershipRepository: MembershipRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanManagementUiState())
    val uiState: StateFlow<PlanManagementUiState> = _uiState.asStateFlow()

    fun load() = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null, message = null) }
        val opticaId = sessionManager.opticaId.first()
        val result = membershipRepository.fetchPlanSettings(opticaId)
        if (result.isSuccess) {
            val s = result.getOrNull()!!
            _uiState.update {
                it.copy(
                    planCode = s.planCode,
                    maxOpticasInput = s.maxOpticas?.toString().orEmpty(),
                    maxPacientesInput = s.maxPacientesPorOptica?.toString().orEmpty(),
                    maxUsuariosInput = s.maxUsuariosPorOptica?.toString().orEmpty(),
                    planStatus = s.planStatus,
                    loading = false
                )
            }
        } else {
            _uiState.update { it.copy(loading = false, error = result.exceptionOrNull()?.message ?: "No se pudo cargar plan.") }
        }
    }

    fun updatePlanCode(value: String) {
        _uiState.update { it.copy(planCode = value, error = null, message = null) }
    }

    fun updatePlanStatus(value: String) {
        _uiState.update { it.copy(planStatus = value, error = null, message = null) }
    }

    fun updateMaxOpticas(value: String) {
        _uiState.update { it.copy(maxOpticasInput = value.filter { ch -> ch.isDigit() }, error = null, message = null) }
    }

    fun updateMaxPacientes(value: String) {
        _uiState.update { it.copy(maxPacientesInput = value.filter { ch -> ch.isDigit() }, error = null, message = null) }
    }

    fun updateMaxUsuarios(value: String) {
        _uiState.update { it.copy(maxUsuariosInput = value.filter { ch -> ch.isDigit() }, error = null, message = null) }
    }

    fun applyPreset() {
        _uiState.update { state ->
            when (state.planCode) {
                "free" -> state.copy(maxOpticasInput = "1", maxPacientesInput = "20", maxUsuariosInput = "2")
                "pro_individual" -> state.copy(maxOpticasInput = "1", maxPacientesInput = "", maxUsuariosInput = "5")
                "pro_multisite_15" -> state.copy(maxOpticasInput = "15", maxPacientesInput = "", maxUsuariosInput = "100")
                "enterprise" -> state.copy(maxOpticasInput = "", maxPacientesInput = "", maxUsuariosInput = "")
                else -> state
            }
        }
    }

    fun save() = viewModelScope.launch {
        val s = _uiState.value
        _uiState.update { it.copy(loading = true, error = null, message = null) }
        val opticaId = sessionManager.opticaId.first()
        val settings = PlanSettings(
            planCode = s.planCode,
            maxOpticas = s.maxOpticasInput.toIntOrNull(),
            maxPacientesPorOptica = s.maxPacientesInput.toIntOrNull(),
            maxUsuariosPorOptica = s.maxUsuariosInput.toIntOrNull(),
            planStatus = s.planStatus
        )
        val result = membershipRepository.updatePlanSettings(opticaId, settings)
        if (result.isSuccess) {
            _uiState.update { it.copy(loading = false, message = "Plan actualizado correctamente.") }
        } else {
            _uiState.update { it.copy(loading = false, error = result.exceptionOrNull()?.message ?: "No se pudo actualizar plan.") }
        }
    }
}
