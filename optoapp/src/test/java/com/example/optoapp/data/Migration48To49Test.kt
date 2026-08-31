package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration48To49Test {

    private fun capturedSql(): String {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_48_49.migrate(db)
        return sql.joinToString("\n")
    }

    @Test
    fun migration_48_49_is_registered() {
        assertEquals(48, MIGRATION_48_49.startVersion)
        assertEquals(49, MIGRATION_48_49.endVersion)
        assertEquals(MIGRATION_48_49, OptoDatabase.MIGRATION_48_49)
    }

    @Test
    fun migration_48_49_sql_contains_null_blank_monturas_updatedAt_backfill() {
        val joined = capturedSql()
        assertTrue(joined.contains("UPDATE monturas"))
        assertTrue(joined.contains("updatedAt"))
        assertTrue(joined.contains("TRIM(COALESCE(updatedAt, ''))"))
        assertTrue(joined.contains("updatedAt IS NULL OR TRIM(COALESCE(updatedAt, '')) = ''"))
    }

    @Test
    fun migration_48_49_uses_room_camelcase() {
        val joined = capturedSql()
        assertTrue(joined.contains("updatedAt"))
        assertTrue(!joined.contains("updated_at"))
    }
}
