package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.optoapp.domain.sync.LEGACY_NULL_UPDATED_AT
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration47To48Test {

    private fun capturedSql(): String {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_47_48.migrate(db)
        return sql.joinToString("\n")
    }

    @Test
    fun migration_47_48_is_registered() {
        assertEquals(47, MIGRATION_47_48.startVersion)
        assertEquals(48, MIGRATION_47_48.endVersion)
        assertEquals(MIGRATION_47_48, OptoDatabase.MIGRATION_47_48)
    }

    @Test
    fun migration_47_48_sql_contains_null_blank_updatedAt_backfill() {
        val joined = capturedSql()
        assertTrue(joined.contains("UPDATE montura_movimientos"))
        assertTrue(joined.contains("updatedAt"))
        assertTrue(joined.contains("TRIM(COALESCE(updatedAt, ''))"))
        assertTrue(joined.contains("updatedAt IS NULL OR TRIM(COALESCE(updatedAt, '')) = ''"))
        assertTrue(joined.contains(LEGACY_NULL_UPDATED_AT))
        assertTrue(!joined.contains("strftime"))
    }

    @Test
    fun migration_47_48_uses_room_camelcase() {
        val joined = capturedSql()
        assertTrue(joined.contains("updatedAt"))
        assertTrue(!joined.contains("updated_at"))
    }
}
