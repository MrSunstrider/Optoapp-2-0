-- T8: Integration test — Cost recalculation with dispensacion_items.costo_real_*
--
-- Validates recalcular_resumen_diario():
--   1. costo_real_* from dispensacion_items is used when items exist
--   2. costo_unitario_snapshot fallback for servicio_extra ventas (no items)
--   3. Mixed ventas — some with items, some without — correct aggregate sum
--
-- Run: psql -d <database> -f supabase/tests/test_cost_recalculation.sql
-- Or:  supabase db test

-- Helper: create test venta with dispensacion_items
DO $$
DECLARE
    v_optica_id TEXT := 'test_optica_cost_v2';
    v_fecha DATE := CURRENT_DATE;
    v_venta_disp_id TEXT;
    v_venta_serv_id TEXT;
    v_dispensacion_id UUID;
    v_costo_total NUMERIC;
    v_expected NUMERIC;
BEGIN
    -- Clean up previous test runs
    DELETE FROM public.resumen_diario WHERE optica_id = v_optica_id;
    DELETE FROM public.dispensacion_items di USING public.dispensaciones d
        WHERE di.dispensacion_id = d.id AND d.venta_id LIKE 'test_cost_%';
    DELETE FROM public.dispensaciones WHERE venta_id LIKE 'test_cost_%';
    DELETE FROM public.ventas WHERE optica_id = v_optica_id AND id LIKE 'test_cost_%';

    -- Create test venta with dispensacion link
    INSERT INTO public.ventas (id, optica_id, fecha, monto_total, costo_unitario_snapshot, estado)
    VALUES ('test_cost_disp', v_optica_id, v_fecha, 500.0, 100.0, 'Completada');

    -- Create dispensacion + items with costo_real_*
    INSERT INTO public.dispensaciones (id, venta_id, optica_id, fecha)
    VALUES (gen_random_uuid(), 'test_cost_disp', v_optica_id, v_fecha)
    RETURNING id INTO v_dispensacion_id;

    INSERT INTO public.dispensacion_items (id, dispensacion_id, tipo, costo_real_od, costo_real_montura)
    VALUES (gen_random_uuid(), v_dispensacion_id, 'lente', 25.0, 80.0);

    -- Create test servicio_extra venta (no dispensacion_items)
    INSERT INTO public.ventas (id, optica_id, fecha, monto_total, costo_unitario_snapshot, estado, categoria_producto_id)
    VALUES ('test_cost_serv', v_optica_id, v_fecha, 200.0, 15.0, 'Completada', 'servicio_extra');

    -- Run the recalculation
    PERFORM public.recalcular_resumen_diario(v_optica_id, v_fecha);

    -- Read the result
    SELECT ventas_costo_total INTO v_costo_total
    FROM public.resumen_diario
    WHERE optica_id = v_optica_id AND fecha = v_fecha;

    -- Expected: 25.0 (costo_real_od) + 80.0 (costo_real_montura) + 15.0 (snapshot fallback) = 120.0
    v_expected := 25.0 + 80.0 + 15.0;

    IF v_costo_total = v_expected THEN
        RAISE NOTICE '✅ T8.1 PASS: ventas_costo_total = % (expected %)', v_costo_total, v_expected;
    ELSE
        RAISE EXCEPTION '❌ T8.1 FAIL: ventas_costo_total = % (expected %)', v_costo_total, v_expected;
    END IF;

    -- Cleanup
    DELETE FROM public.resumen_diario WHERE optica_id = v_optica_id;
    DELETE FROM public.dispensacion_items WHERE dispensacion_id = v_dispensacion_id;
    DELETE FROM public.dispensaciones WHERE id = v_dispensacion_id;
    DELETE FROM public.ventas WHERE optica_id = v_optica_id AND id LIKE 'test_cost_%';
END;
$$;
