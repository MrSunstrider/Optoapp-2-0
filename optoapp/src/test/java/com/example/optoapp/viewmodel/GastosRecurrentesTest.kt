package com.example.optoapp.viewmodel

import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class GastosRecurrentesTest {

    private fun autoGenerar(templates: List<GastoOperativoEntity>, existentes: List<GastoOperativoEntity>, mes: LocalDate) =
        GastosViewModel.autoGenerarRecurrentes(templates, existentes, mes)

    @Test
    fun `nuevo gasto tiene esRecurrente false por defecto`() {
        val g = GastoOperativoEntity(id = "g1", opticaId = "o1", categoria = "alquiler", monto = 800.0, fecha = LocalDate.now())
        assertFalse(g.esRecurrente)
        assertEquals("mensual", g.frecuencia)
    }

    @Test
    fun `gasto recurrente se crea con flag true`() {
        val g = GastoOperativoEntity(id = "g2", opticaId = "o1", categoria = "alquiler", monto = 800.0, fecha = LocalDate.now(), esRecurrente = true)
        assertTrue(g.esRecurrente)
    }

    @Test
    fun `autoGenerarRecurrentes crea copia para mes nuevo`() {
        val template = GastoOperativoEntity(id = "tpl-1", opticaId = "o1", categoria = "alquiler", monto = 800.0, fecha = LocalDate.of(2026, 6, 1), esRecurrente = true)
        val nuevos = autoGenerar(listOf(template), emptyList(), LocalDate.of(2026, 7, 1))
        assertEquals(1, nuevos.size)
        assertEquals("alquiler", nuevos[0].categoria)
        assertEquals(800.0, nuevos[0].monto, 0.0)
        assertEquals(LocalDate.of(2026, 7, 1), nuevos[0].fecha)
        assertFalse(nuevos[0].esRecurrente)
    }

    @Test
    fun `autoGenerarRecurrentes no duplica si ya existe copia este mes`() {
        val template = GastoOperativoEntity(id = "tpl-1", opticaId = "o1", categoria = "personal", monto = 2500.0, fecha = LocalDate.of(2026, 6, 1), esRecurrente = true)
        val copia = GastoOperativoEntity(id = "cop-1", opticaId = "o1", categoria = "personal", monto = 2500.0, fecha = LocalDate.of(2026, 7, 1), esRecurrente = false)
        val nuevos = autoGenerar(listOf(template), listOf(copia), LocalDate.of(2026, 7, 1))
        assertTrue(nuevos.isEmpty())
    }

    @Test
    fun `autoGenerarRecurrentes ignora gastos no recurrentes`() {
        val normal = GastoOperativoEntity(id = "g1", opticaId = "o1", categoria = "servicios", monto = 120.0, fecha = LocalDate.of(2026, 7, 5), esRecurrente = false)
        val nuevos = autoGenerar(listOf(normal), emptyList(), LocalDate.of(2026, 7, 1))
        assertTrue(nuevos.isEmpty())
    }

    @Test
    fun `autoGenerarRecurrentes crea multiples templates`() {
        val t1 = GastoOperativoEntity(id = "t1", opticaId = "o1", categoria = "alquiler", monto = 800.0, fecha = LocalDate.of(2026, 6, 1), esRecurrente = true)
        val t2 = GastoOperativoEntity(id = "t2", opticaId = "o1", categoria = "personal", monto = 2500.0, fecha = LocalDate.of(2026, 6, 1), esRecurrente = true)
        val nuevos = autoGenerar(listOf(t1, t2), emptyList(), LocalDate.of(2026, 7, 1))
        assertEquals(2, nuevos.size)
    }

    @Test
    fun `autoGenerarRecurrentes detecta copia por categoria y mes`() {
        val template = GastoOperativoEntity(id = "tpl-local", opticaId = "o1", categoria = "alquiler", monto = 800.0, fecha = LocalDate.of(2026, 1, 1), esRecurrente = true)
        val copiaEnero = GastoOperativoEntity(id = "cop-enero", opticaId = "o1", categoria = "alquiler", monto = 850.0, fecha = LocalDate.of(2026, 1, 1), esRecurrente = false)
        val nuevos = autoGenerar(listOf(template), listOf(copiaEnero), LocalDate.of(2026, 7, 1))
        assertEquals(1, nuevos.size)
        assertEquals(LocalDate.of(2026, 7, 1), nuevos[0].fecha)
    }

    // ── R30: Category constraint tests ────────────────────────────────────────

    @Test
    fun `GastosUiState default categoria is alquiler`() {
        assertEquals("alquiler", GastosUiState().categoria)
    }

    @Test
    fun `CATEGORIAS contains exactly DB CHECK constraint values`() {
        val expected = setOf("alquiler", "servicios", "personal", "proveedores",
            "insumos", "marketing", "impuestos", "otro")
        assertEquals(expected, GastosViewModel.CATEGORIAS.toSet())
        assertEquals(8, GastosViewModel.CATEGORIAS.size)
    }
}
