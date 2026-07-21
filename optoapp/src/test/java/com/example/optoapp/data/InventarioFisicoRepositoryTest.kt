package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class InventarioFisicoRepositoryTest {

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
    fun createSession_snapshotsStockFromActiveMonturas() = runBlocking {
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
                stockActual = 5,
            ),
        )

        val s = repo.createSession("o1", "u1")
        assertNotNull(s)
        assertEquals("EN_PROGRESO", s!!.estado)
        assertEquals("o1", s.opticaId)

        val detalles = db.inventarioFisicoDao().getDetalles(s.id)
        assertEquals(2, detalles.size)
        assertEquals(10, detalles.find { it.monturaId == "m1" }!!.stockSistema)
        assertEquals(5, detalles.find { it.monturaId == "m2" }!!.stockSistema)
    }

    @Test
    fun createSession_rejectsDuplicateEnProgreso() = runBlocking {
        db.monturaDao().insertMontura(
            Montura(
                id = "m1",
                sku = "S1",
                marca = "A",
                modelo = "X",
                color = "N",
                talla = "M",
                opticaId = "o1",
            ),
        )
        repo.createSession("o1", "u1")

        val duplicate = repo.createSession("o1", "u2")
        assertNull(duplicate)
    }

    @Test
    fun closeSession_marksEstadoCompletado() = runBlocking {
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
        assertNotNull(s)

        repo.closeSession(s!!.id)
        val closed = db.inventarioFisicoDao().getById(s.id)
        assertEquals("COMPLETADO", closed!!.estado)
    }

    @Test
    fun closeSession_createsAdjustmentMovementsForDifferences() = runBlocking {
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
        assertNotNull(s)

        val dao = db.inventarioFisicoDao()
        val detalle = dao.getDetalles(s!!.id).first()
        dao.updateDetalle(detalle.copy(stockContado = 7, diferencia = -3))

        repo.closeSession(s.id)
        val movimientos = db.monturaMovimientoDao().getMovimientosListByOptica("o1")
        assertEquals(1, movimientos.size)
        assertEquals("AJUSTE_INVENTARIO", movimientos[0].tipo)
        assertEquals(3, movimientos[0].cantidad)
        assertEquals("AJUSTE_INVENTARIO", movimientos[0].tipoDocumento)
    }

    @Test
    fun closeSession_noMovementForZeroDifference() = runBlocking {
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
        assertNotNull(s)

        val dao = db.inventarioFisicoDao()
        val detalle = dao.getDetalles(s!!.id).first()
        dao.updateDetalle(detalle.copy(stockContado = 10, diferencia = 0))

        repo.closeSession(s.id)
        val movimientos = db.monturaMovimientoDao().getMovimientosListByOptica("o1")
        assertEquals(0, movimientos.size)
    }

    @Test
    fun closeSession_adjustsStockAfterReconciliation() = runBlocking {
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
                stockActual = 5,
            ),
        )
        val s = repo.createSession("o1", "u1")
        assertNotNull(s)

        val dao = db.inventarioFisicoDao()
        val detalles = dao.getDetalles(s!!.id)
        dao.updateDetalle(detalles.find { it.monturaId == "m1" }!!.copy(stockContado = 7, diferencia = -3))
        dao.updateDetalle(detalles.find { it.monturaId == "m2" }!!.copy(stockContado = 8, diferencia = 3))

        repo.closeSession(s.id)

        val m1 = db.monturaDao().getMonturaByIdForOptica("m1", "o1")
        val m2 = db.monturaDao().getMonturaByIdForOptica("m2", "o1")
        assertEquals(7, m1!!.stockActual)
        assertEquals(8, m2!!.stockActual)
    }

    @Test
    fun getSessionsByOptica_returnsOrderedByFecha() = runBlocking {
        db.monturaDao().insertMontura(
            Montura(
                id = "m1",
                sku = "S1",
                marca = "A",
                modelo = "X",
                color = "N",
                talla = "M",
                opticaId = "o1",
            ),
        )
        repo.createSession("o1", "u1")
        repo.createSession("o1", "u1")?.let { repo.closeSession(it.id) }
        repo.createSession("o1", "u1")

        val list = db.inventarioFisicoDao().getByOptica("o1").first()
        assertTrue(list.size >= 1)
        assertEquals("EN_PROGRESO", list.first().estado)
    }
}
