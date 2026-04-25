package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaFiscalSettings
import com.example.optoapp.data.OpticaFiscalSettingsStore
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FiscalConfigUi(
    val opticaId: String = "",
    val docTipo: String = "RUC",
    val docNumero: String = "",
    val razonSocial: String = "",
    val direccionFiscal: String = "",
    val distritoCiudadDepartamento: String = "",
    val moneda: String = "",
    val pais: String = "",
    val contactoWhatsappTelefono: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FiscalConfigViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val membershipRepository: MembershipRepository,
    private val store: OpticaFiscalSettingsStore
) : ViewModel() {

    private val _status = kotlinx.coroutines.flow.MutableStateFlow(FiscalConfigUi())

    init {
        viewModelScope.launch {
            sessionManager.opticaId.collectLatest { oid ->
                syncFromServerForOptica(oid)
            }
        }
    }

    val uiState: StateFlow<FiscalConfigUi> =
        sessionManager.opticaId
            .flatMapLatest { oid ->
                store.settingsFlow(oid).map { settings ->
                    val status = _status.value
                    FiscalConfigUi(
                        opticaId = oid,
                        docTipo = settings.docTipo.ifBlank { "RUC" },
                        docNumero = settings.docNumero,
                        razonSocial = settings.razonSocial,
                        direccionFiscal = settings.direccionFiscal,
                        distritoCiudadDepartamento = settings.distritoCiudadDepartamento,
                        moneda = settings.moneda,
                        pais = settings.pais,
                        contactoWhatsappTelefono = settings.contactoWhatsappTelefono,
                        loading = status.loading,
                        message = status.message,
                        error = status.error
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FiscalConfigUi())

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

    fun save(
        docTipo: String,
        docNumero: String,
        razonSocial: String,
        direccionFiscal: String,
        distritoCiudadDepartamento: String,
        moneda: String,
        pais: String,
        contactoWhatsappTelefono: String
    ) {
        viewModelScope.launch {
            val tipo = docTipo.trim().uppercase()
            val numero = docNumero.trim()
            val razon = razonSocial.trim()
            val direccion = direccionFiscal.trim()
            val distrito = distritoCiudadDepartamento.trim()
            val monedaNormalized = moneda.trim()
            val paisNormalized = pais.trim()
            val contacto = contactoWhatsappTelefono.trim()
            if (tipo !in setOf("RUC", "RUS")) {
                _status.value = _status.value.copy(error = "Tipo fiscal inválido. Usa RUC o RUS.", message = null)
                return@launch
            }
            if (numero.isBlank() || razon.isBlank() || direccion.isBlank()) {
                _status.value = _status.value.copy(error = "Completa RUC/RUS, razón social y dirección.", message = null)
                return@launch
            }
            _status.value = _status.value.copy(loading = true, message = null, error = null)
            val oid = sessionManager.opticaId.first()
            val result = membershipRepository.updateOpticaFiscalSettings(
                opticaId = oid,
                docTipo = tipo,
                docNumero = numero,
                razonSocial = razon,
                direccionFiscal = direccion,
                distritoCiudadDepartamento = distrito,
                moneda = monedaNormalized,
                pais = paisNormalized,
                contactoWhatsappTelefono = contacto
            )
            if (result.isSuccess) {
                store.save(
                    oid,
                    OpticaFiscalSettings(
                        docTipo = tipo,
                        docNumero = numero,
                        razonSocial = razon,
                        direccionFiscal = direccion,
                        distritoCiudadDepartamento = distrito,
                        moneda = monedaNormalized,
                        pais = paisNormalized,
                        contactoWhatsappTelefono = contacto
                    )
                )
                _status.value = _status.value.copy(loading = false, message = "Datos fiscales guardados.", error = null)
            } else {
                val raw = result.exceptionOrNull()?.localizedMessage.orEmpty()
                val friendly = when {
                    raw.contains("Solo admin/gerente", ignoreCase = true) ->
                        "Solo admin o gerente pueden modificar datos fiscales."
                    raw.contains("permission", ignoreCase = true) || raw.contains("policy", ignoreCase = true) ->
                        "No tienes permisos para modificar datos fiscales."
                    raw.isBlank() -> "No se pudieron guardar los datos fiscales."
                    else -> raw
                }
                _status.value = _status.value.copy(loading = false, message = null, error = friendly)
            }
        }
    }
}
