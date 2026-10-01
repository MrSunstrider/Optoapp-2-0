package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

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
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }
        givenParentEstado("Pendiente")

        viewModel = RegaloDispensacionViewModel(repository, stockHelper)
    }

    private fun givenParentEstado(estado: String) {
        coEvery { repository.getDispensacionById(dispId, opticaId) } returns Resource.Success(
            DispensacionOptica(id = dispId, ot = "OT-1", pacienteId = "pac-1", fecha = LocalDate.of(2026, 9, 1), opticaId = opticaId, estadoEntrega = estado),
        )
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
                "reg-1",
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

        coVerify { repository.deleteRegaloById(testRegalo.id, any()) }
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
                "reg-1",
                "Reversión por eliminación de regalo",
            )
        }
    }

    @Test
    fun `saveRegaloAndDeductStock on Anulado order writes nothing and emits error`() = runTest {
        givenParentEstado("Anulado")

        viewModel.saveRegaloAndDeductStock(testRegalo, opticaId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertRegalo(any()) }
        coVerify(exactly = 0) { stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any()) }
        assertEquals("La orden está anulada y no se puede modificar regalos.", viewModel.error.value)
    }

    @Test
    fun `removeRegaloAndRestoreStock on Anulado order writes nothing and emits error`() = runTest {
        givenParentEstado("Anulado")

        viewModel.removeRegaloAndRestoreStock(testRegalo, opticaId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.deleteRegaloById(any(), any()) }
        coVerify(exactly = 0) { stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any()) }
        assertEquals("La orden está anulada y no se puede modificar regalos.", viewModel.error.value)
    }

    @Test
    fun `saveRegaloAndDeductStock on Entregado order inserts regalo inside one transaction`() = runTest {
        givenParentEstado("Entregado")

        viewModel.saveRegaloAndDeductStock(testRegalo, opticaId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            repository.withTransaction(any<suspend () -> Any?>())
            repository.getDispensacionById(dispId, opticaId)
            repository.insertRegalo(testRegalo)
            stockHelper.adjustStockAndRegistrarMovimiento("prod-1", opticaId, -2, "SALIDA_VENTA", "reg-1", "Salida por regalo")
        }
        assertNull(viewModel.error.value)
    }

    @Test
    fun `saveRegaloAndDeductStock reports insufficient stock as error instead of crashing`() = runTest {
        coEvery {
            stockHelper.adjustStockAndRegistrarMovimiento(any(), any(), any(), any(), any(), any())
        } returns Result.failure(IllegalStateException("sin stock"))

        viewModel.saveRegaloAndDeductStock(testRegalo, opticaId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Stock insuficiente para regalo: Estuche", viewModel.error.value)
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
