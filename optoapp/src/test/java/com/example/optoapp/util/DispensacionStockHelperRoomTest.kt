package com.example.optoapp.util

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.RoomTransactionRunner
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DispensacionStockHelperRoomTest {
    private val opticaId = "o1"
    private val referencia = "d1:anul:i1"
    private lateinit var db: OptoDatabase
    private lateinit var coordinator: MonturaInventoryCoordinator

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val scheduler = mockk<PostSaveSyncScheduler>(relaxed = true)
        coordinator = spyk(MonturaInventoryCoordinator(db.monturaDao(), db.monturaMovimientoDao(), Lazy { scheduler }))
        runBlocking { db.monturaDao().insertMontura(Montura(id = "M1", sku = "sku-M1", stockActual = 3, opticaId = opticaId)) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun helper() = DispensacionStockHelper(coordinator, RoomTransactionRunner(db))
    private suspend fun stock() = db.monturaDao().getMonturaByIdForOptica("M1", opticaId)!!.stockActual
    private suspend fun movimientos(): List<MonturaMovimiento> = db.monturaMovimientoDao().getMovimientosListByOptica(opticaId)

    @Test
    fun secondRestockForSameKey_withoutCallerTransaction_changesNothing() = runTest {
        val first = helper().restockOnce("M1", opticaId, 1, referencia, "Reposición")
        val afterFirst = movimientos().single()

        val second = helper().restockOnce("M1", opticaId, 1, referencia, "Reposición")

        assertTrue(first)
        assertFalse(second)
        assertEquals(4, stock())
        assertEquals(listOf(afterFirst), movimientos())
    }

    @Test
    fun duplicateRestockArrivingMidRestock_adjustsStockOnceAndKeepsFirstMovimiento() = runTest {
        var duplicateSent = false
        var interleaved: Boolean? = null
        coEvery { coordinator.getMonturaById("M1", opticaId) } coAnswers {
            if (!duplicateSent) {
                duplicateSent = true
                interleaved = helper().restockOnce("M1", opticaId, 1, referencia, "Duplicado")
            }
            callOriginal()
        }

        val outer = helper().restockOnce("M1", opticaId, 1, referencia, "Reposición")

        assertEquals(setOf(true, false), setOf(outer, interleaved))
        assertEquals(4, stock())
        val movimiento = movimientos().single()
        assertEquals("Duplicado", movimiento.nota)
        assertEquals(3, movimiento.stockPrevio)
        assertEquals(4, movimiento.stockNuevo)
    }

    @Test
    fun existingSyncedMovimientoForKey_isNeverReplacedByRestock() = runTest {
        val synced = MonturaMovimiento(
            id = "remote-other-device", monturaId = "M1", tipo = "AJUSTE", cantidad = 1,
            stockPrevio = 2, stockNuevo = 3, referenciaId = referencia, nota = "Sincronizado", opticaId = opticaId,
        )
        db.monturaMovimientoDao().insertMovimiento(synced)

        val result = helper().restockOnce("M1", opticaId, 1, referencia, "Reposición")

        assertFalse(result)
        assertEquals(3, stock())
        assertEquals(listOf(synced), movimientos())
    }

    @Test
    fun adjustFailureAfterClaim_standalone_throwsAndRollsBackClaim() = runTest {
        coEvery { coordinator.adjustMonturaStockLocal("M1", opticaId, 1) } returns 0

        val error = runCatching { helper().restockOnce("M1", opticaId, 1, referencia, "Reposición") }.exceptionOrNull()

        assertTrue("restock must fail loudly, got $error", error is IllegalStateException)
        assertEquals("No se pudo ajustar el stock", error?.message)
        assertEquals(emptyList<MonturaMovimiento>(), movimientos())
        assertEquals(3, stock())
    }

    @Test
    fun adjustFailureInsideCallerTransaction_failsTheCallerAndRollsBackItsWrites() = runTest {
        coEvery { coordinator.adjustMonturaStockLocal("M1", opticaId, 1) } returns 0

        val error = runCatching {
            db.withTransaction {
                db.monturaDao().insertMontura(Montura(id = "M2", sku = "sku-M2", stockActual = 7, opticaId = opticaId))
                helper().restockOnce("M1", opticaId, 1, referencia, "Reposición")
            }
        }.exceptionOrNull()

        assertTrue("caller transaction must fail loudly, got $error", error is IllegalStateException)
        assertEquals("No se pudo ajustar el stock", error?.message)
        assertNull(db.monturaDao().getMonturaByIdForOptica("M2", opticaId))
        assertEquals(emptyList<MonturaMovimiento>(), movimientos())
        assertEquals(3, stock())
    }

    @Test
    fun adjustFailureInsideCallerTransactionOnAnotherDispatcher_failsTheCallerAndRollsBackItsWrites() = runTest {
        coEvery { coordinator.adjustMonturaStockLocal("M1", opticaId, 1) } returns 0

        val error = runCatching {
            db.withTransaction {
                db.monturaDao().insertMontura(Montura(id = "M2", sku = "sku-M2", stockActual = 7, opticaId = opticaId))
                withContext(Dispatchers.IO) { helper().restockOnce("M1", opticaId, 1, referencia, "Reposición") }
            }
        }.exceptionOrNull()

        assertTrue("caller transaction must fail loudly, got $error", error is IllegalStateException)
        assertEquals("No se pudo ajustar el stock", error?.message)
        assertNull(db.monturaDao().getMonturaByIdForOptica("M2", opticaId))
        assertEquals(emptyList<MonturaMovimiento>(), movimientos())
        assertEquals(3, stock())
    }

    @Test
    fun concurrentRestocksForSameKey_adjustStockExactlyOnce() = runTest {
        val outcomes = List(2) {
            async(Dispatchers.IO) { helper().restockOnce("M1", opticaId, 1, referencia, "Reposición") }
        }.awaitAll()

        assertEquals(setOf(true, false), outcomes.toSet())
        assertEquals(4, stock())
        assertEquals(1, movimientos().size)
    }
}
