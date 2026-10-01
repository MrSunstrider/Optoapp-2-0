package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OrderStatusPolicyTest {

    @Test
    fun `constants match persisted estado values`() {
        assertEquals("Pendiente", OrderStatusPolicy.PENDIENTE)
        assertEquals("Entregado", OrderStatusPolicy.ENTREGADO)
        assertEquals("Anulado", OrderStatusPolicy.ANULADO)
        assertEquals("Reclamada", OrderStatusPolicy.RECLAMADA)
    }

    @Test
    fun `Anulado and Reclamada are terminal`() {
        assertTrue(OrderStatusPolicy.isTerminal("Anulado"))
        assertTrue(OrderStatusPolicy.isTerminal("Reclamada"))
    }

    @Test
    fun `terminal check ignores surrounding whitespace`() {
        assertTrue(OrderStatusPolicy.isTerminal("  Anulado "))
        assertTrue(OrderStatusPolicy.isTerminal("\tReclamada\n"))
    }

    @Test
    fun `active and blank estados are not terminal`() {
        assertFalse(OrderStatusPolicy.isTerminal("Pendiente"))
        assertFalse(OrderStatusPolicy.isTerminal("Entregado"))
        assertFalse(OrderStatusPolicy.isTerminal(""))
        assertFalse(OrderStatusPolicy.isTerminal("   "))
    }

    @Test
    fun `only Pendiente and Entregado can be cancelled`() {
        assertTrue(OrderStatusPolicy.canCancel("Pendiente"))
        assertTrue(OrderStatusPolicy.canCancel(" Entregado "))
        assertFalse(OrderStatusPolicy.canCancel("Anulado"))
        assertFalse(OrderStatusPolicy.canCancel("Reclamada"))
        assertFalse(OrderStatusPolicy.canCancel(""))
    }

    @Test
    fun `delivery date change keeps a whitespace-padded terminal estado`() {
        assertEquals(" Reclamada ", estadoAfterFechaEntrega(" Reclamada ", LocalDate.of(2026, 9, 30)))
    }
}
