package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

/**
 * Cross-tenant isolation tests for PacienteDao.
 * Verifies that data from one optica is not visible to another.
 */
@RunWith(RobolectricTestRunner::class)
class PacienteDaoCrossTenantTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: PacienteDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.pacienteDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun getPacientesByOptica_returnsOnlyPacientesForThatOptica() = runBlocking {
        val p1 = Paciente(
            id = "p1",
            nombreCompleto = "Juan Pérez",
            edad = 30,
            telefono = "111",
            fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "o1",
        )
        val p2 = Paciente(
            id = "p2",
            nombreCompleto = "María García",
            edad = 25,
            telefono = "222",
            fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "o2",
        )
        dao.insertPaciente(p1)
        dao.insertPaciente(p2)

        val o1Pacientes = dao.getPacientesByOptica("o1").first()
        assertEquals(1, o1Pacientes.size)
        assertEquals("p1", o1Pacientes[0].id)

        val o2Pacientes = dao.getPacientesByOptica("o2").first()
        assertEquals(1, o2Pacientes.size)
        assertEquals("p2", o2Pacientes[0].id)
    }

    @Test
    fun getPacientesByOptica_returnsEmpty_forOpticaWithNoPacientes() = runBlocking {
        dao.insertPaciente(
            Paciente(
                id = "p1",
                nombreCompleto = "Test",
                edad = 20,
                telefono = "111",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
            ),
        )

        val result = dao.getPacientesByOptica("o_other").first()
        assertTrue("Expected empty list for unrelated optica", result.isEmpty())
    }

    @Test
    fun countByOptica_countsOnlyScopedPacientes() = runBlocking {
        dao.insertPaciente(
            Paciente(
                id = "p1",
                nombreCompleto = "A",
                edad = 20,
                telefono = "111",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
            ),
        )
        dao.insertPaciente(
            Paciente(
                id = "p2",
                nombreCompleto = "B",
                edad = 30,
                telefono = "222",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
            ),
        )
        dao.insertPaciente(
            Paciente(
                id = "p3",
                nombreCompleto = "C",
                edad = 25,
                telefono = "333",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o2",
            ),
        )

        assertEquals(2, dao.countByOptica("o1").first())
        assertEquals(1, dao.countByOptica("o2").first())
    }

    @Test
    fun searchPacientesForOptica_doesNotLeakCrossTenant() = runBlocking {
        dao.insertPaciente(
            Paciente(
                id = "p1",
                nombreCompleto = "Juan Pérez",
                edad = 30,
                telefono = "111",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
            ),
        )
        dao.insertPaciente(
            Paciente(
                id = "p2",
                nombreCompleto = "Juan López",
                edad = 40,
                telefono = "222",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o2",
            ),
        )

        val result = dao.searchPacientesForOptica("o1", "Juan").first()
        assertEquals(1, result.size)
        assertEquals("p1", result[0].id)
    }

    @Test
    fun searchPacientesForOptica_matchesDispensacionOt() = runBlocking {
        dao.insertPaciente(paciente("p1", "o1", "Ana"))
        dao.insertPaciente(paciente("p2", "o1", "Bruno"))
        dao.insertPaciente(paciente("p3", "o2", "Ana other optica"))
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = "d1",
                ot = "4582",
                pacienteId = "p1",
                fecha = LocalDate.parse("2026-08-18"),
                opticaId = "o1",
            ),
        )
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = "d2",
                ot = "4582",
                pacienteId = "p3",
                fecha = LocalDate.parse("2026-08-18"),
                opticaId = "o2",
            ),
        )

        val result = dao.searchPacientesForOptica("o1", "4582").first()
        assertEquals(listOf("p1"), result.map { it.id })
    }

    @Test
    fun searchPacientesForOptica_matchesServicioOt() = runBlocking {
        dao.insertPaciente(paciente("p1", "o1", "Ana"))
        db.servicioExtraDao().insertServicio(
            ServicioExtra(
                id = "s1",
                ot = "SE-77",
                descripcion = "Brazos",
                montoTotal = 25.0,
                estado = "Pendiente",
                fecha = LocalDate.parse("2026-08-18"),
                pacienteId = "p1",
                opticaId = "o1",
            ),
        )

        val result = dao.searchPacientesForOptica("o1", "SE-77").first()
        assertEquals(listOf("p1"), result.map { it.id })
    }

    @Test
    fun pendingBalance_excludesAnuladoAndIncludesOpenSaldo() = runBlocking {
        dao.insertPaciente(paciente("p-open", "o1", "Open"))
        dao.insertPaciente(paciente("p-void", "o1", "Void"))
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = "d-open",
                pacienteId = "p-open",
                fecha = LocalDate.parse("2026-08-18"),
                opticaId = "o1",
                montoTotal = 170.0,
                montoPagado = 100.0,
                estadoEntrega = "Pendiente",
            ),
        )
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = "d-void",
                pacienteId = "p-void",
                fecha = LocalDate.parse("2026-08-18"),
                opticaId = "o1",
                montoTotal = 170.0,
                montoPagado = 0.0,
                estadoEntrega = "Anulado",
            ),
        )

        val result = dao.getPacientesWithPendingBalanceForOptica("o1").first()
        assertEquals(listOf("p-open"), result.map { it.id })
    }

    @Test
    fun pendingBalance_excludesWhenPagosCoverTotalDespiteZeroCache() = runBlocking {
        dao.insertPaciente(paciente("p-paid", "o1", "Paid"))
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = "d-paid",
                pacienteId = "p-paid",
                fecha = LocalDate.parse("2026-08-18"),
                opticaId = "o1",
                montoTotal = 170.0,
                montoPagado = 0.0,
                estadoEntrega = "Pendiente",
            ),
        )
        db.pagoDao().insertPago(
            Pago(
                id = "pago-full",
                dispensacionId = "d-paid",
                fecha = LocalDate.parse("2026-08-18"),
                tipo = "Abono",
                monto = 170.0,
                metodoPago = "Efectivo",
                opticaId = "o1",
            ),
        )

        val result = dao.getPacientesWithPendingBalanceForOptica("o1").first()
        assertTrue(result.none { it.id == "p-paid" })
    }

    @Test
    fun pendingBalance_usesLedgerWhenCacheIsDoubled() = runBlocking {
        dao.insertPaciente(paciente("p-drift", "o1", "Drift"))
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = "d-drift",
                pacienteId = "p-drift",
                fecha = LocalDate.parse("2026-08-18"),
                opticaId = "o1",
                montoTotal = 170.0,
                montoPagado = 200.0,
                estadoEntrega = "Pendiente",
            ),
        )
        db.pagoDao().insertPago(
            Pago(
                id = "pago-half",
                dispensacionId = "d-drift",
                fecha = LocalDate.parse("2026-08-18"),
                tipo = "Abono",
                monto = 100.0,
                metodoPago = "Efectivo",
                opticaId = "o1",
            ),
        )

        val result = dao.getPacientesWithPendingBalanceForOptica("o1").first()
        assertEquals(listOf("p-drift"), result.map { it.id })
    }

    @Test
    fun pendingDelivery_includesPendienteDispensacion() = runBlocking {
        dao.insertPaciente(paciente("p-wait", "o1", "Wait"))
        dao.insertPaciente(paciente("p-done", "o1", "Done"))
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = "d-wait",
                pacienteId = "p-wait",
                fecha = LocalDate.parse("2026-08-18"),
                opticaId = "o1",
                estadoEntrega = "Pendiente",
            ),
        )
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = "d-done",
                pacienteId = "p-done",
                fecha = LocalDate.parse("2026-08-18"),
                opticaId = "o1",
                estadoEntrega = "Entregado",
            ),
        )

        val result = dao.getPacientesWithPendingDeliveryForOptica("o1").first()
        assertEquals(listOf("p-wait"), result.map { it.id })
    }

    @Test
    fun pendingDelivery_excludesPendienteWhenFechaEntregaIsSet() = runBlocking {
        dao.insertPaciente(paciente("p-dated", "o1", "Dated"))
        db.dispensacionDao().insertDispensacion(
            DispensacionOptica(
                id = "d-4676",
                ot = "4676",
                pacienteId = "p-dated",
                fecha = LocalDate.parse("2026-07-11"),
                opticaId = "o1",
                estadoEntrega = "Pendiente",
                fechaEntrega = LocalDate.parse("2026-07-11"),
            ),
        )

        val result = dao.getPacientesWithPendingDeliveryForOptica("o1").first()
        assertTrue(result.none { it.id == "p-dated" })
    }

    private fun paciente(id: String, opticaId: String, nombre: String) = Paciente(
        id = id,
        nombreCompleto = nombre,
        edad = 30,
        telefono = "111",
        fechaCreacion = LocalDate.parse("2026-01-01"),
        opticaId = opticaId,
    )
}
