package com.example.optoapp.ui.components.cierre_caja

import com.example.optoapp.data.Pago
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Tests the label logic for [TransactionItem].
 * The [transactionLabel] function is a pure projection of [Pago] fields into a display string.
 */
class TransactionItemTest {

    private val today = LocalDate.of(2026, 7, 3)
    private val opticaId = "optica-test"

    @Test
    fun dispensacionIdSet_labelIsDispensacion() {
        val pago = Pago(
            id = "p1",
            dispensacionId = "d1",
            servicioExtraId = null,
            fecha = today,
            tipo = "Efectivo",
            monto = 100.0,
            opticaId = opticaId,
        )
        assertEquals("Dispensación", transactionLabel(pago))
    }

    @Test
    fun servicioExtraIdSet_labelIsServicioExtra() {
        val pago = Pago(
            id = "p2",
            dispensacionId = null,
            servicioExtraId = "s1",
            fecha = today,
            tipo = "Efectivo",
            monto = 50.0,
            opticaId = opticaId,
        )
        assertEquals("Servicio Extra", transactionLabel(pago))
    }

    @Test
    fun bothIdsNull_labelIsPago() {
        val pago = Pago(
            id = "p3",
            dispensacionId = null,
            servicioExtraId = null,
            fecha = today,
            tipo = "Efectivo",
            monto = 75.0,
            opticaId = opticaId,
        )
        assertEquals("Pago", transactionLabel(pago))
    }

    @Test
    fun bothIdsNonNull_dispensacionWins() {
        val pago = Pago(
            id = "p4",
            dispensacionId = "d2",
            servicioExtraId = "s2",
            fecha = today,
            tipo = "Tarjeta",
            monto = 200.0,
            opticaId = opticaId,
        )
        // when checks dispensacionId first
        assertEquals("Dispensación", transactionLabel(pago))
    }

    @Test
    fun servicioExtraIdOverridesOrphan() {
        val pago = Pago(
            id = "p5",
            dispensacionId = null,
            servicioExtraId = "s3",
            fecha = today,
            tipo = "Transferencia",
            monto = 30.0,
            opticaId = opticaId,
        )
        assertEquals("Servicio Extra", transactionLabel(pago))
    }

    @Test
    fun displayAmount_reembolsoIsNegative() {
        val pago = Pago(
            id = "p6",
            fecha = today,
            tipo = "Reembolso",
            monto = 40.0,
            opticaId = opticaId,
        )
        assertEquals(-40.0, transactionDisplayAmount(pago), 0.001)
        assertEquals("s/. -40.00", formatTransactionAmount(transactionDisplayAmount(pago)))
    }

    @Test
    fun displayAmount_anulacionIsZero() {
        val pago = Pago(
            id = "p7",
            fecha = today,
            tipo = "Anulación",
            monto = 100.0,
            opticaId = opticaId,
        )
        assertEquals(0.0, transactionDisplayAmount(pago), 0.001)
    }
}
