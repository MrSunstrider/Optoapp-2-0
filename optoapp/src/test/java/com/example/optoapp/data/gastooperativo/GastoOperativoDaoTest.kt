package com.example.optoapp.data.gastooperativo

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
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class GastoOperativoDaoTest {
    private lateinit var db: OptoDatabase
    private lateinit var dao: GastoOperativoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
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
            monto = BigDecimal.valueOf(500.0),
            fecha = LocalDate.parse("2026-07-01"),
        )
        dao.insert(gasto)

        val gastos = dao.getByOpticaId("optica1").first()
        assertEquals(1, gastos.size)
        assertEquals("g1", gastos[0].id)
        assertEquals(BigDecimal.valueOf(500.0), gastos[0].monto)
    }

    @Test
    fun delete_removesGasto() = runBlocking {
        val gasto = GastoOperativoEntity(
            id = "g1",
            opticaId = "optica1",
            categoria = "otro",
            descripcion = "Temp",
            monto = BigDecimal.valueOf(50.0),
            fecha = LocalDate.parse("2026-07-01"),
        )
        dao.insert(gasto)
        dao.delete("g1", "optica1")

        val gastos = dao.getByOpticaId("optica1").first()
        assertTrue(gastos.isEmpty())
    }

    @Test
    fun upsert_updatesExisting() = runBlocking {
        val original = GastoOperativoEntity(
            id = "g1",
            opticaId = "optica1",
            categoria = "alquiler",
            descripcion = "Original",
            monto = BigDecimal.valueOf(800.0),
            fecha = LocalDate.parse("2026-07-01"),
        )
        dao.upsert(original)

        val updated = original.copy(monto = BigDecimal.valueOf(1200.0), descripcion = "Actualizado")
        dao.upsert(updated)

        val gastos = dao.getByOpticaId("optica1").first()
        assertEquals(1, gastos.size)
        assertEquals(BigDecimal.valueOf(1200.0), gastos[0].monto)
        assertEquals("Actualizado", gastos[0].descripcion)
    }

    @Test
    fun getByOpticaBetween_returnsOnlyThatOpticasMonthWithInclusiveBoundsOldestFirst() = runBlocking {
        fun gasto(id: String, opticaId: String, fecha: String) = GastoOperativoEntity(
            id = id,
            opticaId = opticaId,
            categoria = "alquiler",
            monto = BigDecimal.valueOf(100.0),
            fecha = LocalDate.parse(fecha),
        )
        dao.insert(gasto("antes", "optica1", "2026-06-30"))
        dao.insert(gasto("fin", "optica1", "2026-07-31"))
        dao.insert(gasto("inicio", "optica1", "2026-07-01"))
        dao.insert(gasto("medio", "optica1", "2026-07-15"))
        dao.insert(gasto("despues", "optica1", "2026-08-01"))
        dao.insert(gasto("ajena", "optica2", "2026-07-10"))

        val delMes = dao.getByOpticaBetween("optica1", LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"))

        assertEquals(listOf("inicio", "medio", "fin"), delMes.map { it.id })
    }

    @Test
    fun tipoGasto_roundTripsAndALegacyNullStaysNull() = runBlocking {
        val base = GastoOperativoEntity(
            id = "g-var",
            opticaId = "optica1",
            categoria = "alquiler",
            monto = BigDecimal.valueOf(300.0),
            fecha = LocalDate.parse("2026-07-05"),
            tipoGasto = "variable",
        )
        dao.upsert(base)
        dao.upsert(base.copy(id = "g-legacy", tipoGasto = null))

        val porId = dao.getByOpticaIdList("optica1").associateBy { it.id }

        assertEquals("variable", porId.getValue("g-var").tipoGasto)
        assertNull(porId.getValue("g-legacy").tipoGasto)
    }
}
