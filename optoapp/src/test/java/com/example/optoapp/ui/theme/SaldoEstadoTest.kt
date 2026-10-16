package com.example.optoapp.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class SaldoEstadoTest {

    @Test
    fun `positive balance is pending`() {
        assertEquals(SaldoEstado.PENDIENTE, saldoEstado(50.0))
    }

    @Test
    fun `zero and overpaid balances are settled`() {
        assertEquals(SaldoEstado.SALDADO, saldoEstado(0.0))
        assertEquals(SaldoEstado.SALDADO, saldoEstado(-5.0))
    }

    @Test
    fun `balance that displays as S slash 0 point 00 is settled`() {
        assertEquals(SaldoEstado.SALDADO, saldoEstado(0.004))
        assertEquals(SaldoEstado.SALDADO, saldoEstado(100.0 - 99.999999999))
    }

    @Test
    fun `balance that displays as one cent is pending`() {
        assertEquals(SaldoEstado.PENDIENTE, saldoEstado(0.005))
        assertEquals(SaldoEstado.PENDIENTE, saldoEstado(0.01))
    }
}
