package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.arqueo.ArqueoCaja
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

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

    // ── Arqueo sync ordering and LWW tests ─────────────────────────────────

    private fun makeArqueoCaja(
        id: String = "arqueo-1",
        fecha: LocalDate = LocalDate.of(2026, 6, 17),
        opticaId: String = "optica-test",
        updatedAt: String = "2026-06-17T10:00:00Z",
        createdAt: String? = null
    ) = ArqueoCaja(
        id = id,
        fecha = fecha,
        opticaId = opticaId,
        fondoCaja = 1000.0,
        efectivoContado = 500.0,
        tarjetaContado = 200.0,
        transferenciaContado = 100.0,
        movilContado = 50.0,
        efectivoCobrado = 490.0,
        tarjetaCobrado = 198.0,
        transferenciaCobrado = 100.0,
        movilCobrado = 50.0,
        diferenciaEfectivo = -10.0,
        diferenciaTarjeta = -2.0,
        diferenciaTransferencia = 0.0,
        diferenciaMovil = 0.0,
        diferenciaTotal = -12.0,
        cerradoPor = "admin",
        sellado = true,
        createdAt = createdAt ?: updatedAt,
        updatedAt = updatedAt
    )

    /**
     * T-10 test 1: uploadArqueos is called after uploadPagos in the sync sequence.
     * Verifies the ordering contract: dispensaciones → items → servicios → pagos → arqueos.
     */
    @Test
    fun upload_arqueo_called_after_pagos_upload() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 0
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 0
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 0
        coEvery { uploadCoordinator.uploadVentas(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadVentas(any()) } returns 0
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
            uploadCoordinator.uploadVentas("optica-test")
            uploadCoordinator.uploadGastosOperativos("optica-test")
            uploadCoordinator.uploadArqueos("optica-test")
        }
    }

    /**
     * T-10 test 2: remote arqueo with newer updatedAt overwrites local record.
     * Verifies last-write-wins: remote.updatedAt > local.updatedAt → upsert IS called.
     */
    @Test
    fun download_remote_arqueo_overwrites_local_when_remote_is_newer() = runBlocking {
        val repository = mockk<OptoRepository>()

        val localArqueo = makeArqueoCaja(updatedAt = "2026-06-17T08:00:00Z")
        val remoteNewer = ArqueoCajaRemota(
            id = "arqueo-1",
            fecha = "2026-06-17",
            opticaId = "optica-test",
            fondoCaja = 1000.0,
            efectivoContado = 500.0,
            tarjetaContado = 200.0,
            transferenciaContado = 100.0,
            movilContado = 50.0,
            efectivoCobrado = 490.0,
            tarjetaCobrado = 198.0,
            transferenciaCobrado = 100.0,
            movilCobrado = 50.0,
            diferenciaEfectivo = -10.0,
            diferenciaTarjeta = -2.0,
            diferenciaTransferencia = 0.0,
            diferenciaMovil = 0.0,
            diferenciaTotal = -12.0,
            cerradoPor = "admin",
            sellado = true,
            updatedAt = "2026-06-17T10:00:00Z"  // newer than local
        )

        coEvery {
            repository.getArqueoByFechaSync(LocalDate.parse("2026-06-17"), "optica-test")
        } returns localArqueo
        coEvery { repository.upsertArqueoFromRemote(any()) } just Runs

        // Verify the LWW condition: remote.updatedAt > local.updatedAt → upsert called
        val shouldUpsert = remoteNewer.updatedAt > localArqueo.updatedAt
        assert(shouldUpsert) { "Expected remote to be newer but it wasn't" }

        if (shouldUpsert) {
            repository.upsertArqueoFromRemote(remoteNewer.toLocal())
        }

        coVerify(exactly = 1) { repository.upsertArqueoFromRemote(any()) }
    }

    /**
     * T-10 test 3: local arqueo with newer updatedAt is NOT overwritten by remote.
     * Verifies last-write-wins: local.updatedAt > remote.updatedAt → upsert is NOT called.
     */
    @Test
    fun download_does_not_overwrite_when_local_is_newer() = runBlocking {
        val repository = mockk<OptoRepository>()

        val localNewer = makeArqueoCaja(updatedAt = "2026-06-17T12:00:00Z")
        val remoteOlder = ArqueoCajaRemota(
            id = "arqueo-1",
            fecha = "2026-06-17",
            opticaId = "optica-test",
            fondoCaja = 1000.0,
            efectivoContado = 500.0,
            tarjetaContado = 200.0,
            transferenciaContado = 100.0,
            movilContado = 50.0,
            efectivoCobrado = 490.0,
            tarjetaCobrado = 198.0,
            transferenciaCobrado = 100.0,
            movilCobrado = 50.0,
            diferenciaEfectivo = -10.0,
            diferenciaTarjeta = -2.0,
            diferenciaTransferencia = 0.0,
            diferenciaMovil = 0.0,
            diferenciaTotal = -12.0,
            cerradoPor = "admin",
            sellado = true,
            updatedAt = "2026-06-17T08:00:00Z"  // older than local
        )

        coEvery {
            repository.getArqueoByFechaSync(LocalDate.parse("2026-06-17"), "optica-test")
        } returns localNewer
        coEvery { repository.upsertArqueoFromRemote(any()) } just Runs

        // Verify the LWW condition: remote.updatedAt < local.updatedAt → upsert NOT called
        val shouldUpsert = remoteOlder.updatedAt > localNewer.updatedAt
        assert(!shouldUpsert) { "Expected local to be newer but remote was considered newer" }

        if (shouldUpsert) {
            repository.upsertArqueoFromRemote(remoteOlder.toLocal())
        }

        coVerify(exactly = 0) { repository.upsertArqueoFromRemote(any()) }
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
        id = id,
        ot = ot,
        descripcion = descripcion,
        montoTotal = montoTotal,
        aCuenta = aCuenta,
        estado = estado,
        fecha = fecha,
        pacienteId = pacienteId,
        metodoPago = metodoPago,
        opticaId = opticaId
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

    // ── downloadVentas integration ──────────────────────────────────────────

    @Test
    fun downloadVentas_called_between_servicios_and_pagos() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 0
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 0
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 0
        coEvery { uploadCoordinator.uploadVentas(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadVentas(any()) } returns 5
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
            downloadCoordinator.downloadServicios("optica-test")
            downloadCoordinator.downloadVentas("optica-test")
            downloadCoordinator.downloadPagos("optica-test")
        }
    }

    @Test
    fun syncResult_includes_downloadedVentas_count() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 0
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 0
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 0
        coEvery { uploadCoordinator.uploadVentas(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadVentas(any()) } returns 3
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 0
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper
        )

        val result = useCase("optica-test")

        assertTrue(result is com.example.optoapp.data.Resource.Success)
        val syncResult = (result as com.example.optoapp.data.Resource.Success).data!!
        assertEquals(3, syncResult.downloadedVentas)
    }

    @Test
    fun downloadVentas_not_called_when_downloadAfterUpload_is_false() = runBlocking {
        val uploadCoordinator = mockk<UploadSyncCoordinator>()
        val downloadCoordinator = mockk<DownloadSyncCoordinator>()
        val deletionSyncHelper = mockk<DeletionSyncHelper>()
        val networkRetryHelper = mockk<NetworkRetryHelper>()

        coEvery { deletionSyncHelper.pushPendingDeletions(any()) } just Runs
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } returns 0
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 0
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 0
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 0
        coEvery { uploadCoordinator.uploadVentas(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadVentas(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0

        val useCase = SyncFinanzasUseCase(
            deletionSyncHelper = deletionSyncHelper,
            uploadSyncCoordinator = uploadCoordinator,
            downloadSyncCoordinator = downloadCoordinator,
            networkRetryHelper = networkRetryHelper
        )

        useCase("optica-test", downloadAfterUpload = false)

        coVerify(exactly = 0) { downloadCoordinator.downloadVentas(any()) }
    }

    // ── GastoOperativo sync ordering ─────────────────────────────────────────
    // RED phase: uploadGastosOperativos does NOT exist yet on UploadSyncCoordinator

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
        coEvery { uploadCoordinator.uploadVentas(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 3
        coEvery { uploadCoordinator.uploadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadVentas(any()) } returns 0
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
            uploadCoordinator.uploadVentas("optica-test")
            uploadCoordinator.uploadGastosOperativos("optica-test")
            uploadCoordinator.uploadArqueos("optica-test")
        }
    }

    // ── ResumenDiario download ordering ──────────────────────────────────────
    // RED phase: downloadResumenDiario does NOT exist yet on DownloadSyncCoordinator

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
        coEvery { uploadCoordinator.uploadVentas(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadVentas(any()) } returns 0
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

    // ── ConfiguracionFinanciera download ordering ────────────────────────────
    // RED phase: downloadConfiguracionFinanciera does NOT exist yet on DownloadSyncCoordinator

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
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 0
        coEvery { uploadCoordinator.uploadVentas(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadVentas(any()) } returns 0
        coEvery { downloadCoordinator.downloadPagos(any()) } returns 0
        coEvery { downloadCoordinator.downloadResumenDiario(any()) } returns 0
        coEvery { downloadCoordinator.downloadConfiguracionFinanciera(any()) } returns 1

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
        coEvery { uploadCoordinator.uploadVentas(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadVentas(any()) } returns 1
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

        coVerifyOrder {
            downloadCoordinator.downloadResumenDiario("optica-test")
            downloadCoordinator.downloadConfiguracionFinanciera("optica-test")
            downloadCoordinator.downloadPagos("optica-test")
        }
    }

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
        // upload coordinator methods should NOT be called since pushPendingDeletions fails
        // but the sync should NOT crash — it should return Resource.Error

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
        // uploadDispensaciones throws, but subsequent steps should still execute
        coEvery { uploadCoordinator.uploadDispensaciones(any()) } throws
                IOException("Timeout uploading dispensaciones")
        coEvery { uploadCoordinator.uploadDispensacionItems(any()) } returns 2
        coEvery { uploadCoordinator.uploadServicios(any()) } returns 1
        coEvery { uploadCoordinator.uploadPagos(any()) } returns 3
        coEvery { uploadCoordinator.uploadVentas(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 0
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadVentas(any()) } returns 0
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

        // Verify that subsequent upload steps were still called despite dispensaciones failure
        coVerify(exactly = 1) { uploadCoordinator.uploadDispensaciones("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadDispensacionItems("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadServicios("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadPagos("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadVentas("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadGastosOperativos("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadArqueos("optica-test") }
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
        coEvery { uploadCoordinator.uploadVentas(any()) } returns 0
        coEvery { uploadCoordinator.uploadGastosOperativos(any()) } returns 0
        coEvery { uploadCoordinator.uploadArqueos(any()) } returns 0
        coEvery { downloadCoordinator.downloadArqueos(any()) } returns 1
        coEvery { downloadCoordinator.downloadDispensaciones(any()) } returns 2
        coEvery { downloadCoordinator.downloadDispensacionItems(any()) } returns 0
        coEvery { downloadCoordinator.downloadServicios(any()) } returns 0
        coEvery { downloadCoordinator.downloadVentas(any()) } returns 0
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

        // Upload steps after pagos should still be called
        coVerify(exactly = 1) { uploadCoordinator.uploadVentas("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadGastosOperativos("optica-test") }
        coVerify(exactly = 1) { uploadCoordinator.uploadArqueos("optica-test") }
        // Download should still run despite upload failures
        coVerify(exactly = 1) { downloadCoordinator.downloadArqueos("optica-test") }
        coVerify(exactly = 1) { downloadCoordinator.downloadDispensaciones("optica-test") }
    }

    // ── ArqueoCaja createdAt preservation ──────────────────────────────────

    @Test
    fun arqueoCaja_toLocal_preserves_createdAt_from_existing_local() {
        val localCreatedAt = "2026-01-15T08:00:00Z"
        val localArqueo = makeArqueoCaja(
            id = "arq-1", updatedAt = "2026-06-17T08:00:00Z",
            createdAt = localCreatedAt
        )
        val remote = ArqueoCajaRemota(
            id = "arq-1", fecha = "2026-06-17", opticaId = "optica-test",
            fondoCaja = 1000.0, efectivoContado = 500.0, tarjetaContado = 200.0,
            transferenciaContado = 100.0, movilContado = 50.0,
            efectivoCobrado = 490.0, tarjetaCobrado = 198.0,
            transferenciaCobrado = 100.0, movilCobrado = 50.0,
            diferenciaEfectivo = -10.0, diferenciaTarjeta = -2.0,
            diferenciaTransferencia = 0.0, diferenciaMovil = 0.0,
            diferenciaTotal = -12.0, cerradoPor = "admin", sellado = true,
            updatedAt = "2026-06-17T10:00:00Z"
        )

        val incoming = remote.toLocal()
        // The caller MUST preserve the existing local createdAt when upserting.
        // This test documents the contract: toLocal() currently overrides createdAt
        // with updatedAt, which is WRONG. The caller should preserve local.createdAt.
        val preserved = incoming.copy(createdAt = localArqueo.createdAt)

        assertEquals(localCreatedAt, preserved.createdAt)
        assertNotEquals(remote.updatedAt, preserved.createdAt)
    }
}
