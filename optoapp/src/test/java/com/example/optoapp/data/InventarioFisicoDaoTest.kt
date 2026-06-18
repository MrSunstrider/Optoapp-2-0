package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class InventarioFisicoDaoTest {

    private lateinit var db: OptoDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertSession_and_getById() = runBlocking {
        val dao = db.inventarioFisicoDao()
        val session = InventarioFisico(id = "if1", fecha = LocalDate.now(),
            estado = "EN_PROGRESO", opticaId = "o1", userId = "u1", notas = "Test")
        dao.insertSession(session)

        val retrieved = dao.getById("if1")
        assertNotNull(retrieved)
        assertEquals("if1", retrieved!!.id)
        assertEquals("EN_PROGRESO", retrieved.estado)
        assertEquals("u1", retrieved.userId)
    }

    @Test
    fun getByOptica_returnsSessionsInDescOrder() = runBlocking {
        val dao = db.inventarioFisicoDao()
        dao.insertSession(InventarioFisico(id = "if1", fecha = LocalDate.of(2026, 1, 1),
            opticaId = "o1", userId = "u1"))
        dao.insertSession(InventarioFisico(id = "if2", fecha = LocalDate.of(2026, 6, 17),
            opticaId = "o1", userId = "u1"))

        val list = dao.getByOptica("o1").first()
        assertEquals(2, list.size)
        assertEquals("if2", list[0].id) // most recent first
    }

    @Test
    fun getActiveByOptica_returnsOnlyEnProgreso() = runBlocking {
        val dao = db.inventarioFisicoDao()
        dao.insertSession(InventarioFisico(id = "if1", fecha = LocalDate.now(),
            estado = "EN_PROGRESO", opticaId = "o1", userId = "u1"))
        dao.insertSession(InventarioFisico(id = "if2", fecha = LocalDate.now(),
            estado = "COMPLETADO", opticaId = "o1", userId = "u1"))

        val active = dao.getActiveByOptica("o1")
        assertNotNull(active)
        assertEquals("if1", active!!.id)
    }

    @Test
    fun getActiveByOptica_returnsNullWhenNoneActive() = runBlocking {
        val dao = db.inventarioFisicoDao()
        dao.insertSession(InventarioFisico(id = "if1", fecha = LocalDate.now(),
            estado = "COMPLETADO", opticaId = "o1", userId = "u1"))

        assertNull(dao.getActiveByOptica("o1"))
    }

    @Test
    fun updateSession_changesEstado() = runBlocking {
        val dao = db.inventarioFisicoDao()
        dao.insertSession(InventarioFisico(id = "if1", fecha = LocalDate.now(),
            estado = "EN_PROGRESO", opticaId = "o1", userId = "u1"))
        dao.updateSession(InventarioFisico(id = "if1", fecha = LocalDate.now(),
            estado = "COMPLETADO", opticaId = "o1", userId = "u1"))

        assertEquals("COMPLETADO", dao.getById("if1")!!.estado)
    }

    @Test
    fun insertDetalles_and_retrieve() = runBlocking {
        val dao = db.inventarioFisicoDao()
        dao.insertSession(InventarioFisico(id = "if1", fecha = LocalDate.now(),
            opticaId = "o1", userId = "u1"))
        db.monturaDao().insertMontura(Montura(id = "m1", sku = "S1", marca = "A", modelo = "X",
            color = "N", talla = "M", opticaId = "o1"))
        db.monturaDao().insertMontura(Montura(id = "m2", sku = "S2", marca = "B", modelo = "Y",
            color = "R", talla = "L", opticaId = "o1"))

        val detalles = listOf(
            InventarioFisicoDetalle(id = "d1", inventarioId = "if1",
                monturaId = "m1", stockSistema = 10, stockContado = 8),
            InventarioFisicoDetalle(id = "d2", inventarioId = "if1",
                monturaId = "m2", stockSistema = 5, stockContado = 5)
        )
        dao.insertDetalles(detalles)

        val retrieved = dao.getDetalles("if1")
        assertEquals(2, retrieved.size)
        assertEquals(10, retrieved[0].stockSistema)
        assertEquals(8, retrieved[0].stockContado)
        assertEquals(5, retrieved[1].stockSistema)
        assertEquals(5, retrieved[1].stockContado)
    }

    @Test
    fun updateDetalle_changesStockContado() = runBlocking {
        val dao = db.inventarioFisicoDao()
        dao.insertSession(InventarioFisico(id = "if1", fecha = LocalDate.now(),
            opticaId = "o1", userId = "u1"))
        db.monturaDao().insertMontura(Montura(id = "m1", sku = "S1", marca = "A", modelo = "X",
            color = "N", talla = "M", opticaId = "o1"))
        dao.insertDetalles(listOf(InventarioFisicoDetalle(id = "d1",
            inventarioId = "if1", monturaId = "m1", stockSistema = 10)))

        dao.updateDetalle(InventarioFisicoDetalle(id = "d1", inventarioId = "if1",
            monturaId = "m1", stockSistema = 10, stockContado = 7, diferencia = -3))

        val detalle = dao.getDetalles("if1")[0]
        assertEquals(7, detalle.stockContado)
        assertEquals(-3, detalle.diferencia)
    }

    @Test
    fun uniqueInventarioMontura_constraint_enforced() = runBlocking {
        val dao = db.inventarioFisicoDao()
        dao.insertSession(InventarioFisico(id = "if1", fecha = LocalDate.now(),
            opticaId = "o1", userId = "u1"))
        db.monturaDao().insertMontura(Montura(id = "m1", sku = "S1", marca = "A", modelo = "X",
            color = "N", talla = "M", opticaId = "o1"))
        dao.insertDetalles(listOf(InventarioFisicoDetalle(id = "d1",
            inventarioId = "if1", monturaId = "m1", stockSistema = 10)))

        try {
            dao.insertDetalles(listOf(InventarioFisicoDetalle(id = "d2",
                inventarioId = "if1", monturaId = "m1", stockSistema = 8)))
            assertTrue("Expected unique constraint violation", false)
        } catch (e: Exception) {
            // Expected: UNIQUE(inventarioId, monturaId)
        }

        assertEquals(1, dao.getDetalles("if1").size)
    }

    @Test
    fun fkConstraint_detalleUnaffiliatedSession_shouldFail() = runBlocking {
        val dao = db.inventarioFisicoDao()
        db.monturaDao().insertMontura(Montura(id = "m1", sku = "S1", marca = "A", modelo = "X",
            color = "N", talla = "M", opticaId = "o1"))

        try {
            dao.insertDetalles(listOf(InventarioFisicoDetalle(id = "d1",
                inventarioId = "nonexistent", monturaId = "m1", stockSistema = 10)))
            assertTrue("Expected FK constraint violation", false)
        } catch (e: Exception) {
            // Expected: FOREIGN KEY(inventarioId) REFERENCES inventario_fisico(id)
        }
    }

    @Test
    fun getByOptica_respectsOpticaIsolation() = runBlocking {
        val dao = db.inventarioFisicoDao()
        dao.insertSession(InventarioFisico(id = "if1", fecha = LocalDate.now(),
            opticaId = "o1", userId = "u1"))
        dao.insertSession(InventarioFisico(id = "if2", fecha = LocalDate.now(),
            opticaId = "o2", userId = "u2"))

        val o1List = dao.getByOptica("o1").first()
        assertEquals(1, o1List.size)
        assertEquals("if1", o1List[0].id)
        assertEquals("o1", o1List[0].opticaId)
    }

    @Test
    fun getDetalles_byInventarioId_returnsOnlyOwnDetalles() = runBlocking {
        val dao = db.inventarioFisicoDao()
        dao.insertSession(InventarioFisico(id = "if1", fecha = LocalDate.now(),
            opticaId = "o1", userId = "u1"))
        dao.insertSession(InventarioFisico(id = "if2", fecha = LocalDate.now(),
            opticaId = "o1", userId = "u1"))
        db.monturaDao().insertMontura(Montura(id = "m1", sku = "S1", marca = "A", modelo = "X",
            color = "N", talla = "M", opticaId = "o1"))
        db.monturaDao().insertMontura(Montura(id = "m2", sku = "S2", marca = "B", modelo = "Y",
            color = "R", talla = "L", opticaId = "o1"))

        dao.insertDetalles(listOf(
            InventarioFisicoDetalle(id = "d1", inventarioId = "if1",
                monturaId = "m1", stockSistema = 10),
            InventarioFisicoDetalle(id = "d2", inventarioId = "if2",
                monturaId = "m2", stockSistema = 5)
        ))

        val if1Detalles = dao.getDetalles("if1")
        assertEquals(1, if1Detalles.size)
        assertEquals("d1", if1Detalles[0].id)
    }

    @Test
    fun deleteSession_cascadeDeletesDetalles() = runBlocking {
        val dao = db.inventarioFisicoDao()
        dao.insertSession(InventarioFisico(id = "if1", fecha = LocalDate.now(),
            opticaId = "o1", userId = "u1"))
        db.monturaDao().insertMontura(Montura(id = "m1", sku = "S1", marca = "A", modelo = "X",
            color = "N", talla = "M", opticaId = "o1"))
        dao.insertDetalles(listOf(InventarioFisicoDetalle(id = "d1",
            inventarioId = "if1", monturaId = "m1", stockSistema = 10)))

        db.openHelper.writableDatabase.delete("inventario_fisico", "id = ?", arrayOf("if1"))

        val detalles = dao.getDetalles("if1")
        assertEquals(0, detalles.size)
    }
}
