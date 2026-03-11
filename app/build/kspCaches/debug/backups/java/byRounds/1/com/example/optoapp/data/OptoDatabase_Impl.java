package com.example.optoapp.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class OptoDatabase_Impl extends OptoDatabase {
  private volatile PacienteDao _pacienteDao;

  private volatile EvaluacionDao _evaluacionDao;

  private volatile DispensacionDao _dispensacionDao;

  private volatile PagoDao _pagoDao;

  private volatile ServicioExtraDao _servicioExtraDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `pacientes` (`id` TEXT NOT NULL, `nombreCompleto` TEXT NOT NULL, `edad` INTEGER NOT NULL, `telefono` TEXT NOT NULL, `fechaCreacion` INTEGER NOT NULL, `dni` TEXT, `fechaNacimiento` TEXT, `sexo` TEXT, `email` TEXT, `direccion` TEXT, `distrito` TEXT, `ocupacion` TEXT, `acompanante` TEXT, `hobbies` TEXT, `ultimasEtiquetas` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `evaluaciones` (`id` TEXT NOT NULL, `pacienteId` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `motivoConsulta` TEXT NOT NULL, `sintomas` TEXT NOT NULL, `antecedentesPersonalesOculares` TEXT NOT NULL, `antecedentesPersonalesSistemicos` TEXT NOT NULL, `antecedentesFamiliaresOculares` TEXT NOT NULL, `antecedentesFamiliaresSistemicos` TEXT NOT NULL, `medicacion` TEXT NOT NULL, `alergias` TEXT NOT NULL, `necesidadVisual` TEXT NOT NULL, `avScOdLejos` TEXT NOT NULL, `avScOiLejos` TEXT NOT NULL, `avScOdCerca` TEXT NOT NULL, `avScOiCerca` TEXT NOT NULL, `avScAo` TEXT NOT NULL, `avCcOdLejos` TEXT NOT NULL, `avCcOiLejos` TEXT NOT NULL, `avCcOdCerca` TEXT NOT NULL, `avCcOiCerca` TEXT NOT NULL, `avCcAoPx` TEXT NOT NULL, `phOd` TEXT NOT NULL, `phOi` TEXT NOT NULL, `kappaOd` TEXT NOT NULL, `kappaOi` TEXT NOT NULL, `hirshberg` TEXT NOT NULL, `duccionesOd` TEXT NOT NULL, `duccionesOi` TEXT NOT NULL, `versionesAo` TEXT NOT NULL, `coverTest6m` TEXT NOT NULL, `coverTest40cm` TEXT NOT NULL, `coverTest10cm` TEXT NOT NULL, `ppcOr` TEXT NOT NULL, `ppcLuz` TEXT NOT NULL, `ppcFrl` TEXT NOT NULL, `reflejoFotomotor` TEXT NOT NULL, `reflejoConsensual` TEXT NOT NULL, `reflejoAcomodativo` TEXT NOT NULL, `k1Od` TEXT NOT NULL, `k2Od` TEXT NOT NULL, `k1Oi` TEXT NOT NULL, `k2Oi` TEXT NOT NULL, `objOdEsf` TEXT NOT NULL, `objOdCil` TEXT NOT NULL, `objOdEje` TEXT NOT NULL, `objOiEsf` TEXT NOT NULL, `objOiCil` TEXT NOT NULL, `objOiEje` TEXT NOT NULL, `subjOdEsf` TEXT NOT NULL, `subjOdCil` TEXT NOT NULL, `subjOdEje` TEXT NOT NULL, `subjOiEsf` TEXT NOT NULL, `subjOiCil` TEXT NOT NULL, `subjOiEje` TEXT NOT NULL, `recetaOdEsf` TEXT NOT NULL, `recetaOdCil` TEXT NOT NULL, `recetaOdEje` TEXT NOT NULL, `recetaOdAv` TEXT NOT NULL, `recetaOiEsf` TEXT NOT NULL, `recetaOiCil` TEXT NOT NULL, `recetaOiEje` TEXT NOT NULL, `recetaOiAv` TEXT NOT NULL, `addCercaOd` TEXT NOT NULL, `addCercaOi` TEXT NOT NULL, `addIntermediaOd` TEXT NOT NULL, `addIntermediaOi` TEXT NOT NULL, `addAv` TEXT NOT NULL, `dipLejos` TEXT NOT NULL, `dipCerca` TEXT NOT NULL, `dipIntermedio` TEXT NOT NULL, `prismaOdValor` TEXT NOT NULL, `prismaOdBase` TEXT NOT NULL, `prismaOiValor` TEXT NOT NULL, `prismaOiBase` TEXT NOT NULL, `diagnostico` TEXT NOT NULL, `planTratamiento` TEXT NOT NULL, `observaciones` TEXT NOT NULL, `proximaFechaControl` TEXT NOT NULL, `proximaCita` INTEGER, `lcOdEsf` TEXT NOT NULL, `lcOdCil` TEXT NOT NULL, `lcOdEje` TEXT NOT NULL, `lcOiEsf` TEXT NOT NULL, `lcOiCil` TEXT NOT NULL, `lcOiEje` TEXT NOT NULL, `lcRadioBaseOd` TEXT NOT NULL, `lcDiametroOd` TEXT NOT NULL, `lcRadioBaseOi` TEXT NOT NULL, `lcDiametroOi` TEXT NOT NULL, `lcLaboratorio` TEXT NOT NULL, `lcTipoLente` TEXT NOT NULL, `lcMaterial` TEXT NOT NULL, `lcFechaAdaptacion` INTEGER, `lcObservaciones` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`pacienteId`) REFERENCES `pacientes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_evaluaciones_pacienteId` ON `evaluaciones` (`pacienteId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `dispensaciones` (`id` TEXT NOT NULL, `pacienteId` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `tipoMontura` TEXT NOT NULL, `materialMontura` TEXT NOT NULL, `tipoLente` TEXT NOT NULL, `materialLente` TEXT NOT NULL, `tratamientos` TEXT NOT NULL, `colorLente` TEXT NOT NULL, `notasDiseno` TEXT NOT NULL, `origenMontura` TEXT NOT NULL, `tipoAro` TEXT NOT NULL, `descripcionMontura` TEXT NOT NULL, `montoTotal` REAL NOT NULL, `metodoPago` TEXT NOT NULL, `montoPagado` REAL NOT NULL, `estadoEntrega` TEXT NOT NULL, `fechaVencimientoGarantia` TEXT, `distanciaLente` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`pacienteId`) REFERENCES `pacientes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_dispensaciones_pacienteId` ON `dispensaciones` (`pacienteId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pagos` (`id` TEXT NOT NULL, `dispensacionId` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `tipo` TEXT NOT NULL, `monto` REAL NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`dispensacionId`) REFERENCES `dispensaciones`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pagos_dispensacionId` ON `pagos` (`dispensacionId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `servicios_extra` (`id` TEXT NOT NULL, `ot` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `montoTotal` REAL NOT NULL, `aCuenta` REAL NOT NULL, `estado` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `pacienteId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`pacienteId`) REFERENCES `pacientes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_servicios_extra_pacienteId` ON `servicios_extra` (`pacienteId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '925c62abc508c4d0608866431c81b463')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `pacientes`");
        db.execSQL("DROP TABLE IF EXISTS `evaluaciones`");
        db.execSQL("DROP TABLE IF EXISTS `dispensaciones`");
        db.execSQL("DROP TABLE IF EXISTS `pagos`");
        db.execSQL("DROP TABLE IF EXISTS `servicios_extra`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPacientes = new HashMap<String, TableInfo.Column>(15);
        _columnsPacientes.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("nombreCompleto", new TableInfo.Column("nombreCompleto", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("edad", new TableInfo.Column("edad", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("telefono", new TableInfo.Column("telefono", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("fechaCreacion", new TableInfo.Column("fechaCreacion", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("dni", new TableInfo.Column("dni", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("fechaNacimiento", new TableInfo.Column("fechaNacimiento", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("sexo", new TableInfo.Column("sexo", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("email", new TableInfo.Column("email", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("direccion", new TableInfo.Column("direccion", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("distrito", new TableInfo.Column("distrito", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("ocupacion", new TableInfo.Column("ocupacion", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("acompanante", new TableInfo.Column("acompanante", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("hobbies", new TableInfo.Column("hobbies", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPacientes.put("ultimasEtiquetas", new TableInfo.Column("ultimasEtiquetas", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPacientes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPacientes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPacientes = new TableInfo("pacientes", _columnsPacientes, _foreignKeysPacientes, _indicesPacientes);
        final TableInfo _existingPacientes = TableInfo.read(db, "pacientes");
        if (!_infoPacientes.equals(_existingPacientes)) {
          return new RoomOpenHelper.ValidationResult(false, "pacientes(com.example.optoapp.data.Paciente).\n"
                  + " Expected:\n" + _infoPacientes + "\n"
                  + " Found:\n" + _existingPacientes);
        }
        final HashMap<String, TableInfo.Column> _columnsEvaluaciones = new HashMap<String, TableInfo.Column>(95);
        _columnsEvaluaciones.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("pacienteId", new TableInfo.Column("pacienteId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("fecha", new TableInfo.Column("fecha", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("motivoConsulta", new TableInfo.Column("motivoConsulta", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("sintomas", new TableInfo.Column("sintomas", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("antecedentesPersonalesOculares", new TableInfo.Column("antecedentesPersonalesOculares", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("antecedentesPersonalesSistemicos", new TableInfo.Column("antecedentesPersonalesSistemicos", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("antecedentesFamiliaresOculares", new TableInfo.Column("antecedentesFamiliaresOculares", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("antecedentesFamiliaresSistemicos", new TableInfo.Column("antecedentesFamiliaresSistemicos", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("medicacion", new TableInfo.Column("medicacion", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("alergias", new TableInfo.Column("alergias", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("necesidadVisual", new TableInfo.Column("necesidadVisual", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("avScOdLejos", new TableInfo.Column("avScOdLejos", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("avScOiLejos", new TableInfo.Column("avScOiLejos", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("avScOdCerca", new TableInfo.Column("avScOdCerca", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("avScOiCerca", new TableInfo.Column("avScOiCerca", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("avScAo", new TableInfo.Column("avScAo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("avCcOdLejos", new TableInfo.Column("avCcOdLejos", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("avCcOiLejos", new TableInfo.Column("avCcOiLejos", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("avCcOdCerca", new TableInfo.Column("avCcOdCerca", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("avCcOiCerca", new TableInfo.Column("avCcOiCerca", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("avCcAoPx", new TableInfo.Column("avCcAoPx", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("phOd", new TableInfo.Column("phOd", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("phOi", new TableInfo.Column("phOi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("kappaOd", new TableInfo.Column("kappaOd", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("kappaOi", new TableInfo.Column("kappaOi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("hirshberg", new TableInfo.Column("hirshberg", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("duccionesOd", new TableInfo.Column("duccionesOd", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("duccionesOi", new TableInfo.Column("duccionesOi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("versionesAo", new TableInfo.Column("versionesAo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("coverTest6m", new TableInfo.Column("coverTest6m", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("coverTest40cm", new TableInfo.Column("coverTest40cm", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("coverTest10cm", new TableInfo.Column("coverTest10cm", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("ppcOr", new TableInfo.Column("ppcOr", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("ppcLuz", new TableInfo.Column("ppcLuz", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("ppcFrl", new TableInfo.Column("ppcFrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("reflejoFotomotor", new TableInfo.Column("reflejoFotomotor", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("reflejoConsensual", new TableInfo.Column("reflejoConsensual", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("reflejoAcomodativo", new TableInfo.Column("reflejoAcomodativo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("k1Od", new TableInfo.Column("k1Od", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("k2Od", new TableInfo.Column("k2Od", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("k1Oi", new TableInfo.Column("k1Oi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("k2Oi", new TableInfo.Column("k2Oi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("objOdEsf", new TableInfo.Column("objOdEsf", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("objOdCil", new TableInfo.Column("objOdCil", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("objOdEje", new TableInfo.Column("objOdEje", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("objOiEsf", new TableInfo.Column("objOiEsf", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("objOiCil", new TableInfo.Column("objOiCil", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("objOiEje", new TableInfo.Column("objOiEje", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("subjOdEsf", new TableInfo.Column("subjOdEsf", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("subjOdCil", new TableInfo.Column("subjOdCil", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("subjOdEje", new TableInfo.Column("subjOdEje", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("subjOiEsf", new TableInfo.Column("subjOiEsf", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("subjOiCil", new TableInfo.Column("subjOiCil", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("subjOiEje", new TableInfo.Column("subjOiEje", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("recetaOdEsf", new TableInfo.Column("recetaOdEsf", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("recetaOdCil", new TableInfo.Column("recetaOdCil", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("recetaOdEje", new TableInfo.Column("recetaOdEje", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("recetaOdAv", new TableInfo.Column("recetaOdAv", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("recetaOiEsf", new TableInfo.Column("recetaOiEsf", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("recetaOiCil", new TableInfo.Column("recetaOiCil", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("recetaOiEje", new TableInfo.Column("recetaOiEje", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("recetaOiAv", new TableInfo.Column("recetaOiAv", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("addCercaOd", new TableInfo.Column("addCercaOd", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("addCercaOi", new TableInfo.Column("addCercaOi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("addIntermediaOd", new TableInfo.Column("addIntermediaOd", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("addIntermediaOi", new TableInfo.Column("addIntermediaOi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("addAv", new TableInfo.Column("addAv", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("dipLejos", new TableInfo.Column("dipLejos", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("dipCerca", new TableInfo.Column("dipCerca", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("dipIntermedio", new TableInfo.Column("dipIntermedio", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("prismaOdValor", new TableInfo.Column("prismaOdValor", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("prismaOdBase", new TableInfo.Column("prismaOdBase", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("prismaOiValor", new TableInfo.Column("prismaOiValor", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("prismaOiBase", new TableInfo.Column("prismaOiBase", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("diagnostico", new TableInfo.Column("diagnostico", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("planTratamiento", new TableInfo.Column("planTratamiento", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("observaciones", new TableInfo.Column("observaciones", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("proximaFechaControl", new TableInfo.Column("proximaFechaControl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("proximaCita", new TableInfo.Column("proximaCita", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcOdEsf", new TableInfo.Column("lcOdEsf", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcOdCil", new TableInfo.Column("lcOdCil", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcOdEje", new TableInfo.Column("lcOdEje", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcOiEsf", new TableInfo.Column("lcOiEsf", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcOiCil", new TableInfo.Column("lcOiCil", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcOiEje", new TableInfo.Column("lcOiEje", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcRadioBaseOd", new TableInfo.Column("lcRadioBaseOd", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcDiametroOd", new TableInfo.Column("lcDiametroOd", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcRadioBaseOi", new TableInfo.Column("lcRadioBaseOi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcDiametroOi", new TableInfo.Column("lcDiametroOi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcLaboratorio", new TableInfo.Column("lcLaboratorio", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcTipoLente", new TableInfo.Column("lcTipoLente", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcMaterial", new TableInfo.Column("lcMaterial", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcFechaAdaptacion", new TableInfo.Column("lcFechaAdaptacion", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEvaluaciones.put("lcObservaciones", new TableInfo.Column("lcObservaciones", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEvaluaciones = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysEvaluaciones.add(new TableInfo.ForeignKey("pacientes", "CASCADE", "NO ACTION", Arrays.asList("pacienteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesEvaluaciones = new HashSet<TableInfo.Index>(1);
        _indicesEvaluaciones.add(new TableInfo.Index("index_evaluaciones_pacienteId", false, Arrays.asList("pacienteId"), Arrays.asList("ASC")));
        final TableInfo _infoEvaluaciones = new TableInfo("evaluaciones", _columnsEvaluaciones, _foreignKeysEvaluaciones, _indicesEvaluaciones);
        final TableInfo _existingEvaluaciones = TableInfo.read(db, "evaluaciones");
        if (!_infoEvaluaciones.equals(_existingEvaluaciones)) {
          return new RoomOpenHelper.ValidationResult(false, "evaluaciones(com.example.optoapp.data.EvaluacionClinica).\n"
                  + " Expected:\n" + _infoEvaluaciones + "\n"
                  + " Found:\n" + _existingEvaluaciones);
        }
        final HashMap<String, TableInfo.Column> _columnsDispensaciones = new HashMap<String, TableInfo.Column>(19);
        _columnsDispensaciones.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("pacienteId", new TableInfo.Column("pacienteId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("fecha", new TableInfo.Column("fecha", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("tipoMontura", new TableInfo.Column("tipoMontura", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("materialMontura", new TableInfo.Column("materialMontura", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("tipoLente", new TableInfo.Column("tipoLente", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("materialLente", new TableInfo.Column("materialLente", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("tratamientos", new TableInfo.Column("tratamientos", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("colorLente", new TableInfo.Column("colorLente", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("notasDiseno", new TableInfo.Column("notasDiseno", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("origenMontura", new TableInfo.Column("origenMontura", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("tipoAro", new TableInfo.Column("tipoAro", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("descripcionMontura", new TableInfo.Column("descripcionMontura", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("montoTotal", new TableInfo.Column("montoTotal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("metodoPago", new TableInfo.Column("metodoPago", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("montoPagado", new TableInfo.Column("montoPagado", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("estadoEntrega", new TableInfo.Column("estadoEntrega", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("fechaVencimientoGarantia", new TableInfo.Column("fechaVencimientoGarantia", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDispensaciones.put("distanciaLente", new TableInfo.Column("distanciaLente", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDispensaciones = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysDispensaciones.add(new TableInfo.ForeignKey("pacientes", "CASCADE", "NO ACTION", Arrays.asList("pacienteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesDispensaciones = new HashSet<TableInfo.Index>(1);
        _indicesDispensaciones.add(new TableInfo.Index("index_dispensaciones_pacienteId", false, Arrays.asList("pacienteId"), Arrays.asList("ASC")));
        final TableInfo _infoDispensaciones = new TableInfo("dispensaciones", _columnsDispensaciones, _foreignKeysDispensaciones, _indicesDispensaciones);
        final TableInfo _existingDispensaciones = TableInfo.read(db, "dispensaciones");
        if (!_infoDispensaciones.equals(_existingDispensaciones)) {
          return new RoomOpenHelper.ValidationResult(false, "dispensaciones(com.example.optoapp.data.DispensacionOptica).\n"
                  + " Expected:\n" + _infoDispensaciones + "\n"
                  + " Found:\n" + _existingDispensaciones);
        }
        final HashMap<String, TableInfo.Column> _columnsPagos = new HashMap<String, TableInfo.Column>(5);
        _columnsPagos.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPagos.put("dispensacionId", new TableInfo.Column("dispensacionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPagos.put("fecha", new TableInfo.Column("fecha", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPagos.put("tipo", new TableInfo.Column("tipo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPagos.put("monto", new TableInfo.Column("monto", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPagos = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysPagos.add(new TableInfo.ForeignKey("dispensaciones", "CASCADE", "NO ACTION", Arrays.asList("dispensacionId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesPagos = new HashSet<TableInfo.Index>(1);
        _indicesPagos.add(new TableInfo.Index("index_pagos_dispensacionId", false, Arrays.asList("dispensacionId"), Arrays.asList("ASC")));
        final TableInfo _infoPagos = new TableInfo("pagos", _columnsPagos, _foreignKeysPagos, _indicesPagos);
        final TableInfo _existingPagos = TableInfo.read(db, "pagos");
        if (!_infoPagos.equals(_existingPagos)) {
          return new RoomOpenHelper.ValidationResult(false, "pagos(com.example.optoapp.data.Pago).\n"
                  + " Expected:\n" + _infoPagos + "\n"
                  + " Found:\n" + _existingPagos);
        }
        final HashMap<String, TableInfo.Column> _columnsServiciosExtra = new HashMap<String, TableInfo.Column>(8);
        _columnsServiciosExtra.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiciosExtra.put("ot", new TableInfo.Column("ot", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiciosExtra.put("descripcion", new TableInfo.Column("descripcion", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiciosExtra.put("montoTotal", new TableInfo.Column("montoTotal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiciosExtra.put("aCuenta", new TableInfo.Column("aCuenta", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiciosExtra.put("estado", new TableInfo.Column("estado", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiciosExtra.put("fecha", new TableInfo.Column("fecha", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiciosExtra.put("pacienteId", new TableInfo.Column("pacienteId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysServiciosExtra = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysServiciosExtra.add(new TableInfo.ForeignKey("pacientes", "SET NULL", "NO ACTION", Arrays.asList("pacienteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesServiciosExtra = new HashSet<TableInfo.Index>(1);
        _indicesServiciosExtra.add(new TableInfo.Index("index_servicios_extra_pacienteId", false, Arrays.asList("pacienteId"), Arrays.asList("ASC")));
        final TableInfo _infoServiciosExtra = new TableInfo("servicios_extra", _columnsServiciosExtra, _foreignKeysServiciosExtra, _indicesServiciosExtra);
        final TableInfo _existingServiciosExtra = TableInfo.read(db, "servicios_extra");
        if (!_infoServiciosExtra.equals(_existingServiciosExtra)) {
          return new RoomOpenHelper.ValidationResult(false, "servicios_extra(com.example.optoapp.data.ServicioExtra).\n"
                  + " Expected:\n" + _infoServiciosExtra + "\n"
                  + " Found:\n" + _existingServiciosExtra);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "925c62abc508c4d0608866431c81b463", "e505873338a8f2f6a952f92f3a1f1932");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "pacientes","evaluaciones","dispensaciones","pagos","servicios_extra");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `pacientes`");
      _db.execSQL("DELETE FROM `evaluaciones`");
      _db.execSQL("DELETE FROM `dispensaciones`");
      _db.execSQL("DELETE FROM `pagos`");
      _db.execSQL("DELETE FROM `servicios_extra`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PacienteDao.class, PacienteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(EvaluacionDao.class, EvaluacionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DispensacionDao.class, DispensacionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PagoDao.class, PagoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ServicioExtraDao.class, ServicioExtraDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PacienteDao pacienteDao() {
    if (_pacienteDao != null) {
      return _pacienteDao;
    } else {
      synchronized(this) {
        if(_pacienteDao == null) {
          _pacienteDao = new PacienteDao_Impl(this);
        }
        return _pacienteDao;
      }
    }
  }

  @Override
  public EvaluacionDao evaluacionDao() {
    if (_evaluacionDao != null) {
      return _evaluacionDao;
    } else {
      synchronized(this) {
        if(_evaluacionDao == null) {
          _evaluacionDao = new EvaluacionDao_Impl(this);
        }
        return _evaluacionDao;
      }
    }
  }

  @Override
  public DispensacionDao dispensacionDao() {
    if (_dispensacionDao != null) {
      return _dispensacionDao;
    } else {
      synchronized(this) {
        if(_dispensacionDao == null) {
          _dispensacionDao = new DispensacionDao_Impl(this);
        }
        return _dispensacionDao;
      }
    }
  }

  @Override
  public PagoDao pagoDao() {
    if (_pagoDao != null) {
      return _pagoDao;
    } else {
      synchronized(this) {
        if(_pagoDao == null) {
          _pagoDao = new PagoDao_Impl(this);
        }
        return _pagoDao;
      }
    }
  }

  @Override
  public ServicioExtraDao servicioExtraDao() {
    if (_servicioExtraDao != null) {
      return _servicioExtraDao;
    } else {
      synchronized(this) {
        if(_servicioExtraDao == null) {
          _servicioExtraDao = new ServicioExtraDao_Impl(this);
        }
        return _servicioExtraDao;
      }
    }
  }
}
