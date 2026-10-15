package com.example.optoapp.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RutaEdicionDeudorTest {

    @Test
    fun `dispensacion debt opens the OT edit route of its patient`() {
        assertEquals("editarDispensacion/p1/d1", rutaEdicionDeudor(ventaId = "v_disp_d1", pacienteId = "p1"))
    }

    @Test
    fun `servicio debt opens the servicio edit route`() {
        assertEquals("editar_servicio/s1", rutaEdicionDeudor(ventaId = "v_serv_s1", pacienteId = "p1"))
    }

    @Test
    fun `debt without a patient has nowhere to go`() {
        assertNull(rutaEdicionDeudor(ventaId = "v_disp_d1", pacienteId = ""))
    }
}
