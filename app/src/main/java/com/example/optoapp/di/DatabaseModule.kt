package com.example.optoapp.di

import android.content.Context
import com.example.optoapp.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// SessionManager se provee aquí porque comparte el mismo DataStore que SecurityManager

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OptoDatabase {
        return try {
            OptoDatabase.getDatabase(context)
        } catch (e: IllegalStateException) {
            throw IllegalStateException(
                "No se pudo abrir la base local por un conflicto de migración. " +
                    "Tus datos no se han borrado. Exporta respaldo y actualiza la app.",
                e
            )
        }
    }

    @Provides
    fun providePacienteDao(database: OptoDatabase): PacienteDao = database.pacienteDao()

    @Provides
    fun provideEvaluacionDao(database: OptoDatabase): EvaluacionDao = database.evaluacionDao()

    @Provides
    fun provideDispensacionDao(database: OptoDatabase): DispensacionDao = database.dispensacionDao()

    @Provides
    fun providePagoDao(database: OptoDatabase): PagoDao = database.pagoDao()

    @Provides
    fun provideServicioExtraDao(database: OptoDatabase): ServicioExtraDao = database.servicioExtraDao()

    @Provides
    fun provideMonturaDao(database: OptoDatabase): MonturaDao = database.monturaDao()

    @Provides
    fun provideMonturaMovimientoDao(database: OptoDatabase): MonturaMovimientoDao = database.monturaMovimientoDao()

    @Provides
    fun provideSyncEntityStateDao(database: OptoDatabase): SyncEntityStateDao = database.syncEntityStateDao()

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context): SecurityManager {
        return SecurityManager(context)
    }

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun provideOptoRepository(
        database: OptoDatabase,
        pacienteDao: PacienteDao,
        evaluacionDao: EvaluacionDao,
        dispensacionDao: DispensacionDao,
        pagoDao: PagoDao,
        servicioExtraDao: ServicioExtraDao,
        monturaDao: MonturaDao,
        monturaMovimientoDao: MonturaMovimientoDao
    ): OptoRepository {
        return OptoRepository(
            database,
            pacienteDao,
            evaluacionDao,
            dispensacionDao,
            pagoDao,
            servicioExtraDao,
            monturaDao,
            monturaMovimientoDao
        )
    }
}
