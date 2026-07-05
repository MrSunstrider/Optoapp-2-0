package com.example.optoapp.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalisisMensualMapperTest {

    @Test
    fun fromJson_fullResponse_mapsAllFields() {
        val json = buildJsonObject {
            put("ventas_mes", 15000.0)
            put("cobros_mes", 12000.0)
            put("margen_neto_pct", 25.5)
            putJsonArray("margen_por_categoria") {
                add(buildJsonObject {
                    put("categoria", "Lentes")
                    put("ventas", 8000.0)
                    put("costos", 4000.0)
                    put("margen_pct", 50.0)
                })
            }
            putJsonObject("deudores") {
                put("cantidad", 5)
                put("saldo_total", 3000.0)
            }
            putJsonObject("proyeccion_caja") {
                put("ingresos_esperados", 5000.0)
                put("egresos_programados", 2000.0)
                put("saldo_neto", 3000.0)
            }
            putJsonArray("stock_estancado") {
                add(buildJsonObject {
                    put("montura_id", "m1")
                    put("sku", "SKU-001")
                    put("modelo", "Modelo A")
                    put("costo", 150.0)
                    put("stock_actual", 5)
                    put("ultima_venta", "2026-01-15")
                    put("dias_sin_venta", 171)
                })
            }
            put("valor_inventario", 45000.0)
            put("ventas_mes_anterior", 12000.0)
            put("variacion_ventas_pct", 25.0)
        }

        val result = AnalisisMensual.fromJson(json)

        assertEquals(15000.0, result.ventasMes, 0.001)
        assertEquals(12000.0, result.cobrosMes, 0.001)
        assertEquals(25.5, result.margenNetoPct, 0.001)
        assertEquals(1, result.margenPorCategoria.size)
        assertEquals("Lentes", result.margenPorCategoria[0].categoria)
        assertEquals(5, result.deudores.cantidad)
        assertEquals(3000.0, result.deudores.saldoTotal, 0.001)
        assertEquals(5000.0, result.proyeccionCaja!!.ingresosEsperados, 0.001)
        assertEquals(1, result.stockEstancado.size)
        assertEquals("m1", result.stockEstancado[0].monturaId)
        assertEquals(45000.0, result.valorInventario, 0.001)
        assertEquals(12000.0, result.ventasMesAnterior, 0.001)
        assertEquals(25.0, result.variacionVentasPct!!, 0.001)
        assertEquals(false, result.esOffline)
    }

    @Test
    fun fromJson_emptyMonth_allZeros() {
        val result = AnalisisMensual.fromJson(buildJsonObject {
            put("ventas_mes", 0.0)
            put("cobros_mes", 0.0)
            put("margen_neto_pct", 0.0)
            putJsonArray("margen_por_categoria") {}
            putJsonObject("deudores") {
                put("cantidad", 0)
                put("saldo_total", 0.0)
            }
            putJsonArray("stock_estancado") {}
            put("valor_inventario", 0.0)
            put("ventas_mes_anterior", 0.0)
        })

        assertEquals(0.0, result.ventasMes, 0.001)
        assertEquals(0, result.deudores.cantidad)
        assertNull(result.proyeccionCaja)
        assertTrue(result.margenPorCategoria.isEmpty())
        assertTrue(result.stockEstancado.isEmpty())
        assertNull(result.variacionVentasPct)
    }

    @Test
    fun fromJson_missingKeys_defaultsToZero() {
        // Minimal JSON with only required top-level keys missing
        val json = buildJsonObject {
            put("ventas_mes", 5000.0)
            // cobros_mes missing
            // margen_neto_pct missing
            putJsonArray("margen_por_categoria") {}
            putJsonObject("deudores") {
                put("cantidad", 0)
                put("saldo_total", 0.0)
            }
            putJsonArray("stock_estancado") {}
            put("valor_inventario", 0.0)
            put("ventas_mes_anterior", 0.0)
            // proyeccion_caja missing entirely
        }

        val result = AnalisisMensual.fromJson(json)

        assertEquals(5000.0, result.ventasMes, 0.001)
        assertEquals(0.0, result.cobrosMes, 0.001)
        assertEquals(0.0, result.margenNetoPct, 0.001)
        assertNull(result.proyeccionCaja)
    }

    // F8-GASTOS-MES RED tests

    @Test
    fun fromJson_withGastosMes_parsesCorrectly() {
        val json = buildJsonObject {
            put("ventas_mes", 15000.0)
            put("cobros_mes", 12000.0)
            put("margen_neto_pct", 25.5)
            putJsonArray("margen_por_categoria") {}
            putJsonObject("deudores") {
                put("cantidad", 0)
                put("saldo_total", 0.0)
            }
            putJsonArray("stock_estancado") {}
            put("valor_inventario", 45000.0)
            put("ventas_mes_anterior", 12000.0)
            put("variacion_ventas_pct", null)
            put("gastos_mes", 3900.0)
        }

        val result = AnalisisMensual.fromJson(json)

        assertEquals(3900.0, result.gastosMes, 0.001)
    }

    @Test
    fun fromJson_withoutGastosMes_defaultsToZero() {
        val json = buildJsonObject {
            put("ventas_mes", 15000.0)
            put("cobros_mes", 12000.0)
            put("margen_neto_pct", 25.5)
            putJsonArray("margen_por_categoria") {}
            putJsonObject("deudores") {
                put("cantidad", 0)
                put("saldo_total", 0.0)
            }
            putJsonArray("stock_estancado") {}
            put("valor_inventario", 45000.0)
            put("ventas_mes_anterior", 12000.0)
            put("variacion_ventas_pct", null)
        }

        val result = AnalisisMensual.fromJson(json)

        assertEquals(0.0, result.gastosMes, 0.001)
    }
}
