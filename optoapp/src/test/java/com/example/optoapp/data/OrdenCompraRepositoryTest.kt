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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class OrdenCompraRepositoryTest {

    private lateinit var db: OptoDatabase
    private lateinit var repository: OrdenCompraRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()

        val mockScheduler = mockk<PostSaveSyncScheduler>(relaxed = true)
        val lazyScheduler = mockk<Lazy<PostSaveSyncScheduler>>()
        every { lazyScheduler.get() } returns mockScheduler

        val coordinator = MonturaInventoryCoordinator(
            db.monturaDao(), db.monturaMovimientoDao(), lazyScheduler
        )

        val ocLazyScheduler = mockk<Lazy<PostSaveSyncScheduler>>()
        every { ocLazyScheduler.get() } returns mockScheduler

        repository = OrdenCompraRepository(
            db.ordenCompraDao(), db.ordenCompraItemDao(), coordinator, ocLazyScheduler
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    private suspend fun seedProveedor() {
        db.proveedorDao().insert(Proveedor(id = "p1", nombre = "P", ruc = "111", opticaId = "o1"))
    }

    private suspend fun seedMontura(id: String = "m1") {
        db.monturaDao().insertMontura(Montura(id = id, sku = "SKU-$id", marca = "A",
            modelo = "X", color = "N", talla = "M", opticaId = "o1"))
    }

    @Test
    fun getByOptica_returnsOCsinDescOrder() = runBlocking {
        seedProveedor()
        repository.create(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.of(2026, 1, 1), opticaId = "o1"), emptyList())
        repository.create(OrdenCompra(id = "oc2", numero = "OC-002", proveedorId = "p1",
            fecha = LocalDate.of(2026, 6, 17), opticaId = "o1"), emptyList())

        val list = repository.getByOptica("o1").first()
        assertEquals(2, list.size)
        assertEquals("oc2", list[0].id)
    }

    @Test
    fun create_withItems_calculatesTotal() = runBlocking {
        seedProveedor()
        seedMontura("m1")
        seedMontura("m2")

        val items = listOf(
            OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
                cantidad = 10, costoUnitario = 120.0),
            OrdenCompraItem(id = "i2", ordenId = "oc1", monturaId = "m2",
                cantidad = 5, costoUnitario = 80.0)
        )
        repository.create(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"), items)

        val oc = db.ordenCompraDao().getById("oc1")
        assertNotNull(oc)
        assertEquals(1600.0, oc!!.total, 0.001)
        val savedItems = db.ordenCompraItemDao().getByOrden("oc1")
        assertEquals(2, savedItems.size)
    }

    @Test
    fun updateEstado_PENDIENTE_to_PARCIAL() = runBlocking {
        seedProveedor()
        seedMontura("m1")
        repository.create(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"),
            listOf(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
                cantidad = 10, costoUnitario = 100.0, recibido = 5)))

        repository.updateEstado("oc1", "PARCIAL")
        val oc = db.ordenCompraDao().getById("oc1")
        assertEquals("PARCIAL", oc!!.estado)
    }

    @Test
    fun updateEstado_PARCIAL_to_COMPLETADA_withFullReceipt() = runBlocking {
        seedProveedor()
        seedMontura("m1")
        repository.create(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"),
            listOf(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
                cantidad = 10, costoUnitario = 100.0)))

        // Mark all items as fully received and complete
        val received = mapOf("i1" to 10)
        repository.receiveItems("oc1", received)
        val oc = db.ordenCompraDao().getById("oc1")
        assertEquals("COMPLETADA", oc!!.estado)
        assertEquals(1000.0, oc.total, 0.001)
    }

    @Test
    fun getById_returnsOCwithItems() = runBlocking {
        seedProveedor()
        seedMontura("m1")
        repository.create(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"),
            listOf(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
                cantidad = 10, costoUnitario = 120.0)))

        val oc = repository.getById("oc1")
        assertNotNull(oc)
        assertEquals("oc1", oc!!.id)
        val items = repository.getItems("oc1")
        assertEquals(1, items.size)
        assertEquals(10, items[0].cantidad)
    }

    @Test
    fun delete_removesOCandItems() = runBlocking {
        seedProveedor()
        seedMontura("m1")
        repository.create(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"),
            listOf(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
                cantidad = 10, costoUnitario = 100.0)))

        repository.delete("oc1")
        val cancelled = db.ordenCompraDao().getById("oc1")
        assertNotNull(cancelled)
        assertEquals("CANCELADA", cancelled!!.estado)
        assertTrue(db.ordenCompraItemDao().getByOrden("oc1").isEmpty())
    }

    @Test
    fun receiveItems_partial_updatesRecibidoAndTotal() = runBlocking {
        seedProveedor()
        seedMontura("m1")
        seedMontura("m2")
        repository.create(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"),
            listOf(
                OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
                    cantidad = 10, costoUnitario = 100.0),
                OrdenCompraItem(id = "i2", ordenId = "oc1", monturaId = "m2",
                    cantidad = 5, costoUnitario = 50.0)
            ))

        repository.receiveItems("oc1", mapOf("i1" to 7))

        val items = db.ordenCompraItemDao().getByOrden("oc1")
        val i1 = items.find { it.id == "i1" }!!
        assertEquals(7, i1.recibido)
        val i2 = items.find { it.id == "i2" }!!
        assertEquals(0, i2.recibido)

        val oc = db.ordenCompraDao().getById("oc1")!!
        assertEquals(700.0, oc.total, 0.001)
    }

    @Test
    fun estadoWorkflow_PENDIENTE_to_APROBADA_to_PARCIAL() = runBlocking {
        seedProveedor()
        seedMontura("m1")
        repository.create(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"),
            listOf(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
                cantidad = 10, costoUnitario = 100.0)))

        assertEquals("PENDIENTE", db.ordenCompraDao().getById("oc1")!!.estado)
        repository.updateEstado("oc1", "APROBADA")
        assertEquals("APROBADA", db.ordenCompraDao().getById("oc1")!!.estado)
        repository.receiveItems("oc1", mapOf("i1" to 5))
        assertEquals("PARCIAL", db.ordenCompraDao().getById("oc1")!!.estado)
    }

    @Test
    fun estadoWorkflow_COMPLETADA_stockUpdateOnFullReceipt() = runBlocking {
        seedProveedor()
        seedMontura("m1")
        // Set initial stock
        db.monturaDao().adjustStock("m1", "o1", 20)
        repository.create(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"),
            listOf(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
                cantidad = 10, costoUnitario = 100.0)))

        repository.receiveItems("oc1", mapOf("i1" to 10))
        val oc = db.ordenCompraDao().getById("oc1")!!
        assertEquals("COMPLETADA", oc.estado)
        // Stock should have increased by 10
        val montura = db.monturaDao().getMonturaById("m1")
        assertNotNull(montura)
        assertEquals(30, montura!!.stockActual)
    }

    @Test
    fun estadoWorkflow_CANCELADA_doesNotUpdateStock() = runBlocking {
        seedProveedor()
        seedMontura("m1")
        db.monturaDao().adjustStock("m1", "o1", 20)
        repository.create(OrdenCompra(id = "oc1", numero = "OC-001", proveedorId = "p1",
            fecha = LocalDate.now(), opticaId = "o1"),
            listOf(OrdenCompraItem(id = "i1", ordenId = "oc1", monturaId = "m1",
                cantidad = 10, costoUnitario = 100.0)))

        repository.delete("oc1")
        val oc = db.ordenCompraDao().getById("oc1")!!
        assertEquals("CANCELADA", oc.estado)
        // Stock should not change on cancel
        val montura = db.monturaDao().getMonturaById("m1")
        assertNotNull(montura)
        assertEquals(20, montura!!.stockActual)
    }
}
