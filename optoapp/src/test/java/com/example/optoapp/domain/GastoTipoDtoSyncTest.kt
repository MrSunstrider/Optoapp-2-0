package com.example.optoapp.domain

import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class GastoTipoDtoSyncTest {

    private val json = Json { encodeDefaults = true }
    private val lenientJson = Json { ignoreUnknownKeys = true }

    private fun gasto(tipoGasto: String?) = GastoOperativoEntity(
        id = "g1",
        opticaId = "optica-1",
        categoria = "marketing",
        monto = BigDecimal("500.00"),
        fecha = LocalDate.of(2026, 7, 10),
        tipoGasto = tipoGasto,
    )

    @Test
    fun explicitTypeIsUploaded() {
        val body = json.encodeToString(GastoOperativoRemoto.serializer(), gasto("variable").toRemoto())

        assertEquals("variable", json.parseToJsonElement(body).jsonObject["tipo_gasto"]?.jsonPrimitive?.content)
    }

    @Test
    fun missingTypeIsUploadedAsNull() {
        val body = json.encodeToString(GastoOperativoRemoto.serializer(), gasto(null).toRemoto())

        assertEquals(JsonNull, json.parseToJsonElement(body).jsonObject["tipo_gasto"])
    }

    @Test
    fun remotePayloadWithoutTheKeyKeepsTheTypeUnsetSoTheCategoryDecides() {
        val payload = """
            {"id":"g1","optica_id":"optica-1","categoria":"marketing","monto":500.0,"fecha":"2026-07-10"}
        """.trimIndent()

        val entity = lenientJson.decodeFromString(GastoOperativoRemoto.serializer(), payload).toEntity()

        assertNull(entity.tipoGasto)
        assertEquals("variable", GastoTipoResolver.resolver(entity.categoria, entity.tipoGasto))
    }

    @Test
    fun remoteExplicitTypeIsStored() {
        val payload = """
            {"id":"g1","optica_id":"optica-1","categoria":"alquiler","monto":500.0,"fecha":"2026-07-10","tipo_gasto":"variable"}
        """.trimIndent()

        val entity = lenientJson.decodeFromString(GastoOperativoRemoto.serializer(), payload).toEntity()

        assertEquals("variable", entity.tipoGasto)
    }
}
