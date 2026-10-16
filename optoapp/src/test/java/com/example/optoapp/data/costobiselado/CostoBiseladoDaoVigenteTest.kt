package com.example.optoapp.data.costobiselado

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Real-Room DAO test (AGENTS.md exception): in-memory Room needs the Android Context Robolectric provides.
@RunWith(RobolectricTestRunner::class)
class CostoBiseladoDaoVigenteTest {

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

    private fun fila(
        id: String,
        altoIndice: String?,
        desde: String = "2026-01-01",
        hasta: String? = null,
        serie: Int? = 2,
        stockOFabricacion: String = "stock",
        opticaId: String = "o1",
    ) = CostoBiseladoEntity(
        id = id,
        opticaId = opticaId,
        material = "Resina",
        tipoAro = "aro_completo",
        stockOFabricacion = stockOFabricacion,
        serie = serie,
        altoIndice = altoIndice,
        costoPorPar = 10.0,
        vigenteDesde = desde,
        vigenteHasta = hasta,
    )

    private suspend fun candidatos(
        fecha: String,
        serie: Int? = 2,
        stockOFabricacion: String = "stock",
    ): List<String> = dao.lookupVigente(
        opticaId = "o1",
        material = "Resina",
        tipoAro = "aro_completo",
        stockOFabricacion = stockOFabricacion,
        serie = serie,
        fecha = fecha,
    ).map { it.id }

    @Test
    fun lookupVigente_returnsEveryAltoIndiceCandidateValidOnTheDate() = runTest {
        dao.upsertAll(
            listOf(
                fila("estandar", altoIndice = null),
                fila("uno-cinco", altoIndice = "1.5"),
                fila("uno-siete", altoIndice = "1,70"),
                fila("cerrada", altoIndice = null, hasta = "2026-06-30"),
                fila("otra-serie", altoIndice = null, serie = 3),
                fila("otra-optica", altoIndice = null, opticaId = "o2"),
            ),
        )

        assertEquals(setOf("estandar", "uno-cinco", "uno-siete"), candidatos("2026-07-15").toSet())
        assertEquals(
            setOf("estandar", "uno-cinco", "uno-siete", "cerrada"),
            candidatos("2026-06-30").toSet(),
        )
    }

    @Test
    fun lookupVigente_ordersCandidatesByNewestVigenteDesde() = runTest {
        dao.upsertAll(
            listOf(
                fila("vieja", altoIndice = null, desde = "2026-01-01"),
                fila("nueva", altoIndice = null, desde = "2026-06-01"),
                fila("futura", altoIndice = null, desde = "2026-09-01"),
            ),
        )

        assertEquals(listOf("nueva", "vieja"), candidatos("2026-07-15"))
    }

    @Test
    fun lookupVigente_fabricacionNullSerieMatchesOnlyNullSerieRows() = runTest {
        dao.upsertAll(
            listOf(
                fila("fab", altoIndice = null, serie = null, stockOFabricacion = "fabricacion"),
                fila("fab-serie", altoIndice = null, serie = 1, stockOFabricacion = "fabricacion"),
            ),
        )

        assertEquals(listOf("fab"), candidatos("2026-07-15", serie = null, stockOFabricacion = "fabricacion"))
    }
}
