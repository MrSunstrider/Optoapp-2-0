package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class Migration54To55Test {

    private fun capturedSql(): List<String> {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_54_55.migrate(db)
        return sql
    }

    @Test
    fun migration_54_55_is_registered() {
        assertEquals(54, MIGRATION_54_55.startVersion)
        assertEquals(55, MIGRATION_54_55.endVersion)
        assertEquals(MIGRATION_54_55, OptoDatabase.MIGRATION_54_55)
    }

    @Test
    fun migration_54_55_adds_nullable_cancellation_columns_to_both_tables() {
        assertEquals(
            listOf(
                "ALTER TABLE dispensaciones ADD COLUMN motivoAnulacion TEXT",
                "ALTER TABLE dispensaciones ADD COLUMN fechaAnulacion TEXT",
                "ALTER TABLE servicios_extra ADD COLUMN motivoAnulacion TEXT",
                "ALTER TABLE servicios_extra ADD COLUMN fechaAnulacion TEXT",
            ),
            capturedSql(),
        )
    }

    @Test
    fun migration_54_55_does_not_set_defaults_or_re_add_reclamo_origen_id() {
        val joined = capturedSql().joinToString("\n")
        assertFalse(joined.contains("DEFAULT", ignoreCase = true))
        assertFalse(joined.contains("NOT NULL", ignoreCase = true))
        assertFalse(joined.contains("reclamo_origen_id"))
    }
}
