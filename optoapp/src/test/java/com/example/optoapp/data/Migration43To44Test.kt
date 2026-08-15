package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration43To44Test {
    @Test
    fun migration_43_44_versions_and_sql() {
        assertEquals(43, MIGRATION_43_44.startVersion)
        assertEquals(44, MIGRATION_43_44.endVersion)
        assertEquals(MIGRATION_43_44, OptoDatabase.MIGRATION_43_44)
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers {
            sql.add(sqlSlot.captured)
        }
        MIGRATION_43_44.migrate(db)
        val joined = sql.joinToString("\n")
        assertTrue(joined.contains("ALTER TABLE pagos ADD COLUMN reversaPagoId TEXT"))
        assertTrue(joined.contains("ABS(monto)"))
        assertTrue(joined.contains("Anulación"))
    }
}
