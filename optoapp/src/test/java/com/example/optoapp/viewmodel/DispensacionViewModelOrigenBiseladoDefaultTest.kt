package com.example.optoapp.viewmodel

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.domain.OrigenBiselado
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelOrigenBiseladoDefaultTest {

    private val testDispatcher = StandardTestDispatcher()
    private val opticaId = "optica-test"
    private lateinit var configuracionFinancieraDao: ConfiguracionFinancieraDao

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)
        configuracionFinancieraDao = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): DispensacionViewModel {
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.opticaId } returns MutableStateFlow(opticaId)
        every { sessionManager.opticaRol } returns MutableStateFlow("gerente")
        val repository = mockk<OptoRepository>(relaxed = true)
        every { repository.getMonturasByOptica(any()) } returns MutableStateFlow(emptyList())
        return DispensacionViewModel(
            repository,
            sessionManager,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            configuracionFinancieraDao,
            mockk(relaxed = true),
        )
    }

    @Test
    fun init_reseedsPristineFirstLineWithConfigOrigenDefault() = runTest {
        coEvery { configuracionFinancieraDao.getByOpticaIdOnce(opticaId) } returns ConfiguracionFinancieraEntity(
            opticaId = opticaId,
            origenBiseladoDefault = OrigenBiselado.PROPIO,
        )

        val vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        val item = vm.uiState.value.items.single()
        assertEquals(OrigenBiselado.PROPIO, item.origenBiselado)
    }
}
