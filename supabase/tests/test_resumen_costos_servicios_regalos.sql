-- recalcular_resumen_diario real cost: servicio labor cost, servicio frame items and gifts of both
-- sale kinds. Runs in one transaction that ends in ROLLBACK, so it is safe on any database.
-- Fixtures are inserted as the owner first (the opticas insert trigger enforces plan limits for
-- auth.uid()); the RPC is SECURITY DEFINER with a membership guard, so a fixture admin is
-- impersonated under the authenticated role. Each scenario uses its own day so its cost is
-- asserted in isolation.

\set ON_ERROR_STOP on

BEGIN;

INSERT INTO public.opticas (id, nombre) VALUES ('zzt_trc_o1', 'ZZT TRC');

INSERT INTO auth.users (id, email) VALUES
    ('a5000000-0000-4000-8000-000000000019', 'zzt_trc_admin@example.invalid'),
    ('a5000000-0000-4000-8000-000000000119', 'zzt_trc_outsider@example.invalid');

ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
VALUES ('a5000000-0000-4000-8000-000000000019', 'zzt_trc_o1', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id)
VALUES ('zzt_trc_pac', 'ZZT TRC', DATE '2026-01-01', 'zzt_trc_o1');

-- Day 1: dispensacion cost comes from its items (OD 25 + montura 80).
INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, estado_entrega)
VALUES ('zzt_trc_d1', 'zzt_trc_pac', 'zzt_trc_o1', DATE '2026-07-01', 300.0, 'Entregado');
INSERT INTO public.dispensacion_items (id, dispensacion_id, optica_id, tipo_lente, costo_real_od, costo_real_montura)
VALUES ('zzt_trc_d1_i1', 'zzt_trc_d1', 'zzt_trc_o1', 'monofocal', 25.0, 80.0);

-- Day 2: dispensacion without items has no server cost snapshot, so it contributes zero.
INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, estado_entrega)
VALUES ('zzt_trc_d2', 'zzt_trc_pac', 'zzt_trc_o1', DATE '2026-07-02', 150.0, 'Entregado');

-- Day 3: gifts of both sale kinds (2 x 5.00 on the dispensacion, 1 x 8.00 on the servicio).
INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, estado_entrega)
VALUES ('zzt_trc_d3', 'zzt_trc_pac', 'zzt_trc_o1', DATE '2026-07-03', 200.0, 'Entregado');
INSERT INTO public.dispensacion_items (id, dispensacion_id, optica_id, tipo_lente, costo_real_od)
VALUES ('zzt_trc_d3_i1', 'zzt_trc_d3', 'zzt_trc_o1', 'monofocal', 50.0);
INSERT INTO public.regalos_dispensacion (id, dispensacion_id, producto_id, cantidad, costo_unitario, descripcion, optica_id)
VALUES ('zzt_trc_d3_r1', 'zzt_trc_d3', 'estuche', 2, 5.0, 'Estuche', 'zzt_trc_o1');
INSERT INTO public.servicios_extra (id, optica_id, fecha, monto_total, estado, descripcion, metodo_pago)
VALUES ('zzt_trc_s3', 'zzt_trc_o1', DATE '2026-07-03', 40.0, 'Entregado', 'Ajuste', 'Efectivo');
INSERT INTO public.regalos_servicio_extra (id, servicio_extra_id, producto_id, cantidad, costo_unitario, optica_id)
VALUES ('zzt_trc_s3_r1', 'zzt_trc_s3', 'pano', 1, 8.0, 'zzt_trc_o1');

-- Day 4: servicio labor cost snapshot.
INSERT INTO public.servicios_extra (id, optica_id, fecha, monto_total, estado, descripcion, metodo_pago, costo_real)
VALUES ('zzt_trc_s4', 'zzt_trc_o1', DATE '2026-07-04', 120.0, 'Entregado', 'Biselado', 'Efectivo', 60.0);

-- Day 5: servicio frame items (30 + 20) plus labor cost 10.
INSERT INTO public.servicios_extra (id, optica_id, fecha, monto_total, estado, descripcion, metodo_pago, costo_real)
VALUES ('zzt_trc_s5', 'zzt_trc_o1', DATE '2026-07-05', 180.0, 'Entregado', 'Montaje', 'Efectivo', 10.0);
INSERT INTO public.servicio_extra_items (id, servicio_extra_id, montura_id, descripcion, monto, optica_id, costo_real) VALUES
    ('zzt_trc_s5_i1', 'zzt_trc_s5', 'zzt_trc_m1', 'Montura A', 90.0, 'zzt_trc_o1', 30.0),
    ('zzt_trc_s5_i2', 'zzt_trc_s5', 'zzt_trc_m2', 'Montura B', 70.0, 'zzt_trc_o1', 20.0);

