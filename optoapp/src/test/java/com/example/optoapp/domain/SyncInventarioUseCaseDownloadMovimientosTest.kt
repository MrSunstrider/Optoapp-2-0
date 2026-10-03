package com.example.optoapp.domain

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SyncInventarioUseCaseDownloadMovimientosTest {

    private val opticaId = "optica-download-test"
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val conflictDao = mockk<ConflictDao>(relaxed = true)
    private var remotes = emptyList<MonturaMovimientoRemoto>()

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun local(updatedAt: String?) = MonturaMovimiento(
        id = "mov-1", monturaId = "m1", fecha = LocalDate.of(2026, 8, 25), tipo = "SALIDA_VENTA", cantidad = 1,
        stockPrevio = 5, stockNuevo = 4, referenciaId = "disp-1", nota = "rewritten locally", opticaId = opticaId,
        updatedAt = updatedAt,
    )

    private fun remote(updatedAt: String?, id: String = "mov-1") = MonturaMovimientoRemoto(
        id = id, monturaId = "m1", fecha = "2026-08-25", tipo = "SALIDA_VENTA", cantidad = 1,
        stockPrevio = 5, stockNuevo = 4, referenciaId = "disp-1", nota = "stale server copy", opticaId = opticaId,
        updatedAt = updatedAt,
    )

    private suspend fun downloadOnly(localRows: List<MonturaMovimiento>, remoteRows: List<MonturaMovimientoRemoto>): Resource<InventarioSyncResult> {
        coEvery { repository.getMovimientosMonturaSnapshotForOptica(opticaId) } returns localRows
        remotes = remoteRows
        val useCase = object : SyncInventarioUseCase(
            repository, mockk<SupabaseClient>(relaxed = true), mockk<OptoDatabase>(relaxed = true),
            mockk<SyncStateTracker>(relaxed = true), mockk<ConflictHelper>(relaxed = true), conflictDao,
        ) {
            override suspend fun fetchAllRemoteMonturas(opticaId: String) = emptyList<MonturaRemota>()
            override suspend fun fetchAllRemoteMovimientos(opticaId: String) = remotes
        }
        return useCase(opticaId, downloadAfterUpload = true, skipUpload = true)
    }

    @Test
    fun `a download-only pass keeps a local movimiento that is newer than the server copy`() = runTest {
        val result = downloadOnly(listOf(local("2026-10-03T10:00:00.000Z")), listOf(remote("2026-10-01T10:00:00.000Z")))

        assertTrue(result is Resource.Success)
        coVerify(exactly = 0) { repository.upsertMonturaMovimiento(any()) }
    }

    @Test
    fun `a download replaces a local movimiento that is older than the server copy`() = runTest {
        downloadOnly(listOf(local("2026-10-01T10:00:00.000Z")), listOf(remote("2026-10-03T10:00:00.000Z")))

        coVerify(exactly = 1) { repository.upsertMonturaMovimiento(match { it.id == "mov-1" && it.nota == "stale server copy" }) }
    }

    @Test
    fun `equal timestamps in different formats follow the local-wins convention of the upload filter`() = runTest {
        downloadOnly(listOf(local("2026-10-03T10:00:00Z")), listOf(remote("2026-10-03T10:00:00.000+00:00")))

        coVerify(exactly = 0) { repository.upsertMonturaMovimiento(any()) }
    }

    @Test
    fun `a movimiento the device does not have yet is downloaded`() = runTest {
        downloadOnly(emptyList(), listOf(remote("2026-10-03T10:00:00.000Z")))

        coVerify(exactly = 1) { repository.upsertMonturaMovimiento(match { it.id == "mov-1" }) }
    }

    @Test
    fun `a missing timestamp on either side keeps the server copy authoritative`() = runTest {
        downloadOnly(listOf(local(null)), listOf(remote("2026-10-03T10:00:00.000Z")))
        downloadOnly(listOf(local("2026-10-03T10:00:00.000Z")), listOf(remote(null)))

        coVerify(exactly = 2) { repository.upsertMonturaMovimiento(any()) }
    }

    @Test
    fun `skipped rows still count as downloaded so the sync summary stays stable`() = runTest {
        val result = downloadOnly(
            listOf(local("2026-10-03T10:00:00.000Z")),
            listOf(remote("2026-10-01T10:00:00.000Z"), remote("2026-10-03T10:00:00.000Z", id = "mov-2")),
        )

        assertEquals(2, result.data?.downloadedMovimientos)
    }
}
