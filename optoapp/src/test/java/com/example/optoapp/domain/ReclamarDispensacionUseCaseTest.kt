package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReclamarDispensacionUseCaseTest {
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val pagoDao = mockk<PagoDao>(relaxed = true)
    private val stockHelper = mockk<DispensacionStockHelper>(relaxed = true)
    private val scheduler = mockk<PostSaveSyncScheduler>(relaxed = true)

    init {
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }
    }

    private fun stubOriginal(estado: String) {
        coEvery { repository.getDispensacionById("d1", "o1") } returns Resource.Success(
            DispensacionOptica(id = "d1", ot = "2026-0042", pacienteId = "pac", fecha = LocalDate.of(2026, 9, 1), estadoEntrega = estado),
        )
    }

    private suspend fun claim(motivo: String = "Lente rayado", total: Double = 200.0) =
        ReclamarDispensacionUseCase(repository, pagoDao, stockHelper, scheduler, CalcularMontoPagadoUseCase(pagoDao))(
            "d1", "o1", motivo, total, "Efectivo",
        )

    private fun assertNoWrites() {
        coVerify(exactly = 0) { repository.insertDispensacion(any()) }
        coVerify(exactly = 0) { repository.updateDispensacion(any()) }
        coVerify(exactly = 0) { repository.insertDispensacionItem(any()) }
        coVerify(exactly = 0) { repository.insertPago(any()) }
        coVerify(exactly = 0) { stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
    }

    @Test
    fun invalidMotivo_isRejectedBeforeAnyWrite() = runTest {
        stubOriginal("Entregado")

        val errors = listOf("", "   ", "x".repeat(501)).map { runCatching { claim(motivo = it) }.exceptionOrNull() }

        assertTrue(errors.all { it is IllegalArgumentException })
        assertNoWrites()
    }

    @Test
    fun negativeOrNonFiniteTotal_isRejectedBeforeAnyWrite() = runTest {
        stubOriginal("Entregado")

        val errors = listOf(-0.01, Double.NaN, Double.POSITIVE_INFINITY).map { runCatching { claim(total = it) }.exceptionOrNull() }

        assertTrue(errors.all { it is IllegalArgumentException })
        assertNoWrites()
    }

    @Test
    fun pendienteOrAnulado_throwsWithNoWrites() = runTest {
        listOf("Pendiente", "Anulado").forEach { estado ->
            stubOriginal(estado)

            val error = runCatching { claim() }.exceptionOrNull()

            assertTrue("estado $estado", error is IllegalStateException)
        }
        assertNoWrites()
    }

    @Test
    fun reclamada_returnsAlreadyTerminalWithNoReplacementPagosOrMovimientos() = runTest {
        stubOriginal(" Reclamada ")

        assertEquals(ReclamoOutcome.AlreadyTerminal("Reclamada"), claim())
        assertNoWrites()
    }
}
