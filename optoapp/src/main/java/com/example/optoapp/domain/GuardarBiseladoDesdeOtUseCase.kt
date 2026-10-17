package com.example.optoapp.domain

import com.example.optoapp.data.DatabaseTransactionRunner
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.domain.costo.LensCatalogRules
import com.example.optoapp.domain.costo.LensCatalogSelection
import com.example.optoapp.domain.costo.LensRecetaInput
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Persists a bevel cost from an OT into costos_biselado so later OTs resolve the same key.
 * Existing open rows are versioned (never rewritten) via [MatrizVigenciaUseCase].
 */
class GuardarBiseladoDesdeOtUseCase @Inject constructor(
    private val costoBiseladoDao: CostoBiseladoDao,
    private val matrizVigenciaUseCase: MatrizVigenciaUseCase,
    private val syncStateTracker: SyncStateTracker,
    private val transactionRunner: DatabaseTransactionRunner,
) {

    suspend operator fun invoke(
        opticaId: String,
        item: DispensacionItem,
        costoPorPar: Double,
        receta: RecetaCosto?,
        hoy: LocalDate = LocalDate.now(),
    ): ResultadoCostoMatriz {
        if (costoPorPar < 0.0) return ResultadoCostoMatriz.Rechazado("El costo no puede ser negativo")
        if (item.tipoAro.isBlank()) return ResultadoCostoMatriz.Rechazado("Falta el tipo de aro")

        val ojo = listOfNotNull(
            receta?.odEsf?.let { LensRecetaInput(it, receta.odCil, receta.adicion) },
            receta?.oiEsf?.let { LensRecetaInput(it, receta.oiCil, receta.adicion) },
        ).firstOrNull() ?: LensRecetaInput(esfera = 0.0, cilindro = 0.0, adicion = receta?.adicion)

        val resolution = LensCatalogRules.resolve(
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
            ojo,
        )

        val material = CalcularCostoItemUseCase.biseladoMaterialKey(item.materialLente)
        val tipoAro = CalcularCostoItemUseCase.tipoAroMatriz(item.tipoAro)
        val stockFab = resolution.stockOFabricacion
        val serie = if (stockFab == LensCatalogRules.FABRICACION) null else resolution.serie
        val altoIndice = AltoIndiceNormalizer.normalize(item.altoIndice)
            ?: AltoIndiceNormalizer.fromTratamientos(item.tratamientos)
        val fechaIso = hoy.toString()

        val candidatos = costoBiseladoDao.lookupVigente(
            opticaId = opticaId,
            material = material,
            tipoAro = tipoAro,
            stockOFabricacion = stockFab,
            serie = serie,
            fecha = fechaIso,
        ).filter { it.vigenteHasta == null && AltoIndiceNormalizer.matches(it.altoIndice, altoIndice) }

        val abierto = candidatos.firstOrNull()
        if (abierto != null) {
            return when (val r = matrizVigenciaUseCase.editarBiselado(abierto, costoPorPar, hoy, hoy)) {
                is ResultadoVigencia.Aplicado -> ResultadoCostoMatriz.Versionado(r.nuevoId)
                is ResultadoVigencia.Rechazado -> ResultadoCostoMatriz.Rechazado(
                    "Fecha mínima de vigencia: ${r.fechaMinima}",
                )
                ResultadoVigencia.YaNoVigente -> ResultadoCostoMatriz.Rechazado("La fila ya no está vigente")
            }
        }

        val id = UUID.randomUUID().toString()
        val fila = CostoBiseladoEntity(
            id = id,
            opticaId = opticaId,
            material = material,
            tipoAro = tipoAro,
            stockOFabricacion = stockFab,
            serie = serie,
            altoIndice = altoIndice,
            costoPorPar = costoPorPar,
            vigenteDesde = fechaIso,
            updatedAt = Instant.now().toString(),
        )
        transactionRunner.inTransaction {
            costoBiseladoDao.upsertAll(listOf(fila))
            syncStateTracker.markLocallyModified(opticaId, TIPO_BISELADO, id)
        }
        return ResultadoCostoMatriz.Creado(id)
    }

    companion object {
        private const val TIPO_BISELADO = "costo_biselado"
    }
}
