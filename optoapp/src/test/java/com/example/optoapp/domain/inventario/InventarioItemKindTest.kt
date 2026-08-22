package com.example.optoapp.domain.inventario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventarioItemKindTest {

    @Test
    fun `accesorio categoria is detected case-insensitively`() {
        assertTrue(InventarioItemKind.isAccesorio("ACCESORIO"))
        assertTrue(InventarioItemKind.isAccesorio("accesorio"))
        assertFalse(InventarioItemKind.isAccesorio(""))
        assertFalse(InventarioItemKind.isAccesorio("GRADUADA"))
        assertFalse(InventarioItemKind.isAccesorio(null))
    }

    @Test
    fun `armazon excludes accesorio for OT picker`() {
        assertTrue(InventarioItemKind.isArmazon(""))
        assertTrue(InventarioItemKind.isArmazon("SOL"))
        assertFalse(InventarioItemKind.isArmazon("ACCESORIO"))
    }

    @Test
    fun `categoriaForSave forces ACCESORIO for accessory tipo`() {
        assertEquals(
            InventarioItemKind.ACCESORIO,
            InventarioItemKind.categoriaForSave(InventarioItemKind.ACCESORIO, "SOL"),
        )
        assertEquals(
            "GRADUADA",
            InventarioItemKind.categoriaForSave(InventarioItemKind.MONTURA, "GRADUADA"),
        )
        assertEquals(
            "",
            InventarioItemKind.categoriaForSave(InventarioItemKind.MONTURA, "ACCESORIO"),
        )
    }

    @Test
    fun `tipoItemFromCategoria maps persisted categoria`() {
        assertEquals(
            InventarioItemKind.ACCESORIO,
            InventarioItemKind.tipoItemFromCategoria("ACCESORIO"),
        )
        assertEquals(
            InventarioItemKind.MONTURA,
            InventarioItemKind.tipoItemFromCategoria("SOL"),
        )
    }
}
