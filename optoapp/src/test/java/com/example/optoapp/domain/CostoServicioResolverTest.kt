package com.example.optoapp.domain

import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CostoServicioResolverTest {

    private val biselado = ServicioTipoEntity(
        id = "t1", opticaId = "o1", nombre = "Biselado", costoPropio = 5.0, costoTercero = 12.0,
    )

    @Test
    fun selectingATypePreloadsTheCostOfTheChosenExecutor() {
        val propio = CostoServicioResolver.seleccionarTipo(
            CostoServicioDraft(realizadoPor = CostoServicioResolver.PROPIO), biselado,
        )
        val tercero = CostoServicioResolver.seleccionarTipo(
            CostoServicioDraft(realizadoPor = CostoServicioResolver.TERCERO), biselado,
        )

        assertEquals("5", propio.costoDraft)
        assertEquals("t1", propio.servicioTipoId)
        assertEquals("12", tercero.costoDraft)
    }

    @Test
    fun switchingTheExecutorPreloadsTheOtherCost() {
        val draft = CostoServicioResolver.seleccionarTipo(
            CostoServicioDraft(realizadoPor = CostoServicioResolver.PROPIO), biselado,
        )

        val switched = CostoServicioResolver.seleccionarRealizadoPor(draft, CostoServicioResolver.TERCERO)

        assertEquals(CostoServicioResolver.TERCERO, switched.realizadoPor)
        assertEquals("12", switched.costoDraft)
    }

    @Test
    fun aManuallyEditedCostSurvivesExecutorAndTypeChanges() {
        val edited = CostoServicioResolver.editarCosto(
            CostoServicioResolver.seleccionarTipo(CostoServicioDraft(realizadoPor = CostoServicioResolver.PROPIO), biselado),
            "7.50",
        )

        val switched = CostoServicioResolver.seleccionarRealizadoPor(edited, CostoServicioResolver.TERCERO)
        val retyped = CostoServicioResolver.seleccionarTipo(switched, biselado.copy(id = "t2", costoTercero = 30.0))

        assertTrue(retyped.costoEditado)
        assertEquals("7.50", retyped.costoDraft)
        assertEquals("t2", retyped.servicioTipoId)
    }

    @Test
    fun goingBackToPropioClearsTheThirdPartyName() {
        val tercero = CostoServicioResolver.editarNombre(
            CostoServicioResolver.seleccionarRealizadoPor(CostoServicioDraft(), CostoServicioResolver.TERCERO),
            "Laboratorio Sur",
        )

        val propio = CostoServicioResolver.seleccionarRealizadoPor(tercero, CostoServicioResolver.PROPIO)

        assertEquals("Laboratorio Sur", tercero.realizadoPorNombre)
        assertEquals("", propio.realizadoPorNombre)
    }

    @Test
    fun aNameIsIgnoredWhileTheExecutorIsPropio() {
        val draft = CostoServicioResolver.editarNombre(
            CostoServicioDraft(realizadoPor = CostoServicioResolver.PROPIO), "Laboratorio Sur",
        )

        assertEquals("", draft.realizadoPorNombre)
    }

    @Test
    fun withoutATypeTheCostDraftIsEmptyNotZero() {
        val draft = CostoServicioResolver.seleccionarRealizadoPor(CostoServicioDraft(), CostoServicioResolver.TERCERO)
        val cleared = CostoServicioResolver.seleccionarTipo(
            CostoServicioResolver.seleccionarTipo(draft, biselado), null,
        )

        assertEquals("", draft.costoDraft)
        assertEquals("", cleared.costoDraft)
        assertEquals(null, cleared.servicioTipoId)
        assertFalse(cleared.costoEditado)
    }

    @Test
    fun costoRealIsTheParsedDraftOrZero() {
        assertEquals(7.5, CostoServicioDraft(costoDraft = "7,50").costoReal(), 0.0)
        assertEquals(0.0, CostoServicioDraft(costoDraft = "").costoReal(), 0.0)
    }
}
