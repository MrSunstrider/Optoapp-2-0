package com.example.optoapp.di

import android.content.Context
import com.example.optoapp.data.*
import com.example.optoapp.data.arqueo.ArqueoCajaDao
import com.example.optoapp.data.arqueo.IArqueoCajaRepo
import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.inventariofisico.InventarioFisicoDao
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaDashboardKpiRepository
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.montura.MonturaMovimientoDao
import com.example.optoapp.data.ordencompra.OrdenCompraDao
import com.example.optoapp.data.ordencompra.OrdenCompraItemDao
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.proveedor.CategoriaMonturaDao
import com.example.optoapp.data.proveedor.MonturaProveedorDao
import com.example.optoapp.data.proveedor.ProveedorDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.viewmodel.auth.AuthDelegate
import com.example.optoapp.viewmodel.auth.BackupDelegate
import com.example.optoapp.viewmodel.auth.PinDelegate
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import kotlinx.serialization.json.Json
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
    fun provideDispensacionItemDao(database: OptoDatabase): DispensacionItemDao = database.dispensacionItemDao()

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
    fun provideConflictDao(database: OptoDatabase): ConflictDao = database.conflictDao()

    @Provides
    fun provideProveedorDao(database: OptoDatabase): ProveedorDao = database.proveedorDao()

    @Provides
    fun provideMonturaProveedorDao(database: OptoDatabase): MonturaProveedorDao = database.monturaProveedorDao()

    @Provides
    fun provideCategoriaMonturaDao(database: OptoDatabase): CategoriaMonturaDao = database.categoriaMonturaDao()

    @Provides
    fun provideOrdenCompraDao(database: OptoDatabase): OrdenCompraDao = database.ordenCompraDao()

    @Provides
    fun provideOrdenCompraItemDao(database: OptoDatabase): OrdenCompraItemDao = database.ordenCompraItemDao()

    @Provides
    fun provideInventarioFisicoDao(database: OptoDatabase): InventarioFisicoDao = database.inventarioFisicoDao()

    @Provides
    fun provideArqueoCajaDao(database: OptoDatabase): ArqueoCajaDao = database.arqueoCajaDao()

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
        dispensacionItemDao: DispensacionItemDao,
        pagoDao: PagoDao,
        servicioExtraDao: ServicioExtraDao
    ): DispensacionRepository = DispensacionRepository(dispensacionDao, dispensacionItemDao, pagoDao, servicioExtraDao)

    @Provides
    fun provideSyncRepository(
        syncStateTracker: SyncStateTracker,
        monturaDao: MonturaDao,
        monturaMovimientoDao: MonturaMovimientoDao
    ): SyncRepository = SyncRepository(syncStateTracker, monturaDao, monturaMovimientoDao)

    @Provides
    fun provideSyncSnapshotCoordinator(
        pacienteDao: PacienteDao,
        monturaDao: MonturaDao,
        monturaMovimientoDao: MonturaMovimientoDao,
        pacienteRepo: PacienteRepository,
        dispensacionRepo: DispensacionRepository,
        syncRepo: SyncRepository
    ): SyncSnapshotCoordinator = SyncSnapshotCoordinator(
        pacienteDao, monturaDao, monturaMovimientoDao, pacienteRepo, dispensacionRepo, syncRepo
    )

    @Provides
    fun provideBackupRestoreCoordinator(
        pacienteRepo: PacienteRepository,
        dispensacionRepo: DispensacionRepository,
        evaluacionDao: EvaluacionDao,
        pacienteDao: PacienteDao,
        postSaveSyncScheduler: Lazy<PostSaveSyncScheduler>
    ): BackupRestoreCoordinator = BackupRestoreCoordinator(
        pacienteRepo, dispensacionRepo, evaluacionDao, pacienteDao, postSaveSyncScheduler
    )

    @Provides
    fun provideMonturaInventoryCoordinator(
        monturaDao: MonturaDao,
        monturaMovimientoDao: MonturaMovimientoDao,
        postSaveSyncScheduler: Lazy<PostSaveSyncScheduler>
    ): MonturaInventoryCoordinator = MonturaInventoryCoordinator(
        monturaDao, monturaMovimientoDao, postSaveSyncScheduler
    )

    @Provides
    fun provideMonturaDashboardKpiRepository(
        monturaDao: MonturaDao,
        monturaMovimientoDao: MonturaMovimientoDao
    ): MonturaDashboardKpiRepository = MonturaDashboardKpiRepository(monturaDao, monturaMovimientoDao)

    @Provides
    @Singleton
    fun provideOptoRepository(
        database: OptoDatabase,
        syncStateTracker: SyncStateTracker,
        postSaveSyncScheduler: Lazy<PostSaveSyncScheduler>,
        pacienteRepo: PacienteRepository,
        dispensacionRepo: DispensacionRepository,
        syncRepo: SyncRepository,
        snapshotCoordinator: SyncSnapshotCoordinator,
        backupCoordinator: BackupRestoreCoordinator,
        monturaCoordinator: MonturaInventoryCoordinator,
        arqueoCajaDao: ArqueoCajaDao
    ): OptoRepository {
        return OptoRepository(
            database,
            syncStateTracker,
            postSaveSyncScheduler,
            pacienteRepo,
            dispensacionRepo,
            syncRepo,
            snapshotCoordinator,
            backupCoordinator,
            monturaCoordinator,
            arqueoCajaDao
        )
    }

    @Provides
    @Singleton
    fun provideBackupJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideAuthDelegate(
        securityManager: SecurityManager,
        sessionManager: SessionManager,
        repository: OptoRepository,
        membershipRepository: MembershipRepository,
        supabase: SupabaseClient,
        fiscalStore: OpticaFiscalSettingsStore,
        @ApplicationContext context: Context
    ): AuthDelegate = AuthDelegate(securityManager, sessionManager, repository, membershipRepository, supabase, fiscalStore, context)

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
        backupJson: Json
    ): BackupDelegate = BackupDelegate(repository, sessionManager, supabase, backupJson)

    @Provides
    fun provideIArqueoCajaRepo(repository: OptoRepository): IArqueoCajaRepo = repository
}
