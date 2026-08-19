package com.example.optoapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacienteListQueryTest {

    @Test
    fun `blank query matches everyone`() {
        assertTrue(
            pacienteMatchesListQuery(
                query = "  ",
                nombreCompleto = "Ana",
                id = "p1",
                telefono = "999",
                historiaOptometrica = null,
            ),
        )
    }

    @Test
    fun `matches nombre id telefono and historia`() {
        assertTrue(pacienteMatchesListQuery("Ana", "Ana Ruiz", "p1", "111", null))
        assertTrue(pacienteMatchesListQuery("p1", "Ana Ruiz", "p1", "111", null))
        assertTrue(pacienteMatchesListQuery("111", "Ana Ruiz", "p1", "111", null))
        assertTrue(pacienteMatchesListQuery("HO-9", "Ana Ruiz", "p1", "111", "HO-9"))
        assertFalse(pacienteMatchesListQuery("Pedro", "Ana Ruiz", "p1", "111", "HO-9"))
    }

    @Test
    fun `matches assigned dispensacion or servicio OT`() {
        assertTrue(
            pacienteMatchesListQuery(
                query = "4582",
                nombreCompleto = "Salome",
                id = "p1",
                telefono = "111",
                historiaOptometrica = null,
                assignedOts = listOf("4582", "SE-1"),
            ),
        )
        assertTrue(
            pacienteMatchesListQuery(
                query = "se-1",
                nombreCompleto = "Salome",
                id = "p1",
                telefono = "111",
                historiaOptometrica = null,
                assignedOts = listOf("4582", "SE-1"),
            ),
        )
        assertFalse(
            pacienteMatchesListQuery(
                query = "4582",
                nombreCompleto = "Otro",
                id = "p2",
                telefono = "222",
                historiaOptometrica = null,
                assignedOts = emptyList(),
            ),
        )
    }

    @Test
    fun `tieneSaldoPendiente prefers ledger over doubled cache`() {
        assertTrue(tieneSaldoPendiente(170.0, cachePagado = 200.0, ledgerPagado = 100.0))
        assertFalse(tieneSaldoPendiente(170.0, cachePagado = 0.0, ledgerPagado = 170.0))
        assertTrue(tieneSaldoPendiente(170.0, cachePagado = 100.0, ledgerPagado = null))
        assertFalse(tieneSaldoPendiente(170.0, cachePagado = 170.0, ledgerPagado = null))
    }
}
