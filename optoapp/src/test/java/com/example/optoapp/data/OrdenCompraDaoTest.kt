package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class OrdenCompraDaoTest {

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
    fun insert_and_getById_returnsCorrectOC() = runBlocking {
        // First insert a proveedor (FK constraint)
        db.proveedorDao().insert(Proveedor(id = "p1", nombre = "Proveedor A", ruc = "111", opticaId = "o1"))

        val ocDao = db.ordenCompraDao()
        val oc = OrdenCompra(id = "oc1", numero = "OC-20260617-001", proveedorId = "p1",
            fecha = LocalDate.of(2026, 6, 17), estado = "PENDIENTE",
            total = 1500.0, opticaId = "o1")
        ocDao.insert(oc)

        val retrieved = ocDao.getById("oc1")
        assertNotNull(retrieved)
        assertEquals("oc1", retrieved!!.id)
        assertEquals("OC-20260617-001", retrieved.numero)
        assertEquals("PENDIENTE", retrieved.estado)
        assertEquals(1500.0, retrieved.total, 0.001)
    }

    @Test
    fun getByOptica_returnsOCsinDescOrder() = runBlocking {
        db.proveedorDao().insert(Proveedor(id = "p1", nombre = "P", ruc = "111", opticaId = "o1"))

        val ocDao = db.ordenCompraDao()
        ocDao.insert(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.of(2026, 1, 1), opticaId = "o1"))
        ocDao.insert(OrdenCompra(id = "oc2", numero = "OC-002", proveedorId = "p1",
            fecha = LocalDate.of(2026, 6, 17), opticaId = "o1"))

        val list = ocDao.getByOptica("o1").first()
        assertEquals(2, list.size)
        // ORDER BY fecha DESC → most recent first
        assertEquals("oc2", list[0].id)
    }

    @Test
    fun getListByOptica_suspend_returnsAll() = runBlocking {
        db.proveedorDao().insert(Proveedor(id = "p1", nombre = "P", ruc = "111", opticaId = "o1"))

        val ocDao = db.ordenCompraDao()
        ocDao.insert(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"))
        ocDao.insert(OrdenCompra(id = "oc2", numero = "OC-002", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o2"))

        assertEquals(1, ocDao.getListByOptica("o1").size)
    }

    @Test
    fun updateEstado_transitionsCorrectly() = runBlocking {
        db.proveedorDao().insert(Proveedor(id = "p1", nombre = "P", ruc = "111", opticaId = "o1"))

        val ocDao = db.ordenCompraDao()
        ocDao.insert(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), estado = "PENDIENTE", opticaId = "o1"))

        ocDao.update(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), estado = "APROBADA", opticaId = "o1"))

        assertEquals("APROBADA", ocDao.getById("oc1")!!.estado)
    }

    @Test
    fun getLastNumero_returnsMaxForPrefix() = runBlocking {
        db.proveedorDao().insert(Proveedor(id = "p1", nombre = "P", ruc = "111", opticaId = "o1"))

        val ocDao = db.ordenCompraDao()
        ocDao.insert(OrdenCompra(id = "oc1", numero = "OC-20260617-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"))
        ocDao.insert(OrdenCompra(id = "oc2", numero = "OC-20260617-003", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"))

        val last = ocDao.getLastNumero("o1", "OC-20260617-%")
        assertEquals("OC-20260617-003", last)
    }

    // ─── Items ──────────────────────────────────────────

    @Test
    fun insertItems_and_getByOrden() = runBlocking {
        db.proveedorDao().insert(Proveedor(id = "p1", nombre = "P", ruc = "111", opticaId = "o1"))
        db.ordenCompraDao().insert(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"))
        // Create monturas for FK references
        db.monturaDao().insertMontura(Montura(id = "m1", sku = "S1", marca = "A", modelo = "X",
            color = "N", talla = "M", opticaId = "o1"))
        db.monturaDao().insertMontura(Montura(id = "m2", sku = "S2", marca = "B", modelo = "Y",
            color = "R", talla = "L", opticaId = "o1"))

        val itemDao = db.ordenCompraItemDao()
        itemDao.insert(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
            cantidad = 10, costoUnitario = 120.0, recibido = 0))
        itemDao.insert(OrdenCompraItem(id = "i2", ordenId = "oc1", monturaId = "m2",
            cantidad = 5, costoUnitario = 80.0, recibido = 3))

        val items = itemDao.getByOrden("oc1")
        assertEquals(2, items.size)
        assertEquals(10, items[0].cantidad)
        assertEquals(120.0, items[0].costoUnitario, 0.001)
        assertEquals(3, items[1].recibido)
    }

    @Test
    fun updateItem_modifiesRecibido() = runBlocking {
        db.proveedorDao().insert(Proveedor(id = "p1", nombre = "P", ruc = "111", opticaId = "o1"))
        db.ordenCompraDao().insert(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"))
        db.monturaDao().insertMontura(Montura(id = "m1", sku = "S1", marca = "A", modelo = "X",
            color = "N", talla = "M", opticaId = "o1"))

        val itemDao = db.ordenCompraItemDao()
        itemDao.insert(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
            cantidad = 10, costoUnitario = 100.0, recibido = 0))
        itemDao.update(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
            cantidad = 10, costoUnitario = 100.0, recibido = 7))

        val item = itemDao.getByOrden("oc1")[0]
        assertEquals(7, item.recibido)
    }

    @Test
    fun deleteByOrden_removesAllItems() = runBlocking {
        db.proveedorDao().insert(Proveedor(id = "p1", nombre = "P", ruc = "111", opticaId = "o1"))
        db.ordenCompraDao().insert(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"))
        db.monturaDao().insertMontura(Montura(id = "m1", sku = "S1", marca = "A", modelo = "X",
            color = "N", talla = "M", opticaId = "o1"))
        db.monturaDao().insertMontura(Montura(id = "m2", sku = "S2", marca = "B", modelo = "Y",
            color = "R", talla = "L", opticaId = "o1"))

        val itemDao = db.ordenCompraItemDao()
        itemDao.insert(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
            cantidad = 10, costoUnitario = 100.0))
        itemDao.insert(OrdenCompraItem(id = "i2", ordenId = "oc1", monturaId = "m2",
            cantidad = 5, costoUnitario = 80.0))

        itemDao.deleteByOrden("oc1", "o1")
        assertTrue(itemDao.getByOrden("oc1").isEmpty())
    }
}