-- Day 6: legacy servicio with the default zero cost.
INSERT INTO public.servicios_extra (id, optica_id, fecha, monto_total, estado, descripcion, metodo_pago)
VALUES ('zzt_trc_s6', 'zzt_trc_o1', DATE '2026-07-06', 25.0, 'Entregado', 'Legacy', 'Efectivo');

-- Day 7: cancelled and claimed sales are excluded with every cost source they carry.
INSERT INTO public.servicios_extra (id, optica_id, fecha, monto_total, estado, descripcion, metodo_pago, costo_real) VALUES
    ('zzt_trc_s7_ok', 'zzt_trc_o1', DATE '2026-07-07', 30.0, 'Entregado', 'Activo', 'Efectivo', 5.0),
    ('zzt_trc_s7_an', 'zzt_trc_o1', DATE '2026-07-07', 30.0, 'Anulado', 'Anulado', 'Efectivo', 99.0);
INSERT INTO public.servicio_extra_items (id, servicio_extra_id, descripcion, monto, optica_id, costo_real)
VALUES ('zzt_trc_s7_an_i1', 'zzt_trc_s7_an', 'Montura', 50.0, 'zzt_trc_o1', 99.0);
INSERT INTO public.regalos_servicio_extra (id, servicio_extra_id, producto_id, cantidad, costo_unitario, optica_id)
VALUES ('zzt_trc_s7_an_r1', 'zzt_trc_s7_an', 'pano', 1, 99.0, 'zzt_trc_o1');
INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, estado_entrega) VALUES
    ('zzt_trc_d7_an', 'zzt_trc_pac', 'zzt_trc_o1', DATE '2026-07-07', 300.0, 'Anulado'),
    ('zzt_trc_d7_rc', 'zzt_trc_pac', 'zzt_trc_o1', DATE '2026-07-07', 300.0, 'Reclamada');
INSERT INTO public.dispensacion_items (id, dispensacion_id, optica_id, tipo_lente, costo_real_od) VALUES
    ('zzt_trc_d7_an_i1', 'zzt_trc_d7_an', 'zzt_trc_o1', 'monofocal', 99.0),
    ('zzt_trc_d7_rc_i1', 'zzt_trc_d7_rc', 'zzt_trc_o1', 'monofocal', 99.0);
INSERT INTO public.regalos_dispensacion (id, dispensacion_id, producto_id, cantidad, costo_unitario, descripcion, optica_id) VALUES
    ('zzt_trc_d7_an_r1', 'zzt_trc_d7_an', 'estuche', 1, 99.0, 'Estuche', 'zzt_trc_o1'),
    ('zzt_trc_d7_rc_r1', 'zzt_trc_d7_rc', 'estuche', 1, 99.0, 'Estuche', 'zzt_trc_o1');

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a5000000-0000-4000-8000-000000000019', 'role', 'authenticated')::text, true);

CREATE TEMP TABLE zzt_trc_expected (fecha DATE, costo NUMERIC, cantidad INTEGER, label TEXT) ON COMMIT DROP;
INSERT INTO zzt_trc_expected VALUES
    (DATE '2026-07-01', 105.0, 1, 'R1 items OD 25 + montura 80'),
    (DATE '2026-07-02',   0.0, 1, 'R2 no items and no gifts -> zero'),
    (DATE '2026-07-03',  68.0, 2, 'R3 item 50 + gifts 2 x 5.00 + 1 x 8.00'),
    (DATE '2026-07-04',  60.0, 1, 'R4 servicio costo_real'),
    (DATE '2026-07-05',  60.0, 1, 'R5 servicio items 30 + 20 + costo_real 10'),
    (DATE '2026-07-06',   0.0, 1, 'R6 legacy servicio zero cost'),
    (DATE '2026-07-07',   5.0, 1, 'R7 Anulado/Reclamada excluded');

DO $$
DECLARE
    r RECORD;
    v_costo NUMERIC;
    v_cantidad INTEGER;
BEGIN
    FOR r IN SELECT * FROM zzt_trc_expected ORDER BY fecha LOOP
        PERFORM public.recalcular_resumen_diario('zzt_trc_o1', r.fecha);
        SELECT ventas_costo_total, ventas_cantidad INTO v_costo, v_cantidad
        FROM public.resumen_diario WHERE optica_id = 'zzt_trc_o1' AND fecha = r.fecha;
        ASSERT v_costo IS NOT NULL, r.label || ' FAIL: no resumen_diario row';
        ASSERT abs(v_costo - r.costo) < 0.005,
            r.label || ' FAIL: ventas_costo_total = ' || v_costo || ' (expected ' || r.costo || ')';
        ASSERT v_cantidad = r.cantidad,
            r.label || ' FAIL: ventas_cantidad = ' || v_cantidad || ' (expected ' || r.cantidad || ')';
        RAISE NOTICE '% PASS', r.label;
    END LOOP;
