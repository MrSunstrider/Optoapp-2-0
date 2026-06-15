package com.example.optoapp.data

import com.example.optoapp.domain.DispensacionRemota
import com.example.optoapp.domain.toRemoto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Verifies the full upload→download→upload timestamp roundtrip is stable.
 *
 * Before RC-1 + RC-2, the sequence was broken in two ways:
 *  1. toRemoto() replaced null updatedAt with Instant.now() on every call → new timestamp
 *     each upload cycle → server saw a "newer" local value → false conflict.
 *  2. downloadAfterUpload=false meant Room never received the server-confirmed updated_at
 *     → on the next cycle, toRemoto() used the old (or null) local value → divergence again.
 *
 * With both fixes in place, the chain must be:
 *  - Upload: local T1 → server receives T1 → server stores T2 (trigger sets updated_at)
 *  - Download: Room receives T2 → entity.updatedAt = T2
 *  - Second upload: toRemoto() sends T2 → server sees T2 == T2 → no conflict
 */
class SyncIntegrationTest {

    private val t1 = "2026-01-01T10:00:00Z"
    private val t2 = "2026-06-15T12:00:00Z"
    private val testDate = LocalDate.of(2026, 1, 1)

    private fun dispensacion(updatedAt: String? = null) = DispensacionOptica(
        id = "d-integration",
        pacienteId = "p-1",
        fecha = testDate,
        opticaId = "optica-test",
        updatedAt = updatedAt
    )

    @Test
    fun uploadDownloadRoundtrip_timestampStable() {
        val localEntity = dispensacion(updatedAt = t1)

        val uploaded = localEntity.toRemoto()
        assertEquals(
            "Upload must send the Room timestamp T1 — not a fabricated Instant.now()",
            t1, uploaded.updatedAt
        )

        val serverResponse = DispensacionRemota(
            id = localEntity.id,
            pacienteId = localEntity.pacienteId,
            fecha = localEntity.fecha.toString(),
            opticaId = localEntity.opticaId,
            updatedAt = t2
        )
        val downloadedEntity = serverResponse.toEntity()
        assertEquals(
            "Download must write the server-confirmed T2 to the Room entity",
            t2, downloadedEntity.updatedAt
        )

        val secondUpload = downloadedEntity.toRemoto()
        assertEquals(
            "Second upload must send T2 — toRemoto() must not replace it with Instant.now()",
            t2, secondUpload.updatedAt
        )

        assertEquals(
            "Roundtrip must be stable: local T2 == remote T2 means no timestamp drift across cycles",
            secondUpload.updatedAt, t2
        )
    }

    @Test
    fun uploadDownloadRoundtrip_secondCycleProducesNoTimestampChange() {
        val entity = dispensacion(updatedAt = t2)

        val firstUpload = entity.toRemoto()
        val secondUpload = entity.toRemoto()

        assertEquals(
            "Repeated toRemoto() calls on the same post-download entity must yield identical " +
                "updatedAt — any difference is phantom drift that triggers false conflicts",
            firstUpload.updatedAt, secondUpload.updatedAt
        )
    }
}
