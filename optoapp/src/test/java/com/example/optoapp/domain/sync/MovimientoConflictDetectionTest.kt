package com.example.optoapp.domain.sync

import com.example.optoapp.data.MonturaMovimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * RED test: Movimiento conflict detection by composite key
 * (referenciaId + tipo + monturaId) + stockNuevo comparison.
 *
 * References ConflictHelper.detectConflictMovimientos() — which does NOT exist yet.
 * This test FAILS until A-10 is implemented.
 */
class MovimientoConflictDetectionTest {

    private fun makeMovimiento(
        id: String,
        refId: String,
        tipo: String,
        monturaId: String,
        stockNuevo: Int,
        cantidad: Int = 1,
    ) = MonturaMovimiento(
        id = id, monturaId = monturaId, fecha = LocalDate.now(),
        tipo = tipo, cantidad = cantidad,
        stockPrevio = stockNuevo - cantidad, stockNuevo = stockNuevo,
        referenciaId = refId, nota = "", opticaId = "o1",
    )

    @Test
    fun noConflict_whenRemoteHasNoMatchingMovement() {
        val local = listOf(
            makeMovimiento("m1", "ref-1", "SALIDA_VENTA", "mont-1", stockNuevo = 5),
        )
        val remote = emptyList<MonturaMovimiento>()

        val (safe, conflicted) = ConflictHelper.detectConflictMovimientos(local, remote)
        assertEquals(listOf("m1"), safe)
        assertTrue(conflicted.isEmpty())
    }

    @Test
    fun conflict_whenRemoteHasDifferentStockNuevo() {
        val local = listOf(
            makeMovimiento("m-local", "ref-1", "SALIDA_VENTA", "mont-1", stockNuevo = 5),
        )
        val remote = listOf(
            makeMovimiento("m-remote", "ref-1", "SALIDA_VENTA", "mont-1", stockNuevo = 3),
        )

        val (safe, conflicted) = ConflictHelper.detectConflictMovimientos(local, remote)
        assertTrue(safe.isEmpty())
        assertEquals(listOf("m-local"), conflicted)
    }

    @Test
    fun idempotent_whenRemoteHasSameStockNuevo() {
        val local = listOf(
            makeMovimiento("m-local", "ref-1", "ENTRADA", "mont-1", stockNuevo = 10),
        )
        val remote = listOf(
            makeMovimiento("m-remote", "ref-1", "ENTRADA", "mont-1", stockNuevo = 10),
        )

        val (safe, conflicted) = ConflictHelper.detectConflictMovimientos(local, remote)
        assertEquals(listOf("m-local"), safe)
        assertTrue(conflicted.isEmpty())
    }

    @Test
    fun noConflict_whenDifferentReferenciaId() {
        val local = listOf(
            makeMovimiento("m1", "ref-1", "ENTRADA", "mont-1", stockNuevo = 5),
        )
        val remote = listOf(
            makeMovimiento("m2", "ref-2", "ENTRADA", "mont-1", stockNuevo = 3),
        )

        val (safe, conflicted) = ConflictHelper.detectConflictMovimientos(local, remote)
        assertEquals(listOf("m1"), safe)
        assertTrue(conflicted.isEmpty())
    }

    @Test
    fun noConflict_whenDifferentTipo() {
        val local = listOf(
            makeMovimiento("m1", "ref-1", "ENTRADA", "mont-1", stockNuevo = 5),
        )
        val remote = listOf(
            makeMovimiento("m2", "ref-1", "SALIDA_VENTA", "mont-1", stockNuevo = 8),
        )

        val (safe, conflicted) = ConflictHelper.detectConflictMovimientos(local, remote)
        assertEquals(listOf("m1"), safe)
        assertTrue(conflicted.isEmpty())
    }

    @Test
    fun mixedResults_multiLocalMovements() {
        val local = listOf(
            makeMovimiento("m1", "ref-1", "ENTRADA", "mont-1", stockNuevo = 5),
            makeMovimiento("m2", "ref-1", "ENTRADA", "mont-1", stockNuevo = 8),
            makeMovimiento("m3", "ref-2", "SALIDA_VENTA", "mont-2", stockNuevo = 3),
        )
        val remote = listOf(
            makeMovimiento("mr1", "ref-1", "ENTRADA", "mont-1", stockNuevo = 5),
        )

        val (safe, conflicted) = ConflictHelper.detectConflictMovimientos(local, remote)
        assertEquals(setOf("m1", "m3"), safe.toSet())
        assertEquals(listOf("m2"), conflicted)
    }
}
