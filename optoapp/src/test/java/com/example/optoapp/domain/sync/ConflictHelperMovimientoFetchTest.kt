package com.example.optoapp.domain.sync

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ConflictHelperMovimientoFetchTest {

    private val opticaId = "optica-fetch"

    private fun mov(id: String) = MonturaMovimiento(
        id = id,
        monturaId = "m1",
        fecha = LocalDate.of(2026, 8, 25),
        tipo = "SALIDA_VENTA",
        cantidad = 1,
        stockPrevio = 5,
        stockNuevo = 4,
        referenciaId = "disp-$id",
        opticaId = opticaId,
    )

    @Test
    fun fetchRemoteMovimientos_paginatesUntilPartialPage() = runTest {
        val page1 = (1..500).map { mov("page1-$it") }
        val page2 = listOf(mov("page2-1"))
        var call = 0

        val helper = object : ConflictHelper(mockk<SupabaseClient>(), mockk(relaxed = true), mockk<ConflictDao>()) {
            override suspend fun fetchRemoteMovimientosPage(
                opticaId: String,
                from: Long,
                to: Long,
            ): List<MonturaMovimiento> = when (call++) {
                0 -> {
                    assertEquals(0L, from)
                    assertEquals(499L, to)
                    page1
                }
                else -> {
                    assertEquals(500L, from)
                    assertEquals(999L, to)
                    page2
                }
            }
        }

        val all = helper.fetchRemoteMovimientos(opticaId)

        assertEquals(501, all.size)
        assertEquals(2, call)
    }
}
