package com.example.optoapp.domain

import com.example.optoapp.data.costolc.CostoLcEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModalidadLcMapperTest {

    private fun row(id: String, modalidad: String, costo: Double) = CostoLcEntity(
        id = id,
        opticaId = "o1",
        tipoLc = "graduado",
        materialLc = "Hydrogel",
        modalidad = modalidad,
        costoUnitario = costo,
        vigenteDesde = "2026-01-01",
    )

    @Test
    fun modalidadIsInferredFromTheSelectorTextOrFreeText() {
        assertEquals("diario", ModalidadLcMapper.inferir("Diario"))
        assertEquals("quincenal", ModalidadLcMapper.inferir("QUINCENAL"))
        assertEquals("mensual", ModalidadLcMapper.inferir("Reemplazo mensual"))
        assertEquals("anual", ModalidadLcMapper.inferir(" Anual "))
    }

    @Test
    fun unknownOrAmbiguousTextIsNotInferred() {
        assertNull(ModalidadLcMapper.inferir("Lentes de Contacto"))
        assertNull(ModalidadLcMapper.inferir("mensual o anual"))
        assertNull(ModalidadLcMapper.inferir(""))
        assertNull(ModalidadLcMapper.inferir(null))
    }

    @Test
    fun knownModalidadPicksTheNewestRowOfThatModalidad() {
        val candidatos = listOf(row("d", "diario", 60.0), row("m2", "mensual", 47.0), row("m1", "mensual", 45.0))

        assertEquals("m2", ModalidadLcMapper.elegir(candidatos, "mensual")?.id)
        assertNull(ModalidadLcMapper.elegir(candidatos, "anual"))
    }

    @Test
    fun unknownModalidadAcceptsOnlyASingleCandidateModalidad() {
        val unica = listOf(row("m2", "mensual", 47.0), row("m1", "mensual", 45.0))
        val varias = listOf(row("m", "mensual", 45.0), row("d", "diario", 60.0))

        assertEquals("m2", ModalidadLcMapper.elegir(unica, null)?.id)
        assertNull(ModalidadLcMapper.elegir(varias, null))
        assertNull(ModalidadLcMapper.elegir(emptyList(), null))
    }
}
