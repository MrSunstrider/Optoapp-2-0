package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.domain.movimientoReferenciaForOrdenCompraItem
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

// WHY Robolectric: atomicity and claim-first idempotency need a real Room transaction (AGENTS.md real-Room exception).
@RunWith(RobolectricTestRunner::class)
class OrdenCompraRepositoryReceiveTest {
    private val opticaId = "o1"
    private lateinit var db: OptoDatabase
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var coordinator: MonturaInventoryCoordinator
    private lateinit var repository: OrdenCompraRepository

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduler = mockk(relaxed = true)
        coordinator = spyk(MonturaInventoryCoordinator(db.monturaDao(), db.monturaMovimientoDao(), Lazy { scheduler }))
        repository = OrdenCompraRepository(
            db.ordenCompraDao(),
            db.ordenCompraItemDao(),
            db.monturaDao(),
            coordinator,
            RoomTransactionRunner(db),
            Lazy { scheduler },
        )
        db.proveedorDao().insert(Proveedor(id = "p1", nombre = "P", ruc = "111", opticaId = opticaId))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedMontura(id: String, stock: Int, costo: Double) {
        db.monturaDao().insertMontura(Montura(id = id, sku = "SKU-$id", stockActual = stock, costo = costo, opticaId = opticaId))
    }

    private suspend fun seedOrden(vararg items: OrdenCompraItem) {
        db.ordenCompraDao().insert(
            OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1", fecha = LocalDate.of(2026, 7, 1), estado = "APROBADA", opticaId = opticaId),
        )
        items.forEach { db.ordenCompraItemDao().insert(it) }
    }

    private fun item(id: String, monturaId: String, cantidad: Int, costo: Double) =
        OrdenCompraItem(id = id, ordenId = "oc1", monturaId = monturaId, cantidad = cantidad, costoUnitario = costo)

    private suspend fun montura(id: String) = db.monturaDao().getMonturaByIdForOptica(id, opticaId)!!
    private suspend fun movimientos() = db.monturaMovimientoDao().getMovimientosListByOptica(opticaId)
    private suspend fun estado() = db.ordenCompraDao().getById("oc1", opticaId)!!.estado

    @Test
    fun partialReceipt_leavesStockAndCostUnchanged() = runTest {
        seedMontura("m1", stock = 10, costo = 50.0)
        seedOrden(item("i1", "m1", cantidad = 10, costo = 60.0))

        repository.receiveItems("oc1", opticaId, mapOf("i1" to 4))

        assertEquals("PARCIAL", estado())
        assertEquals(10, montura("m1").stockActual)
        assertEquals(50.0, montura("m1").costo, 0.0)
        assertTrue(movimientos().isEmpty())
    }

    @Test
    fun partialThenComplete_appliesAverageOnceWithCumulativeQuantity() = runTest {
        seedMontura("m1", stock = 10, costo = 50.0)
        seedOrden(item("i1", "m1", cantidad = 10, costo = 60.0))

        repository.receiveItems("oc1", opticaId, mapOf("i1" to 4))
        repository.receiveItems("oc1", opticaId, mapOf("i1" to 10))

        assertEquals("COMPLETADA", estado())
        assertEquals(20, montura("m1").stockActual)
        assertEquals(55.0, montura("m1").costo, 0.0)
        assertEquals(1, movimientos().size)
    }

    @Test
    fun entradaMovement_recordsRealStockReadBeforeTheAdjustmentAndPurchaseCost() = runTest {
        seedMontura("m1", stock = 10, costo = 50.0)
        seedOrden(item("i1", "m1", cantidad = 10, costo = 60.0))

        repository.receiveItems("oc1", opticaId, mapOf("i1" to 10))

        val entrada = movimientos().single()
        assertEquals("ENTRADA", entrada.tipo)
        assertEquals(10, entrada.cantidad)
        assertEquals(10, entrada.stockPrevio)
        assertEquals(20, entrada.stockNuevo)
        assertEquals(60.0, entrada.costoUnitario, 0.0)
        assertEquals(movimientoReferenciaForOrdenCompraItem("i1"), entrada.referenciaId)
    }

