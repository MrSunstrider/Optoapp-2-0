package com.example.optoapp.domain

import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import com.example.optoapp.domain.sync.MovimientoUploadPlan
import io.github.jan.supabase.SupabaseClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class SyncInventarioUseCaseUploadTest {

    private val opticaId = "optica-upload-test"

    private val repository = mockk<OptoRepository>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val database = mockk<OptoDatabase>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val conflictHelper = mockk<ConflictHelper>(relaxed = true)
    private val conflictDao = mockk<com.example.optoapp.data.ConflictDao>(relaxed = true)

    private val uploadedBatches = mutableListOf<List<MonturaMovimientoRemoto>>()

    @Before
    fun setUp() {
        mockkStatic("android.util.Log")
        coEvery { syncStateTracker.markSynced(any(), any(), any()) } just Runs
        coEvery { syncStateTracker.markError(any(), any(), any(), any()) } just Runs
        uploadedBatches.clear()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun mov(id: String, referenciaId: String = "disp-1") = MonturaMovimiento(
        id = id,
        monturaId = "m1",
        fecha = LocalDate.of(2026, 8, 25),
        tipo = "SALIDA_VENTA",
        cantidad = 1,
        stockPrevio = 5,
        stockNuevo = 4,
        referenciaId = referenciaId,
        opticaId = opticaId,
    )

    private fun stubMonturasUploadEmpty() {
        coEvery { repository.getMonturasSnapshotForOptica(opticaId) } returns emptyList()
        coEvery { conflictHelper.filterConflicts(any(), any(), any(), any()) } returns emptyList()
    }

    private fun createUseCase(): SyncInventarioUseCase = object : SyncInventarioUseCase(
        repository, supabase, database, syncStateTracker, conflictHelper, conflictDao,
    ) {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        override suspend fun upsertMovimientosBatch(chunk: List<MonturaMovimientoRemoto>) {
            uploadedBatches.add(chunk)
        }
    }

    @Test
    fun uploadMovimientos_skipsRemoteUpsert_whenCompositeKeyExistsWithDifferentId() = runTest {
        stubMonturasUploadEmpty()
        val local = mov(id = "uuid-new")
        val remote = mov(id = "uuid-old")
        val key = Triple(remote.referenciaId, remote.tipo, remote.monturaId)

        coEvery { repository.getMovimientosMonturaSnapshotForOptica(opticaId) } returns listOf(local)
        coEvery { conflictHelper.filterConflictMovimientos(opticaId, any()) } returns MovimientoUploadPlan(
            safeIds = listOf(local.id),
            remoteByKey = mapOf(key to remote),
            conflictedIds = emptyList(),
        )
        coEvery { repository.upsertMonturaMovimiento(any()) } just Runs

        val result = createUseCase().invoke(opticaId, downloadAfterUpload = false)

        assertTrue(result is Resource.Success)
        assertTrue(uploadedBatches.isEmpty())
        coVerify(exactly = 1) {
            repository.upsertMonturaMovimiento(match { it.id == "uuid-old" })
        }
        coVerify(exactly = 1) {
            repository.deleteMonturaMovimiento("uuid-new", opticaId)
        }
        coVerify { syncStateTracker.markSynced(opticaId, "montura_movimiento", "uuid-old") }
        coVerify { syncStateTracker.markSynced(opticaId, "montura_movimiento", "uuid-new") }
        assertEquals(0, result.data?.uploadedMovimientos)
        assertEquals(1, result.data?.reconciledMovimientos)
    }

    @Test
    fun uploadMovimientos_failsClosed_whenRemoteFetchFails() = runTest {
        stubMonturasUploadEmpty()
        val local = mov(id = "uuid-new")

        coEvery { repository.getMovimientosMonturaSnapshotForOptica(opticaId) } returns listOf(local)
        coEvery { conflictHelper.filterConflictMovimientos(opticaId, any()) } returns MovimientoUploadPlan(
            safeIds = listOf(local.id),
            remoteByKey = emptyMap(),
            conflictedIds = emptyList(),
            remoteFetchSucceeded = false,
        )

        val result = createUseCase().invoke(opticaId, downloadAfterUpload = false)

        assertTrue(result is Resource.Error)
        assertTrue(uploadedBatches.isEmpty())
        coVerify { syncStateTracker.markError(opticaId, "upload_montura_movimientos", "batch", any()) }
    }

    @Test
    fun uploadMovimientos_uploadsWhenNoRemoteMatch() = runTest {
        stubMonturasUploadEmpty()
        val local = mov(id = "uuid-new", referenciaId = "disp-new")

        coEvery { repository.getMovimientosMonturaSnapshotForOptica(opticaId) } returns listOf(local)
        coEvery { conflictHelper.filterConflictMovimientos(opticaId, any()) } returns MovimientoUploadPlan(
            safeIds = listOf(local.id),
            remoteByKey = emptyMap(),
            conflictedIds = emptyList(),
        )

        createUseCase().invoke(opticaId, downloadAfterUpload = false)

        assertEquals(1, uploadedBatches.size)
        assertEquals("uuid-new", uploadedBatches.single().single().id)
    }

    @Test
    fun uploadMovimientos_doesNotUploadConflictedMovimientos() = runTest {
        stubMonturasUploadEmpty()
        val local = mov(id = "uuid-conflict")

        coEvery { repository.getMovimientosMonturaSnapshotForOptica(opticaId) } returns listOf(local)
        coEvery { conflictHelper.filterConflictMovimientos(opticaId, any()) } returns MovimientoUploadPlan(
            safeIds = emptyList(),
            remoteByKey = emptyMap(),
            conflictedIds = listOf(local.id),
        )

        val result = createUseCase().invoke(opticaId, downloadAfterUpload = false)

        assertTrue(uploadedBatches.isEmpty())
        assertEquals(0, result.data?.uploadedMovimientos)
        assertEquals(0, result.data?.reconciledMovimientos)
    }

    @Test
    fun uploadMovimientos_reconcileRunsInsideTransaction() = runTest {
        stubMonturasUploadEmpty()
        val local = mov(id = "uuid-new")
        val remote = mov(id = "uuid-old")
        val key = Triple(remote.referenciaId, remote.tipo, remote.monturaId)
        var transactionDepth = 0

        coEvery { repository.getMovimientosMonturaSnapshotForOptica(opticaId) } returns listOf(local)
        coEvery { conflictHelper.filterConflictMovimientos(opticaId, any()) } returns MovimientoUploadPlan(
            safeIds = listOf(local.id),
            remoteByKey = mapOf(key to remote),
            conflictedIds = emptyList(),
        )
        coEvery { repository.upsertMonturaMovimiento(any()) } coAnswers {
            assertTrue("reconcile upsert must run inside transaction", transactionDepth > 0)
        }
        coEvery { repository.deleteMonturaMovimiento(any(), any()) } coAnswers {
            assertTrue("reconcile delete must run inside transaction", transactionDepth > 0)
        }

        val useCase = object : SyncInventarioUseCase(
            repository, supabase, database, syncStateTracker, conflictHelper, conflictDao,
        ) {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T {
                transactionDepth++
                return try {
                    block()
                } finally {
                    transactionDepth--
                }
            }

            override suspend fun upsertMovimientosBatch(chunk: List<MonturaMovimientoRemoto>) {
                uploadedBatches.add(chunk)
            }
        }

        useCase.invoke(opticaId, downloadAfterUpload = false)
    }

    @Test
    fun uploadMovimientos_marksErrorWhenReconcileFails() = runTest {
        stubMonturasUploadEmpty()
        val local = mov(id = "uuid-new")
        val remote = mov(id = "uuid-old")
        val key = Triple(remote.referenciaId, remote.tipo, remote.monturaId)

        coEvery { repository.getMovimientosMonturaSnapshotForOptica(opticaId) } returns listOf(local)
        coEvery { conflictHelper.filterConflictMovimientos(opticaId, any()) } returns MovimientoUploadPlan(
            safeIds = listOf(local.id),
            remoteByKey = mapOf(key to remote),
            conflictedIds = emptyList(),
        )
        coEvery { repository.upsertMonturaMovimiento(any()) } throws IOException("room write failed")

        val result = createUseCase().invoke(opticaId, downloadAfterUpload = false)

        assertTrue(result is Resource.Error)
        coVerify { syncStateTracker.markError(opticaId, "upload_montura_movimientos", "batch", any()) }
    }
}
