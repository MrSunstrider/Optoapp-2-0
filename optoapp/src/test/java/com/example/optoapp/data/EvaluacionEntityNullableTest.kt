package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

/**
 * Verifies that EvaluacionClinica with all optional fields set to null can
 * be inserted into Room and read back without crashing.
 *
 * The entity declares nullable fields (String?, Boolean?, List<String>?) matching
 * Supabase schema where these columns may be NULL. Room must not throw when
 * writing or reading nulls for these columns.
 */
@RunWith(RobolectricTestRunner::class)
class EvaluacionEntityNullableTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: EvaluacionDao
    private lateinit var pacienteDao: PacienteDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.evaluacionDao()
        pacienteDao = db.pacienteDao()

        // FK constraint requires the parent Paciente to exist
        runBlocking {
            pacienteDao.insertPaciente(
                Paciente(
                    id = "p1",
                    nombreCompleto = "Test Patient",
                    edad = 30,
                    telefono = "999",
                    fechaCreacion = LocalDate.now(),
                    opticaId = "optica-1",
                )
            )
        }
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertEvaluacionWithNullOptionals_doesNotCrash() = runBlocking {
        val ev = EvaluacionClinica(
            id = "e1",
            pacienteId = "p1",
            fecha = LocalDate.now(),
            opticaId = "optica-1",
        )

        dao.insertEvaluacion(ev)
        val loaded = dao.getEvaluacionById("e1", "optica-1")

        assertNotNull("Entity must be retrievable after insert", loaded)
    }

    @Test
    fun nullableStringFields_readAsNull_whenNotProvided() = runBlocking {
        val ev = EvaluacionClinica(
            id = "e2",
            pacienteId = "p1",
            fecha = LocalDate.now(),
            opticaId = "optica-1",
        )

        dao.insertEvaluacion(ev)
        val loaded = dao.getEvaluacionById("e2", "optica-1")

        assertNotNull(loaded)
        assertNull("motivoConsulta must be null when not provided", loaded?.motivoConsulta)
        assertNull("sintomas must be null when not provided", loaded?.sintomas)
        assertNull("antecedentesPersonalesOculares must be null when not provided", loaded?.antecedentesPersonalesOculares)
        assertNull("planTratamiento must be null when not provided", loaded?.planTratamiento)
        assertNull("observaciones must be null when not provided", loaded?.observaciones)
    }

    @Test
    fun nullableBooleanFields_readAsNull_whenNotProvided() = runBlocking {
        val ev = EvaluacionClinica(
            id = "e3",
            pacienteId = "p1",
            fecha = LocalDate.now(),
            opticaId = "optica-1",
        )

        dao.insertEvaluacion(ev)
        val loaded = dao.getEvaluacionById("e3", "optica-1")

        assertNotNull(loaded)
        assertNull("balanceOd must be null when not provided", loaded?.balanceOd)
        assertNull("balanceOi must be null when not provided", loaded?.balanceOi)
        assertNull("otrosPresbicia must be null when not provided", loaded?.otrosPresbicia)
        assertNull("otrosAnisometropia must be null when not provided", loaded?.otrosAnisometropia)
        assertNull("otrosAmbliopia must be null when not provided", loaded?.otrosAmbliopia)
        assertNull("autoPresbicia must be null when not provided", loaded?.autoPresbicia)
        assertNull("autoAnisometropia must be null when not provided", loaded?.autoAnisometropia)
        assertNull("autoAmbliopia must be null when not provided", loaded?.autoAmbliopia)
    }

    @Test
    fun nullableListFields_readAsNull_whenNotProvided() = runBlocking {
        val ev = EvaluacionClinica(
            id = "e4",
            pacienteId = "p1",
            fecha = LocalDate.now(),
            opticaId = "optica-1",
        )

        dao.insertEvaluacion(ev)
        val loaded = dao.getEvaluacionById("e4", "optica-1")

        assertNotNull(loaded)
        assertNull("necesidadVisual must be null when not provided", loaded?.necesidadVisual)
        assertNull("diagnosticoOd must be null when not provided", loaded?.diagnosticoOd)
        assertNull("diagnosticoOi must be null when not provided", loaded?.diagnosticoOi)
        assertNull("diagnosticoOtros must be null when not provided", loaded?.diagnosticoOtros)
    }

    @Test
    fun entityWithAllFieldsProvided_stillWorksCorrectly() = runBlocking {
        val ev = EvaluacionClinica(
            id = "e5",
            pacienteId = "p1",
            fecha = LocalDate.now(),
            opticaId = "optica-1",
            motivoConsulta = "Dolor de cabeza",
            sintomas = "Vision borrosa",
            necesidadVisual = listOf("lejos", "cerca"),
            balanceOd = true,
            balanceOi = false,
            diagnosticoOd = listOf("Miopia"),
            otrosPresbicia = true,
            autoAnisometropia = false,
        )

        dao.insertEvaluacion(ev)
        val loaded = dao.getEvaluacionById("e5", "optica-1")

        assertNotNull(loaded)
        assertNotNull("motivoConsulta should not be null when provided", loaded?.motivoConsulta)
    }
}
