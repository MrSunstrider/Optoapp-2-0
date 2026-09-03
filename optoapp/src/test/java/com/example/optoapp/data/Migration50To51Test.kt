package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration50To51Test {

    private fun capturedSql(): String {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_50_51.migrate(db)
        return sql.joinToString("\n")
    }

    @Test
    fun migration_50_51_is_registered() {
        assertEquals(50, MIGRATION_50_51.startVersion)
        assertEquals(51, MIGRATION_50_51.endVersion)
        assertEquals(MIGRATION_50_51, OptoDatabase.MIGRATION_50_51)
    }

    @Test
    fun migration_50_51_adds_monturaId_to_servicios_extra() {
        val joined = capturedSql()
        assertTrue(joined.contains("ALTER TABLE servicios_extra ADD COLUMN monturaId TEXT"))
        assertTrue(!joined.contains("updated_at"))
    }
}
