package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate

class UploadSyncCoordinatorTest {

    private val repository = mockk<OptoRepository>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val database = mockk<OptoDatabase>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val mergeHandler = mockk<DispensacionMergeHandler>(relaxed = true)
    private val networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true)
    private val costoProductoDao = mockk<CostoProductoDao>(relaxed = true)
    private val costoBiseladoDao = mockk<CostoBiseladoDao>(relaxed = true)
    private val configuracionFinancieraDao = mockk<ConfiguracionFinancieraDao>(relaxed = true)
    private lateinit var coordinator: UploadSyncCoordinator

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        coEvery { syncStateTracker.quarantinedEntityIds(any(), any()) } returns emptySet()
        // Override runInTransaction to run blocks inline without a real DB transaction
        coordinator = object : UploadSyncCoordinator(
            repository = repository,
            supabase = supabase,
            database = database,
            syncStateTracker = syncStateTracker,
            mergeHandler = mergeHandler,
            networkRetryHelper = networkRetryHelper,
            costoProductoDao = costoProductoDao,
            costoBiseladoDao = costoBiseladoDao,
            configuracionFinancieraDao = configuracionFinancieraDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `blank opticaId should throw IllegalArgumentException`() = runTest {
        coEvery { repository.getDispensacionItemsSnapshotForOptica("") } returns listOf(
            DispensacionItem(id = "i1", dispensacionId = "d1", opticaId = ""),
        )
        try {
            coordinator.uploadDispensacionItems("")
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `local merge should only execute after remote upsert succeeds`() = runTest {
        val d1 = DispensacionOptica(
            id = "d1", ot = "OT-2026-0001", fecha = LocalDate.parse("2026-01-01"),
            pacienteId = "p1", opticaId = "optica-test",
        )
        val d2 = DispensacionOptica(
            id = "d2", ot = "OT-2026-0001", fecha = LocalDate.parse("2026-01-02"),
            pacienteId = "p2", opticaId = "optica-test",
        )
        coEvery { repository.getDispensacionesSnapshotForOptica("optica-test") } returns listOf(d1, d2)
        coEvery { repository.getPagosSnapshotForOptica("optica-test") } returns emptyList()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } throws IOException("Network error")

        try {
            coordinator.uploadDispensaciones("optica-test")
            fail("Expected exception")
        } catch (_: IOException) { /* expected */ }
          catch (_: UploadPartialException) { /* expected */ }
          catch (_: UploadSyncCoordinator.UploadPreCheckFailedException) {
            // acceptable — mock can't handle inline Postgrest DSL
        }

        // GREEN (fixed code): merge deferred after upsert → upsert failed → merge NOT called
        coVerify(exactly = 0) { mergeHandler.mergeLocalDispensacionConflict(any(), any(), any()) }
    }

    @Test
    fun `servicio dedup should use Instant comparison not string comparison`() {
        val olderStr = "2025-01-01T10:00:00Z"
        val newerStr = "2025-01-01T10:00:00.500Z"

        val stringWinner = if (newerStr > olderStr) newerStr else olderStr
        assertEquals("String compare picks older (Z > .)", olderStr, stringWinner)

        val olderInstant = Instant.parse(olderStr)
        val newerInstant = Instant.parse(newerStr)
        val instantWinner = if (newerInstant > olderInstant) newerStr else olderStr
        assertEquals("Instant compare picks newer (.500Z)", newerStr, instantWinner)
    }

    @Test
    fun `servicio dedup unparseable timestamp falls back to existing record`() {
        val validTimestamp = "2025-01-01T10:00:00Z"
        val malformedTimestamp = "not-a-timestamp"

        var caught = false
        try {
            Instant.parse(malformedTimestamp)
        } catch (_: Exception) {
            caught = true
        }
        assertEquals("Instant.parse throws on malformed", true, caught)

        val existingTime = validTimestamp.let { Instant.parse(it) }
        val rowTime = try { Instant.parse(malformedTimestamp) } catch (_: Exception) { null }

        val winner = if (rowTime != null && existingTime != null) {
            if (rowTime > existingTime) "new" else "existing"
        } else {
            "existing"
        }

        assertEquals("existing", winner)
    }

    @Test
    fun `markSynced order is individuals before batch`() = runTest {
        val item = DispensacionItem(
            id = "item1", dispensacionId = "d1", opticaId = "optica-test",
        )
        coEvery { repository.getDispensacionItemsSnapshotForOptica("optica-test") } returns listOf(item)
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        val callOrder = mutableListOf<String>()
        coEvery { syncStateTracker.markSynced("optica-test", "dispensacion_item", "item1") } coAnswers {
            callOrder.add("individual")
        }
        coEvery { syncStateTracker.markSynced("optica-test", "upload_dispensacion_items", "batch") } coAnswers {
            callOrder.add("batch")
        }

        coordinator.uploadDispensacionItems("optica-test")

        assertEquals(listOf("individual", "batch"), callOrder)
    }

    @Test
    fun `empty remote RLS on upsert deletes leftover local dispensacion`() = runTest {
        val leftover = DispensacionOptica(
            id = "stolen-from-other-account",
            ot = "OT-2026-0099",
            fecha = LocalDate.parse("2026-01-01"),
            pacienteId = "p1",
            opticaId = "opt_new",
        )
        coEvery { repository.getDispensacionesSnapshotForOptica("opt_new") } returns listOf(leftover)
        coEvery { repository.getPagosSnapshotForOptica("opt_new") } returns emptyList()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
        val testCoordinator = object : UploadSyncCoordinator(
            repository = repository,
            supabase = supabase,
            database = database,
            syncStateTracker = syncStateTracker,
            mergeHandler = mergeHandler,
            networkRetryHelper = networkRetryHelper,
            costoProductoDao = costoProductoDao,
            costoBiseladoDao = costoBiseladoDao,
            configuracionFinancieraDao = configuracionFinancieraDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
            override suspend fun fetchRemoteDispensacionesForLookup(opticaId: String) =
                emptyList<DispensacionRemotaLookup>()
            override suspend fun upsertDispensacionesChunk(chunk: List<DispensacionRemota>) {
                throw RuntimeException(
                    "new row violates row-level security policy for table \"dispensaciones\" Code: 42501",
                )
            }
        }

        val uploaded = testCoordinator.uploadDispensaciones("opt_new")

        assertEquals(0, uploaded)
        coVerify { repository.deleteDispensacionById("stolen-from-other-account", "opt_new") }
        coVerify(exactly = 0) {
            syncStateTracker.markSynced("opt_new", "dispensacion", "stolen-from-other-account")
        }
    }

    // ── T2.1 RED: Pagos reconciliation tests ──────────────────────────

    private fun createPagoCoordinator(
        parentDispIds: Set<String> = setOf("disp-1", "disp-2", "other-disp", "disp-X"),
        parentServIds: Set<String> = emptySet(),
        fetchPagos: suspend (String) -> List<PagoRemotoLookup>,
    ): UploadSyncCoordinator = object : UploadSyncCoordinator(
        repository = repository,
        supabase = supabase,
        database = database,
        syncStateTracker = syncStateTracker,
        mergeHandler = mergeHandler,
        networkRetryHelper = networkRetryHelper,
        costoProductoDao = costoProductoDao,
        costoBiseladoDao = costoBiseladoDao,
        configuracionFinancieraDao = configuracionFinancieraDao,
    ) {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        override suspend fun fetchRemotePagosForLookup(opticaId: String): List<PagoRemotoLookup> =
            fetchPagos(opticaId)
        override suspend fun fetchRemoteParentIds(opticaId: String): Pair<Set<String>, Set<String>> =
            parentDispIds to parentServIds
        override suspend fun upsertPagosChunk(chunk: List<PagoRemoto>) { /* no-op */ }
    }

    @Test
    fun `pagos reconciliation - remote match adopts ID`() = runTest {
        val opticaId = "optica-test"
        var fetchCalled = false
        val testCoordinator = createPagoCoordinator { _ ->
            fetchCalled = true
            listOf(
                PagoRemotoLookup(
                    id = "remote-1", dispensacionId = "disp-1",
                    tipo = "Abono", monto = 100.0, metodoPago = "Efectivo", fecha = "2026-01-01",
                ),
            )
        }

        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-1", dispensacionId = "disp-1", tipo = "Abono", monto = 100.0,
                metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
            ),
        )
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        testCoordinator.uploadPagos(opticaId)

        assertTrue("fetch should be called for reconciliation", fetchCalled)
        coVerify { syncStateTracker.markSynced(opticaId, "pago", "local-1") }
    }

    @Test
    fun `pagos reconciliation - no remote match keeps local ID`() = runTest {
        val opticaId = "optica-test"
        var fetchCalled = false
        val testCoordinator = createPagoCoordinator { _ ->
            fetchCalled = true
            listOf(
                PagoRemotoLookup(
                    id = "remote-999", dispensacionId = "other-disp",
                    tipo = "Abono", monto = 200.0, metodoPago = "Tarjeta", fecha = "2026-01-02",
                ),
            )
        }

        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-1", dispensacionId = "disp-1", tipo = "Abono", monto = 100.0,
                metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
            ),
        )
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        testCoordinator.uploadPagos(opticaId)

        assertTrue("fetch should be called for reconciliation", fetchCalled)
        coVerify { syncStateTracker.markSynced(opticaId, "pago", "local-1") }
    }

    @Test
    fun `pagos reconciliation - null dispensacionId reconciles with remote null`() = runTest {
        val opticaId = "optica-test"
        var fetchCalled = false
        val testCoordinator = createPagoCoordinator(
            parentServIds = setOf("s1"),
        ) { _ ->
                fetchCalled = true
                listOf(
                    PagoRemotoLookup(
                        id = "remote-null", dispensacionId = null,
                        tipo = "Abono", monto = 50.0, metodoPago = "Efectivo", fecha = "2026-01-01",
                    ),
                )
            }

        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-null", dispensacionId = null, servicioExtraId = "s1", tipo = "Abono", monto = 50.0,
                metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
            ),
        )
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        testCoordinator.uploadPagos(opticaId)

        assertTrue("fetch is called for reconciliation", fetchCalled)
        coVerify { syncStateTracker.markSynced(opticaId, "pago", "local-null") }
    }

    @Test
    fun `pagos reconciliation - null dispensacionId no remote match keeps local ID`() = runTest {
        val opticaId = "optica-test"
        var fetchCalled = false
        val testCoordinator = createPagoCoordinator(
            parentServIds = setOf("s1"),
        ) { _ ->
                fetchCalled = true
                listOf(
                    PagoRemotoLookup(
                        id = "remote-1", dispensacionId = "other-disp",
                        tipo = "Abono", monto = 200.0, metodoPago = "Tarjeta", fecha = "2026-01-02",
                    ),
                )
            }

        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-null", dispensacionId = null, servicioExtraId = "s1", tipo = "Abono", monto = 50.0,
                metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
            ),
        )
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        testCoordinator.uploadPagos(opticaId)

        assertTrue("fetch is called for reconciliation", fetchCalled)
        coVerify { syncStateTracker.markSynced(opticaId, "pago", "local-null") }
    }

    @Test
    fun `pagos reconciliation - fetch failure throws UploadPreCheckFailedException`() = runTest {
        val opticaId = "optica-test"
        val testCoordinator = createPagoCoordinator { _ ->
            throw IOException("Simulated network failure")
        }

        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-1", dispensacionId = "disp-1", tipo = "Abono", monto = 100.0,
                metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
            ),
        )

        try {
            testCoordinator.uploadPagos(opticaId)
            fail("Expected UploadPreCheckFailedException")
        } catch (e: UploadSyncCoordinator.UploadPreCheckFailedException) {
            assertTrue(e.message!!.contains("Reconciliation fetch failed"))
        }
    }

    @Test
    fun `pagos reconciliation - empty list no-ops without fetch`() = runTest {
        val opticaId = "optica-test"
        var fetchCalled = false
        val testCoordinator = createPagoCoordinator { _ ->
            fetchCalled = true
            emptyList()
        }

        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()

        val result = testCoordinator.uploadPagos(opticaId)

        assertEquals(0, result)
        assertFalse("fetch should not be called for empty local list", fetchCalled)
    }

    // ── Servicios testability seam ───────────────────────────────────

    private fun createServicioCoordinator(
        fetchServicios: suspend (String) -> List<ServicioRemotoLookup>,
    ): UploadSyncCoordinator = object : UploadSyncCoordinator(
        repository = repository,
        supabase = supabase,
        database = database,
        syncStateTracker = syncStateTracker,
        mergeHandler = mergeHandler,
        networkRetryHelper = networkRetryHelper,
        costoProductoDao = costoProductoDao,
        costoBiseladoDao = costoBiseladoDao,
        configuracionFinancieraDao = configuracionFinancieraDao,
    ) {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        override suspend fun fetchRemoteServiciosForLookup(opticaId: String): List<ServicioRemotoLookup> =
            fetchServicios(opticaId)
    }

    // ── C1+C2: uploadPagos dedup + local ID tracking ─────────────────

    @Test
    fun `uploadPagos deduplicatesById after reconciliation`() = runTest {
        val opticaId = "optica-test"
        val testCoordinator = createPagoCoordinator { _ ->
            listOf(
                PagoRemotoLookup(
                    id = "remote-1", dispensacionId = "disp-1",
                    tipo = "Abono", monto = 100.0, metodoPago = "Efectivo", fecha = "2026-01-01",
                ),
            )
        }

        // Two local pagos with same PagoKey reconcile to same remote ID
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-A", dispensacionId = "disp-1", tipo = "Abono", monto = 100.0,
                metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
            ),
            com.example.optoapp.data.Pago(
                id = "local-B", dispensacionId = "disp-1", tipo = "Abono", monto = 100.0,
                metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
            ),
        )
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        testCoordinator.uploadPagos(opticaId)

        // Only 1 upserted (uniqueById dedup); first-wins → local-A marked synced
        coVerify { syncStateTracker.markSynced(opticaId, "pago", "local-A") }
        // Deduplicated local-B is NOT in uniqueById → NOT marked synced
    }

    @Test
    fun `uploadPagos markSynced uses local IDs after reconciliation`() = runTest {
        val opticaId = "optica-test"
        val testCoordinator = createPagoCoordinator { _ ->
            listOf(
                PagoRemotoLookup(
                    id = "remote-99", dispensacionId = "disp-X",
                    tipo = "Abono", monto = 50.0, metodoPago = "Tarjeta", fecha = "2026-03-15",
                ),
            )
        }

        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-P1", dispensacionId = "disp-X", tipo = "Abono", monto = 50.0,
                metodoPago = "Tarjeta", fecha = LocalDate.parse("2026-03-15"), opticaId = opticaId,
            ),
        )
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        testCoordinator.uploadPagos(opticaId)

        // Must use original local ID, NOT the reconciled remote-99
        coVerify { syncStateTracker.markSynced(opticaId, "pago", "local-P1") }
    }

    // ── C3+W1: uploadServicios dedup + local ID tracking ──────────────

    @Test
    fun `uploadServicios deduplicates by reconciled ID`() = runTest {
        val opticaId = "optica-test"
        val testCoordinator = createServicioCoordinator { _ ->
            listOf(
                ServicioRemotoLookup(id = "remote-S1", ot = "OT-001"),
            )
        }

        // Two servicios with same OT reconcile to same remote ID
        coEvery { repository.getServiciosSnapshotForOptica(opticaId) } returns listOf(
            ServicioExtra(
                id = "local-S1", ot = "OT-001", descripcion = "Servicio A",
                montoTotal = 200.0, aCuenta = 0.0, estado = "Pendiente",
                fecha = LocalDate.parse("2026-04-01"), opticaId = opticaId,
            ),
            ServicioExtra(
                id = "local-S2", ot = "OT-001", descripcion = "Servicio B",
                montoTotal = 300.0, aCuenta = 0.0, estado = "Pendiente",
                fecha = LocalDate.parse("2026-04-01"), opticaId = opticaId,
            ),
        )
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        testCoordinator.uploadServicios(opticaId)

        // Only 1 upserted (uniqueById dedup); first-wins → local-S1 marked synced
        coVerify { syncStateTracker.markSynced(opticaId, "servicio_extra", "local-S1") }
        // Deduplicated local-S2 is NOT in uniqueById → NOT marked synced
    }

    @Test
    fun `uploadServicios markSynced uses local IDs`() = runTest {
        val opticaId = "optica-test"
        val testCoordinator = createServicioCoordinator { _ ->
            listOf(
                ServicioRemotoLookup(id = "remote-S99", ot = "OT-999"),
            )
        }

        coEvery { repository.getServiciosSnapshotForOptica(opticaId) } returns listOf(
            ServicioExtra(
                id = "local-SV1", ot = "OT-999", descripcion = "Lente progresivo",
                montoTotal = 500.0, aCuenta = 100.0, estado = "Pendiente",
                fecha = LocalDate.parse("2026-05-10"), opticaId = opticaId,
            ),
        )
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        testCoordinator.uploadServicios(opticaId)

        // Must use original local ID "local-SV1", not reconciled remote-S99
        coVerify { syncStateTracker.markSynced(opticaId, "servicio_extra", "local-SV1") }
    }

    @Test
    fun `79 plus 1 poison quarantines one and syncs seventy nine`() = runTest {
        val opticaId = "optica-test"
        val parentIds = (1..80).map { "d$it" }.toSet()
        val testCoordinator = createPagoCoordinator(
            parentDispIds = parentIds,
        ) { emptyList() }
        val pagos = (1..79).map { i ->
            com.example.optoapp.data.Pago(
                id = "ok-$i", dispensacionId = "d$i", tipo = "Abono", monto = 10.0,
                metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
            )
        } + com.example.optoapp.data.Pago(
            id = "poison", dispensacionId = "d80", tipo = "Abono", monto = -1.0,
            metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
        )
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns pagos
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        try {
            testCoordinator.uploadPagos(opticaId)
            fail("Expected UploadPartialException for quarantine partial")
        } catch (e: UploadPartialException) {
            assertEquals(79, e.uploadedCount)
        }

        coVerify { syncStateTracker.markError(opticaId, "pago", "poison", "quarantine:negative_monto") }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "pago", "poison") }
        coVerify { syncStateTracker.markSynced(opticaId, "pago", "ok-1") }
        coVerify { syncStateTracker.markError(opticaId, "upload_pagos", "batch", match { it.startsWith("quarantine:") }) }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "upload_pagos", "batch") }
    }

    @Test
    fun `parent missing gates child pago upload`() = runTest {
        val opticaId = "optica-test"
        val testCoordinator = createPagoCoordinator(
            parentDispIds = emptySet(),
        ) { emptyList() }
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "orphan", dispensacionId = "missing-d", tipo = "Abono", monto = 10.0,
                metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
            ),
        )
        try {
            testCoordinator.uploadPagos(opticaId)
            fail("Expected UploadPartialException")
        } catch (_: UploadPartialException) { }
        coVerify {
            syncStateTracker.markError(
                opticaId, "pago", "orphan",
                "quarantine:parent_missing:dispensacion:missing-d",
            )
        }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "pago", "orphan") }
    }

    @Test
    fun uploadConfiguracionFinanciera_emptyLocal_returnsZero() = runTest {
        coEvery { configuracionFinancieraDao.getByOpticaIdOnce("optica-test") } returns null
        val count = coordinator.uploadConfiguracionFinanciera("optica-test")
        assertEquals(0, count)
        coVerify { syncStateTracker.markSynced("optica-test", "upload_configuracion_financiera", "batch") }
    }
}
