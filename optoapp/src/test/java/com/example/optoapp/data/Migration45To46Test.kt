package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration45To46Test {

    private fun capturedSql(): String {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_45_46.migrate(db)
        return sql.joinToString("\n")
    }

    @Test
    fun migration_45_46_is_registered() {
        assertEquals(45, MIGRATION_45_46.startVersion)
        assertEquals(46, MIGRATION_45_46.endVersion)
        assertEquals(MIGRATION_45_46, OptoDatabase.MIGRATION_45_46)
    }

    @Test
    fun migration_45_46_backfills_blank_referencia_with_row_id() {
        val joined = capturedSql()
        assertTrue(joined.contains("UPDATE montura_movimientos"))
        assertTrue(joined.contains("referenciaId = id"))
        assertTrue(
            "must only touch blank/null referenciaId",
            joined.contains("referenciaId IS NULL") || joined.contains("referenciaId = ''"),
        )
    }

    @Test
    fun migration_45_46_uses_room_camelcase() {
        val joined = capturedSql()
        assertTrue(joined.contains("referenciaId"))
        assertTrue(!joined.contains("referencia_id"))
    }
}
