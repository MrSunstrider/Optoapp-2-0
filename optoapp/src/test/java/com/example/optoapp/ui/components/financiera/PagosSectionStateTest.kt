package com.example.optoapp.ui.components.financiera

import com.example.optoapp.data.Pago
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PagosSectionStateTest {

    private val fecha: LocalDate = LocalDate.of(2026, 8, 14)

    private fun pago(id: String, tipo: String, monto: Double) = Pago(
        id = id,
        dispensacionId = "disp-1",
        fecha = fecha,
        tipo = tipo,
        monto = monto,
        metodoPago = "Efectivo",
        opticaId = "optica-test",
    )

    @Test
    fun `pagado sums abonos`() {
        val state = PagosSectionState(
            montoTotal = 300.0,
            pagos = listOf(pago("p1", "Abono", 100.0), pago("p2", "Abono", 50.0)),
        )

        assertEquals(150.0, state.pagado, 0.001)
        assertEquals(150.0, state.saldo, 0.001)
    }

    @Test
    fun `pagado applies PagoEffect for reembolso and reverso`() {
        val state = PagosSectionState(
            montoTotal = 300.0,
            pagos = listOf(
                pago("p1", "Abono", 200.0),
                pago("p2", "Reembolso", 50.0),
                pago("p3", "Reverso", 40.0),
            ),
        )

        assertEquals(110.0, state.pagado, 0.001)
        assertEquals(190.0, state.saldo, 0.001)
    }

    @Test
    fun `pagado ignores anulacion`() {
        val state = PagosSectionState(
            montoTotal = 300.0,
            pagos = listOf(pago("p1", "Abono", 100.0), pago("p2", "Anulación", 100.0)),
        )

        assertEquals(100.0, state.pagado, 0.001)
    }

    @Test
    fun `montoMaximoParaNuevo equals remaining saldo`() {
        val state = PagosSectionState(
            montoTotal = 300.0,
            pagos = listOf(pago("p1", "Abono", 120.0)),
        )

        assertEquals(180.0, state.montoMaximoParaNuevo(), 0.001)
    }

    @Test
    fun `montoMaximoParaNuevo never negative when overpaid`() {
        val state = PagosSectionState(
            montoTotal = 100.0,
            pagos = listOf(pago("p1", "Abono", 150.0)),
        )

        assertEquals(0.0, state.montoMaximoParaNuevo(), 0.001)
    }

    @Test
    fun `montoMaximoParaEdicion excludes the edited pago`() {
        val state = PagosSectionState(
            montoTotal = 300.0,
            pagos = listOf(pago("p1", "Abono", 100.0), pago("p2", "Abono", 50.0)),
        )

        assertEquals(200.0, state.montoMaximoParaEdicion("p2"), 0.001)
    }

    @Test
    fun `montoMaximoParaEdicion applies PagoEffect to other pagos`() {
        val state = PagosSectionState(
            montoTotal = 300.0,
            pagos = listOf(
                pago("p1", "Abono", 200.0),
                pago("p2", "Reembolso", 50.0),
                pago("p3", "Abono", 30.0),
            ),
        )

        assertEquals(150.0, state.montoMaximoParaEdicion("p3"), 0.001)
    }

    @Test
    fun `saldo can be negative when pagos exceed total`() {
        val state = PagosSectionState(
            montoTotal = 100.0,
            pagos = listOf(pago("p1", "Abono", 130.0)),
        )

        assertEquals(-30.0, state.saldo, 0.001)
    }

    @Test
    fun `empty pagos leaves full saldo`() {
        val state = PagosSectionState(montoTotal = 250.0, pagos = emptyList())

        assertEquals(0.0, state.pagado, 0.001)
        assertEquals(250.0, state.saldo, 0.001)
        assertEquals(250.0, state.montoMaximoParaNuevo(), 0.001)
    }
}
