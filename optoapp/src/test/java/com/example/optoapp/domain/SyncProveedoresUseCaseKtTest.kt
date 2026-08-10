package com.example.optoapp.domain

import com.example.optoapp.data.CategoriaMontura
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.Proveedor
import com.example.optoapp.data.ProveedorRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SyncProveedoresUseCase DTO, reconciliation dedup, and local ID tracking tests.
 */
class SyncProveedoresUseCaseKtTest {

    // ── MockK dependencies ────────────────────────────────────────────
    private val repository = mockk<ProveedorRepository>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val database = mockk<OptoDatabase>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val conflictHelper = mockk<ConflictHelper>(relaxed = true)
    private val conflictDao = mockk<ConflictDao>(relaxed = true)
    private val networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true)

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
        fetchProveedores: suspend (String) -> List<ProveedorRemotoLookup> = { emptyList() },
        fetchCategorias: suspend (String) -> List<CategoriaRemotaLookup> = { emptyList() },
    ): SyncProveedoresUseCase = object : SyncProveedoresUseCase(
        repository, supabase, database, syncStateTracker, conflictHelper, conflictDao, networkRetryHelper,
    ) {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        override suspend fun upsertProveedoresBatch(chunk: List<ProveedorRemoto>) { /* no-op for test */ }
        override suspend fun upsertCategoriasBatch(chunk: List<CategoriaRemota>) { /* no-op for test */ }
        override suspend fun fetchRemoteProveedoresForLookup(opticaId: String): List<ProveedorRemotoLookup> =
            fetchProveedores(opticaId)
        override suspend fun fetchRemoteCategoriasForLookup(opticaId: String): List<CategoriaRemotaLookup> =
            fetchCategorias(opticaId)
    }

    // ── Existing DTO tests (unchanged) ─────────────────────────────────
    @Test
    fun proveedorEntity_allFieldsAreAccessible() {
        val p = Proveedor(
            id = "p1", nombre = "Optical Corp", ruc = "20123456789",
            telefono = "999888", email = "a@b.com", direccion = "Calle 1",
            contacto = "Juan", activo = true, opticaId = "o1",
            updatedAt = "2024-01-01T00:00:00Z",
        )
        assertEquals("p1", p.id)
        assertEquals("Optical Corp", p.nombre)
        assertEquals("20123456789", p.ruc)
        assertEquals("999888", p.telefono)
        assertEquals("a@b.com", p.email)
        assertEquals("Calle 1", p.direccion)
        assertEquals("Juan", p.contacto)
        assertTrue(p.activo)
        assertEquals("o1", p.opticaId)
        assertEquals("2024-01-01T00:00:00Z", p.updatedAt)
    }

    @Test
    fun proveedorEntity_defaultValues() {
        val p = Proveedor(
            id = "p1",
            nombre = "Test",
            ruc = "111",
            opticaId = "o1",
        )
        assertEquals("", p.telefono)
        assertEquals("", p.email)
        assertEquals("", p.direccion)
        assertEquals("", p.contacto)
        assertTrue(p.activo)
        assertTrue(p.updatedAt.isNullOrEmpty())
    }

    @Test
    fun proveedorEntity_inactiveProveedor() {
        val p = Proveedor(
            id = "p1",
            nombre = "Inactive",
            ruc = "111",
            opticaId = "o1",
            activo = false,
        )
        assertEquals(false, p.activo)
    }

    @Test
    fun categoriaMonturaEntity_allFieldsAccessible() {
        val c = CategoriaMontura(
            id = "c1",
            nombre = "Sol",
            descripcion = "Lentes de sol",
            opticaId = "o1",
        )
        assertEquals("c1", c.id)
        assertEquals("Sol", c.nombre)
        assertEquals("Lentes de sol", c.descripcion)
        assertEquals("o1", c.opticaId)
    }

    @Test
    fun categoriaMonturaEntity_defaultDescription() {
        val c = CategoriaMontura(
            id = "c1",
            nombre = "Graduada",
            opticaId = "o1",
        )
        assertEquals("Graduada", c.nombre)
        assertEquals("", c.descripcion)
    }

    @Test
    fun proveedoresSyncResult_hasCorrectFields() {
        val result = ProveedoresSyncResult(
            uploadedProveedores = 5,
            uploadedCategorias = 3,
            downloadedProveedores = 2,
            downloadedCategorias = 1,
        )
        assertEquals(5, result.uploadedProveedores)
        assertEquals(3, result.uploadedCategorias)
        assertEquals(2, result.downloadedProveedores)
        assertEquals(1, result.downloadedCategorias)
    }

    @Test
    fun proveedoresSyncResult_zeroForAll() {
        val result = ProveedoresSyncResult(0, 0, 0, 0)
        assertEquals(0, result.uploadedProveedores)
        assertEquals(0, result.uploadedCategorias)
        assertEquals(0, result.downloadedProveedores)
        assertEquals(0, result.downloadedCategorias)
    }

    @Test
    fun resourceSuccess_wrapsSyncResult() {
        val result = com.example.optoapp.data.Resource.Success(
            ProveedoresSyncResult(1, 0, 0, 0),
        )
        assertNotNull(result.data)
        assertEquals(1, result.data!!.uploadedProveedores)
    }

    // ── Phase 2 RED: uploadProveedores dedup + local ID tracking ──────

    @Test
    fun `uploadProveedores two RUCs reconciling to same remote ID produce duplicate PKs without distinctBy`() = runTest {
        val opticaId = "optica-test"
        val useCase = createUseCase(
            fetchProveedores = { _ ->
                listOf(ProveedorRemotoLookup(id = "remote-1", ruc = "20123456789", opticaId = opticaId))
            }
        )

        // Two local proveedores with same RUC reconcile to same remote ID
        coEvery { repository.getListByOptica(opticaId) } returns listOf(
            Proveedor(id = "local-A", nombre = "Proveedor A", ruc = "20123456789", opticaId = opticaId),
            Proveedor(id = "local-B", nombre = "Proveedor B", ruc = "20123456789", opticaId = opticaId),
        )

        useCase.uploadProveedores(opticaId)

        // CORRECT behavior after fix: only local-A (first-wins) is marked synced
        // RED: current code marks both local-A and local-B with remote ID "remote-1"
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "proveedor", "local-A") }
        // local-B was deduplicated → NOT marked synced
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "proveedor", "local-B") }
    }

    @Test
    fun `uploadProveedores markSynced uses local IDs not reconciled remote IDs`() = runTest {
        val opticaId = "optica-test"
        val useCase = createUseCase(
            fetchProveedores = { _ ->
                listOf(ProveedorRemotoLookup(id = "remote-99", ruc = "20999999999", opticaId = opticaId))
            }
        )

        coEvery { repository.getListByOptica(opticaId) } returns listOf(
            Proveedor(id = "local-P1", nombre = "Single Provider", ruc = "20999999999", opticaId = opticaId),
        )

        useCase.uploadProveedores(opticaId)

        // Must use original local ID "local-P1", not reconciled remote-99
        coVerify { syncStateTracker.markSynced(opticaId, "proveedor", "local-P1") }
    }

    @Test
    fun `uploadProveedores single row no reconciliation still uses local ID`() = runTest {
        val opticaId = "optica-test"
        // No remote match → reconciliation keeps local IDs as-is
        val useCase = createUseCase(
            fetchProveedores = { _ -> emptyList() }
        )

        coEvery { repository.getListByOptica(opticaId) } returns listOf(
            Proveedor(id = "local-only", nombre = "Only", ruc = "11111111111", opticaId = opticaId),
        )

        useCase.uploadProveedores(opticaId)

        coVerify { syncStateTracker.markSynced(opticaId, "proveedor", "local-only") }
    }

    // ── Phase 2 RED: uploadCategorias dedup + local ID tracking ───────

    @Test
    fun `uploadCategorias two nombres reconciling to same remote ID produce duplicate PKs without distinctBy`() = runTest {
        val opticaId = "optica-test"
        val useCase = createUseCase(
            fetchCategorias = { _ ->
                listOf(CategoriaRemotaLookup(id = "remote-C1", nombre = "Sol", opticaId = opticaId))
            }
        )

        coEvery { repository.getCategoriaListByOptica(opticaId) } returns listOf(
            CategoriaMontura(id = "local-CA", nombre = "Sol", opticaId = opticaId),
            CategoriaMontura(id = "local-CB", nombre = "Sol", opticaId = opticaId),
        )

        useCase.uploadCategorias(opticaId)

        // CORRECT behavior after fix: only local-CA (first-wins) is marked synced
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "categoria_montura", "local-CA") }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "categoria_montura", "local-CB") }
    }

    @Test
    fun `uploadCategorias markSynced uses local IDs not reconciled remote IDs`() = runTest {
        val opticaId = "optica-test"
        val useCase = createUseCase(
            fetchCategorias = { _ ->
                listOf(CategoriaRemotaLookup(id = "remote-C99", nombre = "Graduada", opticaId = opticaId))
            }
        )

        coEvery { repository.getCategoriaListByOptica(opticaId) } returns listOf(
            CategoriaMontura(id = "local-CX", nombre = "Graduada", opticaId = opticaId),
        )

        useCase.uploadCategorias(opticaId)

        // Must use original local ID "local-CX", not reconciled remote-C99
        coVerify { syncStateTracker.markSynced(opticaId, "categoria_montura", "local-CX") }
    }

    @Test
    fun `uploadCategorias nombre case insensitive reconciliation still uses local ID`() = runTest {
        val opticaId = "optica-test"
        val useCase = createUseCase(
            fetchCategorias = { _ ->
                listOf(CategoriaRemotaLookup(id = "remote-ABC", nombre = "SOL", opticaId = opticaId))
            }
        )

        coEvery { repository.getCategoriaListByOptica(opticaId) } returns listOf(
            CategoriaMontura(id = "local-case", nombre = "sol", opticaId = opticaId),
        )

        useCase.uploadCategorias(opticaId)

        // Case-insensitive reconciliation: "sol" matches "SOL" → reconciled to remote-ABC
        // markSynced must use original local ID "local-case"
        coVerify { syncStateTracker.markSynced(opticaId, "categoria_montura", "local-case") }
    }

    // ── Task 2.7: Retry wrapper test ────────────────────────────────────
    @Test
    fun `uploadProveedores chunked upsert uses retry wrapper for each chunk`() = runTest {
        val opticaId = "optica-test"
        val useCase = createUseCase()

        coEvery { repository.getListByOptica(opticaId) } returns (1..250).map { i ->
            Proveedor(
                id = "p$i", nombre = "Proveedor $i", ruc = "RUC-$i",
                opticaId = opticaId, updatedAt = "2024-01-01T00:00:00Z",
            )
        }

        useCase.uploadProveedores(opticaId)

        // 250 proveedores → UPSERT_BATCH_SIZE=200 → 2 chunks
        // Each chunk must be wrapped in retryNetwork
        coVerify(atLeast = 1) { networkRetryHelper.retryNetwork(any(), any<suspend () -> Unit>()) }
    }
}
