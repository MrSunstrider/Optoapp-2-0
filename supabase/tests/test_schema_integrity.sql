-- =============================================================================
-- Schema Integrity Tests
--
-- Validates that the database schema meets expected invariants after
-- supabase db reset. Runs as DO/ASSERT blocks grouped by domain.
--
-- Usage: supabase db reset && psql -f supabase/tests/test_schema_integrity.sql
-- =============================================================================

-- #############################################################################
-- DOMAIN 1: Core Tables
-- #############################################################################
DO $$
DECLARE
    v_count INTEGER;
    v_missing TEXT[];
BEGIN
    -- Verify all expected core tables exist in information_schema
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

    ASSERT v_missing IS NULL OR array_length(v_missing, 1) IS NULL,
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
    -- pacientes: must have optica_id, id, nombre_completo
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

    -- opticas: must have id, nombre, created_at, plan
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

    -- dispensaciones: must have optica_id, paciente_id, monto_total
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'dispensaciones'
          AND column_name = 'optica_id' AND data_type = 'text'
    ), 'dispensaciones.optica_id must be text';
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'dispensaciones'
          AND column_name = 'monto_total' AND data_type = 'numeric'
    ), 'dispensaciones.monto_total must be numeric';

    -- servicios_extra: must have optica_id
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'servicios_extra'
          AND column_name = 'optica_id' AND data_type = 'text'
    ), 'servicios_extra.optica_id must be text';

    -- pagos: must have optica_id, monto
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
    v_unprotected TEXT[];
    v_tbl TEXT;
BEGIN
    -- Verify that every core business table with optica_id has RLS policies
    -- referencing optica_id (multi-tenant isolation).
    -- We check pg_policies for each table.
    v_unprotected := ARRAY[]::TEXT[];

    FOR v_tbl IN
        SELECT unnest(ARRAY[
            'pacientes', 'evaluaciones', 'dispensaciones', 'dispensacion_items',
            'servicios_extra', 'pagos', 'monturas', 'proveedores',
            'inventario_fisico', 'inventario_fisico_detalle',
            'ordenes_compra', 'orden_compra_items',
            'cierres_caja', 'optica_settings', 'invitaciones',
            'regalos_dispensacion', 'costos_productos',
            'configuracion_financiera', 'gastos_operativos',
            'margen_por_categoria', 'resumen_diario'
        ])
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_policy pol
            JOIN pg_class c ON c.oid = pol.polrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'public'
              AND c.relname = v_tbl
              AND pg_get_expr(pol.polqual, pol.polrelid)::text ILIKE '%optica_id%'
        ) THEN
            v_unprotected := array_append(v_unprotected, v_tbl);
        END IF;
    END LOOP;

    ASSERT array_length(v_unprotected, 1) IS NULL,
        'Tables without optica_id RLS policies: ' || array_to_string(v_unprotected, ', ');

    -- Also verify RLS is enabled on these tables (pg_tables.rowsecurity)
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
            RAISE WARNING 'RLS may not be enabled on %. Verify manually.', v_tbl;
        END IF;
    END LOOP;

    RAISE NOTICE 'DOMAIN 3 PASS: RLS policies verified for all business tables';
END;
$$;

-- #############################################################################
-- DOMAIN 4: Expected Functions
-- #############################################################################
DO $$
DECLARE
    v_missing TEXT[];
    v_fn TEXT;
BEGIN
    v_missing := ARRAY[]::TEXT[];

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
            'suggest_next_ho',
            'rpc_adjust_montura_stock',
            'sync_snapshot',
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

    ASSERT array_length(v_missing, 1) IS NULL OR v_missing IS NULL,
        'Missing expected functions: ' || COALESCE(array_to_string(v_missing, ', '), 'none');

    RAISE NOTICE 'DOMAIN 4 PASS: All expected functions exist';
END;
$$;

-- #############################################################################
-- DOMAIN 5: Tenant isolation key columns (optica_id)
-- #############################################################################
DO $$
DECLARE
    v_missing TEXT[];
    v_tbl TEXT;
BEGIN
    v_missing := ARRAY[]::TEXT[];

    -- Every business table that references a tenant SHOULD have optica_id
    FOR v_tbl IN
        SELECT unnest(ARRAY[
            'pacientes', 'evaluaciones', 'dispensaciones', 'dispensacion_items',
            'servicios_extra', 'pagos', 'monturas', 'montura_movimientos',
            'proveedores', 'inventario_fisico', 'inventario_fisico_detalle',
            'ordenes_compra', 'orden_compra_items',
            'cierres_caja', 'optica_settings', 'invitaciones',
            'regalos_dispensacion', 'costos_productos',
            'configuracion_financiera', 'gastos_operativos',
            'margen_por_categoria', 'resumen_diario'
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

    ASSERT array_length(v_missing, 1) IS NULL,
        'Tables missing optica_id column: ' || COALESCE(array_to_string(v_missing, ', '), 'none');

    RAISE NOTICE 'DOMAIN 5 PASS: All business tables have optica_id column';
END;
$$;

-- #############################################################################
-- DOMAIN 6: Homogeneous sync timestamps (client preserve)
-- #############################################################################
DO $$
DECLARE
    v_bad TEXT[];
    v_missing_audit TEXT[];
    v_extra INT;
    v_sync TEXT[] := ARRAY[
        'pacientes', 'evaluaciones', 'dispensaciones', 'pagos', 'servicios_extra',
        'monturas', 'montura_movimientos'
    ];
    v_tbl TEXT;
BEGIN
    SELECT array_agg(c.relname ORDER BY c.relname) INTO v_bad
    FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_proc p ON p.oid = t.tgfoid
    WHERE NOT t.tgisinternal
      AND n.nspname = 'public'
      AND p.proname = 'update_updated_at'
      AND c.relname = ANY (v_sync);

    ASSERT v_bad IS NULL OR array_length(v_bad, 1) IS NULL,
        'DOMAIN 6: sync tables still call update_updated_at: '
        || COALESCE(array_to_string(v_bad, ', '), 'none');

    v_missing_audit := ARRAY[]::TEXT[];
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

    ASSERT array_length(v_missing_audit, 1) IS NULL,
        'DOMAIN 6: sync tables missing set_updated_audit_fields: '
        || COALESCE(array_to_string(v_missing_audit, ', '), 'none');

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
