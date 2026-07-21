package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegaloDispensacionViewModelTest {

    private lateinit var repository: OptoRepository
    private lateinit var stockHelper: DispensacionStockHelper
    private lateinit var viewModel: RegaloDispensacionViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val opticaId = "optica-test"
    private val dispId = "disp-1"

    private val testRegalo = RegaloDispensacionEntity(
        id = "reg-1",
        dispensacionId = dispId,
        productoId = "prod-1",
        cantidad = 2,
        costoUnitario = 10.0,
        descripcion = "Estuche",
        motivo = "Cortesía",
        opticaId = opticaId,
    )

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        stockHelper = mockk(relaxed = true)

        coEvery {
            stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any())
        } returns Result.success(1)

        viewModel = RegaloDispensacionViewModel(repository, stockHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveRegaloAndDeductStock inserts regalo via repository`() = runTest {
        viewModel.saveRegaloAndDeductStock(testRegalo, opticaId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.insertRegalo(testRegalo) }
    }

    @Test
    fun `saveRegaloAndDeductStock deducts stock with negative delta`() = runTest {
        viewModel.saveRegaloAndDeductStock(testRegalo, opticaId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            stockHelper.adjustStockAndRegistrarMovimiento(
                "prod-1",
                opticaId,
                -2,
                "SALIDA_VENTA",
                dispId,
                "Salida por regalo",
            )
        }
    }

    @Test
    fun `saveRegaloAndDeductStock does not deduct stock when productoId is blank`() = runTest {
        val regaloSinProducto = testRegalo.copy(productoId = "")
        viewModel.saveRegaloAndDeductStock(regaloSinProducto, opticaId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(inverse = true) {
            stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `removeRegaloAndRestoreStock deletes regalo by ID`() = runTest {
        viewModel.removeRegaloAndRestoreStock(testRegalo, opticaId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteRegaloById(testRegalo.id) }
    }

    @Test
    fun `removeRegaloAndRestoreStock restores stock with positive delta`() = runTest {
        viewModel.removeRegaloAndRestoreStock(testRegalo, opticaId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            stockHelper.adjustStockAndRegistrarMovimiento(
                "prod-1",
                opticaId,
                2,
                "AJUSTE",
                dispId,
                "Reversión por eliminación de regalo",
            )
        }
    }

    @Test
    fun `removeRegaloAndRestoreStock does not restore stock when productoId is blank`() = runTest {
        val regaloSinProducto = testRegalo.copy(productoId = "")
        viewModel.removeRegaloAndRestoreStock(regaloSinProducto, opticaId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(inverse = true) {
            stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any())
        }
    }
}
