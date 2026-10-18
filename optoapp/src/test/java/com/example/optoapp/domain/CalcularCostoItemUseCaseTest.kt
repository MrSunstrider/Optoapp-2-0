package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.Montura
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.domain.ComponenteCosto.BISELADO
import com.example.optoapp.domain.ComponenteCosto.LC
import com.example.optoapp.domain.ComponenteCosto.MONTURA
import com.example.optoapp.domain.ComponenteCosto.OD
import com.example.optoapp.domain.ComponenteCosto.OI
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CalcularCostoItemUseCaseTest {

    private val opticaId = "o1"
    private val fecha = LocalDate.of(2026, 7, 15)
    private val fechaIso = "2026-07-15"

    private lateinit var costoProductoDao: CostoProductoDao
    private lateinit var costoBiseladoDao: CostoBiseladoDao
    private lateinit var monturaDao: MonturaDao
    private lateinit var costoLcDao: CostoLcDao
    private lateinit var configuracionFinancieraDao: ConfiguracionFinancieraDao
    private lateinit var useCase: CalcularCostoItemUseCase

    @Before
    fun setUp() {
        costoProductoDao = mockk()
        costoBiseladoDao = mockk()
        monturaDao = mockk()
        costoLcDao = mockk()
        configuracionFinancieraDao = mockk()
        coEvery {
            costoProductoDao.lookupVigente(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns null
        coEvery { costoBiseladoDao.lookupVigente(any(), any(), any(), any(), any(), any()) } returns emptyList()
        coEvery { monturaDao.getMonturaByIdForOptica(any(), any()) } returns null
        coEvery { costoLcDao.lookupVigente(any(), any(), any(), any(), any()) } returns emptyList()
        coEvery { configuracionFinancieraDao.getByOpticaIdOnce(any()) } returns null
        useCase = CalcularCostoItemUseCase(
            costoProductoDao, costoBiseladoDao, monturaDao, costoLcDao, configuracionFinancieraDao,
        )
    }

    private fun item(
        tipoLente: String = "Monofocal",
        tratamientos: List<String> = listOf("Antireflex"),
        origenMontura: String = "Paciente",
        monturaId: String = "",
        tipoAro: String = "",
        altoIndice: String? = null,
    ) = DispensacionItem(
        id = "i1",
        dispensacionId = "d1",
        tipoLente = tipoLente,
        materialLente = "Resina",
        tratamientos = tratamientos,
        origenMontura = origenMontura,
        monturaId = monturaId,
        tipoAro = tipoAro,
        altoIndice = altoIndice,
        opticaId = opticaId,
    )

    private fun lente(costo: Double) = CostoProductoEntity(
        id = "p$costo",
        opticaId = opticaId,
        material = "Resina",
        tipoLente = "Monofocal",
        stockOFabricacion = "stock",
        costoUnitario = costo,
        vigenteDesde = "2026-01-01",
    )

    private fun biselado(altoIndice: String?, costo: Double) = CostoBiseladoEntity(
        id = "b$costo",
        opticaId = opticaId,
        material = "Resina",
        tipoAro = "aro_completo",
        stockOFabricacion = "stock",
        serie = 2,
        altoIndice = altoIndice,
        costoPorPar = costo,
        vigenteDesde = "2026-01-01",
    )

    private fun stubLente(stockOFabricacion: String, serie: Int?, costo: Double) {
        coEvery {
            costoProductoDao.lookupVigente(
                opticaId, "Resina", "Monofocal", stockOFabricacion, "Antireflex", serie, fechaIso,
                null, null, null,
            )
        } returns lente(costo)
    }

    private fun stubBiselado(vararg candidatos: CostoBiseladoEntity, serie: Int? = 2) {
        coEvery {
            costoBiseladoDao.lookupVigente(opticaId, "Resina", "aro_completo", "stock", serie, fechaIso)
        } returns candidatos.toList()
    }

    private val recetaSerie2 = RecetaCosto(odEsf = -3.0, odCil = -2.5, oiEsf = -3.0, oiCil = -2.5)

    @Test
    fun lensCostIsLookedUpPerEyeWithItsOwnSerieAtTheOtDate() = runTest {
        stubLente("stock", serie = 2, costo = 18.0)
        stubLente("stock", serie = 1, costo = 16.0)

        val result = useCase(opticaId, item(), RecetaCosto(odEsf = -3.0, odCil = -2.5, oiEsf = -1.0, oiCil = null), fecha)

        assertEquals(mapOf(OD to 18.0, OI to 16.0), result.componentes)
    }

    @Test
    fun eyeWithoutEsferaIsNotApplicableAndFabricacionUsesNullSerie() = runTest {
        stubLente("fabricacion", serie = null, costo = 30.0)

        val result = useCase(opticaId, item(), RecetaCosto(odEsf = -7.0, odCil = -1.0, oiEsf = null, oiCil = null), fecha)

        assertEquals(mapOf(OD to 30.0, OI to null), result.componentes)
    }

    @Test
    fun missingLensRuleLeavesTheComponentWithoutCost() = runTest {
        val result = useCase(opticaId, item(), recetaSerie2, fecha)

        assertEquals(mapOf(OD to null, OI to null), result.componentes)
    }

    @Test
    fun monturaCostComesFromInventoryNeverFromTheMatrix() = runTest {
        coEvery { monturaDao.getMonturaByIdForOptica("m1", opticaId) } returns Montura(id = "m1", costo = 80.0)

        val result = useCase(opticaId, item(origenMontura = "Tienda", monturaId = "m1"), null, fecha)

        assertEquals(80.0, result.componentes.getValue(MONTURA))
        coVerify(exactly = 0) {
            costoProductoDao.lookupVigente(any(), any(), "montura", any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun monturaWithZeroCostIsInventoryHitAndMissingRowIsFaltante() = runTest {
        coEvery { monturaDao.getMonturaByIdForOptica("m0", opticaId) } returns Montura(id = "m0", costo = 0.0)

        val conCero = useCase(opticaId, item(origenMontura = "Tienda", monturaId = "m0"), null, fecha)
        val inexistente = useCase(opticaId, item(origenMontura = "Tienda", monturaId = "mx"), null, fecha)

        assertEquals(0.0, conCero.componentes.getValue(MONTURA))
        assertEquals(null, inexistente.componentes[MONTURA])
    }

    @Test
    fun ojOnlySelectionDoesNotEmitOdComponent() = runTest {
        stubLente("stock", serie = 1, costo = 18.0)
        val receta = RecetaCosto(odEsf = -1.0, odCil = -1.0, oiEsf = -2.0, oiCil = -1.0)
        val result = useCase(
            opticaId,
            item().copy(ojosLente = com.example.optoapp.domain.OjosLente.OI),
            receta,
            fecha,
        )

        assertFalse(result.componentes.containsKey(OD))
        assertEquals(18.0, result.componentes.getValue(OI))
    }

    @Test
    fun propioBiseladoUsesInternalRateAndSkipsMatrix() = runTest {
        coEvery { configuracionFinancieraDao.getByOpticaIdOnce(opticaId) } returns ConfiguracionFinancieraEntity(
            opticaId = opticaId,
            costoBiseladoPropio = 12.0,
        )
        stubBiselado(biselado(null, 99.0))

        val result = useCase(
            opticaId,
            item(tipoAro = "Aro Completo").copy(origenBiselado = OrigenBiselado.PROPIO),
            recetaSerie2,
            fecha,
        )

        assertEquals(12.0, result.componentes.getValue(BISELADO))
        coVerify(exactly = 0) { costoBiseladoDao.lookupVigente(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun propioBiseladoWithZeroRateIsZeroNotFaltante() = runTest {
        coEvery { configuracionFinancieraDao.getByOpticaIdOnce(opticaId) } returns ConfiguracionFinancieraEntity(
            opticaId = opticaId,
            costoBiseladoPropio = 0.0,
        )

        val result = useCase(
            opticaId,
            item(tipoAro = "Aro Completo").copy(origenBiselado = OrigenBiselado.PROPIO),
            recetaSerie2,
            fecha,
        )

        assertEquals(0.0, result.componentes.getValue(BISELADO))
    }

    @Test
    fun biseladoUsesTheHigherSerieOfBothEyes() = runTest {
        stubBiselado(biselado(null, 14.0), serie = 3)

        val receta = RecetaCosto(odEsf = -1.0, odCil = -1.0, oiEsf = -1.0, oiCil = -5.0)
        val result = useCase(opticaId, item(tipoAro = "Aro Completo"), receta, fecha)

        assertEquals(14.0, result.componentes.getValue(BISELADO))
    }

    @Test
    fun biseladoForAFabricacionLensUsesNullSerie() = runTest {
        coEvery {
            costoBiseladoDao.lookupVigente(opticaId, "Resina", "aro_completo", "fabricacion", null, fechaIso)
        } returns listOf(biselado(null, 25.0))

        val receta = RecetaCosto(odEsf = -7.0, odCil = null, oiEsf = -2.0, oiCil = null)
        val result = useCase(opticaId, item(tipoAro = "Aro Completo"), receta, fecha)

        assertEquals(25.0, result.componentes.getValue(BISELADO))
    }

    @Test
    fun itemAltoIndiceMatchesTheNormalizedMatrixValue() = runTest {
        stubBiselado(biselado(null, 8.0), biselado("1.5", 12.0))

        val result = useCase(opticaId, item(tipoAro = "Aro Completo", altoIndice = "1.50"), recetaSerie2, fecha)

        assertEquals(12.0, result.componentes.getValue(BISELADO))
    }

    @Test
    fun standardLensMatchesOnlyRowsWithoutAltoIndice() = runTest {
        stubBiselado(biselado("1.70", 20.0), biselado(null, 10.0))

        val result = useCase(opticaId, item(tipoAro = "Aro Completo"), recetaSerie2, fecha)

        assertEquals(10.0, result.componentes.getValue(BISELADO))
    }

    @Test
    fun highIndexTreatmentMatchesOnlyItsIndexAndNeverAStandardPrice() = runTest {
        val tratamientos = listOf("Antireflex", "Alto Índice Blanco 1.7")
        stubBiselado(biselado(null, 10.0), biselado("1.70", 20.0))
        val conIndice = useCase(opticaId, item(tipoAro = "Aro Completo", tratamientos = tratamientos), recetaSerie2, fecha)

        stubBiselado(biselado(null, 10.0))
        val soloEstandar = useCase(opticaId, item(tipoAro = "Aro Completo", tratamientos = tratamientos), recetaSerie2, fecha)

        assertEquals(20.0, conIndice.componentes.getValue(BISELADO))
        assertEquals(null, soloEstandar.componentes.getValue(BISELADO))
    }

    @Test
    fun standardLensWithOnlyHighIndexRowsIsFaltante() = runTest {
        stubBiselado(biselado("1.70", 20.0), biselado("1.80", 24.0))

        val result = useCase(opticaId, item(tipoAro = "Aro Completo"), recetaSerie2, fecha)

        assertEquals(mapOf(OD to null, OI to null, BISELADO to null), result.componentes)
    }

    @Test
    fun newestMatchingBiseladoCandidateWins() = runTest {
        stubBiselado(biselado(null, 11.0), biselado(null, 9.0))

        val result = useCase(opticaId, item(tipoAro = "Aro Completo"), recetaSerie2, fecha)

        assertEquals(11.0, result.componentes.getValue(BISELADO))
    }

    private fun lcItem(tipoLc: String = "Graduado", material: String = "Hydrogel", modalidad: String = "Mensual") =
        item(tipoLente = "Lentes de Contacto", tipoAro = "Aro Completo", origenMontura = "Tienda", monturaId = "m1")
            .copy(materialLente = tipoLc, colorLente = material, notasDiseno = modalidad)

    private fun filaLc(id: String, modalidad: String, costo: Double) = CostoLcEntity(
        id = id,
        opticaId = opticaId,
        tipoLc = "graduado",
        materialLc = "Hydrogel",
        modalidad = modalidad,
        costoUnitario = costo,
        vigenteDesde = "2026-01-01",
    )

    private fun stubLc(vararg candidatos: CostoLcEntity, laboratorioId: String? = null) {
        coEvery { costoLcDao.lookupVigente(opticaId, "graduado", "Hydrogel", laboratorioId, fechaIso) } returns candidatos.toList()
    }

    @Test
    fun contactLensIsLookedUpOnCostosLcAtTheOtDateAndOnlyPricesTheLcComponent() = runTest {
        stubLc(filaLc("m", "mensual", 45.0), laboratorioId = "lab1")

        val result = useCase(opticaId, lcItem(), recetaSerie2.copy(lcLaboratorio = "lab1"), fecha)

        assertEquals(mapOf(LC to 45.0), result.componentes)
        coVerify(exactly = 0) {
            costoProductoDao.lookupVigente(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun lcModalidadFromTheItemPicksItsRow() = runTest {
        stubLc(filaLc("d", "diario", 60.0), filaLc("m", "mensual", 45.0))

        assertEquals(45.0, useCase(opticaId, lcItem(modalidad = "Mensual"), null, fecha).componentes.getValue(LC))
        assertEquals(60.0, useCase(opticaId, lcItem(modalidad = "Diario"), null, fecha).componentes.getValue(LC))
    }

    @Test
    fun unknownLcModalidadUsesOnlyAUniqueCandidate() = runTest {
        stubLc(filaLc("m", "mensual", 45.0))
        val unica = useCase(opticaId, lcItem(modalidad = ""), null, fecha)

        stubLc(filaLc("d", "diario", 60.0), filaLc("m", "mensual", 45.0))
        val varias = useCase(opticaId, lcItem(modalidad = ""), null, fecha)

        stubLc()
        val ninguna = useCase(opticaId, lcItem(modalidad = ""), null, fecha)

        assertEquals(mapOf(LC to 45.0), unica.componentes)
        assertEquals(mapOf(LC to null), varias.componentes)
        assertEquals(mapOf(LC to null), ninguna.componentes)
    }

    @Test
    fun unmappedLcTypeIsFaltanteWithoutAnyLookup() = runTest {
        val result = useCase(opticaId, lcItem(tipoLc = "desconocido"), null, fecha)

        assertEquals(mapOf(LC to null), result.componentes)
        coVerify(exactly = 0) { costoLcDao.lookupVigente(any(), any(), any(), any(), any()) }
    }

    @Test
    fun lcMaterialFallsBackToTheLinkedEvaluation() = runTest {
        stubLc(filaLc("m", "mensual", 45.0))

        val result = useCase(opticaId, lcItem(material = ""), recetaSerie2.copy(lcMaterial = "Hydrogel"), fecha)

        assertEquals(mapOf(LC to 45.0), result.componentes)
    }

    @Test
    fun manualIrLensLooksUpWithItemAltoIndice() = runTest {
        val fila = CostoProductoEntity(
            id = "ir1",
            opticaId = opticaId,
            material = "Resina antireflejo alto indice",
            tipoLente = "Monofocal",
            stockOFabricacion = "fabricacion",
            tratamiento = null,
            serie = null,
            indiceRefraccion = "1.7",
            costoUnitario = 55.0,
            vigenteDesde = "2026-01-01",
        )
        coEvery {
            costoProductoDao.lookupVigente(
                opticaId, "Resina antireflejo alto indice", "Monofocal", "fabricacion",
                null, null, fechaIso, null, "1.7", null,
            )
        } returns fila

        val result = useCase(
            opticaId,
            item(tratamientos = emptyList(), altoIndice = "1.7").copy(materialLente = "Resina antireflejo alto indice"),
            RecetaCosto(odEsf = -2.0, odCil = null, oiEsf = null, oiCil = null),
            fecha,
        )

        assertEquals(mapOf(OD to 55.0, OI to null), result.componentes)
    }

    @Test
    fun manualIrLensWithoutAltoIndiceIsFaltante() = runTest {
        coEvery {
            costoProductoDao.lookupVigente(
                opticaId, "Resina antireflejo alto indice", "Monofocal", "fabricacion",
                null, null, fechaIso, null, null, null,
            )
        } returns null

        val result = useCase(
            opticaId,
            item(tratamientos = emptyList()).copy(materialLente = "Resina antireflejo alto indice"),
            RecetaCosto(odEsf = -2.0, odCil = null, oiEsf = null, oiCil = null),
            fecha,
        )

        assertEquals(mapOf(OD to null, OI to null), result.componentes)
        coVerify {
            costoProductoDao.lookupVigente(
                opticaId, "Resina antireflejo alto indice", "Monofocal", "fabricacion",
                null, null, fechaIso, null, null, null,
            )
        }
    }
}
