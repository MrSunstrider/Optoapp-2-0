-- Runs in one transaction that ends in ROLLBACK, so it is safe on any local database.
-- The RPC is SECURITY INVOKER, so callers are impersonated through request.jwt.claims.

\set ON_ERROR_STOP on

BEGIN;

-- Local seed.sql adds two alias categories that production never had; the
-- contract is one row per production category (9).
DELETE FROM public.categorias_producto WHERE id IN ('garantia_extendida', 'otro_lente');

CREATE FUNCTION pg_temp.call_rpc(p_uid uuid, p_optica text, p_mes date DEFAULT DATE '2026-07-01')
RETURNS jsonb LANGUAGE plpgsql AS $$
BEGIN
    PERFORM set_config('request.jwt.claims',
        json_build_object('sub', p_uid, 'role', 'authenticated')::text, true);
    RETURN public.rpc_analisis_mensual(p_optica, p_mes);
END;
$$;

CREATE FUNCTION pg_temp.cat_row(p_json jsonb, p_nombre text) RETURNS jsonb LANGUAGE sql AS $$
    SELECT e FROM jsonb_array_elements(p_json -> 'margen_por_categoria') e WHERE e ->> 'categoria' = p_nombre;
$$;

INSERT INTO public.opticas (id, nombre) VALUES
    ('zzt_mrc_a', 'ZZT MRC A'), ('zzt_mrc_b', 'ZZT MRC B'), ('zzt_mrc_c', 'ZZT MRC C'),
    ('zzt_mrc_d', 'ZZT MRC D'), ('zzt_mrc_out', 'ZZT MRC OUT');

INSERT INTO auth.users (id, email) VALUES
    ('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_admin@example.invalid'),
    ('a2000000-0000-4000-8000-000000000002', 'zzt_mrc_gerente@example.invalid'),
    ('a2000000-0000-4000-8000-000000000003', 'zzt_mrc_asesor@example.invalid'),
    ('a2000000-0000-4000-8000-000000000004', 'zzt_mrc_outsider@example.invalid');

-- usuario_optica guards (admin bootstrap, optica-per-user cap) are bypassed
-- only while the fixture memberships are inserted.
ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol) VALUES
    ('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_a', 'admin'),
    ('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_b', 'admin'),
    ('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_c', 'admin'),
    ('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_d', 'admin'),
    ('a2000000-0000-4000-8000-000000000002', 'zzt_mrc_a', 'gerente'),
    ('a2000000-0000-4000-8000-000000000003', 'zzt_mrc_a', 'asesor'),
    ('a2000000-0000-4000-8000-000000000004', 'zzt_mrc_out', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id) VALUES
    ('zzt_mrc_pac_a', 'ZZT A', DATE '2026-01-01', 'zzt_mrc_a'),
    ('zzt_mrc_pac_c', 'ZZT C', DATE '2026-01-01', 'zzt_mrc_c');

INSERT INTO public.dispensaciones
    (id, paciente_id, fecha, optica_id, tipo_lente, material_lente, monto_total, estado_entrega,
     motivo_anulacion, fecha_anulacion) VALUES
    ('zzt_mrc_a_d1', 'zzt_mrc_pac_a', DATE '2026-07-10', 'zzt_mrc_a', 'Monofocal',  'Resina',  200, 'Entregado', NULL, NULL),
    ('zzt_mrc_a_d2', 'zzt_mrc_pac_a', DATE '2026-07-11', 'zzt_mrc_a', 'Progresivo', 'Resina',  300, 'Entregado', NULL, NULL),
    ('zzt_mrc_a_d3', 'zzt_mrc_pac_a', DATE '2026-07-12', 'zzt_mrc_a', 'Monofocal',  'Cristal',  50, 'Pendiente', NULL, NULL),
    ('zzt_mrc_a_d4', 'zzt_mrc_pac_a', DATE '2026-07-13', 'zzt_mrc_a', 'Monofocal',  'Resina',  999, 'Anulado',   'zzt', DATE '2026-07-14'),
    ('zzt_mrc_a_d5', 'zzt_mrc_pac_a', DATE '2026-07-13', 'zzt_mrc_a', 'Monofocal',  'Resina',  888, 'Reclamada', NULL, NULL);

