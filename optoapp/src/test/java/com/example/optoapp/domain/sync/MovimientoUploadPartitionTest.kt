package com.example.optoapp.domain.sync

import com.example.optoapp.data.MonturaMovimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MovimientoUploadPartitionTest {

    private val opticaId = "o1"

    private fun mov(
        id: String,
        referenciaId: String = "disp-1",
        tipo: String = "SALIDA_VENTA",
        monturaId: String = "m1",
        stockNuevo: Int = 4,
    ) = MonturaMovimiento(
        id = id,
        monturaId = monturaId,
        fecha = LocalDate.of(2026, 8, 25),
        tipo = tipo,
        cantidad = 1,
        stockPrevio = stockNuevo + 1,
        stockNuevo = stockNuevo,
        referenciaId = referenciaId,
        opticaId = opticaId,
    )

    @Test
    fun partitionMovimientosForUpload_reconcilesWhenCompositeKeyMatchesWithDifferentId() {
        val local = mov(id = "uuid-new")
        val remote = mov(id = "uuid-old")
        val remoteByKey = mapOf(
            Triple(remote.referenciaId, remote.tipo, remote.monturaId) to remote,
        )

        val partition = ConflictHelper.partitionMovimientosForUpload(listOf(local), remoteByKey)

        assertTrue(partition.toUpload.isEmpty())
        assertEquals(1, partition.toReconcileLocally.size)
        assertEquals("uuid-new", partition.toReconcileLocally[0].first.id)
        assertEquals("uuid-old", partition.toReconcileLocally[0].second)
    }

    @Test
    fun partitionMovimientosForUpload_uploadsWhenNoRemoteMatch() {
        val local = mov(id = "uuid-new", referenciaId = "disp-new")
        val partition = ConflictHelper.partitionMovimientosForUpload(listOf(local), emptyMap())

        assertEquals(listOf(local), partition.toUpload)
        assertTrue(partition.toReconcileLocally.isEmpty())
    }

    @Test
    fun partitionMovimientosForUpload_uploadsWhenSameIdAsRemote() {
        val local = mov(id = "uuid-same")
        val remote = mov(id = "uuid-same")
        val remoteByKey = mapOf(
            Triple(remote.referenciaId, remote.tipo, remote.monturaId) to remote,
        )

        val partition = ConflictHelper.partitionMovimientosForUpload(listOf(local), remoteByKey)

        assertEquals(listOf(local), partition.toUpload)
        assertTrue(partition.toReconcileLocally.isEmpty())
    }
}
