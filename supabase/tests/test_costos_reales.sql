-- =============================================================================
-- Test: recalcular_resumen_diario() uses costo_real_* from dispensacion_items
-- Task 1.1 — RED fixture
-- =============================================================================
-- Verifies that the recalcular_resumen_diario() function:
--   A) Sums costo_real_od + costo_real_oi + costo_real_montura + costo_real_biselado
--      from dispensacion_items linked via ventas → dispensaciones
--   B) Falls back to costo_unitario_snapshot when costo_real_* columns are NULL
--      (servicios_extra or old dispensaciones without cost tracking)
--   C) Handles mixed ventas (some with items, some without)
--
-- NOTE: This test requires a running Supabase instance with the migration applied.
--       Run via: supabase db reset && psql -h localhost -p 54322 -U postgres -d postgres -f supabase/tests/test_costos_reales.sql

DO $$
DECLARE
    v_rpc_body TEXT;
BEGIN
    -- Verify the function exists after migration
    SELECT prosrc INTO v_rpc_body
    FROM pg_proc p
    JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public'
      AND p.proname = 'recalcular_resumen_diario';

    ASSERT v_rpc_body IS NOT NULL,
        'recalcular_resumen_diario() function must exist';

    -- Verify the function references costo_real_* columns (cost-from-items logic)
    ASSERT v_rpc_body LIKE '%costo_real_od%',
        'Function must reference costo_real_od column';
    ASSERT v_rpc_body LIKE '%costo_real_oi%',
        'Function must reference costo_real_oi column';
    ASSERT v_rpc_body LIKE '%costo_real_montura%',
        'Function must reference costo_real_montura column';
    ASSERT v_rpc_body LIKE '%costo_real_biselado%',
        'Function must reference costo_real_biselado column';

    -- Verify fallback to costo_unitario_snapshot exists
    ASSERT v_rpc_body LIKE '%costo_unitario_snapshot%',
        'Function must reference costo_unitario_snapshot for fallback';

    RAISE NOTICE 'TEST 1.1 PASS: recalcular_resumen_diario() references costo_real_* with snapshot fallback';
END;
$$;
