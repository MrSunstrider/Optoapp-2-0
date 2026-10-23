-- Runs in one transaction that ends in ROLLBACK; the RPCs authorize through auth.uid(),
-- so a fixture admin is impersonated through request.jwt.claims.

BEGIN;

INSERT INTO public.opticas (id, nombre) VALUES
    ('test_fp_consistency', 'Test Optica Financial Pipeline'),
    ('test_fp_empty', 'Test Optica Financial Pipeline Empty');

INSERT INTO auth.users (id, email)
VALUES ('a4000000-0000-4000-8000-000000000001', 'zzt_fp_admin@example.invalid');

-- usuario_optica guards (admin bootstrap, optica-per-user cap) are bypassed
-- only while the fixture memberships are inserted.
ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol) VALUES
    ('a4000000-0000-4000-8000-000000000001', 'test_fp_consistency', 'admin'),
    ('a4000000-0000-4000-8000-000000000001', 'test_fp_empty', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a4000000-0000-4000-8000-000000000001', 'role', 'authenticated')::text, true);

DO $$
DECLARE
    v_optica_id TEXT := 'test_fp_consistency';
    v_fecha DATE := '2026-07-01';
    v_disp_id TEXT := 'test_fp_disp_1';
    v_serv_id TEXT := 'test_fp_serv_1';
    v_pago_id TEXT := 'test_fp_pago_1';
    v_ventas_monto NUMERIC;
    v_cobros_monto NUMERIC;
    v_costo_total NUMERIC;
    v_expected_ventas NUMERIC;
    v_expected_costo NUMERIC;
BEGIN
    INSERT INTO public.pacientes (id, optica_id, nombre_completo, fecha_creacion)
    VALUES ('test-fp-paciente', v_optica_id, 'Test FP Patient', v_fecha)
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, monto_pagado, estado_entrega)
    VALUES (v_disp_id, 'test-fp-paciente', v_optica_id, v_fecha, 500.0, 0.0, 'Entregado');

    INSERT INTO public.dispensacion_items (id, dispensacion_id, tipo_lente, costo_real_od, costo_real_montura)
    VALUES (gen_random_uuid(), v_disp_id, 'progresivo', 25.0, 80.0);

    INSERT INTO public.servicios_extra (id, optica_id, fecha, monto_total, estado, descripcion, metodo_pago)
    VALUES (v_serv_id, v_optica_id, v_fecha, 200.0, 'Entregado', 'Test servicio', 'Efectivo');

    INSERT INTO public.pagos (id, optica_id, fecha, tipo, monto, metodo_pago, dispensacion_id)
    VALUES (v_pago_id, v_optica_id, v_fecha, 'Abono', 100.0, 'Efectivo', v_disp_id);

    BEGIN
        PERFORM public.recalcular_resumen_diario(v_optica_id, v_fecha);

        SELECT ventas_monto_total, cobros_monto_total, ventas_costo_total
        INTO v_ventas_monto, v_cobros_monto, v_costo_total
        FROM public.resumen_diario
        WHERE optica_id = v_optica_id AND fecha = v_fecha;

        v_expected_ventas := 500.0 + 200.0;
        v_expected_costo := 25.0 + 80.0;

        ASSERT abs(v_ventas_monto - v_expected_ventas) < 0.01,
            'T1.1 FAIL: ventas_monto_total = ' || v_ventas_monto || ' (expected ' || v_expected_ventas || ')';
        RAISE NOTICE 'T1.1.1 PASS: ventas_monto_total = % (expected %)', v_ventas_monto, v_expected_ventas;

        ASSERT abs(v_cobros_monto - 100.0) < 0.01,
            'T1.1 FAIL: cobros_monto_total = ' || v_cobros_monto || ' (expected 100.0)';
        RAISE NOTICE 'T1.1.2 PASS: cobros_monto_total = % (expected 100.0)', v_cobros_monto;

        ASSERT abs(v_costo_total - v_expected_costo) < 0.01,
            'T1.1 FAIL: ventas_costo_total = ' || v_costo_total || ' (expected ' || v_expected_costo || ')';
        RAISE NOTICE 'T1.1.3 PASS: ventas_costo_total = % (expected %)', v_costo_total, v_expected_costo;

    EXCEPTION WHEN OTHERS THEN
        RAISE WARNING 'T1.1 NOTE: recalcular_resumen_diario failed: %', SQLERRM;
        RAISE EXCEPTION 'T1.1 RED: recalcular_resumen_diario must succeed';
    END;
END;
$$;

DO $$
DECLARE
    v_result jsonb;
    v_keys TEXT[];
    v_expected_keys TEXT[] := ARRAY[
        'ventas_mes', 'cobros_mes', 'costo_mes', 'gastos_mes',
        'saldo_pendiente', 'margen_neto_pct', 'ticket_promedio', 'cantidad_ventas',
        'ventas_mes_anterior', 'cantidad_ventas_mes_anterior', 'costo_mes_anterior',
        'ticket_promedio_mes_anterior', 'variacion_ventas_pct', 'meses_historicos',
        'margen_por_categoria', 'margen_por_categoria_mes_anterior', 'deudores', 'proyeccion_caja',
        'stock_estancado', 'valor_inventario',
        'margen_bruto', 'margen_bruto_pct', 'costos_por_linea', 'margen_servicios',
        'cobertura', 'gastos_por_categoria', 'gastos_por_tipo', 'gastos_fijos',
        'margen_contribucion', 'margen_contribucion_pct', 'punto_equilibrio',
        'margen_neto_objetivo', 'brecha_margen_neto_pp', 'serie_6m'
    ];
    v_missing TEXT;
BEGIN
    BEGIN
        v_result := public.rpc_analisis_mensual('test_fp_consistency', '2026-07-01');

        SELECT array_agg(j.key) INTO v_keys FROM jsonb_object_keys(v_result) AS j(key);

        FOREACH v_missing IN ARRAY v_expected_keys LOOP
            IF NOT (v_result ? v_missing) THEN
                RAISE EXCEPTION 'T1.2 FAIL: missing key %', v_missing;
            END IF;
        END LOOP;

        ASSERT array_length(v_keys, 1) = 34,
            'T1.2 FAIL: rpc_analisis_mensual returned ' || COALESCE(array_length(v_keys, 1)::TEXT, '0') || ' keys (expected 34)';

        RAISE NOTICE 'T1.2 PASS: rpc_analisis_mensual returns all 34 keys';

    EXCEPTION WHEN OTHERS THEN
        RAISE WARNING 'T1.2 NOTE: rpc_analisis_mensual failed: %', SQLERRM;
        RAISE EXCEPTION 'T1.2 RED: rpc_analisis_mensual must succeed and return 34 keys';
    END;
END;
$$;

DO $$
DECLARE
    v_result jsonb;
BEGIN
    BEGIN
        v_result := public.rpc_analisis_mensual('test_fp_empty', '2026-07-01');

        ASSERT (v_result->>'ventas_mes')::numeric = 0,
            'T1.3 FAIL: ventas_mes should be 0 for empty month';
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

DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE '  ALL FINANCIAL PIPELINE TESTS PASSED';
    RAISE NOTICE '============================================';
END;
$$;

ROLLBACK;
