package com.example.optoapp.data.resumendiario

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
        dao.deleteAll()

        val result = dao.getByOpticaId("optica1").first()
        assertTrue(result.isEmpty())
    }
}
