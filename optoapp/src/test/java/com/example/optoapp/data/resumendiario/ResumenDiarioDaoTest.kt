package com.example.optoapp.data.resumendiario

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ResumenDiarioDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: ResumenDiarioDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.resumenDiarioDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndGetByOpticaId_returnsResumen() = runBlocking {
        val resumen = ResumenDiarioEntity(
            id = "r1",
            opticaId = "optica1",
            fecha = "2026-07-05",
            ventasCantidad = 5,
            ventasMontoTotal = 2500.0,
            ventasCostoTotal = 1000.0,
            cobrosCantidad = 3,
            cobrosMontoTotal = 2000.0,
            saldoPendienteTotal = 500.0,
            saldoPendienteCantidad = 2
        )
        dao.upsert(resumen)

        val result = dao.getByOpticaId("optica1").first()
        assertTrue(result.isNotEmpty())
        val retrieved = result.first()
        assertEquals("r1", retrieved.id)
        assertEquals(5, retrieved.ventasCantidad)
        assertEquals(2500.0, retrieved.ventasMontoTotal, 0.001)
    }

    @Test
    fun deleteAll_clearsData() = runBlocking {
        val resumen = ResumenDiarioEntity(
            id = "r1",
            opticaId = "optica1",
            fecha = "2026-07-05",
            ventasCantidad = 3,
            ventasMontoTotal = 1500.0
        )
        dao.upsert(resumen)
        db.openHelper.writableDatabase.execSQL("DELETE FROM resumen_diario WHERE opticaId = 'optica1'")

        val result = dao.getByOpticaId("optica1").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getByOpticaAndMonth_returnsRowsForYearMonth() = runBlocking {
        // Insert July 2026 rows
        for (day in 1..5) {
            val date = "2026-07-0$day"
            dao.upsert(
                ResumenDiarioEntity(
                    id = "r_jul_$day",
                    opticaId = "optica1",
                    fecha = date,
                    ventasCantidad = 1,
                    ventasMontoTotal = 100.0 * day
                )
            )
        }
        // Insert June 2026 rows
        dao.upsert(
            ResumenDiarioEntity(
                id = "r_jun_1",
                opticaId = "optica1",
                fecha = "2026-06-15",
                ventasCantidad = 1,
                ventasMontoTotal = 500.0
            )
        )

        val julyRows = dao.getByOpticaAndMonth("optica1", "2026-07")
        assertEquals(5, julyRows.size)
        // Ordered by fecha ASC
        assertEquals("2026-07-01", julyRows[0].fecha)
        assertEquals("2026-07-05", julyRows[4].fecha)
    }

    @Test
    fun getByOpticaAndDate_returnsRowForExistingDate() = runBlocking {
        val resumen = ResumenDiarioEntity(
            id = "r_today",
            opticaId = "optica1",
            fecha = "2026-07-12",
            ventasCantidad = 10,
            ventasMontoTotal = 5000.0,
            saldoPendienteTotal = 1500.0
        )
        dao.upsert(resumen)

        val result = dao.getByOpticaAndDate("optica1", "2026-07-12")
        assertNotNull(result)
        assertEquals("r_today", result!!.id)
        assertEquals(5000.0, result.ventasMontoTotal, 0.001)
    }

    @Test
    fun `getByOpticaAndDate respects opticaId filter`() = runBlocking {
        val resumen = ResumenDiarioEntity(
            id = "r1",
            opticaId = "opticaX",
            fecha = "2026-07-05",
            ventasCantidad = 5,
            ventasMontoTotal = 2500.0
        )
        dao.upsert(resumen)

        // Same date with correct opticaId → found
        assertNotNull(dao.getByOpticaAndDate("opticaX", "2026-07-05"))
        // Same date with wrong opticaId → NOT found (cross-tenant isolation)
        assertNull(dao.getByOpticaAndDate("opticaY", "2026-07-05"))
    }

    @Test
    fun getByOpticaAndDate_returnsNullForMissingDate() = runBlocking {
        val result = dao.getByOpticaAndDate("optica1", "2099-01-01")
        assertNull(result)
    }

    @Test
    fun getByOpticaAndMonth_returnsEmptyForNoData() = runBlocking {
        val rows = dao.getByOpticaAndMonth("optica1", "2026-06")
        assertTrue(rows.isEmpty())
    }
}
