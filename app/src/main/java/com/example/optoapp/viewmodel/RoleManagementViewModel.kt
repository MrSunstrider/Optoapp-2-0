package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaMemberRow
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoleManagementUiState(
    val members: List<OpticaMemberRow> = emptyList(),
    val emailInput: String = "",
    val roleInput: String = "especialista",
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class RoleManagementViewModel @Inject constructor(
    private val membershipRepository: MembershipRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleManagementUiState())
    val uiState: StateFlow<RoleManagementUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.update { it.copy(emailInput = value, error = null, message = null) }
    }

    fun updateRole(value: String) {
        _uiState.update { it.copy(roleInput = value, error = null, message = null) }
    }

    fun loadMembers() = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null, message = null) }
        val opticaId = sessionManager.opticaId.first()
        val rows = membershipRepository.fetchMembersForOptica(opticaId)
        _uiState.update { it.copy(members = rows, loading = false) }
    }

    fun assignRole() = viewModelScope.launch {
        val state = _uiState.value
        if (state.emailInput.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa un email válido.") }
            return@launch
        }
        _uiState.update { it.copy(loading = true, error = null, message = null) }
        val opticaId = sessionManager.opticaId.first()
        val result = membershipRepository.assignRoleByEmail(
            opticaId = opticaId,
            email = state.emailInput,
            rol = state.roleInput
        )
        if (result.isSuccess) {
            val rows = membershipRepository.fetchMembersForOptica(opticaId)
            _uiState.update {
                it.copy(
                    members = rows,
                    loading = false,
                    message = "Rol asignado correctamente.",
                    emailInput = ""
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message ?: "No se pudo asignar el rol."
                )
            }
        }
    }
}
