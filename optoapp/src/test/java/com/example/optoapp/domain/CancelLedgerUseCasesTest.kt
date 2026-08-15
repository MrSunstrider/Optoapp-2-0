package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.sync.PostSaveSyncScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CancelLedgerUseCasesTest {
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val pagoDao = mockk<PagoDao>(relaxed = true)
    private val scheduler = mockk<PostSaveSyncScheduler>(relaxed = true)
    private val date = LocalDate.of(2026, 8, 14)

    @Test
    fun cancelServicio_insertsLinkedReverso() = runBlocking {
        val credit = Pago(
            id = "p1", servicioExtraId = "s1", fecha = date,
            tipo = "Abono", monto = 80.0, metodoPago = "Efectivo", opticaId = "o1",
        )
        coEvery { repository.getServicioById("s1") } returns Resource.Success(
            ServicioExtra(id = "s1", descripcion = "x", montoTotal = 100.0, estado = "Pendiente", fecha = date),
        )
        coEvery { pagoDao.getCreditPagosByParent("s1") } returns listOf(credit)
        coEvery { pagoDao.getReversoByOriginalId("p1") } returns null
        val slot = slot<Pago>()
        coEvery { repository.insertPago(capture(slot)) } returns Unit

        CancelServicioExtraUseCase(repository, pagoDao, scheduler)("s1", "o1")

        assertEquals("Reverso", slot.captured.tipo)
        assertEquals("p1", slot.captured.reversaPagoId)
        assertEquals(80.0, slot.captured.monto, 0.001)
        coVerify { repository.updateServicio(match { it.estado == "Anulado" }) }
        coVerify(exactly = 1) { scheduler.scheduleFinanzasSync("o1") }
    }

    @Test
    fun cancelServicio_idempotentWhenAlreadyAnulado() = runBlocking {
        coEvery { repository.getServicioById("s1") } returns Resource.Success(
            ServicioExtra(id = "s1", descripcion = "x", montoTotal = 1.0, estado = "Anulado", fecha = date),
        )
        CancelServicioExtraUseCase(repository, pagoDao, scheduler)("s1", "o1")
        coVerify(exactly = 0) { repository.insertPago(any()) }
    }

    @Test
    fun cancelDispensacion_insertsLinkedReverso() = runBlocking {
        val credit = Pago(
            id = "p1", dispensacionId = "d1", fecha = date,
            tipo = "Pago completo", monto = 150.0, metodoPago = "Efectivo", opticaId = "o1",
        )
        coEvery { repository.getDispensacionById("d1") } returns Resource.Success(
            DispensacionOptica(
                id = "d1", pacienteId = "pac", fecha = date, opticaId = "o1",
                estadoEntrega = "Pendiente", metodoPago = "Efectivo",
            ),
        )
        coEvery { pagoDao.getCreditPagosByParent("d1") } returns listOf(credit)
        coEvery { pagoDao.getReversoByOriginalId("p1") } returns null
        val slot = slot<Pago>()
        coEvery { repository.insertPago(capture(slot)) } returns Unit

        CancelDispensacionUseCase(repository, pagoDao, scheduler)("d1", "o1")

        assertEquals("Reverso", slot.captured.tipo)
        assertEquals("p1", slot.captured.reversaPagoId)
        coVerify { repository.updateDispensacion(match { it.estadoEntrega == "Anulado" }) }
    }

    @Test
    fun reclaim_positiveReembolsoWithoutReversaLink() = runBlocking {
        coEvery { repository.getDispensacionById("d1") } returns Resource.Success(
            DispensacionOptica(
                id = "d1", pacienteId = "pac", fecha = date, opticaId = "o1",
                estadoEntrega = "Pendiente", metodoPago = "Efectivo", ot = "OT-1",
            ),
        )
        val slot = slot<Pago>()
        coEvery { repository.insertPago(capture(slot)) } returns Unit

        ReclaimDispensacionUseCase(repository, scheduler)("d1", "o1", 50.0, "Efectivo", "OT-1")

        assertEquals("Reembolso", slot.captured.tipo)
        assertEquals(50.0, slot.captured.monto, 0.001)
        assertNull(slot.captured.reversaPagoId)
        coVerify { repository.updateDispensacion(match { it.estadoEntrega == "Reclamada" }) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun reclaim_rejectsNegativeMonto() = runBlocking {
        ReclaimDispensacionUseCase(repository, scheduler)("d1", "o1", -1.0, "Efectivo", "OT-1")
    }
}
