package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.opticasettings.BusinessHoursConfigJson
import com.example.optoapp.data.opticasettings.OpticaSettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class BusinessHoursConfigUi(
    val opticaId: String = "",
    val horario: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BusinessHoursConfigViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val membershipRepository: MembershipRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "BusinessHoursConfigVM"
    }

    private val _draft = MutableStateFlow("")
    private val _status = MutableStateFlow(BusinessHoursConfigUi())
    private var lastOpticaId: String = ""
    private var draftDirty: Boolean = false

    init {
        viewModelScope.launch {
            sessionManager.opticaId
                .flatMapLatest { oid ->
                    membershipRepository.getOpticaSettingsFlow(oid)
                }
                .collectLatest { settings ->
                    val oid = sessionManager.opticaId.first()
                    val hours = BusinessHoursConfigJson.extractBusinessHours(settings?.configJson.orEmpty())
                    if (oid != lastOpticaId || !draftDirty) {
                        _draft.value = hours
                        lastOpticaId = oid
                    }
                }
        }
    }

    val uiState: StateFlow<BusinessHoursConfigUi> =
        combine(sessionManager.opticaId, _draft, _status) { oid, draft, status ->
            BusinessHoursConfigUi(
                opticaId = oid,
                horario = draft,
                loading = status.loading,
                message = status.message,
                error = status.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessHoursConfigUi())

    fun updateHorario(value: String) {
        _draft.value = value
        draftDirty = true
        _status.value = _status.value.copy(message = null, error = null)
    }

    fun clearMessages() {
        _status.value = _status.value.copy(message = null, error = null)
    }

    fun syncFromServer() {
        viewModelScope.launch {
            val oid = sessionManager.opticaId.first()
            if (oid.isBlank()) return@launch
            membershipRepository.syncOpticaSettingsFromRemote(oid)
        }
    }

    fun save() {
        viewModelScope.launch {
            try {
                val oid = sessionManager.opticaId.first()
                if (oid.isBlank()) {
                    _status.value = _status.value.copy(error = "Sin óptica activa.", message = null)
                    return@launch
                }
                _status.value = _status.value.copy(loading = true, message = null, error = null)
                val existing = membershipRepository.getOpticaSettingsFlow(oid).first()
                val merged = BusinessHoursConfigJson.mergeBusinessHours(
                    existing?.configJson.orEmpty(),
                    _draft.value.trim(),
                )
                val entity = OpticaSettingsEntity(opticaId = oid, configJson = merged)
                membershipRepository.upsertOpticaSettings(entity)
                val remote = membershipRepository.upsertOpticaSettingsRemote(oid, merged)
                if (remote.isSuccess) {
                    draftDirty = false
                    _status.value = _status.value.copy(
                        loading = false,
                        message = "Horario de atención guardado.",
                        error = null,
                    )
                } else {
                    val raw = remote.exceptionOrNull()?.localizedMessage.orEmpty()
                    _status.value = _status.value.copy(
                        loading = false,
                        message = null,
                        error = raw.ifBlank { "No se pudo guardar el horario en la nube." },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "save failed: IO", e)
                _status.value = _status.value.copy(
                    loading = false,
                    message = null,
                    error = "Error inesperado. Reintente más tarde.",
                )
            } catch (e: Exception) {
                Log.e(TAG, "save failed", e)
                _status.value = _status.value.copy(
                    loading = false,
                    message = null,
                    error = "Error inesperado. Reintente más tarde.",
                )
            }
        }
    }
}
