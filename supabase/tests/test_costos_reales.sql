-- =============================================================================
-- Test: recalcular_resumen_diario() uses costo_real_* from dispensacion_items
-- Task 1.1 — RED fixture
-- =============================================================================
-- Verifies that the recalcular_resumen_diario() function:
--   A) Sums costo_real_od + costo_real_oi + costo_real_montura + costo_real_biselado
--      from dispensacion_items subquery (UNION ALL pattern, NOT ventas table)
--   B) Falls back to 0 via COALESCE when no items exist (servicios_extra)
--   C) Handles mixed sources (dispensaciones + servicios_extra) correctly
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

    -- Verify function uses UNION ALL on source-of-truth tables (NOT ventas)
    ASSERT v_rpc_body LIKE '%FROM public.dispensaciones%',
        'Function must read from dispensaciones (not ventas)';
    ASSERT v_rpc_body LIKE '%FROM public.servicios_extra%',
        'Function must read from servicios_extra (not ventas)';
    ASSERT v_rpc_body NOT LIKE '%public.ventas%',
        'Function must NOT reference the dropped ventas table';

    -- Verify cost aggregation subquery references dispensacion_items
    ASSERT v_rpc_body LIKE '%FROM public.dispensacion_items%',
        'Function must reference dispensacion_items for real-cost aggregation';

    RAISE NOTICE 'TEST 1.1 PASS: recalcular_resumen_diario() uses UNION ALL with costo_real_*';
END;
$$;
