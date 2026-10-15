package com.example.optoapp.domain

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Mirrors the month asserted by supabase/tests/test_margen_real_costos.sql
 * (LIN1/COV1): costs by line sum to costo_mes, so both sides share one truth.
 */
object MarginFactsFixtures {

    /** The 16 keys that rpc_analisis_mensual returned before MarginFacts existed. */
    fun legacyResponse(): JsonObject = buildJsonObject {
        put("ventas_mes", 750.0)
        put("cobros_mes", 600.0)
        put("costo_mes", 333.0)
        put("gastos_mes", 200.0)
        put("saldo_pendiente", 150.0)
        put("margen_neto_pct", 28.93)
        put("ticket_promedio", 125.0)
        put("cantidad_ventas", 6)
        put("ventas_mes_anterior", 500.0)
        put("variacion_ventas_pct", 50.0)
        put("meses_historicos", 3)
        putJsonArray("margen_por_categoria") {
            add(
                buildJsonObject {
                    put("categoria", "Lentes Monofocales")
                    put("ventas", 200.0)
                    put("costos", 140.0)
                    put("margen", 60.0)
                    put("margen_pct", 30.0)
                },
            )
            add(
                buildJsonObject {
                    put("categoria", "Monturas Premium")
                    put("ventas", 0.0)
                    put("costos", 0.0)
                    put("margen", 0.0)
                    put("margen_pct", JsonNull)
                },
            )
        }
        putJsonObject("deudores") {
            put("cantidad", 2)
            put("saldo_total", 150.0)
        }
        putJsonObject("proyeccion_caja") {
            put("ingresos_esperados", 1000.0)
            put("egresos_programados", 400.0)
            put("saldo_neto", 600.0)
        }
        putJsonArray("stock_estancado") {}
        put("valor_inventario", 9000.0)
    }

    fun fullResponse(): JsonObject = JsonObject(
        legacyResponse() + buildJsonObject {
            put("margen_bruto", 417.0)
            put("margen_bruto_pct", 55.6)
            putJsonObject("costos_por_linea") {
                put("lentes", 130.0)
                put("montura", 80.0)
                put("lc", 0.0)
                put("biselado", 10.0)
                put("servicios_propios", 40.0)
                put("servicios_tercerizados", 60.0)
                put("servicios_sin_clasificar", 0.0)
                put("regalos", 13.0)
            }
            putJsonObject("margen_servicios") {
                putJsonObject("propios") {
                    put("ventas", 80.0)
                    put("costos", 40.0)
                    put("margen", 40.0)
                    put("margen_pct", 50.0)
                    put("cantidad", 1)
                }
                putJsonObject("tercerizados") {
                    put("ventas", 100.0)
                    put("costos", 60.0)
                    put("margen", 40.0)
                    put("margen_pct", 40.0)
                    put("cantidad", 1)
                }
                putJsonObject("sin_clasificar") {
                    put("ventas", 20.0)
                    put("costos", 0.0)
                    put("margen", 20.0)
                    put("margen_pct", 100.0)
                    put("cantidad", 1)
                }
            }
            putJsonObject("cobertura") {
                put("lineas_total", 5)
                put("lineas_reales", 4)
                put("lineas_estimadas", 0)
                put("lineas_faltantes", 1)
                put("pct_real", 80.0)
                put("pct_estimado", 0.0)
                put("pct_faltante", 20.0)
            }
            putJsonArray("gastos_por_categoria") {
                add(
                    buildJsonObject {
                        put("categoria", "alquiler")
                        put("monto", 2000.0)
                        put("tipo_predominante", "fijo")
                    },
                )
                add(
                    buildJsonObject {
                        put("categoria", "marketing")
                        put("monto", 500.0)
                        put("tipo_predominante", "variable")
                    },
                )
            }
            putJsonObject("gastos_por_tipo") {
                put("fijo", 2000.0)
                put("variable", 500.0)
            }
            put("gastos_fijos", 2000.0)
            put("margen_contribucion", 5000.0)
            put("margen_contribucion_pct", 50.0)
            put("punto_equilibrio", 4000.0)
            put("margen_neto_objetivo", 18.5)
            put("brecha_margen_neto_pp", 10.43)
            putJsonArray("serie_6m") {
                add(serieEntry("2026-02", 0.0, 0.0, 0.0, 0.0, 0.0))
                add(serieEntry("2026-03", 1000.0, 700.0, 600.0, -300.0, -30.0))
                add(serieEntry("2026-07", 750.0, 333.0, 200.0, 217.0, 28.93))
            }
        },
    )

    fun serieEntry(
        mes: String,
        ventas: Double,
        costo: Double,
        gastos: Double,
        margen: Double,
        margenNetoPct: Double,
    ): JsonObject = buildJsonObject {
        put("mes", mes)
        put("ventas", ventas)
        put("costo", costo)
        put("gastos", gastos)
        put("margen", margen)
        put("margen_neto_pct", margenNetoPct)
    }
}
