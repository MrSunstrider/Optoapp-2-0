-- T8: Integration test — Cost recalculation with dispensacion_items.costo_real_*
--
-- Validates recalcular_resumen_diario():
--   1. costo_real_* from dispensacion_items is used when items exist
--   2. Zero-cost fallback for servicio_extra (no dispensacion_items)
--   3. Mixed sources — dispensacion with items + servicio_extra without items
--      — correct aggregate sum
--
-- This test inserts data directly into source-of-truth tables
-- (dispensaciones + servicios_extra), NOT the dropped ventas table.
--
-- Run: psql -d <database> -f supabase/tests/test_cost_recalculation.sql
-- Or:  supabase db test

DO $$
DECLARE
    v_optica_id TEXT := 'test_optica_cost_v2';
    v_fecha DATE := CURRENT_DATE;
    v_dispensacion_id TEXT := 'test_cost_disp_v2';
    v_servicio_id TEXT := 'test_cost_serv_v2';
    v_costo_total NUMERIC;
    v_expected NUMERIC;
BEGIN
    -- Clean up previous test runs (skip pacientes + opticas — guard_pacientes_delete + FK prevent cleanup)
    DELETE FROM public.resumen_diario WHERE optica_id = v_optica_id;
    DELETE FROM public.servicios_extra WHERE optica_id = v_optica_id AND id = v_servicio_id;
    DELETE FROM public.dispensacion_items WHERE dispensacion_id = v_dispensacion_id;
    DELETE FROM public.dispensaciones WHERE optica_id = v_optica_id AND id = v_dispensacion_id;

    -- Create test optica and paciente (required by FK constraints)
    INSERT INTO public.opticas (id, nombre)
    VALUES (v_optica_id, 'Test Optica Cost Recalc')
    ON CONFLICT (id) DO NOTHING;
    INSERT INTO public.pacientes (id, optica_id, nombre_completo, fecha_creacion)
    VALUES ('test-cost-paciente', v_optica_id, 'Test Cost Patient', v_fecha)
    ON CONFLICT (id) DO NOTHING;

    -- Create test dispensacion (S/500) with linked items having costo_real_*
    INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, estado_entrega)
    VALUES (v_dispensacion_id, 'test-cost-paciente', v_optica_id, v_fecha, 500.0, 'Entregado');

    INSERT INTO public.dispensacion_items (id, dispensacion_id, tipo_lente, costo_real_od, costo_real_montura)
    VALUES (gen_random_uuid(), v_dispensacion_id, 'progresivo', 25.0, 80.0);

    -- Create test servicio_extra (S/200, no items → cost = 0 fallback)
    INSERT INTO public.servicios_extra (id, optica_id, fecha, monto_total, estado, descripcion, metodo_pago)
    VALUES (v_servicio_id, v_optica_id, v_fecha, 200.0, 'Entregado', 'Test servicio', 'Efectivo');

    -- Run the recalculation
    PERFORM public.recalcular_resumen_diario(v_optica_id, v_fecha);

    -- Read the result
    SELECT ventas_costo_total INTO v_costo_total
    FROM public.resumen_diario
    WHERE optica_id = v_optica_id AND fecha = v_fecha;

    -- Expected: 25.0 (costo_real_od) + 80.0 (costo_real_montura) + 0.0 (no items for servicio_extra) = 105.0
    v_expected := 25.0 + 80.0;

    IF abs(v_costo_total - v_expected) < 0.01 THEN
        RAISE NOTICE '✅ T8.1 PASS: ventas_costo_total = % (expected %)', v_costo_total, v_expected;
    ELSE
        RAISE EXCEPTION '❌ T8.1 FAIL: ventas_costo_total = % (expected %)', v_costo_total, v_expected;
    END IF;

    -- Cleanup (skip pacientes + opticas — guard_pacientes_delete + FK prevent cleanup)
    DELETE FROM public.resumen_diario WHERE optica_id = v_optica_id;
    DELETE FROM public.servicios_extra WHERE optica_id = v_optica_id AND id = v_servicio_id;
    DELETE FROM public.dispensacion_items WHERE dispensacion_id = v_dispensacion_id;
    DELETE FROM public.dispensaciones WHERE optica_id = v_optica_id AND id = v_dispensacion_id;

    RAISE NOTICE '✅ T8 ALL PASS: Cost recalculation with UNION ALL source tables';
END;
$$;
