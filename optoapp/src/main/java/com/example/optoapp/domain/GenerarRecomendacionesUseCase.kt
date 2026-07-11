package com.example.optoapp.domain

import com.example.optoapp.data.Resource
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import java.time.LocalDate
import javax.inject.Inject

open class GenerarRecomendacionesUseCase @Inject constructor(
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao
) {
    companion object {
        private const val MARGEN_BAJO_PCT = 10.0
        private const val MARGEN_ALTO_PCT = 35.0
        private const val CONTRIBUCION_ALTA_PCT = 0.25
        private const val GASTOS_VENTAS_RATIO_ALERTA = 0.4
    }

    suspend operator fun invoke(
        analisis: AnalisisMensual?,
        deudores: List<Deudor>,
        opticaId: String
    ): Resource<List<Recomendacion>> {
        if (analisis == null && deudores.isEmpty()) {
            return Resource.Error("Datos insuficientes para generar recomendaciones")
        }

        val config = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
            ?: return Resource.Error("Configuracion financiera no encontrada")

        val cobrar = evaluarCobrar(deudores, config)

        val recommendations = if (analisis == null) {
            listOfNotNull(cobrar)
        } else {
            listOfNotNull(
                cobrar,
                evaluarMejorarPrecio(analisis.margenPorCategoria, config),
                evaluarLiquidarStock(analisis.stockEstancado, config),
                evaluarVenderMasDe(analisis.margenPorCategoria, config),
                evaluarAlertaCaida(analisis, config),
                evaluarReducirGasto(analisis)
            )
        }

        val sorted = recommendations.sortedWith(
            compareBy<Recomendacion> { it.prioridad.ordinal }
                .thenBy { it.tipo.ordinal }
        )

        return Resource.Success(sorted.take(5))
    }

    // ── R1: COBRAR (ALTA) ──────────────────────────────────────────────────

    private fun evaluarCobrar(
        deudores: List<Deudor>,
        config: ConfiguracionFinancieraEntity
    ): Recomendacion? {
        if (deudores.isEmpty()) return null

        val deudaTotal = deudores.sumOf { it.saldo }
        val tieneVieja = deudores.any { it.diasDeuda > config.deudaViejaAlertaDias }

        if (deudaTotal <= config.deudaTotalAlertaMonto && !tieneVieja) return null

        val top3 = deudores.sortedByDescending { it.saldo }.take(3)
        val nombres = top3.joinToString(", ") { it.pacienteNombre }
        val totalStr = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US)
            .format(deudaTotal)

        val titulo = "Cobranza pendiente"
        return Recomendacion(
            id = "${RecomendacionTipo.COBRAR.name}::$titulo",
            tipo = RecomendacionTipo.COBRAR,
            titulo = titulo,
            detalle = "Deuda total de S/ $totalStr en ${deudores.size} deudores. " +
                "Principales: $nombres.",
            impactoEstimado = "S/ $totalStr si se cobra todo",
            prioridad = Prioridad.ALTA,
            accion = "Contactar a los deudores para coordinar pagos pendientes",
            datosAccion = DatosAccion(
                pacienteIds = top3.map { it.pacienteId },
                montoTotal = top3.sumOf { it.saldo }
            )
        )
    }

    // ── R2: MEJORAR_PRECIO (ALTA) ──────────────────────────────────────────

    private fun evaluarMejorarPrecio(
        categorias: List<MargenCategoria>,
        config: ConfiguracionFinancieraEntity
    ): Recomendacion? {
        val minVentas = config.minVentasParaRecomendar.toDouble()
        val target = categorias.firstOrNull { cat ->
            (cat.margenPct ?: Double.MAX_VALUE) < MARGEN_BAJO_PCT &&
                cat.ventas >= minVentas
        } ?: return null

        val margenPctStr = String.format("%.1f", target.margenPct ?: 0.0)
        val titulo = "Mejorar precio de ${target.categoria}"
        return Recomendacion(
            id = "${RecomendacionTipo.MEJORAR_PRECIO.name}::$titulo",
            tipo = RecomendacionTipo.MEJORAR_PRECIO,
            titulo = titulo,
            detalle = "La categoria ${target.categoria} tiene un margen bajo " +
                "del ${margenPctStr}%. Considera ajustar precios para alcanzar " +
                "al menos un 25% de margen.",
            impactoEstimado = null,
            prioridad = Prioridad.ALTA,
            accion = "Revisar precios de ${target.categoria} y ajustar al alza",
            datosAccion = null
        )
    }

    // ── R3: LIQUIDAR_STOCK (MEDIA) ─────────────────────────────────────────

    private fun evaluarLiquidarStock(
        stockEstancado: List<StockEstancadoItem>,
        config: ConfiguracionFinancieraEntity
    ): Recomendacion? {
        val estancados = stockEstancado.filter {
            it.diasSinVenta > config.stockEstancadoAlertaDias
        }
        if (estancados.isEmpty()) return null

        val modelos = estancados.joinToString(", ") { it.modelo }
        val costoTotal = estancados.sumOf { it.costo * it.stockActual }

        val titulo = "Liquidar stock estancado"
        return Recomendacion(
            id = "${RecomendacionTipo.LIQUIDAR_STOCK.name}::$titulo",
            tipo = RecomendacionTipo.LIQUIDAR_STOCK,
            titulo = titulo,
            detalle = "${estancados.size} items llevan mas de ${config.stockEstancadoAlertaDias} " +
                "dias sin venderse: $modelos. " +
                "Costo total en inventario: S/ ${String.format("%.0f", costoTotal)}.",
            impactoEstimado = null,
            prioridad = Prioridad.MEDIA,
            accion = "Ofrecer descuentos del 20-30% para liberar capital estancado",
            datosAccion = DatosAccion(
                productoIds = estancados.map { it.monturaId }
            )
        )
    }

    // ── R4: VENDER_MAS_DE (MEDIA) ──────────────────────────────────────────

    private fun evaluarVenderMasDe(
        categorias: List<MargenCategoria>,
        config: ConfiguracionFinancieraEntity
    ): Recomendacion? {
        val minVentas = config.minVentasParaRecomendar.toDouble()
        val gananciaTotal = categorias.sumOf { it.ventas - it.costos }
        if (gananciaTotal <= 0.0) return null

        val target = categorias.firstOrNull { cat ->
            val margenPct = cat.margenPct ?: return@firstOrNull false
            val margenBruto = cat.ventas - cat.costos
            val contribucion = margenBruto / gananciaTotal
            margenPct > MARGEN_ALTO_PCT &&
                contribucion > CONTRIBUCION_ALTA_PCT &&
                cat.ventas >= minVentas
        } ?: return null

        val margenPctStr = String.format("%.0f", target.margenPct ?: 0.0)
        val titulo = "Vender mas de ${target.categoria}"
        return Recomendacion(
            id = "${RecomendacionTipo.VENDER_MAS_DE.name}::$titulo",
            tipo = RecomendacionTipo.VENDER_MAS_DE,
            titulo = titulo,
            detalle = "${target.categoria} tiene un margen alto del ${margenPctStr}% " +
                "y contribuye significativamente a la ganancia total. " +
                "Aumenta su promocion y visibilidad.",
            impactoEstimado = null,
            prioridad = Prioridad.MEDIA,
            accion = "Crear promocion destacada para ${target.categoria}",
            datosAccion = null
        )
    }

    // ── R5: ALERTA_CAIDA (ALTA) ────────────────────────────────────────────

    private fun evaluarAlertaCaida(
        analisis: AnalisisMensual,
        config: ConfiguracionFinancieraEntity
    ): Recomendacion? {
        val hoy = LocalDate.now()
        val diasTranscurridos = hoy.dayOfMonth
        val diasMesAnterior = hoy.minusMonths(1).lengthOfMonth()

        val variacion: Double
        val ventasReferencia: Double
        val esProporcional: Boolean

        if (diasTranscurridos > 0 && diasMesAnterior > 0 && analisis.ventasMesAnterior > 0.0) {
            val proporcion = diasTranscurridos.toDouble() / diasMesAnterior.toDouble()
            ventasReferencia = analisis.ventasMesAnterior * proporcion
            variacion = ((analisis.ventasMes - ventasReferencia) / ventasReferencia) * 100.0
            esProporcional = true
        } else {
            val rpcVariacion = analisis.variacionVentasPct ?: return null
            variacion = rpcVariacion
            ventasReferencia = analisis.ventasMesAnterior
            esProporcional = false
        }

        if (variacion >= -config.caidaVentasAlertaPct) return null

        val currentMonth = hoy.monthValue
        if (currentMonth in listOf(1, 2)) return null

        val pctStr = String.format("%.0f", kotlin.math.abs(variacion))
        val titulo = "Caida en ventas detectada"
        val detalleBase = if (esProporcional) {
            "Las ventas cayeron un ${pctStr}% respecto al mismo periodo del mes pasado " +
                "(proporcional a $diasTranscurridos dias). "
        } else {
            "Las ventas cayeron un ${pctStr}% respecto al mes pasado. "
        }
        return Recomendacion(
            id = "${RecomendacionTipo.ALERTA_CAIDA.name}::$titulo",
            tipo = RecomendacionTipo.ALERTA_CAIDA,
            titulo = titulo,
            detalle = detalleBase +
                "¿Estacionalidad o tendencia? " +
                "Ventas actuales: S/ ${String.format("%.0f", analisis.ventasMes)} vs. " +
                "S/ ${String.format("%.0f", ventasReferencia)} esperado.",
            impactoEstimado = null,
            prioridad = Prioridad.ALTA,
            accion = "Revisar causas de la caida y evaluar acciones correctivas",
            datosAccion = null
        )
    }

    // ── R6: REDUCIR_GASTO (MEDIA) ──────────────────────────────────────────

    private fun evaluarReducirGasto(
        analisis: AnalisisMensual
    ): Recomendacion? {
        if (analisis.ventasMes <= 0.0) return null
        val ratio = analisis.gastosMes / analisis.ventasMes
        if (ratio <= GASTOS_VENTAS_RATIO_ALERTA) return null

        val ratioPct = String.format("%.1f", ratio * 100)
        val titulo = "Reducir gastos operativos"
        return Recomendacion(
            id = "${RecomendacionTipo.REDUCIR_GASTO.name}::$titulo",
            tipo = RecomendacionTipo.REDUCIR_GASTO,
            titulo = titulo,
            detalle = "Los gastos representan el ${ratioPct}% de las ventas " +
                "(S/ ${String.format("%.0f", analisis.gastosMes)} gastos vs. " +
                "S/ ${String.format("%.0f", analisis.ventasMes)} ventas). " +
                "Reducir un 10% los gastos liberaria aproximadamente " +
                "S/ ${String.format("%.0f", analisis.gastosMes * 0.1)}.",
            impactoEstimado = null,
            prioridad = Prioridad.MEDIA,
            accion = "Analizar partidas de gasto y eliminar las no esenciales",
            datosAccion = null
        )
    }
}
