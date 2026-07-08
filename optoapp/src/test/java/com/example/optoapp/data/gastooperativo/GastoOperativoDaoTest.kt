package com.example.optoapp.data.gastooperativo

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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class GastoOperativoDaoTest {
    private lateinit var db: OptoDatabase
    private lateinit var dao: GastoOperativoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.gastoOperativoDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetAll_returnsInsertedGasto() = runBlocking {
        val gasto = GastoOperativoEntity(
            id = "g1",
            opticaId = "optica1",
            categoria = "servicios",
            descripcion = "Luz",
            monto = 500.0,
            fecha = LocalDate.parse("2026-07-01")
        )
        dao.insert(gasto)

        val gastos = dao.getByOpticaId("optica1").first()
        assertEquals(1, gastos.size)
        assertEquals("g1", gastos[0].id)
        assertEquals(500.0, gastos[0].monto, 0.001)
    }

    @Test
    fun delete_removesGasto() = runBlocking {
        val gasto = GastoOperativoEntity(
            id = "g1", opticaId = "optica1", categoria = "otro",
            descripcion = "Temp", monto = 50.0,
            fecha = LocalDate.parse("2026-07-01")
        )
        dao.insert(gasto)
        dao.delete("g1", "optica1")

        val gastos = dao.getByOpticaId("optica1").first()
        assertTrue(gastos.isEmpty())
    }

    @Test
    fun upsert_updatesExisting() = runBlocking {
        val original = GastoOperativoEntity(
            id = "g1", opticaId = "optica1", categoria = "alquiler",
            descripcion = "Original", monto = 800.0,
            fecha = LocalDate.parse("2026-07-01")
        )
        dao.upsert(original)

        val updated = original.copy(monto = 1200.0, descripcion = "Actualizado")
        dao.upsert(updated)

        val gastos = dao.getByOpticaId("optica1").first()
        assertEquals(1, gastos.size)
        assertEquals(1200.0, gastos[0].monto, 0.001)
        assertEquals("Actualizado", gastos[0].descripcion)
    }
}
