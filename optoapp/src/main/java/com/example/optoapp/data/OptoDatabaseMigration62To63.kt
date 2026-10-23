package com.example.optoapp.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_62_63 = object : Migration(62, 63) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE feedback_recomendaciones ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''",
        )
        db.execSQL(
            "UPDATE feedback_recomendaciones SET estado = 'EN_CURSO' WHERE estado = 'MAS_TARDE'",
        )
    }
}