END;
$$;

-- R8 idempotent upsert: a second run keeps one row with the same totals and refreshes calculado_en.
RESET ROLE;
UPDATE public.resumen_diario SET calculado_en = TIMESTAMPTZ '2000-01-01 00:00:00+00'
WHERE optica_id = 'zzt_trc_o1' AND fecha = DATE '2026-07-05';
SET LOCAL ROLE authenticated;

DO $$
DECLARE
    v_rows INTEGER;
    v_costo NUMERIC;
    v_calculado TIMESTAMPTZ;
BEGIN
    PERFORM public.recalcular_resumen_diario('zzt_trc_o1', DATE '2026-07-05');
    SELECT count(*), max(ventas_costo_total), max(calculado_en) INTO v_rows, v_costo, v_calculado
    FROM public.resumen_diario WHERE optica_id = 'zzt_trc_o1' AND fecha = DATE '2026-07-05';
    ASSERT v_rows = 1, 'R8 FAIL: expected one row, got ' || v_rows;
    ASSERT abs(v_costo - 60.0) < 0.005, 'R8 FAIL: re-run changed cost to ' || v_costo;
    ASSERT v_calculado > TIMESTAMPTZ '2000-01-01 00:00:00+00', 'R8 FAIL: calculado_en not refreshed';
    RAISE NOTICE 'R8 PASS: idempotent upsert';
END;
$$;

-- R9 a non-member is rejected before any write.
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a5000000-0000-4000-8000-000000000119', 'role', 'authenticated')::text, true);

DO $$
DECLARE
    v_denied BOOLEAN := false;
BEGIN
    BEGIN
        PERFORM public.recalcular_resumen_diario('zzt_trc_o1', DATE '2026-07-08');
    EXCEPTION WHEN OTHERS THEN
        v_denied := SQLERRM = 'Access denied';
    END;
    ASSERT v_denied, 'R9 FAIL: non-member must get Access denied';
    RAISE NOTICE 'R9 PASS: non-member rejected';
END;
$$;

RESET ROLE;

-- R10 security context, grants and the pago_effect cash contract are unchanged.
DO $$
DECLARE
    v_definer BOOLEAN;
    v_cfg TEXT[];
    v_src TEXT;
    v_owner TEXT;
BEGIN
    ASSERT NOT EXISTS (
        SELECT 1 FROM public.resumen_diario WHERE optica_id = 'zzt_trc_o1' AND fecha = DATE '2026-07-08'
    ), 'R9b FAIL: rejected call must not write a row';

    SELECT p.prosecdef, p.proconfig, pg_get_userbyid(p.proowner)
    INTO v_definer, v_cfg, v_owner
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'recalcular_resumen_diario';

    -- The guard lives in the RPC and the aggregation in the shared core, so both bodies are inspected.
    SELECT string_agg(p.prosrc, E'\n') INTO v_src
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE (n.nspname, p.proname) IN (('public', 'recalcular_resumen_diario'),
                                     ('app_private', 'recalcular_resumen_diario_core'));

    ASSERT v_definer, 'R10a FAIL: must stay SECURITY DEFINER';
    ASSERT array_to_string(v_cfg, ',') IN ('search_path=', 'search_path=""'),
        'R10b FAIL: must keep an empty search_path, got ' || array_to_string(v_cfg, ',');
    ASSERT v_owner = 'postgres', 'R10c FAIL: owner must be postgres, got ' || v_owner;
    ASSERT NOT has_function_privilege('anon', 'public.recalcular_resumen_diario(text,date)', 'EXECUTE'),
        'R10d FAIL: anon must not execute';
    ASSERT has_function_privilege('authenticated', 'public.recalcular_resumen_diario(text,date)', 'EXECUTE')
       AND has_function_privilege('service_role', 'public.recalcular_resumen_diario(text,date)', 'EXECUTE'),
        'R10e FAIL: authenticated and service_role must keep EXECUTE';
    ASSERT v_src LIKE '%app_private.is_optica_member%', 'R10f FAIL: membership guard must be preserved';
    ASSERT v_src LIKE '%SUM(public.pago_effect(tipo, monto))%'
       AND v_src LIKE '%public.pago_effect(pg.tipo, pg.monto) AS efecto%',
        'R10g FAIL: cobros and pending balance must still aggregate through pago_effect';
    RAISE NOTICE 'R10 PASS: security context, grants and pago_effect contract preserved';
END;
$$;

ROLLBACK;
