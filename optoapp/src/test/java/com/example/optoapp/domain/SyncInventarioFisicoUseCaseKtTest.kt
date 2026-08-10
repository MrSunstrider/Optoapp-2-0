package com.example.optoapp.domain

import com.example.optoapp.data.InventarioFisico
import com.example.optoapp.data.InventarioFisicoDetalle
import com.example.optoapp.data.InventarioFisicoRepository
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import com.example.optoapp.domain.sync.LocalEntity
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * SyncInventarioFisicoUseCase DTO, reconciliation dedup, and local ID tracking tests.
 */
class SyncInventarioFisicoUseCaseKtTest {

    // ── MockK dependencies ────────────────────────────────────────────
    private val repository = mockk<InventarioFisicoRepository>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val database = mockk<OptoDatabase>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val conflictHelper = mockk<ConflictHelper>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        // filterConflicts passes through all local entities
        coEvery { conflictHelper.filterConflicts(any(), any(), any(), any(), any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val entities = args[3] as List<LocalEntity>
            entities
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── Test use case factory ─────────────────────────────────────────
    private fun createUseCase(
        fetchDetalles: suspend (String) -> List<IFDetalleRemotoLookup> = { emptyList() },
    ): SyncInventarioFisicoUseCase = object : SyncInventarioFisicoUseCase(
        repository, supabase, database, syncStateTracker, conflictHelper,
    ) {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        override suspend fun upsertSessionsBatch(chunk: List<IFRemoto>) { /* no-op for test */ }
        override suspend fun upsertDetallesBatch(chunk: List<IFDetalleRemoto>) { /* no-op for test */ }
        override suspend fun fetchRemoteDetallesForLookup(opticaId: String): List<IFDetalleRemotoLookup> =
            fetchDetalles(opticaId)
    }

    // ── Existing DTO tests (unchanged) ─────────────────────────────────
    @Test
    fun inventarioFisicoEntity_allFieldsAreAccessible() {
        val session = InventarioFisico(
            id = "if1",
            fecha = LocalDate.of(2026, 6, 17),
            estado = "EN_PROGRESO",
            opticaId = "o1",
            userId = "u1",
            notas = "Conteo trimestral",
        )
        assertEquals("if1", session.id)
        assertEquals(LocalDate.of(2026, 6, 17), session.fecha)
        assertEquals("EN_PROGRESO", session.estado)
        assertEquals("o1", session.opticaId)
        assertEquals("u1", session.userId)
        assertEquals("Conteo trimestral", session.notas)
    }

    @Test
    fun inventarioFisicoEntity_defaultValues() {
        val session = InventarioFisico(
            id = "if1",
            fecha = LocalDate.now(),
            opticaId = "o1",
            userId = "u1",
        )
        assertEquals("EN_PROGRESO", session.estado)
        assertEquals("", session.notas)
    }

    @Test
    fun inventarioFisicoDetalleEntity_allFieldsAreAccessible() {
        val detalle = InventarioFisicoDetalle(
            id = "d1",
            inventarioId = "if1",
            monturaId = "m1",
            stockSistema = 10,
            stockContado = 8,
            diferencia = -2,
        )
        assertEquals("d1", detalle.id)
        assertEquals("if1", detalle.inventarioId)
        assertEquals("m1", detalle.monturaId)
        assertEquals(10, detalle.stockSistema)
        assertEquals(8, detalle.stockContado)
        assertEquals(-2, detalle.diferencia)
    }

    @Test
    fun inventarioFisicoDetalleEntity_nullableFields() {
        val detalle = InventarioFisicoDetalle(
            id = "d1",
            inventarioId = "if1",
            monturaId = "m1",
            stockSistema = 5,
        )
        assertEquals(5, detalle.stockSistema)
        assertEquals(null, detalle.stockContado)
        assertEquals(null, detalle.diferencia)
    }

    @Test
    fun inventarioFisicoSyncResult_hasCorrectFields() {
        val result = InventarioFisicoSyncResult(
            uploadedSessions = 2,
            uploadedDetalles = 50,
            downloadedSessions = 1,
            downloadedDetalles = 30,
        )
        assertEquals(2, result.uploadedSessions)
        assertEquals(50, result.uploadedDetalles)
        assertEquals(1, result.downloadedSessions)
        assertEquals(30, result.downloadedDetalles)
    }

    @Test
    fun inventarioFisicoSyncResult_zeroForAll() {
        val result = InventarioFisicoSyncResult(0, 0, 0, 0)
        assertEquals(0, result.uploadedSessions)
        assertEquals(0, result.uploadedDetalles)
        assertEquals(0, result.downloadedSessions)
        assertEquals(0, result.downloadedDetalles)
    }

    @Test
    fun inventarioFisicoSyncResult_singleUploadNoDownload() {
        val result = InventarioFisicoSyncResult(
            uploadedSessions = 1,
            uploadedDetalles = 10,
            downloadedSessions = 0,
            downloadedDetalles = 0,
        )
        assertEquals(1, result.uploadedSessions)
        assertEquals(10, result.uploadedDetalles)
        assertEquals(0, result.downloadedSessions)
    }

    @Test
    fun toRemoto_detalle_passesRealOpticaId() {
        val detalle = InventarioFisicoDetalle(
            id = "d1",
            inventarioId = "if1",
            monturaId = "m1",
            stockSistema = 10,
            stockContado = 8,
            diferencia = -2,
        )
        val remoto = detalle.toRemoto("optica-real-123")
        assertEquals("optica-real-123", remoto.opticaId)
    }

    @Test
    fun toRemoto_detalle_withDifferentOpticaIds_areDistinct() {
        val detalle = InventarioFisicoDetalle(
            id = "d2",
            inventarioId = "if1",
            monturaId = "m2",
            stockSistema = 5,
        )
        val r1 = detalle.toRemoto("optica-a")
        val r2 = detalle.toRemoto("optica-b")
        assertEquals("optica-a", r1.opticaId)
        assertEquals("optica-b", r2.opticaId)
        assertEquals(r1.id, r2.id) // same entity
    }

    @Test
    fun toRemoto_detalle_allDataPreserved() {
        val detalle = InventarioFisicoDetalle(
            id = "d3",
            inventarioId = "if3",
            monturaId = "m3",
            stockSistema = 15,
            stockContado = 12,
            diferencia = -3,
        )
        val remoto = detalle.toRemoto("o3")
        assertEquals("d3", remoto.id)
        assertEquals("if3", remoto.inventarioId)
        assertEquals("m3", remoto.monturaId)
        assertEquals(15, remoto.stockSistema)
        assertEquals(12, remoto.stockContado)
        assertEquals(-3, remoto.diferencia)
        assertEquals("o3", remoto.opticaId)
    }

    // ── Phase 2 RED: uploadDetalles dedup + local ID tracking ─────────

    @Test
    fun `uploadDetalles two composite keys reconciling to same remote ID produce duplicate PKs without distinctBy`() = runTest {
        val opticaId = "optica-test"
        val sessionId = "session-IF1"
        val useCase = createUseCase(
            fetchDetalles = { _ ->
                listOf(IFDetalleRemotoLookup(id = "remote-D1", inventarioId = sessionId, monturaId = "mont-1"))
            }
        )

        // One session with two detalles that share the same composite key (inventarioId + monturaId)
        coEvery { repository.getListByOptica(opticaId) } returns listOf(
            InventarioFisico(id = sessionId, fecha = LocalDate.now(), opticaId = opticaId, userId = "u1"),
        )
        coEvery { repository.getDetalles(sessionId) } returns listOf(
            InventarioFisicoDetalle(id = "local-DA", inventarioId = sessionId, monturaId = "mont-1", stockSistema = 10),
            InventarioFisicoDetalle(id = "local-DB", inventarioId = sessionId, monturaId = "mont-1", stockSistema = 15),
        )

        useCase.uploadDetalles(opticaId)

        // CORRECT behavior after fix: only local-DA (first-wins) is marked synced
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "inventario_fisico_detalle", "local-DA") }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "inventario_fisico_detalle", "local-DB") }
    }

    @Test
    fun `uploadDetalles markSynced uses local IDs not reconciled remote IDs`() = runTest {
        val opticaId = "optica-test"
        val sessionId = "session-IF2"
        val useCase = createUseCase(
            fetchDetalles = { _ ->
                listOf(IFDetalleRemotoLookup(id = "remote-D99", inventarioId = sessionId, monturaId = "mont-X"))
            }
        )

        coEvery { repository.getListByOptica(opticaId) } returns listOf(
            InventarioFisico(id = sessionId, fecha = LocalDate.now(), opticaId = opticaId, userId = "u1"),
        )
        coEvery { repository.getDetalles(sessionId) } returns listOf(
            InventarioFisicoDetalle(id = "local-DX", inventarioId = sessionId, monturaId = "mont-X", stockSistema = 5),
        )

        useCase.uploadDetalles(opticaId)

        // Must use original local ID "local-DX", not reconciled remote-D99
        coVerify { syncStateTracker.markSynced(opticaId, "inventario_fisico_detalle", "local-DX") }
    }

    @Test
    fun `uploadDetalles no remote match keeps local ID`() = runTest {
        val opticaId = "optica-test"
        val sessionId = "session-IF3"
        val useCase = createUseCase(
            fetchDetalles = { _ -> emptyList() }
        )

        coEvery { repository.getListByOptica(opticaId) } returns listOf(
            InventarioFisico(id = sessionId, fecha = LocalDate.now(), opticaId = opticaId, userId = "u1"),
        )
        coEvery { repository.getDetalles(sessionId) } returns listOf(
            InventarioFisicoDetalle(id = "local-only", inventarioId = sessionId, monturaId = "mont-Z", stockSistema = 3),
        )

        useCase.uploadDetalles(opticaId)

        coVerify { syncStateTracker.markSynced(opticaId, "inventario_fisico_detalle", "local-only") }
    }
}
