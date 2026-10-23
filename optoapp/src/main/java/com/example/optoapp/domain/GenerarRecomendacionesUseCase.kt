package com.example.optoapp.domain

import com.example.optoapp.data.Resource
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.util.AnalisisPercentFormatter
import com.example.optoapp.util.MoneyFormatter
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

open class GenerarRecomendacionesUseCase @Inject constructor(
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao,
) {
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
        val supuestos = SupuestosImpacto.from(config)

        val cobrar = evaluarCobrar(deudores, config, supuestos)

        val recommendations = if (analisis == null) {
            listOfNotNull(cobrar)
        } else {
            listOfNotNull(
                cobrar,
                evaluarMejorarPrecio(analisis.margenPorCategoria, config, supuestos),
                evaluarLiquidarStock(analisis.stockEstancado, config, supuestos),
                evaluarVenderMasDe(analisis.margenPorCategoria, config, supuestos),
                evaluarAlertaCaida(analisis, config, mesAnalisis),
                evaluarReducirGasto(analisis, config, supuestos),
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
        supuestos: SupuestosImpacto,
    ): Recomendacion? {
        if (deudores.isEmpty()) return null

        val deudaTotal = deudores.sumOf { it.saldo }
        val tieneVieja = deudores.any { it.diasDeuda > config.deudaViejaAlertaDias }

        if (deudaTotal <= config.deudaTotalAlertaMonto && !tieneVieja) return null

        val top3 = deudores.sortedByDescending { it.saldo }.take(3)
        val nombres = top3.joinToString(", ") { it.pacienteNombre }
        val totalStr = MoneyFormatter.format(deudaTotal)
        val hallazgo = "CxC de $totalStr en ${deudores.size} deudores (principales: $nombres)."
        val titulo = "Cobranza pendiente"
        val impacto = ImpactoRecomendacion.cobrar(deudaTotal, supuestos)
        return Recomendacion(
            id = "${RecomendacionTipo.COBRAR.name}::$titulo",
            tipo = RecomendacionTipo.COBRAR,
            titulo = titulo,
            detalle = hallazgo,
            impactoEstimado = impacto.etiqueta,
            impactoMonto = impacto.monto,
            prioridad = Prioridad.ALTA,
            accion = "Esta semana: contactar a los 3 mayores deudores y acordar fecha de pago",
            datosAccion = DatosAccion(
                pacienteIds = top3.map { it.pacienteId },
                montoTotal = top3.sumOf { it.saldo },
            ),
            hallazgo = hallazgo,
            causaProbable = "Pagos parciales o saldos viejos sin seguimiento activo",
            esfuerzo = EsfuerzoRecomendacion.BAJO,
            supuestoVisible = supuestos.labelCobrar(),
            ctaLabel = "Abrir deudores",
            cta = CtaRecomendacion.DEUDORES,
        )
    }

    private fun evaluarMejorarPrecio(
        categorias: List<MargenCategoria>,
        config: ConfiguracionFinancieraEntity,
        supuestos: SupuestosImpacto,
    ): Recomendacion? {
        val minVentas = config.minVentasParaRecomendar.toDouble()
        val target = categorias.firstOrNull { cat ->
            (cat.margenPct ?: Double.MAX_VALUE) < config.margenBajoAlertaPct &&
                cat.ventas >= minVentas
        } ?: return null

        val margenPctStr = AnalisisPercentFormatter.format(target.margenPct ?: 0.0)
        val ventasStr = MoneyFormatter.format(target.ventas)
        val targetPct = AnalisisPercentFormatter.format(supuestos.margenObjetivoPct)
        val hallazgo = "${target.categoria}: margen $margenPctStr% sobre $ventasStr."
        val titulo = "Mejorar precio de ${target.categoria}"
        val impacto = ImpactoRecomendacion.mejorarPrecio(target.ventas, target.margenPct, supuestos)
        return Recomendacion(
            id = "${RecomendacionTipo.MEJORAR_PRECIO.name}::$titulo",
            tipo = RecomendacionTipo.MEJORAR_PRECIO,
            titulo = titulo,
            detalle = hallazgo,
            impactoEstimado = impacto?.etiqueta ?: ImpactoRecomendacion.SIN_IMPACTO,
            impactoMonto = impacto?.monto,
            prioridad = Prioridad.ALTA,
            accion = "Esta semana: revisar matriz de costos y subir precio efectivo hacia $targetPct%",
            datosAccion = null,
            hallazgo = hallazgo,
            causaProbable = "Precio por debajo de matriz o costo desactualizado",
            esfuerzo = EsfuerzoRecomendacion.MEDIO,
            supuestoVisible = supuestos.labelMejorarPrecio(),
            ctaLabel = "Abrir Costos",
            cta = CtaRecomendacion.COSTOS,
        )
    }

    private fun evaluarLiquidarStock(
        stockEstancado: List<StockEstancadoItem>,
        config: ConfiguracionFinancieraEntity,
        supuestos: SupuestosImpacto,
    ): Recomendacion? {
        val estancados = stockEstancado.filter {
            it.diasSinVenta > config.stockEstancadoAlertaDias
        }
        if (estancados.isEmpty()) return null

        val modelos = estancados.joinToString(", ") { it.modelo }
        val costoTotal = estancados.sumOf { it.costo * it.stockActual }
        val hallazgo = "${estancados.size} items >${config.stockEstancadoAlertaDias} dias sin venta ($modelos); " +
            "costo ${MoneyFormatter.format(costoTotal)}."
        val titulo = "Liquidar stock estancado"
        val impacto = ImpactoRecomendacion.liquidarStock(costoTotal, supuestos)
        return Recomendacion(
            id = "${RecomendacionTipo.LIQUIDAR_STOCK.name}::$titulo",
            tipo = RecomendacionTipo.LIQUIDAR_STOCK,
            titulo = titulo,
            detalle = hallazgo,
            impactoEstimado = impacto.etiqueta,
            impactoMonto = impacto.monto,
            prioridad = Prioridad.MEDIA,
            accion = "Esta semana: ofrecer descuento 20-30% en esos modelos para liberar capital",
            datosAccion = DatosAccion(
                productoIds = estancados.map { it.monturaId },
            ),
            hallazgo = hallazgo,
            causaProbable = "Compra sin rotacion o mix poco demandado",
            esfuerzo = EsfuerzoRecomendacion.MEDIO,
            supuestoVisible = supuestos.labelLiquidarStock(),
            ctaLabel = "Ver detalle",
            cta = CtaRecomendacion.DETALLE,
        )
    }

    private fun evaluarVenderMasDe(
        categorias: List<MargenCategoria>,
        config: ConfiguracionFinancieraEntity,
        supuestos: SupuestosImpacto,
    ): Recomendacion? {
        val minVentas = config.minVentasParaRecomendar.toDouble()
        val gananciaTotal = categorias.sumOf { it.ventas - it.costos }
        if (gananciaTotal <= 0.0) return null

        val target = categorias.firstOrNull { cat ->
            val margenPct = cat.margenPct ?: return@firstOrNull false
            val margenBruto = cat.ventas - cat.costos
            val contribucion = margenBruto / gananciaTotal
            margenPct > config.margenAltoAlertaPct &&
                contribucion > config.contribucionAltaRatio &&
                cat.ventas >= minVentas
        } ?: return null

        val margenPctStr = AnalisisPercentFormatter.format(target.margenPct ?: 0.0)
        val hallazgo = "${target.categoria}: margen $margenPctStr% con peso alto en la utilidad bruta."
        val titulo = "Vender mas de ${target.categoria}"
        val impacto = ImpactoRecomendacion.venderMasDe(target.ventas, target.margenPct, supuestos)
        return Recomendacion(
            id = "${RecomendacionTipo.VENDER_MAS_DE.name}::$titulo",
            tipo = RecomendacionTipo.VENDER_MAS_DE,
            titulo = titulo,
            detalle = hallazgo,
            impactoEstimado = impacto?.etiqueta ?: ImpactoRecomendacion.SIN_IMPACTO,
            impactoMonto = impacto?.monto,
            prioridad = Prioridad.MEDIA,
            accion = "Esta semana: destacar ${target.categoria} en vitrina y pitch de venta",
            datosAccion = null,
            hallazgo = hallazgo,
            causaProbable = "Alta contribucion poco empujada en el piso de venta",
            esfuerzo = EsfuerzoRecomendacion.BAJO,
            supuestoVisible = supuestos.labelVenderMasDe(),
            ctaLabel = "Ver detalle",
            cta = CtaRecomendacion.DETALLE,
        )
    }

    private fun evaluarAlertaCaida(
        analisis: AnalisisMensual,
        config: ConfiguracionFinancieraEntity,
        mesAnalisis: LocalDate,
    ): Recomendacion? {
        val mes = YearMonth.from(mesAnalisis)
        // Jan/Feb YoY comps are mostly calendar seasonality noise for optical retail.
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

        val pctStr = AnalisisPercentFormatter.format(kotlin.math.abs(variacion))
        val titulo = "Caida en ventas detectada"
        val hallazgo = if (esProporcional) {
            "Ventas -$pctStr% vs mismo tramo del mes pasado ($diasProporcionales dias): " +
                "${MoneyFormatter.format(analisis.ventasMes)} vs ${MoneyFormatter.format(ventasReferencia)}."
        } else {
            "Ventas -$pctStr% vs mes pasado: " +
                "${MoneyFormatter.format(analisis.ventasMes)} vs ${MoneyFormatter.format(ventasReferencia)}."
        }
        val impacto = ImpactoRecomendacion.alertaCaida(analisis.ventasMes, ventasReferencia)
        return Recomendacion(
            id = "${RecomendacionTipo.ALERTA_CAIDA.name}::$titulo",
            tipo = RecomendacionTipo.ALERTA_CAIDA,
            titulo = titulo,
            detalle = hallazgo,
            impactoEstimado = impacto?.etiqueta ?: ImpactoRecomendacion.SIN_IMPACTO,
            impactoMonto = impacto?.monto,
            prioridad = Prioridad.ALTA,
            accion = "Esta semana: revisar agenda, ticket y mix de categorias con caida",
            datosAccion = null,
            hallazgo = hallazgo,
            causaProbable = "Estacionalidad, menor afluencia o ticket mas bajo",
            esfuerzo = EsfuerzoRecomendacion.MEDIO,
            supuestoVisible = "Comparacion vs periodo de referencia (sin supuesto de impacto extra)",
            ctaLabel = "Ver detalle",
            cta = CtaRecomendacion.DETALLE,
        )
    }

    private fun evaluarReducirGasto(
        analisis: AnalisisMensual,
        config: ConfiguracionFinancieraEntity,
        supuestos: SupuestosImpacto,
    ): Recomendacion? {
        val alerta = GastoFueraDeRango.evaluar(
            analisis.ventasMes,
            analisis.gastosMes,
            umbral = config.gastoFueraRangoRatio,
        ) ?: return null
        val ratioPct = AnalisisPercentFormatter.format(alerta.ratioPct)
        val hallazgo = "Opex ${MoneyFormatter.format(analisis.gastosMes)} = $ratioPct% de ventas " +
            "${MoneyFormatter.format(analisis.ventasMes)}."
        val titulo = "Reducir gastos operativos"
        val impacto = ImpactoRecomendacion.reducirGasto(analisis.gastosMes, supuestos)
        return Recomendacion(
            id = "${RecomendacionTipo.REDUCIR_GASTO.name}::$titulo",
            tipo = RecomendacionTipo.REDUCIR_GASTO,
            titulo = titulo,
            detalle = hallazgo,
            impactoEstimado = impacto.etiqueta,
            impactoMonto = impacto.monto,
            prioridad = Prioridad.MEDIA,
            accion = "Esta semana: revisar partidas no esenciales en Costos · Gastos",
            datosAccion = null,
            hallazgo = hallazgo,
            causaProbable = "Opex crecio mas rapido que las ventas del mes",
            esfuerzo = EsfuerzoRecomendacion.MEDIO,
            supuestoVisible = supuestos.labelReducirGasto(),
            ctaLabel = "Abrir Costos",
            cta = CtaRecomendacion.COSTOS,
        )
    }
}
