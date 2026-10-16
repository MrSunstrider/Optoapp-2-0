package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

// Real-Room DAO test (AGENTS.md exception): the list query is SQL, and Robolectric only supplies
// the Context that Room.inMemoryDatabaseBuilder needs.
@RunWith(RobolectricTestRunner::class)
class DispensacionDaoOtsSinCostoTest {

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
    fun listsOnlyTheMonthOtsWithAMissingItemNewestFirst() = runBlocking {
        orden("f1", LocalDate.of(2026, 5, 3), listOf(faltante("f1")))
        orden("f2", LocalDate.of(2026, 5, 20), listOf(faltante("f2"), completo("f2")))
        orden("f3", LocalDate.of(2026, 5, 11), listOf(desconocido("f3")))
        (1..5).forEach { orden("c$it", LocalDate.of(2026, 5, it), listOf(completo("c$it"))) }

        val filas = listar()

        assertEquals(listOf("f2", "f3", "f1"), filas.map { it.id })
        assertEquals(dao.countOtsSinCosto(opticaId, desde, hasta).first(), filas.size)
    }

    @Test
    fun rowCarriesIdentificationAmountAndMissingItemCount() = runBlocking {
        orden(
            "ot-1",
            LocalDate.of(2026, 5, 9),
            listOf(faltante("ot-1", "a"), faltante("ot-1", "b"), completo("ot-1", "c")),
            ot = "OT-2026-0042",
            monto = 350.0,
        )

        val fila = listar().single()

        assertEquals(OtSinCostoRow("ot-1", "OT-2026-0042", pacienteId, "Ana Perez", LocalDate.of(2026, 5, 9), 350.0, 2), fila)
    }

    @Test
    fun anuladasReclamadasAndOutOfMonthOtsAreNotListed() = runBlocking {
        orden("anulada", LocalDate.of(2026, 5, 5), listOf(faltante("anulada")), estado = "Anulado")
        orden("reclamada", LocalDate.of(2026, 5, 5), listOf(faltante("reclamada")), estado = "Reclamada")
        orden("primero", desde, listOf(faltante("primero")))
        orden("ultimo", hasta, listOf(faltante("ultimo")))
        orden("junio", hasta.plusDays(1), listOf(faltante("junio")))

        assertEquals(listOf("ultimo", "primero"), listar().map { it.id })
    }

    private suspend fun listar(): List<OtSinCostoRow> = dao.observeOtsSinCosto(opticaId, desde, hasta).first()

    private suspend fun orden(
        id: String,
        fecha: LocalDate,
        items: List<DispensacionItem>,
        estado: String = "Pendiente",
        ot: String = "",
        monto: Double = 100.0,
    ) {
        dao.insertDispensacion(
            DispensacionOptica(
                id = id,
                ot = ot,
                pacienteId = pacienteId,
                fecha = fecha,
                opticaId = opticaId,
                montoTotal = monto,
                estadoEntrega = estado,
            ),
        )
        items.forEach { db.dispensacionItemDao().insertItem(it) }
    }

    private fun faltante(dispensacionId: String, sufijo: String = "f") = item(dispensacionId, sufijo, "faltante", 0.0)

    private fun desconocido(dispensacionId: String) = item(dispensacionId, "u", "importado", 40.0)

    private fun completo(dispensacionId: String, sufijo: String = "m") = item(dispensacionId, sufijo, "manual", 60.0)

    private fun item(dispensacionId: String, sufijo: String, origen: String, costo: Double) = DispensacionItem(
        id = "$dispensacionId-$sufijo",
        dispensacionId = dispensacionId,
        opticaId = opticaId,
        costoRealOd = costo,
        costoOrigen = origen,
    )
}
