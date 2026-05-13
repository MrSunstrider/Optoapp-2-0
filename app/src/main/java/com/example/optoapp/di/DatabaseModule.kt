package com.example.optoapp.di

import android.content.Context
import com.example.optoapp.data.*
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.viewmodel.auth.AuthDelegate
import com.example.optoapp.viewmodel.auth.BackupDelegate
import com.example.optoapp.viewmodel.auth.PinDelegate
import com.google.gson.Gson
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
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
    fun providePacienteRepository(
        pacienteDao: PacienteDao,
        evaluacionDao: EvaluacionDao
    ): PacienteRepository = PacienteRepository(pacienteDao, evaluacionDao)

    @Provides
    fun provideDispensacionRepository(
        dispensacionDao: DispensacionDao,
        pagoDao: PagoDao,
        servicioExtraDao: ServicioExtraDao
    ): DispensacionRepository = DispensacionRepository(dispensacionDao, pagoDao, servicioExtraDao)

    @Provides
    fun provideSyncRepository(
        syncStateTracker: SyncStateTracker,
        monturaDao: MonturaDao,
        monturaMovimientoDao: MonturaMovimientoDao
    ): SyncRepository = SyncRepository(syncStateTracker, monturaDao, monturaMovimientoDao)

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
        monturaMovimientoDao: MonturaMovimientoDao,
        syncStateTracker: SyncStateTracker,
        postSaveSyncScheduler: Lazy<PostSaveSyncScheduler>,
        pacienteRepo: PacienteRepository,
        dispensacionRepo: DispensacionRepository,
        syncRepo: SyncRepository
    ): OptoRepository {
        return OptoRepository(
            database,
            pacienteDao,
            evaluacionDao,
            dispensacionDao,
            pagoDao,
            servicioExtraDao,
            monturaDao,
            monturaMovimientoDao,
            syncStateTracker,
            postSaveSyncScheduler,
            pacienteRepo,
            dispensacionRepo,
            syncRepo
        )
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideAuthDelegate(
        securityManager: SecurityManager,
        sessionManager: SessionManager,
        repository: OptoRepository,
        membershipRepository: MembershipRepository,
        supabase: SupabaseClient,
        @ApplicationContext context: Context
    ): AuthDelegate = AuthDelegate(securityManager, sessionManager, repository, membershipRepository, supabase, context)

    @Provides
    @Singleton
    fun providePinDelegate(
        securityManager: SecurityManager,
        sessionManager: SessionManager
    ): PinDelegate = PinDelegate(securityManager, sessionManager)

    @Provides
    @Singleton
    fun provideBackupDelegate(
        repository: OptoRepository,
        sessionManager: SessionManager,
        supabase: SupabaseClient,
        gson: Gson
    ): BackupDelegate = BackupDelegate(repository, sessionManager, supabase, gson)
}
