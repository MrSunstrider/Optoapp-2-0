package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
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
    fun migration_47_48_backfills_null_and_blank_updatedAt() {
        val joined = capturedSql()
        assertTrue(joined.contains("UPDATE montura_movimientos"))
        assertTrue(joined.contains("updatedAt"))
        assertTrue(joined.contains("NULLIF(updatedAt, '')"))
        assertTrue(joined.contains("updatedAt IS NULL OR updatedAt = ''"))
    }

    @Test
    fun migration_47_48_uses_room_camelcase() {
        val joined = capturedSql()
        assertTrue(joined.contains("updatedAt"))
        assertTrue(!joined.contains("updated_at"))
    }
}
