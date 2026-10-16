package com.example.optoapp.domain

import com.example.optoapp.data.PendingRecalcMarker
import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.data.SyncStateTracker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class ResumenRecalcCoordinatorTest {

    private val opticaId = "o1"
    private val markedAt = 1_000L
    private val tracker = mockk<SyncStateTracker>(relaxed = true)
    private val networkRetryHelper = mockk<NetworkRetryHelper>()
    private val llamadas = mutableListOf<Pair<LocalDate, LocalDate>>()
    private val fallan = mutableSetOf<LocalDate>()

    private val coordinator = object : ResumenRecalcCoordinator(tracker, networkRetryHelper, mockk(relaxed = true)) {
        override suspend fun recalcularRango(opticaId: String, desde: LocalDate, hasta: LocalDate) {
            llamadas += desde to hasta
            if (desde in fallan) throw IOException("offline")
        }
    }

    @Before
    fun setUp() {
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers { secondArg<suspend () -> Unit>().invoke() }
    }

    private fun marker(fecha: LocalDate, causeType: String = "dispensacion", causeId: String = "d1") =
        PendingRecalcMarker(fecha, causeType, causeId, markedAt)

    private fun estado(type: String, id: String, status: String = "synced", updatedAt: Long = markedAt + 10, lastError: String = "") {
        coEvery { tracker.stateOf(opticaId, type, id) } returns SyncEntityState(opticaId, type, id, status, lastError, updatedAt)
    }

    private fun dispensacionSubida(id: String = "d1", updatedAt: Long = markedAt + 10) {
        estado("dispensacion", id, updatedAt = updatedAt)
        estado("upload_dispensacion_items", "batch")
        estado("upload_regalos", "batch")
    }

    @Test
    fun `eligible marker is recalculated through NetworkRetryHelper and cleared`() = runTest {
        val m = marker(LocalDate.of(2026, 7, 10))
        coEvery { tracker.pendingRecalcMarkers(opticaId) } returns listOf(m)
        dispensacionSubida()

        val dias = coordinator.flush(opticaId)

        assertEquals(listOf(LocalDate.of(2026, 7, 10) to LocalDate.of(2026, 7, 10)), llamadas)
        assertEquals(1, dias)
        coVerify { networkRetryHelper.retryNetwork(match { it.contains("recalcular_resumen_diario_rango") }, any()) }
        coVerify(exactly = 1) { tracker.clearPendingRecalc(opticaId, m) }
    }

    @Test
    fun `servicio marker needs its servicio item and gift batches synced`() = runTest {
        val m = marker(LocalDate.of(2026, 7, 10), causeType = "servicio_extra", causeId = "s1")
        coEvery { tracker.pendingRecalcMarkers(opticaId) } returns listOf(m)
        estado("servicio_extra", "s1")
        estado("upload_servicio_extra_items", "batch")
        estado("upload_regalos_servicio_extra", "batch", status = "error")

        coordinator.flush(opticaId)

        assertEquals(emptyList<Pair<LocalDate, LocalDate>>(), llamadas)
        estado("upload_regalos_servicio_extra", "batch")
        coordinator.flush(opticaId)
        assertEquals(1, llamadas.size)
        coVerify(exactly = 1) { tracker.clearPendingRecalc(opticaId, m) }
    }

    @Test
    fun `cause synced before the marker keeps it pending`() = runTest {
        coEvery { tracker.pendingRecalcMarkers(opticaId) } returns listOf(marker(LocalDate.of(2026, 7, 10)))
        dispensacionSubida(updatedAt = markedAt - 1)

        assertEquals(0, coordinator.flush(opticaId))

        assertEquals(emptyList<Pair<LocalDate, LocalDate>>(), llamadas)
        coVerify(exactly = 0) { tracker.clearPendingRecalc(any(), any()) }
    }

    @Test
    fun `error, quarantined, conflicted or missing cause keeps the marker and sends no RPC`() = runTest {
        val fecha = LocalDate.of(2026, 7, 10)
        coEvery { tracker.pendingRecalcMarkers(opticaId) } returns listOf(
            marker(fecha, causeId = "err"),
            marker(fecha, causeId = "quar"),
            marker(fecha, causeId = "conf"),
            marker(fecha, causeId = "missing"),
        )
        estado("dispensacion", "err", status = "error")
        estado("dispensacion", "quar", status = "error", lastError = "quarantine:parent_missing")
        estado("dispensacion", "conf", status = "conflicted")
        coEvery { tracker.stateOf(opticaId, "dispensacion", "missing") } returns null
        estado("upload_dispensacion_items", "batch")
        estado("upload_regalos", "batch")

        coordinator.flush(opticaId)

        assertEquals(emptyList<Pair<LocalDate, LocalDate>>(), llamadas)
        coVerify(exactly = 0) { tracker.clearPendingRecalc(any(), any()) }
    }

    @Test
    fun `child batch not synced keeps the marker`() = runTest {
        coEvery { tracker.pendingRecalcMarkers(opticaId) } returns listOf(marker(LocalDate.of(2026, 7, 10)))
        dispensacionSubida()
        estado("upload_dispensacion_items", "batch", updatedAt = markedAt - 1)

        coordinator.flush(opticaId)

        assertEquals(emptyList<Pair<LocalDate, LocalDate>>(), llamadas)
    }

    @Test
    fun `RPC fails in sync 1, sync 2 uploads nothing, marker flushed and cleared`() = runTest {
        val fecha = LocalDate.of(2026, 7, 10)
        val m = marker(fecha)
        coEvery { tracker.pendingRecalcMarkers(opticaId) } returns listOf(m)
        dispensacionSubida()
        fallan += fecha

        assertEquals(0, coordinator.flush(opticaId))
        coVerify(exactly = 0) { tracker.clearPendingRecalc(any(), any()) }

        fallan.clear()
        assertEquals(1, coordinator.flush(opticaId))
        assertEquals(2, llamadas.size)
        coVerify(exactly = 1) { tracker.clearPendingRecalc(opticaId, m) }
    }

    @Test
    fun `40 contiguous days plus an isolated date split into windows of at most 31 days`() {
        val contiguos = (0L until 40L).map { LocalDate.of(2026, 6, 1).plusDays(it) }
        val aislado = LocalDate.of(2026, 9, 1)

        val ventanas = ResumenRecalcCoordinator.ventanas(contiguos.reversed() + aislado + contiguos.first())

        assertEquals(
            listOf(
                VentanaRecalc(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1)),
                VentanaRecalc(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 10)),
                VentanaRecalc(aislado, aislado),
            ),
            ventanas,
        )
    }

    @Test
    fun `only markers of windows whose RPC succeeded are cleared`() = runTest {
        val julio = marker(LocalDate.of(2026, 7, 10))
        val agosto = marker(LocalDate.of(2026, 8, 2))
        coEvery { tracker.pendingRecalcMarkers(opticaId) } returns listOf(julio, agosto)
        dispensacionSubida()
        fallan += LocalDate.of(2026, 8, 2)

        val dias = coordinator.flush(opticaId)

        assertEquals(1, dias)
        assertEquals(2, llamadas.size)
        coVerify(exactly = 1) { tracker.clearPendingRecalc(opticaId, julio) }
        coVerify(exactly = 0) { tracker.clearPendingRecalc(opticaId, agosto) }
    }

    @Test
    fun `a tracker failure never throws`() = runTest {
        coEvery { tracker.pendingRecalcMarkers(opticaId) } throws IllegalStateException("db closed")

        assertEquals(0, coordinator.flush(opticaId))
    }
}
