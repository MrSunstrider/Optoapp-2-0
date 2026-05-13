package com.example.optoapp.util

import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Helper para operaciones de ajuste de stock de monturas.
 * Extraído de NuevaDispensacionScreen para ser testeable de forma unitaria.
 */
class DispensacionStockHelper @Inject constructor(
    private val monturaDao: MonturaDao,
    private val monturaMovimientoDao: MonturaMovimientoDao
) {
    /**
     * Ajusta el stock de una montura.
     * @return Success(affectedRows) si el ajuste fue exitoso, Failure si no hay stock suficiente,
     *         la montura no existe, o no pertenece a la óptica indicada.
     */
    suspend fun adjustStock(
        monturaId: String,
        opticaId: String,
        delta: Int
    ): Result<Int> {
        val montura = monturaDao.getMonturaById(monturaId)
            ?: return Result.failure(IllegalStateException("Montura no encontrada: $monturaId"))

        if (montura.opticaId != opticaId) {
            return Result.failure(IllegalStateException("Montura no pertenece a la óptica"))
        }

        val newStock = montura.stockActual + delta
        if (newStock < 0) {
            return Result.failure(IllegalStateException(
                "Stock insuficiente: actual=${montura.stockActual}, delta=$delta"
            ))
        }

        val affected = monturaDao.adjustStock(monturaId, opticaId, delta)
        return if (affected > 0) {
            Result.success(affected)
        } else {
            Result.failure(IllegalStateException("No se pudo ajustar el stock"))
        }
    }

    /**
     * Registra un movimiento de inventario para una montura.
     * @param stockNuevo Opcional — si se provee, se usa este valor en vez de stockPrevio + cantidad.
     *                   Útil cuando el delta es negativo (ej: adjustStockAndRegistrarMovimiento).
     */
    suspend fun registrarMovimiento(
        monturaId: String,
        opticaId: String,
        tipo: String,
        cantidad: Int,
        stockPrevio: Int,
        referenciaId: String,
        nota: String,
        stockNuevo: Int? = null
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
            opticaId = opticaId
        )
        monturaMovimientoDao.insertMovimiento(movimiento)
    }

    /**
     * Combina adjustStock + registrarMovimiento en una sola operación.
     * Si el ajuste de stock falla, NO se registra el movimiento.
     */
    suspend fun adjustStockAndRegistrarMovimiento(
        monturaId: String,
        opticaId: String,
        delta: Int,
        tipo: String,
        referenciaId: String,
        nota: String
    ): Result<Int> {
        val montura = monturaDao.getMonturaById(monturaId)
            ?: return Result.failure(IllegalStateException("Montura no encontrada: $monturaId"))

        if (montura.opticaId != opticaId) {
            return Result.failure(IllegalStateException("Montura no pertenece a la óptica"))
        }

        val newStock = montura.stockActual + delta
        if (newStock < 0) {
            return Result.failure(IllegalStateException(
                "Stock insuficiente: actual=${montura.stockActual}, delta=$delta"
            ))
        }

        val affected = monturaDao.adjustStock(monturaId, opticaId, delta)
        if (affected <= 0) {
            return Result.failure(IllegalStateException("No se pudo ajustar el stock"))
        }

        // Insert movimiento directamente al DAO
        monturaMovimientoDao.insertMovimiento(
            MonturaMovimiento(
                id = UUID.randomUUID().toString(),
                monturaId = monturaId,
                fecha = LocalDate.now(),
                tipo = tipo,
                cantidad = delta.coerceAtLeast(0),
                stockPrevio = montura.stockActual,
                stockNuevo = montura.stockActual + delta,
                referenciaId = referenciaId,
                nota = nota,
                opticaId = opticaId
            )
        )
        return Result.success(affected)
    }
}
