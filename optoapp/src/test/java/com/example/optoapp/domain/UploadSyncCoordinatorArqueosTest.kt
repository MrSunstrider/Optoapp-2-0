package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.arqueo.ArqueoCaja
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class UploadSyncCoordinatorArqueosTest {

    private lateinit var repository: OptoRepository
    private lateinit var syncStateTracker: SyncStateTracker
    private lateinit var networkRetryHelper: NetworkRetryHelper
    private lateinit var coordinator: UploadSyncCoordinator

    private val opticaId = "optica-test"
    private val testDate = LocalDate.of(2026, 7, 6)

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        repository = mockk(relaxed = true)
        syncStateTracker = mockk(relaxed = true)
        val mergeHandler: DispensacionMergeHandler = mockk(relaxed = true)
        networkRetryHelper = mockk()
        val supabase = mockk<io.github.jan.supabase.SupabaseClient>(relaxed = true)

        coordinator = UploadSyncCoordinator(
            repository = repository,
            supabase = supabase,
            syncStateTracker = syncStateTracker,
            mergeHandler = mergeHandler,
            networkRetryHelper = networkRetryHelper
        )
    }

    private fun makeArqueo(id: String = "arq-1") = ArqueoCaja(
        id = id, fecha = testDate, opticaId = opticaId,
        fondoCaja = 1000.0, efectivoContado = 500.0, tarjetaContado = 200.0,
        transferenciaContado = 100.0, movilContado = 50.0,
        efectivoCobrado = 490.0, tarjetaCobrado = 198.0,
        transferenciaCobrado = 100.0, movilCobrado = 50.0,
        diferenciaEfectivo = -10.0, diferenciaTarjeta = -2.0,
        diferenciaTransferencia = 0.0, diferenciaMovil = 0.0,
        diferenciaTotal = -12.0, cerradoPor = "admin", sellado = true,
        createdAt = "2026-07-06T10:00:00Z", updatedAt = "2026-07-06T10:00:00Z",
        updatedBy = "admin"
    )

    // ── RED: markSynced batch + per-item on success ──────────────────────────

    @Test
    fun uploadArqueos_markSynced_on_success() = runBlocking {
        val arqueos = listOf(makeArqueo("arq-1"), makeArqueo("arq-2"))
        coEvery { repository.getArqueosByOpticaList(opticaId) } returns arqueos
        coEvery { networkRetryHelper.retryNetwork(any(), any<suspend () -> Unit>()) } just Runs

        val count = coordinator.uploadArqueos(opticaId)

        assertEquals(2, count)
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "upload_arqueo_caja", "batch") }
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "arqueo_caja", "arq-1") }
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "arqueo_caja", "arq-2") }
    }

    // ── RED: markSynced on empty ─────────────────────────────────────────────

    @Test
    fun uploadArqueos_markSynced_on_empty() = runBlocking {
        coEvery { repository.getArqueosByOpticaList(opticaId) } returns emptyList()

        val count = coordinator.uploadArqueos(opticaId)

        assertEquals(0, count)
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "upload_arqueo_caja", "batch") }
    }

    // ── RED: markError on IOException (exception propagates) ──────────────────

    @Test
    fun uploadArqueos_markError_on_ioexception() = runBlocking {
        val arqueos = listOf(makeArqueo("arq-1"))
        coEvery { repository.getArqueosByOpticaList(opticaId) } returns arqueos
        coEvery { networkRetryHelper.retryNetwork(any(), any<suspend () -> Unit>()) } throws
                IOException("Connection reset")

        var exceptionThrown = false
        try {
            coordinator.uploadArqueos(opticaId)
        } catch (_: IOException) {
            exceptionThrown = true
        } finally {
            coVerify(exactly = 1) {
                syncStateTracker.markError(opticaId, "upload_arqueo_caja", "batch", "Connection reset")
            }
        }
        assertTrue("Expected IOException was not thrown", exceptionThrown)
    }

    // ── RED: markError on generic Exception (exception propagates) ────────────

    @Test
    fun uploadArqueos_markError_on_exception() = runBlocking {
        val arqueos = listOf(makeArqueo("arq-1"))
        coEvery { repository.getArqueosByOpticaList(opticaId) } returns arqueos
        coEvery { networkRetryHelper.retryNetwork(any(), any<suspend () -> Unit>()) } throws
                RuntimeException("unexpected error")

        var exceptionThrown = false
        try {
            coordinator.uploadArqueos(opticaId)
        } catch (_: RuntimeException) {
            exceptionThrown = true
        } finally {
            coVerify(exactly = 1) {
                syncStateTracker.markError(opticaId, "upload_arqueo_caja", "batch", "unexpected error")
            }
        }
        assertTrue("Expected RuntimeException was not thrown", exceptionThrown)
    }

    // ── RED: uses retryNetwork for network resilience ─────────────────────────

    @Test
    fun uploadArqueos_uses_retryNetwork() = runBlocking {
        val arqueos = listOf(makeArqueo("arq-1"), makeArqueo("arq-2"), makeArqueo("arq-3"))
        coEvery { repository.getArqueosByOpticaList(opticaId) } returns arqueos
        coEvery { networkRetryHelper.retryNetwork(any(), any<suspend () -> Unit>()) } just Runs

        coordinator.uploadArqueos(opticaId)

        coVerify(atLeast = 1) { networkRetryHelper.retryNetwork(any<String>(), any<suspend () -> Unit>()) }
    }
}
