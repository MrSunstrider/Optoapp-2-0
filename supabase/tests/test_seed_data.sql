-- =============================================================================
-- Seed Data Tests (TDD RED phase for Task 3.1)
--
-- Verifies that after supabase db reset + seed, the expected entities exist
-- and foreign key relationships are consistent.
--
-- These tests validate the behavior of supabase/seed.sql.
--
-- Usage: supabase db reset && psql -f supabase/tests/test_seed_data.sql
-- =============================================================================

-- #############################################################################
-- Test: Core tables have at least one row after seed
-- #############################################################################
DO $$
DECLARE
    v_count INTEGER;
BEGIN
    -- opticas: at least one test optica
    SELECT count(*) INTO v_count FROM public.opticas;
    ASSERT v_count >= 1,
        'Expected at least 1 optica after seed, got ' || v_count;

    -- pacientes: at least 3 test pacientes
    SELECT count(*) INTO v_count FROM public.pacientes;
    ASSERT v_count >= 3,
        'Expected at least 3 pacientes after seed, got ' || v_count;

    -- monturas: at least 2 test products
    SELECT count(*) INTO v_count FROM public.monturas;
    ASSERT v_count >= 2,
        'Expected at least 2 monturas after seed, got ' || v_count;

    -- dispensaciones: at least 1
    SELECT count(*) INTO v_count FROM public.dispensaciones;
    ASSERT v_count >= 1,
        'Expected at least 1 dispensación after seed, got ' || v_count;

    -- servicios_extra: at least 1
    SELECT count(*) INTO v_count FROM public.servicios_extra;
    ASSERT v_count >= 1,
        'Expected at least 1 servicio extra after seed, got ' || v_count;

    RAISE NOTICE 'TEST 1 PASS: All core tables have seed data';
END;
$$;

-- #############################################################################
-- Test: Seed data is synthetic (uses @test.com domains)
-- #############################################################################
DO $$
DECLARE
    v_non_test INTEGER;
BEGIN
    -- Check that pacientes with email use @test.com
    SELECT count(*) INTO v_non_test
    FROM public.pacientes
    WHERE email IS NOT NULL
      AND email NOT LIKE '%@test.com'
      AND email NOT LIKE '%@ejemplo.com';

    ASSERT v_non_test = 0,
        'Found ' || v_non_test || ' pacientes with non-test email domains';

    RAISE NOTICE 'TEST 2 PASS: All seed emails use test domains';
END;
$$;

-- #############################################################################
-- Test: Foreign key consistency across seed data
-- #############################################################################
DO $$
DECLARE
    v_orphan INTEGER;
BEGIN
    -- All dispensaciones reference valid pacientes
    SELECT count(*) INTO v_orphan
    FROM public.dispensaciones d
    WHERE NOT EXISTS (
        SELECT 1 FROM public.pacientes p WHERE p.id = d.paciente_id
    );
    ASSERT v_orphan = 0,
        'Found ' || v_orphan || ' dispensaciones with orphan paciente_id';

    -- All servicios_extra reference valid pacientes
    SELECT count(*) INTO v_orphan
    FROM public.servicios_extra s
    WHERE s.paciente_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM public.pacientes p WHERE p.id = s.paciente_id
    );
    ASSERT v_orphan = 0,
        'Found ' || v_orphan || ' servicios_extra with orphan paciente_id';

    -- All dispensaciones reference valid opticas
    SELECT count(*) INTO v_orphan
    FROM public.dispensaciones d
    WHERE NOT EXISTS (
        SELECT 1 FROM public.opticas o WHERE o.id = d.optica_id
    );
    ASSERT v_orphan = 0,
        'Found ' || v_orphan || ' dispensaciones with orphan optica_id';

    -- All pacientes reference valid opticas
    SELECT count(*) INTO v_orphan
    FROM public.pacientes p
    WHERE NOT EXISTS (
        SELECT 1 FROM public.opticas o WHERE o.id = p.optica_id
    );
    ASSERT v_orphan = 0,
        'Found ' || v_orphan || ' pacientes with orphan optica_id';

    RAISE NOTICE 'TEST 3 PASS: All foreign key references are consistent';
END;
$$;

-- #############################################################################
-- Test: Seed idempotency (ON CONFLICT DO NOTHING)
-- #############################################################################
-- Captures row counts, re-executes seed.sql via \i, then asserts counts
-- are unchanged — proving every INSERT uses ON CONFLICT DO NOTHING.
--
-- NOTE: The \i meta-command MUST be outside any DO block (psql, not PL/pgSQL).

CREATE TEMP TABLE IF NOT EXISTS _seed_counts_before AS
SELECT 'opticas'            AS tbl, count(*) AS cnt FROM public.opticas
UNION ALL
SELECT 'pacientes',               count(*) FROM public.pacientes
UNION ALL
SELECT 'monturas',                count(*) FROM public.monturas
UNION ALL
SELECT 'dispensaciones',          count(*) FROM public.dispensaciones
UNION ALL
SELECT 'servicios_extra',         count(*) FROM public.servicios_extra;

\i supabase/seed.sql

DO $$
DECLARE
    v_after  INTEGER;
    v_before INTEGER;
    r        RECORD;
BEGIN
    FOR r IN SELECT * FROM _seed_counts_before LOOP
        EXECUTE format('SELECT count(*) FROM public.%I', r.tbl) INTO v_after;
        v_before := r.cnt;
        ASSERT v_after = v_before,
            'Idempotency FAILED: ' || r.tbl || ' went from ' || v_before
            || ' to ' || v_after || ' after re-seed. '
            || 'Missing ON CONFLICT DO NOTHING?';
    END LOOP;

    DROP TABLE IF EXISTS _seed_counts_before;
    RAISE NOTICE 'TEST 4 PASS: Seed is idempotent — no row count change on re-run';
END;
$$;

-- =============================================================================
-- Summary
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE '  ALL SEED DATA TESTS PASSED';
    RAISE NOTICE '============================================';
END;
$$;
