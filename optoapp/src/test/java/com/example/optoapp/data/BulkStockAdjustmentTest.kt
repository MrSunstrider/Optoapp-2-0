package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Bulk stock adjustment: commit differences from inventario_fisico results
 * in correct MonturaMovimiento records and stockActual updates.
 */
@RunWith(RobolectricTestRunner::class)
class BulkStockAdjustmentTest {

    private lateinit var db: OptoDatabase
    private lateinit var repo: InventarioFisicoRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        val scheduler = mockk<Lazy<PostSaveSyncScheduler>> {
            every { get() } returns mockk(relaxed = true)
        }
        val coordinator = MonturaInventoryCoordinator(
            db.monturaDao(),
            db.monturaMovimientoDao(),
            scheduler,
        )
        repo = InventarioFisicoRepository(
            ifDao = db.inventarioFisicoDao(),
            monturaCoordinator = coordinator,
            monturaDao = db.monturaDao(),
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun adjust_createsMovementForEachDifference() = runBlocking {
        db.monturaDao().insertMontura(
            Montura(
                id = "m1",
                sku = "S1",
                marca = "A",
                modelo = "X",
                color = "N",
                talla = "M",
                opticaId = "o1",
                stockActual = 10,
            ),
        )
        db.monturaDao().insertMontura(
            Montura(
                id = "m2",
                sku = "S2",
                marca = "B",
                modelo = "Y",
                color = "R",
                talla = "L",
                opticaId = "o1",
                stockActual = 20,
            ),
        )
        db.monturaDao().insertMontura(
            Montura(
                id = "m3",
                sku = "S3",
                marca = "C",
                modelo = "Z",
                color = "B",
                talla = "S",
                opticaId = "o1",
                stockActual = 30,
            ),
        )

        val s = repo.createSession("o1", "u1")
        val dao = db.inventarioFisicoDao()
        val detalles = dao.getDetalles(s!!.id)

        dao.updateDetalle(detalles.find { it.monturaId == "m1" }!!.copy(stockContado = 8, diferencia = -2))
        dao.updateDetalle(detalles.find { it.monturaId == "m2" }!!.copy(stockContado = 25, diferencia = 5))
        // m3: no change (diferencia = null → treated as 0)

        repo.closeSession(s.id, s.opticaId)

        val movimientos = db.monturaMovimientoDao().getMovimientosListByOptica("o1")
        assertEquals(2, movimientos.size)

        val adj1 = movimientos.find { it.monturaId == "m1" }!!
        assertEquals("AJUSTE_INVENTARIO", adj1.tipo)
        assertEquals(2, adj1.cantidad)
        assertEquals(10, adj1.stockPrevio)
        assertEquals(8, adj1.stockNuevo)

        val adj2 = movimientos.find { it.monturaId == "m2" }!!
        assertEquals(5, adj2.cantidad)
        assertEquals(25, adj2.stockNuevo)
    }

    @Test
    fun adjust_updatesStockActualForEachDifference() = runBlocking {
        db.monturaDao().insertMontura(
            Montura(
                id = "m1",
                sku = "S1",
                marca = "A",
                modelo = "X",
                color = "N",
                talla = "M",
                opticaId = "o1",
                stockActual = 10,
            ),
        )
        db.monturaDao().insertMontura(
            Montura(
                id = "m2",
                sku = "S2",
                marca = "B",
                modelo = "Y",
                color = "R",
                talla = "L",
                opticaId = "o1",
                stockActual = 20,
            ),
        )

        val s = repo.createSession("o1", "u1")
        val dao = db.inventarioFisicoDao()
        val detalles = dao.getDetalles(s!!.id)
        dao.updateDetalle(detalles.find { it.monturaId == "m1" }!!.copy(stockContado = 7, diferencia = -3))
        dao.updateDetalle(detalles.find { it.monturaId == "m2" }!!.copy(stockContado = 22, diferencia = 2))

        repo.closeSession(s.id, s.opticaId)

        assertEquals(7, db.monturaDao().getMonturaByIdForOptica("m1", "o1")!!.stockActual)
        assertEquals(22, db.monturaDao().getMonturaByIdForOptica("m2", "o1")!!.stockActual)
    }

    @Test
    fun adjust_marksSessionCompletadoAfterCommit() = runBlocking {
        db.monturaDao().insertMontura(
            Montura(
                id = "m1",
                sku = "S1",
                marca = "A",
                modelo = "X",
                color = "N",
                talla = "M",
                opticaId = "o1",
                stockActual = 10,
            ),
        )
        val s = repo.createSession("o1", "u1")
        repo.closeSession(s!!.id, s!!.opticaId)

        assertEquals("COMPLETADO", db.inventarioFisicoDao().getById(s.id, "o1")!!.estado)
    }

    @Test
    fun adjust_skipsZeroDifferences() = runBlocking {
        db.monturaDao().insertMontura(
            Montura(
                id = "m1",
                sku = "S1",
                marca = "A",
                modelo = "X",
                color = "N",
                talla = "M",
                opticaId = "o1",
                stockActual = 10,
            ),
        )
        val s = repo.createSession("o1", "u1")
        val dao = db.inventarioFisicoDao()
        val detalles = dao.getDetalles(s!!.id)
        dao.updateDetalle(detalles.find { it.monturaId == "m1" }!!.copy(stockContado = 10, diferencia = 0))

        repo.closeSession(s.id, s.opticaId)

        val movimientos = db.monturaMovimientoDao().getMovimientosListByOptica("o1")
        assertEquals(0, movimientos.size)
        assertEquals(10, db.monturaDao().getMonturaByIdForOptica("m1", "o1")!!.stockActual)
    }
}
