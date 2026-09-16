package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration53To54Test {

    private fun capturedSql(): String {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_53_54.migrate(db)
        return sql.joinToString("\n")
    }

    @Test
    fun migration_53_54_is_registered() {
        assertEquals(53, MIGRATION_53_54.startVersion)
        assertEquals(54, MIGRATION_53_54.endVersion)
        assertEquals(MIGRATION_53_54, OptoDatabase.MIGRATION_53_54)
    }

    @Test
    fun migration_53_54_creates_child_tables_and_backfill() {
        val joined = capturedSql()
        assertTrue(joined.contains("CREATE TABLE IF NOT EXISTS servicio_extra_items"))
        assertTrue(joined.contains("CREATE TABLE IF NOT EXISTS regalos_servicio_extra"))
        assertTrue(joined.contains("FOREIGN KEY(servicio_extra_id) REFERENCES servicios_extra(id) ON DELETE CASCADE"))
        assertTrue(joined.contains("INSERT INTO servicio_extra_items"))
        assertTrue(joined.contains("FROM servicios_extra"))
        assertTrue(joined.contains("index_servicio_extra_items_servicio_extra_id"))
        assertTrue(joined.contains("index_regalos_servicio_extra_servicio_extra_id"))
    }
}
