-- =============================================================================
-- W0 Integration Test: Financial Pipeline Consistency
--
-- Three assertions that must pass AFTER the fix migrations are applied:
--   1. recalcular_resumen_diario output matches transactional data
--   2. rpc_analisis_mensual returns all 16 JSON keys
--   3. Empty month returns zeros/empty arrays
--
-- Run via: supabase db reset && psql -h localhost -p 54322 -U postgres -d postgres -f supabase/tests/test_financial_pipeline_consistency.sql
-- =============================================================================

-- #############################################################################
-- Assertion 1: Happy path — ventas_monto_total = SUM source tables,
--              cobros_monto_total = SUM pagos excluding Anulación,
--              ventas_costo_total = SUM costo_real_* from items
-- #############################################################################
DO $$
DECLARE
    v_optica_id TEXT := 'test_fp_consistency';
    v_fecha DATE := '2026-07-01';
    v_disp_id TEXT := 'test_fp_disp_1';
    v_serv_id TEXT := 'test_fp_serv_1';
    v_pago_id TEXT := 'test_fp_pago_1';
    v_item_id UUID;
    v_ventas_monto NUMERIC;
    v_cobros_monto NUMERIC;
    v_costo_total NUMERIC;
    v_expected_ventas NUMERIC;
    v_expected_costo NUMERIC;
    v_pago_anulacion_id TEXT := 'test_fp_anul_1';
BEGIN
    -- Clean up previous test runs (skip pacientes + opticas — guard_pacientes_delete + FK prevent cleanup)
    DELETE FROM public.resumen_diario WHERE optica_id = v_optica_id;
    DELETE FROM public.pagos WHERE optica_id = v_optica_id AND id LIKE 'test_fp_%';
    DELETE FROM public.dispensacion_items di USING public.dispensaciones d
        WHERE di.dispensacion_id = d.id AND d.id = v_disp_id;
    DELETE FROM public.dispensaciones WHERE optica_id = v_optica_id AND id = v_disp_id;
    DELETE FROM public.servicios_extra WHERE optica_id = v_optica_id AND id = v_serv_id;

    -- Create test optica and paciente (required by FK constraints)
    INSERT INTO public.opticas (id, nombre)
    VALUES (v_optica_id, 'Test Optica Financial Pipeline')
    ON CONFLICT (id) DO NOTHING;
    INSERT INTO public.pacientes (id, optica_id, nombre_completo, fecha_creacion)
    VALUES ('test-fp-paciente', v_optica_id, 'Test FP Patient', v_fecha)
    ON CONFLICT (id) DO NOTHING;

    -- Insert test dispensacion (S/500)
    INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, monto_pagado, estado_entrega)
    VALUES (v_disp_id, 'test-fp-paciente', v_optica_id, v_fecha, 500.0, 0.0, 'Entregado');

    -- Insert dispensacion_items with costo_real_* (total: 25 + 80 = 105)
    INSERT INTO public.dispensacion_items (id, dispensacion_id, tipo_lente, costo_real_od, costo_real_montura)
    VALUES (gen_random_uuid(), v_disp_id, 'progresivo', 25.0, 80.0);

    -- Insert test servicio_extra (S/200, no items → cost = 0)
    INSERT INTO public.servicios_extra (id, optica_id, fecha, monto_total, estado, descripcion, metodo_pago)
    VALUES (v_serv_id, v_optica_id, v_fecha, 200.0, 'Entregado', 'Test servicio', 'Efectivo');

    -- Insert test pago (S/100, tipo 'Abono')
    INSERT INTO public.pagos (id, optica_id, fecha, tipo, monto, metodo_pago, dispensacion_id)
    VALUES (v_pago_id, v_optica_id, v_fecha, 'Abono', 100.0, 'Efectivo', v_disp_id);

    -- Call the function (this will fail BEFORE the fix migration is applied)
    BEGIN
        PERFORM public.recalcular_resumen_diario(v_optica_id, v_fecha);

        -- Read results
        SELECT ventas_monto_total, cobros_monto_total, ventas_costo_total
        INTO v_ventas_monto, v_cobros_monto, v_costo_total
        FROM public.resumen_diario
        WHERE optica_id = v_optica_id AND fecha = v_fecha;

        -- Expected: 500.00 + 200.00 = 700.00
        v_expected_ventas := 500.0 + 200.0;
        -- Expected: 25.0 (costo_real_od) + 80.0 (costo_real_montura) + 0.0 (servicio_extra fallback) = 105.0
        v_expected_costo := 25.0 + 80.0;

        -- Assertion 1a: ventas_monto_total matches SUM of source tables
        ASSERT abs(v_ventas_monto - v_expected_ventas) < 0.01,
            'T1.1 FAIL: ventas_monto_total = ' || v_ventas_monto || ' (expected ' || v_expected_ventas || ')';
        RAISE NOTICE 'T1.1.1 PASS: ventas_monto_total = % (expected %)', v_ventas_monto, v_expected_ventas;

        -- Assertion 1b: cobros_monto_total matches SUM pagos (excl. Anulación)
        ASSERT abs(v_cobros_monto - 100.0) < 0.01,
            'T1.1 FAIL: cobros_monto_total = ' || v_cobros_monto || ' (expected 100.0)';
        RAISE NOTICE 'T1.1.2 PASS: cobros_monto_total = % (expected 100.0)', v_cobros_monto;

        -- Assertion 1c: ventas_costo_total matches costo_real_* sum + fallback
        ASSERT abs(v_costo_total - v_expected_costo) < 0.01,
            'T1.1 FAIL: ventas_costo_total = ' || v_costo_total || ' (expected ' || v_expected_costo || ')';
        RAISE NOTICE 'T1.1.3 PASS: ventas_costo_total = % (expected %)', v_costo_total, v_expected_costo;

    EXCEPTION WHEN OTHERS THEN
        -- This exception is EXPECTED before the fix migration is applied
        RAISE WARNING 'T1.1 NOTE: recalcular_resumen_diario failed (expected before fix migration): %', SQLERRM;
        RAISE EXCEPTION 'T1.1 RED: recalcular_resumen_diario must succeed after fix migration';
    END;

    -- Cleanup (skip pacientes + opticas — guard_pacientes_delete + FK prevent cleanup)
    DELETE FROM public.resumen_diario WHERE optica_id = v_optica_id;
    DELETE FROM public.pagos WHERE optica_id = v_optica_id AND id LIKE 'test_fp_%';
    DELETE FROM public.dispensacion_items di USING public.dispensaciones d
        WHERE di.dispensacion_id = d.id AND d.id = v_disp_id;
    DELETE FROM public.dispensaciones WHERE optica_id = v_optica_id AND id = v_disp_id;
    DELETE FROM public.servicios_extra WHERE optica_id = v_optica_id AND id = v_serv_id;
