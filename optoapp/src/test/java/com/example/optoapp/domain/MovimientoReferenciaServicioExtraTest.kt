package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MovimientoReferenciaServicioExtraTest {

    @Test
    fun reversoReferencia_is_distinct_from_sale() {
        val sale = "serv-1"
        val reverso = movimientoReferenciaForServicioExtraReverso("serv-1", "m-liquido")
        assertEquals("serv-1:rev:m-liquido", reverso)
        assertNotEquals(sale, reverso)
    }
}
