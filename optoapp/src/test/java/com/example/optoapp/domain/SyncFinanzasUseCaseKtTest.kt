package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.FinanzasRemoteDefaults
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
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
        opticaId: String = "test-optica",
    ) = ServicioRemoto(
        id = id, ot = ot, descripcion = descripcion,
        montoTotal = montoTotal, aCuenta = aCuenta, estado = estado,
        fecha = fecha, pacienteId = pacienteId, metodoPago = metodoPago, opticaId = opticaId,
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

    @Test
    fun syncFinanzas_includes_gastosOperativos_in_upload_sequence() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
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
            networkRetryHelper = networkRetryHelper,
        )

        useCase("optica-test")

        coVerifyOrder {
            uploadCoordinator.uploadPagos("optica-test")
            uploadCoordinator.uploadGastosOperativos("optica-test")
        }
    }

    @Test
    fun syncFinanzas_includes_resumenDiario_in_download_sequence() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
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
            networkRetryHelper = networkRetryHelper,
        )

        useCase("optica-test")

        coVerify(exactly = 1) { downloadCoordinator.downloadResumenDiario("optica-test") }
    }

    @Test
    fun syncFinanzas_includes_configuracionFinanciera_in_download_sequence() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
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
            networkRetryHelper = networkRetryHelper,
        )

        useCase("optica-test")

        coVerify(exactly = 1) { downloadCoordinator.downloadConfiguracionFinanciera("optica-test") }
    }

    @Test
    fun syncFinanzas_uploads_configuracionFinanciera_before_download() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
        val downloadCoordinator = mockk<DownloadSyncCoordinator>(relaxed = true)
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadConfiguracionFinanciera(any()) } returns 1
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 1

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper,
        )

        val result = useCase("optica-test")
        assertTrue(result is com.example.optoapp.data.Resource.Success)
        assertEquals(1, (result as com.example.optoapp.data.Resource.Success).data!!.uploadedConfiguracionesFinancieras)

        coVerifyOrder {
            uploadCoordinator.uploadConfiguracionFinanciera("optica-test")
            downloadCoordinator.downloadConfiguracionFinanciera("optica-test")
        }
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
        coEvery { uploadCoordinator.uploadCostosBiselado(any()) } returns 0
        coEvery { uploadCoordinator.uploadRegalos(any()) } returns 0
        coEvery { uploadCoordinator.uploadConfiguracionFinanciera(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper,
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
    // WHY: Verifies that a single upload failure doesn't prevent downloads — partial success must still sync

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
            networkRetryHelper = networkRetryHelper,
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
            networkRetryHelper = networkRetryHelper,
        )

        val result = useCase("optica-test")

        assertTrue(result is com.example.optoapp.data.Resource.Error)
    }
    // WHY: UploadPartialException is truthful Resource.Error with partial counts in data

    @Test
    fun `partial upload via UploadPartialException continues to next steps`() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } throws
            UploadPartialException(5, IOException("Partial"))
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 2
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 1
        coEvery { uploadCoordinator.uploadCostosProductos(any()) } returns 0
        coEvery { uploadCoordinator.uploadCostosBiselado(any()) } returns 0
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 3
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadRegalos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 0
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosProductos(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosBiselado(any()) } returns 0
        coEvery { downloadCoordinator.downloadRegalos(any()) } returns 0
        coEvery { downloadCoordinator.downloadGastosOperativos(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper,
        )

        val result = useCase("optica-test")

        coVerify(exactly = 1) { uploadCoordinator.uploadDispensaciones("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadPagos("optica-test") }
        assertTrue(result is com.example.optoapp.data.Resource.Error)
        val err = result as com.example.optoapp.data.Resource.Error
        assertEquals(5, err.data!!.uploadedDispensaciones)
        assertEquals(3, err.data!!.uploadedPagos)
    }

    // WHY: These tests verify safeUpload's new error propagation contract — IOException after retry exhaustion,
    // RestException immediate throw, and UploadPartialException partial-count passthrough

    @Test
    fun `full network failure propagates IOException to Resource Error`() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } throws IOException("Network failure")
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } throws IOException("Network failure")
        coEvery { uploadCoordinator.uploadServicios(any()) } throws IOException("Network failure")
        coEvery { uploadCoordinator.uploadCostosProductos(any()) } throws IOException("Network failure")
        coEvery { uploadCoordinator.uploadCostosBiselado(any()) } throws IOException("Network failure")
        coEvery { uploadCoordinator.uploadPagos(any()) } throws IOException("Network failure")
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } throws IOException("Network failure")
        coEvery { uploadCoordinator.uploadRegalos(any()) } throws IOException("Network failure")
        // Mock downloads to return 0 so download phase doesn't throw before IOException propagates
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosProductos(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosBiselado(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 0
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 0
        coEvery { downloadCoordinator.downloadRegalos(any()) } returns 0
        coEvery { downloadCoordinator.downloadGastosOperativos(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper,
        )

        val result = useCase("optica-test")
        assertTrue("IOException should propagate to Resource.Error", result is com.example.optoapp.data.Resource.Error)
    }

    @Test
    fun `UploadPartialException returns partial count`() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } throws UploadPartialException(5, IOException("Partial"))
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 3
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 2
        coEvery { uploadCoordinator.uploadCostosProductos(any()) } returns 0
        coEvery { uploadCoordinator.uploadCostosBiselado(any()) } returns 0
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 4
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadRegalos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 0
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosProductos(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosBiselado(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadRegalos(any()) } returns 0
        coEvery { downloadCoordinator.downloadGastosOperativos(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper,
        )

        val result = useCase("optica-test")
        assertTrue(result is com.example.optoapp.data.Resource.Error)
        val error = result as com.example.optoapp.data.Resource.Error
        assertEquals(
            "UploadPartialException should preserve partial count of 5",
            5, error.data!!.uploadedDispensaciones,
        )
    }

    @Test
    fun `401 RestException propagates immediately to Resource Error`() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        val mockResponse = mockk<HttpResponse>(relaxed = true)
        every { mockResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } throws
            io.github.jan.supabase.exceptions.RestException("Unauthorized", null, mockResponse)

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper,
        )

        val result = useCase("optica-test")
        assertTrue("401 RestException should propagate to Resource.Error", result is com.example.optoapp.data.Resource.Error)
    }

    @Test
    fun `nonAuth RestException propagates to Resource Error`() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        val mockResponse500 = mockk<HttpResponse>(relaxed = true)
        every { mockResponse500.status } returns HttpStatusCode(500, "Server Error")
        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } throws
            io.github.jan.supabase.exceptions.RestException("Server error", null, mockResponse500)
        // Mock downloads so the test only passes if 500 propagates from safeUpload
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosProductos(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosBiselado(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 0
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 0
        coEvery { downloadCoordinator.downloadRegalos(any()) } returns 0
        coEvery { downloadCoordinator.downloadGastosOperativos(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper,
        )

        val result = useCase("optica-test")
        assertTrue("Non-auth RestException should propagate to Resource.Error", result is com.example.optoapp.data.Resource.Error)
    }

    @Test
    fun `generic Exception propagates to Resource Error`() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } throws RuntimeException("Unexpected error")
        // Mock downloads so the test only passes if generic Exception propagates from safeUpload
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosProductos(any()) } returns 0
        coEvery { downloadCoordinator.downloadCostosBiselado(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 0
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 0
        coEvery { downloadCoordinator.downloadRegalos(any()) } returns 0
        coEvery { downloadCoordinator.downloadGastosOperativos(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper,
        )

        val result = useCase("optica-test")
        assertTrue("Generic Exception should propagate to Resource.Error", result is com.example.optoapp.data.Resource.Error)
    }

    @Test
    fun `IOException in safeUpload aborts remaining uploads and returns Resource Error`() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>(relaxed = true)
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadPagos(any()) } throws
            IOException("Pagos upload failed")
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 0
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 0
        coEvery { uploadCoordinator.uploadCostosProductos(any()) } returns 0
        coEvery { uploadCoordinator.uploadCostosBiselado(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadRegalos(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper,
        )

        val result = useCase("optica-test")

        // Initial call + 3 retries = 4 total
        coVerify(exactly = 4) { uploadCoordinator.uploadPagos("optica-test") }
        assertTrue("IOException should propagate to Resource.Error", result is com.example.optoapp.data.Resource.Error)
    }
}
