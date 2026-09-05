package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration51To52Test {

    private fun capturedSql(): String {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_51_52.migrate(db)
        return sql.joinToString("\n")
    }

    @Test
    fun migration_51_52_is_registered() {
        assertEquals(51, MIGRATION_51_52.startVersion)
        assertEquals(52, MIGRATION_51_52.endVersion)
        assertEquals(MIGRATION_51_52, OptoDatabase.MIGRATION_51_52)
    }

    @Test
    fun migration_51_52_drops_sku_only_unique_and_creates_sku_tipoAro_unique() {
        val joined = capturedSql()
        assertTrue(joined.contains("DROP INDEX IF EXISTS index_monturas_sku_opticaId"))
        assertTrue(joined.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_monturas_sku_opticaId_tipoAro"))
        assertTrue(joined.contains("ON monturas(sku, opticaId, tipoAro)"))
    }

    @Test
    fun migration_51_52_uses_room_camelcase() {
        val joined = capturedSql()
        assertTrue(joined.contains("opticaId"))
        assertTrue(joined.contains("tipoAro"))
        assertTrue(!joined.contains("optica_id"))
        assertTrue(!joined.contains("tipo_aro"))
    }
}
