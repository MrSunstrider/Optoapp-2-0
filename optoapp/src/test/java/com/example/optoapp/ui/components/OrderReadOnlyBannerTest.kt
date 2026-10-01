package com.example.optoapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class OrderReadOnlyBannerTest {

    @Test
    fun `title names the terminal estado`() {
        assertEquals("Orden anulada", readOnlyBannerTitle("Anulado"))
        assertEquals("Orden reclamada", readOnlyBannerTitle(" Reclamada "))
        assertEquals("Servicio anulado", readOnlyBannerTitle("Anulado", servicio = true))
    }

    @Test
    fun `detail shows motivo and cancellation date`() {
        assertEquals(
            "Motivo: Lente rayado · 14 de agosto de 2026",
            readOnlyBannerDetail("Lente rayado", LocalDate.of(2026, 8, 14)),
        )
    }

    @Test
    fun `legacy order without metadata shows a placeholder motivo and no date`() {
        assertEquals("Sin motivo registrado", readOnlyBannerDetail(null, null))
        assertEquals("Sin motivo registrado · 02 de octubre de 2026", readOnlyBannerDetail("  ", LocalDate.of(2026, 10, 2)))
    }
}
