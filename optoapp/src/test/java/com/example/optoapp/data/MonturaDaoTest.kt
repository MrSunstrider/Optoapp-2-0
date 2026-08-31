package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.montura.MonturaDao
import kotlinx.coroutines.flow.first
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

/**
 * Tests de integración para [MonturaDao] usando Room in-memory database.
 *
 * Verifica que @Query, @Insert, @Update, @Delete y @Upsert funcionan
 * correctamente después de la extracción del DAO a archivo separado.
 */
@RunWith(RobolectricTestRunner::class)
class MonturaDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: MonturaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.monturaDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertMontura_and_getById_returnsCorrectMontura() = runBlocking {
        val montura = Montura(
            id = "m1",
            sku = "SKU001",
            marca = "Ray-Ban",
            modelo = "Wayfarer",
            color = "Negro",
            talla = "M",
            costo = 80.0,
            precio = 200.0,
            stockActual = 10,
            stockMinimo = 2,
            activo = true,
            opticaId = "optica1",
        )
        dao.insertMontura(montura)

        val retrieved = dao.getMonturaByIdForOptica("m1", "optica1")
        assertNotNull(retrieved)
        assertEquals("m1", retrieved!!.id)
        assertEquals("Ray-Ban", retrieved.marca)
        assertEquals(200.0, retrieved.precio, 0.001)
        assertEquals(10, retrieved.stockActual)
        assertEquals("optica1", retrieved.opticaId)
    }

    @Test
    fun getMonturaById_withUnknownId_returnsNull() = runBlocking {
        val retrieved = dao.getMonturaByIdForOptica("nonexistent", "optica1")

        assertNull(retrieved)
    }

    @Test
    fun getMonturasByOptica_returnsMonturasForOptica() = runBlocking {
        val m1 = Montura(
            id = "m1", sku = "S001", marca = "MarcaA", modelo = "Mod1",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 5, stockMinimo = 2, activo = true, opticaId = "opticaA",
        )
        val m2 = Montura(
            id = "m2", sku = "S002", marca = "MarcaB", modelo = "Mod2",
            color = "Rojo", talla = "L", costo = 60.0, precio = 120.0,
            stockActual = 3, stockMinimo = 1, activo = true, opticaId = "opticaB",
        )
        dao.insertMontura(m1)
        dao.insertMontura(m2)

        val opticaAMonturas = dao.getMonturasByOptica("opticaA").first()

        assertEquals(1, opticaAMonturas.size)
        assertEquals("m1", opticaAMonturas[0].id)
    }

    @Test
    fun getMonturasByOptica_inactiveMontura_stillReturned() = runBlocking {
        val activa = Montura(
            id = "m1", sku = "S001", marca = "Marca", modelo = "Mod1",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 5, stockMinimo = 2, activo = true, opticaId = "o1",
        )
        val inactiva = Montura(
            id = "m2", sku = "S002", marca = "Marca", modelo = "Mod2",
            color = "Rojo", talla = "L", costo = 60.0, precio = 120.0,
            stockActual = 0, stockMinimo = 1, activo = false, opticaId = "o1",
        )
        dao.insertMontura(activa)
        dao.insertMontura(inactiva)

        val todas = dao.getMonturasByOptica("o1").first()
        // activo DESC means true (1) comes before false (0)
        assertEquals(2, todas.size)
        assertEquals(true, todas[0].activo)
        assertEquals(false, todas[1].activo)
    }

    @Test
    fun adjustStock_increasesStock() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "S001", marca = "Marca", modelo = "Mod1",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 5, stockMinimo = 2, activo = true, opticaId = "o1",
        )
        dao.insertMontura(montura)

        val rowsAffected = dao.adjustStock("m1", "o1", 3, "2026-08-31T10:00:00Z")
        assertEquals(1, rowsAffected)

        val retrieved = dao.getMonturaByIdForOptica("m1", "o1")
        assertEquals(8, retrieved!!.stockActual)
    }

    @Test
    fun adjustStock_decreasesStock() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "S001", marca = "Marca", modelo = "Mod1",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 10, stockMinimo = 2, activo = true, opticaId = "o1",
        )
        dao.insertMontura(montura)

        val rowsAffected = dao.adjustStock("m1", "o1", -4, "2026-08-31T10:00:00Z")
        assertEquals(1, rowsAffected)

        val retrieved = dao.getMonturaByIdForOptica("m1", "o1")
        assertEquals(6, retrieved!!.stockActual)
    }

    @Test
    fun adjustStock_belowZero_doesNotUpdate() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "S001", marca = "Marca", modelo = "Mod1",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 3, stockMinimo = 2, activo = true, opticaId = "o1",
        )
        dao.insertMontura(montura)

        // Intentar reducir más del stock disponible
        val rowsAffected = dao.adjustStock("m1", "o1", -10, "2026-08-31T10:00:00Z")
        assertEquals(0, rowsAffected)

        // Stock and stamp must not change when the WHERE guard rejects the delta
        val retrieved = dao.getMonturaByIdForOptica("m1", "o1")
        assertEquals(3, retrieved!!.stockActual)
        assertNull(retrieved.updatedAt)
    }

    @Test
    fun updateMontura_modifiesExistingRecord() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "S001", marca = "Marca", modelo = "Mod1",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 5, stockMinimo = 2, activo = true, opticaId = "o1",
        )
        dao.insertMontura(montura)

        val updated = montura.copy(precio = 150.0, stockActual = 8)
        val rows = dao.updateMontura(
            id = updated.id, opticaId = updated.opticaId,
            sku = updated.sku, marca = updated.marca, modelo = updated.modelo,
            color = updated.color, talla = updated.talla, costo = updated.costo,
            precio = updated.precio, stockActual = updated.stockActual,
            stockMinimo = updated.stockMinimo, activo = updated.activo,
            tipoAro = updated.tipoAro, materialMontura = updated.materialMontura,
            anchoMm = updated.anchoMm, puenteMm = updated.puenteMm,
            alturaMm = updated.alturaMm, imagenUri = updated.imagenUri,
            categoria = updated.categoria, coleccion = updated.coleccion,
            temporada = updated.temporada, estadoComercial = updated.estadoComercial,
            genero = updated.genero, updatedAt = updated.updatedAt,
            updatedBy = updated.updatedBy,
        )
        assertEquals(1, rows)

        val retrieved = dao.getMonturaByIdForOptica("m1", "o1")
        assertEquals(150.0, retrieved!!.precio, 0.001)
        assertEquals(8, retrieved.stockActual)
    }

    @Test
    fun deleteMontura_removesRecord() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "S001", marca = "Marca", modelo = "Mod1",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 5, stockMinimo = 2, activo = true, opticaId = "o1",
        )
        dao.insertMontura(montura)
        dao.deleteMontura(montura.id, montura.opticaId)

        val retrieved = dao.getMonturaByIdForOptica("m1", "o1")
        assertNull(retrieved)
    }

    @Test
    fun `getMonturaByIdForOptica respects opticaId filter`() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "SKU001", marca = "Marca", modelo = "Modelo",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 5, stockMinimo = 2, activo = true, opticaId = "opticaX",
        )
        dao.insertMontura(montura)

        // Same ID with correct opticaId → found
        assertNotNull(dao.getMonturaByIdForOptica("m1", "opticaX"))
        // Same ID with wrong opticaId → NOT found (cross-tenant isolation)
        assertNull(dao.getMonturaByIdForOptica("m1", "opticaY"))
    }

    @Test
    fun getMonturasListByOptica_returnsAllMonturasForOptica() = runBlocking {
        val m1 = Montura(
            id = "m1", sku = "S001", marca = "A", modelo = "X",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 5, stockMinimo = 2, activo = true, opticaId = "o1",
        )
        val m2 = Montura(
            id = "m2", sku = "S002", marca = "B", modelo = "Y",
            color = "Rojo", talla = "L", costo = 60.0, precio = 120.0,
            stockActual = 3, stockMinimo = 1, activo = false, opticaId = "o1",
        )
        dao.insertMontura(m1)
        dao.insertMontura(m2)

        val list = dao.getMonturasListByOptica("o1")
        assertEquals(2, list.size)
    }
}
