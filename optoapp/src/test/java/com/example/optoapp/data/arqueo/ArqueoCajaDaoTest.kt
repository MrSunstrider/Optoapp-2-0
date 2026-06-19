package com.example.optoapp.data.arqueo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ArqueoCajaDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: ArqueoCajaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.arqueoCajaDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    private fun buildArqueo(id: String, fecha: LocalDate, opticaId: String, fondoCaja: Double = 100.0) =
        ArqueoCaja(
            id = id,
            fecha = fecha,
            opticaId = opticaId,
            fondoCaja = fondoCaja,
            efectivoContado = 0.0,
            tarjetaContado = 0.0,
            transferenciaContado = 0.0,
            movilContado = 0.0,
            efectivoCobrado = 0.0,
            tarjetaCobrado = 0.0,
            transferenciaCobrado = 0.0,
            movilCobrado = 0.0,
            diferenciaEfectivo = 0.0,
            diferenciaTarjeta = 0.0,
            diferenciaTransferencia = 0.0,
            diferenciaMovil = 0.0,
            diferenciaTotal = 0.0,
            cerradoPor = "test@test.com",
            sellado = true,
            createdAt = "2026-06-18T10:00:00Z",
            updatedAt = "2026-06-18T10:00:00Z"
        )

    /**
     * REQ-6: Inserting an arqueo twice for the same (fecha, opticaId)
     * MUST update the existing record, not throw an exception.
     */
    @Test
    fun insertArqueo_twice_for_same_fecha_and_opticaId_updates_existing_record() = runBlocking {
        val fecha = LocalDate.of(2026, 6, 18)
        val opticaId = "optica-test"

        val first = buildArqueo("id-1", fecha, opticaId, fondoCaja = 100.0)
        val second = buildArqueo("id-2", fecha, opticaId, fondoCaja = 200.0)

        // Insert first time — must succeed
        dao.insertArqueo(first)

        // Insert second time for same fecha+opticaId — must NOT throw, must REPLACE
        dao.insertArqueo(second)

        // Only one row should exist (the second one replaced the first)
        val result = dao.getArqueoByFechaAndOptica(fecha, opticaId).first()
        assertEquals("After two inserts for same PK, only the updated record should exist", 200.0, result!!.fondoCaja, 0.001)
    }

    /**
     * REQ-6 (positive path): Inserting an arqueo for a new date inserts successfully.
     */
    @Test
    fun insertArqueo_for_new_date_inserts_successfully() = runBlocking {
        val fecha = LocalDate.of(2026, 6, 17)
        val opticaId = "optica-test"

        val arqueo = buildArqueo("id-new", fecha, opticaId, fondoCaja = 500.0)
        dao.insertArqueo(arqueo)

        val result = dao.getArqueoByFechaAndOptica(fecha, opticaId).first()
        assertEquals("New arqueo insert must succeed and be immediately readable", 500.0, result!!.fondoCaja, 0.001)
    }

    /**
     * Returns null when no record exists for the given (fecha, opticaId).
     */
    @Test
    fun getArqueoByFechaAndOptica_returns_null_when_no_record_exists() = runBlocking {
        val result = dao.getArqueoByFechaAndOptica(LocalDate.of(2099, 1, 1), "never-inserted").first()
        assertNull("Query must return null when no arqueo exists for that date and opticaId", result)
    }

    /**
     * REPLACE strategy overwrites all fields — including cerradoPor — when a second arqueo
     * is inserted for the same (fecha, opticaId).
     */
    @Test
    fun insertArqueo_replace_overwrites_cerradoPor_field() = runBlocking {
        val fecha = LocalDate.of(2026, 6, 19)
        val opticaId = "optica-replace-cerrado"

        val first  = buildArqueo("id-c1", fecha, opticaId).copy(cerradoPor = "first@test.com")
        val second = buildArqueo("id-c2", fecha, opticaId).copy(cerradoPor = "second@test.com")

        dao.insertArqueo(first)
        dao.insertArqueo(second)

        val result = dao.getArqueoByFechaAndOptica(fecha, opticaId).first()
        assertNotNull("A record must exist after two inserts", result)
        assertEquals(
            "REPLACE must overwrite cerradoPor with the second record's value",
            "second@test.com",
            result!!.cerradoPor
        )
    }

    /**
     * Two arqueos for the same fecha but different opticaIds are independent rows —
     * inserting one must not affect the other.
     */
    @Test
    fun insertArqueo_same_fecha_different_optica_creates_independent_records() = runBlocking {
        val fecha = LocalDate.of(2026, 6, 21)

        dao.insertArqueo(buildArqueo("id-p1", fecha, "optica-A", fondoCaja = 111.0))
        dao.insertArqueo(buildArqueo("id-p2", fecha, "optica-B", fondoCaja = 222.0))

        val resultA = dao.getArqueoByFechaAndOptica(fecha, "optica-A").first()
        val resultB = dao.getArqueoByFechaAndOptica(fecha, "optica-B").first()

        assertNotNull("Record for optica-A must exist", resultA)
        assertNotNull("Record for optica-B must exist", resultB)
        assertEquals("fondoCaja for optica-A must be 111.0", 111.0, resultA!!.fondoCaja, 0.001)
        assertEquals("fondoCaja for optica-B must be 222.0", 222.0, resultB!!.fondoCaja, 0.001)
    }
}
