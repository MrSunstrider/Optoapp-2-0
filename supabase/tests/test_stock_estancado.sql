-- Runs in one transaction that ends in ROLLBACK, so it is safe on any database.
-- rpc_analisis_mensual authorizes through auth.uid(), so a fixture admin is impersonated
-- under the authenticated role; fixtures are inserted first because the opticas insert
-- trigger enforces plan limits for auth.uid().
-- A sale is recognised from either source (SALIDA_VENTA movement or a dispensacion frame
-- link), so both are exercised; the inactive and zero-stock frames pin the inclusion filter.

\set ON_ERROR_STOP on

BEGIN;

INSERT INTO public.opticas (id, nombre) VALUES ('zzt_tse_o1', 'ZZT TSE');

INSERT INTO auth.users (id, email)
VALUES ('a5000000-0000-4000-8000-000000000006', 'zzt_tse_admin@example.invalid');

-- usuario_optica guards (admin bootstrap, optica-per-user cap) are bypassed
-- only while the fixture membership is inserted.
ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
VALUES ('a5000000-0000-4000-8000-000000000006', 'zzt_tse_o1', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.monturas (id, optica_id, sku, modelo, costo, stock_actual, stock_minimo, activo)
SELECT 'zzt_tse_m' || n, 'zzt_tse_o1', 'ZZT-SKU-' || n, 'ZZT modelo ' || n, 40, 5, 1, true
FROM generate_series(1, 9) AS n;

INSERT INTO public.monturas (id, optica_id, sku, modelo, costo, stock_actual, stock_minimo, activo) VALUES
    ('zzt_tse_inactive', 'zzt_tse_o1', 'ZZT-SKU-I', 'ZZT inactive', 40, 5, 1, false),
    ('zzt_tse_empty', 'zzt_tse_o1', 'ZZT-SKU-E', 'ZZT empty', 40, 0, 1, true);

INSERT INTO public.montura_movimientos
    (id, montura_id, optica_id, fecha, tipo, cantidad, stock_previo, stock_nuevo) VALUES
    ('zzt_tse_mov1', 'zzt_tse_m1', 'zzt_tse_o1', CURRENT_DATE - 30, 'SALIDA_VENTA', 1, 6, 5),
    ('zzt_tse_mov2', 'zzt_tse_m2', 'zzt_tse_o1', CURRENT_DATE - 60, 'SALIDA_VENTA', 1, 6, 5),
    ('zzt_tse_mov3', 'zzt_tse_m5', 'zzt_tse_o1', CURRENT_DATE - 10, 'ENTRADA_COMPRA', 1, 4, 5);

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id)
VALUES ('zzt_tse_pac', 'ZZT TSE', DATE '2026-01-01', 'zzt_tse_o1');

INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, estado_entrega, montura_id) VALUES
    ('zzt_tse_d1', 'zzt_tse_pac', 'zzt_tse_o1', CURRENT_DATE - 15, 300, 'Entregado', 'zzt_tse_m3'),
    ('zzt_tse_d2', 'zzt_tse_pac', 'zzt_tse_o1', CURRENT_DATE - 90, 300, 'Entregado', 'zzt_tse_m4');

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a5000000-0000-4000-8000-000000000006', 'role', 'authenticated')::text, true);

DO $$
DECLARE
    v_result jsonb;
    v_item jsonb;
    v_item_count INTEGER;
    v_sold_count INTEGER := 0;
    v_unsold_count INTEGER := 0;
BEGIN
    v_result := public.rpc_analisis_mensual('zzt_tse_o1', DATE '2026-07-01') -> 'stock_estancado';
    v_item_count := jsonb_array_length(v_result);

    ASSERT v_item_count = 9,
        'T3 FAIL: expected 9 active monturas with stock in stock_estancado, got ' || v_item_count;

    FOR v_item IN SELECT * FROM jsonb_array_elements(v_result)
    LOOP
        ASSERT v_item ? 'montura_id', 'T3 FAIL: item missing montura_id';
        ASSERT v_item ? 'sku', 'T3 FAIL: item missing sku';
        ASSERT v_item ? 'modelo', 'T3 FAIL: item missing modelo';
        ASSERT v_item ? 'costo', 'T3 FAIL: item missing costo';
        ASSERT v_item ? 'stock_actual', 'T3 FAIL: item missing stock_actual';
        ASSERT v_item ? 'ultima_venta', 'T3 FAIL: item missing ultima_venta';
        ASSERT v_item ? 'dias_sin_venta', 'T3 FAIL: item missing dias_sin_venta';

        IF (v_item ->> 'ultima_venta') IS NOT NULL AND (v_item ->> 'ultima_venta') != '' THEN
            v_sold_count := v_sold_count + 1;
            ASSERT (v_item ->> 'dias_sin_venta')::INTEGER < 999,
                'T3 FAIL: sold montura ' || (v_item ->> 'montura_id') || ' should have dias_sin_venta < 999';
            ASSERT (v_item ->> 'dias_sin_venta')::INTEGER = CURRENT_DATE - (v_item ->> 'ultima_venta')::DATE,
                'T3 FAIL: sold montura ' || (v_item ->> 'montura_id') || ' dias_sin_venta does not match ultima_venta';
        ELSE
            v_unsold_count := v_unsold_count + 1;
            ASSERT (v_item ->> 'dias_sin_venta')::INTEGER = 999,
                'T3 FAIL: unsold montura ' || (v_item ->> 'montura_id') || ' should have dias_sin_venta = 999, got ' ||
                (v_item ->> 'dias_sin_venta');
        END IF;
    END LOOP;

    ASSERT v_sold_count = 4, 'T3 FAIL: expected 4 sold monturas, found ' || v_sold_count;
    ASSERT v_unsold_count = 5, 'T3 FAIL: expected 5 never-sold monturas, found ' || v_unsold_count;

    RAISE NOTICE 'T3 PASS: stock_estancado has % items (% sold, % unsold)',
        v_item_count, v_sold_count, v_unsold_count;
END;
$$;

ROLLBACK;
