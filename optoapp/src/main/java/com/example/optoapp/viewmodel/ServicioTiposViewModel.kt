package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.serviciotipo.ServicioTipoDao
import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import com.example.optoapp.domain.ServicioTipoCatalogUseCase
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.MontoDraftFormatting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class FormServicioTipo(
    val original: ServicioTipoEntity? = null,
    val nombre: String = "",
    val costoPropio: String = "",
    val costoTercero: String = "",
    val nombreError: String? = null,
    val costoError: String? = null,
)

data class ServicioTiposUiState(
    val tipos: List<ServicioTipoEntity> = emptyList(),
    val puedeEditar: Boolean = false,
    val form: FormServicioTipo? = null,
    val error: String? = null,
) {
    val muestraCrearCta: Boolean
        get() = tipos.isEmpty() && puedeEditar
}

@HiltViewModel
class ServicioTiposViewModel @Inject constructor(
    private val servicioTipoDao: ServicioTipoDao,
    private val catalogo: ServicioTipoCatalogUseCase,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServicioTiposUiState())
    val uiState: StateFlow<ServicioTiposUiState> = _uiState.asStateFlow()

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        val tipos = sessionManager.opticaId.flatMapLatest { servicioTipoDao.observeByOptica(it) }
        viewModelScope.launch {
            combine(tipos, sessionManager.opticaRol) { lista, rol -> lista to AppRoles.canWriteGastos(rol) }
                .collect { (lista, puedeEditar) -> _uiState.update { it.copy(tipos = lista, puedeEditar = puedeEditar) } }
        }
    }

    fun nuevo() {
        if (!_uiState.value.puedeEditar) return
        _uiState.update { it.copy(form = FormServicioTipo(), error = null) }
    }

    fun editar(tipo: ServicioTipoEntity) {
        if (!_uiState.value.puedeEditar) return
        _uiState.update {
            it.copy(
                form = FormServicioTipo(
                    original = tipo,
                    nombre = tipo.nombre,
                    costoPropio = MontoDraftFormatting.formatDraft(tipo.costoPropio),
                    costoTercero = MontoDraftFormatting.formatDraft(tipo.costoTercero),
                ),
                error = null,
            )
        }
    }

    fun cambiarForm(cambio: (FormServicioTipo) -> FormServicioTipo) {
        _uiState.update { s -> s.copy(form = s.form?.let(cambio)) }
    }

    fun cerrarForm() {
        _uiState.update { it.copy(form = null) }
    }

    fun guardar() {
        val form = _uiState.value.form ?: return
        if (!_uiState.value.puedeEditar) return
        val costoPropio = costoValido(form.costoPropio)
        val costoTercero = costoValido(form.costoTercero)
        val nombreError = if (form.nombre.isBlank()) ERROR_NOMBRE else null
        val costoError = if (costoPropio == null || costoTercero == null) ERROR_COSTO else null
        if (nombreError != null || costoError != null) {
            _uiState.update { it.copy(form = form.copy(nombreError = nombreError, costoError = costoError)) }
            return
        }
        ejecutar { opticaId ->
            val base = form.original ?: ServicioTipoEntity(id = UUID.randomUUID().toString(), opticaId = opticaId, nombre = "")
            catalogo.guardar(
                base.copy(nombre = form.nombre.trim(), costoPropio = costoPropio!!, costoTercero = costoTercero!!),
            )
            _uiState.update { s -> s.copy(form = null) }
        }
    }

    fun desactivar(tipo: ServicioTipoEntity) {
        if (!_uiState.value.puedeEditar) return
        ejecutar { catalogo.desactivar(tipo.id, it) }
    }

    fun reactivar(tipo: ServicioTipoEntity) {
        if (!_uiState.value.puedeEditar) return
        ejecutar { catalogo.reactivar(tipo.id, it) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun ejecutar(accion: suspend (opticaId: String) -> Unit) {
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                accion(opticaId)
                postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Catálogo de servicios", e)
                _uiState.update { it.copy(error = ERROR_GUARDAR) }
            }
        }
    }

    // A blank cost means "not set"; the catalog stores it as 0 so the preload leaves the field empty.
    private fun costoValido(draft: String): Double? {
        if (draft.isBlank()) return 0.0
        return MontoDraftFormatting.parseDraft(draft)?.takeIf { it >= 0.0 }
    }

    companion object {
        private const val TAG = "ServicioTiposViewModel"
        const val ERROR_NOMBRE = "Ingresa el nombre del servicio."
        const val ERROR_COSTO = "Los costos deben ser números iguales o mayores a 0."
        const val ERROR_GUARDAR = "No se pudo guardar el tipo de servicio. Inténtalo de nuevo."
    }
}
