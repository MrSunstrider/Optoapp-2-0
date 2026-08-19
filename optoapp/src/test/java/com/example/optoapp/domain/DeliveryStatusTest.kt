package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DeliveryStatusTest {

    private val day = LocalDate.of(2026, 7, 11)

    @Test
    fun `pendiente without fecha is waiting`() {
        assertTrue(isPendingDelivery("Pendiente", null))
    }

    @Test
    fun `pendiente with fecha is not waiting — OT 4676`() {
        assertFalse(isPendingDelivery("Pendiente", day))
    }

    @Test
    fun `entregado is not waiting`() {
        assertFalse(isPendingDelivery("Entregado", day))
        assertFalse(isPendingDelivery("Entregado", null))
    }

    @Test
    fun `assigning fecha marks Entregado`() {
        assertEquals("Entregado", estadoAfterFechaEntrega("Pendiente", day))
        assertEquals("Pendiente", estadoAfterFechaEntrega("Entregado", null))
        assertEquals("Anulado", estadoAfterFechaEntrega("Anulado", day))
    }
}
