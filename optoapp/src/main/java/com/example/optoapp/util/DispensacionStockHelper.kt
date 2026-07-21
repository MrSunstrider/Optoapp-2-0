package com.example.optoapp.util

import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.Resource
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

class DispensacionStockHelper @Inject constructor(
    private val coordinator: MonturaInventoryCoordinator,
) {
    suspend fun adjustStock(
        monturaId: String,
        opticaId: String,
        delta: Int,
    ): Result<Int> {
        val montura = when (val r = coordinator.getMonturaById(monturaId, opticaId)) {
            is Resource.Success -> r.data ?: return Result.failure(IllegalStateException("Montura no encontrada"))
            is Resource.Error -> return Result.failure(IllegalStateException(r.message))
            is Resource.Loading -> return Result.failure(IllegalStateException("Cargando"))
        }

        if (montura.opticaId != opticaId) {
            return Result.failure(IllegalStateException("Montura no pertenece a la óptica"))
        }

        val newStock = montura.stockActual + delta
        if (newStock < 0) {
            return Result.failure(
                IllegalStateException(
                    "Stock insuficiente: actual=${montura.stockActual}, delta=$delta",
                ),
            )
        }

        val affected = coordinator.adjustMonturaStock(monturaId, opticaId, delta)
        return if (affected > 0) {
            Result.success(affected)
        } else {
            Result.failure(IllegalStateException("No se pudo ajustar el stock"))
        }
    }

    suspend fun registrarMovimiento(
        monturaId: String,
        opticaId: String,
        tipo: String,
        cantidad: Int,
        stockPrevio: Int,
        referenciaId: String,
        nota: String,
        stockNuevo: Int? = null,
    ) {
        val movimiento = MonturaMovimiento(
            id = UUID.randomUUID().toString(),
            monturaId = monturaId,
            fecha = LocalDate.now(),
            tipo = tipo,
            cantidad = cantidad,
            stockPrevio = stockPrevio,
            stockNuevo = stockNuevo ?: (stockPrevio + cantidad),
            referenciaId = referenciaId,
            nota = nota,
            opticaId = opticaId,
        )
        coordinator.insertMonturaMovimiento(movimiento)
    }

    suspend fun adjustStockAndRegistrarMovimiento(
        monturaId: String,
        opticaId: String,
        delta: Int,
        tipo: String,
        referenciaId: String,
        nota: String,
    ): Result<Int> {
        val montura = when (val r = coordinator.getMonturaById(monturaId, opticaId)) {
            is Resource.Success -> r.data ?: return Result.failure(IllegalStateException("Montura no encontrada"))
            is Resource.Error -> return Result.failure(IllegalStateException(r.message))
            is Resource.Loading -> return Result.failure(IllegalStateException("Cargando"))
        }

        if (montura.opticaId != opticaId) {
            return Result.failure(IllegalStateException("Montura no pertenece a la óptica"))
        }

        val newStock = montura.stockActual + delta
        if (newStock < 0) {
            return Result.failure(
                IllegalStateException(
                    "Stock insuficiente: actual=${montura.stockActual}, delta=$delta",
                ),
            )
        }

        val affected = coordinator.adjustMonturaStock(monturaId, opticaId, delta)
        if (affected <= 0) {
            return Result.failure(IllegalStateException("No se pudo ajustar el stock"))
        }

        coordinator.insertMonturaMovimiento(
            MonturaMovimiento(
                id = UUID.randomUUID().toString(),
                monturaId = monturaId,
                fecha = LocalDate.now(),
                tipo = tipo,
                cantidad = abs(delta), // Always positive, representing units moved
                stockPrevio = montura.stockActual,
                stockNuevo = montura.stockActual + delta,
                referenciaId = referenciaId,
                nota = nota,
                opticaId = opticaId,
            ),
        )
        return Result.success(affected)
    }
}
