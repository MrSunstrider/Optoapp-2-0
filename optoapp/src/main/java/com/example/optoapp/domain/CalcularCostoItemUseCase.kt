package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.domain.costo.LensCatalogRules
import com.example.optoapp.domain.costo.LensCatalogSelection
import com.example.optoapp.domain.costo.LensRecetaInput
import com.example.optoapp.util.AppLogger
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

enum class ComponenteCosto { OD, OI, MONTURA, BISELADO, LC }

data class RecetaCosto(
    val odEsf: Double?,
    val odCil: Double?,
    val oiEsf: Double?,
    val oiCil: Double?,
    val adicion: Double? = null,
    val lcMaterial: String? = null,
    val lcLaboratorio: String? = null,
) {
    companion object {
        fun from(evaluacion: EvaluacionClinica): RecetaCosto = RecetaCosto(
            odEsf = evaluacion.recetaOdEsf.toGraduacion(),
            odCil = evaluacion.recetaOdCil.toGraduacion(),
            oiEsf = evaluacion.recetaOiEsf.toGraduacion(),
            oiCil = evaluacion.recetaOiCil.toGraduacion(),
            adicion = (evaluacion.addCercaOd ?: evaluacion.addCercaOi).toGraduacion(),
            lcMaterial = evaluacion.lcMaterial?.takeIf { it.isNotBlank() },
            lcLaboratorio = evaluacion.lcLaboratorio?.takeIf { it.isNotBlank() },
        )

        private fun String?.toGraduacion(): Double? = this?.replace(",", ".")?.toDoubleOrNull()
    }
}

data class CostoItemCalculado(val componentes: Map<ComponenteCosto, Double?>)

