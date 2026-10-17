package com.example.optoapp.domain

import com.example.optoapp.data.DatabaseTransactionRunner
import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GuardarCostoLenteDesdeOtUseCaseTest {

    private lateinit var dao: CostoProductoDao
    private lateinit var vigencia: MatrizVigenciaUseCase
    private lateinit var sync: SyncStateTracker
    private lateinit var tx: DatabaseTransactionRunner
    private lateinit var useCase: GuardarCostoLenteDesdeOtUseCase

    private val opticaId = "o1"
    private val hoy = LocalDate.of(2026, 10, 17)

    @Before
    fun setUp() {
        dao = mockk()
        vigencia = mockk()
        sync = mockk(relaxed = true)
        tx = mockk()
        coEvery { tx.inTransaction<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        useCase = GuardarCostoLenteDesdeOtUseCase(dao, vigencia, sync, tx)
    }

    private fun item() = DispensacionItem(
        id = "i1",
        dispensacionId = "d1",
        tipoLente = "Monofocal",
        materialLente = "Resina",
        distanciaLente = "Lejos",
        tratamientos = listOf("UV 400"),
        opticaId = opticaId,
    )

    @Test
    fun createsRowForOdWithSerieFromOdCylinder() = runTest {
        coEvery {
            dao.lookupVigente(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns null
        val saved = slot<List<CostoProductoEntity>>()
        coEvery { dao.upsertAll(capture(saved)) } just Runs

        val result = useCase(
            opticaId,
            item(),
            ComponenteCosto.OD,
            costoUnitario = 55.0,
            receta = RecetaCosto(odEsf = -2.0, odCil = -3.0, oiEsf = -1.0, oiCil = -0.5),
            hoy = hoy,
        )

        assertTrue(result is ResultadoCostoMatriz.Creado)
        assertEquals(55.0, saved.captured.single().costoUnitario, 0.0)
        assertEquals(2, saved.captured.single().serie) // |cil| 3 → 2da serie
        assertEquals("Resina", saved.captured.single().material)
        coVerify { sync.markLocallyModified(opticaId, "costo_producto", any()) }
    }

    @Test
    fun versionsExistingOpenRowForOi() = runTest {
        val abierto = CostoProductoEntity(
            id = "p1",
            opticaId = opticaId,
            material = "Resina",
            tipoLente = "Monofocal",
            stockOFabricacion = "stock",
            tratamiento = "UV 400",
            serie = 1,
            costoUnitario = 40.0,
            vigenteDesde = "2026-01-01",
        )
        coEvery {
            dao.lookupVigente(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns abierto
        coEvery { vigencia.editarProducto(abierto, 70.0, hoy, hoy) } returns ResultadoVigencia.Aplicado("p2")

        val result = useCase(
            opticaId,
            item(),
            ComponenteCosto.OI,
            costoUnitario = 70.0,
            receta = RecetaCosto(odEsf = -1.0, odCil = 0.0, oiEsf = -1.0, oiCil = -1.0),
            hoy = hoy,
        )

        assertEquals(ResultadoCostoMatriz.Versionado("p2"), result)
    }

    @Test
    fun rejectsWithoutSphereForEye() = runTest {
        val result = useCase(
            opticaId,
            item(),
            ComponenteCosto.OD,
            10.0,
            receta = RecetaCosto(odEsf = null, odCil = null, oiEsf = -1.0, oiCil = null),
            hoy = hoy,
        )
        assertTrue(result is ResultadoCostoMatriz.Rechazado)
    }
}
