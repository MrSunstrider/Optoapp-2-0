package com.example.optoapp.data.costolc

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

@RunWith(RobolectricTestRunner::class)
class CostoLcDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: CostoLcDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.costoLcDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    private fun entity(
        id: String = "cl1",
        opticaId: String = "optica1",
        tipoLc: String = "cosmetico",
        materialLc: String = "hidrogel",
        modalidad: String = "mensual",
        laboratorioId: String? = null,
        costo: Double = 20.0,
        vigenteHasta: String? = null,
    ) = CostoLcEntity(
        id = id,
        opticaId = opticaId,
        tipoLc = tipoLc,
        materialLc = materialLc,
        modalidad = modalidad,
        laboratorioId = laboratorioId,
        costoUnitario = costo,
        vigenteDesde = "2026-01-01",
        vigenteHasta = vigenteHasta,
    )

    @Test
    fun lookup_byKeys_returnsMatchingVigenteRow() = runBlocking {
        dao.upsertAll(listOf(entity(laboratorioId = "lab1")))

        val result = dao.lookup("optica1", "cosmetico", "hidrogel", "mensual", "lab1")

        assertTrue(result != null)
        assertEquals("cl1", result!!.id)
        assertEquals(20.0, result.costoUnitario, 0.001)
    }

    @Test
    fun lookup_noMatch_returnsNull() = runBlocking {
        assertNull(dao.lookup("optica1", "graduado", "silicona", "diario", null))
    }

    @Test
    fun lookup_softDeleted_excluded() = runBlocking {
        dao.upsertAll(listOf(entity(vigenteHasta = "2026-07-01")))
        assertNull(dao.lookup("optica1", "cosmetico", "hidrogel", "mensual", null))
    }

    @Test
    fun getByOpticaId_listsVigenteOnly() = runBlocking {
        dao.upsertAll(
            listOf(
                entity(id = "v"),
                entity(id = "d", vigenteHasta = "2026-07-16"),
            ),
        )
        val rows = dao.getByOpticaId("optica1").first()
        assertEquals(1, rows.size)
        assertEquals("v", rows.single().id)
    }

    @Test
    fun upsertAll_updatesExistingCost() = runBlocking {
        dao.upsertAll(listOf(entity(costo = 10.0)))
        dao.upsertAll(listOf(entity(costo = 22.5)))
        val result = dao.lookup("optica1", "cosmetico", "hidrogel", "mensual", null)
        assertEquals(22.5, result!!.costoUnitario, 0.001)
    }
}
