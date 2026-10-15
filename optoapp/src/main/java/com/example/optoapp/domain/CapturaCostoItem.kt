package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem

object CapturaCostoItem {

    suspend fun capturar(
        item: DispensacionItem,
        previo: DispensacionItem?,
        overrides: Map<ComponenteCosto, Double>,
        receta: RecetaCosto?,
        recetaPrevia: RecetaCosto?,
        calcular: suspend (DispensacionItem) -> CostoItemCalculado,
    ): DispensacionItem {
        val actual = item.copy(altoIndice = altoIndice(item, previo))
        val pendientes = ComponenteCosto.entries.filter {
            it !in overrides && requiereLookup(it, actual, previo, receta, recetaPrevia)
        }
        val calculado = if (pendientes.isEmpty()) emptyMap() else calcular(actual).componentes
        val costos = mutableMapOf<ComponenteCosto, Double>()
        val origenes = mutableListOf<OrigenComponente>()
        ComponenteCosto.entries.forEach { componente ->
            val override = overrides[componente]
            val persistido = previo?.costo(componente)
            when {
                override != null -> {
                    costos[componente] = override
                    origenes += OrigenComponente.MANUAL
                }
                componente in pendientes -> if (componente in calculado) {
                    val valor = calculado[componente]
                    costos[componente] = valor ?: 0.0
                    origenes += if (valor != null) OrigenComponente.MATRIZ else OrigenComponente.FALTANTE
                }
                persistido != null -> {
                    costos[componente] = persistido
                    origenes += OrigenComponente.PRESERVADA
                }
            }
        }
        return actual.copy(
            costoRealOd = costos[ComponenteCosto.OD],
            costoRealOi = costos[ComponenteCosto.OI],
            costoRealMontura = costos[ComponenteCosto.MONTURA],
            costoRealBiselado = costos[ComponenteCosto.BISELADO],
            costoRealLc = costos[ComponenteCosto.LC],
            costoOrigen = CostoOrigenResolver.resolver(origenes, previo?.costoOrigen),
        )
    }

    // A value persisted by the web stays; one derived from a treatment the user removed must go.
    private fun altoIndice(item: DispensacionItem, previo: DispensacionItem?): String? {
        AltoIndiceNormalizer.fromTratamientos(item.tratamientos)?.let { return it }
        val persistido = previo?.altoIndice ?: return null
        val derivadoPrevio = AltoIndiceNormalizer.fromTratamientos(previo.tratamientos)
        return persistido.takeUnless { derivadoPrevio != null && AltoIndiceNormalizer.normalize(it) == derivadoPrevio }
    }

    private fun requiereLookup(
        componente: ComponenteCosto,
        item: DispensacionItem,
        previo: DispensacionItem?,
        receta: RecetaCosto?,
        recetaPrevia: RecetaCosto?,
    ): Boolean {
        if (previo == null) return true
        if (entradas(componente, item, receta) != entradas(componente, previo, recetaPrevia)) return true
        val sinOrigenConfiable = previo.costoOrigen == null || previo.costoOrigen == CostoOrigenResolver.FALTANTE
        return sinOrigenConfiable && (previo.costo(componente) ?: 0.0) <= 0.0
    }

    private fun entradas(componente: ComponenteCosto, item: DispensacionItem, receta: RecetaCosto?): List<Any?> {
        val lente = listOf(item.tipoLente, item.materialLente, item.tratamientos.sorted())
        return when (componente) {
            ComponenteCosto.OD -> lente + listOf(receta?.odEsf, receta?.odCil)
            ComponenteCosto.OI -> lente + listOf(receta?.oiEsf, receta?.oiCil)
            ComponenteCosto.MONTURA -> listOf(item.monturaId, item.origenMontura)
            ComponenteCosto.BISELADO -> listOf(
                item.materialLente,
                item.tipoAro,
                AltoIndiceNormalizer.normalize(item.altoIndice),
                bloqueBiselado(receta?.odEsf, receta?.odCil),
                bloqueBiselado(receta?.oiEsf, receta?.oiCil),
            )
            ComponenteCosto.LC -> listOf(item.tipoLente, item.materialLente, receta?.lcMaterial, receta?.lcLaboratorio)
        }
    }

    // The bevel price depends on the stock/fabrication block and cylinder series, not on the raw sphere.
    private fun bloqueBiselado(esfera: Double?, cilindro: Double?): Pair<String, Int?>? = esfera?.let {
        CalcularCostoItemUseCase.tipoLente(it, cilindro) to CalcularCostoItemUseCase.seriePorCilindro(cilindro)
    }

    private fun DispensacionItem.costo(componente: ComponenteCosto): Double? = when (componente) {
        ComponenteCosto.OD -> costoRealOd
        ComponenteCosto.OI -> costoRealOi
        ComponenteCosto.MONTURA -> costoRealMontura
        ComponenteCosto.BISELADO -> costoRealBiselado
        ComponenteCosto.LC -> costoRealLc
    }
}
