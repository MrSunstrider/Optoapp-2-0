package com.example.optoapp.domain.sync

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * TDD tests for FR-05b: [ConflictHelper.filterConflictMovimientos] MUST persist
 * conflicts to [ConflictDao.upsertConflict] (not just mark in SyncStateTracker).
 *
 * Uses a testable subclass that overrides the remote fetch seam
 * (fetchRemoteMovimientos — extracted in GREEN phase) to inject canned data.
 */
@RunWith(RobolectricTestRunner::class)
class ConflictHelperMovimientoPersistenceTest {

    private val opticaId = "optica-mov-persist"

    private val mockSupabase: SupabaseClient = mockk(relaxed = true)
    private val mockTracker: SyncStateTracker = mockk(relaxed = true)
    private val mockConflictDao: ConflictDao = mockk(relaxed = true)

    private val movIdConflict = "mov-conflict-1"
    private val movIdSafe = "mov-safe-2"
    private val monturaId = "m1"
    private val refId = "ref-1"

    private fun crearMovimiento(
        id: String,
        stockNuevo: Int = 5,
        tipo: String = "ENTRADA",
    ) = MonturaMovimiento(
        id = id, monturaId = monturaId, tipo = tipo,
        cantidad = 1, stockPrevio = stockNuevo - 1, stockNuevo = stockNuevo,
        referenciaId = refId, nota = "", opticaId = opticaId,
        fecha = LocalDate.now(),
    )

    @Test
    fun filterConflictMovimientos_callsUpsertConflictForStockDiscrepancies() = runTest {
        coEvery {
            mockConflictDao.upsertConflict(any(), any(), any(), any(), any(), any())
        } just Runs
        coEvery {
            mockTracker.markConflicted(any(), any(), any())
        } just Runs

        // Remote has stockNuevo=10, local has stockNuevo=5 → conflict
        val remoteData = listOf(crearMovimiento(movIdConflict, stockNuevo = 10))
        val localData = listOf(crearMovimiento(movIdConflict, stockNuevo = 5))

        val helper = TestableMovimientoConflictHelper(
            mockSupabase,
            mockTracker,
            mockConflictDao,
            remoteData,
        )

        helper.filterConflictMovimientos(opticaId, localData)

        coVerify(exactly = 1) {
            mockConflictDao.upsertConflict(
                entityId = movIdConflict,
                opticaId = opticaId,
                entityType = "montura_movimiento",
                localSnapshot = "",
                remoteSnapshot = "",
                detectedAt = any(),
            )
        }
    }

    @Test
    fun filterConflictMovimientos_doesNotUpsertForNonConflictedMovimientos() = runTest {
        coEvery {
            mockConflictDao.upsertConflict(any(), any(), any(), any(), any(), any())
        } just Runs
        coEvery {
            mockTracker.markConflicted(any(), any(), any())
        } just Runs

        // Both local and remote have same stockNuevo → safe
        val remoteData = listOf(crearMovimiento(movIdSafe, stockNuevo = 5))
        val localData = listOf(crearMovimiento(movIdSafe, stockNuevo = 5))

        val helper = TestableMovimientoConflictHelper(
            mockSupabase,
            mockTracker,
            mockConflictDao,
            remoteData,
        )

        helper.filterConflictMovimientos(opticaId, localData)

        coVerify(exactly = 0) {
            mockConflictDao.upsertConflict(any(), any(), any(), any(), any(), any())
        }
    }
}

/**
 * Testable subclass that overrides [ConflictHelper.fetchRemoteMovimientos]
 * (extracted in GREEN phase) to inject canned remote data instead of hitting Supabase.
 */
internal open class TestableMovimientoConflictHelper(
    supabase: SupabaseClient,
    syncStateTracker: SyncStateTracker,
    conflictDao: ConflictDao,
    private val cannedRemoteMovimientos: List<MonturaMovimiento>,
) : ConflictHelper(supabase, syncStateTracker, conflictDao) {

    override suspend fun fetchRemoteMovimientos(opticaId: String): List<MonturaMovimiento> = cannedRemoteMovimientos
}
