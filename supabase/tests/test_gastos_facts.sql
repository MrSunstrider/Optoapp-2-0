-- Runs in one transaction that ends in ROLLBACK, so it is safe on any local database.
-- The RPC is SECURITY INVOKER, so the caller is impersonated through request.jwt.claims.

\set ON_ERROR_STOP on

BEGIN;

CREATE FUNCTION pg_temp.call_rpc(p_optica text, p_mes date DEFAULT DATE '2026-07-01')
RETURNS jsonb LANGUAGE plpgsql AS $$
BEGIN
    PERFORM set_config('request.jwt.claims',
        json_build_object('sub', 'a3000000-0000-4000-8000-000000000001', 'role', 'authenticated')::text, true);
    RETURN public.rpc_analisis_mensual(p_optica, p_mes);
END;
$$;

INSERT INTO public.opticas (id, nombre)
SELECT id, 'ZZT GF ' || id FROM unnest(ARRAY[
    'zzt_gf_group', 'zzt_gf_none', 'zzt_gf_mixed', 'zzt_gf_contrib', 'zzt_gf_below',
    'zzt_gf_negative', 'zzt_gf_nosales', 'zzt_gf_cfg', 'zzt_gf_serie']) AS id;

INSERT INTO auth.users (id, email)
VALUES ('a3000000-0000-4000-8000-000000000001', 'zzt_gf_admin@example.invalid');

-- usuario_optica guards (admin bootstrap, optica-per-user cap) are bypassed
-- only while the fixture memberships are inserted.
ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
SELECT 'a3000000-0000-4000-8000-000000000001', id, 'admin' FROM public.opticas WHERE id LIKE 'zzt\_gf\_%';
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

DO $$
DECLARE v_bad TEXT;
BEGIN
    SELECT string_agg(t.categoria || '/' || COALESCE(t.tipo, 'null') || '=' || COALESCE(t.got, 'null'), ', ')
    INTO v_bad
    FROM (VALUES
        ('alquiler',    NULL::text,  'fijo',     app_private.gasto_tipo_efectivo(NULL, 'alquiler')),
        ('servicios',   NULL,        'fijo',     app_private.gasto_tipo_efectivo(NULL, 'servicios')),
        ('personal',    NULL,        'fijo',     app_private.gasto_tipo_efectivo(NULL, 'personal')),
        ('impuestos',   NULL,        'fijo',     app_private.gasto_tipo_efectivo(NULL, 'impuestos')),
        ('proveedores', NULL,        'variable', app_private.gasto_tipo_efectivo(NULL, 'proveedores')),
        ('insumos',     NULL,        'variable', app_private.gasto_tipo_efectivo(NULL, 'insumos')),
        ('marketing',   NULL,        'variable', app_private.gasto_tipo_efectivo(NULL, 'marketing')),
        ('otro',        NULL,        'variable', app_private.gasto_tipo_efectivo(NULL, 'otro')),
        ('alquiler',    'variable',  'variable', app_private.gasto_tipo_efectivo('variable', 'alquiler')),
        ('otro',        'fijo',      'fijo',     app_private.gasto_tipo_efectivo('fijo', 'otro'))
    ) AS t(categoria, tipo, expected, got)
    WHERE t.got IS DISTINCT FROM t.expected;
    ASSERT v_bad IS NULL, 'GT1a FAIL: effective type mismatches: ' || COALESCE(v_bad, '');

    ASSERT (SELECT p.provolatile = 'i' AND 'search_path=""' = ANY(p.proconfig)
            FROM pg_proc p WHERE p.oid = 'app_private.gasto_tipo_efectivo(text,text)'::regprocedure)
       AND has_function_privilege('authenticated', 'app_private.gasto_tipo_efectivo(text,text)', 'EXECUTE')
       AND has_function_privilege('service_role', 'app_private.gasto_tipo_efectivo(text,text)', 'EXECUTE')
       AND NOT has_function_privilege('anon', 'app_private.gasto_tipo_efectivo(text,text)', 'EXECUTE'),
        'GT1b FAIL: helper must be IMMUTABLE with empty search_path and authenticated-only grants';
    RAISE NOTICE 'GT1 PASS: gasto_tipo_efectivo';
END;
$$;

INSERT INTO public.gastos_operativos (optica_id, categoria, monto, fecha) VALUES
    ('zzt_gf_group', 'alquiler',  2000, DATE '2026-07-05'),
    ('zzt_gf_group', 'personal',  1000, DATE '2026-07-06'),
    ('zzt_gf_group', 'marketing',  500, DATE '2026-07-07');

