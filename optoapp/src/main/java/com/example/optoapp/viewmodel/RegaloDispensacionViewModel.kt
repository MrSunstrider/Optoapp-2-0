package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.domain.CostoFechado
import com.example.optoapp.domain.OrderStatusPolicy
import com.example.optoapp.domain.ResumenRecalcMarker
import com.example.optoapp.domain.movimientoReferenciaForRegalo
import com.example.optoapp.util.DispensacionStockHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegaloDispensacionViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val stockHelper: DispensacionStockHelper,
    private val resumenRecalcMarker: ResumenRecalcMarker,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun saveRegaloAndDeductStock(
        regalo: RegaloDispensacionEntity,
        opticaId: String,
    ) {
        launchGuarded {
            repository.withTransaction {
                val parent = requireEditableParent(regalo.dispensacionId, opticaId)
                repository.insertRegalo(regalo)
                marcarRecalculo(parent, regalo, opticaId)
                if (regalo.productoId.isNotBlank()) {
                    val result = stockHelper.adjustStockAndRegistrarMovimiento(
                        regalo.productoId,
                        opticaId,
                        -regalo.cantidad,
                        "SALIDA_VENTA",
                        movimientoReferenciaForRegalo(regalo.id),
                        "Salida por regalo",
                    )
                    if (result.isFailure) {
                        throw IllegalStateException("Stock insuficiente para regalo: ${regalo.descripcion}")
                    }
                }
            }
        }
    }

    fun removeRegaloAndRestoreStock(
        regalo: RegaloDispensacionEntity,
        opticaId: String,
    ) {
        launchGuarded {
            repository.withTransaction {
                val parent = requireEditableParent(regalo.dispensacionId, opticaId)
                repository.deleteRegaloById(regalo.id, opticaId)
                marcarRecalculo(parent, regalo, opticaId)
                if (regalo.productoId.isNotBlank()) {
                    stockHelper.adjustStockAndRegistrarMovimiento(
                        regalo.productoId,
                        opticaId,
                        regalo.cantidad,
                        "AJUSTE",
                        movimientoReferenciaForRegalo(regalo.id),
                        "Reversión por eliminación de regalo",
                    )
                }
            }
        }
    }

    private fun launchGuarded(block: suspend () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al modificar regalos."
            }
        }
    }

    private suspend fun requireEditableParent(dispensacionId: String, opticaId: String): DispensacionOptica {
        val parent = (repository.getDispensacionById(dispensacionId, opticaId) as? Resource.Success)?.data
            ?: throw IllegalStateException("Dispensación no encontrada.")
        OrderStatusPolicy.requireEditable(parent.estadoEntrega, "modificar regalos")
        return parent
    }

    // Adding and removing a gift shift the parent's day by the same amount, so the delta alone decides.
    private suspend fun marcarRecalculo(parent: DispensacionOptica, regalo: RegaloDispensacionEntity, opticaId: String) {
        resumenRecalcMarker.registrar(
            opticaId,
            ResumenRecalcMarker.CAUSA_DISPENSACION,
            parent.id,
            anterior = CostoFechado(parent.fecha, 0.0),
            nuevo = CostoFechado(parent.fecha, regalo.cantidad * regalo.costoUnitario),
        )
    }
}
