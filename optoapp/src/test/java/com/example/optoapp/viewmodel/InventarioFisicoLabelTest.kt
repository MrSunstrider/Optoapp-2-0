package com.example.optoapp.viewmodel

import com.example.optoapp.data.Montura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventarioFisicoLabelTest {

    @Test
    fun `buildMonturaConteoLabels uses marca modelo and sku`() {
        val montura = Montura(
            id = "m1",
            sku = "SKU-1",
            marca = "Ray-Ban",
            modelo = "Aviator",
            color = "Negro",
            talla = "54",
        )
        val labels = buildMonturaConteoLabels(listOf(montura))
        assertEquals("Ray-Ban Aviator", labels.getValue("m1").titulo)
        assertEquals("SKU SKU-1 · Negro · Talla 54", labels.getValue("m1").subtitulo)
    }

    @Test
    fun `buildMonturaConteoLabels falls back when marca modelo blank`() {
        val montura = Montura(id = "m2", sku = "ONLY-SKU", marca = "", modelo = "")
        val labels = buildMonturaConteoLabels(listOf(montura))
        assertEquals("ONLY-SKU", labels.getValue("m2").titulo)
    }

    @Test
    fun `buildMonturaConteoLabels uses Sin datos when all identity blank`() {
        val montura = Montura(id = "m3", sku = "", marca = "  ", modelo = "")
        val labels = buildMonturaConteoLabels(listOf(montura))
        assertEquals("Sin datos", labels.getValue("m3").titulo)
        assertTrue(labels.getValue("m3").subtitulo.startsWith("SKU —"))
    }
}