    @Test
    fun secondReceiptCall_losesTheClaimAndChangesNothing() = runTest {
        seedMontura("m1", stock = 10, costo = 50.0)
        seedOrden(item("i1", "m1", cantidad = 10, costo = 60.0))
        repository.receiveItems("oc1", opticaId, mapOf("i1" to 10))

        repository.receiveItems("oc1", opticaId, mapOf("i1" to 10))

        assertEquals(20, montura("m1").stockActual)
        assertEquals(55.0, montura("m1").costo, 0.0)
        assertEquals(1, movimientos().size)
    }

    @Test
    fun zeroPurchaseCost_recordsEntradaAndStockButKeepsCost() = runTest {
        seedMontura("m1", stock = 10, costo = 50.0)
        seedOrden(item("i1", "m1", cantidad = 10, costo = 0.0))

        repository.receiveItems("oc1", opticaId, mapOf("i1" to 10))

        assertEquals(20, montura("m1").stockActual)
        assertEquals(50.0, montura("m1").costo, 0.0)
        assertEquals(1, movimientos().size)
    }

    @Test
    fun itemWithNonPositiveReceivedQuantity_isSkippedEntirely() = runTest {
        seedMontura("m1", stock = 10, costo = 50.0)
        seedMontura("m2", stock = 0, costo = 0.0)
        seedOrden(item("i1", "m1", cantidad = 0, costo = 60.0), item("i2", "m2", cantidad = 5, costo = 30.0))

        repository.receiveItems("oc1", opticaId, mapOf("i1" to 0, "i2" to 5))

        assertEquals("COMPLETADA", estado())
        assertEquals(10, montura("m1").stockActual)
        assertEquals(50.0, montura("m1").costo, 0.0)
        assertEquals(listOf("m2"), movimientos().map { it.monturaId })
        assertEquals(30.0, montura("m2").costo, 0.0)
    }

    @Test
    fun failureAfterTheSecondCostUpdate_rollsBackTheWholeReceipt() = runTest {
        seedMontura("m1", stock = 10, costo = 50.0)
        seedMontura("m2", stock = 2, costo = 20.0)
        seedOrden(item("i1", "m1", cantidad = 10, costo = 60.0), item("i2", "m2", cantidad = 2, costo = 30.0))
        coEvery { coordinator.adjustMonturaStockLocal("m2", opticaId, any()) } returns 0

        try {
            repository.receiveItems("oc1", opticaId, mapOf("i1" to 10, "i2" to 2))
            fail("receipt must fail")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().isNotBlank())
        }

        assertEquals("APROBADA", estado())
        assertEquals(listOf(0, 0), db.ordenCompraItemDao().getByOrden("oc1").map { it.recibido })
        assertEquals(10, montura("m1").stockActual)
        assertEquals(50.0, montura("m1").costo, 0.0)
        assertEquals(2, montura("m2").stockActual)
        assertEquals(20.0, montura("m2").costo, 0.0)
        assertTrue(movimientos().isEmpty())
        verify(exactly = 0) { scheduler.scheduleInventarioSync(any()) }
        verify(exactly = 0) { scheduler.scheduleOrdenCompraSync(any()) }
    }

    @Test
    fun monturaFromAnotherOptica_failsTheReceiptInsteadOfSkippingSilently() = runTest {
        db.monturaDao().insertMontura(Montura(id = "foreign", sku = "SKU-foreign", stockActual = 3, costo = 40.0, opticaId = "o2"))
        seedOrden(item("i1", "foreign", cantidad = 1, costo = 60.0))

        try {
            repository.receiveItems("oc1", opticaId, mapOf("i1" to 1))
            fail("receipt must fail")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().isNotBlank())
        }

        assertEquals("APROBADA", estado())
    }

    @Test
    fun completedReceipt_schedulesInventoryAndPurchaseOrderSyncAfterCommit() = runTest {
        seedMontura("m1", stock = 10, costo = 50.0)
        seedOrden(item("i1", "m1", cantidad = 10, costo = 60.0))

        repository.receiveItems("oc1", opticaId, mapOf("i1" to 10))

        verify(exactly = 1) { scheduler.scheduleInventarioSync(opticaId) }
        verify(exactly = 1) { scheduler.scheduleOrdenCompraSync(opticaId) }
    }
}
