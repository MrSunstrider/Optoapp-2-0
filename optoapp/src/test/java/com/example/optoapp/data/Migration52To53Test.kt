package com.example.optoapp.data

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration52To53Test {

    private fun capturedSql(): String {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        val sql = mutableListOf<String>()
        every { db.execSQL(capture(sqlSlot)) } answers { sql.add(sqlSlot.captured) }
        MIGRATION_52_53.migrate(db)
        return sql.joinToString("\n")
    }

    @Test
    fun migration_52_53_is_registered() {
        assertEquals(52, MIGRATION_52_53.startVersion)
        assertEquals(53, MIGRATION_52_53.endVersion)
        assertEquals(MIGRATION_52_53, OptoDatabase.MIGRATION_52_53)
    }

    @Test
    fun migration_52_53_rebuilds_oc_items_and_if_detalle_with_cascade() {
        val joined = capturedSql()
        assertTrue(joined.contains("orden_compra_items_new"))
        assertTrue(joined.contains("inventario_fisico_detalle_new"))
        assertTrue(
            joined.contains("FOREIGN KEY(monturaId) REFERENCES monturas(id) ON DELETE CASCADE"),
        )
        assertTrue(joined.contains("DROP TABLE orden_compra_items"))
        assertTrue(joined.contains("DROP TABLE inventario_fisico_detalle"))
        assertTrue(joined.contains("RENAME TO orden_compra_items"))
        assertTrue(joined.contains("RENAME TO inventario_fisico_detalle"))
    }

    @Test
    fun migration_52_53_preserves_parent_cascades_and_indexes() {
        val joined = capturedSql()
        assertTrue(
            joined.contains("FOREIGN KEY(ordenId) REFERENCES ordenes_compra(id) ON DELETE CASCADE"),
        )
        assertTrue(
            joined.contains(
                "FOREIGN KEY(inventarioId) REFERENCES inventario_fisico(id) ON DELETE CASCADE",
            ),
        )
        assertTrue(joined.contains("index_orden_compra_items_monturaId"))
        assertTrue(joined.contains("index_inventario_fisico_detalle_monturaId"))
        assertTrue(joined.contains("index_inventario_fisico_detalle_inventarioId_monturaId"))
    }
}

