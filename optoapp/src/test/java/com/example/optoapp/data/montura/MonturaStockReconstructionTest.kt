package com.example.optoapp.data.montura

import com.example.optoapp.data.MonturaMovimiento
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MonturaStockReconstructionTest {

    private fun movimiento(
        id: String,
        monturaId: String = "mont-1",
        fecha: LocalDate = LocalDate.of(2026, 8, 14),
        stockNuevo: Int,
        updatedAt: String? = null,
    ) = MonturaMovimiento(
        id = id,
        monturaId = monturaId,
        fecha = fecha,
        tipo = "SALIDA_VENTA",
        cantidad = 1,
        stockPrevio = stockNuevo + 1,
        stockNuevo = stockNuevo,
        referenciaId = "ref-$id",
        opticaId = "optica-1",
        updatedAt = updatedAt,
    )

    @Test
    fun sameDateMovimientosResolveByUpdatedAt() {
        val older = movimiento("a", stockNuevo = 9, updatedAt = "2026-08-14T10:00:00Z")
        val newer = movimiento("b", stockNuevo = 8, updatedAt = "2026-08-14T11:00:00Z")

        assertEquals(mapOf("mont-1" to 8), latestStockByMontura(listOf(older, newer)))
        assertEquals(mapOf("mont-1" to 8), latestStockByMontura(listOf(newer, older)))
    }

    @Test
    fun sameDateAndTimestampFallBackToIdSoResultIsStable() {
        val first = movimiento("aaa", stockNuevo = 5)
        val second = movimiento("bbb", stockNuevo = 4)

        assertEquals(mapOf("mont-1" to 4), latestStockByMontura(listOf(first, second)))
        assertEquals(mapOf("mont-1" to 4), latestStockByMontura(listOf(second, first)))
    }

    @Test
    fun laterDateAlwaysWinsOverTimestamp() {
        val early = movimiento("a", fecha = LocalDate.of(2026, 8, 15), stockNuevo = 3, updatedAt = "2026-08-01T00:00:00Z")
        val late = movimiento("b", fecha = LocalDate.of(2026, 8, 14), stockNuevo = 7, updatedAt = "2026-08-20T00:00:00Z")

        assertEquals(mapOf("mont-1" to 3), latestStockByMontura(listOf(late, early)))
    }

    @Test
    fun eachMonturaIsReconstructedIndependently() {
        val a = movimiento("a", monturaId = "mont-1", stockNuevo = 2)
        val b = movimiento("b", monturaId = "mont-2", stockNuevo = 6)

        assertEquals(mapOf("mont-1" to 2, "mont-2" to 6), latestStockByMontura(listOf(a, b)))
    }
}
