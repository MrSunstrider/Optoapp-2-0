package com.example.optoapp.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_63_64 = object : Migration(63, 64) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Blank updatedAt breaks LWW/sync stamps; fecha is the only pre-existing clock.
        db.execSQL(
            """
            UPDATE feedback_recomendaciones
            SET updatedAt = strftime('%Y-%m-%dT%H:%M:%fZ', fecha / 1000.0, 'unixepoch')
            WHERE TRIM(COALESCE(updatedAt, '')) = ''
            """.trimIndent(),
        )
    }
}
