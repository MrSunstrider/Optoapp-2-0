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
import org.junit.Assert.assertNull
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
    ) = DispensacionOptica(
        id = id,
        ot = "2026-0042",
        pacienteId = "p1",
        fecha = LocalDate.of(2026, 9, 1),
        opticaId = "o1",
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
}
