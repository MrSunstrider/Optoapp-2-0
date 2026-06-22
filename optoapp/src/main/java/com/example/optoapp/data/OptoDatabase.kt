package com.example.optoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.optoapp.data.arqueo.ArqueoCaja
import com.example.optoapp.data.arqueo.ArqueoCajaDao
import com.example.optoapp.data.inventariofisico.InventarioFisicoDao
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao
import com.example.optoapp.data.ordencompra.OrdenCompraDao
import com.example.optoapp.data.ordencompra.OrdenCompraItemDao
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.proveedor.CategoriaMonturaDao
import com.example.optoapp.data.proveedor.MonturaProveedorDao
import com.example.optoapp.data.proveedor.ProveedorDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.util.LocalDatabaseBackupManager

@Database(
    entities = [
        Paciente::class, EvaluacionClinica::class, DispensacionOptica::class, Pago::class, ServicioExtra::class,
        Montura::class, MonturaMovimiento::class, SyncEntityState::class, DispensacionItem::class,
        ConflictRecord::class,
        Proveedor::class, MonturaProveedor::class, CategoriaMontura::class,
        OrdenCompra::class, OrdenCompraItem::class,
        InventarioFisico::class, InventarioFisicoDetalle::class,
        ArqueoCaja::class
    ],
    version = 28,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OptoDatabase : RoomDatabase() {
    abstract fun pacienteDao(): PacienteDao
    abstract fun evaluacionDao(): EvaluacionDao
    abstract fun dispensacionDao(): DispensacionDao
    abstract fun dispensacionItemDao(): DispensacionItemDao
    abstract fun pagoDao(): PagoDao
    abstract fun servicioExtraDao(): ServicioExtraDao
    abstract fun monturaDao(): MonturaDao
    abstract fun monturaMovimientoDao(): MonturaMovimientoDao
    abstract fun syncEntityStateDao(): SyncEntityStateDao
    abstract fun conflictDao(): ConflictDao
    abstract fun proveedorDao(): ProveedorDao
    abstract fun monturaProveedorDao(): MonturaProveedorDao
    abstract fun categoriaMonturaDao(): CategoriaMonturaDao
    abstract fun ordenCompraDao(): OrdenCompraDao
    abstract fun ordenCompraItemDao(): OrdenCompraItemDao
    abstract fun inventarioFisicoDao(): InventarioFisicoDao
    abstract fun arqueoCajaDao(): ArqueoCajaDao

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
        val MIGRATION_20_21 get() = com.example.optoapp.data.MIGRATION_20_21
        val MIGRATION_21_22 get() = com.example.optoapp.data.MIGRATION_21_22
        val MIGRATION_22_23 get() = com.example.optoapp.data.MIGRATION_22_23
        val MIGRATION_23_24 get() = com.example.optoapp.data.MIGRATION_23_24
        val MIGRATION_24_25 get() = com.example.optoapp.data.MIGRATION_24_25
        val MIGRATION_25_26 get() = com.example.optoapp.data.MIGRATION_25_26
        val MIGRATION_26_27 get() = com.example.optoapp.data.MIGRATION_26_27
        val MIGRATION_27_28 get() = com.example.optoapp.data.MIGRATION_27_28

        fun getDatabase(context: Context): OptoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OptoDatabase::class.java,
                    "opto_database"
                )
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        LocalDatabaseBackupManager.backupIfNeeded(context.applicationContext, "opto_database")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
