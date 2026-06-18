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
            OptoDatabase::class.java
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
        val p = Proveedor(id = "p1", nombre = "Optical Corp", ruc = "20123456789",
            telefono = "999888777", email = "ventas@optical.pe", opticaId = "o1")
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
    fun duplicateRuc_sameOptica_rejected() = runBlocking {
        val dao = db.proveedorDao()
        dao.insert(Proveedor(id = "p1", nombre = "A", ruc = "DUPLICATE", opticaId = "o1"))

        val caught = assertThrows(Exception::class.java) {
            runBlocking {
                dao.insert(Proveedor(id = "p2", nombre = "B", ruc = "DUPLICATE", opticaId = "o1"))
            }
        }
    }

    @Test
    fun update_modifiesExistingProveedor() = runBlocking {
        val dao = db.proveedorDao()
        dao.insert(Proveedor(id = "p1", nombre = "Old", ruc = "111", opticaId = "o1"))
        dao.update(Proveedor(id = "p1", nombre = "Updated", ruc = "111",
            telefono = "123", opticaId = "o1", activo = false))

        val retrieved = dao.getById("p1")
        assertEquals("Updated", retrieved!!.nombre)
        assertEquals("123", retrieved.telefono)
        assertEquals(false, retrieved.activo)
    }
}
