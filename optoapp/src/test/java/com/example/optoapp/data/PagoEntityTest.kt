package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PagoEntityTest {

    @Test
    fun ventaId_explicitValue_isPreserved() {
        val pago = Pago(
            id = "p1",
            fecha = LocalDate.of(2026, 7, 1),
            tipo = "efectivo",
            monto = 100.0,
            ventaId = "v123",
        )
        assertEquals("v123", pago.ventaId)
    }

    @Test
    fun ventaId_defaultValue_isNull() {
        val pago = Pago(
            id = "p2",
            fecha = LocalDate.of(2026, 7, 2),
            tipo = "tarjeta",
            monto = 200.0,
        )
        assertNull(pago.ventaId)
    }
}
