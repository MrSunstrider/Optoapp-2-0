package com.example.optoapp.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumenDiarioRemotoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun serverRowKeepsTheRealCostOfSalesWhenStoredLocally() {
        val payload = """
            {"id":"r-1","optica_id":"o-1","fecha":"2026-07-10","ventas_cantidad":3,
             "ventas_monto_total":450.0,"ventas_costo_total":183.5,"cobros_cantidad":2,
             "cobros_monto_total":300.0,"saldo_pendiente_total":150.0,"saldo_pendiente_cantidad":1,
             "inventario_valor":1200.0,"inventario_unidades":40,"calculado_en":"2026-07-10T23:00:00Z",
             "margen_bruto":266.5}
        """.trimIndent()

        val entidad = json.decodeFromString<ResumenDiarioRemoto>(payload).toEntity()

        assertEquals("r-1", entidad.id)
        assertEquals("o-1", entidad.opticaId)
        assertEquals("2026-07-10", entidad.fecha)
        assertEquals(3, entidad.ventasCantidad)
        assertEquals(450.0, entidad.ventasMontoTotal, 0.0)
        assertEquals(183.5, entidad.ventasCostoTotal, 0.0)
        assertEquals(2, entidad.cobrosCantidad)
        assertEquals(300.0, entidad.cobrosMontoTotal, 0.0)
        assertEquals(150.0, entidad.saldoPendienteTotal, 0.0)
        assertEquals(1, entidad.saldoPendienteCantidad)
        assertEquals(1200.0, entidad.inventarioValor)
        assertEquals(40, entidad.inventarioUnidades)
        assertEquals("2026-07-10T23:00:00Z", entidad.calculadoEn)
    }

    @Test
    fun aDayWithoutActivityDefaultsEveryTotalToZero() {
        val payload = """{"id":"r-2","optica_id":"o-1","fecha":"2026-07-11"}"""

        val entidad = json.decodeFromString<ResumenDiarioRemoto>(payload).toEntity()

        assertEquals(0, entidad.ventasCantidad)
        assertEquals(0.0, entidad.ventasMontoTotal, 0.0)
        assertEquals(0.0, entidad.ventasCostoTotal, 0.0)
        assertEquals(0, entidad.cobrosCantidad)
        assertEquals(0.0, entidad.saldoPendienteTotal, 0.0)
        assertNull(entidad.inventarioValor)
        assertNull(entidad.inventarioUnidades)
        assertNull(entidad.calculadoEn)
    }
}
