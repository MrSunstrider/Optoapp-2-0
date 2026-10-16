package com.example.optoapp.ui.navigation

import com.example.optoapp.ui.screens.dispensacion.pasoInicialDispensacion
import com.example.optoapp.viewmodel.CostosYGastosUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditarDispensacionRouteTest {

    @Test
    fun `an order opened on its cost section carries the section argument`() {
        val route = Route.EditarDispensacion("pac-1", "disp-1", Route.EditarDispensacion.SECCION_COSTO).route

        assertEquals("editarDispensacion/pac-1/disp-1?seccion=costo", route)
    }

    @Test
    fun `an order opened without a section keeps the plain route`() {
        assertEquals("editarDispensacion/pac-1/disp-1", Route.EditarDispensacion("pac-1", "disp-1").route)
    }

    @Test
    fun `the registered pattern accepts the optional section`() {
        assertEquals("editarDispensacion/{pacienteId}/{dispId}?seccion={seccion}", Route.EditarDispensacion.PATTERN)
    }

    @Test
    fun `order costs are managed in the order itself instead of a costs screen route`() {
        assertTrue(Route::class.java.declaredClasses.none { it.simpleName == "CostosYGastosDisp" })
        val estadoCostos = CostosYGastosUiState::class.java.declaredFields.map { it.name }
        assertTrue(estadoCostos.none { it == "costosPorDispensacion" || it == "dispensacionFilterId" })
    }

    @Test
    fun `the cost section opens the products step where the section lives`() {
        assertEquals(1, pasoInicialDispensacion(Route.EditarDispensacion.SECCION_COSTO))
        assertEquals(0, pasoInicialDispensacion(null))
        assertEquals(0, pasoInicialDispensacion("otra"))
    }
}
