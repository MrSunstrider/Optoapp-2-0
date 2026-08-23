package com.example.optoapp.viewmodel

import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.domain.ConfiguracionFinancieraDraft
import com.example.optoapp.sync.PostSaveSyncScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfiguracionFinancieraViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dao: ConfiguracionFinancieraDao
    private lateinit var sessionManager: SessionManager
    private lateinit var scheduler: PostSaveSyncScheduler
    private val opticaId = "optica-cfg-1"
    private val rolFlow = MutableStateFlow("admin")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        dao = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        every { sessionManager.opticaId } returns flowOf(opticaId)
        every { sessionManager.opticaRol } returns rolFlow
        every { dao.getByOpticaId(opticaId) } returns flowOf(
            ConfiguracionFinancieraEntity(opticaId = opticaId, margenNetoObjetivo = 15.0),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createVm() = ConfiguracionFinancieraViewModel(dao, sessionManager, scheduler)

    @Test
    fun save_adminUpsertsAndSchedulesFinanzasSync() = runTest(testDispatcher) {
        rolFlow.value = "admin"
        val vm = createVm()
        advanceUntilIdle()

        vm.updateDraft(ConfiguracionFinancieraDraft(margenNetoObjetivo = 18.0))
        vm.save()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            dao.upsert(
                match {
                    it.opticaId == opticaId && it.margenNetoObjetivo == 18.0
                },
            )
        }
        verify(exactly = 1) { scheduler.scheduleFinanzasSync(opticaId) }
        assertTrue(vm.uiState.value.saveEnabled)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun save_especialistaDenied_doesNotUpsert() = runTest(testDispatcher) {
        rolFlow.value = "especialista"
        val vm = createVm()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.saveEnabled)
        vm.save()
        advanceUntilIdle()

        coVerify(exactly = 0) { dao.upsert(any()) }
        verify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
    }

    @Test
    fun save_nullRol_failClosed() = runTest(testDispatcher) {
        rolFlow.value = ""
        val vm = createVm()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.saveEnabled)
        vm.save()
        advanceUntilIdle()
        coVerify(exactly = 0) { dao.upsert(any()) }
    }
}
