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
    fun `only non-terminal estados are editable`() {
        assertTrue(OrderStatusPolicy.isEditable("Pendiente"))
        assertTrue(OrderStatusPolicy.isEditable("Entregado"))
        assertTrue(OrderStatusPolicy.isEditable(""))
        assertFalse(OrderStatusPolicy.isEditable("Anulado"))
        assertFalse(OrderStatusPolicy.isEditable(" Reclamada "))
    }

    @Test
    fun `only Entregado can be claimed`() {
        assertTrue(OrderStatusPolicy.canClaim("Entregado"))
        assertTrue(OrderStatusPolicy.canClaim(" Entregado "))
        assertFalse(OrderStatusPolicy.canClaim("Pendiente"))
        assertFalse(OrderStatusPolicy.canClaim("Anulado"))
        assertFalse(OrderStatusPolicy.canClaim("Reclamada"))
        assertFalse(OrderStatusPolicy.canClaim(""))
    }

    @Test
    fun `manual transitions only toggle Pendiente and Entregado`() {
        assertTrue(OrderStatusPolicy.canTransition("Pendiente", "Entregado"))
        assertTrue(OrderStatusPolicy.canTransition("Entregado", "Pendiente"))
        assertTrue(OrderStatusPolicy.canTransition("", "Entregado"))
        assertTrue(OrderStatusPolicy.canTransition("Pendiente", "Pendiente"))
        assertFalse(OrderStatusPolicy.canTransition("Pendiente", "Otro"))
    }

    @Test
    fun `terminal estados never transition out`() {
        assertTrue(OrderStatusPolicy.canTransition("Anulado", " Anulado "))
        assertFalse(OrderStatusPolicy.canTransition("Anulado", "Pendiente"))
        assertFalse(OrderStatusPolicy.canTransition("Anulado", "Entregado"))
        assertFalse(OrderStatusPolicy.canTransition(" Reclamada ", "Entregado"))
        assertFalse(OrderStatusPolicy.canTransition("Reclamada", "Anulado"))
        assertFalse(OrderStatusPolicy.canTransition("Anulado", "Reclamada"))
    }

    @Test
    fun `Anulado is reached only through cancel and Reclamada only through claim`() {
        assertTrue(OrderStatusPolicy.canTransition("Pendiente", "Anulado"))
        assertTrue(OrderStatusPolicy.canTransition("Entregado", "Anulado"))
        assertTrue(OrderStatusPolicy.canTransition("Entregado", "Reclamada"))
        assertFalse(OrderStatusPolicy.canTransition("Pendiente", "Reclamada"))
        assertFalse(OrderStatusPolicy.canTransition("", "Anulado"))
    }

    @Test
    fun `selectable estados never offer terminal targets`() {
        assertEquals(listOf("Pendiente", "Entregado"), OrderStatusPolicy.selectableEstados("Pendiente"))
        assertEquals(listOf("Pendiente", "Entregado"), OrderStatusPolicy.selectableEstados("Entregado"))
        assertEquals(listOf("Pendiente", "Entregado"), OrderStatusPolicy.selectableEstados(""))
    }

    @Test
    fun `selectable estados of a terminal order is only the current estado`() {
        assertEquals(listOf("Anulado"), OrderStatusPolicy.selectableEstados(" Anulado "))
        assertEquals(listOf("Reclamada"), OrderStatusPolicy.selectableEstados("Reclamada"))
    }

    @Test
    fun `requireEditable rejects terminal estados with the operation in the message`() {
        val error = runCatching { OrderStatusPolicy.requireEditable(" Anulado ", "modificar pagos") }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("La orden está anulado y no se puede modificar pagos.", error?.message)
    }

    @Test
    fun `requireEditable accepts non-terminal estados`() {
        assertTrue(runCatching { OrderStatusPolicy.requireEditable("Entregado", "modificar pagos") }.isSuccess)
        assertTrue(runCatching { OrderStatusPolicy.requireEditable("", "modificar pagos") }.isSuccess)
    }

    @Test
    fun `delivery date change keeps a whitespace-padded terminal estado`() {
        assertEquals(" Reclamada ", estadoAfterFechaEntrega(" Reclamada ", LocalDate.of(2026, 9, 30)))
    }
}
