package com.example.optoapp.ui.screens

import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import com.example.optoapp.domain.CostoServicioDraft
import com.example.optoapp.domain.CostoServicioResolver
import com.example.optoapp.ui.MainSources
import com.example.optoapp.ui.components.servicio.ejecutorEnIndice
import com.example.optoapp.ui.components.servicio.indiceEjecutor
import com.example.optoapp.ui.components.servicio.muestraNombreTercero
import com.example.optoapp.ui.components.servicio.opcionesTipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServicioFormExecutorTest {

    private val section = "java/com/example/optoapp/ui/components/servicio/ServicioEjecutorSection.kt"

    @Test
    fun yoHidesTheThirdPartyNameFieldAndOtroOpticoShowsIt() {
        assertFalse(muestraNombreTercero(CostoServicioDraft(realizadoPor = CostoServicioResolver.PROPIO)))
        assertTrue(muestraNombreTercero(CostoServicioDraft(realizadoPor = CostoServicioResolver.TERCERO)))
    }

    @Test
    fun theSegmentsAreYoThenOtroOptico() {
        assertEquals(0, indiceEjecutor(CostoServicioResolver.PROPIO))
        assertEquals(CostoServicioResolver.TERCERO, ejecutorEnIndice(1))
        assertEquals("Yo", MainSources.stringValue("servicio_realizado_yo"))
        assertEquals("Otro óptico", MainSources.stringValue("servicio_realizado_otro"))
    }

    @Test
    fun aSavedDeactivatedTypeStaysSelectable() {
        val inactivo = ServicioTipoEntity(id = "t1", opticaId = "o1", nombre = "Biselado", activo = false)
        val activo = ServicioTipoEntity(id = "t2", opticaId = "o1", nombre = "Calibrado")

        val opciones = opcionesTipo(CostoServicioDraft(tipo = inactivo), listOf(activo))

        assertEquals(listOf("t1", "t2"), opciones.map { it.id })
    }

    @Test
    fun theExecutorChoiceUsesTheSharedSegmentedSelector() {
        assertTrue(MainSources.read(section).contains("OptoSegmentedSelector("))
    }
}
