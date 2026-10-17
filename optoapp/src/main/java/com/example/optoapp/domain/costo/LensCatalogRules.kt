package com.example.optoapp.domain.costo

import kotlin.math.abs

data class LensRecetaInput(
    val esfera: Double?,
    val cilindro: Double?,
    val adicion: Double? = null,
)

data class LensCatalogSelection(
    val tipoLente: String,
    val distancia: String? = null,
    val subTipoBifocal: String? = null,
    val familia: String,
    val tratamientos: List<String> = emptyList(),
    val discromatopsiaTipo: String? = null,
    val curvaBase: String? = null,
    val indiceRefraccion: String? = null,
    val forzarStockIgnorandoAstigmatismo: Boolean = false,
)

data class LensCatalogResolution(
    val stockOFabricacion: String,
    val serie: Int?,
    val allowedTratamientos: List<String>,
    val requiresManualIrCost: Boolean,
    val requiresCurvaBase: Boolean,
    val requiresDiscromatopsiaTipo: Boolean,
    val matrixFamilia: String,
    val matrixTratamiento: String?,
)

/**
 * Pure catalog rules: which families/treatments are valid and whether stock|fab + serie apply.
 * Does not look up or invent monetary costs — those live in cost matrices.
 */
object LensCatalogRules {
    const val STOCK = "stock"
    const val FABRICACION = "fabricacion"

    fun distancias(tipoLente: String): List<String> =
        if (tipoLente == LensProductCatalog.TIPO_MONOFOCAL) LensProductCatalog.DISTANCIAS_MONOFOCAL else emptyList()

    fun subtiposBifocal(tipoLente: String): List<String> =
        if (tipoLente == LensProductCatalog.TIPO_BIFOCAL) LensProductCatalog.SUBTIPOS_BIFOCAL else emptyList()

    fun familias(
        tipoLente: String,
        distancia: String? = null,
        subTipoBifocal: String? = null,
    ): List<String> =
        LensProductCatalog.families(tipoLente, distancia, subTipoBifocal).map { it.material }

    fun tratamientosPermitidos(
        tipoLente: String,
        familia: String,
        distancia: String? = null,
        subTipoBifocal: String? = null,
    ): List<String> {
        val family = LensProductCatalog.findFamily(tipoLente, familia, distancia, subTipoBifocal) ?: return emptyList()
        val nearOrInter = distancia == LensProductCatalog.DIST_INTERMEDIA ||
            distancia == LensProductCatalog.DIST_CERCA
        return family.extras.filterNot { nearOrInter && it in LensTreatments.BLOCKED_NEAR_INTERMEDIATE }
    }

    fun resolve(selection: LensCatalogSelection, receta: LensRecetaInput?): LensCatalogResolution {
        val family = LensProductCatalog.findFamily(
            selection.tipoLente,
            selection.familia,
            selection.distancia,
            selection.subTipoBifocal,
        ) ?: return LensCatalogResolution(
            stockOFabricacion = FABRICACION,
            serie = null,
            allowedTratamientos = emptyList(),
            requiresManualIrCost = false,
            requiresCurvaBase = false,
            requiresDiscromatopsiaTipo = false,
            matrixFamilia = LensProductCatalog.canonicalizeBlueAlias(selection.familia),
            matrixTratamiento = matrixTratamientoKey(selection.tratamientos, selection.discromatopsiaTipo),
        )

        val allowed = tratamientosPermitidos(
            selection.tipoLente,
            family.material,
            selection.distancia,
            selection.subTipoBifocal,
        )
        // Keep persisted extras even when not in the current allowed list (legacy labels).
        val selectedExtras = selection.tratamientos.filter { it.isNotBlank() }
        val hasDiscromatopsia = selectedExtras.contains(LensTreatments.DISCROMATOPSIA)
        val hasSolar = selectedExtras.contains(LensTreatments.SOLAR)

        val (stockFab, serie) = deriveStockSerie(
            family = family,
            receta = receta,
            selectedExtras = selectedExtras,
            indiceRefraccion = selection.indiceRefraccion,
            forzarStock = selection.forzarStockIgnorandoAstigmatismo,
        )

        return LensCatalogResolution(
            stockOFabricacion = stockFab,
            serie = serie,
            allowedTratamientos = allowed,
            requiresManualIrCost = family.requiresManualIr,
            requiresCurvaBase = hasSolar,
            requiresDiscromatopsiaTipo = hasDiscromatopsia,
            matrixFamilia = family.material,
            matrixTratamiento = matrixTratamientoKey(selectedExtras, selection.discromatopsiaTipo),
        )
    }