DO $$
DECLARE
    v jsonb := pg_temp.call_rpc('zzt_gf_group');
BEGIN
    ASSERT (SELECT jsonb_agg(jsonb_build_object('c', e ->> 'categoria', 'm', (e ->> 'monto')::numeric,
                                                't', e ->> 'tipo_predominante'))
            FROM jsonb_array_elements(v -> 'gastos_por_categoria') e)
        = '[{"c":"alquiler","m":2000.00,"t":"fijo"},{"c":"personal","m":1000.00,"t":"fijo"},
            {"c":"marketing","m":500.00,"t":"variable"}]'::jsonb,
        'GT2a FAIL: grouped gastos were ' || (v -> 'gastos_por_categoria')::text;
    ASSERT (v -> 'gastos_por_tipo' ->> 'fijo')::numeric = 3000 AND (v -> 'gastos_por_tipo' ->> 'variable')::numeric = 500
       AND (v ->> 'gastos_mes')::numeric = 3500,
        'GT2b FAIL: gastos_por_tipo must split 3000/500 and sum to gastos_mes 3500';
    ASSERT (v ->> 'gastos_fijos')::numeric = 3000, 'GT2c FAIL: gastos_fijos must equal gastos_por_tipo.fijo';
    ASSERT (SELECT count(*) FROM public.gastos_operativos WHERE optica_id = 'zzt_gf_group' AND tipo_gasto IS NOT NULL) = 0,
        'GT2d FAIL: the RPC must not backfill tipo_gasto';
    RAISE NOTICE 'GT2 PASS: grouped totals';
END;
$$;

DO $$
DECLARE
    v jsonb := pg_temp.call_rpc('zzt_gf_none');
BEGIN
    ASSERT v -> 'gastos_por_categoria' = '[]'::jsonb, 'GT3a FAIL: no gastos gives an empty array';
    ASSERT v -> 'gastos_por_tipo' = '{"fijo":0,"variable":0}'::jsonb,
        'GT3b FAIL: no gastos gives zero totals but was ' || (v -> 'gastos_por_tipo')::text;
    ASSERT (v ->> 'gastos_fijos')::numeric = 0 AND jsonb_typeof(v -> 'punto_equilibrio') = 'null',
        'GT3c FAIL: no sales and no gastos gives gastos_fijos 0 and punto_equilibrio null';
    RAISE NOTICE 'GT3 PASS: month without gastos';
END;
$$;

INSERT INTO public.gastos_operativos (optica_id, categoria, monto, fecha, tipo_gasto) VALUES
    ('zzt_gf_mixed', 'otro',        300, DATE '2026-07-05', 'variable'),
    ('zzt_gf_mixed', 'otro',        100, DATE '2026-07-06', 'fijo'),
    ('zzt_gf_mixed', 'insumos',     100, DATE '2026-07-05', 'fijo'),
    ('zzt_gf_mixed', 'insumos',     100, DATE '2026-07-06', 'variable'),
    ('zzt_gf_mixed', 'alquiler',    700, DATE '2026-07-05', 'variable'),
    ('zzt_gf_mixed', 'proveedores', 300, DATE '2026-07-05', NULL),
    ('zzt_gf_mixed', 'proveedores', 100, DATE '2026-07-06', 'fijo');

DO $$
DECLARE
    v jsonb := pg_temp.call_rpc('zzt_gf_mixed');
BEGIN
    ASSERT (SELECT jsonb_agg(jsonb_build_object('c', e ->> 'categoria', 'm', (e ->> 'monto')::numeric,
                                                't', e ->> 'tipo_predominante'))
            FROM jsonb_array_elements(v -> 'gastos_por_categoria') e)
        = '[{"c":"alquiler","m":700.00,"t":"variable"},{"c":"otro","m":400.00,"t":"variable"},
            {"c":"proveedores","m":400.00,"t":"variable"},{"c":"insumos","m":200.00,"t":"fijo"}]'::jsonb,
        'GT4a FAIL: order is monto DESC then categoria; predominant type uses the resolved type and ties go to fijo: '
        || (v -> 'gastos_por_categoria')::text;
    ASSERT (v -> 'gastos_por_tipo' ->> 'fijo')::numeric = 300 AND (v -> 'gastos_por_tipo' ->> 'variable')::numeric = 1400,
        'GT4b FAIL: an explicit variable on alquiler counts as variable: ' || (v -> 'gastos_por_tipo')::text;
    ASSERT (v ->> 'gastos_mes')::numeric = 1700
       AND (v -> 'gastos_por_tipo' ->> 'fijo')::numeric + (v -> 'gastos_por_tipo' ->> 'variable')::numeric
           = (v ->> 'gastos_mes')::numeric,
        'GT4c FAIL: gastos_mes must equal fijo + variable';
    RAISE NOTICE 'GT4 PASS: mixed types, ties and explicit overrides';