INSERT INTO public.dispensacion_items
    (id, dispensacion_id, optica_id, costo_real_od, costo_real_oi, costo_real_montura,
     costo_real_biselado, costo_real_lc, costo_origen) VALUES
    ('zzt_mrc_a_i1', 'zzt_mrc_a_d1', 'zzt_mrc_a', 25, 25, 80, 10, 0, 'matriz'),
    ('zzt_mrc_a_i2', 'zzt_mrc_a_d2', 'zzt_mrc_a', 40, 40, NULL, NULL, NULL, 'manual'),
    ('zzt_mrc_a_i4', 'zzt_mrc_a_d4', 'zzt_mrc_a', 500, 0, 0, 0, 0, 'matriz'),
    ('zzt_mrc_a_i5', 'zzt_mrc_a_d5', 'zzt_mrc_a', 500, 0, 0, 0, 0, 'matriz');

INSERT INTO public.servicios_extra
    (id, optica_id, descripcion, fecha, metodo_pago, monto_total, estado, realizado_por, costo_real,
     motivo_anulacion, fecha_anulacion) VALUES
    ('zzt_mrc_a_s1', 'zzt_mrc_a', 'tercero',  DATE '2026-07-10', 'Efectivo', 100, 'Entregado', 'tercero', 60,  NULL, NULL),
    ('zzt_mrc_a_s2', 'zzt_mrc_a', 'propio',   DATE '2026-07-11', 'Efectivo',  80, 'Entregado', 'propio',  10,  NULL, NULL),
    ('zzt_mrc_a_s3', 'zzt_mrc_a', 'legacy',   DATE '2026-07-12', 'Efectivo',  20, 'Entregado', NULL,       0,  NULL, NULL),
    ('zzt_mrc_a_s4', 'zzt_mrc_a', 'anulado',  DATE '2026-07-12', 'Efectivo', 500, 'Anulado',   'propio', 400,  'zzt', DATE '2026-07-13');

INSERT INTO public.servicio_extra_items (id, servicio_extra_id, optica_id, costo_real) VALUES
    ('zzt_mrc_a_si2', 'zzt_mrc_a_s2', 'zzt_mrc_a', 30),
    ('zzt_mrc_a_si4', 'zzt_mrc_a_s4', 'zzt_mrc_a', 100);

-- A stale legacy row must never leak into the response.
INSERT INTO public.margen_por_categoria
    (optica_id, categoria_producto_id, periodo, tipo_periodo, ventas_totales, costo_total,
     cantidad_ventas, margen_bruto, margen_porcentaje, ticket_promedio)
VALUES ('zzt_mrc_a', 'lente_progresivo', '2026-07', 'mensual', 1, 1, 1, 0, 0, 1);

DO $$
DECLARE
    v jsonb := pg_temp.call_rpc('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_a');
    v_missing TEXT;
BEGIN
    SELECT string_agg(k, ', ') INTO v_missing
    FROM unnest(ARRAY[
        'ventas_mes', 'cobros_mes', 'costo_mes', 'gastos_mes', 'saldo_pendiente', 'margen_neto_pct',
        'ticket_promedio', 'cantidad_ventas', 'ventas_mes_anterior', 'variacion_ventas_pct',
        'meses_historicos', 'margen_por_categoria', 'deudores', 'proyeccion_caja', 'stock_estancado',
        'valor_inventario',
        'margen_bruto', 'margen_bruto_pct', 'costos_por_linea', 'margen_servicios', 'cobertura'
    ]) AS k WHERE NOT (v ? k);
    ASSERT v_missing IS NULL, 'KI1a FAIL: missing top-level keys: ' || COALESCE(v_missing, '');

    SELECT string_agg(k, ', ') INTO v_missing
    FROM unnest(ARRAY['lentes', 'montura', 'lc', 'biselado', 'servicios_propios',
                      'servicios_tercerizados', 'servicios_sin_clasificar', 'regalos']) AS k
    WHERE NOT (v -> 'costos_por_linea' ? k);
    ASSERT v_missing IS NULL, 'KI1b FAIL: costos_por_linea missing: ' || COALESCE(v_missing, '');

    SELECT string_agg(b || '.' || k, ', ') INTO v_missing
    FROM unnest(ARRAY['propios', 'tercerizados', 'sin_clasificar']) AS b,
         unnest(ARRAY['ventas', 'costos', 'margen', 'margen_pct', 'cantidad']) AS k
    WHERE NOT (v -> 'margen_servicios' -> b ? k);
    ASSERT v_missing IS NULL, 'KI1c FAIL: margen_servicios missing: ' || COALESCE(v_missing, '');

    SELECT string_agg(k, ', ') INTO v_missing
    FROM unnest(ARRAY['lineas_total', 'lineas_reales', 'lineas_estimadas', 'lineas_faltantes',
                      'pct_real', 'pct_estimado', 'pct_faltante']) AS k
    WHERE NOT (v -> 'cobertura' ? k);
    ASSERT v_missing IS NULL, 'KI1d FAIL: cobertura missing: ' || COALESCE(v_missing, '');

    SELECT string_agg(k, ', ') INTO v_missing
    FROM unnest(ARRAY['categoria', 'ventas', 'costos', 'margen', 'margen_pct']) AS k
    WHERE NOT (v -> 'margen_por_categoria' -> 0 ? k);
    ASSERT v_missing IS NULL, 'KI1e FAIL: margen_por_categoria row missing: ' || COALESCE(v_missing, '');
    RAISE NOTICE 'KI1 PASS: key inventory';
