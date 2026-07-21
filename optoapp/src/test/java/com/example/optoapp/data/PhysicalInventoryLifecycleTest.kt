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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class PhysicalInventoryLifecycleTest {

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
    fun fullLifecycle_createCountDetailCommitVerify() = runBlocking {
        db.monturaDao().insertMontura(
            Montura(
                id = "m1",
                sku = "SKU1",
                marca = "RayBan",
                modelo = "Aviator",
                color = "Negro",
                talla = "M",
                opticaId = "o1",
                stockActual = 10,
            ),
        )
        db.monturaDao().insertMontura(
            Montura(
                id = "m2",
                sku = "SKU2",
                marca = "Oakley",
                modelo = "Holbrook",
                color = "Marrón",
                talla = "L",
                opticaId = "o1",
                stockActual = 5,
            ),
        )
        db.monturaDao().insertMontura(
            Montura(
                id = "m3",
                sku = "SKU3",
                marca = "Persol",
                modelo = "PO",
                color = "Carey",
                talla = "M",
                opticaId = "o1",
                stockActual = 0,
            ),
        )

        val session = repo.createSession("o1", "u1")
        assertNotNull(session)
        assertEquals("EN_PROGRESO", session!!.estado)

        val dao = db.inventarioFisicoDao()
        val detalles = dao.getDetalles(session.id)
        assertEquals(3, detalles.size)
        assertEquals(10, detalles.find { it.monturaId == "m1" }!!.stockSistema)
        assertEquals(5, detalles.find { it.monturaId == "m2" }!!.stockSistema)
        assertEquals(0, detalles.find { it.monturaId == "m3" }!!.stockSistema)

        assertNull(repo.createSession("o1", "u2"))

        dao.updateDetalle(
            detalles.find { it.monturaId == "m1" }!!
                .copy(stockContado = 8, diferencia = -2),
        )
        dao.updateDetalle(
            detalles.find { it.monturaId == "m2" }!!
                .copy(stockContado = 7, diferencia = 2),
        )
        dao.updateDetalle(
            detalles.find { it.monturaId == "m3" }!!
                .copy(stockContado = 0, diferencia = 0),
        )

        val updatedDetalles = dao.getDetalles(session.id)
        assertEquals(-2, updatedDetalles.find { it.monturaId == "m1" }!!.diferencia)
        assertEquals(2, updatedDetalles.find { it.monturaId == "m2" }!!.diferencia)
        assertEquals(0, updatedDetalles.find { it.monturaId == "m3" }!!.diferencia)

        repo.closeSession(session.id)

        assertEquals("COMPLETADO", dao.getById(session.id)!!.estado)

        assertEquals(8, db.monturaDao().getMonturaByIdForOptica("m1", "o1")!!.stockActual)
        assertEquals(7, db.monturaDao().getMonturaByIdForOptica("m2", "o1")!!.stockActual)
        assertEquals(0, db.monturaDao().getMonturaByIdForOptica("m3", "o1")!!.stockActual)

        val movimientos = db.monturaMovimientoDao().getMovimientosListByOptica("o1")
        assertEquals(2, movimientos.size)

        val m1mov = movimientos.find { it.monturaId == "m1" }!!
        assertEquals("AJUSTE_INVENTARIO", m1mov.tipo)
        assertEquals(2, m1mov.cantidad)
        assertEquals(10, m1mov.stockPrevio)
        assertEquals(8, m1mov.stockNuevo)
        assertEquals("AJUSTE_INVENTARIO", m1mov.tipoDocumento)

        val m2mov = movimientos.find { it.monturaId == "m2" }!!
        assertEquals(2, m2mov.cantidad)
        assertEquals(7, m2mov.stockNuevo)

        val session2 = repo.createSession("o1", "u1")
        assertNotNull(session2)
    }
}
