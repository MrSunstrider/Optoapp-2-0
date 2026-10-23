package com.example.optoapp.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_61_62 = object : Migration(61, 62) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE configuracion_financiera ADD COLUMN impactoMargenObjetivoPct REAL NOT NULL DEFAULT 25.0",
        )
        db.execSQL(
            "ALTER TABLE configuracion_financiera ADD COLUMN impactoRecuperoStockPct REAL NOT NULL DEFAULT 0.70",
        )
        db.execSQL(
            "ALTER TABLE configuracion_financiera ADD COLUMN impactoUpsideVentasPct REAL NOT NULL DEFAULT 0.10",
        )
        db.execSQL(
            "ALTER TABLE configuracion_financiera ADD COLUMN impactoCorteGastoPct REAL NOT NULL DEFAULT 0.10",
        )
        db.execSQL(
            "ALTER TABLE configuracion_financiera ADD COLUMN impactoRecuperoCobranzaPct REAL NOT NULL DEFAULT 1.0",
        )
    }
}
