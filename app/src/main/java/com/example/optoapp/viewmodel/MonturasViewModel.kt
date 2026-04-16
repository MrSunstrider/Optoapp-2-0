package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class MonturaFormState(
    val id: String? = null,
    val sku: String = "",
    val marca: String = "",
    val modelo: String = "",
    val color: String = "",
    val talla: String = "",
    val costo: String = "",
    val precio: String = "",
    val stockActual: String = "",
    val stockMinimo: String = "",
    val activo: Boolean = true
)

data class MonturasUiState(
    val query: String = "",
    val form: MonturaFormState = MonturaFormState(),
    val editing: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MonturasViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(MonturasUiState())
    val uiState: StateFlow<MonturasUiState> = _uiState

    val monturas = combine(_uiState, sessionManager.opticaId) { state, opticaId ->
        state.query to opticaId
    }.flatMapLatest { (query, opticaId) ->
        repository.getMonturasByOptica(opticaId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun updateForm(update: (MonturaFormState) -> MonturaFormState) {
        _uiState.update { it.copy(form = update(it.form), error = null, success = null) }
    }

    fun startCreate() {
        _uiState.update { it.copy(editing = true, form = MonturaFormState(), error = null, success = null) }
    }

    fun startEdit(montura: Montura) {
        _uiState.update {
            it.copy(
                editing = true,
                error = null,
                success = null,
                form = MonturaFormState(
                    id = montura.id,
                    sku = montura.sku,
                    marca = montura.marca,
                    modelo = montura.modelo,
                    color = montura.color,
                    talla = montura.talla,
                    costo = if (montura.costo == 0.0) "" else montura.costo.toString(),
                    precio = if (montura.precio == 0.0) "" else montura.precio.toString(),
                    stockActual = montura.stockActual.toString(),
                    stockMinimo = montura.stockMinimo.toString(),
                    activo = montura.activo
                )
            )
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editing = false, form = MonturaFormState(), error = null) }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val form = state.form
            if (form.sku.isBlank() || form.modelo.isBlank()) {
                _uiState.update { it.copy(error = "SKU y Modelo son obligatorios.") }
                return@launch
            }
            val costo = form.costo.replace(",", ".").toDoubleOrNull() ?: 0.0
            val precio = form.precio.replace(",", ".").toDoubleOrNull() ?: 0.0
            val stockActual = form.stockActual.toIntOrNull() ?: 0
            val stockMinimo = form.stockMinimo.toIntOrNull() ?: 0
            if (stockActual < 0 || stockMinimo < 0) {
                _uiState.update { it.copy(error = "Stock actual y mínimo no pueden ser negativos.") }
                return@launch
            }
            val opticaId = sessionManager.opticaId.first()
            val montura = Montura(
                id = form.id ?: UUID.randomUUID().toString(),
                sku = form.sku.trim(),
                marca = form.marca.trim(),
                modelo = form.modelo.trim(),
                color = form.color.trim(),
                talla = form.talla.trim(),
                costo = costo,
                precio = precio,
                stockActual = stockActual,
                stockMinimo = stockMinimo,
                activo = form.activo,
                opticaId = opticaId
            )
            when (repository.getMonturaById(montura.id)) {
                is Resource.Success -> repository.updateMontura(montura)
                else -> repository.insertMontura(montura)
            }
            _uiState.update {
                it.copy(
                    editing = false,
                    form = MonturaFormState(),
                    error = null,
                    success = "Montura guardada"
                )
            }
        }
    }

    fun delete(montura: Montura) {
        viewModelScope.launch {
            repository.deleteMontura(montura)
            _uiState.update { it.copy(success = "Montura eliminada", error = null) }
        }
    }

    fun registrarEntrada(montura: Montura, cantidad: Int = 1) {
        if (cantidad <= 0) return
        viewModelScope.launch {
            val opticaId = sessionManager.opticaId.first()
            val updated = repository.adjustMonturaStock(montura.id, opticaId, cantidad)
            if (updated > 0) {
                repository.insertMonturaMovimiento(
                    MonturaMovimiento(
                        id = UUID.randomUUID().toString(),
                        monturaId = montura.id,
                        tipo = "ENTRADA",
                        cantidad = cantidad,
                        stockPrevio = montura.stockActual,
                        stockNuevo = montura.stockActual + cantidad,
                        referenciaId = "",
                        nota = "Ingreso manual desde inventario",
                        opticaId = opticaId
                    )
                )
                _uiState.update { it.copy(success = "Stock actualizado (+$cantidad)", error = null) }
            }
        }
    }
}
