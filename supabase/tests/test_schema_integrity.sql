-- =============================================================================
-- Schema Integrity Tests
--
-- The Android client, the web companion and tenant isolation all assume these
-- tables, columns, policies and functions exist. A failure here means a
-- migration removed something production code still depends on.
--
-- Usage: supabase db reset && psql -f supabase/tests/test_schema_integrity.sql
-- =============================================================================

-- #############################################################################
-- DOMAIN 1: Core Tables
-- #############################################################################
DO $$
DECLARE
    v_missing TEXT[];
BEGIN
    WITH expected (tbl) AS (
        VALUES
            ('pacientes'),
            ('evaluaciones'),
            ('dispensaciones'),
            ('dispensacion_items'),
            ('servicios_extra'),
            ('pagos'),
            ('opticas'),
            ('usuario_optica'),
            ('monturas'),
            ('montura_movimientos'),
            ('categorias_producto'),
            ('proveedores'),
            ('inventario_fisico'),
            ('inventario_fisico_detalle'),
            ('ordenes_compra'),
            ('orden_compra_items'),
            ('cierres_caja'),
            ('optica_settings'),
            ('invitaciones'),
            ('app_releases'),
            ('user_profiles'),
            ('pin_attempts'),
            ('schema_migrations_flags'),
            ('regalos_dispensacion'),
            ('feedback_recomendaciones'),
            ('costos_productos'),
            ('configuracion_financiera'),
            ('gastos_operativos'),
            ('margen_por_categoria'),
            ('resumen_diario'),
            ('servicio_tipos'),
            ('pacientes_delete_audit')
    )
    SELECT array_agg(tbl) INTO v_missing
    FROM expected e
    WHERE NOT EXISTS (
        SELECT 1 FROM information_schema.tables t
        WHERE t.table_schema = 'public'
          AND t.table_type = 'BASE TABLE'
          AND t.table_name = e.tbl
    );

    ASSERT COALESCE(array_length(v_missing, 1), 0) = 0,
        'Missing core tables: ' || COALESCE(array_to_string(v_missing, ', '), 'none');

    RAISE NOTICE 'DOMAIN 1 PASS: All % core tables exist',
        (SELECT count(*) FROM information_schema.tables
         WHERE table_schema = 'public' AND table_type = 'BASE TABLE');
END;
$$;

-- #############################################################################
-- DOMAIN 2: Column-level invariants on core tables
-- #############################################################################
DO $$
BEGIN
    -- Ids are client-generated TEXT; a uuid column would reject offline-created rows.
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'pacientes'
          AND column_name = 'id' AND data_type = 'text'
    ), 'pacientes.id must be text';
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'pacientes'
          AND column_name = 'nombre_completo' AND data_type = 'text'
    ), 'pacientes.nombre_completo must be text';
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'pacientes'
          AND column_name = 'optica_id' AND data_type = 'text'
    ), 'pacientes.optica_id must be text';
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'pacientes'
          AND column_name = 'edad' AND data_type = 'integer'
    ), 'pacientes.edad must be integer';

    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'opticas'
          AND column_name = 'plan'
    ), 'opticas.plan must exist';
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'opticas'
          AND column_name = 'id' AND data_type = 'text'
    ), 'opticas.id must be text';

    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'dispensaciones'
          AND column_name = 'optica_id' AND data_type = 'text'
    ), 'dispensaciones.optica_id must be text';
    -- Money must stay NUMERIC; floating point drifts cash totals.
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'dispensaciones'
          AND column_name = 'monto_total' AND data_type = 'numeric'
    ), 'dispensaciones.monto_total must be numeric';

    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'servicios_extra'
          AND column_name = 'optica_id' AND data_type = 'text'
    ), 'servicios_extra.optica_id must be text';

    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'pagos'
          AND column_name = 'optica_id' AND data_type = 'text'
    ), 'pagos.optica_id must be text';
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'pagos'
          AND column_name = 'monto' AND data_type = 'numeric'
    ), 'pagos.monto must be numeric';

    RAISE NOTICE 'DOMAIN 2 PASS: All column-level invariants verified';
