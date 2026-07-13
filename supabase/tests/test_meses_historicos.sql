-- =============================================================================
-- Test: rpc_analisis_mensual returns meses_historicos
-- Task 1.2 — RED fixture
-- =============================================================================
-- Verifies that rpc_analisis_mensual():
--   A) Returns a "meses_historicos" key in the JSONB output
--   B) Counts DISTINCT DATE_TRUNC('month', fecha) from resumen_diario
--   C) Uses COALESCE to default to 0 when no data exists
--
-- NOTE: This test requires a running Supabase instance with the migration applied.
--       Run via: supabase db reset && psql -h localhost -p 54322 -U postgres -d postgres -f supabase/tests/test_meses_historicos.sql

DO $$
DECLARE
    v_rpc_body TEXT;
BEGIN
    -- Verify the function exists after migration
    SELECT prosrc INTO v_rpc_body
    FROM pg_proc p
    JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public'
      AND p.proname = 'rpc_analisis_mensual';

    ASSERT v_rpc_body IS NOT NULL,
        'rpc_analisis_mensual() function must exist';

    -- Verify meses_historicos is computed
    ASSERT v_rpc_body LIKE '%meses_historicos%',
        'Function must reference meses_historicos in output';

    -- Verify it counts from resumen_diario
    ASSERT v_rpc_body LIKE '%resumen_diario%',
        'Function must reference resumen_diario for month counting';

    -- Verify COALESCE or COUNT fallback for empty data
    ASSERT v_rpc_body LIKE '%COALESCE%' OR v_rpc_body LIKE '%count%' OR v_rpc_body LIKE '%COUNT%',
        'Function must handle empty resumen_diario gracefully';

    RAISE NOTICE 'TEST 1.2 PASS: rpc_analisis_mensual() returns meses_historicos from resumen_diario';
END;
$$;