class CalcularCostoItemUseCase @Inject constructor(
    private val costoProductoDao: CostoProductoDao,
    private val costoBiseladoDao: CostoBiseladoDao,
    private val monturaDao: MonturaDao,
    private val costoLcDao: CostoLcDao,
) {

    suspend operator fun invoke(
        opticaId: String,
        item: DispensacionItem,
        receta: RecetaCosto?,
        fecha: LocalDate,
    ): CostoItemCalculado {
        val fechaIso = fecha.toString()
        if (esLenteDeContacto(item)) {
            return CostoItemCalculado(mapOf(ComponenteCosto.LC to costoLc(opticaId, item, receta, fechaIso)))
        }
        val componentes = linkedMapOf<ComponenteCosto, Double?>()
        val ojos = ojosConEsfera(receta)
        ojos.forEach { (componente, ojo) ->
            componentes[componente] = costoLente(opticaId, item, ojo, receta?.adicion, fechaIso)
        }
        if (item.origenMontura == ORIGEN_TIENDA && item.monturaId.isNotBlank()) {
            componentes[ComponenteCosto.MONTURA] = monturaDao.getMonturaByIdForOptica(item.monturaId, opticaId)
                ?.costo?.takeIf { it > 0.0 }
        }
        if (item.tipoAro.isNotBlank()) {
            componentes[ComponenteCosto.BISELADO] = costoBiselado(opticaId, item, ojos.values, receta?.adicion, fechaIso)
        }
        return CostoItemCalculado(componentes)
    }

    private data class Ojo(val esfera: Double, val cilindro: Double?)

    private fun ojosConEsfera(receta: RecetaCosto?): Map<ComponenteCosto, Ojo> = buildMap {
        receta?.odEsf?.let { put(ComponenteCosto.OD, Ojo(it, receta.odCil)) }
        receta?.oiEsf?.let { put(ComponenteCosto.OI, Ojo(it, receta.oiCil)) }
    }

    private fun resolveForEye(
        item: DispensacionItem,
        ojo: Ojo,
        adicion: Double?,
    ) = LensCatalogRules.resolve(
        LensCatalogSelection(
            tipoLente = item.tipoLente,
            distancia = item.distanciaLente.takeIf { it.isNotBlank() },
            subTipoBifocal = item.subTipoBifocal.takeIf { it.isNotBlank() },
            familia = item.materialLente,
            tratamientos = item.tratamientos,
            discromatopsiaTipo = item.filtroDiscromatopsiaTipo.takeIf { it.isNotBlank() },
            curvaBase = item.curvaBase?.takeIf { it.isNotBlank() },
            indiceRefraccion = item.altoIndice?.takeIf { it.isNotBlank() },
            forzarStockIgnorandoAstigmatismo = item.forzarStockIgnorandoAstigmatismo,
        ),
        LensRecetaInput(esfera = ojo.esfera, cilindro = ojo.cilindro, adicion = adicion),
    )

    private suspend fun costoLente(
        opticaId: String,
        item: DispensacionItem,
        ojo: Ojo,
        adicion: Double?,
        fecha: String,
    ): Double? {
        val resolution = resolveForEye(item, ojo, adicion)
        if (resolution.requiresManualIrCost) return null
        return costoProductoDao.lookupVigente(
            opticaId = opticaId,
            material = resolution.matrixFamilia,
            tipoLente = item.tipoLente,
            stockOFabricacion = resolution.stockOFabricacion,
            tratamiento = resolution.matrixTratamiento,
            serie = resolution.serie,
            fecha = fecha,
            subtipoBifocal = item.subTipoBifocal.takeIf { it.isNotBlank() },
            // altoIndice on the item is also used for biselado; only matrix IR rows need it here.
            indiceRefraccion = if (resolution.requiresManualIrCost) {
                item.altoIndice?.takeIf { it.isNotBlank() }
            } else {
                null
            },
            curvaBase = if (resolution.requiresCurvaBase) {
                item.curvaBase?.takeIf { it.isNotBlank() }
            } else {
                null
            },
        )?.costoUnitario
    }

    private suspend fun costoBiselado(
        opticaId: String,
        item: DispensacionItem,
        ojos: Collection<Ojo>,
        adicion: Double?,
        fecha: String,
    ): Double? {
        val resolutions = ojos.map { resolveForEye(item, it, adicion) }
        val fabricacion = resolutions.any { it.stockOFabricacion == LensCatalogRules.FABRICACION }
        val serie = if (fabricacion) null else resolutions.mapNotNull { it.serie }.maxOrNull()
            ?: LensCatalogRules.seriePorCilindro(null)
        val altoIndice = AltoIndiceNormalizer.normalize(item.altoIndice)
            ?: AltoIndiceNormalizer.fromTratamientos(item.tratamientos)
        val materialBase = biseladoMaterialKey(item.materialLente)
        return costoBiseladoDao.lookupVigente(
            opticaId = opticaId,
            material = materialBase,
            tipoAro = tipoAroMatriz(item.tipoAro),
            stockOFabricacion = if (fabricacion) LensCatalogRules.FABRICACION else LensCatalogRules.STOCK,
            serie = serie,
            fecha = fecha,
        ).firstOrNull { AltoIndiceNormalizer.matches(it.altoIndice, altoIndice) }?.costoPorPar
    }

    // The LC form stores the LC type in materialLente, the material in colorLente and the modalidad in
    // notasDiseno; tipoLente is only consulted for legacy items that carried the type there.
    private suspend fun costoLc(opticaId: String, item: DispensacionItem, receta: RecetaCosto?, fecha: String): Double? {
        val tipoLc = TipoLcMapper.map(item.materialLente) ?: TipoLcMapper.map(item.tipoLente)
        if (tipoLc == null) {
            AppLogger.w(TAG, "Unmapped contact lens type for item ${item.id}; cost left missing")
            return null
        }
        val material = item.colorLente.ifBlank { receta?.lcMaterial.orEmpty() }
        if (material.isBlank()) return null
        val candidatos = costoLcDao.lookupVigente(
            opticaId = opticaId,
            tipoLc = tipoLc,
            materialLc = material,
            laboratorioId = receta?.lcLaboratorio,
            fecha = fecha,
        )
        val modalidad = ModalidadLcMapper.inferir(item.notasDiseno) ?: ModalidadLcMapper.inferir(item.tipoLente)
        return ModalidadLcMapper.elegir(candidatos, modalidad)?.costoUnitario
    }

    companion object {
        private const val TAG = "CalcularCostoItem"
        private const val ORIGEN_TIENDA = "Tienda"

        fun esLenteDeContacto(item: DispensacionItem): Boolean = item.tipoLente.contains("Contacto", ignoreCase = true)

        /** Maps product family to substrate used by costos_biselado rows. */
        fun biseladoMaterialKey(familia: String): String = when {
            familia.startsWith("Policarbonato", ignoreCase = true) -> "Policarbonato"
            familia.startsWith("Cristal", ignoreCase = true) -> "Cristal"
            familia.startsWith("Trivex", ignoreCase = true) -> "Trivex"
            else -> "Resina"
        }

        fun tipoLente(esfera: Double, cilindro: Double?): String =
            if (abs(esfera) > 6.00 || abs(cilindro ?: 0.0) > 6.00) {
                LensCatalogRules.FABRICACION
            } else {
                LensCatalogRules.STOCK
            }

        fun seriePorCilindro(cilindro: Double?): Int? = LensCatalogRules.seriePorCilindro(cilindro)

        fun tipoAroMatriz(tipoAro: String): String = when {
            tipoAro.contains("Completo", ignoreCase = true) -> "aro_completo"
            tipoAro.contains("Semi", ignoreCase = true) -> "semi_aire"
            tipoAro.contains("aire", ignoreCase = true) -> "al_aire"
            else -> "aro_completo"
        }
    }
}
