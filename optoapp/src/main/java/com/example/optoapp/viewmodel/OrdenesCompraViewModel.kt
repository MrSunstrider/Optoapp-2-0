package com.example.optoapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OrdenCompra
import com.example.optoapp.data.OrdenCompraItem
import com.example.optoapp.data.OrdenCompraRepository
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class OCEditFormState(
    val id: String? = null,
    val numero: String = "",
    val proveedorId: String = "",
    val fecha: LocalDate = LocalDate.now(),
    val estado: String = "PENDIENTE",
    val items: List<OCItemFormState> = emptyList(),
)

data class OCItemFormState(
    val id: String? = null,
    val monturaId: String = "",
    val cantidad: String = "0",
    val costoUnitario: String = "0.0",
    val recibido: String = "0",
)

data class OrdenesCompraUiState(
    val query: String = "",
    val form: OCEditFormState = OCEditFormState(),
    val editing: Boolean = false,
    val viewingId: String? = null,
    val error: String? = null,
    val success: String? = null,
)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OrdenesCompraViewModel @Inject constructor(
    private val repository: OrdenCompraRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    companion object {
        private const val TAG = "OrdenesCompraVM"
    }

    private val _uiState = MutableStateFlow(OrdenesCompraUiState())
    val uiState: StateFlow<OrdenesCompraUiState> = _uiState

    val ordenesCompra = combine(_uiState, sessionManager.opticaId) { state, opticaId ->
        state.query to opticaId
    }.flatMapLatest { (query, opticaId) ->
        repository.getByOptica(opticaId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun updateForm(update: (OCEditFormState) -> OCEditFormState) {
        _uiState.update { it.copy(form = update(it.form), error = null, success = null) }
    }

    fun startCreate() {
        _uiState.update {
            it.copy(editing = true, form = OCEditFormState(), error = null, success = null)
        }
    }

    fun startEdit(oc: OrdenCompra) {
        viewModelScope.launch {
            val items = repository.getItems(oc.id)
            _uiState.update {
                it.copy(
                    editing = true,
                    error = null,
                    success = null,
                    form = OCEditFormState(
                        id = oc.id,
                        numero = oc.numero,
                        proveedorId = oc.proveedorId,
                        fecha = oc.fecha,
                        estado = oc.estado,
                        items = items.map { item ->
                            OCItemFormState(
                                id = item.id,
                                monturaId = item.monturaId,
                                cantidad = item.cantidad.toString(),
                                costoUnitario = item.costoUnitario.toString(),
                                recibido = item.recibido.toString(),
                            )
                        },
                    ),
                )
            }
        }
    }

    fun viewDetail(ocId: String) {
        _uiState.update { it.copy(viewingId = ocId) }
    }

    fun closeDetail() {
        _uiState.update { it.copy(viewingId = null) }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editing = false, form = OCEditFormState(), error = null) }
    }

    fun addItem() {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    items = it.form.items + OCItemFormState(),
                ),
            )
        }
    }

    fun removeItem(index: Int) {
        _uiState.update {
            val newItems = it.form.items.toMutableList()
            if (index in newItems.indices) newItems.removeAt(index)
            it.copy(form = it.form.copy(items = newItems))
        }
    }

    fun updateItem(index: Int, update: (OCItemFormState) -> OCItemFormState) {
        _uiState.update {
            val newItems = it.form.items.toMutableList()
            if (index in newItems.indices) {
                newItems[index] = update(newItems[index])
            }
            it.copy(form = it.form.copy(items = newItems))
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val form = state.form
            if (form.proveedorId.isBlank()) {
                _uiState.update { it.copy(error = "Debe seleccionar un proveedor.") }
                return@launch
            }
            if (form.items.isEmpty()) {
                _uiState.update { it.copy(error = "Debe agregar al menos un ítem.") }
                return@launch
            }
            try {
                val opticaId = sessionManager.opticaId.first()
                val id = form.id ?: UUID.randomUUID().toString()
                val ocItems = form.items.mapIndexed { index, itemForm ->
                    OrdenCompraItem(
                        id = itemForm.id ?: UUID.randomUUID().toString(),
                        ordenId = id,
                        monturaId = itemForm.monturaId.ifBlank {
                            throw IllegalStateException("Montura requerida para el ítem ${index + 1}")
                        },
                        cantidad = itemForm.cantidad.toIntOrNull() ?: 0,
                        costoUnitario = itemForm.costoUnitario.toDoubleOrNull() ?: 0.0,
                        recibido = itemForm.recibido.toIntOrNull() ?: 0,
                    )
                }
                val oc = OrdenCompra(
                    id = id,
                    numero = form.numero,
                    proveedorId = form.proveedorId,
                    fecha = form.fecha,
                    estado = form.estado,
                    opticaId = opticaId,
                )
                if (form.id != null) {
                    repository.update(oc)
                    ocItems.forEach { repository.upsertItem(it) }
                } else {
                    repository.create(oc, ocItems)
                }
                _uiState.update {
                    it.copy(
                        editing = false,
                        form = OCEditFormState(),
                        error = null,
                        success = "Orden de compra guardada",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "save failed", e)
                _uiState.update { it.copy(error = "Error inesperado. Reintente más tarde.") }
            }
        }
    }

    fun updateEstado(ocId: String, newEstado: String) {
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                repository.updateEstado(ocId, newEstado, opticaId)
                _uiState.update { it.copy(success = "Estado actualizado a $newEstado") }
            } catch (e: Exception) {
                Log.e(TAG, "updateEstado failed", e)
                _uiState.update { it.copy(error = "Error inesperado. Reintente más tarde.") }
            }
        }
    }

    fun delete(ocId: String) {
        viewModelScope.launch {
            try {
                val opticaId = sessionManager.opticaId.first()
                repository.delete(ocId, opticaId)
                _uiState.update { it.copy(success = "Orden cancelada") }
            } catch (e: Exception) {
                Log.e(TAG, "delete failed", e)
                _uiState.update { it.copy(error = "Error inesperado. Reintente más tarde.") }
            }
        }
    }

    suspend fun loadItems(ocId: String): List<OrdenCompraItem> = repository.getItems(ocId)
}