END;
$$;

DO $$
DECLARE
    v jsonb := pg_temp.call_rpc('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_a');
    r jsonb;
BEGIN
    ASSERT jsonb_array_length(v -> 'margen_por_categoria') = 9, 'CAT1a FAIL: expected 9 category rows';
    ASSERT (SELECT jsonb_agg(e ->> 'categoria') FROM jsonb_array_elements(v -> 'margen_por_categoria') e)
        = '["Lentes Progresivos","Lentes Monofocales","Lentes Bifocales","Monturas Premium",
            "Monturas Estandar","Monturas Economicas","Servicios Extra","Garantias Extendidas",
            "Otros Lentes"]'::jsonb,
        'CAT1b FAIL: categories must follow categorias_producto.orden';

    r := pg_temp.cat_row(v, 'Lentes Monofocales');
    ASSERT (r ->> 'ventas')::numeric = 200 AND (r ->> 'costos')::numeric = 140
       AND (r ->> 'margen')::numeric = 60 AND (r ->> 'margen_pct')::numeric = 30.00,
        'CAT1c FAIL: monofocal must be 200/140/60/30.00 but was ' || r::text;

    r := pg_temp.cat_row(v, 'Lentes Progresivos');
    ASSERT (r ->> 'ventas')::numeric = 300 AND (r ->> 'costos')::numeric = 80
       AND (r ->> 'margen')::numeric = 220 AND (r ->> 'margen_pct')::numeric = 73.33,
        'CAT1d FAIL: progresivo must ignore the stale legacy row and exclude nothing valid: ' || r::text;

    r := pg_temp.cat_row(v, 'Otros Lentes');
    ASSERT (r ->> 'ventas')::numeric = 50 AND (r ->> 'costos')::numeric = 0
       AND (r ->> 'margen_pct')::numeric = 100.00,
        'CAT1e FAIL: a non-resina monofocal falls into lente_otro: ' || r::text;

    r := pg_temp.cat_row(v, 'Monturas Premium');
    ASSERT (r ->> 'ventas')::numeric = 0 AND (r ->> 'costos')::numeric = 0
       AND (r ->> 'margen')::numeric = 0 AND jsonb_typeof(r -> 'margen_pct') = 'null',
        'CAT1f FAIL: a category without sales has margen_pct null: ' || r::text;
    RAISE NOTICE 'CAT1 PASS: categories with real costs and exclusions';
END;
$$;

INSERT INTO public.regalos_dispensacion (id, dispensacion_id, producto_id, cantidad, costo_unitario, descripcion, optica_id)
VALUES ('zzt_mrc_a_g1', 'zzt_mrc_a_d1', 'zzt_mrc_prod', 2, 5.00, 'ZZT regalo', 'zzt_mrc_a');

DO $$
DECLARE
    r jsonb := pg_temp.cat_row(
        pg_temp.call_rpc('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_a'), 'Lentes Monofocales');
BEGIN
    ASSERT (r ->> 'costos')::numeric = 150 AND (r ->> 'margen')::numeric = 50
       AND (r ->> 'margen_pct')::numeric = 25.00,
        'CAT2 FAIL: gift 2 x 5.00 must raise monofocal costs to 150 but was ' || r::text;
    RAISE NOTICE 'CAT2 PASS: gift cost reaches the category';
