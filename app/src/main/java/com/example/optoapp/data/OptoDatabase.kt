package com.example.optoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Paciente::class, EvaluacionClinica::class, DispensacionOptica::class, Pago::class, ServicioExtra::class],
    version = 3,
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

        fun getDatabase(context: Context): OptoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OptoDatabase::class.java,
                    "opto_database"
                )
                .fallbackToDestructiveMigration(true) // Simplificado para desarrollo
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
