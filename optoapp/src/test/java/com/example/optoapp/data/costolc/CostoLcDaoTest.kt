package com.example.optoapp.data.costolc

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
    fun tearDown() {
        db.close()
    }

    private fun row(
        id: String,
        desde: String,
        hasta: String? = null,
        modalidad: String = "mensual",
        laboratorioId: String? = null,
        materialLc: String = "Hydrogel",
    ) = CostoLcEntity(
        id = id,
        opticaId = "o1",
        tipoLc = "graduado",
        materialLc = materialLc,
        modalidad = modalidad,
        laboratorioId = laboratorioId,
        costoUnitario = 45.0,
        vigenteDesde = desde,
        vigenteHasta = hasta,
    )

    @Test
    fun lookupVigenteReturnsTheVersionsValidOnTheOtDateNewestFirst() = runTest {
        dao.upsertAll(
            listOf(
                row("old", desde = "2026-01-01", hasta = "2026-07-31"),
                row("new", desde = "2026-08-01"),
                row("diario", desde = "2026-02-01", modalidad = "diario"),
                row("rgp", desde = "2026-01-01", materialLc = "RGP"),
            ),
        )

        val julio = dao.lookupVigente("o1", "graduado", "Hydrogel", null, "2026-07-15")
        val agosto = dao.lookupVigente("o1", "graduado", "Hydrogel", null, "2026-08-02")

        assertEquals(listOf("diario", "old"), julio.map { it.id })
        assertEquals(listOf("new", "diario"), agosto.map { it.id })
    }

    @Test
    fun lookupVigenteFiltersByLaboratorioOnlyWhenTheItemHasOne() = runTest {
        dao.upsertAll(listOf(row("lab1", "2026-01-01", laboratorioId = "lab1"), row("lab2", "2026-01-02", laboratorioId = "lab2")))

        assertEquals(listOf("lab1"), dao.lookupVigente("o1", "graduado", "Hydrogel", "lab1", "2026-07-15").map { it.id })
        assertEquals(listOf("lab2", "lab1"), dao.lookupVigente("o1", "graduado", "Hydrogel", null, "2026-07-15").map { it.id })
    }
}
