package com.example.optoapp.domain

import com.example.optoapp.data.ServicioExtra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ServicioRemotoMonturaIdTest {

    private val date = LocalDate.of(2026, 9, 2)

    @Test
    fun toRemoto_includes_montura_id() {
        val entity = ServicioExtra(
            id = "s1",
            monturaId = "m-liquido",
            descripcion = "Líquido limpiador",
            montoTotal = 15.0,
            estado = "Entregado",
            fecha = date,
            opticaId = "o1",
        )
        val remoto = entity.toRemoto()
        assertEquals("m-liquido", remoto.monturaId)
    }

    @Test
    fun roundtrip_montura_id() {
        val remoto = ServicioRemoto(
            id = "s2",
            monturaId = "m-cofre",
            descripcion = "Cofre de lentes",
            montoTotal = 25.0,
            estado = "Pendiente",
            fecha = date.toString(),
            opticaId = "o1",
        )
        assertEquals("m-cofre", remoto.toEntity().monturaId)
    }

    @Test
    fun blank_montura_id_maps_to_null() {
        val remoto = ServicioRemoto(
            id = "s3",
            monturaId = "  ",
            descripcion = "Servicio libre",
            montoTotal = 10.0,
            estado = "Pendiente",
            fecha = date.toString(),
            opticaId = "o1",
        )
        assertNull(remoto.toEntity().monturaId)
    }
}
