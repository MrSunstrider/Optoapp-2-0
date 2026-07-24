package com.example.optoapp.viewmodel

import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SyncTelemetryLogDao
import com.example.optoapp.data.SyncTelemetryLogEntity
import com.example.optoapp.util.BackgroundErrorCollector
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SyncDiagnosticsViewModelSyncHistoryTest {

    private val syncEntityStateDao = mockk<SyncEntityStateDao>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val bgErrorCollector = mockk<BackgroundErrorCollector>(relaxed = true)
    private val membershipRepository = mockk<MembershipRepository>(relaxed = true)
    private val syncTelemetryLogDao = mockk<SyncTelemetryLogDao>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0

        every { sessionManager.opticaId } returns MutableStateFlow("optica-test")
        every { bgErrorCollector.errors } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun syncHistory_emits_entries_matching_dao_content() = runBlocking {
        val entries = listOf(
            SyncTelemetryLogEntity(
                id = UUID.randomUUID().toString(),
                opticaId = "optica-test",
                status = "error",
                stage = "finanzas",
                errorMessage = "timeout",
                createdAt = 2000L,
            ),
            SyncTelemetryLogEntity(
                id = UUID.randomUUID().toString(),
                opticaId = "optica-test",
                status = "ok",
                stage = "finalizado",
                errorMessage = "",
                createdAt = 1000L,
            ),
        )
        coEvery { syncTelemetryLogDao.observeByOpticaId("optica-test") } returns flowOf(entries)

        val vm = SyncDiagnosticsViewModel(
            syncEntityStateDao = syncEntityStateDao,
            sessionManager = sessionManager,
            supabase = supabase,
            bgErrorCollector = bgErrorCollector,
            membershipRepository = membershipRepository,
            syncTelemetryLogDao = syncTelemetryLogDao,
        )

        val history: List<SyncTelemetryLogEntity> = vm.syncHistory.first()
        assertEquals(2, history.size)
        assertEquals("error", history[0].status)
        assertEquals("ok", history[1].status)
    }

    @Test
    fun syncHistory_emits_empty_when_dao_returns_empty() = runBlocking {
        coEvery { syncTelemetryLogDao.observeByOpticaId(any()) } returns flowOf(emptyList())

        val vm = SyncDiagnosticsViewModel(
            syncEntityStateDao = syncEntityStateDao,
            sessionManager = sessionManager,
            supabase = supabase,
            bgErrorCollector = bgErrorCollector,
            membershipRepository = membershipRepository,
            syncTelemetryLogDao = syncTelemetryLogDao,
        )

        assertEquals(0, vm.syncHistory.first().size)
    }
}
