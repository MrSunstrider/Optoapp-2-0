package com.example.optoapp.domain

import com.example.optoapp.domain.CalcularCostoItemUseCase.Companion.seriePorCilindro
import com.example.optoapp.domain.CalcularCostoItemUseCase.Companion.tipoAroMatriz
import com.example.optoapp.domain.CalcularCostoItemUseCase.Companion.tipoLente
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcularCostoItemReglasTest {

    @Test
    fun `sphere and cylinder up to 6 diopters are stock`() {
        assertEquals("stock", tipoLente(0.0, null))
        assertEquals("stock", tipoLente(6.0, null))
        assertEquals("stock", tipoLente(-6.0, -6.0))
    }

    @Test
    fun `sphere or cylinder beyond 6 diopters is fabricacion`() {
        assertEquals("fabricacion", tipoLente(6.01, null))
        assertEquals("fabricacion", tipoLente(-7.0, -1.0))
        assertEquals("fabricacion", tipoLente(0.0, 6.01))
    }

    @Test
    fun `cylinder series boundaries`() {
        assertEquals(1, seriePorCilindro(null))
        assertEquals(1, seriePorCilindro(-2.00))
        assertEquals(1, seriePorCilindro(1.50))
        assertEquals(2, seriePorCilindro(-2.25))
        assertEquals(2, seriePorCilindro(-4.00))
        assertEquals(3, seriePorCilindro(-4.25))
        assertEquals(3, seriePorCilindro(-6.00))
        assertNull(seriePorCilindro(-6.25))
    }

    @Test
    fun `frame rim labels map to bevel matrix keys`() {
        assertEquals("aro_completo", tipoAroMatriz("Aro completo"))
        assertEquals("semi_aire", tipoAroMatriz("Semi al aire"))
        assertEquals("al_aire", tipoAroMatriz("Al aire"))
        assertEquals("aro_completo", tipoAroMatriz("ranurado"))
    }
}
