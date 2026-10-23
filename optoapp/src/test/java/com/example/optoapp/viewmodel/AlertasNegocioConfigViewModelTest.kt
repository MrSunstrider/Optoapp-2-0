package com.example.optoapp.viewmodel

import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertasNegocioConfigViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var sessionManager: SessionManager
    private lateinit var configuracionFinancieraDao: ConfiguracionFinancieraDao
    private val opticaId = "optica-test-1"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionManager = mockk(relaxed = true)
        configuracionFinancieraDao = mockk(relaxed = true)
        every { sessionManager.opticaId } returns flowOf(opticaId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm() = AlertasNegocioConfigViewModel(sessionManager, configuracionFinancieraDao)

    @Test
    fun guardar_noConfig_setsErrorWithoutUpsert() = runTest(testDispatcher) {
        coEvery { configuracionFinancieraDao.getByOpticaIdOnce(opticaId) } returns null

        val vm = createVm()
        advanceUntilIdle()
        vm.guardar()
        advanceUntilIdle()

        val ui = vm.ui.first()
        assertEquals("No hay configuración financiera para esta óptica.", ui.error)
        assertEquals(false, ui.guardando)
        coVerify(exactly = 0) { configuracionFinancieraDao.upsert(any()) }
    }

    @Test
    fun guardar_margenBajoNotLessThanAlto_rejectsWithoutUpsert() = runTest(testDispatcher) {
        coEvery { configuracionFinancieraDao.getByOpticaIdOnce(opticaId) } returns
            ConfiguracionFinancieraEntity(opticaId = opticaId)

        val vm = createVm()
        advanceUntilIdle()
        vm.updateField { it.copy(margenBajoAlertaPct = "35", margenAltoAlertaPct = "10") }
        vm.guardar()
        advanceUntilIdle()

        val ui = vm.ui.first()
        assertTrue(ui.error!!.contains("margen bajo"))
        coVerify(exactly = 0) { configuracionFinancieraDao.upsert(any()) }
    }

    @Test
    fun guardar_validThresholds_upsertsExistingConfig() = runTest(testDispatcher) {
        val existing = ConfiguracionFinancieraEntity(opticaId = opticaId)
        coEvery { configuracionFinancieraDao.getByOpticaIdOnce(opticaId) } returns existing

        val vm = createVm()
        advanceUntilIdle()
        vm.guardar()
        advanceUntilIdle()

        val ui = vm.ui.first()
        assertNull(ui.error)
        assertEquals("Umbrales de alerta guardados.", ui.mensaje)
        coVerify(exactly = 1) { configuracionFinancieraDao.upsert(any()) }
    }
}
