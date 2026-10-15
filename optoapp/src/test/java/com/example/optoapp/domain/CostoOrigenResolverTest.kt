package com.example.optoapp.domain

import com.example.optoapp.domain.OrigenComponente.FALTANTE
import com.example.optoapp.domain.OrigenComponente.MANUAL
import com.example.optoapp.domain.OrigenComponente.MATRIZ
import com.example.optoapp.domain.OrigenComponente.PRESERVADA
import org.junit.Assert.assertEquals
import org.junit.Test

class CostoOrigenResolverTest {

    @Test
    fun `any missing component makes the item faltante even next to manual ones`() {
        assertEquals("faltante", CostoOrigenResolver.resolver(listOf(MANUAL, FALTANTE, MATRIZ), origenPrevio = null))
    }

    @Test
    fun `all matrix components resolve to matriz`() {
        assertEquals("matriz", CostoOrigenResolver.resolver(listOf(MATRIZ, MATRIZ), origenPrevio = null))
    }

    @Test
    fun `all manual components resolve to manual`() {
        assertEquals("manual", CostoOrigenResolver.resolver(listOf(MANUAL, MANUAL), origenPrevio = null))
    }

    @Test
    fun `manual and matrix components resolve to mixto`() {
        assertEquals("mixto", CostoOrigenResolver.resolver(listOf(MANUAL, MATRIZ), origenPrevio = null))
    }

    @Test
    fun `an item without applicable components is faltante`() {
        assertEquals("faltante", CostoOrigenResolver.resolver(emptyList(), origenPrevio = "matriz"))
    }

    @Test
    fun `preserved components follow a manual or matriz previous origin`() {
        assertEquals("manual", CostoOrigenResolver.resolver(listOf(PRESERVADA, MANUAL), origenPrevio = "manual"))
        assertEquals("matriz", CostoOrigenResolver.resolver(listOf(PRESERVADA, MATRIZ), origenPrevio = "matriz"))
    }

    @Test
    fun `preserved components of a mixto, faltante or legacy item count as mixto`() {
        assertEquals("mixto", CostoOrigenResolver.resolver(listOf(PRESERVADA, MATRIZ), origenPrevio = "mixto"))
        assertEquals("mixto", CostoOrigenResolver.resolver(listOf(PRESERVADA, MATRIZ), origenPrevio = "faltante"))
        assertEquals("mixto", CostoOrigenResolver.resolver(listOf(PRESERVADA, MANUAL), origenPrevio = null))
    }
}
