package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.data.SyncStateTracker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class DeletionSyncHelperTest {

    private val opticaId = "optica-test"

    @Before
    fun setUpLog() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private class FakeRemoteDeletionHelper(
        repository: OptoRepository,
        tracker: SyncStateTracker,
        private val failure: Exception?,
    ) : DeletionSyncHelper(repository, mockk(relaxed = true), tracker) {
        val deleted = mutableListOf<Pair<String, String>>()

        override suspend fun deleteRemote(table: String, id: String, opticaId: String) {
            deleted += table to id
            failure?.let { throw it }
        }
    }

    private fun tombstone(type: String, id: String) =
        SyncEntityState(entityType = type, entityId = id, status = "deleted", opticaId = opticaId)

    private fun repoWith(vararg tombstones: SyncEntityState): OptoRepository = mockk<OptoRepository>().also {
        coEvery { it.getPendingDeletions(opticaId) } returns tombstones.toList()
        coEvery { it.clearDeletionState(any(), any(), any()) } returns Unit
    }

    private fun helper(repository: OptoRepository, tracker: SyncStateTracker, failure: Exception? = null) =
        FakeRemoteDeletionHelper(repository, tracker, failure)

    @Test
    fun injectAnnotation_isPresentOnTheConstructor() {
        val hasInject = DeletionSyncHelper::class.java.declaredConstructors
            .flatMap { it.annotations.toList() }
            .any { it.annotationClass.qualifiedName?.contains("Inject") == true }
        assertTrue(hasInject)
    }

    @Test
    fun eachSyncedEntityType_isDeletedFromItsTable() = runTest {
        val expected = listOf(
            "servicio_extra" to "servicios_extra",
            "dispensacion" to "dispensaciones",
            "pago" to "pagos",
            "gasto_operativo" to "gastos_operativos",
            "dispensacion_item" to "dispensacion_items",
            "servicio_extra_item" to "servicio_extra_items",
            "regalo_servicio_extra" to "regalos_servicio_extra",
        )
        val repository = repoWith(*expected.map { (type, _) -> tombstone(type, "id-$type") }.toTypedArray())
        val fake = helper(repository, mockk(relaxed = true))

        fake.pushPendingDeletions(opticaId)

        assertEquals(expected.map { (type, table) -> table to "id-$type" }, fake.deleted)
        expected.forEach { (type, _) ->
            coVerify { repository.clearDeletionState(opticaId, type, "id-$type") }
        }
    }

    @Test
    fun unknownEntityType_isClearedWithoutARemoteDelete() = runTest {
        val repository = repoWith(tombstone("venta", "v1"))
        val fake = helper(repository, mockk(relaxed = true))

        fake.pushPendingDeletions(opticaId)

        assertTrue(fake.deleted.isEmpty())
        coVerify { repository.clearDeletionState(opticaId, "venta", "v1") }
    }

    @Test
    fun emptyPendingDeletions_doNothing() = runTest {
        val repository = repoWith()
        val fake = helper(repository, mockk(relaxed = true))

        fake.pushPendingDeletions(opticaId)

        assertTrue(fake.deleted.isEmpty())
        coVerify(exactly = 0) { repository.clearDeletionState(any(), any(), any()) }
    }

    @Test
    fun deletedIds_areThePendingTombstoneIds() = runTest {
        val repository = repoWith(tombstone("dispensacion", "d1"), tombstone("pago", "p1"))

        val ids = helper(repository, mockk(relaxed = true)).deletedIds(opticaId)

        assertEquals(setOf("d1", "p1"), ids)
    }

    @Test
    fun cancellation_isRethrownAndKeepsTheTombstone() = runTest {
        val repository = repoWith(tombstone("dispensacion", "d1"))
        val fake = helper(repository, mockk(relaxed = true), CancellationException("Cancelled"))

        try {
            fake.pushPendingDeletions(opticaId)
            fail("Should have thrown CancellationException")
        } catch (expected: CancellationException) {
            coVerify(exactly = 0) { repository.clearDeletionState(any(), any(), any()) }
        }
    }

    @Test
    fun failedDelete_keepsTheTombstoneAndContinuesWithTheNext() = runTest {
        val repository = repoWith(tombstone("pago", "p1"), tombstone("pago", "p2"))
        val fake = helper(repository, mockk(relaxed = true), RuntimeException("Unexpected"))

        fake.pushPendingDeletions(opticaId)

        assertEquals(listOf("pagos" to "p1", "pagos" to "p2"), fake.deleted)
        coVerify(exactly = 0) { repository.clearDeletionState(any(), any(), any()) }
    }

    // ── Server refusal of a traced dispensacion delete ─────────────────

    @Test
    fun refusedDispensacionDelete_clearsTheTombstoneAndTellsTheUser() = runTest {
        val repository = repoWith(tombstone("dispensacion", "d1"))
        val tracker = mockk<SyncStateTracker>(relaxed = true)
        val refusal = RuntimeException("dispensacion_has_trace: d1\nURL: https://x/rest/v1/dispensaciones")

        helper(repository, tracker, refusal).pushPendingDeletions(opticaId)

        coVerify { repository.clearDeletionState(opticaId, "dispensacion", "d1") }
        coVerify {
            tracker.markError(
                opticaId, "eliminacion_rechazada", "d1",
                "No se pudo eliminar la orden: tiene pagos o movimientos registrados en otro dispositivo; se restauró.",
            )
        }
    }

    @Test
    fun refusedDeleteNotice_isNotAQuarantine() {
        assertFalse(DeletionSyncHelper.DELETE_REFUSED_MESSAGE.startsWith("quarantine:"))
    }

    @Test
    fun transientFailures_keepTheTombstoneForRetry() = runTest {
        listOf(IOException("timeout"), RuntimeException("503 Service Unavailable")).forEach { failure ->
            val repository = repoWith(tombstone("dispensacion", "d1"))
            val tracker = mockk<SyncStateTracker>(relaxed = true)

            helper(repository, tracker, failure).pushPendingDeletions(opticaId)

            coVerify(exactly = 0) { repository.clearDeletionState(any(), any(), any()) }
            coVerify(exactly = 0) { tracker.markError(any(), any(), any(), any()) }
        }
    }

    @Test
    fun pacienteTombstones_neverReachTheDispensacionGuard() = runTest {
        // Paciente deletes are pushed by SyncPacientesUseCase; the server lets their cascade through the guard.
        val repository = repoWith(tombstone("paciente", "p1"))
        val tracker = mockk<SyncStateTracker>(relaxed = true)
        val fake = helper(repository, tracker, RuntimeException("dispensacion_has_trace: d1"))

        fake.pushPendingDeletions(opticaId)

        assertTrue(fake.deleted.isEmpty())
        coVerify(exactly = 0) { tracker.markError(any(), any(), any(), any()) }
    }

    @Test
    fun successfulDispensacionDelete_clearsTheTombstoneWithoutNotice() = runTest {
        val repository = repoWith(tombstone("dispensacion", "d1"))
        val tracker = mockk<SyncStateTracker>(relaxed = true)
        val fake = helper(repository, tracker)

        fake.pushPendingDeletions(opticaId)

        assertEquals(listOf("dispensaciones" to "d1"), fake.deleted)
        coVerify { repository.clearDeletionState(opticaId, "dispensacion", "d1") }
        coVerify(exactly = 0) { tracker.markError(any(), any(), any(), any()) }
    }
}
