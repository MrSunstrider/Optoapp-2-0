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
     * Returns `false` when the AJUSTE for [referenciaId] already exists so a retried cancel never
     * restocks twice, and `true` after restocking. Every failure throws [IllegalStateException]:
     * validation before the claim, a failed adjustment after a won claim, and an ignored claim with
     * no row behind it.
     *
     * Failures are never returned as values because the restock runs in a transaction that joins
     * any caller transaction: once it fails, Android SQLite rolls the caller back on commit, so a
     * caller that kept going on a returned failure would lose its writes silently.
     *
     * The movimiento is claimed first through the unique (referenciaId, tipo, monturaId) index and
     * stock moves only when the claim wins, so a duplicate can neither adjust stock twice nor
     * replace the existing movimiento, with or without a caller transaction.
     *
     * Inventory sync is scheduled only after the restock transaction completes; a caller that
     * owns an outer transaction must still schedule after its own commit.
     */
    suspend fun restockOnce(
        monturaId: String,
        opticaId: String,
        delta: Int,
        referenciaId: String,
        nota: String,
    ): Boolean {
        val restocked = transactionRunner.inTransaction { claimAndRestock(monturaId, opticaId, delta, referenciaId, nota) }
        if (restocked) coordinator.scheduleInventarioSync(opticaId)
        return restocked
    }

    private suspend fun claimAndRestock(
        monturaId: String,
        opticaId: String,
        delta: Int,
        referenciaId: String,
        nota: String,
    ): Boolean {
        val montura = monturaForDelta(monturaId, opticaId, delta).getOrThrow()
        val claimed = coordinator.insertMonturaMovimientoIfAbsent(movimientoFor(montura, delta, TIPO_AJUSTE, referenciaId, nota))
        if (!claimed) {
            checkNotNull(coordinator.findMovimientoByKey(referenciaId, TIPO_AJUSTE, monturaId)) {
                "No se pudo registrar el movimiento de reposición"
            }
            return false
        }
        check(coordinator.adjustMonturaStockLocal(monturaId, opticaId, delta) > 0) { "No se pudo ajustar el stock" }
        return true
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

    private companion object {
        const val TIPO_AJUSTE = "AJUSTE"
    }
}
