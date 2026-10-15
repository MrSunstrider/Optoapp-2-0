package com.example.optoapp.domain

import com.example.optoapp.data.SyncStateTracker
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ResumenRecalcMarkerTest {

    private val hoy = LocalDate.of(2026, 7, 20)
    private val pasado = LocalDate.of(2026, 7, 10)

    @Test
    fun `new past record marks its date`() {
        val fechas = ResumenRecalcMarker.fechasAfectadas(anterior = null, nuevo = CostoFechado(pasado, 40.0), hoy = hoy)

        assertEquals(listOf(pasado), fechas)
    }

    @Test
    fun `cost change on a past record marks its date`() {
        val fechas = ResumenRecalcMarker.fechasAfectadas(CostoFechado(pasado, 40.0), CostoFechado(pasado, 55.0), hoy)

        assertEquals(listOf(pasado), fechas)
    }

    @Test
    fun `moved record marks both the old and the new date`() {
        val nueva = LocalDate.of(2026, 7, 12)

        val fechas = ResumenRecalcMarker.fechasAfectadas(CostoFechado(pasado, 40.0), CostoFechado(nueva, 40.0), hoy)

        assertEquals(listOf(pasado, nueva), fechas)
    }

    @Test
    fun `today is never marked because the daily recalc covers it`() {
        val fechas = ResumenRecalcMarker.fechasAfectadas(CostoFechado(pasado, 40.0), CostoFechado(hoy, 40.0), hoy)

        assertEquals(listOf(pasado), fechas)
        assertTrue(ResumenRecalcMarker.fechasAfectadas(null, CostoFechado(hoy, 10.0), hoy).isEmpty())
    }

    @Test
    fun `unchanged cost and date marks nothing`() {
        val fechas = ResumenRecalcMarker.fechasAfectadas(CostoFechado(pasado, 40.0), CostoFechado(pasado, 40.0), hoy)

        assertTrue(fechas.isEmpty())
    }

    @Test
    fun `registrar marks each affected date with the parent cause`() = runTest {
        val tracker = mockk<SyncStateTracker>(relaxed = true)
        val marker = ResumenRecalcMarker(tracker)
        val nueva = LocalDate.of(2026, 7, 12)

        marker.registrar("o1", ResumenRecalcMarker.CAUSA_SERVICIO_EXTRA, "s1", CostoFechado(pasado, 10.0), CostoFechado(nueva, 25.0), hoy)

        coVerify(exactly = 1) { tracker.markPendingRecalc("o1", pasado, "servicio_extra", "s1") }
        coVerify(exactly = 1) { tracker.markPendingRecalc("o1", nueva, "servicio_extra", "s1") }
    }

    @Test
    fun `registrar does not touch the tracker when nothing changed`() = runTest {
        val tracker = mockk<SyncStateTracker>(relaxed = true)

        ResumenRecalcMarker(tracker).registrar(
            "o1", ResumenRecalcMarker.CAUSA_DISPENSACION, "d1", CostoFechado(pasado, 10.0), CostoFechado(pasado, 10.0), hoy,
        )

        coVerify(exactly = 0) { tracker.markPendingRecalc(any(), any(), any(), any()) }
    }
}
