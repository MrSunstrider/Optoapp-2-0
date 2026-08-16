-- =============================================================================
-- Test: homogeneous client timestamp policy on all Room sync tables
-- Change: fix-conflictos-updated-at-homogeneo
--
-- SAFETY CONTRACT
--   * Behavioral probes run inside ONE transaction that ends in ROLLBACK.
--   * Fixture ids / optica are prefixed `zzt_ts_` — no client emits that namespace.
--   * Must run as a role that bypasses RLS (postgres / service_role).
--
-- Requires migration that DROPs legacy *_updated_at triggers on sync tables.
--
-- Run: psql ... -v ON_ERROR_STOP=1 -f supabase/tests/test_sync_timestamp_homogeneous.sql
-- =============================================================================

\set ON_ERROR_STOP on

-- #############################################################################
-- DOMAIN A: Trigger inventory (homogeneous rule)
-- #############################################################################
DO $$
DECLARE
    v_bad TEXT[];
    v_missing_audit TEXT[];
    v_settings_ok INT;
    v_sync TEXT[] := ARRAY[
        'pacientes', 'evaluaciones', 'dispensaciones', 'pagos', 'servicios_extra',
        'monturas', 'montura_movimientos'
    ];
    v_tbl TEXT;
BEGIN
    -- A1: no sync table may call update_updated_at
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
        'A1 FAIL: sync tables still have update_updated_at: '
        || COALESCE(array_to_string(v_bad, ', '), 'none');

    -- A2: every sync table MUST have set_updated_audit_fields
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
        'A2 FAIL: sync tables missing set_updated_audit_fields: '
        || COALESCE(array_to_string(v_missing_audit, ', '), 'none');

    -- A3: update_updated_at allowed ONLY on cierres_caja + optica_settings
    SELECT count(*) INTO v_settings_ok
    FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_proc p ON p.oid = t.tgfoid
    WHERE NOT t.tgisinternal
      AND n.nspname = 'public'
      AND p.proname = 'update_updated_at'
      AND c.relname NOT IN ('cierres_caja', 'optica_settings');

    ASSERT v_settings_ok = 0,
        'A3 FAIL: update_updated_at attached outside settings tables';

    RAISE NOTICE 'DOMAIN A PASS: homogeneous timestamp triggers';
END;
$$;

-- #############################################################################
-- DOMAIN B: Behavioral preserve (all five formerly dual-trigger tables)
-- #############################################################################
BEGIN;

INSERT INTO public.opticas (id, nombre)
VALUES ('zzt_ts_optica', 'ZZT Timestamp Optica')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id, updated_at)
VALUES (
    'zzt_ts_pac',
    'ZZT Timestamp Paciente',
    '2020-01-01',
    'zzt_ts_optica',
    '2020-01-01T00:00:00Z'
);

INSERT INTO public.evaluaciones (id, paciente_id, fecha, optica_id, updated_at)
VALUES (
    'zzt_ts_eval',
    'zzt_ts_pac',
    '2020-01-01',
    'zzt_ts_optica',
    '2020-01-01T00:00:00Z'
);

INSERT INTO public.dispensaciones (
    id, paciente_id, fecha, optica_id, monto_total, estado_entrega, updated_at
) VALUES (
    'zzt_ts_disp',
    'zzt_ts_pac',
    '2020-01-01',
    'zzt_ts_optica',
    0,
    'Pendiente',
    '2020-01-01T00:00:00Z'
);

INSERT INTO public.servicios_extra (
    id, descripcion, monto_total, estado, metodo_pago, fecha, paciente_id, optica_id, updated_at
) VALUES (
    'zzt_ts_serv',
    'ZZT Timestamp Servicio',
    0,
    'Pendiente',
    'Efectivo',
    '2020-01-01',
    'zzt_ts_pac',
    'zzt_ts_optica',
    '2020-01-01T00:00:00Z'
);

-- tipo must come from chk_pagos_tipo, metodo_pago from chk_pagos_metodo;
-- pagos_origen_xor_chk requires exactly one of dispensacion_id / servicio_extra_id.
INSERT INTO public.pagos (
    id, fecha, tipo, metodo_pago, monto, optica_id, dispensacion_id, updated_at
) VALUES (
    'zzt_ts_pago',
    '2020-01-01',
    'Abono',
    'Efectivo',
    1.00,
    'zzt_ts_optica',
    'zzt_ts_disp',
    '2020-01-01T00:00:00Z'
);

DO $$
DECLARE
    v_client TIMESTAMPTZ := '2024-06-15T12:34:56.789Z';
    v_got TIMESTAMPTZ;
BEGIN
    UPDATE public.pacientes
    SET nombre_completo = 'ZZT Timestamp Paciente 2', updated_at = v_client
    WHERE id = 'zzt_ts_pac';
    SELECT updated_at INTO v_got FROM public.pacientes WHERE id = 'zzt_ts_pac';
    ASSERT v_got = v_client, 'B1 FAIL pacientes: expected client stamp, got ' || v_got;

    UPDATE public.evaluaciones
    SET fecha = fecha, updated_at = v_client
    WHERE id = 'zzt_ts_eval';
    SELECT updated_at INTO v_got FROM public.evaluaciones WHERE id = 'zzt_ts_eval';
    ASSERT v_got = v_client, 'B2 FAIL evaluaciones: expected client stamp, got ' || v_got;

    UPDATE public.dispensaciones
    SET monto_total = 1.00, updated_at = v_client
    WHERE id = 'zzt_ts_disp';
    SELECT updated_at INTO v_got FROM public.dispensaciones WHERE id = 'zzt_ts_disp';
    ASSERT v_got = v_client, 'B3 FAIL dispensaciones: expected client stamp, got ' || v_got;

    UPDATE public.pagos
    SET monto = 2.00, updated_at = v_client
    WHERE id = 'zzt_ts_pago';
    SELECT updated_at INTO v_got FROM public.pagos WHERE id = 'zzt_ts_pago';
    ASSERT v_got = v_client, 'B4 FAIL pagos: expected client stamp, got ' || v_got;

    UPDATE public.servicios_extra
    SET monto_total = 1.00, updated_at = v_client
    WHERE id = 'zzt_ts_serv';
    SELECT updated_at INTO v_got FROM public.servicios_extra WHERE id = 'zzt_ts_serv';
    ASSERT v_got = v_client, 'B5 FAIL servicios_extra: expected client stamp, got ' || v_got;

    RAISE NOTICE 'DOMAIN B PASS: client updated_at preserved on all five sync tables';
END;
$$;

ROLLBACK;

DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE '  SYNC TIMESTAMP HOMOGENEOUS TESTS PASSED';
    RAISE NOTICE '============================================';
END;
$$;
