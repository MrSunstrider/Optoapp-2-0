package com.example.optoapp.viewmodel

import com.example.optoapp.data.costoproducto.CostoProductoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CostBlockFilterTest {

    private fun row(
        id: String,
        tipo: String,
        material: String,
        stockFab: String = "stock",
    ) = CostoProductoEntity(
        id = id,
        opticaId = "o1",
        material = material,
        tipoLente = tipo,
        stockOFabricacion = stockFab,
        costoUnitario = 10.0,
        vigenteDesde = "2026-01-01",
    )

    @Test
    fun blockFilter_stockMonofocalKeepsOnlyMonofocal() {
        val filter = blockFilter("Stock Monofocal")
        assertEquals("stock", filter.stockOFabricacion)
        assertEquals("Monofocal", filter.tipoLente)
        val rows = listOf(
            row("1", "Monofocal", "Resina"),
            row("2", "Bifocal", "Resina"),
            row("3", "Multifocal", "Resina"),
        )
        assertEquals(listOf("1"), aplicarFiltroBloque(rows, filter).map { it.id })
    }

    @Test
    fun blockFilter_fabricacionKeepsAllMaterialsIncludingPolicarbonato() {
        val filter = blockFilter("Fabricación")
        assertEquals("fabricacion", filter.stockOFabricacion)
        assertNull(filter.materialContains)
        val rows = listOf(
            row("1", "Monofocal", "Resina antireflejo", "fabricacion"),
            row("2", "Monofocal", "Cristal", "fabricacion"),
            row("3", "Monofocal", "Policarbonato", "fabricacion"),
        )
        assertEquals(listOf("1", "2", "3"), aplicarFiltroBloque(rows, filter).map { it.id })
    }

    @Test
    fun blockToFilter_stillReturnsStockOrFabricacionKey() {
        assertEquals("stock", blockToFilter("Stock Bifocal"))
        assertEquals("fabricacion", blockToFilter("Fabricación"))
        assertEquals("fabricacion", blockToFilter("Fabricación Resina"))
    }

    @Test
    fun blockForCosto_routesStockByTipoAndFabToFabricacion() {
        assertEquals("Stock Monofocal", blockForCosto("stock", "Monofocal"))
        assertEquals("Fabricación", blockForCosto("fabricacion", "Monofocal"))
        assertEquals("Fabricación", blockForCosto("stock", "Ocupacional"))
    }
}
