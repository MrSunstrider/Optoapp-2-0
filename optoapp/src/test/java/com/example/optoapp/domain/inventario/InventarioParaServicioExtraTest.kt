package com.example.optoapp.domain.inventario

import com.example.optoapp.data.Montura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventarioParaServicioExtraTest {

    private fun montura(
        id: String,
        categoria: String = "",
        activo: Boolean = true,
    ) = Montura(id = id, marca = "M", modelo = id, categoria = categoria, activo = activo)

    @Test
    fun includes_active_accessory() {
        val items = listOf(
            montura("liquido-1", categoria = InventarioItemKind.ACCESORIO),
            montura("inactive", categoria = InventarioItemKind.ACCESORIO, activo = false),
        )
        val result = inventarioParaServicioExtra(items)
        assertEquals(1, result.size)
        assertEquals("liquido-1", result.first().id)
    }

    @Test
    fun includes_active_frame() {
        val items = listOf(montura("frame-1", categoria = "SOL"))
        assertEquals(1, inventarioParaServicioExtra(items).size)
    }

    @Test
    fun excludes_inactive_items() {
        val items = listOf(
            montura("cofre-1", categoria = InventarioItemKind.ACCESORIO, activo = false),
        )
        assertTrue(inventarioParaServicioExtra(items).isEmpty())
    }
}
