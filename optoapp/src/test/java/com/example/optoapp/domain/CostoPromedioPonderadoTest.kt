package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CostoPromedioPonderadoTest {

    @Test
    fun `no prior stock takes the purchase order cost`() {
        assertEquals(60.0, CostoPromedioPonderado.calcular(stockPrevio = 0, costoPrevio = 50.0, cantidad = 10, costoPo = 60.0), 0.0)
    }

    @Test
    fun `null previous cost with stock takes the purchase order cost`() {
        assertEquals(60.0, CostoPromedioPonderado.calcular(stockPrevio = 5, costoPrevio = null, cantidad = 5, costoPo = 60.0), 0.0)
    }

    @Test
    fun `zero previous cost means missing cost and is not diluted`() {
        assertEquals(60.0, CostoPromedioPonderado.calcular(stockPrevio = 10, costoPrevio = 0.0, cantidad = 10, costoPo = 60.0), 0.0)
    }

    @Test
    fun `prior stock averages both costs by quantity`() {
        assertEquals(55.0, CostoPromedioPonderado.calcular(stockPrevio = 10, costoPrevio = 50.0, cantidad = 10, costoPo = 60.0), 0.0)
    }

    @Test
    fun `average is rounded half up to two decimals`() {
        assertEquals(10.03, CostoPromedioPonderado.calcular(stockPrevio = 3, costoPrevio = 10.0, cantidad = 1, costoPo = 10.10), 0.0)
    }

    @Test
    fun `negative stock is treated as empty stock`() {
        assertEquals(60.0, CostoPromedioPonderado.calcular(stockPrevio = -2, costoPrevio = 50.0, cantidad = 4, costoPo = 60.0), 0.0)
    }

    @Test
    fun `non positive purchase order cost keeps the previous cost`() {
        assertEquals(50.0, CostoPromedioPonderado.calcular(stockPrevio = 10, costoPrevio = 50.0, cantidad = 10, costoPo = 0.0), 0.0)
        assertEquals(50.0, CostoPromedioPonderado.calcular(stockPrevio = 10, costoPrevio = 50.0, cantidad = 10, costoPo = -1.0), 0.0)
    }

    @Test
    fun `non positive quantity keeps the previous cost`() {
        assertEquals(50.0, CostoPromedioPonderado.calcular(stockPrevio = 10, costoPrevio = 50.0, cantidad = 0, costoPo = 60.0), 0.0)
    }

    @Test
    fun `skipped average with missing previous cost reads as zero`() {
        assertEquals(0.0, CostoPromedioPonderado.calcular(stockPrevio = 10, costoPrevio = null, cantidad = 10, costoPo = 0.0), 0.0)
    }
}
