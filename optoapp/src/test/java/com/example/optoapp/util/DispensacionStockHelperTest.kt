package com.example.optoapp.util

import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.Resource
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DispensacionStockHelperTest {

    private lateinit var coordinator: MonturaInventoryCoordinator
    private lateinit var helper: DispensacionStockHelper

    @Before
    fun setUp() {
        coordinator = mockk(relaxed = true)
        helper = DispensacionStockHelper(coordinator)
    }

    @Test
    fun adjustStock_reduceStock_returnsAffectedRows() = runTest {
        val montura = Montura(id = "m1", opticaId = "o1", stockActual = 5)
        coEvery { coordinator.getMonturaById("m1") } returns Resource.Success(montura)
        coEvery { coordinator.adjustMonturaStock("m1", "o1", -1) } returns 1

        val result = helper.adjustStock("m1", "o1", -1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        coVerify { coordinator.adjustMonturaStock("m1", "o1", -1) }
    }

    @Test
    fun adjustStock_insufficientStock_returnsFailure() = runTest {
        val montura = Montura(id = "m1", opticaId = "o1", stockActual = 0)
        coEvery { coordinator.getMonturaById("m1") } returns Resource.Success(montura)

        val result = helper.adjustStock("m1", "o1", -1)

        assertTrue(result.isFailure)
    }

    @Test
    fun adjustStock_increaseStock_returnsAffectedRows() = runTest {
        val montura = Montura(id = "m1", opticaId = "o1", stockActual = 3)
        coEvery { coordinator.getMonturaById("m1") } returns Resource.Success(montura)
        coEvery { coordinator.adjustMonturaStock("m1", "o1", 5) } returns 1

        val result = helper.adjustStock("m1", "o1", 5)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        coVerify { coordinator.adjustMonturaStock("m1", "o1", 5) }
    }

    @Test
    fun adjustStock_nonExistentMontura_returnsFailure() = runTest {
        coEvery { coordinator.getMonturaById("nonexistent") } returns Resource.Error("No encontrada")

        val result = helper.adjustStock("nonexistent", "o1", -1)

        assertTrue(result.isFailure)
    }

    @Test
    fun adjustStock_wrongOpticaId_returnsFailure() = runTest {
        val montura = Montura(id = "m1", opticaId = "o1", stockActual = 5)
        coEvery { coordinator.getMonturaById("m1") } returns Resource.Success(montura)

        val result = helper.adjustStock("m1", "o2", -1)

        assertTrue(result.isFailure)
    }

    @Test
    fun registrarMovimiento_insertsThroughCoordinator() = runTest {
        helper.registrarMovimiento(
            monturaId = "m1",
            opticaId = "o1",
            tipo = "SALIDA_VENTA",
            cantidad = 1,
            stockPrevio = 5,
            referenciaId = "disp-1",
            nota = "Salida por venta"
        )

        coVerify { coordinator.insertMonturaMovimiento(match { mov ->
            mov.monturaId == "m1" && mov.tipo == "SALIDA_VENTA" &&
            mov.cantidad == 1 && mov.stockPrevio == 5 &&
            mov.referenciaId == "disp-1"
        }) }
    }

    @Test
    fun adjustStockAndRegistrarMovimiento_combinesBoth() = runTest {
        val montura = Montura(id = "m1", opticaId = "o1", stockActual = 10)
        coEvery { coordinator.getMonturaById("m1") } returns Resource.Success(montura)
        coEvery { coordinator.adjustMonturaStock("m1", "o1", -1) } returns 1

        val result = helper.adjustStockAndRegistrarMovimiento(
            monturaId = "m1",
            opticaId = "o1",
            delta = -1,
            tipo = "SALIDA_VENTA",
            referenciaId = "disp-1",
            nota = "Venta"
        )

        assertTrue(result.isSuccess)
        coVerify { coordinator.adjustMonturaStock("m1", "o1", -1) }
        coVerify { coordinator.insertMonturaMovimiento(match { mov ->
            mov.monturaId == "m1" && mov.referenciaId == "disp-1"
        }) }
    }

    @Test
    fun adjustStockAndRegistrarMovimiento_insufficientStock_noMovimiento() = runTest {
        val montura = Montura(id = "m1", opticaId = "o1", stockActual = 0)
        coEvery { coordinator.getMonturaById("m1") } returns Resource.Success(montura)

        val result = helper.adjustStockAndRegistrarMovimiento(
            monturaId = "m1",
            opticaId = "o1",
            delta = -1,
            tipo = "SALIDA_VENTA",
            referenciaId = "disp-1",
            nota = "Venta"
        )

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { coordinator.insertMonturaMovimiento(any()) }
    }

    @Test
    fun adjustStockAndRegistrarMovimiento_positiveDelta_succeeds() = runTest {
        val montura = Montura(id = "m1", opticaId = "o1", stockActual = 5)
        coEvery { coordinator.getMonturaById("m1") } returns Resource.Success(montura)
        coEvery { coordinator.adjustMonturaStock("m1", "o1", 3) } returns 1

        val result = helper.adjustStockAndRegistrarMovimiento(
            monturaId = "m1",
            opticaId = "o1",
            delta = 3,
            tipo = "AJUSTE",
            referenciaId = "ref-ajuste",
            nota = "Ajuste de inventario"
        )

        assertTrue(result.isSuccess)
        coVerify { coordinator.adjustMonturaStock("m1", "o1", 3) }
        coVerify { coordinator.insertMonturaMovimiento(any()) }
    }

    @Test
    fun adjustStock_loadingState_returnsFailure() = runTest {
        coEvery { coordinator.getMonturaById("m1") } returns Resource.Loading()

        val result = helper.adjustStock("m1", "o1", -1)

        assertTrue(result.isFailure)
    }

    @Test
    fun adjustStockAndRegistrarMovimiento_loadingState_returnsFailure() = runTest {
        coEvery { coordinator.getMonturaById("m1") } returns Resource.Loading()

        val result = helper.adjustStockAndRegistrarMovimiento(
            monturaId = "m1",
            opticaId = "o1",
            delta = -1,
            tipo = "SALIDA_VENTA",
            referenciaId = "disp-1",
            nota = "Venta"
        )

        assertTrue(result.isFailure)
    }
}
