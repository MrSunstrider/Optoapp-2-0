package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.proveedor.CategoriaMonturaDao
import com.example.optoapp.data.proveedor.MonturaProveedorDao
import com.example.optoapp.data.proveedor.ProveedorDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ProveedorRepositoryTest {

    private lateinit var db: OptoDatabase
    private lateinit var proveedorDao: ProveedorDao
    private lateinit var monturaProveedorDao: MonturaProveedorDao
    private lateinit var categoriaMonturaDao: CategoriaMonturaDao
    private lateinit var repository: ProveedorRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        proveedorDao = db.proveedorDao()
        monturaProveedorDao = db.monturaProveedorDao()
        categoriaMonturaDao = db.categoriaMonturaDao()
        repository = ProveedorRepository(proveedorDao, monturaProveedorDao, categoriaMonturaDao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_getById_returnsProveedor() = runBlocking {
        val p = Proveedor(id = "p1", nombre = "Test Optics", ruc = "123", opticaId = "o1")
        repository.insert(p)
        val retrieved = repository.getById("p1")
        assertNotNull(retrieved)
        assertEquals("Test Optics", retrieved!!.nombre)
    }

    @Test
    fun getById_unknownId_returnsNull() = runBlocking {
        assertNull(repository.getById("nonexistent"))
    }

    @Test
    fun update_modifiesExisting() = runBlocking {
        repository.insert(Proveedor(id = "p1", nombre = "Old", ruc = "111", opticaId = "o1"))
        repository.update(Proveedor(id = "p1", nombre = "New", ruc = "111", opticaId = "o1"))
        val retrieved = repository.getById("p1")
        assertEquals("New", retrieved!!.nombre)
    }

    @Test
    fun softDelete_setsActivoFalse() = runBlocking {
        repository.insert(Proveedor(id = "p1", nombre = "To Delete", ruc = "111", opticaId = "o1"))
        repository.softDelete("p1")
        val retrieved = repository.getById("p1")
        assertNotNull(retrieved)
        assertFalse(retrieved!!.activo)
    }

    @Test
    fun softDelete_excludesFromActivos() = runBlocking {
        repository.insert(Proveedor(id = "p1", nombre = "Active", ruc = "111", opticaId = "o1"))
        repository.insert(Proveedor(id = "p2", nombre = "Deactivated", ruc = "222", opticaId = "o1"))
        repository.softDelete("p2")

        val activos = repository.getActivosByOptica("o1").first()
        assertEquals(1, activos.size)
        assertEquals("Active", activos[0].nombre)
    }

    @Test
    fun getListByOptica_returnsAllIncludingInactive() = runBlocking {
        repository.insert(Proveedor(id = "p1", nombre = "A", ruc = "111", opticaId = "o1", activo = true))
        repository.insert(Proveedor(id = "p2", nombre = "B", ruc = "222", opticaId = "o1", activo = false))

        val all = repository.getListByOptica("o1")
        assertEquals(2, all.size)
    }

    @Test
    fun duplicateRuc_sameOptica_rejected() = runBlocking {
        repository.insert(Proveedor(id = "p1", nombre = "A", ruc = "DUP", opticaId = "o1"))
        // Room's ABORT strategy throws on duplicate
        val caught = assertThrows(Exception::class.java) {
            runBlocking {
                repository.insert(Proveedor(id = "p2", nombre = "B", ruc = "DUP", opticaId = "o1"))
            }
        }
        assertNotNull(caught)
    }

    @Test
    fun linkMonturaProveedor_createsLink() = runBlocking {
        // FK requires both montura and proveedor to exist
        db.monturaDao().insertMontura(Montura(id = "m1", marca = "Test", modelo = "Test", opticaId = "o1"))
        repository.insert(Proveedor(id = "p1", nombre = "Supplier", ruc = "111", opticaId = "o1"))
        val link = MonturaProveedor(
            id = "link-1", monturaId = "m1", proveedorId = "p1",
            costoProveedor = 120.0, precioSugerido = 250.0
        )
        repository.linkMonturaProveedor(link)

        val links = repository.getProveedoresByMontura("m1").first()
        assertEquals(1, links.size)
        assertEquals("p1", links[0].proveedorId)
        assertEquals(120.0, links[0].costoProveedor, 0.01)
    }

    @Test
    fun unlinkMonturaProveedor_setsActivoFalse() = runBlocking {
        db.monturaDao().insertMontura(Montura(id = "m1", marca = "Test", modelo = "Test", opticaId = "o1"))
        repository.insert(Proveedor(id = "p1", nombre = "Supplier", ruc = "111", opticaId = "o1"))
        val link = MonturaProveedor(
            id = "link-1", monturaId = "m1", proveedorId = "p1",
            costoProveedor = 120.0, precioSugerido = 250.0
        )
        repository.linkMonturaProveedor(link)
        repository.unlinkMonturaProveedor("link-1")

        val links = repository.getProveedoresByMontura("m1").first()
        assertEquals(0, links.size)
    }

    @Test
    fun linkMonturaProveedor_orderedByCostoAsc() = runBlocking {
        db.monturaDao().insertMontura(Montura(id = "m1", marca = "Test", modelo = "Test", opticaId = "o1"))
        repository.insert(Proveedor(id = "p1", nombre = "Supplier 1", ruc = "111", opticaId = "o1"))
        repository.insert(Proveedor(id = "p2", nombre = "Supplier 2", ruc = "222", opticaId = "o1"))
        repository.linkMonturaProveedor(
            MonturaProveedor(id = "l1", monturaId = "m1", proveedorId = "p1", costoProveedor = 150.0)
        )
        repository.linkMonturaProveedor(
            MonturaProveedor(id = "l2", monturaId = "m1", proveedorId = "p2", costoProveedor = 100.0)
        )

        val links = repository.getProveedoresByMontura("m1").first()
        assertEquals(2, links.size)
        assertEquals(100.0, links[0].costoProveedor, 0.01) // cheaper first
        assertEquals(150.0, links[1].costoProveedor, 0.01)
    }

    @Test
    fun categoriaMontura_crud_works() = runBlocking {
        val cat = CategoriaMontura(id = "c1", nombre = "Sol", descripcion = "Lentes de sol", opticaId = "o1")
        repository.insertCategoria(cat)
        val list = repository.getCategoriasByOptica("o1").first()
        assertEquals(1, list.size)
        assertEquals("Sol", list[0].nombre)
    }
}
