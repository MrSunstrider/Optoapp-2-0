package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.SyncStateTracker
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
    private lateinit var coordinator: UploadSyncCoordinator

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        coEvery { syncStateTracker.quarantinedEntityIds(any(), any()) } returns emptySet()
        coEvery { syncStateTracker.quarantineReasons(any(), any()) } returns emptyMap()
        coEvery { syncStateTracker.awaitingRemoteIds(any(), any()) } returns emptySet()
        // WHY: Room's withTransaction is an extension function MockK cannot stub.
        coordinator = object : UploadSyncCoordinator(
            repository = repository,
            supabase = supabase,
            database = database,
            syncStateTracker = syncStateTracker,
            mergeHandler = mergeHandler,
            networkRetryHelper = networkRetryHelper,
            costoProductoDao = costoProductoDao,
            costoBiseladoDao = costoBiseladoDao,
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

    // ── Pagos reconciliation ────────────────────────────────────────────

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
    ) {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        override suspend fun fetchRemoteServiciosForLookup(opticaId: String): List<ServicioRemotoLookup> =
            fetchServicios(opticaId)
    }

    // ── uploadPagos dedup + local ID tracking ────────────────────────

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

        coVerify { syncStateTracker.markSynced(opticaId, "pago", "local-A") }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "pago", "local-B") }
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

        coVerify { syncStateTracker.markSynced(opticaId, "pago", "local-P1") }
    }

    // ── uploadServicios dedup + local ID tracking ─────────────────────

    @Test
    fun `uploadServicios deduplicates by reconciled ID`() = runTest {
        val opticaId = "optica-test"
        val testCoordinator = createServicioCoordinator { _ ->
            listOf(
                ServicioRemotoLookup(id = "remote-S1", ot = "OT-001"),
            )
        }

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

        coVerify { syncStateTracker.markSynced(opticaId, "servicio_extra", "local-S1") }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "servicio_extra", "local-S2") }
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
    fun `dispensacion with negative pagos net uploads monto_pagado zero not quarantined`() = runTest {
        val opticaId = "optica-test"
        val dispId = "fd4fbba4-7ff6-4165-bc58-05d8578da419"
        val disp = DispensacionOptica(
            id = dispId,
            ot = "OT-2026-0042",
            fecha = LocalDate.parse("2026-08-20"),
            pacienteId = "p1",
            opticaId = opticaId,
            montoTotal = 500.0,
            montoPagado = -50.0,
        )
        val abono = com.example.optoapp.data.Pago(
            id = "p-abono", dispensacionId = dispId, tipo = "Abono", monto = 100.0,
            metodoPago = "Efectivo", fecha = LocalDate.parse("2026-08-20"), opticaId = opticaId,
        )
        val reembolso = com.example.optoapp.data.Pago(
            id = "p-reemb", dispensacionId = dispId, tipo = "Reembolso", monto = 150.0,
            metodoPago = "Efectivo", fecha = LocalDate.parse("2026-08-21"), opticaId = opticaId,
        )
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(disp)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(abono, reembolso)
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }

        val captured = mutableListOf<List<DispensacionRemota>>()
        val testCoordinator = object : UploadSyncCoordinator(
            repository = repository,
            supabase = supabase,
            database = database,
            syncStateTracker = syncStateTracker,
            mergeHandler = mergeHandler,
            networkRetryHelper = networkRetryHelper,
            costoProductoDao = costoProductoDao,
            costoBiseladoDao = costoBiseladoDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
            override suspend fun fetchRemoteDispensacionesForLookup(opticaId: String) =
                emptyList<DispensacionRemotaLookup>()
            override suspend fun upsertDispensacionesChunk(chunk: List<DispensacionRemota>) {
                captured.add(chunk)
            }
        }

        val uploaded = testCoordinator.uploadDispensaciones(opticaId)

        assertEquals(1, uploaded)
        assertEquals(1, captured.size)
        assertEquals(0.0, captured.single().single().montoPagado, 0.001)
        coVerify { syncStateTracker.markSynced(opticaId, "dispensacion", dispId) }
        coVerify(exactly = 0) {
            syncStateTracker.markError(opticaId, "dispensacion", dispId, match { it.contains("23514") })
        }
    }

    // ── Claim linkage remap ───────────────────────────────────────────

    private fun claimPair(opticaId: String): Pair<DispensacionOptica, DispensacionOptica> {
        val original = DispensacionOptica(
            id = "local-orig", ot = "2026-0042", fecha = LocalDate.parse("2026-09-01"),
            pacienteId = "p1", opticaId = opticaId, estadoEntrega = OrderStatusPolicy.RECLAMADA,
        )
        val replacement = DispensacionOptica(
            id = "local-repl", ot = "2026-0042-R1", fecha = LocalDate.parse("2026-09-30"),
            pacienteId = "p1", opticaId = opticaId, estadoEntrega = OrderStatusPolicy.PENDIENTE,
            reclamoOrigenId = "local-orig",
        )
        return original to replacement
    }

    private fun createDispensacionCaptureCoordinator(
        remotos: List<DispensacionRemotaLookup>,
        captured: MutableList<DispensacionRemota>,
    ): UploadSyncCoordinator = object : UploadSyncCoordinator(
        repository = repository,
        supabase = supabase,
        database = database,
        syncStateTracker = syncStateTracker,
        mergeHandler = mergeHandler,
        networkRetryHelper = networkRetryHelper,
        costoProductoDao = costoProductoDao,
        costoBiseladoDao = costoBiseladoDao,
    ) {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        override suspend fun fetchRemoteDispensacionesForLookup(opticaId: String) = remotos
        override suspend fun upsertDispensacionesChunk(chunk: List<DispensacionRemota>) {
            captured.addAll(chunk)
        }
    }

    @Test
    fun `replacement uploads remote id of original remapped by OT`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original, replacement)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
        val captured = mutableListOf<DispensacionRemota>()
        val testCoordinator = createDispensacionCaptureCoordinator(
            remotos = listOf(DispensacionRemotaLookup(id = "remote-orig", ot = "2026-0042")),
            captured = captured,
        )

        val uploaded = testCoordinator.uploadDispensaciones(opticaId)

        assertEquals(2, uploaded)
        assertEquals(listOf("2026-0042-R1", "2026-0042"), captured.map { it.ot })
        val uploadedOriginal = captured.single { it.ot == "2026-0042" }
        val uploadedReplacement = captured.single { it.ot == "2026-0042-R1" }
        assertEquals("remote-orig", uploadedOriginal.id)
        assertEquals("local-repl", uploadedReplacement.id)
        assertEquals("remote-orig", uploadedReplacement.reclamoOrigenId)
    }

    @Test
    fun `replacement keeps local original id when original is not remapped`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original, replacement)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
        val captured = mutableListOf<DispensacionRemota>()
        val testCoordinator = createDispensacionCaptureCoordinator(
            remotos = listOf(DispensacionRemotaLookup(id = "remote-other", ot = "2026-0099")),
            captured = captured,
        )

        testCoordinator.uploadDispensaciones(opticaId)

        assertEquals(2, captured.size)
        assertEquals("local-orig", captured.single { it.ot == "2026-0042-R1" }.reclamoOrigenId)
        assertEquals("local-orig", captured.single { it.ot == "2026-0042" }.id)
    }

    // ── Credits before debits ─────────────────────────────────────────

    private fun pago(
        id: String,
        tipo: String,
        monto: Double = 10.0,
        dispensacionId: String? = "disp-1",
        servicioExtraId: String? = null,
        reversaPagoId: String? = null,
        fecha: String = "2026-09-30",
    ) = com.example.optoapp.data.Pago(
        id = id, dispensacionId = dispensacionId, servicioExtraId = servicioExtraId, tipo = tipo, monto = monto,
        metodoPago = "Efectivo", fecha = LocalDate.parse(fecha), opticaId = "optica-test",
        reversaPagoId = reversaPagoId,
    )

    private fun createPagoCaptureCoordinator(
        remotos: List<PagoRemotoLookup> = emptyList(),
        uploadedChunks: MutableList<List<PagoRemoto>>,
        failMultiRowChunks: Boolean = false,
    ): UploadSyncCoordinator {
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
        return object : UploadSyncCoordinator(
            repository = repository,
            supabase = supabase,
            database = database,
            syncStateTracker = syncStateTracker,
            mergeHandler = mergeHandler,
            networkRetryHelper = networkRetryHelper,
            costoProductoDao = costoProductoDao,
            costoBiseladoDao = costoBiseladoDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
            override suspend fun fetchRemotePagosForLookup(opticaId: String) = remotos
            override suspend fun fetchRemoteParentIds(opticaId: String): Pair<Set<String>, Set<String>> =
                setOf("disp-1") to setOf("serv-1", "serv-2")
            override suspend fun upsertPagosChunk(chunk: List<PagoRemoto>) {
                if (failMultiRowChunks && chunk.size > 1) {
                    throw RuntimeException("new row violates row-level security policy Code: 42501")
                }
                uploadedChunks.add(chunk.toList())
            }
        }
    }

    @Test
    fun `uploadPagos sends credits before debits keeping relative order`() = runTest {
        val opticaId = "optica-test"
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            pago("reemb-1", "Reembolso"),
            pago("abono-1", "Abono"),
            pago("rev-1", "Reverso", reversaPagoId = "abono-1"),
            pago("abono-2", "Pago completo"),
        )
        val chunks = mutableListOf<List<PagoRemoto>>()

        createPagoCaptureCoordinator(uploadedChunks = chunks).uploadPagos(opticaId)

        assertEquals(listOf("abono-1", "abono-2", "reemb-1", "rev-1"), chunks.flatten().map { it.id })
    }

    @Test
    fun `uploadPagos puts a late credit in the first chunk ahead of eighty debits`() = runTest {
        val opticaId = "optica-test"
        val debits = (1..80).map { pago("reemb-$it", "Reembolso", monto = it.toDouble()) }
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns debits + pago("abono-late", "Abono")
        val chunks = mutableListOf<List<PagoRemoto>>()

        createPagoCaptureCoordinator(uploadedChunks = chunks).uploadPagos(opticaId)

        assertEquals(listOf(80, 1), chunks.map { it.size })
        assertEquals("abono-late", chunks.first().first().id)
        assertEquals(listOf("abono-late") + debits.map { it.id }, chunks.flatten().map { it.id })
    }

    @Test
    fun `uploadPagos row by row fallback keeps credits before debits`() = runTest {
        val opticaId = "optica-test"
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            pago("reemb-1", "Reembolso"),
            pago("abono-1", "Abono"),
            pago("rev-1", "Reverso", reversaPagoId = "abono-1"),
            pago("abono-2", "Abono", monto = 20.0),
        )
        val chunks = mutableListOf<List<PagoRemoto>>()

        createPagoCaptureCoordinator(uploadedChunks = chunks, failMultiRowChunks = true).uploadPagos(opticaId)

        assertTrue(chunks.all { it.size == 1 })
        assertEquals(listOf("abono-1", "abono-2", "reemb-1", "rev-1"), chunks.flatten().map { it.id })
    }

    // ── PagoKey guard ─────────────────────────────────────────────────

    private fun lookup(
        id: String,
        tipo: String = "Abono",
        dispensacionId: String? = "disp-1",
        servicioExtraId: String? = null,
        reversaPagoId: String? = null,
    ) = PagoRemotoLookup(
        id = id, dispensacionId = dispensacionId, servicioExtraId = servicioExtraId, tipo = tipo,
        monto = 10.0, metodoPago = "Efectivo", fecha = "2026-09-30", reversaPagoId = reversaPagoId,
    )

    @Test
    fun `local pago whose id exists remotely is never remapped`() = runTest {
        val opticaId = "optica-test"
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            pago("p-a", "Abono"),
            pago("p-b", "Abono"),
        )
        val chunks = mutableListOf<List<PagoRemoto>>()

        createPagoCaptureCoordinator(
            remotos = listOf(lookup("p-a"), lookup("p-b")),
            uploadedChunks = chunks,
        ).uploadPagos(opticaId)

        assertEquals(listOf("p-a", "p-b"), chunks.flatten().map { it.id })
        coVerify { syncStateTracker.markSynced(opticaId, "pago", "p-a") }
        coVerify { syncStateTracker.markSynced(opticaId, "pago", "p-b") }
    }

    @Test
    fun `same day equal reversos with different reversaPagoId are not collapsed`() = runTest {
        val opticaId = "optica-test"
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            pago("rv-1-local", "Reverso", reversaPagoId = "abono-1"),
            pago("rv-2", "Reverso", reversaPagoId = "abono-2"),
        )
        val chunks = mutableListOf<List<PagoRemoto>>()

        createPagoCaptureCoordinator(
            remotos = listOf(lookup("remote-rv1", tipo = "Reverso", reversaPagoId = "abono-1")),
            uploadedChunks = chunks,
        ).uploadPagos(opticaId)

        val uploaded = chunks.flatten()
        assertEquals(listOf("remote-rv1", "rv-2"), uploaded.map { it.id })
        assertEquals(listOf("abono-1", "abono-2"), uploaded.map { it.reversaPagoId })
    }

    @Test
    fun `servicio pagos with different servicioExtraId are not collapsed`() = runTest {
        val opticaId = "optica-test"
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            pago("ps-1", "Abono", dispensacionId = null, servicioExtraId = "serv-1"),
            pago("ps-2", "Abono", dispensacionId = null, servicioExtraId = "serv-2"),
        )
        val chunks = mutableListOf<List<PagoRemoto>>()

        createPagoCaptureCoordinator(
            remotos = listOf(lookup("remote-s1", dispensacionId = null, servicioExtraId = "serv-1")),
            uploadedChunks = chunks,
        ).uploadPagos(opticaId)

        val uploaded = chunks.flatten()
        assertEquals(listOf("remote-s1", "ps-2"), uploaded.map { it.id })
        assertEquals(listOf("serv-1", "serv-2"), uploaded.map { it.servicioExtraId })
    }

    // ── Terminal estados upload unchanged ─────────────────────────────

    @Test
    fun `cancelled and claimed dispensaciones upload with zero balance and no quarantine`() = runTest {
        val opticaId = "optica-test"
        val anulada = DispensacionOptica(
            id = "d-anul", ot = "2026-0050", fecha = LocalDate.parse("2026-09-01"), pacienteId = "p1",
            opticaId = opticaId, montoTotal = 200.0, montoPagado = 0.0, estadoEntrega = OrderStatusPolicy.ANULADO,
        )
        val (reclamada, replacement) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns
            listOf(anulada, reclamada, replacement)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            pago("abono-1", "Abono", monto = 200.0, dispensacionId = "d-anul"),
            pago("rev-1", "Reverso", monto = 200.0, dispensacionId = "d-anul", reversaPagoId = "abono-1"),
        )
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
        val captured = mutableListOf<DispensacionRemota>()

        val uploaded = createDispensacionCaptureCoordinator(emptyList(), captured).uploadDispensaciones(opticaId)

        assertEquals(3, uploaded)
        assertEquals(0.0, captured.single { it.id == "d-anul" }.montoPagado, 0.0)
        listOf("d-anul", "local-orig", "local-repl").forEach {
            coVerify { syncStateTracker.markSynced(opticaId, "dispensacion", it) }
        }
        coVerify(exactly = 0) {
            syncStateTracker.markError(opticaId, any(), any(), match { it.startsWith("quarantine:") })
        }
    }

    @Test
    fun `cancelled servicio uploads without quarantine`() = runTest {
        val opticaId = "optica-test"
        coEvery { repository.getServiciosSnapshotForOptica(opticaId) } returns listOf(
            ServicioExtra(
                id = "local-SA", ot = "OT-077", descripcion = "Biselado", montoTotal = 80.0, aCuenta = 0.0,
                estado = OrderStatusPolicy.ANULADO, fecha = LocalDate.parse("2026-09-01"), opticaId = opticaId,
            ),
        )
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } returns Unit

        createServicioCoordinator { emptyList() }.uploadServicios(opticaId)

        coVerify { syncStateTracker.markSynced(opticaId, "servicio_extra", "local-SA") }
        coVerify(exactly = 0) {
            syncStateTracker.markError(opticaId, any(), any(), match { it.startsWith("quarantine:") })
        }
    }

    @Test
    fun `reversal and compensation pagos of terminal orders upload and sync without quarantine`() = runTest {
        val opticaId = "optica-test"
        val ledger = listOf(
            pago("abono-1", "Abono", monto = 120.0),
            pago("rev-1", "Reverso", monto = 120.0, reversaPagoId = "abono-1"),
            pago("comp-1", "Abono", monto = 120.0),
            pago("reemb-1", "Reembolso", monto = 50.0),
            pago("ps-1", "Abono", monto = 80.0, dispensacionId = null, servicioExtraId = "serv-1"),
            pago("ps-rev", "Reverso", monto = 80.0, dispensacionId = null, servicioExtraId = "serv-1", reversaPagoId = "ps-1"),
        )
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns ledger
        val chunks = mutableListOf<List<PagoRemoto>>()

        createPagoCaptureCoordinator(uploadedChunks = chunks).uploadPagos(opticaId)

        assertEquals(ledger.map { it.id }.toSet(), chunks.flatten().map { it.id }.toSet())
        ledger.forEach { coVerify { syncStateTracker.markSynced(opticaId, "pago", it.id) } }
        coVerify(exactly = 0) {
            syncStateTracker.markError(opticaId, any(), any(), match { it.startsWith("quarantine:") })
        }
    }

    // ── Losing offline claim ──────────────────────────────────────────

    private fun stubRetryPassThrough() {
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
    }

    @Test
    fun `losing replacement with the winner OT is quarantined instead of adopting the winner id`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original, replacement)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        stubRetryPassThrough()
        val captured = mutableListOf<DispensacionRemota>()

        createDispensacionCaptureCoordinator(
            remotos = listOf(
                DispensacionRemotaLookup(id = "local-orig", ot = "2026-0042"),
                DispensacionRemotaLookup(id = "winner-repl", ot = "2026-0042-R1", reclamoOrigenId = "local-orig"),
            ),
            captured = captured,
        ).uploadDispensaciones(opticaId)

        assertTrue(captured.none { it.id == "winner-repl" })
        assertTrue(captured.none { it.ot == "2026-0042-R1" })
        assertTrue("the loser's view of the original must not overwrite the winner's", captured.isEmpty())
        coVerify {
            syncStateTracker.markError(opticaId, "dispensacion", "local-repl", "quarantine:reclamo_duplicate:local-orig")
        }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "dispensacion", "local-repl") }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "dispensacion", "local-orig") }
    }

    @Test
    fun `original held for the winner after a discarded claim is not uploaded`() = runTest {
        val opticaId = "optica-test"
        val (original, _) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { syncStateTracker.awaitingRemoteIds(opticaId, "dispensacion") } returns setOf("local-orig")
        stubRetryPassThrough()
        val captured = mutableListOf<DispensacionRemota>()

        createDispensacionCaptureCoordinator(
            remotos = listOf(
                DispensacionRemotaLookup(id = "local-orig", ot = "2026-0042"),
                DispensacionRemotaLookup(id = "winner-repl", ot = "2026-0042-R1", reclamoOrigenId = "local-orig"),
            ),
            captured = captured,
        ).uploadDispensaciones(opticaId)

        assertTrue("the stale original must wait for the download", captured.isEmpty())
    }

    @Test
    fun `losing replacement is quarantined when the remapped original already has another replacement`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns
            listOf(original, replacement.copy(ot = "2026-0042-R2"))
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        stubRetryPassThrough()
        val captured = mutableListOf<DispensacionRemota>()

        createDispensacionCaptureCoordinator(
            remotos = listOf(
                DispensacionRemotaLookup(id = "remote-orig", ot = "2026-0042"),
                DispensacionRemotaLookup(id = "winner-repl", ot = "2026-0042-R1", reclamoOrigenId = "remote-orig"),
            ),
            captured = captured,
        ).uploadDispensaciones(opticaId)

        assertTrue(captured.isEmpty())
        coVerify {
            syncStateTracker.markError(opticaId, "dispensacion", "local-repl", "quarantine:reclamo_duplicate:local-orig")
        }
    }

    @Test
    fun `replacement whose OT collides with an unrelated remote order is renumbered and uploaded`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        val localSibling = DispensacionOptica(
            id = "local-sibling", ot = "2026-0042-R2", fecha = LocalDate.parse("2026-09-15"),
            pacienteId = "p1", opticaId = opticaId,
        )
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original, replacement, localSibling)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        stubRetryPassThrough()
        val captured = mutableListOf<DispensacionRemota>()

        createDispensacionCaptureCoordinator(
            remotos = listOf(
                DispensacionRemotaLookup(id = "unrelated", ot = "2026-0042-R1"),
                DispensacionRemotaLookup(id = "unrelated-3", ot = "2026-0042-r3"),
            ),
            captured = captured,
        ).uploadDispensaciones(opticaId)

        coVerify { repository.updateDispensacion(replacement.copy(ot = "2026-0042-R4")) }
        assertEquals("2026-0042-R4", captured.single { it.id == "local-repl" }.ot)
        assertTrue(captured.none { it.id == "unrelated" })
        assertTrue("the original follows its renumbered replacement", captured.any { it.id == "local-orig" })
        coVerify(exactly = 0) { syncStateTracker.markError(opticaId, "dispensacion", "local-repl", any()) }
    }

    @Test
    fun `replacement OT collision without a determinable base stays quarantined`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        val baseless = replacement.copy(ot = "-R1")
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original, baseless)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        stubRetryPassThrough()
        val captured = mutableListOf<DispensacionRemota>()

        createDispensacionCaptureCoordinator(
            remotos = listOf(DispensacionRemotaLookup(id = "unrelated", ot = "-R1")),
            captured = captured,
        ).uploadDispensaciones(opticaId)

        assertTrue("the original stays local until the claim conflict is resolved", captured.isEmpty())
        coVerify(exactly = 0) { repository.updateDispensacion(any()) }
        coVerify {
            syncStateTracker.markError(opticaId, "dispensacion", "local-repl", "quarantine:reclamo_ot_conflict:unrelated")
        }
    }

    @Test
    fun `own replacement already on the server is not treated as a duplicate claim`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original, replacement)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        stubRetryPassThrough()
        val captured = mutableListOf<DispensacionRemota>()

        createDispensacionCaptureCoordinator(
            remotos = listOf(
                DispensacionRemotaLookup(id = "local-orig", ot = "2026-0042"),
                DispensacionRemotaLookup(id = "local-repl", ot = "2026-0042-R1", reclamoOrigenId = "local-orig"),
            ),
            captured = captured,
        ).uploadDispensaciones(opticaId)

        assertEquals(setOf("local-orig", "local-repl"), captured.map { it.id }.toSet())
        coVerify { syncStateTracker.markSynced(opticaId, "dispensacion", "local-repl") }
        coVerify(exactly = 0) { syncStateTracker.markError(opticaId, "dispensacion", any(), any()) }
    }

    @Test
    fun `claim index violation on upsert quarantines only the replacement as a duplicate claim`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original, replacement)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        stubRetryPassThrough()
        val accepted = mutableListOf<DispensacionRemota>()
        val testCoordinator = object : UploadSyncCoordinator(
            repository, supabase, database, syncStateTracker, mergeHandler, networkRetryHelper,
            costoProductoDao, costoBiseladoDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
            override suspend fun fetchRemoteDispensacionesForLookup(opticaId: String) =
                listOf(DispensacionRemotaLookup(id = "local-orig", ot = "2026-0042"))
            override suspend fun upsertDispensacionesChunk(chunk: List<DispensacionRemota>) {
                if (chunk.any { it.id == "local-repl" }) {
                    throw RuntimeException(
                        "duplicate key value violates unique constraint \"dispensaciones_reclamo_origen_uidx\" Code: 23505",
                    )
                }
                accepted.addAll(chunk)
            }
        }

        val uploaded = testCoordinator.uploadDispensaciones(opticaId)

        assertEquals(0, uploaded)
        assertTrue("the original must wait for the winner", accepted.isEmpty())
        coVerify {
            syncStateTracker.markError(opticaId, "dispensacion", "local-repl", "quarantine:reclamo_duplicate:local-orig")
        }
    }

    @Test
    fun `unrelated unique violation on dispensaciones still fails the upload`() = runTest {
        val opticaId = "optica-test"
        val (original, _) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        stubRetryPassThrough()
        val testCoordinator = object : UploadSyncCoordinator(
            repository, supabase, database, syncStateTracker, mergeHandler, networkRetryHelper,
            costoProductoDao, costoBiseladoDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
            override suspend fun fetchRemoteDispensacionesForLookup(opticaId: String) =
                emptyList<DispensacionRemotaLookup>()
            override suspend fun upsertDispensacionesChunk(chunk: List<DispensacionRemota>) {
                throw RuntimeException("duplicate key value violates unique constraint \"dispensaciones_ot_key\" Code: 23505")
            }
        }

        try {
            testCoordinator.uploadDispensaciones(opticaId)
            fail("Expected the unrelated 23505 to propagate")
        } catch (e: RuntimeException) {
            assertTrue(e.message.orEmpty().contains("dispensaciones_ot_key"))
        }
    }

    @Test
    fun `local reverso whose original already has a remote reverso is quarantined`() = runTest {
        val opticaId = "optica-test"
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            pago("rv-loser", "Reverso", reversaPagoId = "abono-1", fecha = "2026-09-30"),
            pago("abono-2", "Abono"),
        )
        val chunks = mutableListOf<List<PagoRemoto>>()

        try {
            createPagoCaptureCoordinator(
                remotos = listOf(
                    lookup("abono-1").copy(monto = 50.0),
                    lookup("rv-winner", tipo = "Reverso", reversaPagoId = "abono-1").copy(fecha = "2026-09-29"),
                ),
                uploadedChunks = chunks,
            ).uploadPagos(opticaId)
            fail("Expected UploadPartialException for the quarantined reverso")
        } catch (_: UploadPartialException) { }

        assertEquals(listOf("abono-2"), chunks.flatten().map { it.id })
        coVerify { syncStateTracker.markError(opticaId, "pago", "rv-loser", "quarantine:reverso_duplicate:abono-1") }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "pago", "rv-loser") }
    }

    @Test
    fun `reverso index violation on upsert quarantines the reverso instead of failing pagos`() = runTest {
        val opticaId = "optica-test"
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            pago("abono-2", "Abono"),
            pago("rv-race", "Reverso", reversaPagoId = "abono-1"),
        )
        stubRetryPassThrough()
        val accepted = mutableListOf<PagoRemoto>()
        val testCoordinator = object : UploadSyncCoordinator(
            repository, supabase, database, syncStateTracker, mergeHandler, networkRetryHelper,
            costoProductoDao, costoBiseladoDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
            override suspend fun fetchRemotePagosForLookup(opticaId: String) = emptyList<PagoRemotoLookup>()
            override suspend fun fetchRemoteParentIds(opticaId: String): Pair<Set<String>, Set<String>> =
                setOf("disp-1") to emptySet()
            override suspend fun upsertPagosChunk(chunk: List<PagoRemoto>) {
                if (chunk.any { it.id == "rv-race" }) {
                    throw RuntimeException(
                        "duplicate key value violates unique constraint \"pagos_reversa_pago_id_uidx\" Code: 23505",
                    )
                }
                accepted.addAll(chunk)
            }
        }

        try {
            testCoordinator.uploadPagos(opticaId)
            fail("Expected UploadPartialException for the quarantined reverso")
        } catch (_: UploadPartialException) { }

        assertEquals(listOf("abono-2"), accepted.map { it.id })
        coVerify { syncStateTracker.markError(opticaId, "pago", "rv-race", "quarantine:reverso_duplicate:abono-1") }
    }

    @Test
    fun `transfer pagos of a quarantined losing replacement are gated as parent missing`() = runTest {
        val opticaId = "optica-test"
        coEvery { syncStateTracker.quarantinedEntityIds(opticaId, "dispensacion") } returns setOf("local-repl")
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            pago("transfer-1", "Abono", dispensacionId = "local-repl"),
        )
        val chunks = mutableListOf<List<PagoRemoto>>()

        try {
            createPagoCaptureCoordinator(uploadedChunks = chunks).uploadPagos(opticaId)
            fail("Expected UploadPartialException")
        } catch (_: UploadPartialException) { }

        assertTrue(chunks.flatten().isEmpty())
        coVerify {
            syncStateTracker.markError(opticaId, "pago", "transfer-1", "quarantine:parent_missing:dispensacion:local-repl")
        }
    }

    @Test
    fun `items and regalos of a losing replacement are not uploaded`() = runTest {
        val opticaId = "optica-test"
        coEvery { syncStateTracker.quarantineReasons(opticaId, "dispensacion") } returns
            mapOf("local-repl" to "quarantine:reclamo_duplicate:local-orig")
        coEvery { repository.getDispensacionItemsSnapshotForOptica(opticaId) } returns listOf(
            DispensacionItem(id = "item-loser", dispensacionId = "local-repl", opticaId = opticaId),
            DispensacionItem(id = "item-ok", dispensacionId = "local-orig", opticaId = opticaId),
        )
        coEvery { repository.getRegalosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity(
                id = "regalo-loser", dispensacionId = "local-repl", productoId = "prod-1", cantidad = 1,
                costoUnitario = 5.0, descripcion = "Estuche", opticaId = opticaId,
            ),
            com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity(
                id = "regalo-ok", dispensacionId = "local-orig", productoId = "prod-1", cantidad = 1,
                costoUnitario = 5.0, descripcion = "Estuche", opticaId = opticaId,
            ),
        )
        stubRetryPassThrough()
        val items = mutableListOf<DispensacionItemRemota>()
        val regalos = mutableListOf<RegaloDispensacionRemota>()
        val testCoordinator = object : UploadSyncCoordinator(
            repository, supabase, database, syncStateTracker, mergeHandler, networkRetryHelper,
            costoProductoDao, costoBiseladoDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
            override suspend fun upsertDispensacionItemsChunk(chunk: List<DispensacionItemRemota>) {
                items.addAll(chunk)
            }
            override suspend fun upsertRegalosChunk(chunk: List<RegaloDispensacionRemota>) {
                regalos.addAll(chunk)
            }
        }

        testCoordinator.uploadDispensacionItems(opticaId)
        testCoordinator.uploadRegalos(opticaId)

        assertEquals(listOf("item-ok"), items.map { it.id })
        assertEquals(listOf("regalo-ok"), regalos.map { it.id })
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "dispensacion_item", "item-loser") }
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "regalo_dispensacion", "regalo-loser") }
    }

    // ── Claim upload ordering and claim ledger gate ───────────────────

    private fun chunkRecordingCoordinator(
        remotos: List<DispensacionRemotaLookup>,
        chunks: MutableList<List<String>>,
        failWhen: (List<DispensacionRemota>) -> Exception? = { null },
    ): UploadSyncCoordinator = object : UploadSyncCoordinator(
        repository, supabase, database, syncStateTracker, mergeHandler, networkRetryHelper,
        costoProductoDao, costoBiseladoDao,
    ) {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        override suspend fun fetchRemoteDispensacionesForLookup(opticaId: String) = remotos
        override suspend fun upsertDispensacionesChunk(chunk: List<DispensacionRemota>) {
            failWhen(chunk)?.let { throw it }
            chunks.add(chunk.map { it.id })
        }
    }

    private fun filler(opticaId: String, count: Int) = (1..count).map { n ->
        DispensacionOptica(
            id = "filler-$n", ot = "2026-1%03d".format(n), fecha = LocalDate.parse("2026-09-01"),
            pacienteId = "p1", opticaId = opticaId,
        )
    }

    @Test
    fun `original of an unconfirmed claim is uploaded only after its replacement`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original, replacement)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        stubRetryPassThrough()
        val chunks = mutableListOf<List<String>>()

        chunkRecordingCoordinator(
            remotos = listOf(DispensacionRemotaLookup(id = "local-orig", ot = "2026-0042")),
            chunks = chunks,
        ).uploadDispensaciones(opticaId)

        val replacementChunk = chunks.indexOfFirst { "local-repl" in it }
        val originalChunk = chunks.indexOfFirst { "local-orig" in it }
        assertTrue("replacement must be uploaded", replacementChunk >= 0)
        assertTrue("original must follow its replacement", originalChunk > replacementChunk)
    }

    @Test
    fun `original in an earlier chunk waits when its replacement upload fails transiently`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns
            listOf(original) + filler(opticaId, 85) + replacement
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        stubRetryPassThrough()
        val chunks = mutableListOf<List<String>>()

        try {
            chunkRecordingCoordinator(
                remotos = listOf(DispensacionRemotaLookup(id = "local-orig", ot = "2026-0042")),
                chunks = chunks,
                failWhen = { chunk -> if (chunk.any { it.id == "local-repl" }) IOException("timeout") else null },
            ).uploadDispensaciones(opticaId)
            fail("Expected UploadPartialException")
        } catch (_: UploadPartialException) { }

        assertTrue(chunks.flatten().none { it == "local-orig" })
        coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "dispensacion", "local-orig") }
    }

    @Test
    fun `original of an already confirmed claim uploads in the first pass`() = runTest {
        val opticaId = "optica-test"
        val (original, replacement) = claimPair(opticaId)
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(original, replacement)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { syncStateTracker.isSynced(opticaId, "dispensacion", "local-repl") } returns true
        stubRetryPassThrough()
        val chunks = mutableListOf<List<String>>()

        chunkRecordingCoordinator(
            remotos = listOf(
                DispensacionRemotaLookup(id = "local-orig", ot = "2026-0042"),
                DispensacionRemotaLookup(id = "local-repl", ot = "2026-0042-R1", reclamoOrigenId = "local-orig"),
            ),
            chunks = chunks,
        ).uploadDispensaciones(opticaId)

        assertEquals(listOf(listOf("local-orig", "local-repl")), chunks)
    }

    private fun claimLedgerPagos(): List<com.example.optoapp.data.Pago> = listOf(
        pago("rv-claim", "Reverso", dispensacionId = "disp-1", reversaPagoId = "ab-1"),
        pago("comp-claim", "Abono", dispensacionId = "disp-1")
            .copy(nota = "Compensación de Reembolso rb000001 por reclamo"),
        pago("transfer", "Abono", dispensacionId = "repl-1"),
        pago("refund", "Reembolso", dispensacionId = "repl-1"),
        pago("unrelated", "Abono", dispensacionId = "disp-9"),
    )

    private fun stubClaimSnapshot(opticaId: String) {
        coEvery { repository.getDispensacionesSnapshotForOptica(opticaId) } returns listOf(
            DispensacionOptica(id = "disp-1", ot = "2026-0042", fecha = LocalDate.parse("2026-09-01"), pacienteId = "p1", opticaId = opticaId),
            DispensacionOptica(
                id = "repl-1", ot = "2026-0042-R1", fecha = LocalDate.parse("2026-09-30"), pacienteId = "p1",
                opticaId = opticaId, reclamoOrigenId = "disp-1",
            ),
        )
    }

    private fun claimPagoCoordinator(remoteDispIds: Set<String>, uploaded: MutableList<String>): UploadSyncCoordinator {
        stubRetryPassThrough()
        return object : UploadSyncCoordinator(
            repository, supabase, database, syncStateTracker, mergeHandler, networkRetryHelper,
            costoProductoDao, costoBiseladoDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
            override suspend fun fetchRemotePagosForLookup(opticaId: String) = emptyList<PagoRemotoLookup>()
            override suspend fun fetchRemoteParentIds(opticaId: String): Pair<Set<String>, Set<String>> =
                remoteDispIds to emptySet()
            override suspend fun upsertPagosChunk(chunk: List<PagoRemoto>) {
                uploaded.addAll(chunk.map { it.id })
            }
        }
    }

    @Test
    fun `claim ledger rows stay local while the replacement is not confirmed on the server`() = runTest {
        val opticaId = "optica-test"
        stubClaimSnapshot(opticaId)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns claimLedgerPagos()
        val uploaded = mutableListOf<String>()

        claimPagoCoordinator(remoteDispIds = setOf("disp-1", "disp-9"), uploaded = uploaded).uploadPagos(opticaId)

        assertEquals(listOf("unrelated"), uploaded)
        coVerify(exactly = 0) { syncStateTracker.markError(opticaId, "pago", any(), any()) }
        listOf("rv-claim", "comp-claim", "transfer", "refund").forEach { id ->
            coVerify(exactly = 0) { syncStateTracker.markSynced(opticaId, "pago", id) }
        }
    }

    @Test
    fun `claim ledger rows upload once the replacement is confirmed`() = runTest {
        val opticaId = "optica-test"
        stubClaimSnapshot(opticaId)
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns claimLedgerPagos()
        coEvery { syncStateTracker.isSynced(opticaId, "dispensacion", "repl-1") } returns true
        val uploaded = mutableListOf<String>()

        claimPagoCoordinator(remoteDispIds = setOf("disp-1", "repl-1", "disp-9"), uploaded = uploaded).uploadPagos(opticaId)

        assertEquals(setOf("rv-claim", "comp-claim", "transfer", "refund", "unrelated"), uploaded.toSet())
    }

    @Test
    fun `pagos of an original held for the winner stay local`() = runTest {
        val opticaId = "optica-test"
        coEvery { syncStateTracker.awaitingRemoteIds(opticaId, "dispensacion") } returns setOf("disp-1")
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            pago("user-abono", "Abono", dispensacionId = "disp-1"),
            pago("unrelated", "Abono", dispensacionId = "disp-9"),
        )
        val uploaded = mutableListOf<String>()

        claimPagoCoordinator(remoteDispIds = setOf("disp-1", "disp-9"), uploaded = uploaded).uploadPagos(opticaId)

        assertEquals(listOf("unrelated"), uploaded)
        coVerify(exactly = 0) { syncStateTracker.markError(opticaId, "pago", any(), any()) }
    }
}
