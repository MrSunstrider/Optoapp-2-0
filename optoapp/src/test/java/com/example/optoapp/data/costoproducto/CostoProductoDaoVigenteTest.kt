package com.example.optoapp.data.costoproducto

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
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
class CostoProductoDaoVigenteTest {

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
        id: String,
        costo: Double,
        desde: String,
        hasta: String? = null,
        serie: Int? = 2,
    ) = CostoProductoEntity(
        id = id,
        opticaId = "o1",
        material = "Resina",
        tipoLente = "Monofocal",
        stockOFabricacion = "stock",
        tratamiento = "Antireflex",
        serie = serie,
        costoUnitario = costo,
        vigenteDesde = desde,
        vigenteHasta = hasta,
    )

    private suspend fun costoEn(fecha: String, serie: Int? = 2): Double? = dao.lookupVigente(
        opticaId = "o1",
        material = "Resina",
        tipoLente = "Monofocal",
        stockOFabricacion = "stock",
        tratamiento = "Antireflex",
        serie = serie,
        fecha = fecha,
    )?.costoUnitario

    @Test
    fun lookupVigente_resolvesTheVersionValidOnTheOtDate() = runTest {
        dao.upsertAll(
            listOf(
                fila("p18", 18.0, desde = "2026-01-01", hasta = "2026-07-31"),
                fila("p20", 20.0, desde = "2026-08-01"),
            ),
        )

        assertEquals(18.0, costoEn("2026-07-15")!!, 0.001)
        assertEquals(18.0, costoEn("2026-07-31")!!, 0.001)
        assertEquals(20.0, costoEn("2026-08-01")!!, 0.001)
        assertEquals(20.0, costoEn("2026-08-02")!!, 0.001)
    }

    @Test
    fun lookupVigente_beforeAnyVersion_returnsNull() = runTest {
        dao.upsertAll(listOf(fila("p18", 18.0, desde = "2026-01-01")))

        assertNull(costoEn("2025-12-31"))
    }

    @Test
    fun lookupVigente_twoOpenRows_greatestVigenteDesdeWins() = runTest {
        dao.upsertAll(
            listOf(
                fila("a", 18.0, desde = "2026-01-01"),
                fila("b", 22.0, desde = "2026-06-01"),
            ),
        )

        assertEquals(22.0, costoEn("2026-07-15")!!, 0.001)
        assertEquals(18.0, costoEn("2026-05-31")!!, 0.001)
    }

    @Test
    fun lookupVigente_nullSerieMatchesOnlyNullSerieRows() = runTest {
        dao.upsertAll(
            listOf(
                fila("fab", 30.0, desde = "2026-01-01", serie = null),
                fila("s2", 18.0, desde = "2026-01-01", serie = 2),
            ),
        )

        assertEquals(30.0, costoEn("2026-07-15", serie = null)!!, 0.001)
        assertEquals(18.0, costoEn("2026-07-15", serie = 2)!!, 0.001)
        assertNull(costoEn("2026-07-15", serie = 3))
    }
}
