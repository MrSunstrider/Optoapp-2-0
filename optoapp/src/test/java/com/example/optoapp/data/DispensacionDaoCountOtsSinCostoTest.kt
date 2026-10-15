package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.domain.ClaseCobertura
import com.example.optoapp.domain.CoberturaCostoClassifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

// Real-Room DAO test (AGENTS.md exception): the predicate under test is SQL, and Robolectric only supplies
// the Context that Room.inMemoryDatabaseBuilder needs.
@RunWith(RobolectricTestRunner::class)
class DispensacionDaoCountOtsSinCostoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: DispensacionDao
    private val opticaId = "optica-1"
    private val pacienteId = "paciente-1"
    private val desde = LocalDate.of(2026, 5, 1)
    private val hasta = LocalDate.of(2026, 5, 31)

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.dispensacionDao()
        db.pacienteDao().insertPaciente(
            Paciente(
                id = pacienteId,
                nombreCompleto = "Ana Perez",
                edad = 30,
                telefono = "999",
                fechaCreacion = desde,
                opticaId = opticaId,
            ),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun countsEveryItemTheClassifierCallsFaltante() = runBlocking {
        val vectores = listOf(
            Vector("faltante", "faltante", 50.0),
            Vector("legacy-sin-costo", null, 0.0),
            Vector("origen-desconocido", "importado", 40.0),
            Vector("manual", "manual", 40.0),
            Vector("matriz", "matriz", 40.0),
            Vector("mixto", "mixto", 40.0),
            Vector("legacy-con-costo", null, 25.0),
        )
        vectores.forEach { orden(it.id, items = listOf(item("${it.id}-1", it.id, it.origen, it.costo))) }

        val esperado = vectores.count {
            CoberturaCostoClassifier.claseItem(it.origen, it.costo) == ClaseCobertura.FALTANTE
        }
        assertEquals(3, esperado)
        assertEquals(esperado, contar())
    }

    @Test
    fun oneMissingItemMakesTheWholeOtCountOnce() = runBlocking {
        orden(
            "ot-1",
            items = listOf(
                item("a", "ot-1", "manual", 30.0),
                item("b", "ot-1", "faltante", 0.0),
                item("c", "ot-1", "faltante", 0.0),
            ),
        )

        assertEquals(1, contar())
    }

    @Test
    fun anuladasAndReclamadasAreExcluded() = runBlocking {
        orden("anulada", estado = "Anulado", items = listOf(item("a", "anulada", "faltante", 0.0)))
        orden("reclamada", estado = "Reclamada", items = listOf(item("b", "reclamada", "faltante", 0.0)))
        orden("entregada", estado = "Entregado", items = listOf(item("c", "entregada", "faltante", 0.0)))

        assertEquals(1, contar())
    }

    @Test
    fun monthBoundsAreInclusive() = runBlocking {
        orden("primero", fecha = desde, items = listOf(item("a", "primero", "faltante", 0.0)))
        orden("ultimo", fecha = hasta, items = listOf(item("b", "ultimo", "faltante", 0.0)))
        orden("abril", fecha = desde.minusDays(1), items = listOf(item("c", "abril", "faltante", 0.0)))
        orden("junio", fecha = hasta.plusDays(1), items = listOf(item("d", "junio", "faltante", 0.0)))

        assertEquals(2, contar())
    }

    @Test
    fun otherOpticasAreNotCounted() = runBlocking {
        db.pacienteDao().insertPaciente(
            Paciente(
                id = "paciente-2",
                nombreCompleto = "Luis Diaz",
                edad = 40,
                telefono = "888",
                fechaCreacion = desde,
                opticaId = "optica-2",
            ),
        )
        dao.insertDispensacion(DispensacionOptica(id = "ajena", pacienteId = "paciente-2", fecha = desde, opticaId = "optica-2"))
        db.dispensacionItemDao().insertItem(item("a", "ajena", "faltante", 0.0, opticaDelItem = "optica-2"))

        assertEquals(0, contar())
    }

    @Test
    fun servicioExtraRowsAreNeverCounted() = runBlocking {
        db.servicioExtraDao().insertServicio(
            ServicioExtra(
                id = "servicio-1",
                descripcion = "Ajuste",
                montoTotal = 20.0,
                estado = "Pendiente",
                fecha = desde,
                opticaId = opticaId,
            ),
        )

        assertEquals(0, contar())
    }

    private suspend fun contar(): Int = dao.countOtsSinCosto(opticaId, desde, hasta).first()

    private suspend fun orden(
        id: String,
        estado: String = "Pendiente",
        fecha: LocalDate = LocalDate.of(2026, 5, 15),
        items: List<DispensacionItem>,
    ) {
        dao.insertDispensacion(
            DispensacionOptica(id = id, pacienteId = pacienteId, fecha = fecha, opticaId = opticaId, estadoEntrega = estado),
        )
        items.forEach { db.dispensacionItemDao().insertItem(it) }
    }

    private fun item(
        id: String,
        dispensacionId: String,
        origen: String?,
        costo: Double,
        opticaDelItem: String = opticaId,
    ) = DispensacionItem(
        id = id,
        dispensacionId = dispensacionId,
        opticaId = opticaDelItem,
        costoRealOd = costo,
        costoOrigen = origen,
    )

    private data class Vector(val id: String, val origen: String?, val costo: Double)
}
