package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ProveedorDaoTest {

    private lateinit var db: OptoDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_getById_returnsCorrectProveedor() = runBlocking {
        val dao = db.proveedorDao()
        val p = Proveedor(
            id = "p1",
            nombre = "Optical Corp",
            ruc = "20123456789",
            telefono = "999888777",
            email = "ventas@optical.pe",
            opticaId = "o1",
        )
        dao.insert(p)

        val retrieved = dao.getById("p1")
        assertNotNull(retrieved)
        assertEquals("p1", retrieved!!.id)
        assertEquals("Optical Corp", retrieved.nombre)
        assertEquals("20123456789", retrieved.ruc)
        assertEquals(true, retrieved.activo)
    }

    @Test
    fun getById_unknownId_returnsNull() = runBlocking {
        val dao = db.proveedorDao()
        assertNull(dao.getById("nonexistent"))
    }

    @Test
    fun getActivosByOptica_filtersInactive() = runBlocking {
        val dao = db.proveedorDao()
        dao.insert(Proveedor(id = "p1", nombre = "A", ruc = "111", opticaId = "o1", activo = true))
        dao.insert(Proveedor(id = "p2", nombre = "B", ruc = "222", opticaId = "o1", activo = false))
        dao.insert(Proveedor(id = "p3", nombre = "C", ruc = "333", opticaId = "o1", activo = true))

        val activos = dao.getActivosByOptica("o1").first()
        assertEquals(2, activos.size)
        assertTrue(activos.all { it.activo })
        assertEquals("A", activos[0].nombre)
    }

    @Test
    fun getListByOptica_returnsAllForOptica() = runBlocking {
        val dao = db.proveedorDao()
        dao.insert(Proveedor(id = "p1", nombre = "A", ruc = "111", opticaId = "o1"))
        dao.insert(Proveedor(id = "p2", nombre = "B", ruc = "222", opticaId = "o2"))

        val o1List = dao.getListByOptica("o1")
        assertEquals(1, o1List.size)
        assertEquals("A", o1List[0].nombre)
    }

    @Test
    fun duplicateRuc_sameOptica_replaced() = runBlocking {
        val dao = db.proveedorDao()
        dao.insert(Proveedor(id = "p1", nombre = "A", ruc = "DUPLICATE", opticaId = "o1"))

        // REPLACE silently overwrites the conflicting row
        dao.insert(Proveedor(id = "p2", nombre = "B", ruc = "DUPLICATE", opticaId = "o1"))

        // Second insert replaced the first
        val all = dao.getListByOptica("o1")
        assertEquals(1, all.size)
        assertEquals("B", all[0].nombre)
    }

    @Test
    fun update_modifiesExistingProveedor() = runBlocking {
        val dao = db.proveedorDao()
        dao.insert(Proveedor(id = "p1", nombre = "Old", ruc = "111", opticaId = "o1"))
        val rows = dao.update(
            id = "p1", opticaId = "o1", nombre = "Updated", ruc = "111",
            telefono = "123", email = "", direccion = "",
            contacto = "", activo = false, updatedAt = null, updatedBy = null,
        )
        assertEquals(1, rows)

        val retrieved = dao.getById("p1")
        assertEquals("Updated", retrieved!!.nombre)
        assertEquals("123", retrieved.telefono)
        assertEquals(false, retrieved.activo)
    }

    @Test
    fun getActivosByOptica_ordersByNombreAsc() = runBlocking {
        val dao = db.proveedorDao()
        dao.insert(Proveedor(id = "p1", nombre = "ZZ Top Optics", ruc = "111", opticaId = "o1"))
        dao.insert(Proveedor(id = "p2", nombre = "Alpha Vision", ruc = "222", opticaId = "o1"))
        dao.insert(Proveedor(id = "p3", nombre = "Beta Optical", ruc = "333", opticaId = "o1"))

        val activos = dao.getActivosByOptica("o1").first()
        assertEquals(3, activos.size)
        assertEquals("Alpha Vision", activos[0].nombre)
        assertEquals("Beta Optical", activos[1].nombre)
        assertEquals("ZZ Top Optics", activos[2].nombre)
    }

    @Test
    fun getActivosByOptica_respectsCrossTenantIsolation() = runBlocking {
        val dao = db.proveedorDao()
        dao.insert(Proveedor(id = "p1", nombre = "Optica A", ruc = "111", opticaId = "o1"))
        dao.insert(Proveedor(id = "p2", nombre = "Optica B", ruc = "222", opticaId = "o2"))

        val o1List = dao.getActivosByOptica("o1").first()
        assertEquals(1, o1List.size)
        assertEquals("Optica A", o1List[0].nombre)

        val o2List = dao.getActivosByOptica("o2").first()
        assertEquals(1, o2List.size)
        assertEquals("Optica B", o2List[0].nombre)
    }

    @Test
    fun getListByOptica_returnsEmpty_forUnrelatedOptica() = runBlocking {
        val dao = db.proveedorDao()
        dao.insert(Proveedor(id = "p1", nombre = "Only One", ruc = "111", opticaId = "o1"))

        val result = dao.getListByOptica("o_does_not_exist")
        assertTrue("Expected empty list for unrelated optica", result.isEmpty())
    }

    @Test
    fun softDelete_setsActivoFalseAndExcludesFromActivos() = runBlocking {
        val dao = db.proveedorDao()
        dao.insert(Proveedor(id = "p1", nombre = "Active Corp", ruc = "111", opticaId = "o1"))
        dao.insert(Proveedor(id = "p2", nombre = "To Deactivate", ruc = "222", opticaId = "o1"))

        val before = dao.getActivosByOptica("o1").first()
        assertEquals(2, before.size)

        val p2 = dao.getById("p2")!!
        dao.update(
            id = p2.id, opticaId = p2.opticaId, nombre = p2.nombre,
            ruc = p2.ruc, telefono = p2.telefono, email = p2.email,
            direccion = p2.direccion, contacto = p2.contacto,
            activo = false, updatedAt = p2.updatedAt, updatedBy = p2.updatedBy,
        )

        val after = dao.getActivosByOptica("o1").first()
        assertEquals(1, after.size)
        assertEquals("Active Corp", after[0].nombre)

        val retrieved = dao.getById("p2")
        assertNotNull(retrieved)
        assertEquals(false, retrieved!!.activo)

        val allList = dao.getListByOptica("o1")
        assertEquals(2, allList.size)
    }
}
