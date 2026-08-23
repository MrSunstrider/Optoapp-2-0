package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.domain.ConfiguracionFinancieraDraft
import com.example.optoapp.domain.ConfiguracionFinancieraPolicy
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfiguracionFinancieraUiState(
    val draft: ConfiguracionFinancieraDraft = ConfiguracionFinancieraDraft(),
    val opticaId: String = "",
    val rol: String = "",
    val saveEnabled: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConfiguracionFinancieraViewModel @Inject constructor(
    private val dao: ConfiguracionFinancieraDao,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracionFinancieraUiState())
    val uiState: StateFlow<ConfiguracionFinancieraUiState> = _uiState.asStateFlow()

    private var draftDirty: Boolean = false
    private var lastOpticaId: String = ""

    init {
        viewModelScope.launch {
            combine(sessionManager.opticaId, sessionManager.opticaRol) { oid, rol ->
                oid to rol
            }.collect { (oid, rol) ->
                _uiState.update { cur ->
                    cur.copy(
                        opticaId = oid,
                        rol = rol,
                        saveEnabled = ConfiguracionFinancieraPolicy.canWrite(rol) && !cur.saving,
                    )
                }
            }
        }
        viewModelScope.launch {
            sessionManager.opticaId
                .flatMapLatest { oid ->
                    dao.getByOpticaId(oid).map { entity -> oid to entity }
                }
                .collect { (oid, entity) ->
                    val shouldReset = oid != lastOpticaId || !draftDirty
                    if (shouldReset) {
                        _uiState.update {
                            it.copy(
                                draft = entity?.let { e -> ConfiguracionFinancieraDraft.fromEntity(e) }
                                    ?: ConfiguracionFinancieraDraft(),
                                opticaId = oid,
                            )
                        }
                        lastOpticaId = oid
                        draftDirty = false
                    }
                }
        }
    }

    fun updateDraft(draft: ConfiguracionFinancieraDraft) {
        draftDirty = true
        _uiState.update { it.copy(draft = draft, error = null, message = null) }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!ConfiguracionFinancieraPolicy.canWrite(state.rol)) {
                _uiState.update { it.copy(error = "Sin permiso para guardar configuración financiera") }
                return@launch
            }
            val validationError = ConfiguracionFinancieraPolicy.validate(state.draft)
            if (validationError != null) {
                _uiState.update { it.copy(error = validationError) }
                return@launch
            }
            val opticaId = state.opticaId
            if (opticaId.isBlank()) {
                _uiState.update { it.copy(error = "Óptica no disponible") }
                return@launch
            }
            _uiState.update {
                it.copy(
                    saving = true,
                    saveEnabled = false,
                    error = null,
                )
            }
            try {
                dao.upsert(state.draft.toEntity(opticaId))
                draftDirty = false
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
                _uiState.update {
                    it.copy(
                        saving = false,
                        saveEnabled = ConfiguracionFinancieraPolicy.canWrite(it.rol),
                        message = "Configuración guardada",
                        error = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        saving = false,
                        saveEnabled = ConfiguracionFinancieraPolicy.canWrite(it.rol),
                        error = e.message ?: "No se pudo guardar",
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
