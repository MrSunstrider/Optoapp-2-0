package com.example.optoapp.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DispensacionFinancieraRepositoryTest {

    private lateinit var optoRepository: OptoRepository
    private lateinit var repository: DispensacionFinancieraRepository

    private val testDate = LocalDate.of(2026, 7, 4)
    private val testDispensacion = DispensacionOptica(
        id = "disp-1",
        ot = "OT-2026-0001",
        pacienteId = "pac-1",
        fecha = testDate,
        opticaId = "optica-test",
        tipoLente = "Monofocal",
        montoTotal = 150.0,
        estadoEntrega = "Pendiente",
    )

    @Before
    fun setUp() {
        optoRepository = mockk(relaxed = true)
        repository = DispensacionFinancieraRepositoryImpl(optoRepository)
    }

    @Test
    fun `obtenerDispensacion delegates to OptoRepository getDispensacionById`() = runTest {
        coEvery { optoRepository.getDispensacionById("disp-1") } returns Resource.Success(testDispensacion)

        val result = repository.obtenerDispensacion("disp-1")

        coVerify { optoRepository.getDispensacionById("disp-1") }
        assertTrue(result is Resource.Success)
        assertEquals("disp-1", (result as Resource.Success).data!!.id)
    }

    @Test
    fun `obtenerPagos delegates to OptoRepository getPagosByDispensacion`() = runTest {
        val pagos = listOf(
            Pago(id = "p-1", dispensacionId = "disp-1", fecha = testDate, tipo = "Abono", monto = 50.0, opticaId = "optica-test"),
        )
        coEvery { optoRepository.getPagosByDispensacion("disp-1") } returns flowOf(pagos)

        val result = repository.obtenerPagos("disp-1")

        assertEquals(1, result.size)
        assertEquals("p-1", result[0].id)
        coVerify { optoRepository.getPagosByDispensacion("disp-1") }
    }

    @Test
    fun `obtenerContexto builds ContextoFinanciero from dispensacion and paciente`() = runTest {
        coEvery { optoRepository.getDispensacionById("disp-1") } returns Resource.Success(testDispensacion)
        coEvery { optoRepository.getPacienteByIdScoped("pac-1", any()) } returns Resource.Success(
            Paciente(
                id = "pac-1",
                nombreCompleto = "Juan Perez",
                edad = 30,
                telefono = "555",
                fechaCreacion = testDate,
                opticaId = "optica-test",
            ),
        )

        val result = repository.obtenerContexto("disp-1")

        assertEquals("OT-2026-0001", result.ot)
        assertEquals("Juan Perez", result.pacienteNombre)
        assertEquals(testDate, result.fecha)
        assertTrue(result.descripcion.contains("Monofocal"))
        coVerify { optoRepository.getDispensacionById("disp-1") }
        coVerify { optoRepository.getPacienteByIdScoped("pac-1", any()) }
    }

    @Test
    fun `obtenerContexto returns fallback when dispensacion not found`() = runTest {
        coEvery { optoRepository.getDispensacionById("bad-id") } returns Resource.Error("Not found")

        val result = repository.obtenerContexto("bad-id")

        assertEquals("", result.ot)
        assertEquals("", result.pacienteNombre)
        assertEquals("", result.descripcion)
    }

    @Test
    fun `actualizarMontoTotal loads dispensacion updates montoTotal and persists`() = runTest {
        coEvery { optoRepository.getDispensacionById("disp-1") } returns Resource.Success(testDispensacion)
        coEvery { optoRepository.updateDispensacion(any()) } returns Unit

        repository.actualizarMontoTotal("disp-1", 200.0, "optica-test")

        coVerify { optoRepository.getDispensacionById("disp-1") }
        coVerify { optoRepository.updateDispensacion(match { it.montoTotal == 200.0 && it.id == "disp-1" }) }
    }

    @Test
    fun `actualizarMontoTotal skips persist when dispensacion not found`() = runTest {
        coEvery { optoRepository.getDispensacionById("bad-id") } returns Resource.Error("Not found")

        repository.actualizarMontoTotal("bad-id", 200.0, "optica-test")

        coVerify(exactly = 0) { optoRepository.updateDispensacion(any()) }
    }

    @Test
    fun `actualizarMontoPagado loads dispensacion updates montoPagado and persists`() = runTest {
        coEvery { optoRepository.getDispensacionById("disp-1") } returns Resource.Success(testDispensacion)
        coEvery { optoRepository.updateDispensacion(any()) } returns Unit

        repository.actualizarMontoPagado("disp-1", 80.0, "optica-test")

        coVerify { optoRepository.getDispensacionById("disp-1") }
        coVerify { optoRepository.updateDispensacion(match { it.montoPagado == 80.0 && it.id == "disp-1" }) }
    }

    @Test
    fun `actualizarEstado updates estado and fechaEntrega`() = runTest {
        val fechaEntrega = LocalDate.of(2026, 7, 15)
        coEvery { optoRepository.getDispensacionById("disp-1") } returns Resource.Success(testDispensacion)

        repository.actualizarEstado("disp-1", "Entregado", fechaEntrega, "optica-test")

        coVerify {
            optoRepository.updateDispensacion(
                match {
                    it.estadoEntrega == "Entregado" && it.fechaEntrega == fechaEntrega
                },
            )
        }
    }

    @Test
    fun `actualizarEstado sets fechaEntrega null when Pendiente`() = runTest {
        coEvery { optoRepository.getDispensacionById("disp-1") } returns Resource.Success(
            testDispensacion.copy(estadoEntrega = "Entregado", fechaEntrega = LocalDate.of(2026, 7, 10)),
        )

        repository.actualizarEstado("disp-1", "Pendiente", null, "optica-test")

        coVerify {
            optoRepository.updateDispensacion(
                match {
                    it.estadoEntrega == "Pendiente" && it.fechaEntrega == null
                },
            )
        }
    }

    @Test
    fun `agregarPago delegates to insertPago`() = runTest {
        val pago = Pago(
            id = "pago-1",
            dispensacionId = "disp-1",
            fecha = testDate,
            tipo = "Abono",
            monto = 50.0,
            metodoPago = "Efectivo",
            opticaId = "optica-test",
        )

        repository.agregarPago(pago)

        coVerify { optoRepository.insertPago(pago) }
    }

    @Test
    fun `editarPago delegates to updatePago`() = runTest {
        val pago = Pago(
            id = "pago-1",
            dispensacionId = "disp-1",
            fecha = testDate,
            tipo = "Abono",
            monto = 75.0,
            metodoPago = "Tarjeta",
            opticaId = "optica-test",
        )

        repository.editarPago(pago)

        coVerify { optoRepository.updatePago(pago) }
    }

    @Test
    fun `eliminarPago delegates to deletePagoRegistrandoAnulacionEnCaja`() = runTest {
        val pago = Pago(
            id = "pago-1",
            dispensacionId = "disp-1",
            fecha = testDate,
            tipo = "Abono",
            monto = 50.0,
            opticaId = "optica-test",
        )

        repository.eliminarPago(pago, "optica-test")

        coVerify { optoRepository.deletePagoRegistrandoAnulacionEnCaja(pago, "optica-test") }
    }
}
