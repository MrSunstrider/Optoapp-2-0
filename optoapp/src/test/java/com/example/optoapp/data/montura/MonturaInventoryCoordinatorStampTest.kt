package com.example.optoapp.data.montura

import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.Resource
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class MonturaInventoryCoordinatorStampTest {

    private val monturaDao = mockk<MonturaDao>(relaxed = true)
    private val movimientoDao = mockk<MonturaMovimientoDao>(relaxed = true)
    private val scheduler = mockk<PostSaveSyncScheduler>(relaxed = true)
    private lateinit var coordinator: MonturaInventoryCoordinator

    @Before
    fun setUp() {
        coordinator = MonturaInventoryCoordinator(monturaDao, movimientoDao, Lazy { scheduler })
    }

    @Test
    fun insertMonturaMovimiento_setsUpdatedAt() = runTest {
        val slot = slot<MonturaMovimiento>()
        coEvery { movimientoDao.insertMovimiento(capture(slot)) } returns Unit

        val mov = MonturaMovimiento(
            id = "mov-1",
            monturaId = "m1",
            fecha = LocalDate.of(2026, 8, 29),
            tipo = "ENTRADA",
            cantidad = 1,
            stockPrevio = 0,
            stockNuevo = 1,
            opticaId = "optica-1",
        )
        assertNull(mov.updatedAt)

        coordinator.insertMonturaMovimiento(mov)

        assertNotNull(slot.captured.updatedAt)
        coVerify { movimientoDao.insertMovimiento(any()) }
    }

    @Test
    fun adjustMonturaStock_passesNonBlankUpdatedAt() = runTest {
        coEvery { monturaDao.adjustStock(any(), any(), any(), any()) } returns 1

        coordinator.adjustMonturaStock("m1", "optica-1", -1)

        coVerify {
            monturaDao.adjustStock(
                "m1",
                "optica-1",
                -1,
                match { it.isNotBlank() },
            )
        }
    }

    @Test
    fun registrarSalida_stampsMovimientoUpdatedAt() = runTest {
        coEvery { monturaDao.getMonturaByIdForOptica("m1", "optica-1") } returns Montura(
            id = "m1",
            sku = "S1",
            marca = "A",
            modelo = "X",
            color = "N",
            talla = "M",
            costo = 50.0,
            precio = 100.0,
            stockActual = 5,
            stockMinimo = 2,
            activo = true,
            opticaId = "optica-1",
        )
        coEvery { monturaDao.adjustStock(any(), any(), any(), any()) } returns 1
        val slot = slot<MonturaMovimiento>()
        coEvery { movimientoDao.insertMovimiento(capture(slot)) } returns Unit

        val result = coordinator.registrarSalida(
            monturaId = "m1",
            opticaId = "optica-1",
            cantidad = 1,
            userId = "u1",
            costoUnitario = 50.0,
            tipoDocumento = "DISPENSACION",
            referenciaId = "d1",
            nota = "venta",
        )

        assertTrue(result is Resource.Success)
        assertNotNull(slot.captured.updatedAt)
        assertTrue(slot.captured.updatedAt!!.isNotBlank())
    }

    @Test
    fun syncStockFromMovimientos_passesNonBlankUpdatedAtOnAdjust() = runTest {
        coEvery { monturaDao.getMonturasListByOptica("optica-1") } returns listOf(
            Montura(
                id = "m1",
                sku = "S1",
                marca = "A",
                modelo = "X",
                color = "N",
                talla = "M",
                costo = 50.0,
                precio = 100.0,
                stockActual = 0,
                stockMinimo = 2,
                activo = true,
                opticaId = "optica-1",
            ),
        )
        coEvery { movimientoDao.getMovimientosListByOptica("optica-1") } returns listOf(
            MonturaMovimiento(
                id = "mov-1",
                monturaId = "m1",
                fecha = LocalDate.of(2026, 8, 29),
                tipo = "ENTRADA",
                cantidad = 3,
                stockPrevio = 0,
                stockNuevo = 3,
                opticaId = "optica-1",
                updatedAt = "2026-08-29T12:00:00Z",
            ),
        )
        coEvery { monturaDao.adjustStock(any(), any(), any(), any()) } returns 1

        val result = coordinator.syncStockFromMovimientos("optica-1")

        assertTrue(result is Resource.Success)
        coVerify {
            monturaDao.adjustStock(
                "m1",
                "optica-1",
                3,
                match { it.isNotBlank() },
            )
        }
    }
}
