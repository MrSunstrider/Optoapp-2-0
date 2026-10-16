package com.example.optoapp.domain

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class MarginFactsParserTest {

    @Test
    fun parse_fullFixture_mapsEveryField() {
        val facts = MarginFactsParser.parse(MarginFactsFixtures.fullResponse())

        assertEquals(750.0, facts.ventasMes, 0.001)
        assertEquals(333.0, facts.costoMes, 0.001)
        assertEquals(417.0, facts.margenBruto, 0.001)
        assertEquals(55.6, facts.margenBrutoPct, 0.001)
        assertEquals(28.93, facts.margenNetoPct, 0.001)

        assertEquals(2, facts.margenPorCategoria.size)
        assertEquals(
            MargenCategoria("Lentes Monofocales", 200.0, 140.0, 30.0, margen = 60.0),
            facts.margenPorCategoria[0],
        )

        assertEquals(
            CostosPorLinea(
                lentes = 130.0,
                montura = 80.0,
                lc = 0.0,
                biselado = 10.0,
                serviciosPropios = 40.0,
                serviciosTercerizados = 60.0,
                serviciosSinClasificar = 0.0,
                regalos = 13.0,
            ),
            facts.costosPorLinea,
        )

        assertEquals(MargenLinea(80.0, 40.0, 40.0, 50.0, 1), facts.margenServicios.propios)
        assertEquals(MargenLinea(100.0, 60.0, 40.0, 40.0, 1), facts.margenServicios.tercerizados)
        assertEquals(MargenLinea(20.0, 0.0, 20.0, 100.0, 1), facts.margenServicios.sinClasificar)

        assertEquals(CoberturaCosto(5, 4, 0, 1, 80.0, 0.0, 20.0), facts.cobertura)

        assertEquals(
            listOf(GastoCategoria("alquiler", 2000.0, "fijo"), GastoCategoria("marketing", 500.0, "variable")),
            facts.gastosPorCategoria,
        )
        assertEquals(GastosPorTipo(fijo = 2000.0, variable = 500.0), facts.gastosPorTipo)
        assertEquals(2000.0, facts.gastosFijos, 0.001)
        assertEquals(5000.0, facts.margenContribucion, 0.001)
        assertEquals(50.0, facts.margenContribucionPct, 0.001)
        assertEquals(4000.0, facts.puntoEquilibrio!!, 0.001)
        assertEquals(18.5, facts.margenNetoObjetivo, 0.001)
        assertEquals(10.43, facts.brechaMargenNetoPp, 0.001)

        assertEquals(
            listOf(
                PuntoSerieMensual(YearMonth.of(2026, 2), 0.0, 0.0, 0.0, 0.0, 0.0),
                PuntoSerieMensual(YearMonth.of(2026, 3), 1000.0, 700.0, 600.0, -300.0, -30.0),
                PuntoSerieMensual(YearMonth.of(2026, 7), 750.0, 333.0, 200.0, 217.0, 28.93),
            ),
            facts.serie6m,
        )
        assertEquals(FuenteDatos.SERVIDOR, facts.fuente)
        assertTrue(facts.detalleServidorDisponible)
    }

    @Test
    fun parse_legacyResponseWithoutNewKeys_usesDocumentedDefaults() {
        val facts = MarginFactsParser.parse(MarginFactsFixtures.legacyResponse())

        assertEquals(750.0, facts.ventasMes, 0.001)
        assertEquals(333.0, facts.costoMes, 0.001)
        assertEquals(0.0, facts.margenBruto, 0.001)
        assertEquals(0.0, facts.margenBrutoPct, 0.001)
        assertEquals(CostosPorLinea(), facts.costosPorLinea)
        assertEquals(MargenServicios(), facts.margenServicios)
        assertEquals(CoberturaCosto(), facts.cobertura)
        assertTrue(facts.cobertura.sinDatos)
        assertTrue(facts.gastosPorCategoria.isEmpty())
        assertEquals(GastosPorTipo(), facts.gastosPorTipo)
        assertEquals(0.0, facts.gastosFijos, 0.001)
        assertEquals(0.0, facts.margenContribucion, 0.001)
        assertEquals(0.0, facts.margenContribucionPct, 0.001)
        assertNull(facts.puntoEquilibrio)
        assertEquals(15.0, facts.margenNetoObjetivo, 0.001)
        assertEquals(0.0, facts.brechaMargenNetoPp, 0.001)
        assertTrue(facts.serie6m.isEmpty())
    }

    @Test
    fun parse_emptyObject_doesNotThrowAndKeepsDefaults() {
        val facts = MarginFactsParser.parse(JsonObject(emptyMap()))

        assertEquals(MarginFacts(), facts)
    }

    @Test
    fun parse_nullValues_fallBackToDefaults() {
        val json = buildJsonObject {
            put("costo_mes", JsonNull)
            put("margen_bruto", JsonNull)
            put("costos_por_linea", JsonNull)
            put("margen_servicios", JsonNull)
            put("cobertura", JsonNull)
            put("gastos_por_categoria", JsonNull)
            put("gastos_por_tipo", JsonNull)
            put("margen_neto_objetivo", JsonNull)
            put("serie_6m", JsonNull)
        }

        val facts = MarginFactsParser.parse(json)

        assertEquals(MarginFacts(), facts)
    }

    @Test
    fun parse_emptyArrays_yieldEmptyLists() {
        val json = buildJsonObject {
            putJsonArray("margen_por_categoria") {}
            putJsonArray("gastos_por_categoria") {}
            putJsonArray("serie_6m") {}
        }

        val facts = MarginFactsParser.parse(json)

        assertTrue(facts.margenPorCategoria.isEmpty())
        assertTrue(facts.gastosPorCategoria.isEmpty())
        assertTrue(facts.serie6m.isEmpty())
    }

    @Test
    fun parse_unknownKey_isIgnored() {
        val json = JsonObject(
            MarginFactsFixtures.legacyResponse() + buildJsonObject { put("futuro_desconocido", "x") },
        )

        val facts = MarginFactsParser.parse(json)

        assertEquals(MarginFactsParser.parse(MarginFactsFixtures.legacyResponse()), facts)
    }

    @Test
    fun parse_nullablePuntoEquilibrioAndCategoryMargin_stayNull() {
        val json = JsonObject(
            MarginFactsFixtures.fullResponse() + buildJsonObject { put("punto_equilibrio", JsonNull) },
        )

        val facts = MarginFactsParser.parse(json)

        assertNull(facts.puntoEquilibrio)
        assertNull(facts.margenPorCategoria[1].margenPct)
    }

    @Test
    fun parse_servicioBucketWithNullMargenPct_staysNull() {
        val json = JsonObject(
            MarginFactsFixtures.fullResponse() + buildJsonObject {
                put(
                    "margen_servicios",
                    buildJsonObject {
                        put(
                            "propios",
                            buildJsonObject {
                                put("ventas", 0.0)
                                put("costos", 0.0)
                                put("margen", 0.0)
                                put("margen_pct", JsonNull)
                            },
                        )
                    },
                )
            },
        )

        val facts = MarginFactsParser.parse(json)

        assertNull(facts.margenServicios.propios.margenPct)
        assertEquals(0, facts.margenServicios.propios.cantidad)
        assertEquals(MargenLinea(), facts.margenServicios.tercerizados)
    }

    @Test
    fun parse_malformedSerieMes_dropsOnlyThoseEntries() {
        val json = JsonObject(
            MarginFactsFixtures.legacyResponse() + buildJsonObject {
                putJsonArray("serie_6m") {
                    add(MarginFactsFixtures.serieEntry("2026-06", 10.0, 4.0, 1.0, 5.0, 50.0))
                    add(MarginFactsFixtures.serieEntry("junio", 1.0, 1.0, 1.0, 1.0, 1.0))
                    add(MarginFactsFixtures.serieEntry("2026-13", 1.0, 1.0, 1.0, 1.0, 1.0))
                    add(buildJsonObject { put("ventas", 9.0) })
                    add(MarginFactsFixtures.serieEntry("2026-07", 20.0, 8.0, 2.0, 10.0, 50.0))
                }
            },
        )

        val facts = MarginFactsParser.parse(json)

        assertEquals(listOf(YearMonth.of(2026, 6), YearMonth.of(2026, 7)), facts.serie6m.map { it.mes })
    }

    @Test
    fun parse_negativeSerieMargin_isNotClamped() {
        val facts = MarginFactsParser.parse(MarginFactsFixtures.fullResponse())

        val march = facts.serie6m.first { it.mes == YearMonth.of(2026, 3) }
        assertEquals(-300.0, march.margen, 0.001)
        assertEquals(-30.0, march.margenNetoPct, 0.001)
    }

    @Test
    fun parse_gastoCategoryWithoutPredominantType_defaultsToFijo() {
        val json = JsonObject(
            MarginFactsFixtures.legacyResponse() + buildJsonObject {
                putJsonArray("gastos_por_categoria") {
                    add(
                        buildJsonObject {
                            put("categoria", "otro")
                            put("monto", 10.0)
                        },
                    )
                }
            },
        )

        val facts = MarginFactsParser.parse(json)

        assertEquals("fijo", facts.gastosPorCategoria.single().tipoPredominante)
    }

    @Test
    fun parse_categoryWithoutMargenKey_derivesItFromVentasAndCostos() {
        val json = JsonObject(
            MarginFactsFixtures.legacyResponse() + buildJsonObject {
                putJsonArray("margen_por_categoria") {
                    add(
                        buildJsonObject {
                            put("categoria", "Lentes")
                            put("ventas", 100.0)
                            put("costos", 30.0)
                        },
                    )
                }
            },
        )

        val row = MarginFactsParser.parse(json).margenPorCategoria.single()

        assertEquals(70.0, row.margen, 0.001)
        assertNull(row.margenPct)
    }

    @Test
    fun defaultFacts_areFromServer() {
        assertFalse(MarginFacts(fuente = FuenteDatos.OFFLINE).detalleServidorDisponible)
        assertTrue(MarginFacts().detalleServidorDisponible)
    }
}