    fun seriePorCilindro(cilindro: Double?): Int? {
        val absCil = abs(cilindro ?: 0.0)
        return when {
            absCil <= 2.00 -> 1
            absCil <= 4.00 -> 2
            absCil <= 6.00 -> 3
            else -> null
        }
    }

    fun matrixTratamientoKey(tratamientos: List<String>, discromatopsiaTipo: String?): String? {
        if (tratamientos.isEmpty()) return null
        val sorted = tratamientos.sorted()
        val base = sorted.joinToString(" + ")
        val tipo = discromatopsiaTipo?.takeIf { it.isNotBlank() }
        return if (tipo != null && LensTreatments.DISCROMATOPSIA in tratamientos) {
            "$base ($tipo)"
        } else {
            base
        }
    }

    private fun deriveStockSerie(
        family: ProductFamily,
        receta: LensRecetaInput?,
        selectedExtras: List<String>,
        indiceRefraccion: String?,
        forzarStock: Boolean,
    ): Pair<String, Int?> {
        val extrasForceFab = selectedExtras.any { it != LensTreatments.UV_400 && it in LensTreatments.FORCE_FABRICACION }
        if (extrasForceFab) return FABRICACION to null

        return when (family.stockPolicy) {
            StockPolicy.ALWAYS_FABRICACION -> FABRICACION to null

            StockPolicy.MONOFOCAL_CIL_SERIES -> monofocalStockSerie(receta)

            StockPolicy.CRISTAL_ALTO_INDICE_1_7_STOCK -> {
                val ir = indiceRefraccion?.trim().orEmpty()
                if (ir == "1.7" || ir == "1.70") {
                    monofocalStockSerie(receta)
                } else {
                    FABRICACION to null
                }
            }

            StockPolicy.BIFOCAL_FLAPTOP_RESINA -> bifocalWindow(
                receta = receta,
                esfMin = -1.00,
                esfMax = 3.00,
                addMin = 1.00,
                addMax = 3.00,
                forzarStock = forzarStock,
            )

            StockPolicy.BIFOCAL_PLUS_WINDOW -> bifocalWindow(
                receta = receta,
                esfMin = 1.00,
                esfMax = 3.00,
                addMin = 1.00,
                addMax = 3.00,
                forzarStock = forzarStock,
            )
        }
    }

    private fun monofocalStockSerie(receta: LensRecetaInput?): Pair<String, Int?> {
        val esf = receta?.esfera ?: return FABRICACION to null
        val cil = receta.cilindro
        if (abs(esf) > 6.00 || abs(cil ?: 0.0) > 6.00) return FABRICACION to null
        val serie = seriePorCilindro(cil) ?: return FABRICACION to null
        return STOCK to serie
    }

    private fun bifocalWindow(
        receta: LensRecetaInput?,
        esfMin: Double,
        esfMax: Double,
        addMin: Double,
        addMax: Double,
        forzarStock: Boolean,
    ): Pair<String, Int?> {
        val esf = receta?.esfera ?: return FABRICACION to null
        val add = receta.adicion ?: return FABRICACION to null
        val cil = abs(receta.cilindro ?: 0.0)
        val inSphere = esf in esfMin..esfMax
        val inAdd = add in addMin..addMax
        val hasAstig = cil > 0.0
        return when {
            forzarStock && inSphere && inAdd -> STOCK to 1
            !inSphere || !inAdd -> FABRICACION to null
            hasAstig -> FABRICACION to null
            else -> STOCK to 1
        }
    }
}
