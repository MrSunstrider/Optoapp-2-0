package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProveedorTipoDefaultTest {

    @Test
    fun proveedor_defaults_tipo_to_monturas() {
        val p = Proveedor(
            id = "p1",
            nombre = "Acme",
            ruc = "20111111111",
            opticaId = "o1",
        )
        assertEquals("monturas", p.tipo)
    }

    @Test
    fun proveedor_preserves_explicit_tipo() {
        val p = Proveedor(
            id = "p2",
            nombre = "Lab X",
            ruc = "20222222222",
            tipo = "laboratorio",
            opticaId = "o1",
        )
        assertEquals("laboratorio", p.tipo)
    }
}
