package com.example.optoapp.viewmodel

import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DateUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ResumenDiarioViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dao: ResumenDiarioDao
    private lateinit var sessionManager: SessionManager
    private lateinit var scheduler: PostSaveSyncScheduler
    private val opticaId = "optica-resumen-1"
    private val month = LocalDate.of(2026, 8, 1)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        mockkObject(DateUtils)
        every { DateUtils.today() } returns month
        dao = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        every { sessionManager.opticaId } returns flowOf(opticaId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun rowsForAugust(): List<ResumenDiarioEntity> =
        (1..10).map { day ->
            ResumenDiarioEntity(
                id = "r$day",
                opticaId = opticaId,
                fecha = "2026-08-%02d".format(day),
                ventasMontoTotal = day * 10.0,
            )
        }

    @Test
    fun monthList_showsDailyRowsOrderedByFecha() = runTest(testDispatcher) {
        every { dao.observeByOpticaAndMonth(opticaId, "2026-08") } returns flowOf(rowsForAugust())

        val vm = ResumenDiarioViewModel(dao, sessionManager, scheduler)
        advanceUntilIdle()

        assertEquals(10, vm.uiState.value.rows.size)
        assertEquals("2026-08-01", vm.uiState.value.rows.first().fecha)
        assertEquals("2026-08-10", vm.uiState.value.rows.last().fecha)
    }

    @Test
    fun refresh_schedulesFinanzasSync_withoutResumenUpload() = runTest(testDispatcher) {
        every { dao.observeByOpticaAndMonth(opticaId, "2026-08") } returns flowOf(emptyList())

        val vm = ResumenDiarioViewModel(dao, sessionManager, scheduler)
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()

        verify(exactly = 1) { scheduler.scheduleFinanzasSync(opticaId) }
        assertTrue(vm.uiState.value.message!!.contains("programada"))
    }

    @Test
    fun yearMonthOf_formatsCorrectly() {
        assertEquals("2026-08", ResumenDiarioViewModel.yearMonthOf(LocalDate.of(2026, 8, 15)))
        assertEquals("2026-01", ResumenDiarioViewModel.yearMonthOf(LocalDate.of(2026, 1, 1)))
    }
}
