package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.venta.Venta
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

class UploadSyncCoordinatorVentasTest {

    private lateinit var repository: OptoRepository
    private lateinit var syncStateTracker: SyncStateTracker
    private lateinit var networkRetryHelper: NetworkRetryHelper
    private lateinit var coordinator: UploadSyncCoordinator

    private val opticaId = "optica-test"
    private val testDate = LocalDate.of(2026, 7, 4)

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

    private fun makeVenta(id: String = "v1") = Venta(
        id = id, opticaId = opticaId, origen = "dispensacion",
        origenId = "disp_1", fecha = testDate,
        montoTotal = 100.0, estado = "pendiente"
    )

    @Test
    fun uploadVentas_markSynced_on_success() = runBlocking {
        val ventas = listOf(makeVenta("v1"), makeVenta("v2"))
        coEvery { repository.getVentasForOptica(opticaId) } returns ventas
        coEvery { networkRetryHelper.retryNetwork(any(), any<suspend () -> Unit>()) } just Runs

        val count = coordinator.uploadVentas(opticaId)

        assertEquals(2, count)
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "upload_ventas", "batch") }
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "venta", "v1") }
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "venta", "v2") }
    }

    @Test
    fun uploadVentas_markSynced_on_empty() = runBlocking {
        coEvery { repository.getVentasForOptica(opticaId) } returns emptyList()
        val count = coordinator.uploadVentas(opticaId)
        assertEquals(0, count)
        coVerify(exactly = 1) { syncStateTracker.markSynced(opticaId, "upload_ventas", "batch") }
    }

    @Test
    fun uploadVentas_markError_on_ioexception() = runBlocking {
        val ventas = listOf(makeVenta("v1"))
        coEvery { repository.getVentasForOptica(opticaId) } returns ventas
        coEvery { networkRetryHelper.retryNetwork(any(), any<suspend () -> Unit>()) } throws
                IOException("Connection reset")

        var exceptionThrown = false
        try {
            coordinator.uploadVentas(opticaId)
        } catch (_: IOException) {
            exceptionThrown = true
        } finally {
            coVerify(exactly = 1) {
                syncStateTracker.markError(opticaId, "upload_ventas", "batch", "Connection reset")
            }
        }
        assertTrue("Expected IOException was not thrown", exceptionThrown)
    }

    @Test
    fun uploadVentas_markError_on_exception() = runBlocking {
        val ventas = listOf(makeVenta("v1"))
        coEvery { repository.getVentasForOptica(opticaId) } returns ventas
        coEvery { networkRetryHelper.retryNetwork(any(), any<suspend () -> Unit>()) } throws
                RuntimeException("unexpected error")

        var exceptionThrown = false
        try {
            coordinator.uploadVentas(opticaId)
        } catch (_: RuntimeException) {
            exceptionThrown = true
        } finally {
            coVerify(exactly = 1) {
                syncStateTracker.markError(opticaId, "upload_ventas", "batch", "unexpected error")
            }
        }
        assertTrue("Expected RuntimeException was not thrown", exceptionThrown)
    }
}
