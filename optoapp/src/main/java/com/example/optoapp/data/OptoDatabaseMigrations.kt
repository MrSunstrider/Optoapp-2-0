package com.example.optoapp.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.TimeUnit

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pacientes ADD COLUMN opticaId TEXT NOT NULL DEFAULT 'mi_optica_base'")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN opticaId TEXT NOT NULL DEFAULT 'mi_optica_base'")
        db.execSQL("ALTER TABLE dispensaciones ADD COLUMN opticaId TEXT NOT NULL DEFAULT 'mi_optica_base'")
        db.execSQL("ALTER TABLE servicios_extra ADD COLUMN opticaId TEXT NOT NULL DEFAULT 'mi_optica_base'")
        db.execSQL("ALTER TABLE pagos ADD COLUMN opticaId TEXT NOT NULL DEFAULT 'mi_optica_base'")
    }
}
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN avScAoCerca TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN avCcAoCerca TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN estereopsisValor TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN estereopsisSegundos TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN lang TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN worth TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN ishihara TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN farnsworth TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN schirmerOd TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN schirmerOi TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN osdiPuntuacion INTEGER")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN osdiClasificacion TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN sensibilidadContraste TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN sensibilidadFrecuencia TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN amsler TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN campoVisual TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN campoVisualDescripcion TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Gap intencional: bump de versión sin cambios de schema.
        // Ver commit 08685cd para contexto.
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val dayMillis = TimeUnit.DAYS.toMillis(1)
        val utcMidnightThresholdMillis = TimeUnit.HOURS.toMillis(1) // tolerancia

        fun millisToIsoExpr(column: String): String {
            // SQLite date() modifiers: unixepoch, localtime, +N day
            // Si el epoch parece UTC midnight (mod cercano a 0), sumamos 1 día.
            return """
                        date(
                          ($column / 1000),
                          'unixepoch',
                          'localtime',
                          CASE WHEN (abs(($column % $dayMillis)) < $utcMidnightThresholdMillis) THEN '+1 day' ELSE '+0 day' END
                        )
                    """.trimIndent()
        }

        db.execSQL("ALTER TABLE pacientes RENAME TO pacientes_old")
        db.execSQL(
            """
                    CREATE TABLE IF NOT EXISTS pacientes (
                      id TEXT NOT NULL PRIMARY KEY,
                      nombreCompleto TEXT NOT NULL,
                      edad INTEGER NOT NULL,
                      telefono TEXT NOT NULL,
                      fechaCreacion TEXT NOT NULL,
                      dni TEXT,
                      fechaNacimiento TEXT,
                      sexo TEXT,
                      email TEXT,
                      direccion TEXT,
                      distrito TEXT,
                      ocupacion TEXT,
                      acompanante TEXT,
                      hobbies TEXT,
                      ultimasEtiquetas TEXT NOT NULL,
                      opticaId TEXT NOT NULL
                    )
                    """.trimIndent()
        )
        db.execSQL(
            """
                    INSERT INTO pacientes (
                      id, nombreCompleto, edad, telefono, fechaCreacion, dni, fechaNacimiento, sexo, email, direccion,
                      distrito, ocupacion, acompanante, hobbies, ultimasEtiquetas, opticaId
                    )
                    SELECT
                      id, nombreCompleto, edad, telefono,
                      ${millisToIsoExpr("fechaCreacion")} AS fechaCreacion,
                      dni, fechaNacimiento, sexo, email, direccion,
                      distrito, ocupacion, acompanante, hobbies, ultimasEtiquetas, opticaId
                    FROM pacientes_old
                    """.trimIndent()
        )
        db.execSQL("DROP TABLE pacientes_old")

        db.execSQL("ALTER TABLE evaluaciones RENAME TO evaluaciones_old")
        db.execSQL(
            """
                    CREATE TABLE IF NOT EXISTS evaluaciones (
                      id TEXT NOT NULL PRIMARY KEY,
                      pacienteId TEXT NOT NULL,
                      fecha TEXT NOT NULL,
                      opticaId TEXT NOT NULL,
                      motivoConsulta TEXT NOT NULL,
                      sintomas TEXT NOT NULL,
                      antecedentesPersonalesOculares TEXT NOT NULL,
                      antecedentesPersonalesSistemicos TEXT NOT NULL,
                      antecedentesFamiliaresOculares TEXT NOT NULL,
                      antecedentesFamiliaresSistemicos TEXT NOT NULL,
                      medicacion TEXT NOT NULL,
                      alergias TEXT NOT NULL,
                      necesidadVisual TEXT NOT NULL,
                      avScOdLejos TEXT NOT NULL,
                      avScOiLejos TEXT NOT NULL,
                      avScOdCerca TEXT NOT NULL,
                      avScOiCerca TEXT NOT NULL,
                      avScAo TEXT NOT NULL,
                      avScAoCerca TEXT NOT NULL,
                      avCcOdLejos TEXT NOT NULL,
                      avCcOiLejos TEXT NOT NULL,
                      avCcOdCerca TEXT NOT NULL,
                      avCcOiCerca TEXT NOT NULL,
                      avCcAoPx TEXT NOT NULL,
                      avCcAoCerca TEXT NOT NULL,
                      phOd TEXT NOT NULL,
                      phOi TEXT NOT NULL,
                      kappaOd TEXT NOT NULL,
                      kappaOi TEXT NOT NULL,
                      hirshberg TEXT NOT NULL,
                      duccionesOd TEXT NOT NULL,
                      duccionesOi TEXT NOT NULL,
                      versionesAo TEXT NOT NULL,
                      estereopsisValor TEXT NOT NULL,
                      estereopsisSegundos TEXT NOT NULL,
                      lang TEXT NOT NULL,
                      worth TEXT NOT NULL,
                      ishihara TEXT NOT NULL,
                      farnsworth TEXT NOT NULL,
                      schirmerOd TEXT NOT NULL,
                      schirmerOi TEXT NOT NULL,
                      osdiPuntuacion INTEGER,
                      osdiClasificacion TEXT NOT NULL,
                      sensibilidadContraste TEXT NOT NULL,
                      sensibilidadFrecuencia TEXT NOT NULL,
                      amsler TEXT NOT NULL,
                      campoVisual TEXT NOT NULL,
                      campoVisualDescripcion TEXT NOT NULL,
                      coverTest6m TEXT NOT NULL,
                      coverTest40cm TEXT NOT NULL,
                      coverTest10cm TEXT NOT NULL,
                      ppcOr TEXT NOT NULL,
                      ppcLuz TEXT NOT NULL,
                      ppcFrl TEXT NOT NULL,
                      reflejoFotomotor TEXT NOT NULL,
                      reflejoConsensual TEXT NOT NULL,
                      reflejoAcomodativo TEXT NOT NULL,
                      k1Od TEXT NOT NULL,
                      k2Od TEXT NOT NULL,
                      k1Oi TEXT NOT NULL,
                      k2Oi TEXT NOT NULL,
                      objOdEsf TEXT NOT NULL,
                      objOdCil TEXT NOT NULL,
                      objOdEje TEXT NOT NULL,
                      objOiEsf TEXT NOT NULL,
                      objOiCil TEXT NOT NULL,
                      objOiEje TEXT NOT NULL,
                      subjOdEsf TEXT NOT NULL,
                      subjOdCil TEXT NOT NULL,
                      subjOdEje TEXT NOT NULL,
                      subjOiEsf TEXT NOT NULL,
                      subjOiCil TEXT NOT NULL,
                      subjOiEje TEXT NOT NULL,
                      recetaOdEsf TEXT NOT NULL,
                      recetaOdCil TEXT NOT NULL,
                      recetaOdEje TEXT NOT NULL,
                      recetaOdAv TEXT NOT NULL,
                      recetaOiEsf TEXT NOT NULL,
                      recetaOiCil TEXT NOT NULL,
                      recetaOiEje TEXT NOT NULL,
                      recetaOiAv TEXT NOT NULL,
                      addCercaOd TEXT NOT NULL,
                      addCercaOi TEXT NOT NULL,
                      addIntermediaOd TEXT NOT NULL,
                      addIntermediaOi TEXT NOT NULL,
                      addAv TEXT NOT NULL,
                      dipLejos TEXT NOT NULL,
                      dipCerca TEXT NOT NULL,
                      dipIntermedio TEXT NOT NULL,
                      prismaOdValor TEXT NOT NULL,
                      prismaOdBase TEXT NOT NULL,
                      prismaOiValor TEXT NOT NULL,
                      prismaOiBase TEXT NOT NULL,
                      diagnostico TEXT NOT NULL,
                      diagnosticoOd TEXT NOT NULL,
                      diagnosticoOi TEXT NOT NULL,
                      diagnosticoOtros TEXT NOT NULL,
                      planTratamiento TEXT NOT NULL,
                      observaciones TEXT NOT NULL,
                      proximaFechaControl TEXT NOT NULL,
                      proximaCita TEXT,
                      balanceOd INTEGER NOT NULL,
                      balanceOi INTEGER NOT NULL,
                      otrosPresbicia INTEGER NOT NULL,
                      otrosAnisometropia INTEGER NOT NULL,
                      otrosAmbliopia INTEGER NOT NULL,
                      autoPresbicia INTEGER NOT NULL,
                      autoAnisometropia INTEGER NOT NULL,
                      autoAmbliopia INTEGER NOT NULL,
                      lcOdEsf TEXT NOT NULL,
                      lcOdCil TEXT NOT NULL,
                      lcOdEje TEXT NOT NULL,
                      lcOiEsf TEXT NOT NULL,
                      lcOiCil TEXT NOT NULL,
                      lcOiEje TEXT NOT NULL,
                      lcRadioBaseOd TEXT NOT NULL,
                      lcDiametroOd TEXT NOT NULL,
                      lcRadioBaseOi TEXT NOT NULL,
                      lcDiametroOi TEXT NOT NULL,
                      lcLaboratorio TEXT NOT NULL,
                      lcTipoLente TEXT NOT NULL,
                      lcMaterial TEXT NOT NULL,
                      lcFechaAdaptacion TEXT,
                      lcObservaciones TEXT NOT NULL,
                      FOREIGN KEY(pacienteId) REFERENCES pacientes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
        )
        db.execSQL(
            """
                    INSERT INTO evaluaciones (
                      id, pacienteId, fecha, opticaId, motivoConsulta, sintomas, antecedentesPersonalesOculares,
                      antecedentesPersonalesSistemicos, antecedentesFamiliaresOculares, antecedentesFamiliaresSistemicos,
                      medicacion, alergias, necesidadVisual,
                      avScOdLejos, avScOiLejos, avScOdCerca, avScOiCerca, avScAo, avScAoCerca,
                      avCcOdLejos, avCcOiLejos, avCcOdCerca, avCcOiCerca, avCcAoPx, avCcAoCerca,
                      phOd, phOi, kappaOd, kappaOi, hirshberg, duccionesOd, duccionesOi, versionesAo,
                      estereopsisValor, estereopsisSegundos, lang, worth,
                      ishihara, farnsworth,
                      schirmerOd, schirmerOi, osdiPuntuacion, osdiClasificacion, sensibilidadContraste, sensibilidadFrecuencia,
                      amsler, campoVisual, campoVisualDescripcion,
                      coverTest6m, coverTest40cm, coverTest10cm,
                      ppcOr, ppcLuz, ppcFrl,
                      reflejoFotomotor, reflejoConsensual, reflejoAcomodativo,
                      k1Od, k2Od, k1Oi, k2Oi,
                      objOdEsf, objOdCil, objOdEje, objOiEsf, objOiCil, objOiEje,
                      subjOdEsf, subjOdCil, subjOdEje, subjOiEsf, subjOiCil, subjOiEje,
                      recetaOdEsf, recetaOdCil, recetaOdEje, recetaOdAv,
                      recetaOiEsf, recetaOiCil, recetaOiEje, recetaOiAv,
                      addCercaOd, addCercaOi, addIntermediaOd, addIntermediaOi, addAv,
                      dipLejos, dipCerca, dipIntermedio,
                      prismaOdValor, prismaOdBase, prismaOiValor, prismaOiBase,
                      diagnostico, diagnosticoOd, diagnosticoOi, diagnosticoOtros,
                      planTratamiento, observaciones, proximaFechaControl, proximaCita,
                      balanceOd, balanceOi,
                      otrosPresbicia, otrosAnisometropia, otrosAmbliopia,
                      autoPresbicia, autoAnisometropia, autoAmbliopia,
                      lcOdEsf, lcOdCil, lcOdEje, lcOiEsf, lcOiCil, lcOiEje,
                      lcRadioBaseOd, lcDiametroOd, lcRadioBaseOi, lcDiametroOi,
                      lcLaboratorio, lcTipoLente, lcMaterial, lcFechaAdaptacion, lcObservaciones
                    )
                    SELECT
                      id, pacienteId,
                      ${millisToIsoExpr("fecha")} AS fecha,
                      opticaId, motivoConsulta, sintomas, antecedentesPersonalesOculares,
                      antecedentesPersonalesSistemicos, antecedentesFamiliaresOculares, antecedentesFamiliaresSistemicos,
                      medicacion, alergias, necesidadVisual,
                      avScOdLejos, avScOiLejos, avScOdCerca, avScOiCerca, avScAo, avScAoCerca,
                      avCcOdLejos, avCcOiLejos, avCcOdCerca, avCcOiCerca, avCcAoPx, avCcAoCerca,
                      phOd, phOi, kappaOd, kappaOi, hirshberg, duccionesOd, duccionesOi, versionesAo,
                      estereopsisValor, estereopsisSegundos, lang, worth,
                      ishihara, farnsworth,
                      schirmerOd, schirmerOi, osdiPuntuacion, osdiClasificacion, sensibilidadContraste, sensibilidadFrecuencia,
                      amsler, campoVisual, campoVisualDescripcion,
                      coverTest6m, coverTest40cm, coverTest10cm,
                      ppcOr, ppcLuz, ppcFrl,
                      reflejoFotomotor, reflejoConsensual, reflejoAcomodativo,
                      k1Od, k2Od, k1Oi, k2Oi,
                      objOdEsf, objOdCil, objOdEje, objOiEsf, objOiCil, objOiEje,
                      subjOdEsf, subjOdCil, subjOdEje, subjOiEsf, subjOiCil, subjOiEje,
                      recetaOdEsf, recetaOdCil, recetaOdEje, recetaOdAv,
                      recetaOiEsf, recetaOiCil, recetaOiEje, recetaOiAv,
                      addCercaOd, addCercaOi, addIntermediaOd, addIntermediaOi, addAv,
                      dipLejos, dipCerca, dipIntermedio,
                      prismaOdValor, prismaOdBase, prismaOiValor, prismaOiBase,
                      diagnostico, diagnosticoOd, diagnosticoOi, diagnosticoOtros,
                      planTratamiento, observaciones, proximaFechaControl,
                      CASE WHEN proximaCita IS NULL THEN NULL ELSE ${millisToIsoExpr("proximaCita")} END AS proximaCita,
                      balanceOd, balanceOi,
                      otrosPresbicia, otrosAnisometropia, otrosAmbliopia,
                      autoPresbicia, autoAnisometropia, autoAmbliopia,
                      lcOdEsf, lcOdCil, lcOdEje, lcOiEsf, lcOiCil, lcOiEje,
                      lcRadioBaseOd, lcDiametroOd, lcRadioBaseOi, lcDiametroOi,
                      lcLaboratorio, lcTipoLente, lcMaterial,
                      CASE WHEN lcFechaAdaptacion IS NULL THEN NULL ELSE ${millisToIsoExpr("lcFechaAdaptacion")} END AS lcFechaAdaptacion,
                      lcObservaciones
                    FROM evaluaciones_old
                    """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evaluaciones_pacienteId ON evaluaciones(pacienteId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evaluaciones_opticaId ON evaluaciones(opticaId)")
        db.execSQL("DROP TABLE evaluaciones_old")

        db.execSQL("ALTER TABLE dispensaciones RENAME TO dispensaciones_old")
        db.execSQL(
            """
                    CREATE TABLE IF NOT EXISTS dispensaciones (
                      id TEXT NOT NULL PRIMARY KEY,
                      pacienteId TEXT NOT NULL,
                      fecha TEXT NOT NULL,
                      opticaId TEXT NOT NULL,
                      tipoMontura TEXT NOT NULL,
                      materialMontura TEXT NOT NULL,
                      tipoLente TEXT NOT NULL,
                      materialLente TEXT NOT NULL,
                      tratamientos TEXT NOT NULL,
                      colorLente TEXT NOT NULL,
                      notasDiseno TEXT NOT NULL,
                      origenMontura TEXT NOT NULL,
                      tipoAro TEXT NOT NULL,
                      descripcionMontura TEXT NOT NULL,
                      montoTotal REAL NOT NULL,
                      metodoPago TEXT NOT NULL,
                      montoPagado REAL NOT NULL,
                      estadoEntrega TEXT NOT NULL,
                      fechaVencimientoGarantia TEXT,
                      distanciaLente TEXT NOT NULL,
                      subTipoBifocal TEXT NOT NULL,
                      FOREIGN KEY(pacienteId) REFERENCES pacientes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
        )
        db.execSQL(
            """
                    INSERT INTO dispensaciones (
                      id, pacienteId, fecha, opticaId, tipoMontura, materialMontura, tipoLente, materialLente,
                      tratamientos, colorLente, notasDiseno, origenMontura, tipoAro, descripcionMontura,
                      montoTotal, metodoPago, montoPagado, estadoEntrega, fechaVencimientoGarantia, distanciaLente, subTipoBifocal
                    )
                    SELECT
                      id, pacienteId,
                      ${millisToIsoExpr("fecha")} AS fecha,
                      opticaId, tipoMontura, materialMontura, tipoLente, materialLente,
                      tratamientos, colorLente, notasDiseno, origenMontura, tipoAro, descripcionMontura,
                      montoTotal, metodoPago, montoPagado, estadoEntrega,
                      fechaVencimientoGarantia,
                      distanciaLente, subTipoBifocal
                    FROM dispensaciones_old
                    """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_dispensaciones_pacienteId ON dispensaciones(pacienteId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_dispensaciones_opticaId ON dispensaciones(opticaId)")
        db.execSQL("DROP TABLE dispensaciones_old")

        db.execSQL("ALTER TABLE servicios_extra RENAME TO servicios_extra_old")
        db.execSQL(
            """
                    CREATE TABLE IF NOT EXISTS servicios_extra (
                      id TEXT NOT NULL PRIMARY KEY,
                      ot TEXT NOT NULL,
                      descripcion TEXT NOT NULL,
                      montoTotal REAL NOT NULL,
                      aCuenta REAL NOT NULL,
                      estado TEXT NOT NULL,
                      fecha TEXT NOT NULL,
                      pacienteId TEXT,
                      metodoPago TEXT NOT NULL,
                      opticaId TEXT NOT NULL,
                      FOREIGN KEY(pacienteId) REFERENCES pacientes(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
        )
        db.execSQL(
            """
                    INSERT INTO servicios_extra (
                      id, ot, descripcion, montoTotal, aCuenta, estado, fecha, pacienteId, metodoPago, opticaId
                    )
                    SELECT
                      id, ot, descripcion, montoTotal, aCuenta, estado,
                      ${millisToIsoExpr("fecha")} AS fecha,
                      pacienteId, metodoPago, opticaId
                    FROM servicios_extra_old
                    """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_servicios_extra_pacienteId ON servicios_extra(pacienteId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_servicios_extra_opticaId ON servicios_extra(opticaId)")
        db.execSQL("DROP TABLE servicios_extra_old")

        db.execSQL("ALTER TABLE pagos RENAME TO pagos_old")
        db.execSQL(
            """
                    CREATE TABLE IF NOT EXISTS pagos (
                      id TEXT NOT NULL PRIMARY KEY,
                      dispensacionId TEXT,
                      servicioExtraId TEXT,
                      fecha TEXT NOT NULL,
                      tipo TEXT NOT NULL,
                      monto REAL NOT NULL,
                      metodoPago TEXT NOT NULL,
                      nota TEXT NOT NULL,
                      opticaId TEXT NOT NULL,
                      FOREIGN KEY(dispensacionId) REFERENCES dispensaciones(id) ON DELETE CASCADE,
                      FOREIGN KEY(servicioExtraId) REFERENCES servicios_extra(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
        )
        db.execSQL(
            """
                    INSERT INTO pagos (
                      id, dispensacionId, servicioExtraId, fecha, tipo, monto, metodoPago, nota, opticaId
                    )
                    SELECT
                      id, dispensacionId, servicioExtraId,
                      ${millisToIsoExpr("fecha")} AS fecha,
                      tipo, monto, metodoPago, nota, opticaId
                    FROM pagos_old
                    """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pagos_dispensacionId ON pagos(dispensacionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pagos_servicioExtraId ON pagos(servicioExtraId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pagos_opticaId ON pagos(opticaId)")
        db.execSQL("DROP TABLE pagos_old")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN dipTotalMm REAL")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN dnpOdMm REAL")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN dnpOiMm REAL")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dispensaciones ADD COLUMN altura TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dispensaciones ADD COLUMN ot TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                    CREATE TABLE IF NOT EXISTS monturas (
                        id TEXT NOT NULL PRIMARY KEY,
                        sku TEXT NOT NULL,
                        marca TEXT NOT NULL,
                        modelo TEXT NOT NULL,
                        color TEXT NOT NULL,
                        talla TEXT NOT NULL,
                        costo REAL NOT NULL,
                        precio REAL NOT NULL,
                        stockActual INTEGER NOT NULL,
                        stockMinimo INTEGER NOT NULL,
                        activo INTEGER NOT NULL,
                        opticaId TEXT NOT NULL
                    )
                    """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_monturas_sku_opticaId ON monturas(sku, opticaId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_monturas_opticaId ON monturas(opticaId)")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dispensaciones ADD COLUMN monturaId TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            """
                    CREATE TABLE IF NOT EXISTS montura_movimientos (
                        id TEXT NOT NULL PRIMARY KEY,
                        monturaId TEXT NOT NULL,
                        fecha TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        cantidad INTEGER NOT NULL,
                        stockPrevio INTEGER NOT NULL,
                        stockNuevo INTEGER NOT NULL,
                        referenciaId TEXT NOT NULL,
                        nota TEXT NOT NULL,
                        opticaId TEXT NOT NULL,
                        FOREIGN KEY(monturaId) REFERENCES monturas(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_montura_movimientos_monturaId ON montura_movimientos(monturaId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_montura_movimientos_opticaId ON montura_movimientos(opticaId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_montura_movimientos_fecha ON montura_movimientos(fecha)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_montura_movimientos_referenciaId ON montura_movimientos(referenciaId)")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                    UPDATE dispensaciones
                    SET ot = ot || '-d' || rowid
                    WHERE length(trim(ot)) > 0
                    AND rowid NOT IN (
                        SELECT MIN(rowid) FROM dispensaciones
                        WHERE length(trim(ot)) > 0
                        GROUP BY opticaId, UPPER(TRIM(ot))
                    )
                    """.trimIndent()
        )
        db.execSQL(
            """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_dispensaciones_optica_ot_unique
                    ON dispensaciones(opticaId, UPPER(TRIM(ot)))
                    WHERE length(trim(ot)) > 0
                    """.trimIndent()
        )
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE evaluaciones ADD COLUMN citaEstado TEXT NOT NULL DEFAULT 'programada'"
        )
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                    CREATE TABLE IF NOT EXISTS sync_entity_state (
                        opticaId TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        lastError TEXT NOT NULL DEFAULT '',
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY (opticaId, entityType, entityId)
                    )
                    """.trimIndent()
        )
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pacientes ADD COLUMN historiaOptometrica TEXT")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE monturas ADD COLUMN tipoAro TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE monturas ADD COLUMN materialMontura TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Clean up failed attempts from pre-MIGRATION_20_21 (wrong index names, missing DEFAULTs)
        // Column names use snake_case to match @ColumnInfo(name = "...") in DispensacionItemEntity
        db.execSQL("DROP INDEX IF EXISTS idx_dispensacion_items_dispensacion_id")
        db.execSQL("DROP INDEX IF EXISTS idx_dispensacion_items_optica_id")
        db.execSQL("DROP TABLE IF EXISTS dispensacion_items")

        // snake_case columns son intencionales: DispensacionItem usa @ColumnInfo(name = "snake_case")
        // para que Room y Supabase compartan el mismo schema sin mapeo adicional.
        db.execSQL("""
            CREATE TABLE dispensacion_items (
                id TEXT NOT NULL PRIMARY KEY,
                dispensacion_id TEXT NOT NULL,
                tipo_lente TEXT NOT NULL,
                material_lente TEXT NOT NULL,
                tratamientos TEXT NOT NULL,
                color_lente TEXT NOT NULL,
                distancia_lente TEXT NOT NULL,
                altura TEXT NOT NULL,
                sub_tipo_bifocal TEXT NOT NULL,
                notas_diseno TEXT NOT NULL,
                montura_id TEXT NOT NULL,
                origen_montura TEXT NOT NULL,
                tipo_aro TEXT NOT NULL,
                material_montura TEXT NOT NULL,
                descripcion_montura TEXT NOT NULL,
                tipo_montura TEXT NOT NULL,
                optica_id TEXT NOT NULL,
                FOREIGN KEY (dispensacion_id) REFERENCES dispensaciones(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_dispensacion_items_dispensacion_id ON dispensacion_items(dispensacion_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_dispensacion_items_optica_id ON dispensacion_items(optica_id)")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE monturas ADD COLUMN anchoMm REAL")
        db.execSQL("ALTER TABLE monturas ADD COLUMN puenteMm REAL")
        db.execSQL("ALTER TABLE monturas ADD COLUMN alturaMm REAL")
        db.execSQL("ALTER TABLE monturas ADD COLUMN imagenUri TEXT")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Añadir updatedAt/updatedBy para detección de conflictos de sync
        db.execSQL("ALTER TABLE pacientes ADD COLUMN updatedAt TEXT")
        db.execSQL("ALTER TABLE pacientes ADD COLUMN updatedBy TEXT")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN updatedAt TEXT")
        db.execSQL("ALTER TABLE evaluaciones ADD COLUMN updatedBy TEXT")
        db.execSQL("ALTER TABLE dispensaciones ADD COLUMN updatedAt TEXT")
        db.execSQL("ALTER TABLE dispensaciones ADD COLUMN updatedBy TEXT")
        db.execSQL("ALTER TABLE pagos ADD COLUMN updatedAt TEXT")
        db.execSQL("ALTER TABLE pagos ADD COLUMN updatedBy TEXT")
        db.execSQL("ALTER TABLE servicios_extra ADD COLUMN updatedAt TEXT")
        db.execSQL("ALTER TABLE servicios_extra ADD COLUMN updatedBy TEXT")
        db.execSQL("ALTER TABLE monturas ADD COLUMN updatedAt TEXT")
        db.execSQL("ALTER TABLE monturas ADD COLUMN updatedBy TEXT")
        db.execSQL("ALTER TABLE montura_movimientos ADD COLUMN updatedAt TEXT")
        db.execSQL("ALTER TABLE montura_movimientos ADD COLUMN updatedBy TEXT")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS conflict_records (
                entityId TEXT NOT NULL PRIMARY KEY,
                opticaId TEXT NOT NULL,
                entityType TEXT NOT NULL,
                localSnapshot TEXT NOT NULL,
                remoteSnapshot TEXT NOT NULL,
                detectedAt INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dispensaciones ADD COLUMN fechaEntrega TEXT")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Catalog/commercial fields for categorised frame search and filtering
        db.execSQL("ALTER TABLE monturas ADD COLUMN categoria TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE monturas ADD COLUMN coleccion TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE monturas ADD COLUMN temporada TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE monturas ADD COLUMN estadoComercial TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE monturas ADD COLUMN genero TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_monturas_estadoComercial ON monturas(estadoComercial)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_monturas_categoria ON monturas(categoria)")

        // Audit trail and composite-key conflict detection (referenciaId+tipo+monturaId)
        db.execSQL("ALTER TABLE montura_movimientos ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE montura_movimientos ADD COLUMN costoUnitario REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE montura_movimientos ADD COLUMN tipoDocumento TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_movimientos_conflict ON montura_movimientos(referenciaId, tipo, monturaId)")

        // Supplier catalog with unique RUC per optica (shared across multiple frames)
        db.execSQL("CREATE TABLE IF NOT EXISTS proveedores (id TEXT NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, ruc TEXT NOT NULL, telefono TEXT NOT NULL DEFAULT '', email TEXT NOT NULL DEFAULT '', direccion TEXT NOT NULL DEFAULT '', contacto TEXT NOT NULL DEFAULT '', activo INTEGER NOT NULL DEFAULT 1, opticaId TEXT NOT NULL, updatedAt TEXT, updatedBy TEXT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_proveedores_opticaId ON proveedores(opticaId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_proveedores_ruc_opticaId ON proveedores(ruc, opticaId)")

        // Many-to-many link: each frame can be sourced from multiple suppliers
        db.execSQL("CREATE TABLE IF NOT EXISTS montura_proveedor (id TEXT NOT NULL PRIMARY KEY, monturaId TEXT NOT NULL, proveedorId TEXT NOT NULL, costoProveedor REAL NOT NULL DEFAULT 0.0, precioSugerido REAL NOT NULL DEFAULT 0.0, activo INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(monturaId) REFERENCES monturas(id) ON DELETE CASCADE, FOREIGN KEY(proveedorId) REFERENCES proveedores(id) ON DELETE CASCADE)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_mp_montura_proveedor ON montura_proveedor(monturaId, proveedorId)")

        // User-defined frame categories (e.g. SOL, RECETADOS, DEPORTIVOS)
        db.execSQL("CREATE TABLE IF NOT EXISTS categorias_montura (id TEXT NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, descripcion TEXT NOT NULL DEFAULT '', opticaId TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cat_nombre_opticaId ON categorias_montura(nombre, opticaId)")

        // Purchase orders tracking supplier orders with multi-status lifecycle
        db.execSQL("CREATE TABLE IF NOT EXISTS ordenes_compra (id TEXT NOT NULL PRIMARY KEY, numero TEXT NOT NULL, proveedorId TEXT NOT NULL, fecha TEXT NOT NULL, estado TEXT NOT NULL DEFAULT 'PENDIENTE', total REAL NOT NULL DEFAULT 0.0, opticaId TEXT NOT NULL, updatedAt TEXT, updatedBy TEXT, FOREIGN KEY(proveedorId) REFERENCES proveedores(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_oc_opticaId ON ordenes_compra(opticaId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_oc_estado ON ordenes_compra(estado)")

        // Line items for each purchase order (frame + quantity + unit cost)
        db.execSQL("CREATE TABLE IF NOT EXISTS orden_compra_items (id TEXT NOT NULL PRIMARY KEY, ordenId TEXT NOT NULL, monturaId TEXT NOT NULL, cantidad INTEGER NOT NULL, costoUnitario REAL NOT NULL DEFAULT 0.0, recibido INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(ordenId) REFERENCES ordenes_compra(id) ON DELETE CASCADE, FOREIGN KEY(monturaId) REFERENCES monturas(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_oci_ordenId ON orden_compra_items(ordenId)")

        // Physical inventory count events per optica (cycle count reconciliation)
        db.execSQL("CREATE TABLE IF NOT EXISTS inventario_fisico (id TEXT NOT NULL PRIMARY KEY, fecha TEXT NOT NULL, estado TEXT NOT NULL DEFAULT 'EN_PROGRESO', opticaId TEXT NOT NULL, userId TEXT NOT NULL, notas TEXT NOT NULL DEFAULT '', updatedAt TEXT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_if_opticaId ON inventario_fisico(opticaId)")

        // Per-frame counted quantities with system-vs-actual variance tracking
        db.execSQL("CREATE TABLE IF NOT EXISTS inventario_fisico_detalle (id TEXT NOT NULL PRIMARY KEY, inventarioId TEXT NOT NULL, monturaId TEXT NOT NULL, stockSistema INTEGER NOT NULL, stockContado INTEGER, diferencia INTEGER, FOREIGN KEY(inventarioId) REFERENCES inventario_fisico(id) ON DELETE CASCADE, FOREIGN KEY(monturaId) REFERENCES monturas(id))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ifd_inventario_montura ON inventario_fisico_detalle(inventarioId, monturaId)")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS arqueo_caja (
                id TEXT NOT NULL PRIMARY KEY,
                fecha INTEGER NOT NULL,
                opticaId TEXT NOT NULL,
                fondoCaja REAL NOT NULL,
                efectivoContado REAL NOT NULL,
                tarjetaContado REAL NOT NULL,
                transferenciaContado REAL NOT NULL,
                movilContado REAL NOT NULL,
                efectivoCobrado REAL NOT NULL,
                tarjetaCobrado REAL NOT NULL,
                transferenciaCobrado REAL NOT NULL,
                movilCobrado REAL NOT NULL,
                diferenciaEfectivo REAL NOT NULL,
                diferenciaTarjeta REAL NOT NULL,
                diferenciaTransferencia REAL NOT NULL,
                diferenciaMovil REAL NOT NULL,
                diferenciaTotal REAL NOT NULL,
                cerradoPor TEXT NOT NULL,
                sellado INTEGER NOT NULL DEFAULT 0,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                updatedBy TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_arqueo_caja_fecha_opticaId ON arqueo_caja(fecha, opticaId)"
        )
    }
}
