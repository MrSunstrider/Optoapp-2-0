package com.example.optoapp.util

import androidx.room.Room
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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

        assertEquals(Result.success(true), first)
        assertEquals(Result.success(false), second)
        assertEquals(4, stock())
        assertEquals(listOf(afterFirst), movimientos())
    }

    @Test
    fun duplicateRestockArrivingMidRestock_adjustsStockOnceAndKeepsFirstMovimiento() = runTest {
        var duplicateSent = false
        var interleaved: Result<Boolean>? = null
        coEvery { coordinator.getMonturaById("M1", opticaId) } coAnswers {
            if (!duplicateSent) {
                duplicateSent = true
                interleaved = helper().restockOnce("M1", opticaId, 1, referencia, "Duplicado")
            }
            callOriginal()
        }

        val outer = helper().restockOnce("M1", opticaId, 1, referencia, "Reposición")

        assertEquals(setOf(Result.success(true), Result.success(false)), setOf(outer, interleaved))
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

        assertEquals(Result.success(false), result)
        assertEquals(3, stock())
        assertEquals(listOf(synced), movimientos())
    }
}
