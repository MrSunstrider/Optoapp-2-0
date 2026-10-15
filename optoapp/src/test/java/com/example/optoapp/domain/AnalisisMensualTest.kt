package com.example.optoapp.domain

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalisisMensualTest {

    @Test
    fun fromJson_costoMes_isReadAndDrivesCostoDeVentasEvenWhenCategoryCostsAreZero() {
        val json = JsonObject(
            MarginFactsFixtures.legacyResponse() + buildJsonObject {
                put("costo_mes", 4000.0)
                putJsonArray("margen_por_categoria") {
                    add(
                        buildJsonObject {
                            put("categoria", "Lentes")
                            put("ventas", 8000.0)
                            put("costos", 0.0)
                            put("margen_pct", 100.0)
                        },
                    )
                }
            },
        )

        val result = AnalisisMensual.fromJson(json)

        assertEquals(4000.0, result.costoMes, 0.001)
        assertEquals(4000.0, result.costoDeVentas(), 0.001)
    }

    @Test
    fun fromJson_missingCostoMes_defaultsToZeroWithoutCrash() {
        val json = JsonObject(MarginFactsFixtures.legacyResponse() - "costo_mes")

        val result = AnalisisMensual.fromJson(json)

        assertEquals(0.0, result.costoMes, 0.001)
        assertEquals(0.0, result.costoDeVentas(), 0.001)
    }

    @Test
    fun fromJson_exposesMarginFactsFromTheSameResponse() {
        val result = AnalisisMensual.fromJson(MarginFactsFixtures.fullResponse())

        assertEquals(417.0, result.marginFacts.margenBruto, 0.001)
        assertEquals(333.0, result.marginFacts.costosPorLinea.total(), 0.001)
        assertEquals(result.margenPorCategoria, result.marginFacts.margenPorCategoria)
        assertEquals(FuenteDatos.SERVIDOR, result.marginFacts.fuente)
    }

    @Test
    fun fromJson_legacyResponse_keepsExistingFieldsAndDefaultsFacts() {
        val result = AnalisisMensual.fromJson(MarginFactsFixtures.legacyResponse())

        assertEquals(750.0, result.ventasMes, 0.001)
        assertEquals(2, result.deudores.cantidad)
        assertEquals(3, result.proyeccionCaja!!.mesesHistoricos)
        assertTrue(result.marginFacts.cobertura.sinDatos)
        assertNull(result.marginFacts.puntoEquilibrio)
    }

    @Test
    fun fromJson_nullNestedSections_degradeToDefaultsWithoutCrash() {
        val json = JsonObject(
            MarginFactsFixtures.legacyResponse() + buildJsonObject {
                put("deudores", JsonNull)
                put("proyeccion_caja", JsonNull)
                put("meses_historicos", JsonNull)
                put("stock_estancado", JsonNull)
            },
        )

        val result = AnalisisMensual.fromJson(json)

        assertEquals(DeudoresResumen(0, 0.0), result.deudores)
        assertNull(result.proyeccionCaja)
        assertTrue(result.stockEstancado.isEmpty())
        assertEquals(750.0, result.ventasMes, 0.001)
    }

    @Test
    fun fromJson_stockEstancadoRows_skipNonObjectsAndKeepNullUltimaVenta() {
        val json = JsonObject(
            MarginFactsFixtures.legacyResponse() + buildJsonObject {
                putJsonArray("stock_estancado") {
                    add(
                        buildJsonObject {
                            put("montura_id", "m-1")
                            put("sku", "RB-01")
                            put("modelo", "Aviator")
                            put("costo", 80.0)
                            put("stock_actual", 3)
                            put("ultima_venta", JsonNull)
                            put("dias_sin_venta", 120)
                        },
                    )
                    add(JsonNull)
                }
            },
        )

        val result = AnalisisMensual.fromJson(json)

        assertEquals(
            listOf(StockEstancadoItem("m-1", "RB-01", "Aviator", 80.0, 3, null, 120)),
            result.stockEstancado,
        )
    }

    @Test
    fun fromJson_unknownKey_isIgnored() {
        val json = JsonObject(
            MarginFactsFixtures.legacyResponse() + buildJsonObject { put("futuro_desconocido", 1) },
        )

        val result = AnalisisMensual.fromJson(json)

        assertEquals(750.0, result.ventasMes, 0.001)
    }
}
