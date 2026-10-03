package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SyncFinanzasMergeTest {

    private val repository = mockk<OptoRepository>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val handler = DispensacionMergeHandler(repository, syncStateTracker)
    private val merged = slot<DispensacionOptica>()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }
        coEvery { repository.updateDispensacion(capture(merged)) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun disp(
        id: String,
        reclamoOrigenId: String? = null,
        motivoAnulacion: String? = null,
        fechaAnulacion: LocalDate? = null,
        estadoEntrega: String = "",
    ) = DispensacionOptica(
        id = id,
        ot = "2026-0042",
        pacienteId = "p1",
        fecha = LocalDate.of(2026, 9, 1),
        opticaId = "o1",
        estadoEntrega = estadoEntrega,
        reclamoOrigenId = reclamoOrigenId,
        motivoAnulacion = motivoAnulacion,
        fechaAnulacion = fechaAnulacion,
    )

    @Test
    fun `merge keeps canonical claim linkage and cancellation metadata`() = runTest {
        handler.mergeLocalDispensacionConflict(
            opticaId = "o1",
            canonical = disp("c", "orig-c", "Motivo canónico", LocalDate.of(2026, 9, 10)),
            duplicate = disp("d", "orig-d", "Motivo duplicado", LocalDate.of(2026, 9, 20)),
        )

        assertEquals("orig-c", merged.captured.reclamoOrigenId)
        assertEquals("Motivo canónico", merged.captured.motivoAnulacion)
        assertEquals(LocalDate.of(2026, 9, 10), merged.captured.fechaAnulacion)
    }

    @Test
    fun `merge fills missing canonical linkage and metadata from duplicate`() = runTest {
        handler.mergeLocalDispensacionConflict(
            opticaId = "o1",
            canonical = disp("c"),
            duplicate = disp("d", "orig-d", "Lente rayado", LocalDate.of(2026, 9, 20)),
        )

        assertEquals("orig-d", merged.captured.reclamoOrigenId)
        assertEquals("Lente rayado", merged.captured.motivoAnulacion)
        assertEquals(LocalDate.of(2026, 9, 20), merged.captured.fechaAnulacion)
    }

    @Test
    fun `merge leaves metadata null when neither side has it`() = runTest {
        handler.mergeLocalDispensacionConflict(opticaId = "o1", canonical = disp("c"), duplicate = disp("d"))

        assertEquals("c", merged.captured.id)
        assertNull(merged.captured.reclamoOrigenId)
        assertNull(merged.captured.motivoAnulacion)
        assertNull(merged.captured.fechaAnulacion)
    }

    @Test
    fun `keepLocalTerminal only protects a local terminal estado from a remote non terminal one`() {
        assertTrue(keepLocalTerminal("Anulado", "Pendiente"))
        assertTrue(keepLocalTerminal("Reclamada", "Entregado"))
        assertTrue(keepLocalTerminal(" Anulado ", ""))
        assertFalse(keepLocalTerminal("Anulado", "Reclamada"))
        assertFalse(keepLocalTerminal("Anulado", "Anulado"))
        assertFalse(keepLocalTerminal("Pendiente", "Anulado"))
        assertFalse(keepLocalTerminal("Entregado", "Pendiente"))
        assertFalse(keepLocalTerminal(null, "Pendiente"))
        assertFalse(keepLocalTerminal("", "Pendiente"))
    }

    @Test
    fun `merge prefers terminal duplicate estado with its cancellation metadata`() = runTest {
        handler.mergeLocalDispensacionConflict(
            opticaId = "o1",
            canonical = disp("c", estadoEntrega = OrderStatusPolicy.PENDIENTE),
            duplicate = disp(
                "d",
                motivoAnulacion = "Cliente desistió",
                fechaAnulacion = LocalDate.of(2026, 9, 30),
                estadoEntrega = OrderStatusPolicy.ANULADO,
            ),
        )

        assertEquals(OrderStatusPolicy.ANULADO, merged.captured.estadoEntrega)
        assertEquals("Cliente desistió", merged.captured.motivoAnulacion)
        assertEquals(LocalDate.of(2026, 9, 30), merged.captured.fechaAnulacion)
    }

    @Test
    fun `merge keeps terminal canonical over non terminal duplicate`() = runTest {
        handler.mergeLocalDispensacionConflict(
            opticaId = "o1",
            canonical = disp(
                "c",
                motivoAnulacion = "Lente rayado",
                fechaAnulacion = LocalDate.of(2026, 9, 25),
                estadoEntrega = OrderStatusPolicy.RECLAMADA,
            ),
            duplicate = disp("d", estadoEntrega = OrderStatusPolicy.ENTREGADO),
        )

        assertEquals(OrderStatusPolicy.RECLAMADA, merged.captured.estadoEntrega)
        assertEquals("Lente rayado", merged.captured.motivoAnulacion)
    }

    @Test
    fun `merge keeps canonical estado and metadata when both sides are terminal`() = runTest {
        handler.mergeLocalDispensacionConflict(
            opticaId = "o1",
            canonical = disp(
                "c",
                motivoAnulacion = "Lente rayado",
                fechaAnulacion = LocalDate.of(2026, 9, 25),
                estadoEntrega = OrderStatusPolicy.RECLAMADA,
            ),
            duplicate = disp(
                "d",
                motivoAnulacion = "Error de registro",
                fechaAnulacion = LocalDate.of(2026, 9, 28),
                estadoEntrega = OrderStatusPolicy.ANULADO,
            ),
        )

        assertEquals(OrderStatusPolicy.RECLAMADA, merged.captured.estadoEntrega)
        assertEquals("Lente rayado", merged.captured.motivoAnulacion)
        assertEquals(LocalDate.of(2026, 9, 25), merged.captured.fechaAnulacion)
    }

    @Test
    fun `merge of two non terminal sides keeps canonical estado`() = runTest {
        handler.mergeLocalDispensacionConflict(
            opticaId = "o1",
            canonical = disp("c", estadoEntrega = OrderStatusPolicy.ENTREGADO),
            duplicate = disp("d", estadoEntrega = OrderStatusPolicy.PENDIENTE),
        )

        assertEquals(OrderStatusPolicy.ENTREGADO, merged.captured.estadoEntrega)
    }
}
