package com.example.optoapp.domain.inventario

import com.example.optoapp.data.Montura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonturaLabelTest {

    @Test
    fun monturaLabel_withoutTipoAro_keepsSkuOnlyFormat() {
        val m = Montura(id = "1", sku = "SKU-1", marca = "Ray", modelo = "Ban", tipoAro = "")
        assertEquals("Ray Ban (SKU-1)", monturaLabel(m))
    }

    @Test
    fun monturaLabel_withTipoAro_appendsTipo() {
        val m = Montura(
            id = "1",
            sku = "RAY-2140",
            marca = "Ray-Ban",
            modelo = "Aviator",
            tipoAro = "Aro Completo",
        )
        assertEquals("Ray-Ban Aviator (RAY-2140) · Aro Completo", monturaLabel(m))
    }

    @Test
    fun monturaMatchesDescripcion_matchesLabelWithTipo() {
        val m = Montura(id = "1", sku = "S1", marca = "A", modelo = "B", tipoAro = "Semi al aire")
        assertTrue(monturaMatchesDescripcion(m, monturaLabel(m)))
        assertTrue(monturaMatchesDescripcion(m, "A B (S1) · Semi al aire"))
        assertFalse(monturaMatchesDescripcion(m, "algo (S1) resto"))
        assertFalse(monturaMatchesDescripcion(m, ""))
    }

    @Test
    fun monturaMatchesDescripcion_skuOnly_matchesAccessoryWithoutTipo() {
        val m = Montura(id = "1", sku = "LIQ-1", marca = "X", modelo = "Y", tipoAro = "")
        assertTrue(monturaMatchesDescripcion(m, "producto (LIQ-1)"))
    }
}