END;
$$;

INSERT INTO public.regalos_servicio_extra (id, servicio_extra_id, producto_id, cantidad, costo_unitario, descripcion, optica_id)
VALUES ('zzt_mrc_a_g2', 'zzt_mrc_a_s3', 'zzt_mrc_prod', 1, 3.00, 'ZZT regalo servicio', 'zzt_mrc_a');

-- resumen_diario as recalculation will produce it once servicio and gift costs
-- are included: items 220 + servicios 100 + gifts 13.
INSERT INTO public.resumen_diario (optica_id, fecha, ventas_cantidad, ventas_monto_total, ventas_costo_total)
VALUES ('zzt_mrc_a', DATE '2026-07-15', 6, 750, 333);

DO $$
DECLARE
    v jsonb := pg_temp.call_rpc('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_a');
    c jsonb := v -> 'costos_por_linea';
    m jsonb := v -> 'margen_servicios';
BEGIN
    ASSERT (c ->> 'lentes')::numeric = 130 AND (c ->> 'montura')::numeric = 80
       AND (c ->> 'lc')::numeric = 0 AND (c ->> 'biselado')::numeric = 10,
        'LIN1a FAIL: lens/frame/lc/biselado lines were ' || c::text;
    ASSERT (c ->> 'servicios_propios')::numeric = 40 AND (c ->> 'servicios_tercerizados')::numeric = 60
       AND (c ->> 'servicios_sin_clasificar')::numeric = 0 AND (c ->> 'regalos')::numeric = 13,
        'LIN1b FAIL: servicio buckets and gifts were ' || c::text;

    ASSERT (m -> 'tercerizados' ->> 'ventas')::numeric = 100 AND (m -> 'tercerizados' ->> 'costos')::numeric = 60
       AND (m -> 'tercerizados' ->> 'margen')::numeric = 40 AND (m -> 'tercerizados' ->> 'margen_pct')::numeric = 40.00
       AND (m -> 'tercerizados' ->> 'cantidad')::int = 1,
        'LIN1c FAIL: tercerizados must be 100/60/40/40.00 but was ' || (m -> 'tercerizados')::text;
    ASSERT (m -> 'propios' ->> 'costos')::numeric = 40 AND (m -> 'propios' ->> 'margen')::numeric = 40
       AND (m -> 'propios' ->> 'margen_pct')::numeric = 50.00,
        'LIN1d FAIL: own servicio carries its frame cost: ' || (m -> 'propios')::text;
    ASSERT (m -> 'sin_clasificar' ->> 'ventas')::numeric = 20 AND (m -> 'sin_clasificar' ->> 'costos')::numeric = 0
       AND (m -> 'sin_clasificar' ->> 'margen_pct')::numeric = 100.00
       AND (m -> 'sin_clasificar' ->> 'cantidad')::int = 1,
        'LIN1e FAIL: legacy servicio must be sin_clasificar: ' || (m -> 'sin_clasificar')::text;

    ASSERT abs((SELECT sum(value::numeric) FROM jsonb_each_text(c)) - (v ->> 'costo_mes')::numeric) < 0.01,
        'LIN1f FAIL: costos_por_linea must sum to costo_mes';
    ASSERT abs((SELECT sum((e ->> 'costos')::numeric) FROM jsonb_array_elements(v -> 'margen_por_categoria') e)
               - (v ->> 'costo_mes')::numeric) < 0.01,
        'LIN1g FAIL: category costs must sum to costo_mes';
    ASSERT (v ->> 'margen_bruto')::numeric = 417.00 AND (v ->> 'margen_bruto_pct')::numeric = 55.60,
        'LIN1h FAIL: margen_bruto must be ventas - costo (417.00 / 55.60)';
    RAISE NOTICE 'LIN1 PASS: costs by line, servicio buckets, convergence, gross margin';
END;
$$;

DO $$
DECLARE
    c jsonb := pg_temp.call_rpc('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_a') -> 'cobertura';