END;
$$;

-- #############################################################################
-- DOMAIN 3: RLS Policies on business tables
-- #############################################################################
DO $$
DECLARE
    v_unprotected TEXT[] := ARRAY[]::TEXT[];
    v_rls_disabled TEXT[] := ARRAY[]::TEXT[];
    v_tbl TEXT;
BEGIN
    -- Policies are matched through pg_depend instead of their expression text,
    -- so USING and WITH CHECK clauses both count and formatting changes cannot
    -- hide or fake a tenant guard. Child tables (orden_compra_items,
    -- inventario_fisico_detalle) isolate through their parent's optica_id, so a
    -- dependency on any table's optica_id qualifies.
    FOR v_tbl IN
        SELECT unnest(ARRAY[
            'pacientes', 'evaluaciones', 'dispensaciones', 'dispensacion_items',
            'servicios_extra', 'pagos', 'monturas', 'proveedores',
            'inventario_fisico', 'inventario_fisico_detalle',
            'ordenes_compra', 'orden_compra_items',
            'cierres_caja', 'optica_settings', 'invitaciones',
            'regalos_dispensacion', 'costos_productos',
            'configuracion_financiera', 'gastos_operativos',
            'margen_por_categoria', 'resumen_diario', 'servicio_tipos'
        ])
    LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM pg_policy pol
            JOIN pg_depend d
              ON d.classid = 'pg_policy'::regclass
             AND d.objid = pol.oid
             AND d.refclassid = 'pg_class'::regclass
            JOIN pg_attribute a
              ON a.attrelid = d.refobjid
             AND a.attnum = d.refobjsubid
            WHERE pol.polrelid = format('public.%I', v_tbl)::regclass
              AND a.attname = 'optica_id'
        ) THEN
            v_unprotected := array_append(v_unprotected, v_tbl);
        END IF;
    END LOOP;

    ASSERT COALESCE(array_length(v_unprotected, 1), 0) = 0,
        'Tables without optica_id RLS policies: ' || array_to_string(v_unprotected, ', ');

    -- A policy is inert unless row security is enabled on its table.
    FOR v_tbl IN
        SELECT unnest(ARRAY[
            'pacientes', 'evaluaciones', 'dispensaciones', 'servicios_extra',
            'pagos', 'monturas', 'opticas'
        ])
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_tables
            WHERE schemaname = 'public'
              AND tablename = v_tbl
              AND rowsecurity = true
        ) THEN
            v_rls_disabled := array_append(v_rls_disabled, v_tbl);
        END IF;
    END LOOP;

    ASSERT COALESCE(array_length(v_rls_disabled, 1), 0) = 0,
        'Tables with row level security disabled: ' || array_to_string(v_rls_disabled, ', ');

    RAISE NOTICE 'DOMAIN 3 PASS: RLS policies verified for all business tables';
END;
$$;

-- #############################################################################
-- DOMAIN 4: Expected Functions
-- #############################################################################
DO $$
DECLARE
    v_missing TEXT[] := ARRAY[]::TEXT[];
    v_fn TEXT;
BEGIN
    FOR v_fn IN
        SELECT unnest(ARRAY[
            'rpc_analisis_mensual',
            'set_updated_audit_fields',
            'has_optica_role',
            'enforce_admin_role_assignment_guard',
            'enforce_optica_limit_for_creator',
            'guard_pacientes_delete',
            'sync_user_profiles_from_auth',
            'rpc_cierre_caja_resumen',
            'rpc_adjust_montura_stock',
            'is_internal_owner',
            'enforce_dev_owner_guard',
            'recalcular_resumen_diario'
        ])
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_proc p
            JOIN pg_namespace n ON n.oid = p.pronamespace
            WHERE n.nspname = 'public'
              AND p.proname = v_fn
        ) THEN
            v_missing := array_append(v_missing, v_fn);
        END IF;
    END LOOP;

    ASSERT COALESCE(array_length(v_missing, 1), 0) = 0,
        'Missing expected functions: ' || array_to_string(v_missing, ', ');

    RAISE NOTICE 'DOMAIN 4 PASS: All expected functions exist';
