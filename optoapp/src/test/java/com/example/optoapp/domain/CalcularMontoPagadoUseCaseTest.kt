package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.pago.PagoDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalcularMontoPagadoUseCaseTest {

    @Before
    fun setUpLog() {
        mockkStatic("android.util.Log")
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
    }

    @Test
    fun `delegates to effect-aware sumMontoByDispensacion`() = runBlocking {
        val pagoDao = mockk<PagoDao>()
        coEvery { pagoDao.sumMontoByDispensacion("disp1") } returns 250.0

        val useCase = CalcularMontoPagadoUseCase(pagoDao)
        val result = useCase("disp1")

        assertEquals(250.0, result, 0.001)
        coVerify(exactly = 1) { pagoDao.sumMontoByDispensacion("disp1") }
    }

    @Test
    fun `returns zero when no pagos exist`() = runBlocking {
        val pagoDao = mockk<PagoDao>()
        coEvery { pagoDao.sumMontoByDispensacion("dispEmpty") } returns 0.0

        val useCase = CalcularMontoPagadoUseCase(pagoDao)
        assertEquals(0.0, useCase("dispEmpty"), 0.001)
    }

    @Test
    fun `returns PagoEffect net including Reverso`() = runBlocking {
        val pagoDao = mockk<PagoDao>()
        // Abono 200 + Reverso 50 + Anulación 100 → net 150
        coEvery { pagoDao.sumMontoByDispensacion("dispMixed") } returns 150.0

        val useCase = CalcularMontoPagadoUseCase(pagoDao)
        assertEquals(150.0, useCase("dispMixed"), 0.001)
        coVerify(exactly = 1) { pagoDao.sumMontoByDispensacion("dispMixed") }
    }
}
