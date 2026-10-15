package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.data.regaloservicio.RegaloServicioExtraEntity
import com.example.optoapp.data.servicio.ServicioExtraItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CostoVentaCalculatorTest {

    private fun item(
        od: Double? = null,
        oi: Double? = null,
        montura: Double? = null,
        biselado: Double? = null,
        lc: Double? = null,
        origen: String? = null,
    ) = DispensacionItem(
        id = "i-${od}-${montura}-$origen",
        dispensacionId = "d1",
        costoRealOd = od,
        costoRealOi = oi,
        costoRealMontura = montura,
        costoRealBiselado = biselado,
        costoRealLc = lc,
        costoOrigen = origen,
    )

    private fun regaloDisp(cantidad: Int, costo: Double) = RegaloDispensacionEntity(
        id = "r-$cantidad-$costo",
        dispensacionId = "d1",
        productoId = "p1",
        cantidad = cantidad,
        costoUnitario = costo,
        descripcion = "Estuche",
        opticaId = "o1",
    )

    private fun servicio(realizadoPor: String? = "propio", costoReal: Double = 0.0, servicioTipoId: String? = null) = ServicioExtra(
        id = "s1",
        descripcion = "Reparacion",
        montoTotal = 100.0,
        estado = "Entregado",
        fecha = LocalDate.of(2026, 7, 1),
        realizadoPor = realizadoPor,
        costoReal = costoReal,
        servicioTipoId = servicioTipoId,
    )

    @Test
    fun dispensacion_sumsEveryItemCostComponentPlusGifts() {
        val costo = CostoVentaCalculator.dispensacion(
            items = listOf(item(od = 30.0, oi = 30.0, montura = 50.0, biselado = 10.0, origen = "matriz"), item(lc = 20.0, origen = "manual")),
            costoUnitarioSnapshot = null,
            regalos = listOf(regaloDisp(2, 5.0)),
        )

        assertEquals(150.0, costo.costo, 0.001)
        assertFalse(costo.costoIncompleto)
    }

    @Test
    fun dispensacion_withoutItems_usesSnapshot() {
        val costo = CostoVentaCalculator.dispensacion(items = emptyList(), costoUnitarioSnapshot = 80.0, regalos = emptyList())

        assertEquals(80.0, costo.costo, 0.001)
        assertFalse(costo.costoIncompleto)
    }

    @Test
    fun dispensacion_legacyItemWithCost_isEstimatedNotIncomplete() {
        val costo = CostoVentaCalculator.dispensacion(listOf(item(od = 70.0, oi = 70.0)), null, emptyList())

        assertEquals(140.0, costo.costo, 0.001)
        assertFalse(costo.costoIncompleto)
    }

    @Test
    fun dispensacion_faltanteItem_flagsIncomplete() {
        val costo = CostoVentaCalculator.dispensacion(listOf(item(od = 70.0, origen = "faltante")), null, emptyList())

        assertTrue(costo.costoIncompleto)
    }

    @Test
    fun dispensacion_legacyZeroTotal_flagsIncomplete() {
        assertTrue(CostoVentaCalculator.dispensacion(emptyList(), null, emptyList()).costoIncompleto)
        assertTrue(CostoVentaCalculator.dispensacion(listOf(item()), null, emptyList()).costoIncompleto)
    }

    @Test
    fun servicio_sumsCostoRealItemsAndGifts() {
        val costo = CostoVentaCalculator.servicio(
            servicio = servicio(costoReal = 60.0),
            items = listOf(ServicioExtraItem(id = "si1", servicioExtraId = "s1", opticaId = "o1", costoReal = 25.0)),
            regalos = listOf(
                RegaloServicioExtraEntity(
                    id = "rs1",
                    servicioExtraId = "s1",
                    productoId = "p1",
                    cantidad = 3,
                    costoUnitario = 2.0,
                    descripcion = "Pano",
                    opticaId = "o1",
                ),
            ),
        )

        assertEquals(91.0, costo.costo, 0.001)
        assertFalse(costo.costoIncompleto)
    }

    @Test
    fun servicio_withoutExecutorAndZeroCost_flagsIncomplete() {
        val costo = CostoVentaCalculator.servicio(servicio(realizadoPor = null, costoReal = 0.0), emptyList(), emptyList())

        assertEquals(0.0, costo.costo, 0.001)
        assertTrue(costo.costoIncompleto)
    }
}
