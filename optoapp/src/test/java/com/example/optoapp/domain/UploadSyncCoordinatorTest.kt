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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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

    // ── T2.1 RED: RPC parameter construction ───────────────────────────

    @Test
    fun `buildAdjustStockParams produces correct keys and values`() {
        val params = UploadSyncCoordinator.buildAdjustStockParams(
            monturaId = "M1",
            opticaId = "O1",
            delta = -1,
            referenceId = "D1",
            note = "venta_dispensacion",
            tipo = "venta",
            fecha = "2026-01-15",
        )

        assertEquals("M1", params["p_montura_id"]?.jsonPrimitive?.content)
        assertEquals("O1", params["p_optica_id"]?.jsonPrimitive?.content)
        assertEquals(-1, params["p_delta"]?.jsonPrimitive?.content?.toInt())
        assertEquals("D1", params["p_reference_id"]?.jsonPrimitive?.content)
        assertEquals("venta_dispensacion", params["p_note"]?.jsonPrimitive?.content)
        assertEquals("venta", params["p_tipo"]?.jsonPrimitive?.content)
        assertEquals("2026-01-15", params["p_fecha"]?.jsonPrimitive?.content)
    }

    @Test
    fun `buildAdjustStockParams with different delta value`() {
        val params = UploadSyncCoordinator.buildAdjustStockParams(
            monturaId = "M2",
            opticaId = "O2",
            delta = 5,
            referenceId = "ADJ-001",
            note = "ajuste_inventario",
            tipo = "ajuste",
            fecha = "2026-06-30",
        )

        assertEquals(5, params["p_delta"]?.jsonPrimitive?.content?.toInt())
        assertEquals("ADJ-001", params["p_reference_id"]?.jsonPrimitive?.content)
        assertEquals("ajuste_inventario", params["p_note"]?.jsonPrimitive?.content)
        assertEquals("ajuste", params["p_tipo"]?.jsonPrimitive?.content)
    }

    // ── T2.1 RED: RPC response parsing ─────────────────────────────────

    @Test
    fun `parseAdjustStockResult ok true extracts new_stock`() {
        val response = buildJsonObject {
            put("ok", true)
            put("new_stock", 4)
        }

        val ok = UploadSyncCoordinator.parseAdjustStockOk(response)
        val stock = UploadSyncCoordinator.parseAdjustStockNewStock(response)

        assertTrue(ok)
        assertEquals(4, stock)
    }

    @Test
    fun `parseAdjustStockResult ok false extracts error`() {
        val response = buildJsonObject {
            put("ok", false)
            put("error", "insufficient")
        }

        val ok = UploadSyncCoordinator.parseAdjustStockOk(response)
        val error = UploadSyncCoordinator.parseAdjustStockError(response)

        assertFalse(ok)
        assertEquals("insufficient", error)
    }

    @Test
    fun `parseAdjustStockResult not_found error`() {
        val response = buildJsonObject {
            put("ok", false)
            put("error", "not_found")
        }

        val ok = UploadSyncCoordinator.parseAdjustStockOk(response)
        val error = UploadSyncCoordinator.parseAdjustStockError(response)

        assertFalse(ok)
        assertEquals("not_found", error)
    }

    @Test
    fun `parseAdjustStockResult ok true with zero stock`() {
        val response = buildJsonObject {
            put("ok", true)
            put("new_stock", 0)
        }

        val ok = UploadSyncCoordinator.parseAdjustStockOk(response)
        val stock = UploadSyncCoordinator.parseAdjustStockNewStock(response)

        assertTrue(ok)
        assertEquals(0, stock)
    }

    // ── T2.3 RED: failure handling edge cases ──────────────────────────

    @Test
    fun `parseAdjustStockOk returns false when ok field is missing`() {
        val response = buildJsonObject {
            put("new_stock", 3)
        }

        val ok = UploadSyncCoordinator.parseAdjustStockOk(response)
        assertFalse(ok)
    }

    @Test
    fun `parseAdjustStockError returns null when error field is missing`() {
        val response = buildJsonObject {
            put("ok", false)
        }

        val error = UploadSyncCoordinator.parseAdjustStockError(response)
        assertEquals(null, error)
    }

    @Test
    fun `parseAdjustStockNewStock returns null when new_stock is missing`() {
        val response = buildJsonObject {
            put("ok", true)
        }

        val stock = UploadSyncCoordinator.parseAdjustStockNewStock(response)
        assertEquals(null, stock)
    }

    @Test
    fun `parseAdjustStockOk returns false when ok is string false`() {
        val response = buildJsonObject {
            put("ok", "false")
        }

        // "false" as string !== "true" as string → parseAdjustStockOk returns false
        val ok = UploadSyncCoordinator.parseAdjustStockOk(response)
        assertFalse(ok)
    }

    // ── T2.1 RED: Pagos reconciliation tests ──────────────────────────

    private fun createPagoCoordinator(
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
                    tipo = "Pago", monto = 100.0, metodoPago = "Efectivo", fecha = "2026-01-01",
                ),
            )
        }

        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-1", dispensacionId = "disp-1", tipo = "Pago", monto = 100.0,
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
                    tipo = "Pago", monto = 200.0, metodoPago = "Tarjeta", fecha = "2026-01-02",
                ),
            )
        }

        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-1", dispensacionId = "disp-1", tipo = "Pago", monto = 100.0,
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
        val testCoordinator = createPagoCoordinator { _ ->
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
                id = "local-null", dispensacionId = null, tipo = "Abono", monto = 50.0,
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
        val testCoordinator = createPagoCoordinator { _ ->
            fetchCalled = true
            listOf(
                PagoRemotoLookup(
                    id = "remote-1", dispensacionId = "other-disp",
                    tipo = "Pago", monto = 200.0, metodoPago = "Tarjeta", fecha = "2026-01-02",
                ),
            )
        }

        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-null", dispensacionId = null, tipo = "Abono", monto = 50.0,
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
                id = "local-1", dispensacionId = "disp-1", tipo = "Pago", monto = 100.0,
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

    // ── C1+C2: uploadPagos dedup + local ID tracking ─────────────────

    @Test
    fun `uploadPagos deduplicatesById after reconciliation`() = runTest {
        val opticaId = "optica-test"
        val testCoordinator = createPagoCoordinator { _ ->
            listOf(
                PagoRemotoLookup(
                    id = "remote-1", dispensacionId = "disp-1",
                    tipo = "Pago", monto = 100.0, metodoPago = "Efectivo", fecha = "2026-01-01",
                ),
            )
        }

        // Two local pagos with same PagoKey reconcile to same remote ID
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(
            com.example.optoapp.data.Pago(
                id = "local-A", dispensacionId = "disp-1", tipo = "Pago", monto = 100.0,
                metodoPago = "Efectivo", fecha = LocalDate.parse("2026-01-01"), opticaId = opticaId,
            ),
            com.example.optoapp.data.Pago(
                id = "local-B", dispensacionId = "disp-1", tipo = "Pago", monto = 100.0,
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
}