END;
$$;

INSERT INTO public.resumen_diario (optica_id, fecha, ventas_cantidad, ventas_monto_total, ventas_costo_total) VALUES
    ('zzt_gf_contrib', DATE '2026-07-10', 10, 10000, 4000),
    ('zzt_gf_below',   DATE '2026-07-10', 10, 10000, 4000),
    ('zzt_gf_negative', DATE '2026-07-10', 1, 1000, 900),
    ('zzt_gf_cfg',     DATE '2026-07-10', 10, 10000, 4000);
INSERT INTO public.gastos_operativos (optica_id, categoria, monto, fecha) VALUES
    ('zzt_gf_contrib', 'alquiler',  3000, DATE '2026-07-05'),
    ('zzt_gf_contrib', 'marketing', 1000, DATE '2026-07-06'),
    ('zzt_gf_below',   'alquiler',  5200, DATE '2026-07-05'),
    ('zzt_gf_negative', 'marketing', 300, DATE '2026-07-05'),
    ('zzt_gf_nosales', 'alquiler',   500, DATE '2026-07-05'),
    ('zzt_gf_cfg',     'alquiler',  3000, DATE '2026-07-05'),
    ('zzt_gf_cfg',     'marketing', 1000, DATE '2026-07-06');
INSERT INTO public.configuracion_financiera (optica_id, margen_neto_objetivo) VALUES ('zzt_gf_cfg', 25.0);

DO $$
DECLARE
    v jsonb := pg_temp.call_rpc('zzt_gf_contrib');
BEGIN
    ASSERT (v ->> 'margen_contribucion')::numeric = 5000.00 AND (v ->> 'margen_contribucion_pct')::numeric = 50.00
       AND (v ->> 'punto_equilibrio')::numeric = 6000.00 AND (v ->> 'gastos_fijos')::numeric = 3000,
        'CB1 FAIL: contribution 5000 / 50.00 / break-even 6000.00 expected but was '
        || (v ->> 'margen_contribucion') || ' / ' || (v ->> 'margen_contribucion_pct') || ' / ' || COALESCE(v ->> 'punto_equilibrio', 'null');
    ASSERT (v ->> 'margen_neto_pct')::numeric = 20.0 AND (v ->> 'margen_neto_objetivo')::numeric = 15.00
       AND (v ->> 'brecha_margen_neto_pp')::numeric = 5.00,
        'NO1 FAIL: net margin 20 against the default target 15 gives a gap of 5.00';
    RAISE NOTICE 'CB1/NO1 PASS: contribution, break-even and target met';

    v := pg_temp.call_rpc('zzt_gf_below');
    ASSERT (v ->> 'margen_neto_pct')::numeric = 8.0 AND (v ->> 'brecha_margen_neto_pp')::numeric = -7.00,
        'NO2 FAIL: net margin 8 against target 15 gives a gap of -7.00 but was ' || (v ->> 'brecha_margen_neto_pp');
    ASSERT (v ->> 'punto_equilibrio')::numeric = 8666.67,
        'CB2 FAIL: break-even 5200 / 0.60 must round to 8666.67 but was ' || COALESCE(v ->> 'punto_equilibrio', 'null');
    RAISE NOTICE 'NO2/CB2 PASS: below target and rounded break-even';

    v := pg_temp.call_rpc('zzt_gf_cfg');
    ASSERT (v ->> 'margen_neto_objetivo')::numeric = 25.00 AND (v ->> 'brecha_margen_neto_pp')::numeric = -5.00,
        'NO3 FAIL: a configured target of 25 gives a gap of -5.00';
    RAISE NOTICE 'NO3 PASS: configured target';

    v := pg_temp.call_rpc('zzt_gf_negative');
    ASSERT (v ->> 'margen_contribucion')::numeric = -200.00 AND (v ->> 'margen_contribucion_pct')::numeric = -20.00
       AND jsonb_typeof(v -> 'punto_equilibrio') = 'null',
        'CB3 FAIL: negative contribution means no break-even';
    RAISE NOTICE 'CB3 PASS: negative contribution';

    v := pg_temp.call_rpc('zzt_gf_nosales');
    ASSERT (v ->> 'margen_contribucion_pct')::numeric = 0 AND jsonb_typeof(v -> 'punto_equilibrio') = 'null'
       AND (v ->> 'gastos_fijos')::numeric = 500,
        'CB4 FAIL: no sales gives contribution pct 0, null break-even and gastos_fijos 500';
    RAISE NOTICE 'CB4 PASS: no sales';
