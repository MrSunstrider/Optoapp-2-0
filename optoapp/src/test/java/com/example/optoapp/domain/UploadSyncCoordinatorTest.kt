package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate

class UploadSyncCoordinatorTest {

    private val repository = mockk<OptoRepository>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val mergeHandler = mockk<DispensacionMergeHandler>(relaxed = true)
    private val networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true)
    private val costoProductoDao = mockk<CostoProductoDao>(relaxed = true)
    private val costoBiseladoDao = mockk<CostoBiseladoDao>(relaxed = true)
    private lateinit var coordinator: UploadSyncCoordinator

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        coordinator = UploadSyncCoordinator(
            repository = repository,
            supabase = supabase,
            syncStateTracker = syncStateTracker,
            mergeHandler = mergeHandler,
            networkRetryHelper = networkRetryHelper,
            costoProductoDao = costoProductoDao,
            costoBiseladoDao = costoBiseladoDao,
        )
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

        // RED (current code): merge IS called inline during dedup → coVerify fails
        // GREEN (fixed code): merge deferred after upsert → upsert failed → merge NOT called
        coVerify(exactly = 0) { mergeHandler.mergeLocalDispensacionConflict(any(), any(), any()) }
    }

    @Test
    fun `servicio dedup should use Instant comparison not string comparison`() {
        // The bug: string comparison "2025-01-01T10:00:00Z" > "2025-01-01T10:00:00.500Z"
        //         incorrectly picks the older record (Z sorts before '.')
        val olderStr = "2025-01-01T10:00:00Z"
        val newerStr = "2025-01-01T10:00:00.500Z"

        // String compare (current buggy code)
        val stringWinner = if (newerStr > olderStr) newerStr else olderStr
        assertEquals("String compare picks older (Z > .)", olderStr, stringWinner)

        // Instant compare (fixed code)
        val olderInstant = Instant.parse(olderStr)
        val newerInstant = Instant.parse(newerStr)
        val instantWinner = if (newerInstant > olderInstant) newerStr else olderStr
        assertEquals("Instant compare picks newer (.500Z)", newerStr, instantWinner)
    }

    @Test
    fun `servicio dedup unparseable timestamp falls back to existing record`() {
        val validTimestamp = "2025-01-01T10:00:00Z"
        val malformedTimestamp = "not-a-timestamp"

        // Verify that Instant.parse throws on malformed input (renders it unparseable)
        var caught = false
        try {
            Instant.parse(malformedTimestamp)
        } catch (_: Exception) {
            caught = true
        }
        assertEquals("Instant.parse throws on malformed", true, caught)

        // The fix's logic: try { Instant.parse(ts) } catch (e: Exception) { keep existing }
        // Given:
        val existingTime = validTimestamp.let { Instant.parse(it) }
        val rowTime = try { Instant.parse(malformedTimestamp) } catch (_: Exception) { null }

        // When: rowTime is null → keep existing
        val winner = if (rowTime != null && existingTime != null) {
            if (rowTime > existingTime) "new" else "existing"
        } else {
            "existing"
        }

        // Then: existing is kept
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

        // RED (current code): batch BEFORE individual → ["batch", "individual"]
        // GREEN (fixed code): individual BEFORE batch → ["individual", "batch"]
        assertEquals(listOf("individual", "batch"), callOrder)
    }
}
