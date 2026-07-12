package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.FinanzasRemoteDefaults
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SyncFinanzasUseCaseKtTest {

    @Before
    fun setUpLog() {
        mockkStatic("android.util.Log")
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    // ── ServicioRemoto DTO mapping tests ────────────────────────────────────

    private fun makeServicioRemoto(
        id: String = "test-servicio-id",
        ot: String = "",
        descripcion: String = "",
        montoTotal: Double = 0.0,
        aCuenta: Double = 0.0,
        estado: String = "",
        fecha: String = "2024-06-15",
        pacienteId: String? = null,
        metodoPago: String = "",
        opticaId: String = "test-optica"
    ) = ServicioRemoto(
        id = id, ot = ot, descripcion = descripcion,
        montoTotal = montoTotal, aCuenta = aCuenta, estado = estado,
        fecha = fecha, pacienteId = pacienteId, metodoPago = metodoPago, opticaId = opticaId
    )

    @Test
    fun toEntity_negativeMontoTotal_coercesToZero() {
        val remoto = makeServicioRemoto(montoTotal = -5.0)
        val entity = remoto.toEntity()
        assertEquals(0.0, entity.montoTotal, 0.001)
    }

    @Test
    fun toEntity_aCuentaExceedsMontoTotal_clampedToMontoTotal() {
        val remoto = makeServicioRemoto(montoTotal = 100.0, aCuenta = 150.0)
        val entity = remoto.toEntity()
        assertEquals(100.0, entity.aCuenta, 0.001)
    }

    @Test
    fun toEntity_negativeACuenta_coercesToZero() {
        val remoto = makeServicioRemoto(montoTotal = 50.0, aCuenta = -10.0)
        val entity = remoto.toEntity()
        assertEquals(0.0, entity.aCuenta, 0.001)
    }

    @Test
    fun toEntity_normalValues_passesThrough() {
        val remoto = makeServicioRemoto(montoTotal = 50.0, aCuenta = 25.0)
        val entity = remoto.toEntity()
        assertEquals(50.0, entity.montoTotal, 0.001)
        assertEquals(25.0, entity.aCuenta, 0.001)
    }

    @Test
    fun toEntity_blankOpticaId_usesFallback() {
        val remoto = makeServicioRemoto(opticaId = "  ")
        val entity = remoto.toEntity()
        assertEquals(FinanzasRemoteDefaults.OPTICA_ID_FALLBACK, entity.opticaId)
    }

    @Test
    fun toEntity_zeroValues_passesThrough() {
        val remoto = makeServicioRemoto(montoTotal = 0.0, aCuenta = 0.0)
        val entity = remoto.toEntity()
        assertEquals(0.0, entity.montoTotal, 0.001)
        assertEquals(0.0, entity.aCuenta, 0.001)
    }

    // ── Sync sequence tests (no Venta) ──────────────────────────────────────

    @Test
    fun syncFinanzas_includes_gastosOperativos_in_upload_sequence() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 0
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 0
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 3
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 0
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper
        )

        useCase("optica-test")

        coVerifyOrder {
            uploadCoordinator.uploadPagos("optica-test")
            uploadCoordinator.uploadGastosOperativos("optica-test")
        }
    }

    @Test
    fun syncFinanzas_includes_resumenDiario_in_download_sequence() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 0
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 0
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 2
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 1

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper
        )

        useCase("optica-test")

        coVerify(exactly = 1) { downloadCoordinator.downloadResumenDiario("optica-test") }
    }

    @Test
    fun syncFinanzas_includes_configuracionFinanciera_in_download_sequence() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 0
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 0
        coEvery { uploadCoordinator.uploadCostosProductos(any()) } returns 0
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadRegalos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 2
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 1
        coEvery { downloadCoordinator.downloadCostosProductos(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosBiselado(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadRegalos(any()) } returns 0
        coEvery { downloadCoordinator.downloadGastosOperativos(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper
        )

        useCase("optica-test")

        coVerify(exactly = 1) { downloadCoordinator.downloadConfiguracionFinanciera("optica-test") }
    }

    @Test
    fun syncFinanzas_download_sequence_includes_finanzas_entities() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 0
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 0
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 2
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 1
        coEvery { downloadCoordinator.downloadCostosProductos(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosBiselado(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadRegalos(any()) } returns 0
        coEvery { downloadCoordinator.downloadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadCostosProductos(any()) } returns 0
        coEvery { uploadCoordinator.uploadRegalos(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper
        )

        useCase("optica-test")

        coVerifyOrder {
            downloadCoordinator.downloadResumenDiario("optica-test")
            downloadCoordinator.downloadConfiguracionFinanciera("optica-test")
            downloadCoordinator.downloadCostosProductos("optica-test")
            downloadCoordinator.downloadCostosBiselado("optica-test")
            downloadCoordinator.downloadPagos("optica-test")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Bug 1 (Part 2): partial upload skips download — smoke test
    // ═══════════════════════════════════════════════════════════════════════════
    // Bug 3 RED: pushPendingDeletions inside try block
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun pushPendingDeletions_error_returns_ResourceError_not_crash() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } throws IOException("Network failure")

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper
        )

        val result = useCase("optica-test")

        assertTrue(result is com.example.optoapp.data.Resource.Error)
        val error = result as com.example.optoapp.data.Resource.Error
        assertTrue(error.message.orEmpty().contains("sincronizando finanzas"))
    }

    @Test
    fun pushPendingDeletions_generic_error_returns_ResourceError() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } throws
                RuntimeException("Unexpected deletion error")

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper
        )

        val result = useCase("optica-test")

        assertTrue(result is com.example.optoapp.data.Resource.Error)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Bug 2 RED: single upload failure does not abort entire sync
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun single_upload_failure_continues_to_next_steps() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } throws
                IOException("Timeout uploading dispensaciones")
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 2
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 1
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 3
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 0
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper
        )

        useCase("optica-test")

        coVerify(exactly = 1) { uploadCoordinator.uploadDispensaciones("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadDispensacionItems("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadServicios("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadPagos("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadGastosOperativos("optica-test") }
    }

    @Test
    fun single_upload_failure_still_runs_download() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadPagos(any()) } throws
                IOException("Pagos upload failed")
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 2
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 0
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper
        )

        useCase("optica-test")

        coVerify(exactly = 1) { uploadCoordinator.uploadGastosOperativos("optica-test") }
        coVerify(exactly = 1) { downloadCoordinator.downloadDispensaciones("optica-test") }
    }
}
