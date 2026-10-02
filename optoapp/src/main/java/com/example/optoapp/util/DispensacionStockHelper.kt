package com.example.optoapp.util

import com.example.optoapp.data.DatabaseTransactionRunner
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.Resource
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

class DispensacionStockHelper @Inject constructor(
    private val coordinator: MonturaInventoryCoordinator,
    private val transactionRunner: DatabaseTransactionRunner,
) {
    suspend fun adjustStock(
        monturaId: String,
        opticaId: String,
        delta: Int,
    ): Result<Int> {
        monturaForDelta(monturaId, opticaId, delta).onFailure { return Result.failure(it) }

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
        val montura = monturaForDelta(monturaId, opticaId, delta).getOrElse { return Result.failure(it) }

        val affected = coordinator.adjustMonturaStock(monturaId, opticaId, delta)
        if (affected <= 0) {
            return Result.failure(IllegalStateException("No se pudo ajustar el stock"))
        }

        coordinator.insertMonturaMovimiento(movimientoFor(montura, delta, tipo, referenciaId, nota))
        return Result.success(affected)
    }

    /**
     * Returns `success(false)` when the AJUSTE for [referenciaId] already exists so a retried
     * cancel never restocks twice; `success(true)` after restocking.
     *
     * The movimiento is claimed first through the unique (referenciaId, tipo, monturaId) index and
     * stock moves only when the claim wins, all in one transaction, so a duplicate can neither
     * adjust stock twice nor replace the existing movimiento, with or without a caller transaction.
     * A failed adjustment after a won claim throws so the claim rolls back; inside a caller
     * transaction that failure also aborts the caller.
     */
    suspend fun restockOnce(
        monturaId: String,
        opticaId: String,
        delta: Int,
        referenciaId: String,
        nota: String,
    ): Result<Boolean> = try {
        transactionRunner.inTransaction { claimAndRestock(monturaId, opticaId, delta, referenciaId, nota) }
    } catch (e: RestockAdjustFailed) {
        Result.failure(IllegalStateException(e.message))
    }

    private suspend fun claimAndRestock(
        monturaId: String,
        opticaId: String,
        delta: Int,
        referenciaId: String,
        nota: String,
    ): Result<Boolean> {
        val montura = monturaForDelta(monturaId, opticaId, delta).getOrElse { return Result.failure(it) }
        val claimed = coordinator.insertMonturaMovimientoIfAbsent(movimientoFor(montura, delta, TIPO_AJUSTE, referenciaId, nota))
        if (!claimed) return Result.success(false)
        if (coordinator.adjustMonturaStock(monturaId, opticaId, delta) <= 0) {
            throw RestockAdjustFailed("No se pudo ajustar el stock")
        }
        return Result.success(true)
    }

    private suspend fun monturaForDelta(monturaId: String, opticaId: String, delta: Int): Result<Montura> {
        val montura = when (val r = coordinator.getMonturaById(monturaId, opticaId)) {
            is Resource.Success -> r.data ?: return Result.failure(IllegalStateException("Montura no encontrada"))
            is Resource.Error -> return Result.failure(IllegalStateException(r.message))
            is Resource.Loading -> return Result.failure(IllegalStateException("Cargando"))
        }
        if (montura.opticaId != opticaId) {
            return Result.failure(IllegalStateException("Montura no pertenece a la óptica"))
        }
        if (montura.stockActual + delta < 0) {
            return Result.failure(IllegalStateException("Stock insuficiente: actual=${montura.stockActual}, delta=$delta"))
        }
        return Result.success(montura)
    }

    private fun movimientoFor(montura: Montura, delta: Int, tipo: String, referenciaId: String, nota: String) = MonturaMovimiento(
        id = UUID.randomUUID().toString(),
        monturaId = montura.id,
        fecha = LocalDate.now(),
        tipo = tipo,
        cantidad = abs(delta),
        stockPrevio = montura.stockActual,
        stockNuevo = montura.stockActual + delta,
        referenciaId = referenciaId,
        nota = nota,
        opticaId = montura.opticaId,
    )

    private class RestockAdjustFailed(message: String) : IllegalStateException(message)

    private companion object {
        const val TIPO_AJUSTE = "AJUSTE"
    }
}
