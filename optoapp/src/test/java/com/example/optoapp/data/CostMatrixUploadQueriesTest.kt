package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Real-Room DAO test: in-memory Room needs Robolectric for its Android context, the AGENTS.md exception.
@RunWith(RobolectricTestRunner::class)
class CostMatrixUploadQueriesTest {

    private lateinit var db: OptoDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun producto(id: String, opticaId: String, hasta: String?) = CostoProductoEntity(
        id = id,
        opticaId = opticaId,
        material = "Resina",
        tipoLente = "Monofocal",
        stockOFabricacion = "stock",
        costoUnitario = 18.0,
        vigenteDesde = "2026-01-01",
        vigenteHasta = hasta,
    )

    private fun biselado(id: String, opticaId: String, hasta: String?) = CostoBiseladoEntity(
        id = id,
        opticaId = opticaId,
        material = "Resina",
        tipoAro = "completo",
        stockOFabricacion = "stock",
        costoPorPar = 10.0,
        vigenteDesde = "2026-01-01",
        vigenteHasta = hasta,
    )

    @Test
    fun `producto upload query returns open and closed versions of the optica only`() = runTest {
        val dao = db.costoProductoDao()
        dao.upsertAll(listOf(producto("open", "o1", null), producto("closed", "o1", "2026-06-30"), producto("other", "o2", null)))

        assertEquals(setOf("open", "closed"), dao.getAllByOpticaIdList("o1").map { it.id }.toSet())
        assertEquals("2026-06-30", dao.getById("closed")?.vigenteHasta)
        assertNull(dao.getById("missing"))
    }

    @Test
    fun `biselado upload query returns open and closed versions of the optica only`() = runTest {
        val dao = db.costoBiseladoDao()
        dao.upsertAll(listOf(biselado("open", "o1", null), biselado("closed", "o1", "2026-06-30"), biselado("other", "o2", null)))

        assertEquals(setOf("open", "closed"), dao.getAllByOpticaIdList("o1").map { it.id }.toSet())
        assertEquals("2026-06-30", dao.getById("closed")?.vigenteHasta)
        assertNull(dao.getById("missing"))
    }
}
