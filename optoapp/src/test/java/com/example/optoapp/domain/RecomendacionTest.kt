package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecomendacionTest {

    @Test
    fun recomendacion_allFields_constructedCorrectly() {
        val datos = DatosAccion(
            pacienteIds = listOf("p1", "p2"),
            productoIds = listOf("prod1"),
            montoTotal = 150.0,
        )
        val rec = Recomendacion(
            id = "COBRAR::Hay deudas pendientes",
            tipo = RecomendacionTipo.COBRAR,
            titulo = "Hay deudas pendientes",
            detalle = "Total de S/ 4,200 en 3 deudores",
            impactoEstimado = "S/ 4,200 si se cobra todo",
            prioridad = Prioridad.ALTA,
            accion = "Llamar a los deudores",
            datosAccion = datos,
        )

        assertEquals("COBRAR::Hay deudas pendientes", rec.id)
        assertEquals(RecomendacionTipo.COBRAR, rec.tipo)
        assertEquals("Hay deudas pendientes", rec.titulo)
        assertEquals("Total de S/ 4,200 en 3 deudores", rec.detalle)
        assertEquals("S/ 4,200 si se cobra todo", rec.impactoEstimado)
        assertEquals(Prioridad.ALTA, rec.prioridad)
        assertEquals("Llamar a los deudores", rec.accion)
        assertEquals(datos, rec.datosAccion)
    }

    @Test
    fun recomendacion_differentTipoValues_compiles() {
        val tipos = RecomendacionTipo.entries
        assertEquals(6, tipos.size)

        for (tipo in tipos) {
            val rec = Recomendacion(
                id = "test",
                tipo = tipo,
                titulo = "Test",
                detalle = "Test",
                impactoEstimado = null,
                prioridad = Prioridad.MEDIA,
                accion = null,
                datosAccion = null,
            )
            assertEquals(tipo, rec.tipo)
        }
    }

    @Test
    fun prioridad_altaOrdinalIsZero() {
        assertEquals(0, Prioridad.ALTA.ordinal)
        assertEquals(1, Prioridad.MEDIA.ordinal)
        assertEquals(2, Prioridad.BAJA.ordinal)
    }

    @Test
    fun datosAccion_allDefaults_areNull() {
        val datos = DatosAccion()
        assertNull(datos.pacienteIds)
        assertNull(datos.productoIds)
        assertNull(datos.montoTotal)
    }

    @Test
    fun datosAccion_withValues_preservesValues() {
        val datos = DatosAccion(
            pacienteIds = listOf("a", "b"),
            productoIds = listOf("x"),
            montoTotal = 123.45,
        )
        assertEquals(listOf("a", "b"), datos.pacienteIds)
        assertEquals(listOf("x"), datos.productoIds)
        assertEquals(123.45, datos.montoTotal)
    }
}
