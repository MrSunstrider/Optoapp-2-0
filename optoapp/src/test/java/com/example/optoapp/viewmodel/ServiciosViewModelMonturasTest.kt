package com.example.optoapp.viewmodel

import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.CancelServicioExtraUseCase
import com.example.optoapp.domain.inventario.InventarioItemKind
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.util.DispensacionStockHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServiciosViewModelMonturasTest {

    private lateinit var repository: OptoRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler
    private lateinit var cancelServicioExtraUseCase: CancelServicioExtraUseCase
    private lateinit var stockHelper: DispensacionStockHelper

    private val opticaIdFlow = MutableStateFlow("optica-test")
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        sessionManager = mockk()
        postSaveSyncScheduler = mockk(relaxed = true)
        cancelServicioExtraUseCase = mockk(relaxed = true)
        stockHelper = mockk(relaxed = true)

        every { sessionManager.opticaId } returns opticaIdFlow
        coEvery { repository.reassignLegacyMiOpticaBaseTo(any()) } returns Unit
        every { repository.getAllServiciosForOptica(any()) } returns flowOf(emptyList())
        every { repository.pacientesFlowForOptica(any()) } returns flowOf(emptyList())
        every { repository.getAllPagosFlowForOptica(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun monturas_includes_active_accessories() = runTest(testDispatcher) {
        val armazon = Montura(id = "m1", marca = "Ray", modelo = "Aviator", categoria = "SOL", activo = true)
        val liquido = Montura(
            id = "m2",
            marca = "Opti",
            modelo = "Líquido",
            categoria = InventarioItemKind.ACCESORIO,
            activo = true,
        )
        every { repository.getMonturasByOptica("optica-test") } returns flowOf(listOf(armazon, liquido))

        val viewModel = ServiciosViewModel(
            repository,
            sessionManager,
            postSaveSyncScheduler,
            cancelServicioExtraUseCase,
            stockHelper,
        )
        advanceUntilIdle()

        val monturas = viewModel.monturas.first()
        assertEquals(2, monturas.size)
        assertEquals("m2", monturas.first { it.categoria == InventarioItemKind.ACCESORIO }.id)
    }
}
