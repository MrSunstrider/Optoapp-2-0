package com.example.optoapp.data.costoproducto

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Real-Room DAO test (AGENTS.md exception): in-memory Room needs the Android Context Robolectric provides.
@RunWith(RobolectricTestRunner::class)
class CostoProductoDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: CostoProductoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.costoProductoDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun fila(
        id: String = "cp1",
        opticaId: String = "optica1",
        tratamiento: String? = "Antireflex",
        stockOFabricacion: String = "stock",
        serie: Int? = 2,
        costo: Double = 18.0,
    ) = CostoProductoEntity(
        id = id,
        opticaId = opticaId,
        material = "Resina",
        tipoLente = "Monofocal",
        stockOFabricacion = stockOFabricacion,
        tratamiento = tratamiento,
        serie = serie,
        costoUnitario = costo,
        vigenteDesde = "2026-01-01",
        vigenteHasta = null,
    )

    private suspend fun lookup(opticaId: String = "optica1", tratamiento: String? = "Antireflex", serie: Int? = 2) =
        dao.lookupVigente(
            opticaId = opticaId,
            material = "Resina",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            tratamiento = tratamiento,
            serie = serie,
            fecha = "2026-06-01",
        )

    @Test
    fun lookupVigente_byMaterialTipoStockTratamientoSerie_returnsTheRow() = runTest {
        dao.upsertAll(listOf(fila()))

        assertEquals(18.0, lookup()!!.costoUnitario, 0.001)
    }

    @Test
    fun lookupVigente_unknownTratamiento_returnsNull() = runTest {
        dao.upsertAll(listOf(fila()))

        assertNull(lookup(tratamiento = "Nonexistent"))
    }

    @Test
    fun lookupVigente_isScopedToTheOptica() = runTest {
        dao.upsertAll(listOf(fila()))

        assertNull(lookup(opticaId = "optica-other"))
        assertEquals("cp1", lookup()!!.id)
    }

    @Test
    fun getByBloque_returnsEntitiesForBlock() = runTest {
        dao.upsertAll(
            listOf(
                fila(id = "cp-s1", tratamiento = "AR", serie = 1),
                fila(id = "cp-f1", tratamiento = "Simple", stockOFabricacion = "fabricacion", serie = null),
            ),
        )

        val stockResults = dao.getByBloque(opticaId = "optica1", bloque = "stock").first()

        assertEquals(listOf("cp-s1"), stockResults.map { it.id })
    }

    @Test
    fun upsertAll_updatesExistingEntity() = runTest {
        val original = fila(costo = 5.0)
        dao.upsertAll(listOf(original))

        dao.upsertAll(listOf(original.copy(costoUnitario = 6.0)))

        assertEquals(6.0, lookup()!!.costoUnitario, 0.001)
    }
}
