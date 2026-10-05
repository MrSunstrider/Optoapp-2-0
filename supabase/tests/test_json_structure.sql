-- Runs in one transaction that ends in ROLLBACK, so it is safe on any database.
-- rpc_analisis_mensual authorizes through auth.uid(), so a fixture admin is impersonated
-- under the authenticated role; fixtures are inserted first because the opticas insert
-- trigger enforces plan limits for auth.uid().

\set ON_ERROR_STOP on

BEGIN;

-- Local seed.sql adds two alias categories that production never had; the
-- contract is one row per production category (9).
DELETE FROM public.categorias_producto WHERE id IN ('garantia_extendida', 'otro_lente');

INSERT INTO public.opticas (id, nombre) VALUES ('zzt_tjs_o1', 'ZZT TJS');

INSERT INTO auth.users (id, email)
VALUES ('a5000000-0000-4000-8000-000000000001', 'zzt_tjs_admin@example.invalid');

-- usuario_optica guards (admin bootstrap, optica-per-user cap) are bypassed
-- only while the fixture membership is inserted.
ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
VALUES ('a5000000-0000-4000-8000-000000000001', 'zzt_tjs_o1', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id)
VALUES ('zzt_tjs_pac', 'ZZT TJS', DATE '2026-01-01', 'zzt_tjs_o1');

INSERT INTO public.dispensaciones
    (id, paciente_id, fecha, optica_id, tipo_lente, material_lente, monto_total, estado_entrega) VALUES
    ('zzt_tjs_d1', 'zzt_tjs_pac', DATE '2026-07-05', 'zzt_tjs_o1', 'Monofocal',  'Resina', 300, 'Entregado'),
    ('zzt_tjs_d2', 'zzt_tjs_pac', DATE '2026-07-06', 'zzt_tjs_o1', 'Progresivo', 'Resina', 600, 'Entregado'),
    ('zzt_tjs_d3', 'zzt_tjs_pac', DATE '2026-07-07', 'zzt_tjs_o1', 'Bifocal',    'Resina', 200, 'Pendiente');

-- A frame cost on a lens order must not leak revenue into the montura categories.
INSERT INTO public.dispensacion_items (id, dispensacion_id, optica_id, costo_real_od, costo_real_montura, costo_origen)
VALUES ('zzt_tjs_i1', 'zzt_tjs_d1', 'zzt_tjs_o1', 40, 90, 'matriz');

INSERT INTO public.servicios_extra (id, optica_id, descripcion, fecha, metodo_pago, monto_total, estado)
VALUES ('zzt_tjs_se1', 'zzt_tjs_o1', 'ZZT servicio', DATE '2026-07-08', 'Efectivo', 50, 'Entregado');

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a5000000-0000-4000-8000-000000000001', 'role', 'authenticated')::text, true);

DO $$
DECLARE
    v_result jsonb;
    v_item jsonb;
    v_expected_order TEXT[] := ARRAY[
        'Lentes Progresivos',
        'Lentes Monofocales',
        'Lentes Bifocales',
        'Monturas Premium',
        'Monturas Estandar',
        'Monturas Economicas',
        'Servicios Extra',
        'Garantias Extendidas',
        'Otros Lentes'
    ];
    v_idx INTEGER;
    v_cat_name TEXT;
    v_has_nonzero_ventas BOOLEAN := false;
    v_total_ventas NUMERIC := 0;
BEGIN
    v_result := public.rpc_analisis_mensual('zzt_tjs_o1', DATE '2026-07-01') -> 'margen_por_categoria';

    ASSERT jsonb_array_length(v_result) = 9,
        'T4 FAIL: expected 9 categories, got ' || jsonb_array_length(v_result);

    FOR v_idx IN 0..jsonb_array_length(v_result) - 1
    LOOP
        v_item := v_result -> v_idx;

        ASSERT v_item ? 'categoria', 'T4 FAIL: item ' || (v_idx + 1) || ' missing categoria';
        ASSERT v_item ? 'ventas', 'T4 FAIL: item ' || (v_idx + 1) || ' missing ventas';
        ASSERT v_item ? 'costos', 'T4 FAIL: item ' || (v_idx + 1) || ' missing costos';
        ASSERT v_item ? 'margen_pct', 'T4 FAIL: item ' || (v_idx + 1) || ' missing margen_pct';

        ASSERT jsonb_typeof(v_item -> 'ventas') = 'number',
            'T4 FAIL: item ' || (v_idx + 1) || ' ventas is not a number';
        ASSERT (v_item ->> 'ventas')::NUMERIC >= 0,
            'T4 FAIL: item ' || (v_idx + 1) || ' ventas is negative: ' || (v_item ->> 'ventas');

        v_total_ventas := v_total_ventas + (v_item ->> 'ventas')::NUMERIC;
        IF (v_item ->> 'ventas')::NUMERIC > 0 THEN
            v_has_nonzero_ventas := true;
        END IF;

        v_cat_name := v_item ->> 'categoria';
        ASSERT v_cat_name = v_expected_order[v_idx + 1],
            'T4 FAIL: order mismatch at position ' || (v_idx + 1) || ': expected "' ||
            v_expected_order[v_idx + 1] || '", got "' || v_cat_name || '"';

        IF v_cat_name IN ('Monturas Premium', 'Monturas Estandar', 'Monturas Economicas') THEN
            ASSERT (v_item ->> 'ventas')::NUMERIC = 0,
                'T4 FAIL: ' || v_cat_name || ' should have ventas = 0, got ' || (v_item ->> 'ventas');
        END IF;
    END LOOP;

    ASSERT v_has_nonzero_ventas,
        'T4 FAIL: no category has ventas > 0; total ventas: ' || v_total_ventas;

    RAISE NOTICE 'T4 PASS: JSON structure valid — % categories in order, total ventas = %',
        jsonb_array_length(v_result), v_total_ventas;
END;
$$;

ROLLBACK;