END;
$$;

-- Scenario SERIE: six months ending at the requested one. Months outside the
-- window (2026-01) must not leak in; several resumen rows in a month add up.
INSERT INTO public.resumen_diario (optica_id, fecha, ventas_cantidad, ventas_monto_total, ventas_costo_total) VALUES
    ('zzt_gf_serie', DATE '2026-01-15', 1, 9999, 9999),
    ('zzt_gf_serie', DATE '2026-02-10', 1,  500,  100),
    ('zzt_gf_serie', DATE '2026-06-05', 1, 1200,  500),
    ('zzt_gf_serie', DATE '2026-06-20', 1,  800,  300),
    ('zzt_gf_serie', DATE '2026-07-10', 1, 1000,  700);
INSERT INTO public.gastos_operativos (optica_id, categoria, monto, fecha) VALUES
    ('zzt_gf_serie', 'alquiler', 9999, DATE '2026-01-05'),
    ('zzt_gf_serie', 'alquiler',  400, DATE '2026-06-05'),
    ('zzt_gf_serie', 'alquiler',  600, DATE '2026-07-05');

DO $$
DECLARE
    s jsonb := pg_temp.call_rpc('zzt_gf_serie') -> 'serie_6m';
    e jsonb;
BEGIN
    ASSERT jsonb_array_length(s) = 6, 'SE1a FAIL: serie_6m must have exactly 6 entries';
    ASSERT (SELECT jsonb_agg(x ->> 'mes') FROM jsonb_array_elements(s) x)
        = '["2026-02","2026-03","2026-04","2026-05","2026-06","2026-07"]'::jsonb,
        'SE1b FAIL: months must run oldest to newest ending at the requested month';

    e := s -> 5;
    ASSERT (e ->> 'ventas')::numeric = 1000 AND (e ->> 'costo')::numeric = 700 AND (e ->> 'gastos')::numeric = 600
       AND (e ->> 'margen')::numeric = -300.00 AND (e ->> 'margen_neto_pct')::numeric = -30.00,
        'SE1c FAIL: a negative margin must be kept, not clamped: ' || e::text;

    e := s -> 4;
    ASSERT (e ->> 'ventas')::numeric = 2000 AND (e ->> 'costo')::numeric = 800 AND (e ->> 'gastos')::numeric = 400
       AND (e ->> 'margen')::numeric = 800.00 AND (e ->> 'margen_neto_pct')::numeric = 40.00,
        'SE1d FAIL: several rows of one month must be summed: ' || e::text;

    e := s -> 2;
    ASSERT e = '{"mes":"2026-04","ventas":0,"costo":0,"gastos":0,"margen":0,"margen_neto_pct":0}'::jsonb,
        'SE1e FAIL: a month without data is a zero entry with margen_neto_pct: ' || e::text;

    e := s -> 0;
    ASSERT (e ->> 'ventas')::numeric = 500 AND (e ->> 'margen')::numeric = 400.00
       AND (e ->> 'margen_neto_pct')::numeric = 80.00,
        'SE1f FAIL: the oldest month of the window is included: ' || e::text;

    ASSERT NOT EXISTS (SELECT 1 FROM jsonb_array_elements(s) x WHERE NOT (x ? 'margen_neto_pct')),
        'SE1g FAIL: every entry must carry margen_neto_pct';
    RAISE NOTICE 'SE1 PASS: serie_6m';
END;
$$;

DO $$
DECLARE
    s jsonb := pg_temp.call_rpc('zzt_gf_none') -> 'serie_6m';
BEGIN
    ASSERT jsonb_array_length(s) = 6
       AND NOT EXISTS (SELECT 1 FROM jsonb_array_elements(s) x
                       WHERE (x ->> 'ventas')::numeric <> 0 OR (x ->> 'costo')::numeric <> 0
                          OR (x ->> 'gastos')::numeric <> 0 OR (x ->> 'margen_neto_pct')::numeric <> 0),
        'SE2 FAIL: an empty optica still returns 6 zero entries';
    RAISE NOTICE 'SE2 PASS: empty serie_6m';
END;
$$;

ROLLBACK;
