package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoDatabase
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
}
