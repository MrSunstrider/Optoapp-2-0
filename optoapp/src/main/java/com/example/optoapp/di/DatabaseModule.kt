package com.example.optoapp.di

import android.content.Context
import com.example.optoapp.data.*
import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.categoriaproducto.CategoriaProductoDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costolc.CostoLcDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.inventariofisico.InventarioFisicoDao
import com.example.optoapp.data.opticasettings.OpticaSettingsDao
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
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionDao
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import com.example.optoapp.domain.SyncLogger
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.viewmodel.auth.AuthDelegate
import com.example.optoapp.viewmodel.auth.BackupDelegate
import com.example.optoapp.viewmodel.auth.PinDelegate
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OptoDatabase = try {
        OptoDatabase.getDatabase(context)
    } catch (e: IllegalStateException) {
        throw IllegalStateException(
            "No se pudo abrir la base local por un conflicto de migración. " +
                "Tus datos no se han borrado. Exporta respaldo y actualiza la app.",
            e,
        )
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
    fun provideGastoOperativoDao(database: OptoDatabase): GastoOperativoDao = database.gastoOperativoDao()

    @Provides
    fun provideResumenDiarioDao(database: OptoDatabase): ResumenDiarioDao = database.resumenDiarioDao()

    @Provides
    fun provideConfiguracionFinancieraDao(database: OptoDatabase): ConfiguracionFinancieraDao = database.configuracionFinancieraDao()

    @Provides
    fun provideCategoriaProductoDao(database: OptoDatabase): CategoriaProductoDao = database.categoriaProductoDao()

    @Provides
    fun provideCostoProductoDao(database: OptoDatabase): CostoProductoDao = database.costoProductoDao()

    @Provides
    fun provideCostoBiseladoDao(database: OptoDatabase): CostoBiseladoDao = database.costoBiseladoDao()

    @Provides
    @Singleton
    fun provideCostoLcDao(database: OptoDatabase): CostoLcDao = database.costoLcDao()

    @Provides
    fun provideFeedbackRecomendacionDao(database: OptoDatabase): FeedbackRecomendacionDao = database.feedbackRecomendacionDao()

    @Provides
    fun provideRegaloDispensacionDao(database: OptoDatabase): RegaloDispensacionDao = database.regaloDispensacionDao()

    @Provides
    fun provideOpticaSettingsDao(database: OptoDatabase): OpticaSettingsDao = database.opticaSettingsDao()

    @Provides
    fun provideSyncTelemetryLogDao(database: OptoDatabase): SyncTelemetryLogDao = database.syncTelemetryLogDao()

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context): SecurityManager = SecurityManager(context)

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager = SessionManager(context)

    @Provides
    fun providePacienteRepository(
        pacienteDao: PacienteDao,
        evaluacionDao: EvaluacionDao,
    ): PacienteRepository = PacienteRepository(pacienteDao, evaluacionDao)

    @Provides
    fun provideDispensacionRepository(
        dispensacionDao: DispensacionDao,
        dispensacionItemDao: DispensacionItemDao,
        pagoDao: PagoDao,
        servicioExtraDao: ServicioExtraDao,
    ): DispensacionRepository = DispensacionRepository(dispensacionDao, dispensacionItemDao, pagoDao, servicioExtraDao)

    @Provides
    fun provideSyncRepository(
        syncStateTracker: SyncStateTracker,
        monturaDao: MonturaDao,
        monturaMovimientoDao: MonturaMovimientoDao,
    ): SyncRepository = SyncRepository(syncStateTracker, monturaDao, monturaMovimientoDao)

    @Provides
    fun provideSyncSnapshotCoordinator(
        pacienteDao: PacienteDao,
        monturaDao: MonturaDao,
        monturaMovimientoDao: MonturaMovimientoDao,
        pacienteRepo: PacienteRepository,
        dispensacionRepo: DispensacionRepository,
        syncRepo: SyncRepository,
        regaloDispensacionDao: RegaloDispensacionDao,
    ): SyncSnapshotCoordinator = SyncSnapshotCoordinator(
        pacienteDao,
        monturaDao,
        monturaMovimientoDao,
        pacienteRepo,
        dispensacionRepo,
        syncRepo,
        regaloDispensacionDao,
    )

    @Provides
    fun provideBackupRestoreCoordinator(
        pacienteRepo: PacienteRepository,
        dispensacionRepo: DispensacionRepository,
        evaluacionDao: EvaluacionDao,
        pacienteDao: PacienteDao,
        postSaveSyncScheduler: Lazy<PostSaveSyncScheduler>,
        database: OptoDatabase,
    ): BackupRestoreCoordinator = BackupRestoreCoordinator(
        pacienteRepo,
        dispensacionRepo,
        evaluacionDao,
        pacienteDao,
        postSaveSyncScheduler,
        database,
    )

    @Provides
    fun provideMonturaInventoryCoordinator(
        monturaDao: MonturaDao,
        monturaMovimientoDao: MonturaMovimientoDao,
        postSaveSyncScheduler: Lazy<PostSaveSyncScheduler>,
    ): MonturaInventoryCoordinator = MonturaInventoryCoordinator(
        monturaDao,
        monturaMovimientoDao,
        postSaveSyncScheduler,
    )

    @Provides
    fun provideMonturaDashboardKpiRepository(
        monturaDao: MonturaDao,
        monturaMovimientoDao: MonturaMovimientoDao,
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
        gastoOperativoDao: GastoOperativoDao,
        supabase: SupabaseClient,
    ): OptoRepository = OptoRepository(
        database = database,
        syncStateTracker = syncStateTracker,
        postSaveSyncScheduler = postSaveSyncScheduler,
        pacienteRepo = pacienteRepo,
        dispensacionRepo = dispensacionRepo,
        syncRepo = syncRepo,
        snapshotCoordinator = snapshotCoordinator,
        backupCoordinator = backupCoordinator,
        monturaCoordinator = monturaCoordinator,
        gastoOperativoDao = gastoOperativoDao,
        supabase = supabase,
    )

    @Provides
    @Singleton
    fun provideSyncLogger(): SyncLogger = AndroidSyncLogger()

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
        @ApplicationContext context: Context,
    ): AuthDelegate = AuthDelegate(securityManager, sessionManager, repository, membershipRepository, supabase, fiscalStore, context)

    @Provides
    @Singleton
    fun providePinDelegate(
        securityManager: SecurityManager,
        sessionManager: SessionManager,
    ): PinDelegate = PinDelegate(securityManager, sessionManager)

    @Provides
    @Singleton
    fun provideBackupDelegate(
        repository: OptoRepository,
        sessionManager: SessionManager,
        supabase: SupabaseClient,
        backupJson: Json,
    ): BackupDelegate = BackupDelegate(repository, sessionManager, supabase, backupJson)

    @Provides
    @Singleton
    fun provideDispensacionFinancieraRepository(
        repository: OptoRepository,
    ): DispensacionFinancieraRepository = DispensacionFinancieraRepositoryImpl(repository)
}