BEGIN
    ASSERT (c ->> 'lineas_total')::int = 5 AND (c ->> 'lineas_reales')::int = 4
       AND (c ->> 'lineas_estimadas')::int = 0 AND (c ->> 'lineas_faltantes')::int = 1,
        'COV1a FAIL: counts were ' || c::text;
    ASSERT (c ->> 'pct_real')::numeric = 80.00 AND (c ->> 'pct_estimado')::numeric = 0
       AND (c ->> 'pct_faltante')::numeric = 20.00,
        'COV1b FAIL: percentages were ' || c::text;
    RAISE NOTICE 'COV1 PASS: coverage of the full month';
END;
$$;

INSERT INTO public.servicios_extra (id, optica_id, descripcion, fecha, metodo_pago, monto_total, estado, realizado_por, costo_real)
VALUES ('zzt_mrc_b_s1', 'zzt_mrc_b', 'montura en servicio', DATE '2026-07-10', 'Efectivo', 100, 'Entregado', 'propio', 0);
INSERT INTO public.servicio_extra_items (id, servicio_extra_id, optica_id, costo_real)
VALUES ('zzt_mrc_b_si1', 'zzt_mrc_b_s1', 'zzt_mrc_b', 30);
INSERT INTO public.resumen_diario (optica_id, fecha, ventas_cantidad, ventas_monto_total, ventas_costo_total)
VALUES ('zzt_mrc_b', DATE '2026-07-10', 1, 100, 30);

DO $$
DECLARE
    v jsonb := pg_temp.call_rpc('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_b');
BEGIN
    ASSERT (v -> 'costos_por_linea' ->> 'servicios_propios')::numeric = 30
       AND (v -> 'costos_por_linea' ->> 'montura')::numeric = 0,
        'LIN2a FAIL: frame inside a servicio belongs to the servicio bucket, not montura';
    ASSERT abs((SELECT sum(value::numeric) FROM jsonb_each_text(v -> 'costos_por_linea'))
               - (v ->> 'costo_mes')::numeric) < 0.01,
        'LIN2b FAIL: lines must converge with costo_mes';
    RAISE NOTICE 'LIN2 PASS: frame inside servicio counted once';
END;
$$;

-- COV2: parity vectors. The same table is asserted in CoberturaCostoClassifierTest.
DO $$
DECLARE v_bad TEXT;
BEGIN
    SELECT string_agg(t.nombre || '=' || COALESCE(got, 'null'), ', ') INTO v_bad
    FROM (VALUES
        ('item manual',               app_private.cobertura_clase_item('manual', 10),   'real'),
        ('item matriz',               app_private.cobertura_clase_item('matriz', 0),    'real'),
        ('item mixto',                app_private.cobertura_clase_item('mixto', 5),     'real'),
        ('item legacy 50',            app_private.cobertura_clase_item(NULL, 50),       'estimado'),
        ('item legacy 20',            app_private.cobertura_clase_item(NULL, 20),       'estimado'),
        ('item legacy 0',             app_private.cobertura_clase_item(NULL, 0),        'faltante'),
        ('item legacy null total',    app_private.cobertura_clase_item(NULL, NULL),     'faltante'),
        ('item faltante with 35',     app_private.cobertura_clase_item('faltante', 35), 'faltante'),
        ('item unknown origin',       app_private.cobertura_clase_item('desconocido', 40), 'faltante'),
        ('servicio typed zero',       app_private.cobertura_clase_servicio('propio', 0, 'tipo-1'), 'real'),
        ('servicio untyped zero',     app_private.cobertura_clase_servicio('propio', 0, NULL),     'faltante'),
        ('servicio cost no executor', app_private.cobertura_clase_servicio(NULL, 25, NULL),        'faltante'),
        ('servicio tercero cost',     app_private.cobertura_clase_servicio('tercero', 60, NULL),   'real'),
        ('servicio no executor typed', app_private.cobertura_clase_servicio(NULL, 0, 'tipo-1'),    'faltante')
    ) AS t(nombre, got, expected)
    WHERE got IS DISTINCT FROM expected;
    ASSERT v_bad IS NULL, 'COV2 FAIL: classification mismatches: ' || COALESCE(v_bad, '');
    RAISE NOTICE 'COV2 PASS: classification parity vectors';
END;
$$;

INSERT INTO public.dispensaciones (id, paciente_id, fecha, optica_id, tipo_lente, material_lente, monto_total, estado_entrega)
VALUES ('zzt_mrc_c_d1', 'zzt_mrc_pac_c', DATE '2026-07-10', 'zzt_mrc_c', 'Monofocal', 'Resina', 100, 'Entregado');

