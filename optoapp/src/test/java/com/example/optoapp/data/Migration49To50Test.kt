package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration49To50Test {

    private fun capturedSql(): String {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_49_50.migrate(db)
        return sql.joinToString("\n")
    }

    @Test
    fun migration_49_50_is_registered() {
        assertEquals(49, MIGRATION_49_50.startVersion)
        assertEquals(50, MIGRATION_49_50.endVersion)
        assertEquals(MIGRATION_49_50, OptoDatabase.MIGRATION_49_50)
    }

    @Test
    fun migration_49_50_backfills_null_and_blank_proveedores_updatedAt() {
        val joined = capturedSql()
        assertTrue(joined.contains("UPDATE proveedores"))
        assertTrue(joined.contains("updatedAt"))
        assertTrue(joined.contains("TRIM(COALESCE(updatedAt, ''))"))
        assertTrue(joined.contains("updatedAt IS NULL OR TRIM(COALESCE(updatedAt, '')) = ''"))
    }

    @Test
    fun migration_49_50_uses_room_camelcase() {
        val joined = capturedSql()
        assertTrue(joined.contains("updatedAt"))
        assertTrue(!joined.contains("updated_at"))
    }
}
