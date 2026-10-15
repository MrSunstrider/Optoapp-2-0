package com.example.optoapp.data.costobiselado

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Real-Room DAO test (AGENTS.md exception): in-memory Room needs the Android Context Robolectric provides.
@RunWith(RobolectricTestRunner::class)
class CostoBiseladoDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: CostoBiseladoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.costoBiseladoDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun lookupVigente_noMatchingRow_returnsNoCandidates() = runTest {
        val candidatos = dao.lookupVigente(
            opticaId = "optica1",
            material = "Cristal",
            tipoAro = "taladro",
            stockOFabricacion = "fabricacion",
            serie = null,
            fecha = "2026-06-01",
        )

        assertTrue(candidatos.isEmpty())
    }

    @Test
    fun upsertAll_updatesExistingEntity() = runTest {
        val original = CostoBiseladoEntity(
            id = "cb1", opticaId = "optica1",
            material = "Resina", tipoAro = "aro_completo",
            stockOFabricacion = "stock", serie = 1, altoIndice = "1.50",
            costoPorPar = 12.0, vigenteDesde = "2026-01-01", vigenteHasta = null,
        )
        dao.upsertAll(listOf(original))

        dao.upsertAll(listOf(original.copy(costoPorPar = 14.0)))

        val candidatos = dao.lookupVigente(
            opticaId = "optica1",
            material = "Resina",
            tipoAro = "aro_completo",
            stockOFabricacion = "stock",
            serie = 1,
            fecha = "2026-06-01",
        )
        assertEquals(listOf(14.0), candidatos.map { it.costoPorPar })
    }
}
