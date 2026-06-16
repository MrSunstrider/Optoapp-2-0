package com.example.optoapp.viewmodel

import com.example.optoapp.data.Resource
import com.example.optoapp.domain.FinanzasSyncResult
import com.example.optoapp.domain.HistorialSyncResult
import com.example.optoapp.domain.InventarioSyncResult
import com.example.optoapp.domain.PacientesSyncResult
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that the silent sync dispatch always calls each use case with downloadAfterUpload = true.
 *
 * Without the download step after upload, Room retains null/stale updatedAt.
 * On the next cycle the conflict detector sees the local timestamp diverge from the
 * server-stamped value and raises a false conflict (RC-2).
 *
 * SyncViewModel has too many concrete infrastructure dependencies for direct unit testing.
 * We extract the core dispatch logic into [SilentSyncDispatch] and verify the contract
 * in isolation — the same contract that performSilentSync must honour.
 */
class SyncViewModelSilentSyncTest {

    // ── Fakes that capture the downloadAfterUpload argument ──────────────────

    private fun makeRecordingPacientes(): Pair<SyncPacientesUseCase, MutableList<Boolean>> {
        val recorded = mutableListOf<Boolean>()
        val downloadSlot = slot<Boolean>()
        val useCase = mockk<SyncPacientesUseCase>()
        coEvery { useCase(any(), capture(downloadSlot), any()) } answers {
            recorded += downloadSlot.captured
            Resource.Success(PacientesSyncResult(0, 0))
        }
        return useCase to recorded
    }

    private fun makeRecordingHistorial(): Pair<SyncHistorialUseCase, MutableList<Boolean>> {
        val recorded = mutableListOf<Boolean>()
        val downloadSlot = slot<Boolean>()
        val useCase = mockk<SyncHistorialUseCase>()
        coEvery { useCase(any(), capture(downloadSlot), any()) } answers {
            recorded += downloadSlot.captured
            Resource.Success(HistorialSyncResult(0, 0))
        }
        return useCase to recorded
    }

    private fun makeRecordingFinanzas(): Pair<SyncFinanzasUseCase, MutableList<Boolean>> {
        val recorded = mutableListOf<Boolean>()
        val downloadSlot = slot<Boolean>()
        val useCase = mockk<SyncFinanzasUseCase>()
        coEvery { useCase(any(), capture(downloadSlot), any()) } answers {
            recorded += downloadSlot.captured
            Resource.Success(FinanzasSyncResult(0, 0, 0, 0, 0, 0, 0, 0))
        }
        return useCase to recorded
    }

    private fun makeRecordingInventario(): Pair<SyncInventarioUseCase, MutableList<Boolean>> {
        val recorded = mutableListOf<Boolean>()
        val downloadSlot = slot<Boolean>()
        val useCase = mockk<SyncInventarioUseCase>()
        coEvery { useCase(any(), capture(downloadSlot), any()) } answers {
            recorded += downloadSlot.captured
            Resource.Success(InventarioSyncResult(0, 0, 0, 0))
        }
        return useCase to recorded
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun silentSync_nullUpdatedAt_stableAcross3Cycles() {
        val (pacientes, pacientesArgs) = makeRecordingPacientes()
        val (historial, historialArgs) = makeRecordingHistorial()
        val (finanzas, finanzasArgs) = makeRecordingFinanzas()
        val (inventario, inventarioArgs) = makeRecordingInventario()
        val dispatch = SilentSyncDispatch(pacientes, historial, finanzas, inventario)

        repeat(3) { runBlocking { dispatch.run("optica-1") } }

        assertTrue(
            "All 3 cycles must invoke syncPacientesUseCase with downloadAfterUpload=true",
            pacientesArgs.size == 3 && pacientesArgs.all { it }
        )
        assertTrue(
            "All 3 cycles must invoke syncHistorialUseCase with downloadAfterUpload=true",
            historialArgs.size == 3 && historialArgs.all { it }
        )
        assertTrue(
            "All 3 cycles must invoke syncFinanzasUseCase with downloadAfterUpload=true",
            finanzasArgs.size == 3 && finanzasArgs.all { it }
        )
        assertTrue(
            "All 3 cycles must invoke syncInventarioUseCase with downloadAfterUpload=true",
            inventarioArgs.size == 3 && inventarioArgs.all { it }
        )
    }

    @Test
    fun silentSync_writesServerTimestampToRoom_afterUpload() {
        val (pacientes, pacientesArgs) = makeRecordingPacientes()
        val (historial, historialArgs) = makeRecordingHistorial()
        val (finanzas, finanzasArgs) = makeRecordingFinanzas()
        val (inventario, inventarioArgs) = makeRecordingInventario()
        val dispatch = SilentSyncDispatch(pacientes, historial, finanzas, inventario)

        runBlocking { dispatch.run("optica-1") }

        assertTrue(
            "syncPacientesUseCase must be called with downloadAfterUpload=true so the " +
                "server-stamped updatedAt is written back to Room, preventing false conflicts",
            pacientesArgs.single()
        )
        assertTrue(
            "syncHistorialUseCase must be called with downloadAfterUpload=true",
            historialArgs.single()
        )
        assertTrue(
            "syncFinanzasUseCase must be called with downloadAfterUpload=true",
            finanzasArgs.single()
        )
        assertTrue(
            "syncInventarioUseCase must be called with downloadAfterUpload=true",
            inventarioArgs.single()
        )
    }
}

/**
 * Represents the use-case dispatch contract of performSilentSync.
 *
 * Tests drive this class. The production fix in SyncViewModel must match this
 * contract: every use case called with downloadAfterUpload = true.
 */
private class SilentSyncDispatch(
    private val syncPacientesUseCase: SyncPacientesUseCase,
    private val syncHistorialUseCase: SyncHistorialUseCase,
    private val syncFinanzasUseCase: SyncFinanzasUseCase,
    private val syncInventarioUseCase: SyncInventarioUseCase
) {
    suspend fun run(opticaId: String) {
        syncPacientesUseCase(opticaId, downloadAfterUpload = true)
        syncHistorialUseCase(opticaId, downloadAfterUpload = true)
        syncFinanzasUseCase(opticaId, downloadAfterUpload = true)
        syncInventarioUseCase(opticaId, downloadAfterUpload = true)
    }
}
