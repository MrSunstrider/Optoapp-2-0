package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GastoTipoResolverTest {

    @Test
    fun `fixed category without explicit type resolves to fijo`() {
        assertEquals("fijo", GastoTipoResolver.resolver(categoria = "alquiler", tipoGasto = null))
    }

    @Test
    fun `variable category without explicit type resolves to variable`() {
        assertEquals("variable", GastoTipoResolver.resolver(categoria = "marketing", tipoGasto = null))
    }

    @Test
    fun `explicit variable type wins over a fixed category`() {
        assertEquals("variable", GastoTipoResolver.resolver(categoria = "alquiler", tipoGasto = "variable"))
    }

    @Test
    fun `explicit fijo type wins over a variable category`() {
        assertEquals("fijo", GastoTipoResolver.resolver(categoria = "otro", tipoGasto = "fijo"))
    }

    @Test
    fun `unknown category resolves to variable`() {
        assertEquals("variable", GastoTipoResolver.resolver(categoria = "categoria_nueva", tipoGasto = null))
    }

    @Test
    fun `every server fixed category resolves to fijo`() {
        listOf("alquiler", "servicios", "personal", "impuestos").forEach {
            assertEquals(it, "fijo", GastoTipoResolver.resolver(categoria = it, tipoGasto = null))
        }
    }

    @Test
    fun `a new gasto is preselected from its category`() {
        assertEquals("variable", GastoTipoResolver.preseleccionar(categoria = "marketing"))
        assertEquals("fijo", GastoTipoResolver.preseleccionar(categoria = "alquiler"))
    }

    @Test
    fun `only fijo and variable are valid explicit types`() {
        assertEquals(true, GastoTipoResolver.esValido("fijo"))
        assertEquals(true, GastoTipoResolver.esValido("variable"))
        assertEquals(false, GastoTipoResolver.esValido("mixto"))
        assertEquals(false, GastoTipoResolver.esValido(null))
    }

    @Test
    fun `unrecognized explicit type falls back to the category rule`() {
        assertEquals("fijo", GastoTipoResolver.resolver(categoria = "alquiler", tipoGasto = "otro"))
    }
}
