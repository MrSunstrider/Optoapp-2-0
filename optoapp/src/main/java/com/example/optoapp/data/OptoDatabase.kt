package com.example.optoapp.data

import android.content.Context
import com.example.optoapp.BuildConfig
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.util.LocalDatabaseBackupManager

@Database(
    entities = [
        Paciente::class, EvaluacionClinica::class, DispensacionOptica::class, Pago::class, ServicioExtra::class,
        Montura::class, MonturaMovimiento::class, SyncEntityState::class
    ],
    version = 20,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OptoDatabase : RoomDatabase() {
    abstract fun pacienteDao(): PacienteDao
    abstract fun evaluacionDao(): EvaluacionDao
    abstract fun dispensacionDao(): DispensacionDao
    abstract fun pagoDao(): PagoDao
    abstract fun servicioExtraDao(): ServicioExtraDao
    abstract fun monturaDao(): MonturaDao
    abstract fun monturaMovimientoDao(): MonturaMovimientoDao
    abstract fun syncEntityStateDao(): SyncEntityStateDao

    companion object {
        @Volatile
        private var INSTANCE: OptoDatabase? = null

        // Re-exports for backward compatibility (MigrationTest.kt uses OptoDatabase.MIGRATION_X_Y)
        val MIGRATION_7_8 get() = com.example.optoapp.data.MIGRATION_7_8
        val MIGRATION_6_7 get() = com.example.optoapp.data.MIGRATION_6_7
        val MIGRATION_8_9 get() = com.example.optoapp.data.MIGRATION_8_9
        val MIGRATION_9_10 get() = com.example.optoapp.data.MIGRATION_9_10
        val MIGRATION_10_11 get() = com.example.optoapp.data.MIGRATION_10_11
        val MIGRATION_11_12 get() = com.example.optoapp.data.MIGRATION_11_12
        val MIGRATION_12_13 get() = com.example.optoapp.data.MIGRATION_12_13
        val MIGRATION_13_14 get() = com.example.optoapp.data.MIGRATION_13_14
        val MIGRATION_14_15 get() = com.example.optoapp.data.MIGRATION_14_15
        val MIGRATION_15_16 get() = com.example.optoapp.data.MIGRATION_15_16
        val MIGRATION_16_17 get() = com.example.optoapp.data.MIGRATION_16_17
        val MIGRATION_17_18 get() = com.example.optoapp.data.MIGRATION_17_18
        val MIGRATION_18_19 get() = com.example.optoapp.data.MIGRATION_18_19
        val MIGRATION_19_20 get() = com.example.optoapp.data.MIGRATION_19_20

        fun getDatabase(context: Context): OptoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OptoDatabase::class.java,
                    "opto_database"
                )
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        LocalDatabaseBackupManager.backupIfNeeded(context.applicationContext, "opto_database")
                    }
                })
                .let { builder ->
                    // Seguridad local: evitar borrado silencioso en producción.
                    if (BuildConfig.DEBUG) builder.fallbackToDestructiveMigration(true) else builder
                }
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
