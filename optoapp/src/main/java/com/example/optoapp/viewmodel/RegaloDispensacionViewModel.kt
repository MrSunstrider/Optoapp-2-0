package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.domain.movimientoReferenciaForRegalo
import com.example.optoapp.util.DispensacionStockHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegaloDispensacionViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val stockHelper: DispensacionStockHelper,
) : ViewModel() {

    /**
     * Saves a regalo and deducts stock for the associated producto.
     * Throws RuntimeException if stock is insufficient.
     */
    fun saveRegaloAndDeductStock(
        regalo: RegaloDispensacionEntity,
        opticaId: String,
    ) {
        viewModelScope.launch {
            repository.insertRegalo(regalo)
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
                    throw RuntimeException("Stock insuficiente para regalo: ${regalo.descripcion}")
                }
            }
        }
    }

    /**
     * Removes a regalo and restores stock for the associated producto.
     */
    fun removeRegaloAndRestoreStock(
        regalo: RegaloDispensacionEntity,
        opticaId: String,
    ) {
        viewModelScope.launch {
            repository.deleteRegaloById(regalo.id)
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
