package com.example.optoapp.domain

import com.example.optoapp.data.EvaluacionClinica
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ClinicoRefraccionTest {

    @Test
    fun grupo_prefersRecetaAndCoarsensAstigmatismo() {
        val ev = EvaluacionClinica(
            id = "e1",
            pacienteId = "p1",
            fecha = LocalDate.of(2026, 10, 1),
            recetaOdEsf = "-1.00",
            recetaOdCil = "-0.50",
        )
        assertEquals(ClinicoRefraccion.MIOPIA, ClinicoRefraccion.grupo(ev))
    }

    @Test
    fun grupo_emetropiaWhenPlano() {
        val ev = EvaluacionClinica(
            id = "e1",
            pacienteId = "p1",
            fecha = LocalDate.of(2026, 10, 1),
            recetaOdEsf = "plano",
            recetaOdCil = "0",
        )
        assertEquals(ClinicoRefraccion.EMETROPIA, ClinicoRefraccion.grupo(ev))
    }

    @Test
    fun grupo_sinDatoWhenNoRefraction() {
        val ev = EvaluacionClinica(id = "e1", pacienteId = "p1", fecha = LocalDate.of(2026, 10, 1))
        assertEquals(ClinicoRefraccion.SIN_DATO, ClinicoRefraccion.grupo(ev))
    }

    @Test
    fun normalizeDiagnosticoLabel_collapsesEyeSuffixAndAccents() {
        assertEquals(ClinicoRefraccion.MIOPIA, ClinicoRefraccion.normalizeDiagnosticoLabel("Miopia (OD)"))
        assertEquals(ClinicoRefraccion.MIOPIA, ClinicoRefraccion.normalizeDiagnosticoLabel("Miopía OI"))
        assertEquals(ClinicoRefraccion.MIOPIA, ClinicoRefraccion.normalizeDiagnosticoLabel("Astigmatismo miópico compuesto"))
        assertEquals(ClinicoRefraccion.ASTIGMATISMO, ClinicoRefraccion.normalizeDiagnosticoLabel("Astigmatismo mixto"))
        assertEquals("Presbicia", ClinicoRefraccion.normalizeDiagnosticoLabel("Presbicia (AO)"))
    }
}
