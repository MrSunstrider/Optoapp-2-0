package com.example.optoapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderEstadoChipTest {

    @Test
    fun `Entregado is positive regardless of saldo`() {
        assertEquals(OrderEstadoChipStyle("Entregado", OrderEstadoTone.POSITIVE), orderEstadoChipStyle("Entregado", saldo = 40.0))
    }

    @Test
    fun `Pendiente with saldo is an alert and without saldo a warning`() {
        assertEquals(OrderEstadoChipStyle("Pendiente", OrderEstadoTone.ALERT), orderEstadoChipStyle("Pendiente", saldo = 0.01))
        assertEquals(OrderEstadoChipStyle("Pendiente", OrderEstadoTone.WARNING), orderEstadoChipStyle("Pendiente", saldo = 0.0))
        assertEquals(OrderEstadoChipStyle("Pendiente", OrderEstadoTone.WARNING), orderEstadoChipStyle("Pendiente"))
    }

    @Test
    fun `Anulado reads Anulada for a dispensacion and Anulado for a servicio`() {
        assertEquals(OrderEstadoChipStyle("Anulada", OrderEstadoTone.NEUTRAL), orderEstadoChipStyle("Anulado", saldo = 100.0))
        assertEquals(
            OrderEstadoChipStyle("Anulado", OrderEstadoTone.NEUTRAL),
            orderEstadoChipStyle("Anulado", saldo = 100.0, servicio = true),
        )
    }

    @Test
    fun `Reclamada has its own claimed tone`() {
        assertEquals(OrderEstadoChipStyle("Reclamada", OrderEstadoTone.CLAIMED), orderEstadoChipStyle(" Reclamada "))
    }

    @Test
    fun `unknown estado keeps its text with a neutral tone`() {
        assertEquals(OrderEstadoChipStyle("En taller", OrderEstadoTone.NEUTRAL), orderEstadoChipStyle("En taller", saldo = 10.0))
    }
}
