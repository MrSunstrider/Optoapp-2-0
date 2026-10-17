package com.example.optoapp.domain

import com.example.optoapp.data.DatabaseTransactionRunner
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.domain.costo.LensCatalogRules
import com.example.optoapp.domain.costo.LensCatalogSelection
import com.example.optoapp.domain.costo.LensRecetaInput
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Persists OD/OI lens unit cost from an OT into costos_productos (same key the resolver uses).
 * Open rows are versioned via [MatrizVigenciaUseCase], never rewritten in place.
 */
class GuardarCostoLenteDesdeOtUseCase @Inject constructor(
    private val costoProductoDao: CostoProductoDao,
    private val matrizVigenciaUseCase: MatrizVigenciaUseCase,
    private val syncStateTracker: SyncStateTracker,
    private val transactionRunner: DatabaseTransactionRunner,
) {

    suspend operator fun invoke(
        opticaId: String,
        item: DispensacionItem,
        ojo: ComponenteCosto,
        costoUnitario: Double,
        receta: RecetaCosto?,
        hoy: LocalDate = LocalDate.now(),
    ): ResultadoCostoMatriz {
        if (ojo != ComponenteCosto.OD && ojo != ComponenteCosto.OI) {
            return ResultadoCostoMatriz.Rechazado("Solo OD u OI se guardan en la matriz de lentes")
        }
        if (costoUnitario < 0.0) return ResultadoCostoMatriz.Rechazado("El costo no puede ser negativo")
        if (item.materialLente.isBlank()) return ResultadoCostoMatriz.Rechazado("Falta la familia / material del lente")
        if (item.tipoLente.isBlank()) return ResultadoCostoMatriz.Rechazado("Falta el tipo de lente")

        val recetaOjo = when (ojo) {
            ComponenteCosto.OD -> LensRecetaInput(receta?.odEsf, receta?.odCil, receta?.adicion)
            ComponenteCosto.OI -> LensRecetaInput(receta?.oiEsf, receta?.oiCil, receta?.adicion)
            else -> return ResultadoCostoMatriz.Rechazado("Componente no aplicable")
        }
        if (recetaOjo.esfera == null) {
            return ResultadoCostoMatriz.Rechazado("Falta la refracción del ojo para derivar la serie")
        }

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
            recetaOjo,
        )

        val indiceRefraccion = if (resolution.requiresManualIrCost) {
            item.altoIndice?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        val curvaBase = if (resolution.requiresCurvaBase) {
            item.curvaBase?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        val subtipo = item.subTipoBifocal.takeIf { it.isNotBlank() }
        val fechaIso = hoy.toString()

        val abierto = costoProductoDao.lookupVigente(
            opticaId = opticaId,
            material = resolution.matrixFamilia,
            tipoLente = item.tipoLente,
            stockOFabricacion = resolution.stockOFabricacion,
            tratamiento = resolution.matrixTratamiento,
            serie = resolution.serie,
            fecha = fechaIso,
            subtipoBifocal = subtipo,
            indiceRefraccion = indiceRefraccion,
            curvaBase = curvaBase,
        )?.takeIf { it.vigenteHasta == null }

        if (abierto != null) {
            return when (val r = matrizVigenciaUseCase.editarProducto(abierto, costoUnitario, hoy, hoy)) {
                is ResultadoVigencia.Aplicado -> ResultadoCostoMatriz.Versionado(r.nuevoId)
                is ResultadoVigencia.Rechazado -> ResultadoCostoMatriz.Rechazado(
                    "Fecha mínima de vigencia: ${r.fechaMinima}",
                )
                ResultadoVigencia.YaNoVigente -> ResultadoCostoMatriz.Rechazado("La fila ya no está vigente")
            }
        }

        val id = UUID.randomUUID().toString()
        val fila = CostoProductoEntity(
            id = id,
            opticaId = opticaId,
            material = resolution.matrixFamilia,
            tipoLente = item.tipoLente,
            stockOFabricacion = resolution.stockOFabricacion,
            tratamiento = resolution.matrixTratamiento,
            serie = resolution.serie,
            subtipoBifocal = subtipo,
            indiceRefraccion = indiceRefraccion,
            curvaBase = curvaBase,
            costoUnitario = costoUnitario,
            vigenteDesde = fechaIso,
            updatedAt = Instant.now().toString(),
        )
        transactionRunner.inTransaction {
            costoProductoDao.upsertAll(listOf(fila))
            syncStateTracker.markLocallyModified(opticaId, TIPO_PRODUCTO, id)
        }
        return ResultadoCostoMatriz.Creado(id)
    }

    companion object {
        private const val TIPO_PRODUCTO = "costo_producto"
    }
}
