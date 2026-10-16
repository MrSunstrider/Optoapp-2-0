package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncFinanzasDtoCostoOrigenTest {

    // WHY: mirrors the Supabase client serializer (SupabaseModule), so the wire body matches what PostgREST receives.
    private val wireJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private fun item(costoOrigen: String?) = DispensacionItem(
        id = "item-1",
        dispensacionId = "disp-1",
        opticaId = "optica-1",
        costoRealOd = 18.0,
        costoOrigen = costoOrigen,
    )

    @Test
    fun upload_body_carries_costo_origen() {
        val body = wireJson.parseToJsonElement(wireJson.encodeToString(item("matriz").toRemoto())).jsonObject

        assertEquals("matriz", body.getValue("costo_origen").jsonPrimitive.content)
    }

    @Test
    fun upload_body_sends_null_costo_origen_for_legacy_items() {
        val body = wireJson.parseToJsonElement(wireJson.encodeToString(item(null).toRemoto())).jsonObject

        assertTrue(body.containsKey("costo_origen"))
        assertEquals(JsonNull, body.getValue("costo_origen"))
    }

    @Test
    fun remote_row_without_costo_origen_maps_to_null() {
        val payload = """{"id":"item-1","dispensacion_id":"disp-1","optica_id":"optica-1","costo_real_od":25.0}"""

        val entity = wireJson.decodeFromString<DispensacionItemRemota>(payload).toEntity()

        assertNull(entity.costoOrigen)
        assertEquals(25.0, entity.costoRealOd!!, 0.0)
    }

    @Test
    fun round_trip_preserves_mixto() {
        val wire = wireJson.encodeToString(item("mixto").toRemoto())

        val entity = wireJson.decodeFromString<DispensacionItemRemota>(wire).toEntity()

        assertEquals("mixto", entity.costoOrigen)
    }

    @Test
    fun round_trip_preserves_faltante() {
        val wire = wireJson.encodeToString(item("faltante").toRemoto())

        val entity = wireJson.decodeFromString<DispensacionItemRemota>(wire).toEntity()

        assertEquals("faltante", entity.costoOrigen)
    }
}
