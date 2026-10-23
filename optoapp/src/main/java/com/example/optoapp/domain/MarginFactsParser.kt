package com.example.optoapp.domain

import kotlinx.serialization.json.JsonObject
import java.time.YearMonth
import java.time.format.DateTimeParseException

object MarginFactsParser {

    // The server breaks ties toward fijo, so an absent type resolves the same way.
    private const val TIPO_GASTO_POR_DEFECTO = "fijo"

    fun parse(obj: JsonObject): MarginFacts = MarginFacts(
        ventasMes = obj.optDouble("ventas_mes"),
        costoMes = obj.optDouble("costo_mes"),
        margenBruto = obj.optDouble("margen_bruto"),
        margenBrutoPct = obj.optDouble("margen_bruto_pct"),
        margenPorCategoria = obj.parseMargenPorCategoria(),
        costosPorLinea = obj.optObject("costos_por_linea")?.parseCostosPorLinea() ?: CostosPorLinea(),
        margenServicios = obj.optObject("margen_servicios")?.parseMargenServicios() ?: MargenServicios(),
        cobertura = obj.optObject("cobertura")?.parseCobertura() ?: CoberturaCosto(),
        gastosPorCategoria = obj.parseGastosPorCategoria(),
        gastosPorTipo = obj.optObject("gastos_por_tipo")?.let {
            GastosPorTipo(fijo = it.optDouble("fijo"), variable = it.optDouble("variable"))
        } ?: GastosPorTipo(),
        gastosFijos = obj.optDouble("gastos_fijos"),
        margenContribucion = obj.optDouble("margen_contribucion"),
        margenContribucionPct = obj.optDouble("margen_contribucion_pct"),
        puntoEquilibrio = obj.optDoubleNullable("punto_equilibrio"),
        margenNetoPct = obj.optDouble("margen_neto_pct"),
        margenNetoObjetivo = obj.optDoubleNullable("margen_neto_objetivo")
            ?: MarginFacts.DEFAULT_MARGEN_NETO_OBJETIVO,
        brechaMargenNetoPp = obj.optDouble("brecha_margen_neto_pp"),
        serie6m = obj.parseSerie6m(),
        fuente = FuenteDatos.SERVIDOR,
    )

    private fun JsonObject.parseMargenPorCategoria(
        key: String = "margen_por_categoria",
    ): List<MargenCategoria> =
        optArray(key).orEmpty().mapNotNull { item ->
            val row = item as? JsonObject ?: return@mapNotNull null
            val ventas = row.optDouble("ventas")
            val costos = row.optDouble("costos")
            MargenCategoria(
                categoria = row.optString("categoria"),
                ventas = ventas,
                costos = costos,
                margenPct = row.optDoubleNullable("margen_pct"),
                margen = row.optDoubleNullable("margen") ?: (ventas - costos),
            )
        }

    fun parseMargenPorCategoriaMesAnterior(obj: JsonObject): List<MargenCategoria> =
        obj.parseMargenPorCategoria("margen_por_categoria_mes_anterior")

    private fun JsonObject.parseCostosPorLinea() = CostosPorLinea(
        lentes = optDouble("lentes"),
        montura = optDouble("montura"),
        lc = optDouble("lc"),
        biselado = optDouble("biselado"),
        serviciosPropios = optDouble("servicios_propios"),
        serviciosTercerizados = optDouble("servicios_tercerizados"),
        serviciosSinClasificar = optDouble("servicios_sin_clasificar"),
        regalos = optDouble("regalos"),
    )

    private fun JsonObject.parseMargenServicios() = MargenServicios(
        propios = optObject("propios")?.parseMargenLinea() ?: MargenLinea(),
        tercerizados = optObject("tercerizados")?.parseMargenLinea() ?: MargenLinea(),
        sinClasificar = optObject("sin_clasificar")?.parseMargenLinea() ?: MargenLinea(),
    )

    private fun JsonObject.parseMargenLinea() = MargenLinea(
        ventas = optDouble("ventas"),
        costos = optDouble("costos"),
        margen = optDouble("margen"),
        margenPct = optDoubleNullable("margen_pct"),
        cantidad = optInt("cantidad"),
    )

    private fun JsonObject.parseCobertura() = CoberturaCosto(
        lineasTotal = optInt("lineas_total"),
        lineasReales = optInt("lineas_reales"),
        lineasEstimadas = optInt("lineas_estimadas"),
        lineasFaltantes = optInt("lineas_faltantes"),
        pctReal = optDouble("pct_real"),
        pctEstimado = optDouble("pct_estimado"),
        pctFaltante = optDouble("pct_faltante"),
    )

    private fun JsonObject.parseGastosPorCategoria(): List<GastoCategoria> =
        optArray("gastos_por_categoria").orEmpty().mapNotNull { item ->
            val row = item as? JsonObject ?: return@mapNotNull null
            GastoCategoria(
                categoria = row.optString("categoria"),
                monto = row.optDouble("monto"),
                tipoPredominante = row.optString("tipo_predominante").ifEmpty { TIPO_GASTO_POR_DEFECTO },
            )
        }

    private fun JsonObject.parseSerie6m(): List<PuntoSerieMensual> =
        optArray("serie_6m").orEmpty().mapNotNull { item ->
            val row = item as? JsonObject ?: return@mapNotNull null
            val mes = parseYearMonth(row.optString("mes")) ?: return@mapNotNull null
            PuntoSerieMensual(
                mes = mes,
                ventas = row.optDouble("ventas"),
                costo = row.optDouble("costo"),
                gastos = row.optDouble("gastos"),
                margen = row.optDouble("margen"),
                margenNetoPct = row.optDouble("margen_neto_pct"),
            )
        }

    private fun parseYearMonth(raw: String): YearMonth? = try {
        YearMonth.parse(raw)
    } catch (_: DateTimeParseException) {
        null
    }
}