END;
$$;

-- #############################################################################
-- DOMAIN 5: Tenant isolation key columns (optica_id)
-- #############################################################################
DO $$
DECLARE
    v_missing TEXT[] := ARRAY[]::TEXT[];
    v_tbl TEXT;
BEGIN
    FOR v_tbl IN
        SELECT unnest(ARRAY[
            'pacientes', 'evaluaciones', 'dispensaciones', 'dispensacion_items',
            'servicios_extra', 'pagos', 'monturas', 'montura_movimientos',
            'proveedores', 'inventario_fisico', 'inventario_fisico_detalle',
            'ordenes_compra', 'orden_compra_items',
            'cierres_caja', 'optica_settings', 'invitaciones',
            'regalos_dispensacion', 'costos_productos',
            'configuracion_financiera', 'gastos_operativos',
            'margen_por_categoria', 'resumen_diario', 'servicio_tipos'
        ])
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = v_tbl
              AND column_name = 'optica_id'
        ) THEN
            v_missing := array_append(v_missing, v_tbl);
        END IF;
    END LOOP;

    ASSERT COALESCE(array_length(v_missing, 1), 0) = 0,
        'Tables missing optica_id column: ' || array_to_string(v_missing, ', ');

    RAISE NOTICE 'DOMAIN 5 PASS: All business tables have optica_id column';
END;
$$;

-- #############################################################################
-- DOMAIN 6: Homogeneous sync timestamps (client preserve)
-- #############################################################################
DO $$
DECLARE
    v_bad TEXT[];
    v_missing_audit TEXT[] := ARRAY[]::TEXT[];
    v_extra INT;
    v_sync TEXT[] := ARRAY[
        'pacientes', 'evaluaciones', 'dispensaciones', 'pagos', 'servicios_extra',
        'monturas', 'montura_movimientos'
    ];
    v_tbl TEXT;
BEGIN
    -- update_updated_at overwrites the client timestamp that conflict
    -- resolution compares, so synced tables must use set_updated_audit_fields.
    SELECT array_agg(c.relname ORDER BY c.relname) INTO v_bad
    FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_proc p ON p.oid = t.tgfoid
    WHERE NOT t.tgisinternal
      AND n.nspname = 'public'
      AND p.proname = 'update_updated_at'
      AND c.relname = ANY (v_sync);

    ASSERT COALESCE(array_length(v_bad, 1), 0) = 0,
        'DOMAIN 6: sync tables still call update_updated_at: '
        || COALESCE(array_to_string(v_bad, ', '), 'none');

    FOREACH v_tbl IN ARRAY v_sync
    LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM pg_trigger t
            JOIN pg_class c ON c.oid = t.tgrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            JOIN pg_proc p ON p.oid = t.tgfoid
            WHERE NOT t.tgisinternal
              AND n.nspname = 'public'
              AND c.relname = v_tbl
              AND p.proname = 'set_updated_audit_fields'
        ) THEN
            v_missing_audit := array_append(v_missing_audit, v_tbl);
        END IF;
    END LOOP;

    ASSERT COALESCE(array_length(v_missing_audit, 1), 0) = 0,
        'DOMAIN 6: sync tables missing set_updated_audit_fields: '
        || array_to_string(v_missing_audit, ', ');

    SELECT count(*) INTO v_extra
    FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_proc p ON p.oid = t.tgfoid
    WHERE NOT t.tgisinternal
      AND n.nspname = 'public'
      AND p.proname = 'update_updated_at'
      AND c.relname NOT IN ('cierres_caja', 'optica_settings');

    ASSERT v_extra = 0,
        'DOMAIN 6: update_updated_at must only attach to cierres_caja and optica_settings';

    RAISE NOTICE 'DOMAIN 6 PASS: Homogeneous sync timestamp triggers';
END;
$$;

-- =============================================================================
-- Summary
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE '  ALL SCHEMA INTEGRITY TESTS PASSED';
    RAISE NOTICE '============================================';
END;
$$;
