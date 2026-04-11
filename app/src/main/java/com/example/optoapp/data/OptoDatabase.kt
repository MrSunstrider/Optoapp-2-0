package com.example.optoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Paciente::class, EvaluacionClinica::class, DispensacionOptica::class, Pago::class, ServicioExtra::class],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OptoDatabase : RoomDatabase() {
    abstract fun pacienteDao(): PacienteDao
    abstract fun evaluacionDao(): EvaluacionDao
    abstract fun dispensacionDao(): DispensacionDao
    abstract fun pagoDao(): PagoDao
    abstract fun servicioExtraDao(): ServicioExtraDao

    companion object {
        @Volatile
        private var INSTANCE: OptoDatabase? = null

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Añadir campo multi-inquilino a todas las tablas principales
                // Los registros existentes quedan asignados a 'mi_optica_base'
                db.execSQL("ALTER TABLE pacientes ADD COLUMN opticaId TEXT NOT NULL DEFAULT 'mi_optica_base'")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN opticaId TEXT NOT NULL DEFAULT 'mi_optica_base'")
                db.execSQL("ALTER TABLE dispensaciones ADD COLUMN opticaId TEXT NOT NULL DEFAULT 'mi_optica_base'")
                db.execSQL("ALTER TABLE servicios_extra ADD COLUMN opticaId TEXT NOT NULL DEFAULT 'mi_optica_base'")
                db.execSQL("ALTER TABLE pagos ADD COLUMN opticaId TEXT NOT NULL DEFAULT 'mi_optica_base'")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Agudeza visual (Nuevos campos Cerca AO)
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN avScAoCerca TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN avCcAoCerca TEXT NOT NULL DEFAULT ''")

                // Visión binocular y Percepción
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN estereopsisValor TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN estereopsisSegundos TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN lang TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN worth TEXT NOT NULL DEFAULT ''")

                // Percepción color
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN ishihara TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN farnsworth TEXT NOT NULL DEFAULT ''")

                // Salud Ocular y Función Visual
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN schirmerOd TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN schirmerOi TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN osdiPuntuacion INTEGER")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN osdiClasificacion TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN sensibilidadContraste TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN sensibilidadFrecuencia TEXT NOT NULL DEFAULT ''")

                // Otras Pruebas
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN amsler TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN campoVisual TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evaluaciones ADD COLUMN campoVisualDescripcion TEXT NOT NULL DEFAULT ''")
            }
        }


        fun getDatabase(context: Context): OptoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OptoDatabase::class.java,
                    "opto_database"
                )
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration(true) // Permitir recrear tablas ante cambios de índices en desarrollo
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
