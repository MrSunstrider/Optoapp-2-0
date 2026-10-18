package com.example.optoapp.viewmodel

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.domain.OrigenBiselado
import com.example.optoapp.domain.ReclamarDispensacionUseCase
import com.example.optoapp.domain.ReclamoOutcome
import com.example.optoapp.domain.ReclamoStockInsuficienteException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DispensacionViewModelReclamoTest {
    private val testDispatcher = StandardTestDispatcher()
    private val originalId = "disp-original"
    private val opticaId = "optica-test"
    private val rolFlow = MutableStateFlow("admin")
    private lateinit var repository: OptoRepository
    private lateinit var reclamar: ReclamarDispensacionUseCase
    private lateinit var gastoOperativoDao: GastoOperativoDao
    private lateinit var viewModel: DispensacionViewModel

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        reclamar = mockk()
        gastoOperativoDao = mockk(relaxed = true)
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.opticaId } returns MutableStateFlow(opticaId)
        every { sessionManager.opticaRol } returns rolFlow
        viewModel = DispensacionViewModel(
            repository,
            sessionManager,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            reclamar,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            gastoOperativoDao,
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun reclamarAndAwait(total: Double = 250.0, metodo: String = "Tarjeta"): String? {
        var navigatedTo: String? = null
        viewModel.crearReclamo(originalId, "Lente rayado", total, metodo) { navigatedTo = it }
        testDispatcher.scheduler.advanceUntilIdle()
        return navigatedTo
    }

    @Test
    fun `asesor is rejected without invoking the use case`() = runTest {
        rolFlow.value = "asesor"

        val navigatedTo = reclamarAndAwait()

        assertNull(navigatedTo)
        assertTrue(viewModel.uiState.value.error!!.contains("reclamar dispensación"))
        assertFalse(viewModel.uiState.value.isLoading)
        confirmVerified(reclamar)
    }

    @Test
    fun `created claim delegates motivo total and refund method and navigates to the replacement`() = runTest {
        rolFlow.value = "gerente"
        coEvery { reclamar(originalId, opticaId, "Lente rayado", 150.0, "Yape") } returns
            ReclamoOutcome.Created("repl-1", "OT-1-R1")

        val navigatedTo = reclamarAndAwait(total = 150.0, metodo = "Yape")

        assertEquals("repl-1", navigatedTo)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 1) { reclamar(originalId, opticaId, "Lente rayado", 150.0, "Yape") }
    }

    @Test
    fun `created claim clears original linked gastos and upserts mirrors for replacement items`() = runTest {
        rolFlow.value = "gerente"
        val fecha = LocalDate.of(2026, 10, 18)
        val originalItem = DispensacionItem(
            id = "item-orig",
            dispensacionId = originalId,
            tipoLente = "Monofocal",
            materialLente = "Resina",
            tratamientos = emptyList(),
            origenMontura = "Paciente",
            opticaId = opticaId,
            origenBiselado = OrigenBiselado.TERCERO,
            costoRealBiselado = 20.0,
        )
        val replacementItem = originalItem.copy(id = "item-repl", dispensacionId = "repl-1")
        val linked = GastoOperativoEntity(
            id = "gb_item-orig",
            opticaId = opticaId,
            categoria = "Biselado",
            monto = BigDecimal("20"),
            fecha = fecha,
            origenDispensacionItemId = "item-orig",
        )
        val replacement = DispensacionOptica(
            id = "repl-1",
            pacienteId = "pac-1",
            ot = "OT-1-R1",
            fecha = fecha,
            opticaId = opticaId,
        )
        coEvery { reclamar(originalId, opticaId, any(), any(), any()) } returns
            ReclamoOutcome.Created("repl-1", "OT-1-R1")
        coEvery { repository.getDispensacionItemsByDispensacion(originalId, opticaId) } returns listOf(originalItem)
        coEvery { repository.getDispensacionItemsByDispensacion("repl-1", opticaId) } returns listOf(replacementItem)
        coEvery { repository.getDispensacionById("repl-1", opticaId) } returns Resource.Success(replacement)
        coEvery { gastoOperativoDao.getByOrigenDispensacionItemId(opticaId, "item-orig") } returns linked
        coEvery { gastoOperativoDao.getByOrigenDispensacionItemId(opticaId, "item-repl") } returns null

        val navigatedTo = reclamarAndAwait()

        assertEquals("repl-1", navigatedTo)
        assertNull(viewModel.uiState.value.error)
        coVerify(exactly = 1) { repository.deleteGastoOperativo(linked) }
        coVerify(exactly = 1) {
            repository.upsertGastoOperativo(
                match { it.origenDispensacionItemId == "item-repl" && it.descripcion == "Biselado OT OT-1-R1" },
            )
        }
    }

    @Test
    fun `claim on an already Reclamada order informs the user and does not navigate`() = runTest {
        coEvery { reclamar(originalId, opticaId, any(), any(), any()) } returns ReclamoOutcome.AlreadyTerminal("Reclamada")

        val navigatedTo = reclamarAndAwait()

        assertNull(navigatedTo)
        assertEquals("Esta orden ya fue reclamada", viewModel.uiState.value.infoMessage)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `a new claim attempt clears the previous info message`() = runTest {
        coEvery { reclamar(originalId, opticaId, any(), any(), any()) } returns ReclamoOutcome.AlreadyTerminal("Reclamada")
        reclamarAndAwait()
        val gate = CompletableDeferred<ReclamoOutcome>()
        coEvery { reclamar(originalId, opticaId, any(), any(), any()) } coAnswers { gate.await() }

        viewModel.crearReclamo(originalId, "Lente rayado", 250.0, "Tarjeta") {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.infoMessage)
        gate.complete(ReclamoOutcome.Created("repl-1", "OT-1-R1"))
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `out of stock copied frame surfaces the typed error message`() = runTest {
        val error = ReclamoStockInsuficienteException("mon-1", "Ray-Ban RB2140")
        coEvery { reclamar(originalId, opticaId, any(), any(), any()) } throws error

        val navigatedTo = reclamarAndAwait()

        assertNull(navigatedTo)
        assertEquals(error.message, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `isLoading is true while the claim runs and ignores a second request`() = runTest {
        val gate = CompletableDeferred<ReclamoOutcome>()
        coEvery { reclamar(originalId, opticaId, any(), any(), any()) } coAnswers { gate.await() }

        viewModel.crearReclamo(originalId, "Lente rayado", 250.0, "Tarjeta") {}
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.crearReclamo(originalId, "Lente rayado", 250.0, "Tarjeta") {}
        testDispatcher.scheduler.advanceUntilIdle()

        gate.complete(ReclamoOutcome.Created("repl-1", "OT-1-R1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 1) { reclamar(originalId, opticaId, any(), any(), any()) }
    }

    @Test
    fun `double tap on the claim confirmation creates one replacement`() = runTest {
        coEvery { reclamar(originalId, opticaId, any(), any(), any()) } returns ReclamoOutcome.Created("repl-1", "OT-1-R1")
        var navigations = 0

        viewModel.crearReclamo(originalId, "Lente rayado", 250.0, "Tarjeta") { navigations++ }
        viewModel.crearReclamo(originalId, "Lente rayado", 250.0, "Tarjeta") { navigations++ }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { reclamar(originalId, opticaId, any(), any(), any()) }
        assertEquals(1, navigations)
    }

    @Test
    fun `a Reclamada original exposes its replacement link`() = runTest {
        givenOrder(DispensacionOptica(id = originalId, ot = "2026-0042", pacienteId = "pac-1", fecha = LocalDate.of(2026, 9, 1), estadoEntrega = "Reclamada"))
        coEvery { repository.getDispensacionByReclamoOrigenId(originalId, opticaId) } returns
            DispensacionOptica(id = "repl-1", ot = "2026-0042-R1", pacienteId = "pac-1", fecha = LocalDate.of(2026, 9, 20), reclamoOrigenId = originalId)

        viewModel.loadDispensacion(originalId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReclamoLink(id = "repl-1", ot = "2026-0042-R1"), state.reemplazo)
        assertNull(state.reclamoOrigen)
    }

    @Test
    fun `a replacement exposes the link back to its original`() = runTest {
        val replacementId = "repl-1"
        givenOrder(DispensacionOptica(id = replacementId, ot = "2026-0042-R1", pacienteId = "pac-1", fecha = LocalDate.of(2026, 9, 20), reclamoOrigenId = originalId), replacementId)
        coEvery { repository.getDispensacionById(originalId, opticaId) } returns
            Resource.Success(DispensacionOptica(id = originalId, ot = "2026-0042", pacienteId = "pac-1", fecha = LocalDate.of(2026, 9, 1), estadoEntrega = "Reclamada"))

        viewModel.loadDispensacion(replacementId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReclamoLink(id = originalId, ot = "2026-0042"), state.reclamoOrigen)
        assertNull(state.reemplazo)
    }

    @Test
    fun `an order outside a claim exposes no links and skips the lookups`() = runTest {
        givenOrder(DispensacionOptica(id = originalId, ot = "2026-0050", pacienteId = "pac-1", fecha = LocalDate.of(2026, 9, 1), estadoEntrega = "Entregado"))

        viewModel.loadDispensacion(originalId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.reemplazo)
        assertNull(state.reclamoOrigen)
        coVerify(exactly = 0) { repository.getDispensacionByReclamoOrigenId(any(), any()) }
    }

    @Test
    fun `suggested refund method is the last credit payment method of the original`() = runTest {
        every { repository.getPagosByDispensacion(originalId, opticaId) } returns flowOf(
            listOf(
                pago("a1", "Abono", "Efectivo", LocalDate.of(2026, 7, 1)),
                pago("a2", "Abono", "Tarjeta", LocalDate.of(2026, 7, 5)),
                pago("r1", "Reembolso", "Yape", LocalDate.of(2026, 7, 9)),
            ),
        )

        assertEquals("Tarjeta", viewModel.metodoReembolsoSugerido(originalId))
    }

    @Test
    fun `suggested refund method falls back to Efectivo without credit payments`() = runTest {
        every { repository.getPagosByDispensacion(originalId, opticaId) } returns flowOf(emptyList())

        assertEquals("Efectivo", viewModel.metodoReembolsoSugerido(originalId))
    }

    @Test
    fun `suggested refund method falls back to Efectivo when reading the payments fails`() = runTest {
        every { repository.getPagosByDispensacion(originalId, opticaId) } returns flow { throw IllegalStateException("db closed") }

        assertEquals("Efectivo", viewModel.metodoReembolsoSugerido(originalId))
    }

    @Test(expected = CancellationException::class)
    fun `suggested refund method propagates cancellation`() = runTest {
        every { repository.getPagosByDispensacion(originalId, opticaId) } returns flow { throw CancellationException("cancelled") }

        viewModel.metodoReembolsoSugerido(originalId)
    }

    private fun givenOrder(order: DispensacionOptica, id: String = originalId) {
        coEvery { repository.getDispensacionById(id, opticaId) } returns Resource.Success(order)
        every { repository.getPagosByDispensacion(id, opticaId) } returns flowOf(emptyList())
        coEvery { repository.getDispensacionItemsByDispensacion(id, opticaId) } returns emptyList()
        coEvery { repository.getRegalosByDispensacionId(id, opticaId) } returns emptyList()
    }

    private fun pago(id: String, tipo: String, metodo: String, fecha: LocalDate) = Pago(
        id = id, dispensacionId = originalId, fecha = fecha, tipo = tipo, monto = 50.0, metodoPago = metodo, opticaId = opticaId,
    )
}
