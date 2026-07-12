package com.example.optoapp.data.costoproducto

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * RED test: CostoProductoDao lookup queries, upsert, and block filtering.
 *
 * References CostoProductoEntity and CostoProductoDao that don't exist yet.
 */
@RunWith(RobolectricTestRunner::class)
class CostoProductoDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: CostoProductoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.costoProductoDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun lookup_byMaterialTipoStockTratSerie_returnsCorrectEntity() = runBlocking {
        val entity = CostoProductoEntity(
            id = "cp1",
            opticaId = "optica1",
            material = "Resina",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            tratamiento = "Antireflex",
            serie = 2,
            costoUnitario = 18.0,
            laboratorioId = "lab1",
            vigenteDesde = "2026-01-01",
            vigenteHasta = null
        )
        dao.upsertAll(listOf(entity))

        val result = dao.lookup(
            material = "Resina",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            tratamiento = "Antireflex",
            serie = 2
        )

        assertTrue(result != null)
        assertEquals("cp1", result!!.id)
        assertEquals(18.0, result.costoUnitario, 0.001)
    }

    @Test
    fun lookup_noMatchingEntity_returnsNull() = runBlocking {
        val result = dao.lookup(
            material = "Resina",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            tratamiento = "Nonexistent",
            serie = 1
        )

        assertNull(result)
    }

    @Test
    fun lookup_considersVigenteHastaNull_onlyActiveRules() = runBlocking {
        val active = CostoProductoEntity(
            id = "cp-active",
            opticaId = "optica1",
            material = "Resina",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            tratamiento = "Antireflex",
            serie = 1,
            costoUnitario = 5.0,
            vigenteDesde = "2026-01-01",
            vigenteHasta = null
        )
        val expired = CostoProductoEntity(
            id = "cp-expired",
            opticaId = "optica1",
            material = "Resina",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            tratamiento = "Antireflex",
            serie = 1,
            costoUnitario = 3.0,
            vigenteDesde = "2025-01-01",
            vigenteHasta = "2025-12-31"
        )
        dao.upsertAll(listOf(active, expired))

        val result = dao.lookup(
            material = "Resina",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            tratamiento = "Antireflex",
            serie = 1
        )

        assertTrue(result != null)
        assertEquals("Only active (vigenteHasta IS NULL) entity should be returned", "cp-active", result!!.id)
        assertEquals(5.0, result.costoUnitario, 0.001)
    }

    @Test
    fun getByBloque_returnsEntitiesForBlock() = runBlocking {
        val stockEntity = CostoProductoEntity(
            id = "cp-s1", opticaId = "optica1",
            material = "Resina", tipoLente = "Monofocal",
            stockOFabricacion = "stock", tratamiento = "AR", serie = 1,
            costoUnitario = 5.0, vigenteDesde = "2026-01-01", vigenteHasta = null
        )
        val fabricacionEntity = CostoProductoEntity(
            id = "cp-f1", opticaId = "optica1",
            material = "Resina", tipoLente = "Bifocal",
            stockOFabricacion = "fabricacion", tratamiento = "Simple", serie = null,
            costoUnitario = 20.0, vigenteDesde = "2026-01-01", vigenteHasta = null
        )
        dao.upsertAll(listOf(stockEntity, fabricacionEntity))

        val stockResults = dao.getByBloque(opticaId = "optica1", bloque = "stock").first()

        assertEquals(1, stockResults.size)
        assertEquals("cp-s1", stockResults[0].id)
    }

    @Test
    fun upsertAll_updatesExistingEntity() = runBlocking {
        val original = CostoProductoEntity(
            id = "cp1", opticaId = "optica1",
            material = "Resina", tipoLente = "Monofocal",
            stockOFabricacion = "stock", tratamiento = "AR", serie = 1,
            costoUnitario = 5.0, vigenteDesde = "2026-01-01", vigenteHasta = null
        )
        dao.upsertAll(listOf(original))

        val updated = original.copy(costoUnitario = 6.0)
        dao.upsertAll(listOf(updated))

        val result = dao.lookup(
            material = "Resina", tipoLente = "Monofocal",
            stockOFabricacion = "stock", tratamiento = "AR", serie = 1
        )
        assertTrue(result != null)
        assertEquals(6.0, result!!.costoUnitario, 0.001)
    }
}
