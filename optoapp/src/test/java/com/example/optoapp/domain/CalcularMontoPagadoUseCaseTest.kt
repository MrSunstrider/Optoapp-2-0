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
    fun `delegates to sumMontoByDispensacion with correct parameters`() = runBlocking {
        val pagoDao = mockk<PagoDao>()
        coEvery { pagoDao.sumMontoByDispensacion("disp1", "Anulación") } returns 250.0

        val useCase = CalcularMontoPagadoUseCase(pagoDao)
        val result = useCase("disp1")

        assertEquals(250.0, result, 0.001)
        coVerify(exactly = 1) { pagoDao.sumMontoByDispensacion("disp1", "Anulación") }
    }

    @Test
    fun `returns zero when no pagos exist`() = runBlocking {
        val pagoDao = mockk<PagoDao>()
        coEvery { pagoDao.sumMontoByDispensacion("dispEmpty", "Anulación") } returns 0.0

        val useCase = CalcularMontoPagadoUseCase(pagoDao)
        val result = useCase("dispEmpty")

        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `excludes Anulacion from mixed tipos`() = runBlocking {
        val pagoDao = mockk<PagoDao>()
        coEvery { pagoDao.sumMontoByDispensacion("dispMixed", "Anulación") } returns 300.0

        val useCase = CalcularMontoPagadoUseCase(pagoDao)
        val result = useCase("dispMixed")

        assertEquals(300.0, result, 0.001)
        coVerify(exactly = 1) { pagoDao.sumMontoByDispensacion("dispMixed", "Anulación") }
    }
}
