package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertasDesdeRecomendacionesTest {

    @Test
    fun mapsAltaToRojoAndMediaToAmbar_skipsBaja() {
        val alertas = AlertasDesdeRecomendaciones.from(
            listOf(
                rec("a", Prioridad.BAJA, "Baja"),
                rec("b", Prioridad.MEDIA, "Media"),
                rec("c", Prioridad.ALTA, "Alta"),
            ),
        )
        assertEquals(2, alertas.size)
        assertEquals(SeveridadAlerta.ROJO, alertas[0].severidad)
        assertEquals("Alta", alertas[0].titulo)
        assertEquals(SeveridadAlerta.AMBAR, alertas[1].severidad)
        assertEquals("Media", alertas[1].titulo)
    }

    @Test
    fun capsAtLimitePreferringRojoOrder() {
        val alertas = AlertasDesdeRecomendaciones.from(
            listOf(
                rec("m1", Prioridad.MEDIA, "M1"),
                rec("m2", Prioridad.MEDIA, "M2"),
                rec("a1", Prioridad.ALTA, "A1"),
                rec("a2", Prioridad.ALTA, "A2"),
                rec("m3", Prioridad.MEDIA, "M3"),
                rec("a3", Prioridad.ALTA, "A3"),
            ),
            limite = 3,
        )
        assertEquals(3, alertas.size)
        assertTrue(alertas.all { it.severidad == SeveridadAlerta.ROJO })
        assertEquals(listOf("A1", "A2", "A3"), alertas.map { it.titulo })
    }

    @Test
    fun usesHallazgoWhenPresentElseDetalle() {
        val conHallazgo = AlertasDesdeRecomendaciones.from(
            listOf(rec("h", Prioridad.ALTA, "T", hallazgo = "dato", detalle = "largo")),
        ).single()
        assertEquals("dato", conHallazgo.detalle)

        val sinHallazgo = AlertasDesdeRecomendaciones.from(
            listOf(rec("d", Prioridad.MEDIA, "T", hallazgo = "", detalle = "largo")),
        ).single()
        assertEquals("largo", sinHallazgo.detalle)
    }

    private fun rec(
        id: String,
        prioridad: Prioridad,
        titulo: String,
        hallazgo: String = "",
        detalle: String = "detalle",
    ) = Recomendacion(
        id = id,
        tipo = RecomendacionTipo.COBRAR,
        titulo = titulo,
        detalle = detalle,
        prioridad = prioridad,
        hallazgo = hallazgo,
    )
}
