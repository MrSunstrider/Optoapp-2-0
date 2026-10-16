package com.example.optoapp.domain

import com.example.optoapp.data.Resource
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.util.MoneyFormatter
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

open class GenerarRecomendacionesUseCase @Inject constructor(
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao,
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
        opticaId: String,
        mesAnalisis: LocalDate = LocalDate.now().withDayOfMonth(1),
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
                evaluarAlertaCaida(analisis, config, mesAnalisis),
                evaluarReducirGasto(analisis),
            )
        }

        val sorted = recommendations.sortedWith(
            compareBy<Recomendacion> { it.prioridad.ordinal }
                .thenByDescending { it.impactoMonto ?: Double.NEGATIVE_INFINITY }
                .thenBy { it.tipo.ordinal },
        )

        return Resource.Success(sorted.take(5))
    }

    private fun evaluarCobrar(
        deudores: List<Deudor>,
        config: ConfiguracionFinancieraEntity,
    ): Recomendacion? {
        if (deudores.isEmpty()) return null

        val deudaTotal = deudores.sumOf { it.saldo }
        val tieneVieja = deudores.any { it.diasDeuda > config.deudaViejaAlertaDias }

        if (deudaTotal <= config.deudaTotalAlertaMonto && !tieneVieja) return null

        val top3 = deudores.sortedByDescending { it.saldo }.take(3)
        val nombres = top3.joinToString(", ") { it.pacienteNombre }
        val totalStr = MoneyFormatter.format(deudaTotal)

        val titulo = "Cobranza pendiente"
        val impacto = ImpactoRecomendacion.cobrar(deudaTotal)
        return Recomendacion(
            id = "${RecomendacionTipo.COBRAR.name}::$titulo",
            tipo = RecomendacionTipo.COBRAR,
            titulo = titulo,
            detalle = "Deuda total de $totalStr en ${deudores.size} deudores. " +
                "Principales: $nombres.",
            impactoEstimado = impacto.etiqueta,
            impactoMonto = impacto.monto,
            prioridad = Prioridad.ALTA,
            accion = "Contactar a los deudores para coordinar pagos pendientes",
            datosAccion = DatosAccion(
                pacienteIds = top3.map { it.pacienteId },
                montoTotal = top3.sumOf { it.saldo },
            ),
        )
    }

    private fun evaluarMejorarPrecio(
        categorias: List<MargenCategoria>,
        config: ConfiguracionFinancieraEntity,
    ): Recomendacion? {
        val minVentas = config.minVentasParaRecomendar.toDouble()
        val target = categorias.firstOrNull { cat ->
            (cat.margenPct ?: Double.MAX_VALUE) < MARGEN_BAJO_PCT &&
                cat.ventas >= minVentas
        } ?: return null

        val margenPctStr = String.format("%.1f", target.margenPct ?: 0.0)
        val titulo = "Mejorar precio de ${target.categoria}"
        val impacto = ImpactoRecomendacion.mejorarPrecio(target.ventas, target.margenPct)
        return Recomendacion(
            id = "${RecomendacionTipo.MEJORAR_PRECIO.name}::$titulo",
            tipo = RecomendacionTipo.MEJORAR_PRECIO,
            titulo = titulo,
            detalle = "La categoria ${target.categoria} tiene un margen bajo " +
                "del $margenPctStr%. Considera ajustar precios para alcanzar " +
                "al menos un 25% de margen.",
            impactoEstimado = impacto?.etiqueta ?: ImpactoRecomendacion.SIN_IMPACTO,
            impactoMonto = impacto?.monto,
            prioridad = Prioridad.ALTA,
            accion = "Revisar precios de ${target.categoria} y ajustar al alza",
            datosAccion = null,
        )
    }

    private fun evaluarLiquidarStock(
        stockEstancado: List<StockEstancadoItem>,
        config: ConfiguracionFinancieraEntity,
    ): Recomendacion? {
        val estancados = stockEstancado.filter {
            it.diasSinVenta > config.stockEstancadoAlertaDias
        }
        if (estancados.isEmpty()) return null

        val modelos = estancados.joinToString(", ") { it.modelo }
        val costoTotal = estancados.sumOf { it.costo * it.stockActual }

        val titulo = "Liquidar stock estancado"
        val impacto = ImpactoRecomendacion.liquidarStock(costoTotal)
        return Recomendacion(
            id = "${RecomendacionTipo.LIQUIDAR_STOCK.name}::$titulo",
            tipo = RecomendacionTipo.LIQUIDAR_STOCK,
            titulo = titulo,
            detalle = "${estancados.size} items llevan mas de ${config.stockEstancadoAlertaDias} " +
                "dias sin venderse: $modelos. " +
                "Costo total en inventario: ${MoneyFormatter.format(costoTotal)}.",
            impactoEstimado = impacto.etiqueta,
            impactoMonto = impacto.monto,
            prioridad = Prioridad.MEDIA,
            accion = "Ofrecer descuentos del 20-30% para liberar capital estancado",
            datosAccion = DatosAccion(
                productoIds = estancados.map { it.monturaId },
            ),
        )
    }

    private fun evaluarVenderMasDe(
        categorias: List<MargenCategoria>,
        config: ConfiguracionFinancieraEntity,
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
        val impacto = ImpactoRecomendacion.venderMasDe(target.ventas, target.margenPct)
        return Recomendacion(
            id = "${RecomendacionTipo.VENDER_MAS_DE.name}::$titulo",
            tipo = RecomendacionTipo.VENDER_MAS_DE,
            titulo = titulo,
            detalle = "${target.categoria} tiene un margen alto del $margenPctStr% " +
                "y contribuye significativamente a la ganancia total. " +
                "Aumenta su promocion y visibilidad.",
            impactoEstimado = impacto?.etiqueta ?: ImpactoRecomendacion.SIN_IMPACTO,
            impactoMonto = impacto?.monto,
            prioridad = Prioridad.MEDIA,
            accion = "Crear promocion destacada para ${target.categoria}",
            datosAccion = null,
        )
    }

    private fun evaluarAlertaCaida(
        analisis: AnalisisMensual,
        config: ConfiguracionFinancieraEntity,
        mesAnalisis: LocalDate,
    ): Recomendacion? {
        val mes = YearMonth.from(mesAnalisis)
        // Seasonal mute is about the analyzed month, not wall-clock "today".
        if (mes.monthValue in 1..2) return null

        val hoy = LocalDate.now()
        val esMesEnCurso = mes == YearMonth.from(hoy)

        val variacion: Double
        val ventasReferencia: Double
        val esProporcional: Boolean
        val diasProporcionales: Int

        if (esMesEnCurso && analisis.ventasMesAnterior > 0.0) {
            val diasTranscurridos = hoy.dayOfMonth
            val diasMesAnterior = mes.minusMonths(1).lengthOfMonth()
            if (diasTranscurridos <= 0 || diasMesAnterior <= 0) return null
            val proporcion = diasTranscurridos.toDouble() / diasMesAnterior.toDouble()
            ventasReferencia = analisis.ventasMesAnterior * proporcion
            variacion = ((analisis.ventasMes - ventasReferencia) / ventasReferencia) * 100.0
            esProporcional = true
            diasProporcionales = diasTranscurridos
        } else {
            val rpcVariacion = analisis.variacionVentasPct
            if (rpcVariacion != null) {
                variacion = rpcVariacion
                ventasReferencia = analisis.ventasMesAnterior
            } else if (analisis.ventasMesAnterior > 0.0) {
                ventasReferencia = analisis.ventasMesAnterior
                variacion = ((analisis.ventasMes - ventasReferencia) / ventasReferencia) * 100.0
            } else {
                return null
            }
            esProporcional = false
            diasProporcionales = 0
        }

        if (variacion >= -config.caidaVentasAlertaPct) return null

        val pctStr = String.format("%.0f", kotlin.math.abs(variacion))
        val titulo = "Caida en ventas detectada"
        val detalleBase = if (esProporcional) {
            "Las ventas cayeron un $pctStr% respecto al mismo periodo del mes pasado " +
                "(proporcional a $diasProporcionales dias). "
        } else {
            "Las ventas cayeron un $pctStr% respecto al mes pasado. "
        }
        val impacto = ImpactoRecomendacion.alertaCaida(analisis.ventasMes, ventasReferencia)
        return Recomendacion(
            id = "${RecomendacionTipo.ALERTA_CAIDA.name}::$titulo",
            tipo = RecomendacionTipo.ALERTA_CAIDA,
            titulo = titulo,
            detalle = detalleBase +
                "¿Estacionalidad o tendencia? " +
                "Ventas actuales: ${MoneyFormatter.format(analisis.ventasMes)} vs. " +
                "${MoneyFormatter.format(ventasReferencia)} esperado.",
            impactoEstimado = impacto?.etiqueta ?: ImpactoRecomendacion.SIN_IMPACTO,
            impactoMonto = impacto?.monto,
            prioridad = Prioridad.ALTA,
            accion = "Revisar causas de la caida y evaluar acciones correctivas",
            datosAccion = null,
        )
    }

    private fun evaluarReducirGasto(
        analisis: AnalisisMensual,
    ): Recomendacion? {
        if (analisis.ventasMes <= 0.0) return null
        val ratio = analisis.gastosMes / analisis.ventasMes
        if (ratio <= GASTOS_VENTAS_RATIO_ALERTA) return null

        val ratioPct = String.format("%.1f", ratio * 100)
        val titulo = "Reducir gastos operativos"
        val impacto = ImpactoRecomendacion.reducirGasto(analisis.gastosMes)
        return Recomendacion(
            id = "${RecomendacionTipo.REDUCIR_GASTO.name}::$titulo",
            tipo = RecomendacionTipo.REDUCIR_GASTO,
            titulo = titulo,
            detalle = "Los gastos representan el $ratioPct% de las ventas " +
                "(${MoneyFormatter.format(analisis.gastosMes)} gastos vs. " +
                "${MoneyFormatter.format(analisis.ventasMes)} ventas). " +
                "Reducir un 10% los gastos liberaria aproximadamente " +
                "${MoneyFormatter.format(analisis.gastosMes * 0.1)}.",
            impactoEstimado = impacto.etiqueta,
            impactoMonto = impacto.monto,
            prioridad = Prioridad.MEDIA,
            accion = "Analizar partidas de gasto y eliminar las no esenciales",
            datosAccion = null,
        )
    }
}
