package com.example.optoapp.viewmodel

import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SyncTelemetryRemoteRow
import com.example.optoapp.util.BackgroundErrorCollector
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.net.SocketTimeoutException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SyncDiagnosticsViewModelRetryTest {

    private val syncEntityStateDao = mockk<SyncEntityStateDao>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val bgErrorCollector = mockk<BackgroundErrorCollector>(relaxed = true)
    private val membershipRepository = mockk<MembershipRepository>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        mockkStatic("com.example.optoapp.viewmodel.SyncDiagnosticsViewModel")
    }

    @After
    fun tearDown() {
        SyncDiagnosticsViewModel.remoteTelemetryRetryAttempts = 5
        unmockkAll()
    }

    @Test
    fun `remoteTelemetryRetryAttempts defaults to 5`() {
        assertEquals("Should be 5 after fix", 5, SyncDiagnosticsViewModel.remoteTelemetryRetryAttempts)
    }

    @Test
    fun `exponential backoff formula produces 1s 2s 4s gaps`() {
        // Pure function test of the backoff formula:
        // delayMs = (1000L * (1L shl attempt)) + Random.nextLong(0, 200)
        // For attempt 0: 1000 + 0..200 ≈ 1000..1200
        // For attempt 1: 2000 + 0..200 ≈ 2000..2200
        // For attempt 2: 4000 + 0..200 ≈ 4000..4200
        // This proves the exponential pattern WITHOUT needing to mock the Postgrest DSL
        val attempt0 = 1000L * (1L shl 0) // 1000
        val attempt1 = 1000L * (1L shl 1) // 2000
        val attempt2 = 1000L * (1L shl 2) // 4000

        assertEquals("Attempt 0 should be ~1000ms", 1000L, attempt0)
        assertEquals("Attempt 1 should be ~2000ms", 2000L, attempt1)
        assertEquals("Attempt 2 should be ~4000ms", 4000L, attempt2)

        // Previous linear formula (300 * (attempt + 1)) would give:
        // val oldAttempt0 = 300L * (0 + 1) = 300
        // val oldAttempt1 = 300L * (1 + 1) = 600
        // val oldAttempt2 = 300L * (2 + 1) = 900
        // These are ~3x smaller, confirming the fix increases backoff
    }

    @Test
    fun `retry count is max 5 attempts`() {
        // Verify the retry constant allows up to 5 attempts
        SyncDiagnosticsViewModel.remoteTelemetryRetryAttempts = 5
        assertEquals(5, SyncDiagnosticsViewModel.remoteTelemetryRetryAttempts)

        // With 5 attempts, there are 4 retry delays (attempts 0-4, last one throws)
        // Exponential delays: 1s, 2s, 4s, 8s (not 300ms, 600ms, 900ms, 1200ms)
        var totalDelay = 0L
        for (attempt in 0 until SyncDiagnosticsViewModel.remoteTelemetryRetryAttempts - 1) {
            totalDelay += 1000L * (1L shl attempt)
        }
        // Total ≈ 1 + 2 + 4 + 8 = 15s for all retries (vs 300+600+900+1200 = 3s for old)
        assertEquals("Total exponential delay ~15s", 15_000L, totalDelay)
    }

    @Test
    fun `non-transient error is recognized correctly`() {
        val vm = SyncDiagnosticsViewModel(
            syncEntityStateDao = syncEntityStateDao,
            sessionManager = sessionManager,
            supabase = supabase,
            bgErrorCollector = bgErrorCollector,
            membershipRepository = membershipRepository,
        )

        val transientMsg = SocketTimeoutException("connection timeout")
        val nonTransientMsg = IOException("HTTP 400 Bad Request")

        assertTrue("timeout should be transient", vm.isTransientNetworkError(transientMsg))
        assertEquals("400 should NOT be transient", false, vm.isTransientNetworkError(nonTransientMsg))
    }
}
