package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EliminarDispensacionUseCaseTest {
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val pagoDao = mockk<PagoDao>()
    private val coordinator = mockk<MonturaInventoryCoordinator>()
    private val disp = DispensacionOptica(
        id = "d1", pacienteId = "pac", fecha = LocalDate.of(2026, 9, 1), opticaId = "o1", estadoEntrega = "Pendiente",
    )

    init {
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }
        coEvery { repository.getDispensacionById("d1", "o1") } returns Resource.Success(disp)
        coEvery { repository.getRegalosByDispensacionId("d1", "o1") } returns listOf(
            RegaloDispensacionEntity(
                id = "r1", dispensacionId = "d1", productoId = "P1", cantidad = 1,
                costoUnitario = 5.0, descripcion = "Estuche", motivo = "", opticaId = "o1",
            ),
        )
    }

    private fun stubCounts(pagos: Int, movimientos: Int) {
        coEvery { pagoDao.countByDispensacion("d1", "o1") } returns pagos
        coEvery { coordinator.countMovimientosForDispensacion("d1", listOf("r1"), "o1") } returns movimientos
    }

    private fun useCase() = EliminarDispensacionUseCase(repository, pagoDao, coordinator)

    @Test
    fun noPagosAndNoMovimientos_deletesWithTombstone() = runTest {
        stubCounts(pagos = 0, movimientos = 0)

        useCase()("d1", "o1")

        coVerify(exactly = 1) { repository.deleteDispensacion(disp) }
        coVerify(exactly = 0) { repository.updateDispensacion(any()) }
        coVerify(exactly = 0) { repository.insertPago(any()) }
    }

    @Test
    fun orderWithAPago_isRejectedWithoutChanges() = runTest {
        stubCounts(pagos = 1, movimientos = 0)

        val error = runCatching { useCase()("d1", "o1") }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("La orden tiene pagos o movimientos de stock. Usa Anular.", error?.message)
        coVerify(exactly = 0) { repository.deleteDispensacion(any()) }
    }

    @Test
    fun orderWithStockMovimientos_isRejectedWithoutChanges() = runTest {
        stubCounts(pagos = 0, movimientos = 2)

        val error = runCatching { useCase()("d1", "o1") }.exceptionOrNull()

        assertEquals("La orden tiene pagos o movimientos de stock. Usa Anular.", error?.message)
        coVerify(exactly = 0) { repository.deleteDispensacion(any()) }
    }

    @Test
    fun missingOrder_isRejected() = runTest {
        coEvery { repository.getDispensacionById("d1", "o1") } returns Resource.Error("not found")
        stubCounts(pagos = 0, movimientos = 0)

        val error = runCatching { useCase()("d1", "o1") }.exceptionOrNull()

        assertEquals("Dispensación no encontrada.", error?.message)
        coVerify(exactly = 0) { repository.deleteDispensacion(any()) }
    }
}
