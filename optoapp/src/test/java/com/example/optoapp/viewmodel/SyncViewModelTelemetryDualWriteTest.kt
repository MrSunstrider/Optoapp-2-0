package com.example.optoapp.viewmodel

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.OrdenCompraRepository
import com.example.optoapp.data.ProveedorRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SyncTelemetry
import com.example.optoapp.data.SyncTelemetryLogDao
import com.example.optoapp.data.SyncTelemetryLogEntity
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioFisicoUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncInventoryKpisUseCase
import com.example.optoapp.domain.SyncOrdenesCompraUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncProveedoresUseCase
import com.example.optoapp.domain.observer.TableObserver
import com.example.optoapp.domain.sync.SyncOrchestrator
import com.example.optoapp.subscription.SubscriptionManager
import com.example.optoapp.sync.PostSaveSyncScheduler
import android.content.Context
import android.net.ConnectivityManager
import com.example.optoapp.sync.SyncGate
import com.example.optoapp.util.BackgroundErrorCollector
import io.github.jan.supabase.SupabaseClient
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncViewModelTelemetryDualWriteTest {

    private lateinit var syncTelemetryLogDao: SyncTelemetryLogDao
    private lateinit var viewModel: SyncViewModel

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        syncTelemetryLogDao = mockk(relaxed = true)

        val context = mockk<Context>(relaxed = true)
        val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

        viewModel = SyncViewModel(
            context = context,
            sessionManager = mockk(relaxed = true) {
                every { opticaId } returns MutableStateFlow("optica-1")
            },
            membershipRepository = mockk<MembershipRepository>(relaxed = true),
            repository = mockk<OptoRepository>(relaxed = true),
            proveedorRepository = mockk<ProveedorRepository>(relaxed = true),
            ordenCompraRepository = mockk<OrdenCompraRepository>(relaxed = true),
            syncTelemetry = mockk<SyncTelemetry>(relaxed = true),
            subscriptionManager = mockk<SubscriptionManager>(relaxed = true),
            supabase = mockk(relaxed = true),
            syncPacientesUseCase = mockk(relaxed = true),
            syncHistorialUseCase = mockk(relaxed = true),
            syncFinanzasUseCase = mockk(relaxed = true),
            syncInventarioUseCase = mockk(relaxed = true),
            syncProveedoresUseCase = mockk(relaxed = true),
            syncOrdenesCompraUseCase = mockk(relaxed = true),
            syncInventarioFisicoUseCase = mockk(relaxed = true),
            syncInventoryKpisUseCase = mockk(relaxed = true),
            syncGate = SyncGate(),
            conflictDao = mockk<ConflictDao>(relaxed = true),
            syncEntityStateDao = mockk<SyncEntityStateDao>(relaxed = true),
            supabaseObserver = mockk<TableObserver>(relaxed = true),
            bgErrorCollector = mockk<BackgroundErrorCollector>(relaxed = true),
            postSaveSyncScheduler = mockk<PostSaveSyncScheduler>(relaxed = true),
            syncOrchestrator = mockk<SyncOrchestrator>(relaxed = true),
            syncTelemetryLogDao = syncTelemetryLogDao,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `recordRemoteSyncTelemetry inserts Room SyncTelemetryLogEntity`() = runBlocking {
        viewModel.recordRemoteSyncTelemetry("optica-1", "ok", "finalizado", null)

        val slot = slot<SyncTelemetryLogEntity>()
        coVerify(exactly = 1) { syncTelemetryLogDao.insert(capture(slot)) }
        val entity = slot.captured
        assertEquals("optica-1", entity.opticaId)
        assertEquals("ok", entity.status)
        assertEquals("finalizado", entity.stage)
    }

    @Test
    fun `recordRemoteSyncTelemetry inserts Room entity with error message`() = runBlocking {
        viewModel.recordRemoteSyncTelemetry("optica-1", "error", "pacientes", "Network timeout")

        val slot = slot<SyncTelemetryLogEntity>()
        coVerify(exactly = 1) { syncTelemetryLogDao.insert(capture(slot)) }
        val entity = slot.captured
        assertEquals("optica-1", entity.opticaId)
        assertEquals("error", entity.status)
        assertEquals("pacientes", entity.stage)
        assertTrue(entity.errorMessage.isNotBlank())
        assertTrue(entity.createdAt > 0)
    }
}
