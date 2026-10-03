package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReplaceOtTokenTest {
    @Test
    fun `replaces the exact token wherever it stands alone`() {
        assertEquals(
            "Venta por reclamo de OT 2026-0042-R4",
            replaceOtToken("Venta por reclamo de OT 2026-0042-R1", "2026-0042-R1", "2026-0042-R4"),
        )
        assertEquals(
            "2026-0042-R4 y (2026-0042-R4).",
            replaceOtToken("2026-0042-R1 y (2026-0042-R1).", "2026-0042-R1", "2026-0042-R4"),
        )
    }

    @Test
    fun `does not touch longer or prefixed OTs that merely contain the token`() {
        val nota = "OT 2026-0042-R12 y X2026-0042-R1 y 2026-0042-R1-bis"

        assertEquals(nota, replaceOtToken(nota, "2026-0042-R1", "2026-0042-R4"))
    }

    @Test
    fun `leaves the original OT of the claim alone`() {
        val nota = "Crédito por reclamo de OT 2026-0042"

        assertEquals(nota, replaceOtToken(nota, "2026-0042-R1", "2026-0042-R4"))
    }

    @Test
    fun `treats regex characters in the OT literally`() {
        assertEquals("OT B.1+2", replaceOtToken("OT A.1+1", "A.1+1", "B.1+2"))
        assertEquals("OT AX1+1", replaceOtToken("OT AX1+1", "A.1+1", "B.1+2"))
    }

    @Test
    fun `a blank old OT changes nothing`() {
        assertEquals("nota", replaceOtToken("nota", "", "2026-0042-R4"))
    }
}
