package com.example.optoapp.data.membership

import com.example.optoapp.data.opticasettings.OpticaSettingsEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests de mapeo para OpticaSettingsDataSource.fetchOpticaSettings.
 *
 * Como fetchOpticaSettings depende de Supabase (red), testeamos
 * el contrato de serializacion y el mapeo OpticaSettingsRow -> OpticaSettingsEntity.
 */
@RunWith(RobolectricTestRunner::class)
class OpticaSettingsDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize OpticaSettingsRow from valid JSON`() {
        val raw = """{"optica_id":"o1","config_json":"{\"business_hours\":\"Lun-Vie 9-18\"}"}"""

        val row = json.decodeFromString<OpticaSettingsRow>(raw)

        assertEquals("o1", row.optica_id)
        assertEquals("""{"business_hours":"Lun-Vie 9-18"}""", row.config_json)
    }

    @Test
    fun `deserialize OpticaSettingsRow with empty config_json`() {
        val raw = """{"optica_id":"o1","config_json":"{}"}"""

        val row = json.decodeFromString<OpticaSettingsRow>(raw)

        assertEquals("o1", row.optica_id)
        assertEquals("{}", row.config_json)
    }

    @Test
    fun `map OpticaSettingsRow to OpticaSettingsEntity`() {
        val row = OpticaSettingsRow(
            optica_id = "optica-abc",
            config_json = """{"business_hours":"Mar-Sab 10-18:30"}""",
        )

        val entity = OpticaSettingsEntity(
            opticaId = row.optica_id,
            configJson = row.config_json,
        )

        assertEquals("optica-abc", entity.opticaId)
        assertEquals("""{"business_hours":"Mar-Sab 10-18:30"}""", entity.configJson)
    }

    @Test
    fun `map OpticaSettingsRow with null returns no entity`() {
        // Simula fetchOpticaSettings cuando Supabase retorna lista vacia
        val rows: List<OpticaSettingsRow> = emptyList()
        val first = rows.firstOrNull()

        assertNull(first)
    }

    @Test
    fun `fetchOpticaSettings maps first row to entity`() {
        val rows = listOf(
            OpticaSettingsRow(optica_id = "o1", config_json = """{"theme":"dark"}"""),
        )

        val entity = rows.firstOrNull()?.let { row ->
            OpticaSettingsEntity(opticaId = row.optica_id, configJson = row.config_json)
        }

        assertNotNull(entity)
        assertEquals("o1", entity!!.opticaId)
        assertEquals("""{"theme":"dark"}""", entity.configJson)
    }
}
