package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.montura.MonturaDao
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
    val lcMaterial: String? = null,
    val lcLaboratorio: String? = null,
) {
    companion object {
        fun from(evaluacion: EvaluacionClinica): RecetaCosto = RecetaCosto(
            odEsf = evaluacion.recetaOdEsf.toGraduacion(),
            odCil = evaluacion.recetaOdCil.toGraduacion(),
            oiEsf = evaluacion.recetaOiEsf.toGraduacion(),
            oiCil = evaluacion.recetaOiCil.toGraduacion(),
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
        ojos.forEach { (componente, ojo) -> componentes[componente] = costoLente(opticaId, item, ojo, fechaIso) }
        if (item.origenMontura == ORIGEN_TIENDA && item.monturaId.isNotBlank()) {
            componentes[ComponenteCosto.MONTURA] = monturaDao.getMonturaByIdForOptica(item.monturaId, opticaId)
                ?.costo?.takeIf { it > 0.0 }
        }
        if (item.tipoAro.isNotBlank()) {
            componentes[ComponenteCosto.BISELADO] = costoBiselado(opticaId, item, ojos.values, fechaIso)
        }
        return CostoItemCalculado(componentes)
    }

    private data class Ojo(val esfera: Double, val cilindro: Double?) {
        val stockOFabricacion: String = tipoLente(esfera, cilindro)
        val serie: Int? = if (stockOFabricacion == STOCK) seriePorCilindro(cilindro) else null
    }

    private fun ojosConEsfera(receta: RecetaCosto?): Map<ComponenteCosto, Ojo> = buildMap {
        receta?.odEsf?.let { put(ComponenteCosto.OD, Ojo(it, receta.odCil)) }
        receta?.oiEsf?.let { put(ComponenteCosto.OI, Ojo(it, receta.oiCil)) }
    }

    private suspend fun costoLente(opticaId: String, item: DispensacionItem, ojo: Ojo, fecha: String): Double? =
        costoProductoDao.lookupVigente(
            opticaId = opticaId,
            material = item.materialLente,
            tipoLente = item.tipoLente,
            stockOFabricacion = ojo.stockOFabricacion,
            tratamiento = item.tratamientos.sorted().joinToString(" + ").ifBlank { null },
            serie = ojo.serie,
            fecha = fecha,
        )?.costoUnitario

    private suspend fun costoBiselado(
        opticaId: String,
        item: DispensacionItem,
        ojos: Collection<Ojo>,
        fecha: String,
    ): Double? {
        val fabricacion = ojos.any { it.stockOFabricacion == FABRICACION }
        val serie = if (fabricacion) null else ojos.mapNotNull { it.serie }.maxOrNull() ?: seriePorCilindro(null)
        val altoIndice = AltoIndiceNormalizer.normalize(item.altoIndice)
            ?: AltoIndiceNormalizer.fromTratamientos(item.tratamientos)
        return costoBiseladoDao.lookupVigente(
            opticaId = opticaId,
            material = item.materialLente,
            tipoAro = tipoAroMatriz(item.tipoAro),
            stockOFabricacion = if (fabricacion) FABRICACION else STOCK,
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
        private const val STOCK = "stock"
        private const val FABRICACION = "fabricacion"

        fun esLenteDeContacto(item: DispensacionItem): Boolean = item.tipoLente.contains("Contacto", ignoreCase = true)

        fun tipoLente(esfera: Double, cilindro: Double?): String =
            if (abs(esfera) > 6.00 || abs(cilindro ?: 0.0) > 6.00) FABRICACION else STOCK

        fun seriePorCilindro(cilindro: Double?): Int? {
            val absCil = abs(cilindro ?: 0.0)
            return when {
                absCil <= 2.00 -> 1
                absCil <= 4.00 -> 2
                absCil <= 6.00 -> 3
                else -> null
            }
        }

        fun tipoAroMatriz(tipoAro: String): String = when {
            tipoAro.contains("Completo", ignoreCase = true) -> "aro_completo"
            tipoAro.contains("Semi", ignoreCase = true) -> "semi_aire"
            tipoAro.contains("aire", ignoreCase = true) -> "al_aire"
            else -> "aro_completo"
        }
    }
}
