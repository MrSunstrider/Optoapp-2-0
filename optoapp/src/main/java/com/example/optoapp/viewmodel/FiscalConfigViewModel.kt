package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaFiscalSettings
import com.example.optoapp.data.OpticaFiscalSettingsStore
import com.example.optoapp.data.SessionManager
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class FiscalConfigUi(
    val opticaId: String = "",
    val nombreComercial: String = "",
    val docTipo: String = "RUC",
    val docNumero: String = "",
    val razonSocial: String = "",
    val direccionFiscal: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

data class FiscalDraft(
    val nombreComercial: String = "",
    val docTipo: String = "RUC",
    val docNumero: String = "",
    val razonSocial: String = "",
    val direccionFiscal: String = "",
) {
    fun toSettings(): OpticaFiscalSettings = OpticaFiscalSettings(
        nombreComercial = nombreComercial,
        docTipo = docTipo,
        docNumero = docNumero,
        razonSocial = razonSocial,
        direccionFiscal = direccionFiscal,
    )

    companion object {
        fun fromSettings(settings: OpticaFiscalSettings): FiscalDraft = FiscalDraft(
            nombreComercial = settings.nombreComercial,
            docTipo = settings.docTipo.ifBlank { "RUC" },
            docNumero = settings.docNumero,
            razonSocial = settings.razonSocial,
            direccionFiscal = settings.direccionFiscal,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FiscalConfigViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val membershipRepository: MembershipRepository,
    private val store: OpticaFiscalSettingsStore,
) : ViewModel() {

    companion object {
        private const val TAG = "FiscalConfigVM"
    }

    private val _status = MutableStateFlow(FiscalConfigUi())
    private val _draft = MutableStateFlow(FiscalDraft())
    private var lastDraftOpticaId: String = ""
    private var draftDirty: Boolean = false

    init {
        viewModelScope.launch {
            sessionManager.opticaId.collectLatest { oid ->
                syncFromServerForOptica(oid)
            }
        }
        viewModelScope.launch {
            sessionManager.opticaId
                .flatMapLatest { oid ->
                    store.settingsFlow(oid).map { settings -> oid to settings }
                }
                .collectLatest { (oid, settings) ->
                    val shouldResetDraft = oid != lastDraftOpticaId || !draftDirty
                    if (shouldResetDraft) {
                        _draft.value = FiscalDraft.fromSettings(settings)
                        lastDraftOpticaId = oid
                    }
                }
        }
    }

    val uiState: StateFlow<FiscalConfigUi> =
        combine(sessionManager.opticaId, _draft, _status) { oid, draft, status ->
            FiscalConfigUi(
                opticaId = oid,
                nombreComercial = draft.nombreComercial,
                docTipo = draft.docTipo.ifBlank { "RUC" },
                docNumero = draft.docNumero,
                razonSocial = draft.razonSocial,
                direccionFiscal = draft.direccionFiscal,
                loading = status.loading,
                message = status.message,
                error = status.error,
            )
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FiscalConfigUi())

    fun updateDraft(
        nombreComercial: String? = null,
        docTipo: String? = null,
        docNumero: String? = null,
        razonSocial: String? = null,
        direccionFiscal: String? = null,
    ) {
        _draft.value = _draft.value.copy(
            nombreComercial = nombreComercial ?: _draft.value.nombreComercial,
            docTipo = docTipo ?: _draft.value.docTipo,
            docNumero = docNumero ?: _draft.value.docNumero,
            razonSocial = razonSocial ?: _draft.value.razonSocial,
            direccionFiscal = direccionFiscal ?: _draft.value.direccionFiscal,
        )
        draftDirty = true
    }

    fun clearMessages() {
        _status.value = _status.value.copy(message = null, error = null)
    }

    fun syncFromServer() {
        viewModelScope.launch {
            val oid = sessionManager.opticaId.first()
            syncFromServerForOptica(oid)
        }
    }

    private suspend fun syncFromServerForOptica(opticaId: String) {
        val remote = membershipRepository.fetchOpticaFiscalSettings(opticaId) ?: return
        store.save(opticaId, remote)
    }

    fun save() {
        viewModelScope.launch {
            try {
                val draft = _draft.value
                val tipo = draft.docTipo.trim().uppercase()
                val nombre = draft.nombreComercial.trim()
                val numero = draft.docNumero.trim()
                val razon = draft.razonSocial.trim()
                val direccion = draft.direccionFiscal.trim()
                if (tipo !in setOf("RUC", "RUS")) {
                    _status.value = _status.value.copy(error = "Tipo fiscal inválido. Usa RUC o RUS.", message = null)
                    return@launch
                }
                if (nombre.isBlank() || numero.isBlank() || razon.isBlank() || direccion.isBlank()) {
                    _status.value = _status.value.copy(
                        error = "Completa razón comercial, RUC/RUS, razón social y dirección.",
                        message = null,
                    )
                    return@launch
                }
                _status.value = _status.value.copy(loading = true, message = null, error = null)
                val oid = sessionManager.opticaId.first()
                val result = membershipRepository.updateOpticaFiscalSettings(
                    opticaId = oid,
                    nombreComercial = nombre,
                    docTipo = tipo,
                    docNumero = numero,
                    razonSocial = razon,
                    direccionFiscal = direccion,
                )
                if (result.isSuccess) {
                    store.save(
                        oid,
                        OpticaFiscalSettings(
                            nombreComercial = nombre,
                            docTipo = tipo,
                            docNumero = numero,
                            razonSocial = razon,
                            direccionFiscal = direccion,
                        ),
                    )
                    draftDirty = false
                    _status.value = _status.value.copy(loading = false, message = "Datos fiscales guardados.", error = null)
                } else {
                    val raw = result.exceptionOrNull()?.localizedMessage.orEmpty()
                    val friendly = when {
                        raw.contains("Solo admin/gerente", ignoreCase = true) ->
                            "Solo admin o gerente pueden modificar datos fiscales."
                        raw.contains("permission", ignoreCase = true) || raw.contains("policy", ignoreCase = true) ->
                            "No tienes permisos para modificar datos fiscales."
                        raw.contains("no confirmó la persistencia", ignoreCase = true) ||
                            raw.contains("no se pudo verificar la actualización", ignoreCase = true) ->
                            "No se pudo confirmar el guardado en la nube. Revisa permisos de esta óptica e inténtalo de nuevo."
                        raw.isBlank() -> "No se pudieron guardar los datos fiscales."
                        else -> raw
                    }
                    _status.value = _status.value.copy(loading = false, message = null, error = friendly)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "save failed: IO error", e)
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
