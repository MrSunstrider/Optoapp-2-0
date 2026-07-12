package com.example.optoapp.data.costobiselado

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
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
 * RED test: CostoBiseladoDao lookup queries, upsert, and empty fallback.
 *
 * References CostoBiseladoEntity and CostoBiseladoDao that don't exist yet.
 */
@RunWith(RobolectricTestRunner::class)
class CostoBiseladoDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: CostoBiseladoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.costoBiseladoDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun lookup_byMaterialTipoAroStockSerieAltoIndice_returnsCorrectEntity() = runBlocking {
        val entity = CostoBiseladoEntity(
            id = "cb1",
            opticaId = "optica1",
            material = "Resina",
            tipoAro = "aro_completo",
            stockOFabricacion = "stock",
            serie = 2,
            altoIndice = "1.50",
            costoPorPar = 15.0,
            proveedor = "Lab1",
            vigenteDesde = "2026-01-01",
            vigenteHasta = null
        )
        dao.upsertAll(listOf(entity))

        val result = dao.lookup(
            material = "Resina",
            tipoAro = "aro_completo",
            stockOFabricacion = "stock",
            serie = 2,
            altoIndice = "1.50"
        )

        assertTrue(result != null)
        assertEquals("cb1", result!!.id)
        assertEquals(15.0, result.costoPorPar, 0.001)
    }

    @Test
    fun lookup_noMatchingEntity_returnsNull() = runBlocking {
        // Per R4: if no rule matches, leave field empty (not a crash)
        val result = dao.lookup(
            material = "Cristal",
            tipoAro = "taladro",
            stockOFabricacion = "fabricacion",
            serie = null,
            altoIndice = "1.67"
        )

        assertNull(result)
    }

    @Test
    fun lookup_withNullSerie_returnsCorrectEntity() = runBlocking {
        // Fabricacion items have serie=null
        val entity = CostoBiseladoEntity(
            id = "cb2",
            opticaId = "optica1",
            material = "Cristal",
            tipoAro = "ranurado",
            stockOFabricacion = "fabricacion",
            serie = null,
            altoIndice = "1.67",
            costoPorPar = 35.0,
            proveedor = "Lab1",
            vigenteDesde = "2026-01-01",
            vigenteHasta = null
        )
        dao.upsertAll(listOf(entity))

        val result = dao.lookup(
            material = "Cristal",
            tipoAro = "ranurado",
            stockOFabricacion = "fabricacion",
            serie = null,
            altoIndice = "1.67"
        )

        assertTrue(result != null)
        assertEquals("cb2", result!!.id)
        assertEquals(35.0, result.costoPorPar, 0.001)
    }

    @Test
    fun upsertAll_updatesExistingEntity() = runBlocking {
        val original = CostoBiseladoEntity(
            id = "cb1", opticaId = "optica1",
            material = "Resina", tipoAro = "aro_completo",
            stockOFabricacion = "stock", serie = 1, altoIndice = "1.50",
            costoPorPar = 12.0, vigenteDesde = "2026-01-01", vigenteHasta = null
        )
        dao.upsertAll(listOf(original))

        val updated = original.copy(costoPorPar = 14.0)
        dao.upsertAll(listOf(updated))

        val result = dao.lookup(
            material = "Resina", tipoAro = "aro_completo",
            stockOFabricacion = "stock", serie = 1, altoIndice = "1.50"
        )
        assertTrue(result != null)
        assertEquals(14.0, result!!.costoPorPar, 0.001)
    }
}
