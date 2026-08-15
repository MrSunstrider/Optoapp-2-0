package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration44To45Test {

    private fun capturedSql(): String {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_44_45.migrate(db)
        return sql.joinToString("\n")
    }

    @Test
    fun migration_44_45_is_registered() {
        assertEquals(44, MIGRATION_44_45.startVersion)
        assertEquals(45, MIGRATION_44_45.endVersion)
        assertEquals(MIGRATION_44_45, OptoDatabase.MIGRATION_44_45)
    }

    @Test
    fun migration_44_45_purges_only_correlated_phantom_venta_rows() {
        val joined = capturedSql()

        assertTrue("must delete from montura_movimientos", joined.contains("DELETE FROM montura_movimientos"))
        assertTrue("must target tipo 'venta'", joined.contains("tipo = 'venta'"))
        assertTrue("must require the phantom note", joined.contains("nota = 'venta_dispensacion'"))
        assertTrue("must require a SALIDA_VENTA twin", joined.contains("SALIDA_VENTA"))
        assertTrue("correlation must be an EXISTS subquery", joined.contains("EXISTS"))
    }

    @Test
    fun migration_44_45_uses_room_camelcase_columns() {
        val joined = capturedSql()

        listOf("referenciaId", "monturaId", "opticaId").forEach { column ->
            assertTrue("must correlate on $column", joined.contains(column))
        }
        listOf("referencia_id", "montura_id", "optica_id").forEach { column ->
            assertTrue("snake_case $column matches nothing in Room", !joined.contains(column))
        }
    }

    @Test
    fun migration_44_45_does_not_touch_stock_or_salida_rows() {
        val joined = capturedSql()

        assertTrue("must not rewrite stockActual", !joined.contains("UPDATE monturas"))
        assertEquals("SALIDA_VENTA may only appear in the correlation subquery", 1, joined.split("SALIDA_VENTA").size - 1)
        assertTrue(
            "SALIDA_VENTA must sit inside the EXISTS clause, never in the DELETE predicate",
            joined.indexOf("EXISTS") in 0 until joined.indexOf("SALIDA_VENTA"),
        )
    }
}