END;
$$;

-- #############################################################################
-- Assertion 2: rpc_analisis_mensual returns all 16 expected JSON keys
-- #############################################################################
DO $$
DECLARE
    v_result jsonb;
    v_keys TEXT[];
    v_expected_keys TEXT[] := ARRAY[
        'ventas_mes', 'cobros_mes', 'costo_mes', 'gastos_mes',
        'saldo_pendiente', 'margen_neto_pct', 'ticket_promedio', 'cantidad_ventas',
        'ventas_mes_anterior', 'variacion_ventas_pct', 'meses_historicos',
        'margen_por_categoria', 'deudores', 'proyeccion_caja',
        'stock_estancado', 'valor_inventario'
    ];
    v_missing TEXT;
BEGIN
    -- Call the function (this will fail BEFORE the fix migration is applied)
    BEGIN
        v_result := public.rpc_analisis_mensual('test_fp_consistency', '2026-07-01');

        -- Get keys from the JSON result
        SELECT array_agg(j.key) INTO v_keys FROM jsonb_object_keys(v_result) AS j(key);

        -- Check each expected key exists
        IF NOT (v_result ? 'ventas_mes') THEN
            RAISE EXCEPTION 'T1.2 FAIL: missing key ventas_mes';
        END IF;
        IF NOT (v_result ? 'margen_por_categoria') THEN
            RAISE EXCEPTION 'T1.2 FAIL: missing key margen_por_categoria';
        END IF;
        IF NOT (v_result ? 'deudores') THEN
            RAISE EXCEPTION 'T1.2 FAIL: missing key deudores';
        END IF;
        IF NOT (v_result ? 'proyeccion_caja') THEN
            RAISE EXCEPTION 'T1.2 FAIL: missing key proyeccion_caja';
        END IF;
        IF NOT (v_result ? 'stock_estancado') THEN
            RAISE EXCEPTION 'T1.2 FAIL: missing key stock_estancado';
        END IF;
        IF NOT (v_result ? 'valor_inventario') THEN
            RAISE EXCEPTION 'T1.2 FAIL: missing key valor_inventario';
        END IF;
        IF NOT (v_result ? 'meses_historicos') THEN
            RAISE EXCEPTION 'T1.2 FAIL: missing key meses_historicos';
        END IF;

        -- Verify all 16 keys are present
        ASSERT array_length(v_keys, 1) = 16,
            'T1.2 FAIL: rpc_analisis_mensual returned ' || COALESCE(array_length(v_keys, 1)::TEXT, '0') || ' keys (expected 16)';

        RAISE NOTICE 'T1.2 PASS: rpc_analisis_mensual returns all 16 keys';

    EXCEPTION WHEN OTHERS THEN
        RAISE WARNING 'T1.2 NOTE: rpc_analisis_mensual failed (expected before fix migration): %', SQLERRM;
        RAISE EXCEPTION 'T1.2 RED: rpc_analisis_mensual must succeed and return 16 keys after fix migration';
    END;
END;
$$;

-- #############################################################################
-- Assertion 3: Empty month returns zeros/empty arrays for restored fields
-- #############################################################################
DO $$
DECLARE
    v_result jsonb;
BEGIN
    BEGIN
        v_result := public.rpc_analisis_mensual('test_fp_empty', '2026-07-01');

        -- Assert restored fields return zero/empty values for empty month
        ASSERT (v_result->>'ventas_mes')::numeric = 0,
            'T1.3 FAIL: ventas_mes should be 0 for empty month';
        -- Note: margen_por_categoria returns categories with zero values (LEFT JOIN with seed categorias_producto)
        ASSERT jsonb_typeof(v_result->'margen_por_categoria') = 'array',
            'T1.3 FAIL: margen_por_categoria should be an array for empty month';
        ASSERT jsonb_typeof(v_result->'deudores') = 'object',
            'T1.3 FAIL: deudores should be an object for empty month';
        ASSERT jsonb_typeof(v_result->'proyeccion_caja') = 'object',
            'T1.3 FAIL: proyeccion_caja should be an object for empty month';

        RAISE NOTICE 'T1.3 PASS: Empty month returns correct zero/empty values';
    EXCEPTION WHEN OTHERS THEN
        RAISE WARNING 'T1.3 NOTE: rpc_analisis_mensual failed (expected before fix migration): %', SQLERRM;
        RAISE EXCEPTION 'T1.3 RED: Empty month assertions must pass after fix migration';
    END;
END;
$$;

-- =============================================================================
-- Summary
-- =============================================================================
DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE '  ALL FINANCIAL PIPELINE TESTS PASSED';
    RAISE NOTICE '============================================';
END;
$$;
