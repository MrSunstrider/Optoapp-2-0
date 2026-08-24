package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration46To47Test {

    private fun capturedSql(): String {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_46_47.migrate(db)
        return sql.joinToString("\n")
    }

    @Test
    fun migration_46_47_is_registered() {
        assertEquals(46, MIGRATION_46_47.startVersion)
        assertEquals(47, MIGRATION_46_47.endVersion)
        assertEquals(MIGRATION_46_47, OptoDatabase.MIGRATION_46_47)
    }

    @Test
    fun migration_46_47_adds_tipo_default_monturas() {
        val joined = capturedSql()
        assertTrue(joined.contains("ALTER TABLE proveedores"))
        assertTrue(joined.contains("ADD COLUMN tipo"))
        assertTrue(joined.contains("DEFAULT 'monturas'"))
        assertTrue(joined.contains("NOT NULL"))
    }

    @Test
    fun migration_46_47_uses_room_camelcase_table() {
        val joined = capturedSql()
        assertTrue(joined.contains("proveedores"))
        assertTrue(joined.contains(" tipo "))
    }
}