INSERT INTO public.dispensacion_items (id, dispensacion_id, optica_id, costo_real_od, costo_origen) VALUES
    ('zzt_mrc_c_i1', 'zzt_mrc_c_d1', 'zzt_mrc_c', 10, 'manual'),
    ('zzt_mrc_c_i2', 'zzt_mrc_c_d1', 'zzt_mrc_c', 20, 'matriz'),
    ('zzt_mrc_c_i3', 'zzt_mrc_c_d1', 'zzt_mrc_c', 5,  'mixto'),
    ('zzt_mrc_c_i4', 'zzt_mrc_c_d1', 'zzt_mrc_c', 50, NULL),
    ('zzt_mrc_c_i5', 'zzt_mrc_c_d1', 'zzt_mrc_c', 20, NULL),
    ('zzt_mrc_c_i6', 'zzt_mrc_c_d1', 'zzt_mrc_c', NULL, NULL),
    ('zzt_mrc_c_i7', 'zzt_mrc_c_d1', 'zzt_mrc_c', 35, 'faltante');

INSERT INTO public.servicio_tipos (id, optica_id, nombre, costo_propio, costo_tercero)
VALUES ('zzt_mrc_c_tipo', 'zzt_mrc_c', 'Tipo cero', 0, 0);

INSERT INTO public.servicios_extra
    (id, optica_id, descripcion, fecha, metodo_pago, monto_total, estado, realizado_por, costo_real, servicio_tipo_id) VALUES
    ('zzt_mrc_c_s1', 'zzt_mrc_c', 'typed zero',   DATE '2026-07-10', 'Efectivo', 10, 'Entregado', 'propio', 0,  'zzt_mrc_c_tipo'),
    ('zzt_mrc_c_s2', 'zzt_mrc_c', 'untyped zero', DATE '2026-07-10', 'Efectivo', 10, 'Entregado', 'propio', 0,  NULL),
    ('zzt_mrc_c_s3', 'zzt_mrc_c', 'no executor',  DATE '2026-07-10', 'Efectivo', 10, 'Entregado', NULL,      25, NULL);

DO $$
DECLARE
    c jsonb := pg_temp.call_rpc('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_c') -> 'cobertura';
BEGIN
    ASSERT (c ->> 'lineas_total')::int = 10 AND (c ->> 'lineas_reales')::int = 4
       AND (c ->> 'lineas_estimadas')::int = 2 AND (c ->> 'lineas_faltantes')::int = 4,
        'COV3a FAIL: 4 real / 2 estimado / 4 faltante expected but was ' || c::text;
    ASSERT (c ->> 'pct_real')::numeric = 40.00 AND (c ->> 'pct_estimado')::numeric = 20.00
       AND (c ->> 'pct_faltante')::numeric = 40.00,
        'COV3b FAIL: percentages were ' || c::text;
    RAISE NOTICE 'COV3 PASS: coverage vectors through the RPC';
END;
$$;

DO $$
DECLARE
    v jsonb := pg_temp.call_rpc('a2000000-0000-4000-8000-000000000001', 'zzt_mrc_d');
BEGIN
    ASSERT jsonb_array_length(v -> 'margen_por_categoria') = 9
       AND NOT EXISTS (
           SELECT 1 FROM jsonb_array_elements(v -> 'margen_por_categoria') e
           WHERE (e ->> 'ventas')::numeric <> 0 OR (e ->> 'costos')::numeric <> 0
              OR (e ->> 'margen')::numeric <> 0 OR jsonb_typeof(e -> 'margen_pct') <> 'null'),
        'EMP1a FAIL: empty month must give 9 zero rows with null margen_pct';
    ASSERT (v ->> 'margen_bruto')::numeric = 0 AND (v ->> 'margen_bruto_pct')::numeric = 0,
        'EMP1b FAIL: zero sales gives zero gross margin without a division error';
    ASSERT (v -> 'cobertura') = '{"lineas_total":0,"lineas_reales":0,"lineas_estimadas":0,"lineas_faltantes":0,
                                  "pct_real":0,"pct_estimado":0,"pct_faltante":0}'::jsonb,
        'EMP1c FAIL: coverage must be all zeros but was ' || (v -> 'cobertura')::text;
    ASSERT NOT EXISTS (
        SELECT 1 FROM jsonb_each(v -> 'costos_por_linea') WHERE value::text::numeric <> 0),
        'EMP1d FAIL: costos_por_linea must be all zeros';
    ASSERT (v -> 'margen_servicios' -> 'tercerizados' ->> 'ventas')::numeric = 0
       AND jsonb_typeof(v -> 'margen_servicios' -> 'tercerizados' -> 'margen_pct') = 'null'
       AND (v -> 'margen_servicios' -> 'tercerizados' ->> 'cantidad')::int = 0,
        'EMP1e FAIL: empty bucket has zeros and null margen_pct';
    RAISE NOTICE 'EMP1 PASS: empty month shape';
