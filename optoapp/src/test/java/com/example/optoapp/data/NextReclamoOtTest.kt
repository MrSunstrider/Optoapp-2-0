package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class NextReclamoOtTest {

    private lateinit var db: OptoDatabase
    private lateinit var dispensacionDao: DispensacionDao
    private lateinit var repo: DispensacionRepository

    private val claimDate = LocalDate.of(2026, 10, 1)

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dispensacionDao = db.dispensacionDao()
        repo = DispensacionRepository(dispensacionDao, db.dispensacionItemDao(), db.pagoDao(), db.servicioExtraDao())
        listOf("o1", "o2").forEach { opticaId ->
            db.pacienteDao().insertPaciente(
                Paciente(
                    id = "p-$opticaId",
                    nombreCompleto = "Paciente",
                    edad = 30,
                    telefono = "000",
                    fechaCreacion = LocalDate.of(2026, 1, 15),
                    opticaId = opticaId,
                ),
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertOrder(
        id: String,
        ot: String,
        opticaId: String = "o1",
        reclamoOrigenId: String? = null,
    ) {
        dispensacionDao.insertDispensacion(
            DispensacionOptica(
                id = id,
                ot = ot,
                pacienteId = "p-$opticaId",
                fecha = LocalDate.of(2026, 9, 1),
                opticaId = opticaId,
                reclamoOrigenId = reclamoOrigenId,
            ),
        )
    }

    @Test
    fun firstClaim_appendsR1() = runTest {
        insertOrder("d1", "OT-2026-0042")

        assertEquals("OT-2026-0042-R1", repo.nextReclamoOt("o1", "OT-2026-0042", claimDate))
    }

    @Test
    fun existingReplacements_useHighestSuffixPlusOne() = runTest {
        insertOrder("d1", "OT-2026-0042")
        insertOrder("d2", "OT-2026-0042-R1")
        insertOrder("d3", "OT-2026-0042-R3")

        assertEquals("OT-2026-0042-R4", repo.nextReclamoOt("o1", "OT-2026-0042", claimDate))
    }

    @Test
    fun claimingAReplacement_stripsItsSuffixCaseInsensitively() = runTest {
        insertOrder("d1", "OT-2026-0042")
        insertOrder("d2", "OT-2026-0042-R1")
        insertOrder("d3", "ot-2026-0042-r2")

        assertEquals("OT-2026-0042-R3", repo.nextReclamoOt("o1", " OT-2026-0042-r1 ", claimDate))
    }

    @Test
    fun unrelatedPrefixesAndOtherOpticas_areIgnored() = runTest {
        insertOrder("d1", "OT-2026-0042")
        insertOrder("d2", "OT-2026-00421-R7")
        insertOrder("d3", "OT-2026-0042-R1-X")
        insertOrder("d4", "OT-2026-0042-R5", opticaId = "o2")

        assertEquals("OT-2026-0042-R1", repo.nextReclamoOt("o1", "OT-2026-0042", claimDate))
    }

    @Test
    fun blankOriginalOt_fallsBackToSuggestNextOtForThePassedFecha() = runTest {
        insertOrder("d1", "OT-2025-0007")
        insertOrder("d2", "OT-2026-0042")

        assertEquals("OT-2025-0008", repo.nextReclamoOt("o1", "  ", LocalDate.of(2025, 12, 31)))
        assertEquals("OT-2026-0043", repo.nextReclamoOt("o1", "", claimDate))
    }

    @Test
    fun getByReclamoOrigenId_returnsTheReplacementOfTheOriginal() = runTest {
        insertOrder("d1", "OT-2026-0042")
        insertOrder("d2", "OT-2026-0042-R1", reclamoOrigenId = "d1")
        insertOrder("d3", "OT-2026-0042-R1", opticaId = "o2", reclamoOrigenId = "d1")

        assertEquals("d2", dispensacionDao.getByReclamoOrigenId("d1", "o1")?.id)
        assertNull(dispensacionDao.getByReclamoOrigenId("d2", "o1"))
    }
}
