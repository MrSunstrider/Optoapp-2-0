package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Variance = stockContado - stockSistema.
 * Negative when counted is less than system, positive when counted exceeds system.
 */
class VarianceCalculationTest {

    @Test
    fun variance_positiveWhenCountedExceedsSystem() {
        assertEquals(5, calculateVariance(stockSistema = 10, stockContado = 15))
    }

    @Test
    fun variance_negativeWhenCountedLessThanSystem() {
        assertEquals(-3, calculateVariance(stockSistema = 10, stockContado = 7))
    }

    @Test
    fun variance_zeroWhenExactMatch() {
        assertEquals(0, calculateVariance(stockSistema = 10, stockContado = 10))
    }

    @Test
    fun variance_handlesZeroSystemStock() {
        assertEquals(5, calculateVariance(stockSistema = 0, stockContado = 5))
    }

    @Test
    fun variance_handlesZeroCounted() {
        assertEquals(-10, calculateVariance(stockSistema = 10, stockContado = 0))
    }

    @Test
    fun variance_handlesBothZero() {
        assertEquals(0, calculateVariance(stockSistema = 0, stockContado = 0))
    }

    @Test
    fun variance_largeNumbers() {
        assertEquals(500, calculateVariance(stockSistema = 1000, stockContado = 1500))
    }

    @Test
    fun variance_largeNegativeDifference() {
        assertEquals(-500, calculateVariance(stockSistema = 1000, stockContado = 500))
    }

    companion object {
        fun calculateVariance(stockSistema: Int, stockContado: Int): Int = stockContado - stockSistema
    }
}