END;
$$;

DO $$
DECLARE
    v_caught BOOLEAN;
BEGIN
    v_caught := false;
    BEGIN
        PERFORM pg_temp.call_rpc('a2000000-0000-4000-8000-000000000003', 'zzt_mrc_a');
    EXCEPTION WHEN OTHERS THEN
        v_caught := SQLERRM = 'BI access requires admin or gerente role';
    END;
    ASSERT v_caught, 'GRD1a FAIL: an asesor must be rejected with the BI role error';

    v_caught := false;
    BEGIN
        PERFORM pg_temp.call_rpc('a2000000-0000-4000-8000-000000000004', 'zzt_mrc_a');
    EXCEPTION WHEN OTHERS THEN
        v_caught := SQLERRM = 'Access denied';
    END;
    ASSERT v_caught, 'GRD1b FAIL: a member of another optica must be denied';

    v_caught := false;
    BEGIN
        PERFORM pg_temp.call_rpc(NULL, 'zzt_mrc_a');
    EXCEPTION WHEN OTHERS THEN
        v_caught := SQLERRM = 'Access denied';
    END;
    ASSERT v_caught, 'GRD1c FAIL: an unauthenticated caller must be denied';

    ASSERT pg_temp.call_rpc('a2000000-0000-4000-8000-000000000002', 'zzt_mrc_a') ? 'margen_bruto',
        'GRD1d FAIL: a gerente must be allowed';
    RAISE NOTICE 'GRD1 PASS: caller guards';
END;
$$;

DO $$
DECLARE
    v_def BOOLEAN; v_vol "char"; v_cfg TEXT[]; v_bad TEXT;
BEGIN
    SELECT p.prosecdef, p.provolatile, p.proconfig INTO v_def, v_vol, v_cfg
    FROM pg_proc p WHERE p.oid = 'public.rpc_analisis_mensual(text,date)'::regprocedure;
    ASSERT NOT v_def AND v_vol = 's' AND 'search_path=public' = ANY(v_cfg),
        'GRD2a FAIL: rpc_analisis_mensual must stay SECURITY INVOKER STABLE with search_path=public';
    ASSERT has_function_privilege('authenticated', 'public.rpc_analisis_mensual(text,date)', 'EXECUTE')
       AND has_function_privilege('service_role', 'public.rpc_analisis_mensual(text,date)', 'EXECUTE')
       AND NOT has_function_privilege('anon', 'public.rpc_analisis_mensual(text,date)', 'EXECUTE'),
        'GRD2b FAIL: grants must stay authenticated + service_role, never anon';

    SELECT string_agg(f.sig, ', ') INTO v_bad
    FROM (VALUES ('app_private.cobertura_clase_item(text,numeric)'),
                 ('app_private.cobertura_clase_servicio(text,numeric,text)')) AS f(sig)
    WHERE NOT (has_function_privilege('authenticated', f.sig, 'EXECUTE')
               AND has_function_privilege('service_role', f.sig, 'EXECUTE')
               AND NOT has_function_privilege('anon', f.sig, 'EXECUTE')
               AND (SELECT p.provolatile = 'i' AND 'search_path=""' = ANY(p.proconfig)
                    FROM pg_proc p WHERE p.oid = f.sig::regprocedure));
    ASSERT v_bad IS NULL,
        'GRD2c FAIL: classifier helpers must be IMMUTABLE, search_path empty, authenticated-only: ' || COALESCE(v_bad, '');
    RAISE NOTICE 'GRD2 PASS: security mode and grants';
END;
$$;

ROLLBACK;
