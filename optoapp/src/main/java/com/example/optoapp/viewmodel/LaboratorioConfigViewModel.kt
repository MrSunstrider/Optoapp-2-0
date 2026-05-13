package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaLaboratorioSettings
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

data class LaboratorioConfigUi(
    val opticaId: String = "",
    val laboratorioNombre: String = "",
    val laboratorioContacto: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LaboratorioConfigViewModel @Inject constructor(
    private val settings: OpticaLaboratorioSettings,
    private val sessionManager: SessionManager,
    private val membershipRepository: MembershipRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            sessionManager.opticaId.collectLatest { oid ->
                syncFromServerForOptica(oid)
            }
        }
    }

    /** Vuelve a leer Supabase y actualiza caché local (p. ej. al abrir Configuración si la sesión aún no estaba lista en el arranque). */
    fun syncFromServer() {
        viewModelScope.launch {
            val oid = sessionManager.opticaId.first()
            syncFromServerForOptica(oid)
        }
    }

    private suspend fun syncFromServerForOptica(oid: String) {
        val remote = membershipRepository.fetchOpticaLaboratorioSettings(oid) ?: return
        settings.save(oid, remote.first, remote.second)
    }

    val uiState: StateFlow<LaboratorioConfigUi> =
        sessionManager.opticaId
            .flatMapLatest { oid ->
                settings.settingsFlow(oid).map { (nombre, contacto) ->
                    LaboratorioConfigUi(oid, nombre, contacto)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                LaboratorioConfigUi()
            )

    fun save(laboratorioNombre: String, laboratorioContacto: String) {
        viewModelScope.launch {
            val oid = sessionManager.opticaId.first()
            membershipRepository.updateOpticaLaboratorioSettings(oid, laboratorioNombre, laboratorioContacto)
            settings.save(oid, laboratorioNombre, laboratorioContacto)
        }
    }
}
