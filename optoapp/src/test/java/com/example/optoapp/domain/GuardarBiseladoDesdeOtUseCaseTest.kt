package com.example.optoapp.domain

import com.example.optoapp.data.DatabaseTransactionRunner
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GuardarBiseladoDesdeOtUseCaseTest {

    private lateinit var dao: CostoBiseladoDao
    private lateinit var vigencia: MatrizVigenciaUseCase
    private lateinit var sync: SyncStateTracker
    private lateinit var tx: DatabaseTransactionRunner
    private lateinit var useCase: GuardarBiseladoDesdeOtUseCase

    private val opticaId = "o1"
    private val hoy = LocalDate.of(2026, 10, 17)

    @Before
    fun setUp() {
        dao = mockk()
        vigencia = mockk()
        sync = mockk(relaxed = true)
        tx = mockk()
        coEvery { tx.inTransaction<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        useCase = GuardarBiseladoDesdeOtUseCase(dao, vigencia, sync, tx)
    }

    private fun item() = DispensacionItem(
        id = "i1",
        dispensacionId = "d1",
        tipoLente = "Monofocal",
        materialLente = "Resina antireflejo",
        distanciaLente = "Lejos",
        tipoAro = "Aro Completo",
        opticaId = opticaId,
    )

    @Test
    fun createsNewRowWhenNoOpenMatch() = runTest {
        coEvery { dao.lookupVigente(any(), any(), any(), any(), any(), any()) } returns emptyList()
        val saved = slot<List<CostoBiseladoEntity>>()
        coEvery { dao.upsertAll(capture(saved)) } just Runs

        val result = useCase(
            opticaId,
            item(),
            costoPorPar = 25.0,
            receta = RecetaCosto(odEsf = -1.0, odCil = -0.5, oiEsf = -1.0, oiCil = -0.5),
            hoy = hoy,
        )

        assertTrue(result is ResultadoCostoMatriz.Creado)
        assertEquals(25.0, saved.captured.single().costoPorPar, 0.0)
        assertEquals("Resina", saved.captured.single().material)
        assertEquals("aro_completo", saved.captured.single().tipoAro)
        coVerify { sync.markLocallyModified(opticaId, "costo_biselado", any()) }
    }

    @Test
    fun versionsExistingOpenRow() = runTest {
        val abierto = CostoBiseladoEntity(
            id = "b1",
            opticaId = opticaId,
            material = "Resina",
            tipoAro = "aro_completo",
            stockOFabricacion = "stock",
            serie = 1,
            costoPorPar = 10.0,
            vigenteDesde = "2026-01-01",
        )
        coEvery { dao.lookupVigente(any(), any(), any(), any(), any(), any()) } returns listOf(abierto)
        coEvery { vigencia.editarBiselado(abierto, 40.0, hoy, hoy) } returns ResultadoVigencia.Aplicado("b2")

        val result = useCase(
            opticaId,
            item(),
            costoPorPar = 40.0,
            receta = RecetaCosto(odEsf = -1.0, odCil = -0.5, oiEsf = null, oiCil = null),
            hoy = hoy,
        )

        assertEquals(ResultadoCostoMatriz.Versionado("b2"), result)
    }

    @Test
    fun rejectsMissingTipoAro() = runTest {
        val result = useCase(
            opticaId,
            item().copy(tipoAro = ""),
            costoPorPar = 10.0,
            receta = null,
            hoy = hoy,
        )
        assertTrue(result is ResultadoCostoMatriz.Rechazado)
    }

    @Test
    fun writebackUsesTheHigherSerieOfBothEyes() = runTest {
        coEvery { dao.lookupVigente(any(), any(), any(), any(), any(), any()) } returns emptyList()
        val saved = slot<List<CostoBiseladoEntity>>()
        coEvery { dao.upsertAll(capture(saved)) } just Runs

        // OD cil -1 → serie 1; OI cil -5 → serie 3 (mirrors CalcularCostoItemUseCaseTest)
        val result = useCase(
            opticaId,
            item(),
            costoPorPar = 14.0,
            receta = RecetaCosto(odEsf = -1.0, odCil = -1.0, oiEsf = -1.0, oiCil = -5.0),
            hoy = hoy,
        )

        assertTrue(result is ResultadoCostoMatriz.Creado)
        assertEquals("stock", saved.captured.single().stockOFabricacion)
        assertEquals(3, saved.captured.single().serie)
        coVerify {
            dao.lookupVigente(opticaId, "Resina", "aro_completo", "stock", 3, hoy.toString())
        }
    }

    @Test
    fun writebackForAnyEyeFabricacionUsesNullSerie() = runTest {
        coEvery { dao.lookupVigente(any(), any(), any(), any(), any(), any()) } returns emptyList()
        val saved = slot<List<CostoBiseladoEntity>>()
        coEvery { dao.upsertAll(capture(saved)) } just Runs

        // OD |esf|>6 → fabricacion; OI is stock — writeback must still be fabricacion/null
        val result = useCase(
            opticaId,
            item(),
            costoPorPar = 25.0,
            receta = RecetaCosto(odEsf = -7.0, odCil = null, oiEsf = -2.0, oiCil = null),
            hoy = hoy,
        )

        assertTrue(result is ResultadoCostoMatriz.Creado)
        assertEquals("fabricacion", saved.captured.single().stockOFabricacion)
        assertEquals(null, saved.captured.single().serie)
        coVerify {
            dao.lookupVigente(opticaId, "Resina", "aro_completo", "fabricacion", null, hoy.toString())
        }
    }
}
