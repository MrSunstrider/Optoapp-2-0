package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PagoEffectTest {
    @Test
    fun matrix_and_trim_and_fixture() {
        assertEquals(100.0, PagoEffect.signedAmount("Abono", 100.0), 0.001)
        assertEquals(50.0, PagoEffect.signedAmount("Pago completo", 50.0), 0.001)
        assertEquals(-40.0, PagoEffect.signedAmount("Reembolso", 40.0), 0.001)
        assertEquals(-60.0, PagoEffect.signedAmount("Reverso", 60.0), 0.001)
        assertEquals(0.0, PagoEffect.signedAmount("Anulación", 100.0), 0.001)
        assertEquals(0.0, PagoEffect.signedAmount("Efectivo", 25.0), 0.001)
        assertEquals(80.0, PagoEffect.signedAmount("  Abono  ", 80.0), 0.001)
        val net = listOf(
            "Abono" to 100.0, "Reverso" to 100.0, "Reembolso" to 25.0, "Anulación" to 50.0,
        ).sumOf { PagoEffect.signedAmount(it.first, it.second) }
        assertEquals(-25.0, net, 0.001)
    }
}
