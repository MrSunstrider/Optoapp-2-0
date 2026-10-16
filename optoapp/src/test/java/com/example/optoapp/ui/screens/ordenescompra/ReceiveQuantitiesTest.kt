package com.example.optoapp.ui.screens.ordenescompra

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiveQuantitiesTest {

    @Test
    fun clamp_keepsValueInsideZeroToCantidad() {
        assertEquals(0, ReceiveQuantities.clamp(-3, 10))
        assertEquals(0, ReceiveQuantities.clamp(0, 10))
        assertEquals(4, ReceiveQuantities.clamp(4, 10))
        assertEquals(10, ReceiveQuantities.clamp(10, 10))
        assertEquals(10, ReceiveQuantities.clamp(15, 10))
    }

    @Test
    fun clamp_zeroCantidadAlwaysZero() {
        assertEquals(0, ReceiveQuantities.clamp(5, 0))
        assertEquals(0, ReceiveQuantities.clamp(-1, 0))
    }

    @Test
    fun clamp_respectsMinRecibidoFloor() {
        assertEquals(3, ReceiveQuantities.clamp(0, 10, minRecibido = 3))
        assertEquals(3, ReceiveQuantities.clamp(2, 10, minRecibido = 3))
        assertEquals(5, ReceiveQuantities.clamp(5, 10, minRecibido = 3))
        assertEquals(10, ReceiveQuantities.clamp(15, 10, minRecibido = 3))
        assertEquals(10, ReceiveQuantities.clamp(0, 10, minRecibido = 15))
    }

    @Test
    fun fillPending_mapsEachIdToCantidad() {
        val items = listOf(
            ReceiveQuantities.Line("i1", 10),
            ReceiveQuantities.Line("i2", 5),
        )
        assertEquals(mapOf("i1" to 10, "i2" to 5), ReceiveQuantities.fillPending(items))
    }

    @Test
    fun fillPending_emptyList_returnsEmptyMap() {
        assertEquals(emptyMap<String, Int>(), ReceiveQuantities.fillPending(emptyList()))
    }

    @Test
    fun parseQuantity_parsesDigitsAndClamps() {
        assertEquals(4, ReceiveQuantities.parseQuantity("4", 10))
        assertEquals(10, ReceiveQuantities.parseQuantity("99", 10))
        assertEquals(0, ReceiveQuantities.parseQuantity("-2", 10))
    }

    @Test
    fun parseQuantity_blankOrInvalid_returnsZeroWhenNoPrior() {
        assertEquals(0, ReceiveQuantities.parseQuantity("", 10))
        assertEquals(0, ReceiveQuantities.parseQuantity("   ", 10))
        assertEquals(0, ReceiveQuantities.parseQuantity("abc", 10))
        assertEquals(0, ReceiveQuantities.parseQuantity("1.5", 10))
    }

    @Test
    fun parseQuantity_blankOrInvalid_keepsPriorRecibido() {
        assertEquals(4, ReceiveQuantities.parseQuantity("", 10, minRecibido = 4))
        assertEquals(4, ReceiveQuantities.parseQuantity("   ", 10, minRecibido = 4))
        assertEquals(4, ReceiveQuantities.parseQuantity("abc", 10, minRecibido = 4))
        assertEquals(4, ReceiveQuantities.parseQuantity("1.5", 10, minRecibido = 4))
    }

    @Test
    fun parseQuantity_parsesDigitsAndClampsToFloor() {
        assertEquals(4, ReceiveQuantities.parseQuantity("2", 10, minRecibido = 4))
        assertEquals(7, ReceiveQuantities.parseQuantity("7", 10, minRecibido = 4))
    }

    @Test
    fun toAbsoluteMap_buildsClampedMapFromEditableText() {
        val textById = mapOf(
            "i1" to "4",
            "i2" to "",
            "i3" to "99",
        )
        val cantidadById = mapOf(
            "i1" to 10,
            "i2" to 5,
            "i3" to 8,
        )
        assertEquals(
            mapOf("i1" to 4, "i2" to 0, "i3" to 8),
            ReceiveQuantities.toAbsoluteMap(textById, cantidadById),
        )
    }

    @Test
    fun toAbsoluteMap_blankKeepsPriorAndRespectsFloor() {
        val textById = mapOf(
            "i1" to "4",
            "i2" to "",
            "i3" to "1",
        )
        val cantidadById = mapOf(
            "i1" to 10,
            "i2" to 5,
            "i3" to 8,
        )
        val minRecibidoById = mapOf(
            "i1" to 0,
            "i2" to 3,
            "i3" to 2,
        )
        assertEquals(
            mapOf("i1" to 4, "i2" to 3, "i3" to 2),
            ReceiveQuantities.toAbsoluteMap(textById, cantidadById, minRecibidoById),
        )
    }

    @Test
    fun toAbsoluteMap_skipsIdsWithoutCantidad() {
        val textById = mapOf("i1" to "3", "ghost" to "9")
        val cantidadById = mapOf("i1" to 10)
        assertEquals(mapOf("i1" to 3), ReceiveQuantities.toAbsoluteMap(textById, cantidadById))
    }
}
