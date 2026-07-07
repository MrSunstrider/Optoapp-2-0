package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.MonturaProveedor
import com.example.optoapp.data.Proveedor
import com.example.optoapp.data.ProveedorRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.SyncProveedoresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProveedorFormState(
    val id: String? = null,
    val nombre: String = "",
    val ruc: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val contacto: String = "",
    val activo: Boolean = true
)

data class ProveedoresUiState(
    val query: String = "",
    val form: ProveedorFormState = ProveedorFormState(),
    val editing: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProveedoresViewModel @Inject constructor(
    private val repository: ProveedorRepository,
    private val sessionManager: SessionManager,
    private val syncProveedoresUseCase: SyncProveedoresUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "ProveedoresViewModel"
    }

    private val _uiState = MutableStateFlow(ProveedoresUiState())
    val uiState: StateFlow<ProveedoresUiState> = _uiState

    val proveedores = combine(_uiState, sessionManager.opticaId) { state, opticaId ->
        state.query to opticaId
    }.flatMapLatest { (query, opticaId) ->
        repository.getActivosByOptica(opticaId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun updateForm(update: (ProveedorFormState) -> ProveedorFormState) {
        _uiState.update { it.copy(form = update(it.form), error = null, success = null) }
    }

    fun startCreate() {
        _uiState.update { it.copy(editing = true, form = ProveedorFormState(), error = null, success = null) }
    }

    fun startEdit(proveedor: Proveedor) {
        _uiState.update {
            it.copy(
                editing = true,
                error = null,
                success = null,
                form = ProveedorFormState(
                    id = proveedor.id,
                    nombre = proveedor.nombre,
                    ruc = proveedor.ruc,
                    telefono = proveedor.telefono,
                    email = proveedor.email,
                    direccion = proveedor.direccion,
                    contacto = proveedor.contacto,
                    activo = proveedor.activo
                )
            )
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editing = false, form = ProveedorFormState(), error = null) }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val form = state.form
            if (form.nombre.isBlank()) {
                _uiState.update { it.copy(error = "Nombre es obligatorio.") }
                return@launch
            }
            if (form.ruc.isBlank()) {
                _uiState.update { it.copy(error = "RUC es obligatorio.") }
                return@launch
            }
            try {
                val opticaId = sessionManager.opticaId.first()
                val proveedor = Proveedor(
                    id = form.id ?: UUID.randomUUID().toString(),
                    nombre = form.nombre.trim(),
                    ruc = form.ruc.trim(),
                    telefono = form.telefono.trim(),
                    email = form.email.trim(),
                    direccion = form.direccion.trim(),
                    contacto = form.contacto.trim(),
                    activo = form.activo,
                    opticaId = opticaId
                )
                if (form.id != null) {
                    repository.update(proveedor)
                } else {
                    repository.insert(proveedor)
                }
                _uiState.update {
                    it.copy(editing = false, form = ProveedorFormState(), error = null,
                        success = "Proveedor guardado")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "save failed", e)
                val msg = if (e.message?.contains("UNIQUE") == true) "El RUC ya existe para esta óptica."
                          else "Error inesperado. Reintente más tarde."
                _uiState.update { it.copy(error = msg) }
            }
        }
    }

    fun delete(proveedor: Proveedor) {
        viewModelScope.launch {
            repository.softDelete(proveedor.id)
            _uiState.update { it.copy(success = "Proveedor desactivado", error = null) }
        }
    }

    fun linkMontura(proveedorId: String, monturaId: String, costoProveedor: Double, precioSugerido: Double) {
        viewModelScope.launch {
            try {
                val link = MonturaProveedor(
                    id = UUID.randomUUID().toString(),
                    monturaId = monturaId,
                    proveedorId = proveedorId,
                    costoProveedor = costoProveedor,
                    precioSugerido = precioSugerido
                )
                repository.linkMonturaProveedor(link)
                _uiState.update { it.copy(success = "Montura vinculada al proveedor") }
            } catch (e: Exception) {
                Log.e(TAG, "linkMontura failed", e)
                _uiState.update { it.copy(error = "Error inesperado. Reintente más tarde.") }
            }
        }
    }
}
