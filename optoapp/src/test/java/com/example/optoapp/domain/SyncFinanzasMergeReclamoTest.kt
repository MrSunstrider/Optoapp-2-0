package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SyncFinanzasMergeReclamoTest {

    private val repository = mockk<OptoRepository>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val handler = DispensacionMergeHandler(repository, syncStateTracker)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun disp(id: String, ot: String, day: Int, reclamoOrigenId: String? = null) = DispensacionOptica(
        id = id,
        ot = ot,
        pacienteId = "p1",
        fecha = LocalDate.of(2026, 9, day),
        opticaId = "o1",
        reclamoOrigenId = reclamoOrigenId,
    )

    @Test
    fun originalAndSuffixedReplacement_areNeverMerged() = runTest {
        coEvery { repository.getDispensacionesSnapshotForOptica("o1") } returns listOf(
            disp("orig", "OT-2026-0042", day = 1),
            disp("repl", "OT-2026-0042-R1", day = 20, reclamoOrigenId = "orig"),
        )

        handler.resolveLocalDuplicateDispensaciones("o1")

        coVerify(exactly = 0) { repository.deleteDispensacionById(any(), any()) }
        coVerify(exactly = 0) { repository.reassignPagosDispensacion(any(), any(), any()) }
        coVerify(exactly = 0) { repository.reassignItemsDispensacion(any(), any(), any()) }
        coVerify(exactly = 0) { repository.reassignRegalosDispensacion(any(), any(), any()) }
    }

    @Test
    fun trueDuplicateOfTheOriginal_stillMergesWhileTheReplacementStaysApart() = runTest {
        coEvery { repository.getDispensacionesSnapshotForOptica("o1") } returns listOf(
            disp("orig-old", " ot-2026-0042 ", day = 1),
            disp("orig-new", "OT-2026-0042", day = 5),
            disp("repl", "OT-2026-0042-R1", day = 20, reclamoOrigenId = "orig-new"),
        )

        handler.resolveLocalDuplicateDispensaciones("o1")

        coVerify(exactly = 1) { repository.reassignPagosDispensacion("orig-old", "orig-new", "o1") }
        coVerify(exactly = 1) { repository.reassignItemsDispensacion("orig-old", "orig-new", "o1") }
        coVerify(exactly = 1) { repository.reassignRegalosDispensacion("orig-old", "orig-new", "o1") }
        coVerify(exactly = 1) { repository.deleteDispensacionById("orig-old", "o1") }
        coVerify(exactly = 0) { repository.deleteDispensacionById("repl", any()) }
        coVerify(exactly = 0) { repository.reassignPagosDispensacion("repl", any(), any()) }
    }
}
