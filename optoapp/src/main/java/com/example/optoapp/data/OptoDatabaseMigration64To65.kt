package com.example.optoapp.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_64_65 = object : Migration(64, 65) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE configuracion_financiera ADD COLUMN margenBajoAlertaPct REAL NOT NULL DEFAULT 10.0",
        )
        db.execSQL(
            "ALTER TABLE configuracion_financiera ADD COLUMN margenAltoAlertaPct REAL NOT NULL DEFAULT 35.0",
        )
        db.execSQL(
            "ALTER TABLE configuracion_financiera ADD COLUMN contribucionAltaRatio REAL NOT NULL DEFAULT 0.25",
        )
        db.execSQL(
            "ALTER TABLE configuracion_financiera ADD COLUMN gastoFueraRangoRatio REAL NOT NULL DEFAULT 0.4",
        )
    }
}
