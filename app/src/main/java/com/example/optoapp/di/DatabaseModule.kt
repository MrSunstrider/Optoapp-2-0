package com.example.optoapp.di

import android.content.Context
import androidx.room.Room
import com.example.optoapp.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OptoDatabase {
        return Room.databaseBuilder(
            context,
            OptoDatabase::class.java,
            "opto_database"
        )
                .fallbackToDestructiveMigration(true)
                .build()
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
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context): SecurityManager {
        return SecurityManager(context)
    }

    @Provides
    @Singleton
    fun provideOptoRepository(
        pacienteDao: PacienteDao,
        evaluacionDao: EvaluacionDao,
        dispensacionDao: DispensacionDao,
        pagoDao: PagoDao,
        servicioExtraDao: ServicioExtraDao
    ): OptoRepository {
        return OptoRepository(
            pacienteDao,
            evaluacionDao,
            dispensacionDao,
            pagoDao,
            servicioExtraDao
        )
    }
}
